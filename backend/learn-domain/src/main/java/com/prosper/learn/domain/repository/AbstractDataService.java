package com.prosper.learn.domain.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.connection.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

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
public abstract class AbstractDataService<T, M> implements BaseDataService<T, Long> {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 批量操作的默认分批大小
     */
    private static final int DEFAULT_BATCH_SIZE = 100;
    
    /**
     * 获取Mapper实例
     */
    protected abstract M getMapper();
    
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
    protected abstract Long getEntityId(T entity);
    
    /**
     * 通过Mapper获取单个实体
     */
    protected abstract T getByIdFromMapper(M mapper, Long id);
    
    /**
     * 通过Mapper批量获取实体列表
     */
    protected abstract List<T> getByIdsFromMapper(M mapper, Collection<Long> ids);
    
    /**
     * 通过Mapper批量获取实体映射
     */
    protected abstract Map<Long, T> getMapByIdsFromMapper(M mapper, Collection<Long> ids);
    
    /**
     * 获取缓存TTL（默认10分钟）
     */
    protected Duration getCacheTtl() {
        return Duration.ofMinutes(10);
    }
    
    /**
     * 获取批量操作大小
     */
    protected int getBatchSize() {
        return DEFAULT_BATCH_SIZE;
    }
    
    /**
     * 生成缓存key
     */
    private String buildCacheKey(Long id) {
        return getCacheName() + "::" + id;
    }
    
    @Override
    @Cacheable(value = "#{getCacheName()}", key = "#id", condition = "#id != null")
    public T getById(Long id) {
        if (id == null) {
            log.warn("Attempt to query {} with null id", getEntityName());
            return null;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            T result = getByIdFromMapper(getMapper(), id);
            long duration = System.currentTimeMillis() - startTime;
            
            if (result == null) {
                log.debug("No {} found with id: {}", getEntityName(), id);
            } else {
                log.debug("Found {} with id: {} in {}ms", getEntityName(), id, duration);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error querying {} with id: {}", getEntityName(), id, e);
            throw new RuntimeException("Failed to query " + getEntityName() + " with id: " + id, e);
        }
    }
    
    @Override
    public List<T> getByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 过滤null值并去重，保持顺序
        List<Long> validIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
                
        if (validIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 1. 使用MGET批量从缓存获取
            Map<Long, T> cachedResults = batchGetFromCache(validIds);
            
            // 2. 找出缓存未命中的ID
            List<Long> missedIds = validIds.stream()
                    .filter(id -> !cachedResults.containsKey(id))
                    .collect(Collectors.toList());
            
            // 3. 从数据库查询未命中的数据
            Map<Long, T> dbResults = new HashMap<>();
            if (!missedIds.isEmpty()) {
                List<T> fromDB = getByIdsFromMapper(getMapper(), missedIds);
                dbResults = fromDB.stream()
                        .collect(Collectors.toMap(this::getEntityId, Function.identity()));
                
                // 4. 使用分批Pipeline将数据库结果写入缓存
                batchPutToCache(dbResults);
                
                log.debug("Cache miss for {} {}: {}/{}, queried from DB in {}ms", 
                         getEntityName(), missedIds.size(), validIds.size(), 
                         System.currentTimeMillis() - startTime);
            }
            
            // 5. 合并缓存和数据库结果
            Map<Long, T> allResults = new HashMap<>(cachedResults);
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
            throw new RuntimeException("Failed to batch query " + getEntityName(), e);
        }
    }
    
    @Override
    public Map<Long, T> getMapByIds(Collection<Long> ids) {
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
    private Map<Long, T> batchGetFromCache(List<Long> ids) {
        Map<Long, T> results = new HashMap<>();
        
        if (ids.isEmpty()) {
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
    private void batchPutToCache(Map<Long, T> entities) {
        if (entities.isEmpty()) {
            return;
        }
        
        int batchSize = getBatchSize();
        Duration ttl = getCacheTtl();
        List<Map.Entry<Long, T>> entries = new ArrayList<>(entities.entrySet());
        
        for (int i = 0; i < entries.size(); i += batchSize) {
            int end = Math.min(i + batchSize, entries.size());
            List<Map.Entry<Long, T>> batch = entries.subList(i, end);
            
            try {
                redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    RedisSerializer<String> keySerializer = redisTemplate.getStringSerializer();
                    RedisSerializer<Object> valueSerializer = redisTemplate.getValueSerializer();
                    
                    batch.forEach(entry -> {
                        try {
                            String key = buildCacheKey(entry.getKey());
                            byte[] keyBytes = keySerializer.serialize(key);
                            byte[] valueBytes = valueSerializer.serialize(entry.getValue());
                            
                            if (keyBytes != null && valueBytes != null) {
                                connection.setEx(keyBytes, ttl.getSeconds(), valueBytes);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to serialize cache entry for {} id: {}", 
                                    getEntityName(), entry.getKey(), e);
                        }
                    });
                    return null;
                });
                
                log.debug("Pipeline cached batch {}-{} ({} entities) for {}", 
                         i, end, batch.size(), getEntityName());
                
            } catch (Exception e) {
                log.warn("Failed to cache batch {}-{} for {}: {}", 
                        i, end, getEntityName(), e.getMessage());
                
                // 降级：逐个设置缓存
                fallbackPutToCache(batch, ttl);
            }
        }
    }
    
    /**
     * 降级方案：逐个设置缓存
     */
    private void fallbackPutToCache(List<Map.Entry<Long, T>> batch, Duration ttl) {
        try {
            batch.forEach(entry -> {
                try {
                    String key = buildCacheKey(entry.getKey());
                    redisTemplate.opsForValue().set(key, entry.getValue(), ttl);
                } catch (Exception e) {
                    log.warn("Failed to cache single entry for {} id: {}", 
                            getEntityName(), entry.getKey(), e);
                }
            });
            log.debug("Fallback cached {} entities for {}", batch.size(), getEntityName());
        } catch (Exception e) {
            log.error("Fallback cache also failed for {}: {}", getEntityName(), e.getMessage());
        }
    }
    
    @Override
    @CacheEvict(value = "#{getCacheName()}", key = "#id")
    public void evictCache(Long id) {
        if (id != null) {
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
    public T getByIdWithFallback(Long id) {
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
    
    /**
     * 预热缓存
     */
    public void warmUpCache(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        
        log.info("Warming up cache for {} with {} ids", getEntityName(), ids.size());
        getByIds(ids);
    }
    
    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        
        List<Long> validIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, T> cached = batchGetFromCache(validIds);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", validIds.size());
        stats.put("cached", cached.size());
        stats.put("hitRate", validIds.isEmpty() ? 0.0 : (double) cached.size() / validIds.size());
        stats.put("entity", getEntityName());
        
        return stats;
    }
}