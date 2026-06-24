package com.twicemax.content.role;

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
 * <p>
 * 配合 revision 模型：state 是 NewContentState 字符串值；写入/状态切换通过专用方法
 * （updatePending / approve / ban / updateState）协同 RoleMapper 完成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleDataService {

    private final RoleMapper roleMapper;

    // ==================== 查询方法 ====================

    @Cacheable(value = "roles", key = "#id", unless = "#result == null")
    public RoleDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return roleMapper.getById(id);
    }

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

    public Map<Long, RoleDO> getMapByIds(Collection<Long> ids) {
        return getByIds(ids).stream()
                .collect(Collectors.toMap(RoleDO::getId, Function.identity()));
    }

    public Long countActiveRoles() {
        return roleMapper.countActiveRoles();
    }

    /**
     * 按主体状态分页（state 为 NewContentState 字符串值）。
     */
    public List<RoleDO> listByState(String state, Long lastId, int limit) {
        return roleMapper.listByState(state, lastId, limit);
    }

    public List<RoleDO> listByMainCategoryAndLastId(int mainCategory, Long lastId, int limit) {
        return roleMapper.listByMainCategoryAndLastId(mainCategory, lastId, limit);
    }

    public List<RoleDO> listBySubCategoryAndLastId(int subCategory, Long lastId, int limit) {
        return roleMapper.listBySubCategoryAndLastId(subCategory, lastId, limit);
    }

    public List<RoleDO> listByMainCategoryAndSubCategoryAndLastId(int mainCategory, int subCategory, Long lastId, int limit) {
        return roleMapper.listByMainCategoryAndSubCategoryAndLastId(mainCategory, subCategory, lastId, limit);
    }

    public List<RoleDO> searchByKeyword(String keyword) {
        return roleMapper.searchByKeyword(keyword);
    }

    public List<RoleDO> searchByName(String name, Long lastId, int limit) {
        return roleMapper.searchByName(name, lastId, limit);
    }

    /**
     * 按创建者分页（state 为 NewContentState 字符串值，可为 null 表示默认排除 BANNED）。
     */
    public List<RoleDO> listByCreator(long creatorId, Long lastId, int limit, String state) {
        return roleMapper.listByCreator(creatorId, lastId, limit, state);
    }

    // ==================== 验证方法 ====================

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

    public void validateExists(Long id) {
        validateAndGet(id);
    }

    // ==================== 写入方法 ====================

    public void insert(RoleDO roleDO) {
        roleMapper.insert(roleDO);
    }

    @CacheEvict(value = "roles", key = "#roleDO.id")
    public void update(RoleDO roleDO) {
        if (roleDO == null || roleDO.getId() == null) {
            throw new IllegalArgumentException("roleDO ID cannot be null");
        }
        roleMapper.update(roleDO);
    }

    /**
     * 切换 pending_revision_id（提交 / 撤回 / 驳回 时用）。
     */
    @CacheEvict(value = "roles", key = "#id")
    public int updatePending(long id, Long pendingRevisionId) {
        return roleMapper.updatePending(id, pendingRevisionId);
    }

    /**
     * 审核通过：state=PUBLISHED，刷新内容镜像字段，设置 current_revision_id，清空 pending_revision_id。
     */
    @CacheEvict(value = "roles", key = "#id")
    public int approve(long id, String name, String description, String icon, String skills,
                       int mainCategory, int subCategory, long currentRevisionId) {
        return roleMapper.approve(id, name, description, icon, skills, mainCategory, subCategory, currentRevisionId);
    }

    /**
     * 封禁：state=BANNED，pending_revision_id 清空。
     */
    @CacheEvict(value = "roles", key = "#id")
    public int ban(long id) {
        return roleMapper.ban(id);
    }

    /**
     * 简单状态切换（解封时使用）。
     */
    @CacheEvict(value = "roles", key = "#id")
    public int updateState(long id, String state) {
        return roleMapper.updateState(id, state);
    }

    @CacheEvict(value = "roles", key = "#id")
    public void delete(long id) {
        roleMapper.delete(id);
    }
}
