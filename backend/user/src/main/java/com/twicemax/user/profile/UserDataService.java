package com.twicemax.user.profile;

import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.shared.domain.Enums.UserState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户数据服务
 * 负责用户数据的 CRUD 和缓存管理
 *
 * 缓存策略：
 * - users: userId -> UserDO（完整用户对象）
 * - userIdByEmail: email -> userId（只存ID映射）
 * - userIdByName: name -> userId（只存ID映射）
 *
 * 这样设计的好处：
 * - 修改用户属性（头像、状态等）只需清除 users::{userId}
 * - 修改邮箱需额外清除 userIdByEmail::{oldEmail}
 * - 修改用户名需额外清除 userIdByName::{oldName}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataService {

    private final UserMapper userMapper;

    // ==================== 查询方法 ====================

    /**
     * 根据ID查询用户
     */
    @Cacheable(value = "users", key = "#id", unless = "#result == null")
    public UserDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return userMapper.getById(id);
    }

    /**
     * 批量根据ID查询用户（不缓存，直接查库）
     */
    public List<UserDO> getByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> validIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (validIds.isEmpty()) {
            return new ArrayList<>();
        }
        return userMapper.getByIds(validIds);
    }

    /**
     * 批量根据ID查询用户并转为Map
     */
    public Map<Long, UserDO> getMapByIds(Collection<Long> ids) {
        return getByIds(ids).stream()
                .collect(Collectors.toMap(UserDO::getId, Function.identity()));
    }

    /**
     * 根据邮箱获取用户ID（只缓存ID映射）
     */
    @Cacheable(value = "userIdByEmail", key = "#email", unless = "#result == null")
    public Long getUserIdByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        UserDO user = userMapper.getByEmail(email);
        return user != null ? user.getId() : null;
    }

    /**
     * 根据邮箱查询用户（组合查询：先查ID缓存，再查用户缓存）
     */
    public UserDO getByEmail(String email) {
        Long userId = getUserIdByEmail(email);
        return userId != null ? getById(userId) : null;
    }

    /**
     * 根据用户名获取用户ID（只缓存ID映射）
     */
    @Cacheable(value = "userIdByName", key = "#name", unless = "#result == null")
    public Long getUserIdByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        UserDO user = userMapper.getByName(name);
        return user != null ? user.getId() : null;
    }

    /**
     * 根据用户名查询用户（组合查询：先查ID缓存，再查用户缓存）
     */
    public UserDO getByName(String name) {
        Long userId = getUserIdByName(name);
        return userId != null ? getById(userId) : null;
    }

    /**
     * 根据名称搜索用户（模糊匹配，不缓存）
     */
    public List<UserDO> searchByName(String name) {
        return userMapper.searchByName(name, UserState.ACTIVE.value());
    }

    /**
     * 获取用户列表
     */
    public List<UserDO> getList(int count) {
        return userMapper.getList(count);
    }

    /**
     * 获取用户列表（分页）
     */
    public List<UserDO> getListPaginated(long offsetId, int count) {
        return userMapper.getListPaginated(offsetId, count);
    }

    /**
     * 根据状态获取用户列表
     */
    public List<UserDO> listByState(String state, Long lastId, int limit) {
        return userMapper.listByState(state, lastId, limit);
    }

    // ==================== 验证方法 ====================

    /**
     * 验证用户ID并获取用户
     */
    public UserDO validateAndGet(Long userId) {
        if (userId == null || userId <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("用户ID无效");
        }
        UserDO user = getById(userId);
        if (user == null) {
            throw StatusCode.USER_NOT_FOUND.exception();
        }
        return user;
    }

    /**
     * 验证用户名并获取用户
     */
    public UserDO validateAndGetByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw StatusCode.INVALID_PARAMETER.exception("用户名不能为空");
        }
        UserDO user = getByName(name);
        if (user == null) {
            throw StatusCode.USER_NOT_FOUND.exception();
        }
        return user;
    }

    /**
     * 验证用户存在
     */
    public void validateExists(Long userId) {
        validateAndGet(userId);
    }

    // ==================== 写入方法 ====================

    /**
     * 插入用户
     */
    public int insert(UserDO user) {
        return userMapper.insert(user);
    }

    /**
     * 更新用户基本信息（可能修改name/email，需要先查旧值清除缓存）
     */
    public void update(UserDO user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User or user ID cannot be null");
        }

        // 先查出旧数据，用于清除旧的缓存
        UserDO oldUser = userMapper.getById(user.getId());

        // 更新数据库
        userMapper.update(user);

        // 清除缓存
        evictUserCache(user.getId());
        evictUserIdByNameCache(oldUser.getName());
        evictUserIdByEmailCache(oldUser.getEmail());

        log.debug("用户 更新成功: userId={}", user.getId());
    }

    /**
     * 更新用户头像
     */
    @CacheEvict(value = "users", key = "#userId")
    public int updateAvatar(long userId, String avatarUrl) {
        return userMapper.updateAvatar(userId, avatarUrl);
    }

    /**
     * 更新用户状态
     */
    @CacheEvict(value = "users", key = "#userId")
    public void updateState(long userId, String state) {
        userMapper.updateState(userId, state, LocalDateTime.now());
    }

    /**
     * 更新用户角色
     */
    @CacheEvict(value = "users", key = "#userId")
    public void updateRole(long userId, String role) {
        userMapper.updateRole(userId, role, LocalDateTime.now());
    }

    /**
     * 更新邮箱验证状态
     */
    @CacheEvict(value = "users", key = "#userId")
    public void updateEmailValidated(long userId, boolean emailValidated) {
        userMapper.updateEmailValidated(userId, emailValidated, LocalDateTime.now());
    }

    /**
     * 更新用户密码（传入的已是 BCrypt 加密后的串）
     */
    @CacheEvict(value = "users", key = "#userId")
    public int updatePassword(long userId, String encodedPassword) {
        return userMapper.updatePassword(userId, encodedPassword);
    }

    /**
     * 更新用户偏好语言（LanguageSwitcher 走这里）
     */
    @CacheEvict(value = "users", key = "#userId")
    public int updateLocale(long userId, String locale) {
        return userMapper.updateLocale(userId, locale, LocalDateTime.now());
    }

    // ==================== 缓存清除（内部使用）====================

    @CacheEvict(value = "users", key = "#userId")
    public void evictUserCache(Long userId) {
        // 仅用于触发缓存清除
    }

    @CacheEvict(value = "userIdByEmail", key = "#email")
    public void evictUserIdByEmailCache(String email) {
        // 仅用于触发缓存清除
    }

    @CacheEvict(value = "userIdByName", key = "#name")
    public void evictUserIdByNameCache(String name) {
        // 仅用于触发缓存清除
    }
}
