package com.prosper.learn.persistence.repository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 基础Repository接口，提供通用的缓存数据访问方法
 * 
 * @param <T> 实体类型
 * @param <ID> 主键类型
 */
public interface BaseRepository<T, ID> {
    
    /**
     * 根据ID查询实体（带缓存）
     * 
     * @param id 主键ID
     * @return 实体对象，不存在时返回null
     */
    T getById(ID id);
    
    /**
     * 批量根据ID查询实体列表（带缓存）
     * 
     * @param ids ID集合
     * @return 实体列表
     */
    List<T> getByIds(Collection<ID> ids);
    
    /**
     * 批量根据ID查询实体映射（带缓存）
     * 
     * @param ids ID集合
     * @return ID到实体的映射
     */
    Map<ID, T> getMapByIds(Collection<ID> ids);
    
    /**
     * 清除指定ID的缓存
     * 
     * @param id 主键ID
     */
    void evictCache(ID id);
    
    /**
     * 清除所有相关缓存
     */
    void evictAllCache();
}