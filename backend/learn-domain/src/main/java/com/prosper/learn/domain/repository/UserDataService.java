package com.prosper.learn.domain.repository;

import com.prosper.learn.persistence.dataobject.UserDO;
import com.prosper.learn.persistence.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户数据服务，提供缓存功能
 * 专注于数据访问和缓存管理，避免循环依赖
 */
@Slf4j
@Service
public class UserDataService extends AbstractDataService<UserDO, UserMapper> {
    
    @Autowired
    private UserMapper userMapper;
    
    @Override
    protected UserMapper getMapper() {
        return userMapper;
    }
    
    @Override
    protected String getCacheName() {
        return "users";
    }
    
    @Override
    protected String getEntityName() {
        return "User";
    }
    
    @Override
    protected Long getEntityId(UserDO entity) {
        return entity.getId();
    }
    
    @Override
    protected UserDO getByIdFromMapper(UserMapper mapper, Long id) {
        return mapper.getById(id);
    }
    
    @Override
    protected List<UserDO> getByIdsFromMapper(UserMapper mapper, Collection<Long> ids) {
        return mapper.getByIds(ids);
    }
    
    @Override
    protected Map<Long, UserDO> getMapByIdsFromMapper(UserMapper mapper, Collection<Long> ids) {
        return mapper.getMapByIds(ids);
    }
    
    @Override
    protected Duration getCacheTtl() {
        // 用户信息缓存30分钟
        return Duration.ofMinutes(30);
    }
    
    @Override
    protected int getBatchSize() {
        // 用户查询使用较大的批次
        return 200;
    }
    
    /**
     * 根据邮箱查询用户（带缓存）
     */
    @Cacheable(value = "usersByEmail", key = "#email")
    public UserDO getByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        
        try {
            return userMapper.getByEmail(email);
        } catch (Exception e) {
            log.error("Error querying user by email: {}", email, e);
            throw new RuntimeException("Failed to query user by email: " + email, e);
        }
    }
    
    /**
     * 更新用户信息并清除缓存
     */
    @CacheEvict(value = "users", key = "#user.id")
    public void update(UserDO user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User or user ID cannot be null");
        }
        
        try {
            userMapper.update(user);
            // 如果邮箱可能变更，也要清除邮箱缓存
            if (user.getEmail() != null) {
                evictEmailCache(user.getEmail());
            }
            log.debug("Updated user {}", user.getId());
        } catch (Exception e) {
            log.error("Error updating user: {}", user.getId(), e);
            throw new RuntimeException("Failed to update user: " + user.getId(), e);
        }
    }
    
    /**
     * 清除邮箱缓存
     */
    @CacheEvict(value = "usersByEmail", key = "#email")
    public void evictEmailCache(String email) {
        if (email != null) {
            log.debug("Evicted email cache for: {}", email);
        }
    }
}