package com.prosper.learn.domain.service.data;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.common.config.SystemProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 基础数据服务抽象实现，提供通用的缓存和数据访问功能
 * 使用RedisTemplate实现高效的批量缓存操作
 * 
 * @param <T> 实体类型
 * @param <M> Mapper类型
 */
@Slf4j
public abstract class AbstractDataService<T, M, Y> implements BaseDataService<T, Y> {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private SystemProperties systemProperties;
    
    /**
     * 获取Mapper实例
     */
    protected abstract M mapper();
    
    /**
     * 获取缓存空间名称
     */
    protected abstract String getCacheName();
    
    /**
     * 获取实体名称（用于日志）
     */
    protected abstract String getEntityName();
    
    /**
     * 获取实体ID的方法
     */
    protected abstract Y getEntityId(T entity);
    
    /**
     * 通过Mapper获取单个实体
     */
    protected abstract T getByIdFromMapper(M mapper, Y id);
    
    /**
     * 通过Mapper批量获取实体列表
     */
    protected abstract List<T> getByIdsFromMapper(M mapper, Collection<Y> ids);
    
    /**
     * 通过Mapper批量获取实体映射
     */
    protected abstract Map<Y, T> getMapByIdsFromMapper(M mapper, Collection<Y> ids);
    
    /**
     * 获取缓存TTL（默认10分钟）
     */
    protected Duration getCacheTtl() {
        return Duration.ofMinutes(10);
    }
    
    /**
     * 生成缓存key
     */
    private String buildCacheKey(Y id) {
        return getCacheName() + "::" + id;
    }
    
    /**
     * 检查是否启用缓存
     */
    private boolean isCacheEnabled() {
        return !"none".equals(systemProperties.getCache().getType());
    }
    
