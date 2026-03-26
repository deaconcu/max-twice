package com.prosper.learn.shared.dataservice;

import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 基础数据服务抽象实现
 * 提供通用的数据访问和验证功能
 *
 * 缓存策略：父类不处理缓存，由子类根据需要自行添加 @Cacheable/@CacheEvict 注解
 *
 * @param <T> 实体类型
 * @param <M> Mapper类型
 * @param <Y> ID类型
 */
@Slf4j
public abstract class AbstractDataService<T, M, Y> implements BaseDataService<T, Y> {

    /**
     * 获取Mapper实例
     */
    protected abstract M mapper();

    /**
     * 获取实体名称（用于日志和错误信息）
     */
    protected abstract String getEntityName();

    /**
     * 获取实体ID
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
     * 从Mapper中根据ID删除实体
     */
    protected abstract int deleteByIdFromMapper(M mapper, Y id);

    // ========== 查询方法 ==========

    @Override
    public T getById(Y id) {
        if (id == null) {
            log.warn("Attempt to query {} with null id", getEntityName());
            return null;
        }

        try {
            T result = getByIdFromMapper(mapper(), id);
            if (result == null) {
                log.debug("No {} found with id: {}", getEntityName(), id);
            }
            return result;
        } catch (Exception e) {
            log.error("Error querying {} with id: {}", getEntityName(), id, e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    @Override
    public List<T> getByIds(Collection<Y> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        List<Y> validIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (validIds.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return getByIdsFromMapper(mapper(), validIds);
        } catch (Exception e) {
            log.error("Error batch querying {} with ids: {}", getEntityName(), validIds, e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    @Override
    public Map<Y, T> getMapByIds(Collection<Y> ids) {
        List<T> entities = getByIds(ids);
        return entities.stream()
                .collect(Collectors.toMap(this::getEntityId, Function.identity()));
    }

    // ========== 删除方法 ==========

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
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    // ========== 验证方法 ==========

    /**
     * 验证ID并获取实体
     */
    public T validateAndGet(Y id) {
        if (id == null) {
            throw StatusCode.INVALID_PARAMETER.exception(getEntityName() + "ID不能为空");
        }

        if (id instanceof Number && ((Number) id).longValue() <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception(getEntityName() + "ID必须大于0");
        }

        T entity = getById(id);
        if (entity == null) {
            throw StatusCode.NOT_FOUND.exception(getEntityName() + "不存在");
        }

        return entity;
    }

    /**
     * 验证ID格式（不查数据库）
     */
    public void validateId(Y id) {
        if (id == null) {
            throw StatusCode.INVALID_PARAMETER.exception(getEntityName() + "ID不能为空");
        }
        if (id instanceof Long && ((Long) id) <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception(getEntityName() + "ID必须大于0");
        }
    }

    /**
     * 验证实体存在
     */
    public void validateExists(Y id) {
        validateAndGet(id);
    }

    // ========== 缓存方法（空实现，子类按需覆盖）==========

    @Override
    public void evictCache(Y id) {
        // 子类按需实现
    }

    @Override
    public void evictAllCache() {
        // 子类按需实现
    }
}
