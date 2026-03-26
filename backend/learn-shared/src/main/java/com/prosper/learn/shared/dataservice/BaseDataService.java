package com.prosper.learn.shared.dataservice;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 基础数据服务接口，提供通用的数据访问方法
 *
 * @param <T> 实体类型
 * @param <ID> 主键类型
 */
public interface BaseDataService<T, ID> {

    /**
     * 根据ID查询实体
     *
     * @param id 主键ID
     * @return 实体对象，不存在时返回null
     */
    T getById(ID id);

    /**
     * 批量根据ID查询实体列表
     *
     * @param ids ID集合
     * @return 实体列表
     */
    List<T> getByIds(Collection<ID> ids);

    /**
     * 批量根据ID查询实体映射
     *
     * @param ids ID集合
     * @return ID到实体的映射
     */
    Map<ID, T> getMapByIds(Collection<ID> ids);

    /**
     * 清除指定ID的缓存（子类按需实现）
     *
     * @param id 主键ID
     */
    void evictCache(ID id);

    /**
     * 清除所有相关缓存（子类按需实现）
     */
    void evictAllCache();
}