    @Override
    @Cacheable(value = "#{getCacheName()}", key = "#id", condition = "#id != null")
    public T getById(Y id) {
        if (id == null) {
            log.warn("Attempt to query {} with null id", getEntityName());
            return null;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            T result = getByIdFromMapper(mapper(), id);
            long duration = System.currentTimeMillis() - startTime;
            
            if (result == null) {
                log.debug("No {} found with id: {}", getEntityName(), id);
            } else {
                log.debug("Found {} with id: {} in {}ms", getEntityName(), id, duration);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error querying {} with id: {}", getEntityName(), id, e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }
    
    @Override
    public List<T> getByIds(Collection<Y> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 过滤null值并去重，保持顺序
        List<Y> validIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
                
        if (validIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 如果缓存未启用，直接查询数据库
        if (!isCacheEnabled()) {
            try {
                List<T> result = getByIdsFromMapper(mapper(), validIds);
                log.debug("Cache disabled, retrieved {} {} entities from DB", 
                         result.size(), getEntityName());
                return result;
            } catch (Exception e) {
                log.error("Error querying {} from DB when cache disabled", getEntityName(), e);
                throw ErrorCode.DATABASE_ERROR.exception(e);
            }
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 1. 使用MGET批量从缓存获取
            Map<Y, T> cachedResults = batchGetFromCache(validIds);
            
            // 2. 找出缓存未命中的ID
            List<Y> missedIds = validIds.stream()
                    .filter(id -> !cachedResults.containsKey(id))
                    .collect(Collectors.toList());
            
            // 3. 从数据库查询未命中的数据
            Map<Y, T> dbResults = new HashMap<>();
            if (!missedIds.isEmpty()) {
                List<T> fromDB = getByIdsFromMapper(mapper(), missedIds);
                dbResults = fromDB.stream()
                        .collect(Collectors.toMap(this::getEntityId, Function.identity()));
                
                // 4. 使用分批Pipeline将数据库结果写入缓存
                batchPutToCache(dbResults);
                
                log.debug("Cache miss for {} {}: {}/{}, queried from DB in {}ms", 
                         getEntityName(), missedIds.size(), validIds.size(), 
                         System.currentTimeMillis() - startTime);
            }
            
            // 5. 合并缓存和数据库结果
            Map<Y, T> allResults = new HashMap<>(cachedResults);
            allResults.putAll(dbResults);
            
            // 6. 按原始顺序返回结果
            List<T> orderedResults = validIds.stream()
                    .map(allResults::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Retrieved {} {} entities (cache:{}, db:{}) in {}ms", 
                     orderedResults.size(), getEntityName(), 
                     cachedResults.size(), dbResults.size(), duration);
            
            return orderedResults;
            
        } catch (Exception e) {
            log.error("Error batch querying {} with ids: {}", getEntityName(), validIds, e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }
    
    @Override
    public Map<Y, T> getMapByIds(Collection<Y> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashMap<>();
        }
        
        // 复用getByIds的缓存逻辑
        List<T> entities = getByIds(ids);
        
        // 转换为Map
        return entities.stream()
                .collect(Collectors.toMap(this::getEntityId, Function.identity()));
    }
    
    /**
     * 使用Redis MGET批量从缓存获取数据
     */
    private Map<Y, T> batchGetFromCache(List<Y> ids) {
        Map<Y, T> results = new HashMap<>();
        
        if (ids.isEmpty() || !isCacheEnabled()) {
            return results;
        }
        
        try {
            // 构建缓存key列表
            List<String> keys = ids.stream()
                    .map(this::buildCacheKey)
                    .collect(Collectors.toList());
            
            // 使用MGET批量获取
            List<Object> values = redisTemplate.opsForValue().multiGet(keys);
            
            if (values != null) {
                // 将结果映射回ID
                for (int i = 0; i < ids.size() && i < values.size(); i++) {
                    Object value = values.get(i);
                    if (value != null) {
                        @SuppressWarnings("unchecked")
                        T entity = (T) value;
                        results.put(ids.get(i), entity);
                    }
                }
            }
            
            log.debug("MGET cache hit: {}/{} for {}", results.size(), ids.size(), getEntityName());
            
        } catch (Exception e) {
            log.warn("Error in batch cache get for {}: {}", getEntityName(), e.getMessage());
        }
        
        return results;
    }
    
    /**
     * 使用分批Pipeline批量写入缓存
     */
    private void batchPutToCache(Map<Y, T> entities) {
        if (entities.isEmpty() || !isCacheEnabled()) {
            return;
        }
        
        Duration ttl = getCacheTtl();
        
        // 简化实现：使用 RedisTemplate 的高级 API
        // 避免底层序列化器的类型问题
        entities.forEach((id, entity) -> {
            try {
                String key = buildCacheKey(id);
                redisTemplate.opsForValue().set(key, entity, ttl);
            } catch (Exception e) {
                log.warn("Failed to cache {} with id: {}", getEntityName(), id, e);
            }
        });
        
        log.debug("Cached {} entities for {}", entities.size(), getEntityName());
    }
    
    @Override
    @CacheEvict(value = "#{getCacheName()}", key = "#id")
    public void evictCache(Y id) {
        if (id != null && isCacheEnabled()) {
            try {
                redisTemplate.delete(buildCacheKey(id));
                log.debug("Evicted cache for {} with id: {}", getEntityName(), id);
            } catch (Exception e) {
                log.warn("Error evicting cache for {} with id: {}", getEntityName(), id, e);
            }
        }
    }
    
    @Override
    @CacheEvict(value = "#{getCacheName()}", allEntries = true)
    public void evictAllCache() {
        if (!isCacheEnabled()) {
            return;
        }
        
        try {
            Set<String> keys = redisTemplate.keys(getCacheName() + "::*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Evicted {} cache entries for {}", keys.size(), getEntityName());
            }
        } catch (Exception e) {
            log.warn("Error evicting all cache for {}: {}", getEntityName(), e.getMessage());
        }
    }
    
    /**
     * 带降级的查询
     */
    public T getByIdWithFallback(Y id) {
        try {
            return getById(id);
        } catch (Exception e) {
            log.warn("Failed to query {} with id: {}, using fallback", getEntityName(), id, e);
            return getDefaultEntity();
        }
    }
    
    /**
     * 获取默认实体（用于降级）
     */
    protected T getDefaultEntity() {
        return null;
    }


    // ========== 验证方法 ==========
    
    /**
     * 验证ID并获取实体（通用验证方法）
     */
    public T validateAndGet(Y id) {
        if (id == null) {
            throw ErrorCode.INVALID_PARAMETER.exception(getEntityName() + "ID不能为空");
        }

        T entity = getById(id);
        if (entity == null) {
            throw ErrorCode.NOT_FOUND.exception(getEntityName() + "不存在");
        }
        
        return entity;
    }
    
    /**
     * 验证ID的有效性（只检查ID格式，不查询数据库）
     */
    public void validateId(Y id) {
        if (id == null) {
            throw ErrorCode.INVALID_PARAMETER.exception(getEntityName() + "ID不能为空");
        }
        // 如果ID是Long类型，检查是否大于0
        if (id instanceof Long && ((Long) id) <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception(getEntityName() + "ID必须大于0");
        }
    }
    
    /**
     * 验证实体是否存在（只验证，不返回实体）
     */
    public void validateExists(Y id) {
        validateAndGet(id);
    }
    
    /**
     * 预热缓存
     */
    public void warmUpCache(Collection<Y> ids) {
        if (ids == null || ids.isEmpty() || !isCacheEnabled()) {
            return;
        }
        
        log.info("Warming up cache for {} with {} ids", getEntityName(), ids.size());
        getByIds(ids);
    }
    
    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats(Collection<Y> ids) {
        if (ids == null || ids.isEmpty() || !isCacheEnabled()) {
            return Collections.emptyMap();
        }
        
        List<Y> validIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Y, T> cached = batchGetFromCache(validIds);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", validIds.size());
        stats.put("cached", cached.size());
        stats.put("hitRate", validIds.isEmpty() ? 0.0 : (double) cached.size() / validIds.size());
        stats.put("entity", getEntityName());
        
        return stats;
    }
    
    /**
     * 根据ID删除实体并清除缓存
     */
    @CacheEvict(value = "#{target.cacheName}", key = "#id")
    public boolean deleteById(Y id) {
        if (id == null) {
            throw new IllegalArgumentException(getEntityName() + " ID cannot be null");
        }
        
        try {
            int result = deleteByIdFromMapper(mapper(), id);
            
            if (result > 0) {
                log.debug("Deleted {} with id: {}", getEntityName(), id);
            }
            
            return result > 0;
        } catch (Exception e) {
            log.error("Error deleting {} with id: {}", getEntityName(), id, e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }
    
    /**
     * 子类必须实现：从 Mapper 中根据ID删除实体
     */
    protected abstract int deleteByIdFromMapper(M mapper, Y id);
}