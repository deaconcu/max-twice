package com.prosper.learn.content.profession;

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
 * 职业领域服务
 *
 * 只依赖 content 模块，处理职业的核心业务逻辑
 *
 * 职责：
 * - 职业的增删改查
 * - 职业状态变更（SUBMITTED → PUBLISHED → REJECTED → BANNED）
 * - 权限验证
 * - 参数验证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfessionDomainService {

    private final ProfessionDataService professionDataService;
    private final SystemDomainService systemDomainService;

    // ========== Command 方法（写操作）==========

    /**
     * 创建职业
     *
     * @param creatorId 创建者ID
     * @param name 职业名称
     * @param description 描述
     * @param skills 技能列表
     * @param mainCategory 主分类
     * @param subCategory 子分类
     * @return 职业ID
     */
    @Transactional
    public Long create(long creatorId, String name, String description, String skills,
                       int mainCategory, int subCategory) {
        // 验证分类是否有效
        systemDomainService.validateProfessionCategory(mainCategory, subCategory);

        ProfessionDO professionDO = new ProfessionDO();
        professionDO.setName(name);
        professionDO.setDescription(description);
        professionDO.setSkills(skills);
        professionDO.setMainCategory(mainCategory);
        professionDO.setSubCategory(subCategory);
        professionDO.setCreatorId(creatorId);
        professionDO.setState(ContentState.SUBMITTED.value());
        professionDO.setReason("");
        professionDO.setIcon("");

        professionDataService.insert(professionDO);

        log.info("Created profession: id={}, name={}, creatorId={}",
                professionDO.getId(), name, creatorId);
        return professionDO.getId();
    }

    /**
     * 更新职业
     *
     * @param id 职业ID
     * @param operatorId 操作者ID
     * @param operatorRole 操作者角色
     * @param name 职业名称
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
        ValidationUtils.requireNonBlank(name, "职业名称");

        // 验证分类是否有效
        systemDomainService.validateProfessionCategory(mainCategory, subCategory);

        // 验证职业是否存在并获取
        ProfessionDO professionDO = professionDataService.getById(id);
        if (professionDO == null) {
            throw StatusCode.PROFESSION_NOT_FOUND.exception();
        }

        // 更新字段
        professionDO.setName(name);
        professionDO.setDescription(description);
        professionDO.setPrice(price);
        professionDO.setSkills(skills);
        professionDO.setMainCategory(mainCategory);
        professionDO.setSubCategory(subCategory);
        professionDO.setIcon(icon);
        professionDO.setReason(reason);

        professionDataService.update(professionDO);

        log.info("Updated profession: id={}", id);
    }

    /**
     * 审核通过职业
     *
     * @param id 职业ID
     * @param enableStateValidation 是否启用状态验证
     * @param enableConcurrencyCheck 是否启用并发检查
     * @return 受影响的行数
     */
    @Transactional
    public int approve(long id, boolean enableStateValidation, boolean enableConcurrencyCheck) {
        ProfessionDO profession = professionDataService.validateAndGet(id);

        // 状态验证：只有已批准的职业不能重复批准，已拒绝和已屏蔽的可以重新批准
        if (enableStateValidation) {
            Utils.validateStateTransition(profession.getState(), ContentState.PUBLISHED);
        }

        // 执行审核
        int rowsAffected = professionDataService.approve(id);

        // 并发检查
        if (enableConcurrencyCheck && rowsAffected == 0) {
            throw StatusCode.PROFESSION_STATE_CONFLICT.exception();
        }

        log.info("Approved profession: id={}", id);
        return rowsAffected;
    }

    /**
     * 拒绝职业
     *
     * @param id 职业ID
     * @param reason 拒绝原因
     * @param enableStateValidation 是否启用状态验证
     * @param enableConcurrencyCheck 是否启用并发检查
     * @return 受影响的行数
     */
    @Transactional
    public int reject(long id, String reason, boolean enableStateValidation, boolean enableConcurrencyCheck) {
        ProfessionDO profession = professionDataService.validateAndGet(id);

        // 状态验证：已拒绝和已屏蔽的不能重复拒绝
        if (enableStateValidation) {
            Utils.validateStateTransition(profession.getState(), ContentState.REJECTED);
        }

        String reasonValue = reason != null ? reason : "";

        // 执行拒绝
        int rowsAffected = professionDataService.reject(id, reasonValue);

        // 并发检查
        if (enableConcurrencyCheck && rowsAffected == 0) {
            throw StatusCode.PROFESSION_STATE_CONFLICT.exception();
        }

        log.info("Rejected profession: id={}, reason={}", id, reasonValue);
        return rowsAffected;
    }

    /**
     * 封禁职业
     *
     * @param id 职业ID
     * @param reason 封禁原因
     * @param enableStateValidation 是否启用状态验证
     * @param enableConcurrencyCheck 是否启用并发检查
     * @return 受影响的行数
     */
    @Transactional
    public int ban(long id, String reason, boolean enableStateValidation, boolean enableConcurrencyCheck) {
        ProfessionDO profession = professionDataService.validateAndGet(id);

        // 状态验证：已屏蔽的不能重复屏蔽
        if (enableStateValidation) {
            Utils.validateStateTransition(profession.getState(), ContentState.BANNED);
        }

        String reasonValue = reason != null ? reason : "";

        // 执行封禁
        int rowsAffected = professionDataService.ban(id, reasonValue);

        // 并发检查
        if (enableConcurrencyCheck && rowsAffected == 0) {
            throw StatusCode.PROFESSION_STATE_CONFLICT.exception();
        }

        log.info("Banned profession: id={}, reason={}", id, reasonValue);
        return rowsAffected;
    }

    /**
     * 删除职业
     *
     * @param id 职业ID
     */
    @Transactional
    public void delete(long id) {
        ProfessionDO professionDO = professionDataService.getById(id);
        if (professionDO == null) {
            throw StatusCode.PROFESSION_NOT_FOUND.exception();
        }

        // 执行删除
        professionDataService.delete(id);

        log.info("Deleted profession: id={}", id);
    }

    // ========== Query 方法（读操作）==========

    /**
     * 根据ID获取职业
     */
    public ProfessionDO getById(Long id) {
        return professionDataService.getById(id);
    }

    /**
     * 验证并获取职业
     */
    public ProfessionDO validateAndGet(Long id) {
        return professionDataService.validateAndGet(id);
    }

    /**
     * 根据状态和最后ID获取职业列表
     */
    public List<ProfessionDO> listByStateAndLastId(int state, Long lastId, int limit) {
        return professionDataService.listByStateAndLastId((byte) state, lastId, limit);
    }

    /**
     * 根据主分类和最后ID获取职业列表
     */
    public List<ProfessionDO> listByMainCategoryAndLastId(int mainCategory, Long lastId, int limit) {
        return professionDataService.listByMainCategoryAndLastId(mainCategory, lastId, limit);
    }

    /**
     * 根据子分类和最后ID获取职业列表
     */
    public List<ProfessionDO> listBySubCategoryAndLastId(int subCategory, Long lastId, int limit) {
        return professionDataService.listBySubCategoryAndLastId(subCategory, lastId, limit);
    }

    /**
     * 根据主分类和子分类获取职业列表
     */
    public List<ProfessionDO> listByMainCategoryAndSubCategoryAndLastId(int mainCategory, int subCategory, Long lastId, int limit) {
        return professionDataService.listByMainCategoryAndSubCategoryAndLastId(mainCategory, subCategory, lastId, limit);
    }

    /**
     * 搜索职业（按关键词）
     */
    public List<ProfessionDO> searchByKeyword(String keyword) {
        return professionDataService.searchByKeyword(keyword);
    }

    /**
     * 根据ID列表批量获取职业
     */
    public List<ProfessionDO> getByIds(List<Long> ids) {
        return professionDataService.getByIds(ids);
    }
}
