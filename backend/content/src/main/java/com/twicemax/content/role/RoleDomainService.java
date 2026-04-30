package com.twicemax.content.role;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.twicemax.content.shared.revision.ContentRevisionDO;
import com.twicemax.content.shared.revision.ContentRevisionDataService;
import com.twicemax.shared.common.utils.CanonicalJson;
import com.twicemax.shared.common.utils.ValidationUtils;
import com.twicemax.shared.domain.Enums.NewContentState;
import com.twicemax.shared.domain.Enums.NewContentType;
import com.twicemax.shared.domain.Enums.RevisionStatus;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.shared.infrastructure.config.SystemDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 角色领域服务（revision 模型）。
 * <p>
 * 主体状态（role.state）只描述对外可见性：NEVER_PUBLISHED / PUBLISHED / BANNED。
 * 一次"申请→审核"的生命周期落在 {@link ContentRevisionDO}（status: SUBMITTED / PUBLISHED /
 * REJECTED / WITHDRAWN）。两者通过 current_revision_id（已发布版本）和 pending_revision_id
 * （审核中版本）连接。
 * <p>
 * Role 与 Roadmap 的差异：
 * <ul>
 *   <li>role 没有草稿（无 draft_content）。用户每次申请/重提都直接 submit。</li>
 *   <li>主表 name/description/icon/skills/mainCategory/subCategory 是 current_revision.payload
 *       的镜像字段：approve 时刷新；NEVER_PUBLISHED 时承载用户最近一次提交的待审值，便于
 *       管理员在审核列表里直接看到内容。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleDomainService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String CONTENT_TYPE = NewContentType.ROLE_VALUE;

    private final RoleDataService roleDataService;
    private final ContentRevisionDataService revisionDataService;
    private final SystemDomainService systemDomainService;

    // ========== Query 方法 ==========

    public RoleDO getById(Long id) {
        return roleDataService.getById(id);
    }

    public RoleDO validateAndGet(Long id) {
        return roleDataService.validateAndGet(id);
    }

    public List<RoleDO> listByState(String state, Long lastId, int limit) {
        return roleDataService.listByState(state, lastId, limit);
    }

    public List<RoleDO> listByMainCategoryAndLastId(int mainCategory, Long lastId, int limit) {
        return roleDataService.listByMainCategoryAndLastId(mainCategory, lastId, limit);
    }

    public List<RoleDO> listBySubCategoryAndLastId(int subCategory, Long lastId, int limit) {
        return roleDataService.listBySubCategoryAndLastId(subCategory, lastId, limit);
    }

    public List<RoleDO> listByMainCategoryAndSubCategoryAndLastId(int mainCategory, int subCategory, Long lastId, int limit) {
        return roleDataService.listByMainCategoryAndSubCategoryAndLastId(mainCategory, subCategory, lastId, limit);
    }

    public List<RoleDO> searchByKeyword(String keyword) {
        return roleDataService.searchByKeyword(keyword);
    }

    public List<RoleDO> searchByName(String name, Long lastId, int limit) {
        return roleDataService.searchByName(name, lastId, limit);
    }

    /**
     * 用户/作者视角：按创建者分页查看自己的申请。
     * @param state 可为 null（默认排除 BANNED）；或 NEVER_PUBLISHED / PUBLISHED。
     */
    public List<RoleDO> listByCreator(long creatorId, Long lastId, int limit, NewContentState state) {
        return roleDataService.listByCreator(creatorId, lastId, limit, state != null ? state.value() : null);
    }

    public List<RoleDO> getByIds(List<Long> ids) {
        return roleDataService.getByIds(ids);
    }

    // ========== 内容编排：payload 构建 ==========

    /**
     * 把用户提交的字段打包为 canonical JSON payload，并计算 hash。
     */
    private PayloadInfo buildPayload(String name, String description, String icon, String skills,
                                     int mainCategory, int subCategory) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("v", 1);
        root.put("name", name);
        root.put("description", description != null ? description : "");
        root.put("icon", icon != null ? icon : "");
        root.put("skills", skills != null ? skills : "");
        root.put("mainCategory", mainCategory);
        root.put("subCategory", subCategory);

        String raw;
        try {
            raw = objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw StatusCode.JSON_PROCESSING_ERROR.exception(e);
        }
        String canonical = CanonicalJson.canonicalize(raw);
        String hash = CanonicalJson.hash(raw);
        return new PayloadInfo(canonical, hash);
    }

    /**
     * 从 payload 反解 6 个内容字段，approve 时把它们刷回主表镜像。
     */
    private RoleContent parsePayload(String payload) {
        try {
            var node = objectMapper.readTree(payload);
            RoleContent c = new RoleContent();
            c.name = node.path("name").asText("");
            c.description = node.path("description").asText("");
            c.icon = node.path("icon").asText("");
            c.skills = node.path("skills").asText("");
            c.mainCategory = node.path("mainCategory").asInt(0);
            c.subCategory = node.path("subCategory").asInt(0);
            return c;
        } catch (Exception e) {
            throw StatusCode.JSON_PROCESSING_ERROR.exception(e);
        }
    }

    private void validateContentFields(String name, int mainCategory, int subCategory) {
        ValidationUtils.requireNonBlank(name, "角色名称");
        systemDomainService.validateRoleCategory(mainCategory, subCategory);
    }

    // ========== Command 方法（revision 模型）==========

    /**
     * 用户创建角色申请：写入 role 主体（state=NEVER_PUBLISHED，name 等镜像字段先填用户提交值），
     * 同步写入 SUBMITTED revision，设置 pending_revision_id。
     * <p>
     * 创建时不收集 icon，icon 由后续编辑/审核流程补充。
     *
     * @return 新 role 的 id
     */
    @Transactional
    public Long create(long creatorId, String name, String description, String skills,
                       int mainCategory, int subCategory) {
        validateContentFields(name, mainCategory, subCategory);

        PayloadInfo payload = buildPayload(name, description, "", skills, mainCategory, subCategory);

        RoleDO roleDO = new RoleDO();
        roleDO.setName(name);
        roleDO.setDescription(description != null ? description : "");
        roleDO.setIcon("");
        roleDO.setSkills(skills != null ? skills : "");
        roleDO.setMainCategory(mainCategory);
        roleDO.setSubCategory(subCategory);
        roleDO.setCreatorId(creatorId);
        roleDO.setState(NewContentState.NEVER_PUBLISHED_VALUE);
        roleDataService.insert(roleDO);

        // 写入 SUBMITTED revision
        ContentRevisionDO revision = new ContentRevisionDO();
        revision.setContentType(CONTENT_TYPE);
        revision.setContentId(roleDO.getId());
        revision.setRevisionNo(1);
        revision.setStatus(RevisionStatus.SUBMITTED_VALUE);
        revision.setPayload(payload.canonical);
        revision.setHash(payload.hash);
        revision.setAuthorId(creatorId);
        revisionDataService.insert(revision);

        // 设置 pending_revision_id
        roleDataService.updatePending(roleDO.getId(), revision.getId());

        log.info("Role 创建申请: roleId={}, revisionId={}, creatorId={}",
                roleDO.getId(), revision.getId(), creatorId);
        return roleDO.getId();
    }

    /**
     * 用户重新提交（被驳回 / 撤回后再申请）。
     * 要求当前没有 pending revision；写一条新的 SUBMITTED revision，与最近一次内容做 hash 去重。
     */
    @Transactional
    public Long resubmit(long roleId, long authorId, String name, String description, String skills,
                         String icon, int mainCategory, int subCategory) {
        validateContentFields(name, mainCategory, subCategory);

        RoleDO role = roleDataService.validateAndGet(roleId);
        if (NewContentState.BANNED_VALUE.equals(role.getState())) {
            throw StatusCode.INVALID_PARAMETER.exception("已封禁的角色不能提交");
        }
        if (role.getPendingRevisionId() != null) {
            throw StatusCode.INVALID_PARAMETER.exception("已有审核中的版本，请先撤回再提交");
        }
        if (!Objects.equals(role.getCreatorId(), authorId)) {
            throw StatusCode.INVALID_PARAMETER.exception("无权限操作他人申请");
        }

        PayloadInfo payload = buildPayload(name, description, icon, skills, mainCategory, subCategory);

        ContentRevisionDO latest = revisionDataService.getLatest(CONTENT_TYPE, roleId);
        if (latest != null && Objects.equals(payload.hash, latest.getHash())) {
            throw StatusCode.INVALID_PARAMETER.exception("内容与最近一次版本相同，无需重复提交");
        }

        ContentRevisionDO revision = new ContentRevisionDO();
        revision.setContentType(CONTENT_TYPE);
        revision.setContentId(roleId);
        revision.setRevisionNo(revisionDataService.nextRevisionNo(CONTENT_TYPE, roleId));
        revision.setStatus(RevisionStatus.SUBMITTED_VALUE);
        revision.setPayload(payload.canonical);
        revision.setHash(payload.hash);
        revision.setAuthorId(authorId);
        revisionDataService.insert(revision);

        // 同步主表镜像（待审值）
        role.setName(name);
        role.setDescription(description != null ? description : "");
        role.setIcon(icon != null ? icon : "");
        role.setSkills(skills != null ? skills : "");
        role.setMainCategory(mainCategory);
        role.setSubCategory(subCategory);
        roleDataService.update(role);

        roleDataService.updatePending(roleId, revision.getId());

        log.info("Role 重新提交: roleId={}, revisionId={}, revisionNo={}",
                roleId, revision.getId(), revision.getRevisionNo());
        return revision.getId();
    }

    /**
     * 作者撤回审核中的版本：revision → WITHDRAWN，pending 清空。
     */
    @Transactional
    public void withdraw(long roleId, long authorId) {
        RoleDO role = roleDataService.validateAndGet(roleId);
        Long pendingId = role.getPendingRevisionId();
        if (pendingId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("当前没有审核中的版本");
        }
        if (!Objects.equals(role.getCreatorId(), authorId)) {
            throw StatusCode.INVALID_PARAMETER.exception("无权限操作他人申请");
        }
        ContentRevisionDO revision = revisionDataService.validateAndGet(pendingId);
        if (!RevisionStatus.SUBMITTED_VALUE.equals(revision.getStatus())) {
            throw StatusCode.INVALID_PARAMETER.exception("版本状态非 SUBMITTED，无法撤回");
        }

        revision.setStatus(RevisionStatus.WITHDRAWN_VALUE);
        revision.setReviewedAt(LocalDateTime.now());
        revisionDataService.updateStatus(revision);

        roleDataService.updatePending(roleId, null);
        log.info("Role 撤回申请: roleId={}, revisionId={}, authorId={}", roleId, pendingId, authorId);
    }

    /**
     * 审核通过：revision → PUBLISHED，role.state = PUBLISHED，
     * 主表镜像字段从 payload 刷新，current_revision_id = revision.id，pending 清空。
     */
    @Transactional
    public void approve(long roleId, long reviewerId) {
        RoleDO role = roleDataService.validateAndGet(roleId);
        Long pendingId = role.getPendingRevisionId();
        if (pendingId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("当前没有审核中的版本");
        }
        ContentRevisionDO revision = revisionDataService.validateAndGet(pendingId);
        if (!RevisionStatus.SUBMITTED_VALUE.equals(revision.getStatus())) {
            throw StatusCode.INVALID_PARAMETER.exception("版本状态非 SUBMITTED，无法通过");
        }

        revision.setStatus(RevisionStatus.PUBLISHED_VALUE);
        revision.setReviewerId(reviewerId);
        revision.setReviewedAt(LocalDateTime.now());
        revisionDataService.updateStatus(revision);

        RoleContent content = parsePayload(revision.getPayload());
        roleDataService.approve(roleId, content.name, content.description, content.icon, content.skills,
                content.mainCategory, content.subCategory, revision.getId());

        log.info("Role 审核通过: roleId={}, revisionId={}, reviewerId={}", roleId, pendingId, reviewerId);
    }

    /**
     * 审核驳回：revision → REJECTED（带 reason），pending 清空。主表镜像保留为最近待审值，
     * 用户可在"我的申请"看到驳回原因后修改重提。
     */
    @Transactional
    public void reject(long roleId, String reason, long reviewerId) {
        RoleDO role = roleDataService.validateAndGet(roleId);
        Long pendingId = role.getPendingRevisionId();
        if (pendingId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("当前没有审核中的版本");
        }
        ContentRevisionDO revision = revisionDataService.validateAndGet(pendingId);
        if (!RevisionStatus.SUBMITTED_VALUE.equals(revision.getStatus())) {
            throw StatusCode.INVALID_PARAMETER.exception("版本状态非 SUBMITTED，无法驳回");
        }

        revision.setStatus(RevisionStatus.REJECTED_VALUE);
        revision.setRejectReason(reason);
        revision.setReviewerId(reviewerId);
        revision.setReviewedAt(LocalDateTime.now());
        revisionDataService.updateStatus(revision);

        roleDataService.updatePending(roleId, null);
        log.info("Role 审核驳回: roleId={}, revisionId={}, reason={}", roleId, pendingId, reason);
    }

    /**
     * 封禁：role.state = BANNED；若存在 pending revision，连带标 REJECTED；不写新 revision。
     */
    @Transactional
    public void ban(long roleId, String reason, long operatorId) {
        RoleDO role = roleDataService.validateAndGet(roleId);
        if (NewContentState.BANNED_VALUE.equals(role.getState())) {
            throw StatusCode.INVALID_PARAMETER.exception("已是封禁状态");
        }

        Long pendingId = role.getPendingRevisionId();
        if (pendingId != null) {
            ContentRevisionDO revision = revisionDataService.validateAndGet(pendingId);
            if (RevisionStatus.SUBMITTED_VALUE.equals(revision.getStatus())) {
                revision.setStatus(RevisionStatus.REJECTED_VALUE);
                revision.setRejectReason(reason);
                revision.setReviewerId(operatorId);
                revision.setReviewedAt(LocalDateTime.now());
                revisionDataService.updateStatus(revision);
            }
        }

        roleDataService.ban(roleId);
        log.info("Role 封禁: roleId={}, operatorId={}, reason={}", roleId, operatorId, reason);
    }

    /**
     * 解封：根据是否存在已发布版本恢复到 PUBLISHED 或 NEVER_PUBLISHED。
     */
    @Transactional
    public void restore(long roleId) {
        RoleDO role = roleDataService.validateAndGet(roleId);
        if (!NewContentState.BANNED_VALUE.equals(role.getState())) {
            throw StatusCode.INVALID_PARAMETER.exception("仅 BANNED 状态可解封");
        }
        String newState = role.getCurrentRevisionId() != null
                ? NewContentState.PUBLISHED_VALUE
                : NewContentState.NEVER_PUBLISHED_VALUE;
        roleDataService.updateState(roleId, newState);
        log.info("Role 解封: roleId={}, newState={}", roleId, newState);
    }

    /**
     * 管理员直接编辑（走 revision 留审计）：写入一条 status=PUBLISHED 的 revision，
     * authorId=reviewerId=管理员；同步刷新主表镜像和 current_revision_id。
     * 不影响 pending revision（如果用户提交在审，管理员先审/驳回再编辑）。
     */
    @Transactional
    public void edit(long roleId, long adminId, String name, String description, String skills,
                          String icon, int mainCategory, int subCategory) {
        validateContentFields(name, mainCategory, subCategory);

        RoleDO role = roleDataService.validateAndGet(roleId);
        if (NewContentState.BANNED_VALUE.equals(role.getState())) {
            throw StatusCode.INVALID_PARAMETER.exception("已封禁的角色无法编辑");
        }
        if (role.getPendingRevisionId() != null) {
            throw StatusCode.INVALID_PARAMETER.exception("存在审核中的版本，请先处理后再编辑");
        }

        PayloadInfo payload = buildPayload(name, description, icon, skills, mainCategory, subCategory);

        ContentRevisionDO latest = revisionDataService.getLatest(CONTENT_TYPE, roleId);
        if (latest != null && Objects.equals(payload.hash, latest.getHash())) {
            throw StatusCode.INVALID_PARAMETER.exception("内容与最近一次版本相同，无需重复发布");
        }

        ContentRevisionDO revision = new ContentRevisionDO();
        revision.setContentType(CONTENT_TYPE);
        revision.setContentId(roleId);
        revision.setRevisionNo(revisionDataService.nextRevisionNo(CONTENT_TYPE, roleId));
        revision.setStatus(RevisionStatus.PUBLISHED_VALUE);
        revision.setPayload(payload.canonical);
        revision.setHash(payload.hash);
        revision.setAuthorId(adminId);
        revision.setReviewerId(adminId);
        revision.setReviewedAt(LocalDateTime.now());
        revisionDataService.insert(revision);

        // 主表 state 变为 PUBLISHED（如果原本是 NEVER_PUBLISHED），并刷新镜像
        roleDataService.approve(roleId, name, description != null ? description : "",
                icon != null ? icon : "", skills != null ? skills : "",
                mainCategory, subCategory, revision.getId());

        log.info("Role 管理员编辑: roleId={}, revisionId={}, adminId={}", roleId, revision.getId(), adminId);
    }

    @Transactional
    public void delete(long roleId) {
        RoleDO role = roleDataService.getById(roleId);
        if (role == null) {
            throw StatusCode.ROLE_NOT_FOUND.exception();
        }
        roleDataService.delete(roleId);
        log.info("Role 软删除: roleId={}", roleId);
    }

    // ========== 辅助类型 ==========

    private static final class PayloadInfo {
        final String canonical;
        final String hash;
        PayloadInfo(String canonical, String hash) {
            this.canonical = canonical;
            this.hash = hash;
        }
    }

    private static final class RoleContent {
        String name;
        String description;
        String icon;
        String skills;
        int mainCategory;
        int subCategory;
    }
}
