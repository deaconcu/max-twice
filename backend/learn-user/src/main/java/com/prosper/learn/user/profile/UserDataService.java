package com.prosper.learn.user.profile;

import com.prosper.learn.shared.dataservice.AbstractDataService;
import com.prosper.learn.shared.domain.exception.ErrorCode;
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
     * 根据用户名查询用户（带缓存）
     */
    @Cacheable(value = "usersByName", key = "#name")
    public UserDO getByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return userMapper.getByName(name);
    }

    /**
     * 根据用户名查询用户并验证存在（带缓存）
     */
    public UserDO validateAndGetByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
        UserDO userDO = getByName(name);
        if (userDO == null) {
            throw ErrorCode.USER_NOT_FOUND.exception();
        }
        return userDO;
    }

    /**
     * 更新用户信息并清除缓存
     */
    @CacheEvict(value = "users", key = "#user.id")
    public void update(UserDO user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User or user ID cannot be null");
        }

        // 如果用户名可能变更，先获取旧用户名用于清除缓存
        UserDO oldUser = userMapper.getById(user.getId());
        String oldName = oldUser != null ? oldUser.getName() : null;

        userMapper.update(user);

        // 如果邮箱可能变更，也要清除邮箱缓存
        if (user.getEmail() != null) {
            evictEmailCache(user.getEmail());
        }

        // 清除旧用户名的缓存
        if (oldName != null) {
            evictNameCache(oldName);
        }

        // 清除新用户名的缓存（如果用户名变更了）
        if (user.getName() != null && !user.getName().equals(oldName)) {
            evictNameCache(user.getName());
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
     * 清除用户名缓存
     */
    @CacheEvict(value = "usersByName", key = "#name")
    public void evictNameCache(String name) {
        if (name != null) {
            log.debug("Evicted name cache for: {}", name);
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

    /**
     * 更新用户头像并清除缓存
     */
    @CacheEvict(value = "users", key = "#userId")
    public int updateAvatar(Long userId, String avatarUrl) {
        log.debug("Updating avatar for user {}: {}", userId, avatarUrl);
        return userMapper.updateAvatar(userId, avatarUrl);
    }

    public List<UserDO> getList(int count) {
        return userMapper.getList(count);
    }

    public List<UserDO> getListPaginated(long offsetId, int count) {
        return userMapper.getListPaginated(offsetId, count);
    }
}