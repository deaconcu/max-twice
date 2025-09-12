package com.prosper.learn.domain.service.data;

import com.prosper.learn.common.exception.ErrorCode;
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
public class UserDataService extends AbstractDataService<UserDO, UserMapper, Long> {
    
    @Autowired
    private UserMapper userMapper;
    
    @Override
    protected UserMapper mapper() {
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
    protected int deleteByIdFromMapper(UserMapper mapper, Long id) {
        return 0;
    }

    /**
     * 根据邮箱查询用户（带缓存）
     */
    @Cacheable(value = "usersByEmail", key = "#email")
    public UserDO getByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return userMapper.getByEmail(email);
    }
    
    /**
     * 更新用户信息并清除缓存
     */
    @CacheEvict(value = "users", key = "#user.id")
    public void update(UserDO user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User or user ID cannot be null");
        }

        userMapper.update(user);

        // 如果邮箱可能变更，也要清除邮箱缓存
        if (user.getEmail() != null) {
            evictEmailCache(user.getEmail());
        }
        log.debug("Updated user {}", user.getId());
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
    
    /**
     * 根据名称搜索用户（不缓存）
     */
    public List<UserDO> searchByName(String name) {
        return userMapper.searchByName(name);
    }
    
    /**
     * 插入用户
     */
    public int insert(UserDO user) {
        return userMapper.insert(user);
    }
}