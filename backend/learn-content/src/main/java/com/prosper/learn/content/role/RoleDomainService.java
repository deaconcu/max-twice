package com.prosper.learn.content.role;

import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.common.utils.ValidationUtils;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 角色领域服务
 *
 * 只依赖 content 模块，处理角色的核心业务逻辑
 *
 * 职责：
 * - 角色的增删改查
 * - 角色状态变更（SUBMITTED → PUBLISHED → REJECTED → BANNED）
 * - 权限验证
 * - 参数验证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleDomainService {

    private final RoleDataService roleDataService;
    private final SystemDomainService systemDomainService;

    // ========== Command 方法（写操作）==========

    /**
     * 创建角色
     *
     * @param creatorId 创建者ID
     * @param name 角色名称
     * @param description 描述
     * @param skills 技能列表
     * @param mainCategory 主分类
     * @param subCategory 子分类
     * @return 角色ID
     */
    @Transactional
    public Long create(long creatorId, String name, String description, String skills,
                       int mainCategory, int subCategory) {
        // 验证分类是否有效
        systemDomainService.validateRoleCategory(mainCategory, subCategory);

        RoleDO roleDO = new RoleDO();
        roleDO.setName(name);
        roleDO.setDescription(description);
        roleDO.setSkills(skills);
        roleDO.setMainCategory(mainCategory);
        roleDO.setSubCategory(subCategory);
        roleDO.setCreatorId(creatorId);
        roleDO.setState(ContentState.SUBMITTED.value());
        roleDO.setReason("");
        roleDO.setIcon("");

        roleDataService.insert(roleDO);

        log.info("角色 创建成功: roleId={}，name={}，creatorId={}",
                roleDO.getId(), name, creatorId);
        return roleDO.getId();
    }

    /**
     * 更新角色
     *
     * @param id 角色ID
     * @param name 角色名称
     * @param description 描述
     * @param price 价格
     * @param skills 技能列表
     * @param mainCategory 主分类
     * @param subCategory 子分类
     * @param icon 图标
     * @param reason 原因
     */
    @Transactional
    public void update(long id, String name, String description, String price, String skills,
                       int mainCategory, int subCategory, String icon, String reason) {
        // 参数验证
        ValidationUtils.requireNonBlank(name, "角色名称");

        // 验证分类是否有效
        systemDomainService.validateRoleCategory(mainCategory, subCategory);

        // 验证角色是否存在并获取
        RoleDO roleDO = roleDataService.getById(id);
        if (roleDO == null) {
            throw StatusCode.ROLE_NOT_FOUND.exception();
        }

        // 更新字段
        roleDO.setName(name);
        roleDO.setDescription(description);
        roleDO.setPrice(price);
        roleDO.setSkills(skills);
        roleDO.setMainCategory(mainCategory);
        roleDO.setSubCategory(subCategory);
        roleDO.setIcon(icon);
        roleDO.setReason(reason);

        roleDataService.update(roleDO);

        log.info("角色 更新成功: roleId={}", id);
    }

    /**
     * 审核通过角色
     *
     * @param id 角色ID
     * @param enableStateValidation 是否启用状态验证
     * @param enableConcurrencyCheck 是否启用并发检查
     * @return 受影响的行数
     */
    @Transactional
    public int approve(long id, boolean enableStateValidation, boolean enableConcurrencyCheck) {
        RoleDO roleDO = roleDataService.validateAndGet(id);

        // 状态验证：只有已批准的角色不能重复批准，已拒绝和已屏蔽的可以重新批准
        if (enableStateValidation) {
            Utils.validateStateTransition(roleDO.getState(), ContentState.PUBLISHED);
        }

        // 执行审核
        int rowsAffected = roleDataService.approve(id);

        // 并发检查
        if (enableConcurrencyCheck && rowsAffected == 0) {
            throw StatusCode.ROLE_STATE_CONFLICT.exception();
        }

        log.info("角色 审核通过: roleId={}", id);
        return rowsAffected;
    }

    /**
     * 拒绝角色
     *
     * @param id 角色ID
     * @param reason 拒绝原因
     * @param enableStateValidation 是否启用状态验证
     * @param enableConcurrencyCheck 是否启用并发检查
     * @return 受影响的行数
     */
    @Transactional
    public int reject(long id, String reason, boolean enableStateValidation, boolean enableConcurrencyCheck) {
        RoleDO roleDO = roleDataService.validateAndGet(id);

        // 状态验证：已拒绝和已屏蔽的不能重复拒绝
        if (enableStateValidation) {
            Utils.validateStateTransition(roleDO.getState(), ContentState.REJECTED);
        }

        String reasonValue = reason != null ? reason : "";

        // 执行拒绝
        int rowsAffected = roleDataService.reject(id, reasonValue);

        // 并发检查
        if (enableConcurrencyCheck && rowsAffected == 0) {
            throw StatusCode.ROLE_STATE_CONFLICT.exception();
        }

        log.info("角色 审核拒绝: roleId={}，reason={}", id, reasonValue);
        return rowsAffected;
    }

    /**
     * 封禁角色
     *
     * @param id 角色ID
     * @param reason 封禁原因
     * @param enableStateValidation 是否启用状态验证
     * @param enableConcurrencyCheck 是否启用并发检查
     * @return 受影响的行数
     */
    @Transactional
    public int ban(long id, String reason, boolean enableStateValidation, boolean enableConcurrencyCheck) {
        RoleDO roleDO = roleDataService.validateAndGet(id);

        // 状态验证：已屏蔽的不能重复屏蔽
        if (enableStateValidation) {
            Utils.validateStateTransition(roleDO.getState(), ContentState.BANNED);
        }

        String reasonValue = reason != null ? reason : "";

        // 执行封禁
        int rowsAffected = roleDataService.ban(id, reasonValue);

        // 并发检查
        if (enableConcurrencyCheck && rowsAffected == 0) {
            throw StatusCode.ROLE_STATE_CONFLICT.exception();
        }

        log.info("角色 封禁: roleId={}，reason={}", id, reasonValue);
        return rowsAffected;
    }

    /**
     * 删除角色
     *
     * @param id 角色ID
     */
    @Transactional
    public void delete(long id) {
        RoleDO roleDO = roleDataService.getById(id);
        if (roleDO == null) {
            throw StatusCode.ROLE_NOT_FOUND.exception();
        }

        // 执行删除
        roleDataService.delete(id);

        log.info("角色 删除成功: roleId={}", id);
    }

    // ========== Query 方法（读操作）==========

    /**
     * 根据ID获取角色
     */
    public RoleDO getById(Long id) {
        return roleDataService.getById(id);
    }

    /**
     * 验证并获取角色
     */
    public RoleDO validateAndGet(Long id) {
        return roleDataService.validateAndGet(id);
    }

    /**
     * 根据状态获取角色列表
     */
    public List<RoleDO> listByState(Byte state, Long lastId, int limit) {
        return roleDataService.listByState(state, lastId, limit);
    }

    /**
     * 根据主分类和最后ID获取角色列表
     */
    public List<RoleDO> listByMainCategoryAndLastId(int mainCategory, Long lastId, int limit) {
        return roleDataService.listByMainCategoryAndLastId(mainCategory, lastId, limit);
    }

    /**
     * 根据子分类和最后ID获取角色列表
     */
    public List<RoleDO> listBySubCategoryAndLastId(int subCategory, Long lastId, int limit) {
        return roleDataService.listBySubCategoryAndLastId(subCategory, lastId, limit);
    }

    /**
     * 根据主分类和子分类获取角色列表
     */
    public List<RoleDO> listByMainCategoryAndSubCategoryAndLastId(int mainCategory, int subCategory, Long lastId, int limit) {
        return roleDataService.listByMainCategoryAndSubCategoryAndLastId(mainCategory, subCategory, lastId, limit);
    }

    /**
     * 搜索角色（按关键词，用户端）
     */
    public List<RoleDO> searchByKeyword(String keyword) {
        return roleDataService.searchByKeyword(keyword);
    }

    /**
     * 管理后台按名称搜索角色（搜索所有状态，支持分页）
     */
    public List<RoleDO> searchByName(String name, Long lastId, int limit) {
        return roleDataService.searchByName(name, lastId, limit);
    }

    /**
     * 根据ID列表批量获取角色
     */
    public List<RoleDO> getByIds(List<Long> ids) {
        return roleDataService.getByIds(ids);
    }
}
