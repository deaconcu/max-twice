package com.twicemax.content.course;

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
 * 课程领域服务（revision 模型）。
 * <p>
 * 主体状态（course.state）只描述对外可见性：NEVER_PUBLISHED / PUBLISHED / BANNED。
 * 一次"申请→审核"的生命周期落在 {@link ContentRevisionDO}（status: SUBMITTED / PUBLISHED /
 * REJECTED / WITHDRAWN）。两者通过 current_revision_id（已发布版本）和 pending_revision_id
 * （审核中版本）连接。
 * <p>
 * Course 与 Role 的差异：
 * <ul>
 *   <li>course 有 parentCourseId / rootNodeId / subCourseCount 三个 role 没有的字段。</li>
 *   <li>parentCourseId 在 payload 中（允许审核时变更归属）；rootNodeId 在创建时定，不在 payload 中。</li>
 *   <li>subCourseCount 是父课程的派生计数，由 approve / ban / restore / delete 维护，不在 payload 中。</li>
 *   <li>已知 TODO：rootNode 的可见性级联（NEVER_PUBLISHED 课程下的 PUBLISHED rootNode 违反不变量），
 *       本次迁移保留现状，留待独立 PR 处理。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseDomainService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String CONTENT_TYPE = NewContentType.COURSE_VALUE;

    private final CourseDataService courseDataService;
    private final ContentRevisionDataService revisionDataService;
    private final SystemDomainService systemDomainService;

    // ========== Query 方法 ==========

    public CourseDO getById(Long id) {
        return courseDataService.getById(id);
    }

    public CourseDO validateAndGet(Long id) {
        return courseDataService.validateAndGet(id);
    }

    /**
     * 检查用户是否是课程创建者
     */
    public boolean isCreator(long courseId, long userId) {
        CourseDO course = courseDataService.validateAndGet(courseId);
        return course.getCreatorId().equals(userId);
    }

    /**
     * 用户/作者视角：按创建者分页查看自己的申请。
     * @param state 可为 null（默认排除 BANNED）；或 NEVER_PUBLISHED / PUBLISHED。
     */
    public List<CourseDO> listByCreator(long creatorId, Long lastId, int limit, NewContentState state) {
        return courseDataService.listByCreator(creatorId, lastId, limit,
                state != null ? state.value() : null);
    }

    // ========== 内容编排：payload 构建 ==========

    /**
     * 把用户提交的字段打包为 canonical JSON payload，并计算 hash。
     * payload 包含审核可变字段：name / description / icon / mainCategory / subCategory / parentCourseId。
     */
    private PayloadInfo buildPayload(String name, String description, String icon,
                                     int mainCategory, int subCategory, long parentCourseId) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("v", 1);
        root.put("name", name);
        root.put("description", description != null ? description : "");
        root.put("icon", icon != null ? icon : "");
        root.put("mainCategory", mainCategory);
        root.put("subCategory", subCategory);
        root.put("parentCourseId", parentCourseId);

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
     * 从 payload 反解审核字段，approve 时把它们刷回主表镜像。
     */
    private CourseContent parsePayload(String payload) {
        try {
            var node = objectMapper.readTree(payload);
            CourseContent c = new CourseContent();
            c.name = node.path("name").asText("");
            c.description = node.path("description").asText("");
            c.icon = node.path("icon").asText("");
            c.mainCategory = node.path("mainCategory").asInt(0);
            c.subCategory = node.path("subCategory").asInt(0);
            c.parentCourseId = node.path("parentCourseId").asLong(0L);
            return c;
        } catch (Exception e) {
            throw StatusCode.JSON_PROCESSING_ERROR.exception(e);
        }
    }

    private void validateContentFields(String name, int mainCategory, int subCategory) {
        ValidationUtils.requireNonBlank(name, "课程名称");
        systemDomainService.validateCourseCategory(mainCategory, subCategory);
    }

    // ========== Command 方法（revision 模型）==========

    /**
     * 用户创建课程申请。parentCourseId=0 表示主课程，>0 表示子课程。
     * 写入 course 主体（state=NEVER_PUBLISHED，name 等镜像字段先填用户提交值），
     * 同步写入 SUBMITTED revision，设置 pending_revision_id。
     * <p>
     * rootNodeId 由调用方在创建后回填（保留现状：course 申请时立刻创建一个 rootNode）。
     *
     * @return 新 course 的 id
     */
    @Transactional
    public Long create(long creatorId, String name, String description, String icon,
                       int mainCategory, int subCategory, long parentCourseId) {
        validateContentFields(name, mainCategory, subCategory);

        PayloadInfo payload = buildPayload(name, description, icon, mainCategory, subCategory, parentCourseId);

        CourseDO courseDO = new CourseDO();
        courseDO.setName(name);
        courseDO.setDescription(description != null ? description : "");
        courseDO.setIcon(icon != null ? icon : "");
        courseDO.setMainCategory(mainCategory);
        courseDO.setSubCategory(subCategory);
        courseDO.setCreatorId(creatorId);
        courseDO.setParentCourseId(parentCourseId);
        courseDO.setRootNodeId(0L); // 由调用方在创建 rootNode 后回填
        courseDO.setState(NewContentState.NEVER_PUBLISHED_VALUE);
        courseDataService.insert(courseDO);

        // 写入 SUBMITTED revision
        ContentRevisionDO revision = new ContentRevisionDO();
        revision.setContentType(CONTENT_TYPE);
        revision.setContentId(courseDO.getId());
        revision.setRevisionNo(1);
        revision.setStatus(RevisionStatus.SUBMITTED_VALUE);
        revision.setPayload(payload.canonical);
        revision.setHash(payload.hash);
        revision.setAuthorId(creatorId);
        revisionDataService.insert(revision);

        courseDataService.updatePending(courseDO.getId(), revision.getId());

        log.info("Course 创建申请: courseId={}, revisionId={}, creatorId={}, parentCourseId={}",
                courseDO.getId(), revision.getId(), creatorId, parentCourseId);
        return courseDO.getId();
    }

    /**
     * 由调用方（application 层 createCourse 流程）在创建 rootNode 后回填。
     */
    @Transactional
    public void bindRootNode(long courseId, long rootNodeId) {
        CourseDO course = courseDataService.validateAndGet(courseId);
        course.setRootNodeId(rootNodeId);
        courseDataService.update(course);
    }

    /**
     * 用户重新提交（被驳回 / 撤回后再申请）。
     * 要求当前没有 pending revision；写一条新的 SUBMITTED revision，与最近一次内容做 hash 去重。
     * parentCourseId 不允许在重提时变更（保留原值），避免子课程→主课程之类的越界变更。
     */
    @Transactional
    public Long resubmit(long courseId, long authorId, String name, String description,
                         String icon, int mainCategory, int subCategory) {
        validateContentFields(name, mainCategory, subCategory);

        CourseDO course = courseDataService.validateAndGet(courseId);
        if (NewContentState.BANNED_VALUE.equals(course.getState())) {
            throw StatusCode.INVALID_PARAMETER.exception("已封禁的课程不能提交");
        }
        if (course.getPendingRevisionId() != null) {
            throw StatusCode.INVALID_PARAMETER.exception("已有审核中的版本，请先撤回再提交");
        }
        if (!Objects.equals(course.getCreatorId(), authorId)) {
            throw StatusCode.INVALID_PARAMETER.exception("无权限操作他人申请");
        }

        long parentCourseId = course.getParentCourseId() != null ? course.getParentCourseId() : 0L;
        PayloadInfo payload = buildPayload(name, description, icon, mainCategory, subCategory, parentCourseId);

        ContentRevisionDO latest = revisionDataService.getLatest(CONTENT_TYPE, courseId);
        if (latest != null && Objects.equals(payload.hash, latest.getHash())) {
            throw StatusCode.INVALID_PARAMETER.exception("内容与最近一次版本相同，无需重复提交");
        }

        ContentRevisionDO revision = new ContentRevisionDO();
        revision.setContentType(CONTENT_TYPE);
        revision.setContentId(courseId);
        revision.setRevisionNo(revisionDataService.nextRevisionNo(CONTENT_TYPE, courseId));
        revision.setStatus(RevisionStatus.SUBMITTED_VALUE);
        revision.setPayload(payload.canonical);
        revision.setHash(payload.hash);
        revision.setAuthorId(authorId);
        revisionDataService.insert(revision);

        // 同步主表镜像（待审值）
        course.setName(name);
        course.setDescription(description != null ? description : "");
        course.setIcon(icon != null ? icon : "");
        course.setMainCategory(mainCategory);
        course.setSubCategory(subCategory);
        courseDataService.update(course);

        courseDataService.updatePending(courseId, revision.getId());

        log.info("Course 重新提交: courseId={}, revisionId={}, revisionNo={}",
                courseId, revision.getId(), revision.getRevisionNo());
        return revision.getId();
    }

    /**
     * 作者撤回审核中的版本：revision → WITHDRAWN，pending 清空。
     */
    @Transactional
    public void withdraw(long courseId, long authorId) {
        CourseDO course = courseDataService.validateAndGet(courseId);
        Long pendingId = course.getPendingRevisionId();
        if (pendingId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("当前没有审核中的版本");
        }
        if (!Objects.equals(course.getCreatorId(), authorId)) {
            throw StatusCode.INVALID_PARAMETER.exception("无权限操作他人申请");
        }
        ContentRevisionDO revision = revisionDataService.validateAndGet(pendingId);
        if (!RevisionStatus.SUBMITTED_VALUE.equals(revision.getStatus())) {
            throw StatusCode.INVALID_PARAMETER.exception("版本状态非 SUBMITTED，无法撤回");
        }

        revision.setStatus(RevisionStatus.WITHDRAWN_VALUE);
        revision.setReviewedAt(LocalDateTime.now());
        revisionDataService.updateStatus(revision);

        courseDataService.updatePending(courseId, null);
        log.info("Course 撤回申请: courseId={}, revisionId={}, authorId={}", courseId, pendingId, authorId);
    }

    /**
     * 审核通过：revision → PUBLISHED，course.state = PUBLISHED，主表镜像字段从 payload 刷新，
     * current_revision_id = revision.id，pending 清空。
     * <p>
     * 子课程数维护交由调用方（application 层）按 wasPublished 判断后处理（因为需要拿到旧 state）。
     *
     * @return 通过后从 payload 反解出的内容（含 parentCourseId），便于调用方维护子课程数
     */
    @Transactional
    public CourseContent approve(long courseId, long reviewerId) {
        CourseDO course = courseDataService.validateAndGet(courseId);
        Long pendingId = course.getPendingRevisionId();
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

        CourseContent content = parsePayload(revision.getPayload());
        courseDataService.approve(courseId, content.name, content.description, content.icon,
                content.mainCategory, content.subCategory, content.parentCourseId, revision.getId());

        log.info("Course 审核通过: courseId={}, revisionId={}, reviewerId={}", courseId, pendingId, reviewerId);
        return content;
    }

    /**
     * 审核驳回：revision → REJECTED（带 reason），pending 清空。主表镜像保留为最近待审值。
     */
    @Transactional
    public void reject(long courseId, String reason, long reviewerId) {
        CourseDO course = courseDataService.validateAndGet(courseId);
        Long pendingId = course.getPendingRevisionId();
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

        courseDataService.updatePending(courseId, null);
        log.info("Course 审核驳回: courseId={}, revisionId={}, reason={}", courseId, pendingId, reason);
    }

    /**
     * 封禁：course.state = BANNED；若存在 pending revision，连带标 REJECTED；不写新 revision。
     */
    @Transactional
    public void ban(long courseId, String reason, long operatorId) {
        CourseDO course = courseDataService.validateAndGet(courseId);
        if (NewContentState.BANNED_VALUE.equals(course.getState())) {
            throw StatusCode.INVALID_PARAMETER.exception("已是封禁状态");
        }

        Long pendingId = course.getPendingRevisionId();
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

        courseDataService.ban(courseId);
        log.info("Course 封禁: courseId={}, operatorId={}, reason={}", courseId, operatorId, reason);
    }

    /**
     * 解封：根据是否存在已发布版本恢复到 PUBLISHED 或 NEVER_PUBLISHED。
     * @return 解封后的状态，便于调用方决定是否维护父课程的子课程数
     */
    @Transactional
    public NewContentState restore(long courseId) {
        CourseDO course = courseDataService.validateAndGet(courseId);
        if (!NewContentState.BANNED_VALUE.equals(course.getState())) {
            throw StatusCode.INVALID_PARAMETER.exception("仅 BANNED 状态可解封");
        }
        NewContentState newState = course.getCurrentRevisionId() != null
                ? NewContentState.PUBLISHED
                : NewContentState.NEVER_PUBLISHED;
        courseDataService.updateState(courseId, newState.value());
        log.info("Course 解封: courseId={}, newState={}", courseId, newState);
        return newState;
    }

    /**
     * 管理员直接编辑（走 revision 留审计）：写入一条 status=PUBLISHED 的 revision，
     * authorId=reviewerId=管理员；同步刷新主表镜像和 current_revision_id。
     * 不影响 pending revision（如果用户提交在审，管理员先审/驳回再编辑）。
     */
    @Transactional
    public void edit(long courseId, long adminId, String name, String description,
                     String icon, int mainCategory, int subCategory) {
        validateContentFields(name, mainCategory, subCategory);

        CourseDO course = courseDataService.validateAndGet(courseId);
        if (NewContentState.BANNED_VALUE.equals(course.getState())) {
            throw StatusCode.INVALID_PARAMETER.exception("已封禁的课程无法编辑");
        }
        if (course.getPendingRevisionId() != null) {
            throw StatusCode.INVALID_PARAMETER.exception("存在审核中的版本，请先处理后再编辑");
        }

        long parentCourseId = course.getParentCourseId() != null ? course.getParentCourseId() : 0L;
        PayloadInfo payload = buildPayload(name, description, icon, mainCategory, subCategory, parentCourseId);

        ContentRevisionDO latest = revisionDataService.getLatest(CONTENT_TYPE, courseId);
        if (latest != null && Objects.equals(payload.hash, latest.getHash())) {
            throw StatusCode.INVALID_PARAMETER.exception("内容与最近一次版本相同，无需重复发布");
        }

        ContentRevisionDO revision = new ContentRevisionDO();
        revision.setContentType(CONTENT_TYPE);
        revision.setContentId(courseId);
        revision.setRevisionNo(revisionDataService.nextRevisionNo(CONTENT_TYPE, courseId));
        revision.setStatus(RevisionStatus.PUBLISHED_VALUE);
        revision.setPayload(payload.canonical);
        revision.setHash(payload.hash);
        revision.setAuthorId(adminId);
        revision.setReviewerId(adminId);
        revision.setReviewedAt(LocalDateTime.now());
        revisionDataService.insert(revision);

        courseDataService.approve(courseId, name, description != null ? description : "",
                icon != null ? icon : "", mainCategory, subCategory, parentCourseId, revision.getId());

        log.info("Course 管理员编辑: courseId={}, revisionId={}, adminId={}",
                courseId, revision.getId(), adminId);
    }

    /**
     * 删除课程（硬删除，与 role 软删除不同：course 表无 deleted_at 列）。
     */
    @Transactional
    public void deleteCourse(long courseId) {
        courseDataService.validateAndGet(courseId);
        courseDataService.delete(courseId);
        log.info("Course 删除成功: courseId={}", courseId);
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

    /**
     * 从 revision payload 反解出的内容字段。public 是为了 application 层在 approve 后
     * 拿到 parentCourseId 维护子课程数。
     */
    public static final class CourseContent {
        public String name;
        public String description;
        public String icon;
        public int mainCategory;
        public int subCategory;
        public long parentCourseId;
    }
}
