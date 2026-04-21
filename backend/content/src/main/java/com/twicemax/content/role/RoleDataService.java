package com.twicemax.content.role;

import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 角色数据服务
 * 负责角色数据的 CRUD 和缓存管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleDataService {

    private final RoleMapper roleMapper;

    // ==================== 查询方法 ====================

    /**
     * 根据ID查询角色
     */
    @Cacheable(value = "roles", key = "#id", unless = "#result == null")
    public RoleDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return roleMapper.getById(id);
    }

    /**
     * 批量根据ID查询角色
     */
    public List<RoleDO> getByIds(Collection<Long> ids) {
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
        return roleMapper.getByIds(validIds);
    }

    /**
     * 批量根据ID查询角色并转为Map
     */
    public Map<Long, RoleDO> getMapByIds(Collection<Long> ids) {
        return getByIds(ids).stream()
                .collect(Collectors.toMap(RoleDO::getId, Function.identity()));
    }

    /**
     * 统计活跃角色数量
     */
    public Long countActiveRoles() {
        return roleMapper.countActiveRoles();
    }

    /**
     * 根据状态获取角色列表
     */
    public List<RoleDO> listByState(Byte state, Long lastId, int limit) {
        return roleMapper.listByState(state, lastId, limit);
    }

    /**
     * 根据主分类查询
     */
    public List<RoleDO> listByMainCategoryAndLastId(int mainCategory, Long lastId, int limit) {
        return roleMapper.listByMainCategoryAndLastId(mainCategory, lastId, limit);
    }

    /**
     * 根据子分类查询
     */
    public List<RoleDO> listBySubCategoryAndLastId(int subCategory, Long lastId, int limit) {
        return roleMapper.listBySubCategoryAndLastId(subCategory, lastId, limit);
    }

    /**
     * 根据主分类和子分类查询
     */
    public List<RoleDO> listByMainCategoryAndSubCategoryAndLastId(int mainCategory, int subCategory, Long lastId, int limit) {
        return roleMapper.listByMainCategoryAndSubCategoryAndLastId(mainCategory, subCategory, lastId, limit);
    }

    /**
     * 搜索角色（按关键词，用户端）
     */
    public List<RoleDO> searchByKeyword(String keyword) {
        return roleMapper.searchByKeyword(keyword);
    }

    /**
     * 管理后台按名称搜索角色
     */
    public List<RoleDO> searchByName(String name, Long lastId, int limit) {
        return roleMapper.searchByName(name, lastId, limit);
    }

    // ==================== 验证方法 ====================

    /**
     * 验证角色ID并获取角色
     */
    public RoleDO validateAndGet(Long id) {
        if (id == null || id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("角色ID无效");
        }
        RoleDO roleDO = getById(id);
        if (roleDO == null) {
            throw StatusCode.ROLE_NOT_FOUND.exception();
        }
        return roleDO;
    }

    /**
     * 验证角色存在
     */
    public void validateExists(Long id) {
        validateAndGet(id);
    }

    // ==================== 写入方法 ====================

    /**
     * 插入角色
     */
    public void insert(RoleDO roleDO) {
        roleMapper.insert(roleDO);
    }

    /**
     * 更新角色
     */
    @CacheEvict(value = "roles", key = "#roleDO.id")
    public void update(RoleDO roleDO) {
        if (roleDO == null || roleDO.getId() == null) {
            throw new IllegalArgumentException("roleDO ID cannot be null");
        }
        roleMapper.update(roleDO);
    }

    /**
     * 审批通过角色
     */
    @CacheEvict(value = "roles", key = "#id")
    public int approve(long id) {
        return roleMapper.updateState(id, Enums.ContentState.PUBLISHED.value(), "");
    }

    /**
     * 拒绝角色申请
     */
    @CacheEvict(value = "roles", key = "#id")
    public int reject(long id, String reason) {
        return roleMapper.updateState(id, Enums.ContentState.REJECTED.value(), reason);
    }

    /**
     * 封禁角色
     */
    @CacheEvict(value = "roles", key = "#id")
    public int ban(long id, String reason) {
        return roleMapper.updateState(id, Enums.ContentState.BANNED.value(), reason);
    }

    /**
     * 删除角色
     */
    @CacheEvict(value = "roles", key = "#id")
    public void delete(long id) {
        roleMapper.delete(id);
    }
}
