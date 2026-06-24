package com.twicemax.application.service;

import com.twicemax.analytics.dto.ContentStatsDTO;
import com.twicemax.analytics.stats.dataservice.ContentStatsDataService;
import com.twicemax.analytics.stats.mapper.ContentStatsDO;
import com.twicemax.analytics.stats.service.ContentStatsDomainService;
import com.twicemax.application.converter.RoadmapConverter;
import com.twicemax.application.converter.RoleConverter;
import com.twicemax.application.converter.UserConverter;
import com.twicemax.application.dto.response.KeysetPageResponse;
import com.twicemax.application.dto.response.roadmap.RoadmapAdminDTO;
import com.twicemax.application.dto.response.roadmap.RoadmapBriefDTO;
import com.twicemax.application.dto.response.roadmap.RoadmapDetailDTO;
import com.twicemax.application.dto.response.roadmap.RoadmapEditDTO;
import com.twicemax.application.dto.response.roadmap.RoadmapSaveResult;
import com.twicemax.application.dto.response.roadmap.RoadmapSummaryDTO;
import com.twicemax.application.dto.response.roadmap.RoadmapWithStatusDTO;
import com.twicemax.application.dto.v2.Cursor;
import com.twicemax.content.course.CourseDO;
import com.twicemax.content.course.CourseDataService;
import com.twicemax.content.node.NodeDO;
import com.twicemax.content.node.NodeDataService;
import com.twicemax.content.roadmap.RoadmapDO;
import com.twicemax.content.roadmap.RoadmapDataService;
import com.twicemax.content.roadmap.RoadmapDomainService;
import com.twicemax.content.role.RoleDO;
import com.twicemax.content.role.RoleDataService;
import com.twicemax.content.shared.revision.ContentRevisionDO;
import com.twicemax.content.shared.revision.ContentRevisionDataService;
import com.twicemax.interaction.upvote.UpvoteDomainService;
import com.twicemax.learning.enrollment.UserLearningDO;
import com.twicemax.learning.enrollment.UserLearningDomainService;
import com.twicemax.shared.common.constants.CommonConstants;
import com.twicemax.shared.domain.Enums.ContentType;
import com.twicemax.shared.domain.Enums.NewContentType;
import com.twicemax.shared.domain.Enums.RevisionStatus;
import com.twicemax.shared.domain.Enums.NewContentState;
import com.twicemax.shared.domain.Enums.UserRole;
import com.twicemax.shared.domain.event.content.lifecycle.RoadmapApprovedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.RoadmapBannedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.RoadmapRejectedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.RoadmapRestoredEvent;
import com.twicemax.shared.domain.event.content.lifecycle.RoadmapWithdrawnEvent;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.user.profile.UserDO;
import com.twicemax.user.profile.UserDataService;
import com.twicemax.user.profile.UserDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 路线图应用服务（revision 模型）。
 * <p>
 * 主体状态（roadmap.state）只有 NEVER_PUBLISHED / PUBLISHED / BANNED，描述对外可见性；
 * 一次"提交→审核"的生命周期落到 content_revision（status: SUBMITTED / PUBLISHED /
 * REJECTED / WITHDRAWN）。本服务负责跨域协调、DTO 转换、事件发布。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoadmapService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String CONTENT_TYPE = NewContentType.ROADMAP_VALUE;
    private static final int DEFAULT_ADMIN_PAGE_SIZE = 20;

    private final RoadmapDomainService domainService;
    private final RoadmapDataService roadmapDataService;
    private final ContentRevisionDataService revisionDataService;
    private final NodeDataService nodeDataService;
    private final CourseDataService courseDataService;
    private final UserDataService userDataService;
    private final UserDomainService userDomainService;
    private final UserLearningDomainService userLearningDomainService;
    private final RoleDataService roleDataService;
    private final UpvoteDomainService upvoteDomainService;
    private final ContentStatsDomainService contentStatsDomainService;
    private final ContentStatsDataService contentStatsDataService;
    private final BookmarkService bookmarkService;
    private final LearningProgressService learningProgressService;
    private final ApplicationEventPublisher eventPublisher;
    private final RoadmapConverter roadmapConverter;
    private final UserConverter userConverter;
    private final RoleConverter roleConverter;
    private final ContentVisibilityService contentVisibilityService;

    // ========== DTO转换方法 ==========

    public RoadmapSummaryDTO toSummaryDTO(RoadmapDO roadmapDO) {
        return roadmapConverter.toSummaryDTO(roadmapDO);
    }

    public List<RoadmapSummaryDTO> toSummaryDTO(List<RoadmapDO> roadmapDOList) {
        return roadmapConverter.toSummaryDTO(roadmapDOList);
    }

    public RoadmapBriefDTO toBriefDTO(RoadmapDO roadmapDO) {
        if (roadmapDO == null) {
            return null;
        }
        RoadmapBriefDTO dto = new RoadmapBriefDTO();
        dto.setId(roadmapDO.getId());
        dto.setNodeCount(roadmapDO.getNodeCount());
        if (roadmapDO.getRoleId() != null) {
            RoleDO role = roleDataService.getById(roadmapDO.getRoleId());
            if (role != null) {
                dto.setRoleName(role.getName());
            }
        }
        return dto;
    }

    public List<RoadmapBriefDTO> toBriefDTO(List<RoadmapDO> roadmapDOList) {
        if (roadmapDOList == null || roadmapDOList.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> roleIds = roadmapDOList.stream()
            .map(RoadmapDO::getRoleId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        Map<Long, String> roleNameMap = roleIds.isEmpty()
            ? Map.of()
            : roleDataService.getByIds(roleIds).stream()
                .collect(Collectors.toMap(RoleDO::getId, RoleDO::getName));

        return roadmapDOList.stream().map(roadmap -> {
            RoadmapBriefDTO dto = new RoadmapBriefDTO();
            dto.setId(roadmap.getId());
            dto.setNodeCount(roadmap.getNodeCount());
            if (roadmap.getRoleId() != null) {
                dto.setRoleName(roleNameMap.get(roadmap.getRoleId()));
            }
            return dto;
        }).collect(Collectors.toList());
    }

    public List<RoadmapDetailDTO> toDetailDTO(List<RoadmapDO> roadmapDOList) {
        if (roadmapDOList == null || roadmapDOList.isEmpty()) {
            return new ArrayList<>();
        }
        List<RoadmapDetailDTO> dtoList = roadmapConverter.toDetailDTO(roadmapDOList);

        Set<Long> roleIds = roadmapDOList.stream()
            .map(RoadmapDO::getRoleId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (!roleIds.isEmpty()) {
            List<RoleDO> roles = roleDataService.getByIds(new ArrayList<>(roleIds));
            Map<Long, RoleDO> roleMap = roles.stream()
                .collect(Collectors.toMap(RoleDO::getId, p -> p));
            for (RoadmapDetailDTO dto : dtoList) {
                if (dto.getRoleId() != null) {
                    RoleDO role = roleMap.get(dto.getRoleId());
                    if (role != null) {
                        dto.setRole(roleConverter.toBriefDTO(role));
                    }
                }
            }
        }
        return dtoList;
    }

    public RoadmapWithStatusDTO toRoadmapWithStatus(RoadmapDO roadmapDO, long userId) {
        if (roadmapDO == null) return null;

        RoadmapWithStatusDTO dto = roadmapConverter.toWithStatusDTO(roadmapDO);

        if (roadmapDO.getCreatorId() != null) {
            dto.setCreator(userConverter.toBriefDTO(userDataService.getById(roadmapDO.getCreatorId())));
        }
        if (roadmapDO.getRoleId() != null) {
            RoleDO roleDO = roleDataService.getById(roadmapDO.getRoleId());
            dto.setRole(roleConverter.toBriefDTO(roleDO));
        }

        dto.setLiked(upvoteDomainService.hasUpvoted(roadmapDO.getId(), ContentType.roadmap.value(), userId));
        dto.setLearning(userLearningDomainService.isLearning(userId, ContentType.roadmap, roadmapDO.getId()));
        dto.setBookmarked(bookmarkService.isBookmarked(userId, roadmapDO.getId(), ContentType.roadmap));

        ContentStatsDTO stats = contentStatsDomainService.getContentStats(ContentType.roadmap, roadmapDO.getId());
        dto.setLikeCount(stats.getLikeCount() != null ? stats.getLikeCount() : 0);
        dto.setCommentCount(stats.getCommentCount() != null ? stats.getCommentCount() : 0);
        dto.setLearnerCount(stats.getInProgressUserCount() != null ? stats.getInProgressUserCount() : 0);

        if (roadmapDO.getContent() != null) {
            dto.setContent(enrichContent(roadmapDO.getContent()));
        }
        return dto;
    }

    /**
     * 统计路线图内容中的 c/n 叶子节点数量
     */
    public Integer countNodesInContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return 0;
        }
        try {
            return domainService.countLeafBindings(content);
        } catch (Exception e) {
            log.error("路线图服务 节点数量统计失败", e);
            return 0;
        }
    }

    private List<RoadmapWithStatusDTO> toRoadmapWithFullInfo(List<RoadmapDO> roadmapList, long userId, Long roleId) {
        List<RoadmapWithStatusDTO> dtoList = roadmapConverter.toWithStatusDTO(roadmapList);

        if (!dtoList.isEmpty()) {
            List<Long> roadmapIds = dtoList.stream().map(RoadmapSummaryDTO::getId).collect(Collectors.toList());

            Set<Long> creatorIds = roadmapList.stream()
                .map(RoadmapDO::getCreatorId)
                .collect(Collectors.toSet());
            Map<Long, UserDO> creatorMap = userDataService.getMapByIds(creatorIds);

            RoleDO roleDO = roleId != null ? roleDataService.getById(roleId) : null;
            Set<Long> upvotedIds = upvoteDomainService.getUpvotedIds(roadmapIds, ContentType.roadmap.value(), userId);
            Set<Long> learningIds = getLearningIds(userId, roadmapIds);
            List<Long> bookmarkedIds = bookmarkService.getBookmarkedIds(userId, roadmapIds, ContentType.roadmap);
            Set<Long> bookmarkedSet = new HashSet<>(bookmarkedIds);

            Map<Long, ContentStatsDTO> statsMap = contentStatsDomainService.batchGetContentStats(ContentType.roadmap, roadmapIds);

            Map<Long, RoadmapDO> roadmapDOMap = roadmapList.stream()
                .collect(Collectors.toMap(RoadmapDO::getId, r -> r));

            for (RoadmapWithStatusDTO dto : dtoList) {
                RoadmapDO roadmapDO = roadmapDOMap.get(dto.getId());
                UserDO creator = creatorMap.get(roadmapDO.getCreatorId());

                dto.setRole(roleDO != null ? roleConverter.toBriefDTO(roleDO) : null);
                dto.setCreator(creator != null ? userConverter.toBriefDTO(creator) : null);
                dto.setLiked(upvotedIds.contains(dto.getId()));
                dto.setLearning(learningIds.contains(dto.getId()));
                dto.setBookmarked(bookmarkedSet.contains(dto.getId()));

                ContentStatsDTO stats = statsMap.get(dto.getId());
                if (stats != null) {
                    dto.setLikeCount(stats.getLikeCount() != null ? stats.getLikeCount() : 0);
                    dto.setCommentCount(stats.getCommentCount() != null ? stats.getCommentCount() : 0);
                    dto.setLearnerCount(stats.getInProgressUserCount() != null ? stats.getInProgressUserCount() : 0);
                } else {
                    dto.setLikeCount(0);
                    dto.setCommentCount(0);
                    dto.setLearnerCount(0);
                }
                dto.setContent(null);
            }
        }
        return dtoList;
    }

    // ========== 公共查询 ==========

    public List<RoadmapSummaryDTO> getRoadmapsByRolePublic(Long roleId, Long lastId, Integer pageSize) {
        validateRoleId(roleId);
        int limit = normalizePageSize(pageSize);
        List<RoadmapDO> roadmapList = domainService.getRoadmapsByRolePublic(roleId, lastId, limit);
        return toSummaryDTO(roadmapList);
    }

    public RoadmapSummaryDTO getById(long id) {
        validateRoadmapId(id);
        RoadmapDO roadmapDO = roadmapDataService.getById(id);
        return roadmapDO == null ? null : toSummaryDTO(roadmapDO);
    }

    public RoadmapWithStatusDTO getById(long id, long userId) {
        validateRoadmapId(id);
        validateUserId(userId);
        RoadmapDO roadmapDO = roadmapDataService.getById(id);
        if (roadmapDO == null) {
            return null;
        }
        contentVisibilityService.validateVisibility(ContentType.roadmap, id, userId);
        return toRoadmapWithStatus(roadmapDO, userId);
    }

    /**
     * c/n 节点 label 用数据库中的课程/节点名称回填，输出可供前端渲染的 JSON。
     */
    public String enrichContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        try {
            RoadmapDomainService.BoundIds bound = domainService.collectBoundIds(content);

            Map<Long, String> courseNames = bound.getCourseIds().isEmpty()
                ? Map.of()
                : courseDataService.getMapByIds(bound.getCourseIds()).entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));

            Map<Long, String> nodeNames = bound.getNodeIds().isEmpty()
                ? Map.of()
                : nodeDataService.getMapByIds(bound.getNodeIds()).entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));

            return domainService.enrichLabels(content, courseNames, nodeNames);
        } catch (Exception e) {
            log.error("路线图内容回填失败", e);
            throw StatusCode.JSON_PROCESSING_ERROR.exception(e);
        }
    }

    public List<RoadmapWithStatusDTO> getRoadmapsByRole(Long roleId, String cursor, String sortBy, Integer pageSize, UserDO currentUser) {
        validateRoleId(roleId);
        if (sortBy == null || sortBy.isEmpty() || (!sortBy.equals("latest") && !sortBy.equals("score"))) {
            sortBy = "score";
        }
        int limit = normalizePageSize(pageSize);
        List<RoadmapDO> roadmapList = domainService.getRoadmapsByRole(
            roleId, Cursor.decode(cursor).id(), limit, sortBy);
        return toRoadmapWithFullInfo(roadmapList, currentUser.getId(), roleId);
    }

    /**
     * 用户创建的路线图列表。state 为 RoadmapState 字符串枚举值，可为 null（默认排除 BANNED）。
     */
    public List<RoadmapDetailDTO> getUserRoadmaps(Long userId, String cursor, NewContentState state, Integer pageSize) {
        validateUserId(userId);
        int limit = normalizePageSize(pageSize);
        List<RoadmapDO> roadmapList = domainService.getUserRoadmaps(
            userId, Cursor.decode(cursor).id(), limit, state);
        return toDetailDTO(roadmapList);
    }

    public List<RoadmapWithStatusDTO> getRoadmapsByIdsWithStatus(List<Long> roadmapIds, Long userId) {
        if (roadmapIds == null || roadmapIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<RoadmapDO> roadmapList = domainService.getRoadmapsByIds(roadmapIds);
        return toRoadmapWithFullInfo(roadmapList, userId, null);
    }

    /**
     * 编辑页接口：作者或 admin 可调。
     * 返回 editable.content（已回填 label）+ pending + lastReject。
     */
    public RoadmapEditDTO getEditView(long roadmapId, UserDO currentUser) {
        validateRoadmapId(roadmapId);
        RoadmapDO roadmap = roadmapDataService.validateAndGet(roadmapId);

        if (!roadmap.getCreatorId().equals(currentUser.getId()) && !currentUser.hasRole(UserRole.ADMIN)) {
            throw StatusCode.PERMISSION_DENIED.exception();
        }

        RoadmapEditDTO dto = new RoadmapEditDTO();
        dto.setId(roadmap.getId());
        dto.setRoleId(roadmap.getRoleId());
        dto.setDescription(roadmap.getDescription());
        dto.setState(roadmap.getState());

        // 决定 editable.content 来源
        String rawContent;
        String source;
        LocalDateTime updatedAt;
        if (roadmap.getDraftContent() != null && !roadmap.getDraftContent().isEmpty()) {
            rawContent = roadmap.getDraftContent();
            source = "DRAFT";
            updatedAt = roadmap.getDraftUpdatedAt();
        } else if (roadmap.getContent() != null && !roadmap.getContent().isEmpty()) {
            rawContent = roadmap.getContent();
            source = "CURRENT";
            updatedAt = roadmap.getUpdatedAt();
        } else {
            rawContent = null;
            source = "EMPTY";
            updatedAt = null;
        }
        dto.setContent(rawContent != null ? enrichContent(rawContent) : null);
        dto.setContentSource(source);
        dto.setContentUpdatedAt(updatedAt != null ? updatedAt.format(ISO) : null);

        // pending
        if (roadmap.getPendingRevisionId() != null) {
            ContentRevisionDO pending = revisionDataService.getById(roadmap.getPendingRevisionId());
            if (pending != null) {
                RoadmapEditDTO.PendingInfo info = new RoadmapEditDTO.PendingInfo();
                info.setRevisionId(pending.getId());
                info.setRevisionNo(pending.getRevisionNo());
                info.setSubmittedAt(pending.getCreatedAt() != null ? pending.getCreatedAt().format(ISO) : null);
                dto.setPending(info);
            }
        }

        // lastReject：最近一次 revision 若是 REJECTED 才填
        ContentRevisionDO latest = revisionDataService.getLatest(CONTENT_TYPE, roadmapId);
        if (latest != null && RevisionStatus.REJECTED_VALUE.equals(latest.getStatus())) {
            RoadmapEditDTO.LastRejectInfo info = new RoadmapEditDTO.LastRejectInfo();
            info.setRevisionId(latest.getId());
            info.setRevisionNo(latest.getRevisionNo());
            info.setReason(latest.getRejectReason());
            info.setReviewedAt(latest.getReviewedAt() != null ? latest.getReviewedAt().format(ISO) : null);
            dto.setLastReject(info);
        }

        return dto;
    }

    // ========== 写入命令 ==========

    /**
     * 创建路线图。
     * @param submit true → 创建后立刻提交审核（SUBMITTED revision 一并产生）；false → 仅保存草稿
     */
    @Transactional
    public RoadmapSaveResult createRoadmap(Long roleId, String content, String description,
                                           long userId, boolean submit) {
        validateRoleId(roleId);
        validateContent(content);
        validateUserId(userId);

        roleDataService.validateExists(roleId);
        userDataService.validateExists(userId);
        contentVisibilityService.validateCanCreateOn(ContentType.role, roleId);

        // 校验内容并清洗（剥离 c/n label）
        String cleanedContent = domainService.validateAndStrip(content);

        // 引用完整性：草稿允许失效，提交时不允许
        RoadmapSaveResult.InvalidReferences invalid = findMissingReferences(cleanedContent, submit);

        // 1) 先建草稿主体
        Long roadmapId = domainService.createDraft(roleId, cleanedContent, description, userId);

        // 2) 如果要提交，紧接着 submit
        if (submit) {
            domainService.submit(roadmapId, cleanedContent, userId);
        }

        return new RoadmapSaveResult(roadmapId, invalid);
    }

    /**
     * 仅 saveDraft（不产生 revision）。提交审核请走 submit 端点。
     */
    @Transactional
    public RoadmapSaveResult updateRoadmap(Long id, String content, String description, UserDO operator) {
        validateRoadmapId(id);
        validateContent(content);

        RoadmapDO roadmap = roadmapDataService.validateAndGet(id);
        if (!roadmap.getCreatorId().equals(operator.getId()) && !operator.hasRole(UserRole.ADMIN)) {
            throw StatusCode.PERMISSION_DENIED.exception();
        }
        if (NewContentState.BANNED_VALUE.equals(roadmap.getState())) {
            throw StatusCode.INVALID_PARAMETER.exception("已封禁的路线图不能修改");
        }

        String cleanedContent = domainService.validateAndStrip(content);
        // 草稿允许引用失效（仅返回失效列表，不抛异常）
        RoadmapSaveResult.InvalidReferences invalid = findMissingReferences(cleanedContent, false);

        domainService.saveDraft(id, cleanedContent, description);
        return new RoadmapSaveResult(id, invalid);
    }

    /**
     * 提交审核：基于当前 draft_content 生成新 revision。
     */
    @Transactional
    public RoadmapSaveResult submit(long id, UserDO operator) {
        validateRoadmapId(id);
        RoadmapDO roadmap = roadmapDataService.validateAndGet(id);
        if (!roadmap.getCreatorId().equals(operator.getId()) && !operator.hasRole(UserRole.ADMIN)) {
            throw StatusCode.PERMISSION_DENIED.exception();
        }
        if (roadmap.getDraftContent() == null || roadmap.getDraftContent().isEmpty()) {
            throw StatusCode.INVALID_PARAMETER.exception("没有可提交的草稿");
        }

        // 提交时校验 + 引用完整性（不允许失效）
        String cleanedContent = domainService.validateAndStrip(roadmap.getDraftContent());
        RoadmapSaveResult.InvalidReferences invalid = findMissingReferences(cleanedContent, true);

        domainService.submit(id, cleanedContent, operator.getId());
        return new RoadmapSaveResult(id, invalid);
    }

    /**
     * 作者撤回 pending revision。
     */
    @Transactional
    public void withdraw(long id, UserDO operator) {
        validateRoadmapId(id);
        RoadmapDO roadmap = roadmapDataService.validateAndGet(id);
        if (!roadmap.getCreatorId().equals(operator.getId())) {
            throw StatusCode.PERMISSION_DENIED.exception();
        }
        Long pendingId = roadmap.getPendingRevisionId();
        domainService.withdraw(id, operator.getId());

        RoleDO role = roleDataService.getById(roadmap.getRoleId());
        eventPublisher.publishEvent(RoadmapWithdrawnEvent.of(
            roadmap.getCreatorId(), id, pendingId,
            roadmap.getRoleId(), role != null ? role.getName() : null));
    }

    @Transactional
    public void deleteRoadmap(Long id, UserDO operator) {
        validateRoadmapId(id);
        RoadmapDO roadmap = roadmapDataService.validateAndGet(id);
        if (!roadmap.getCreatorId().equals(operator.getId()) && !operator.hasRole(UserRole.ADMIN)) {
            throw StatusCode.PERMISSION_DENIED.exception();
        }
        domainService.deleteRoadmap(id);
    }

    // ========== Admin 管理 ==========

    public KeysetPageResponse<RoadmapAdminDTO> listByState(NewContentState state, Long lastId) {
        List<RoadmapDO> roadmapDOList = domainService.listByState(state, lastId, DEFAULT_ADMIN_PAGE_SIZE + 1);
        return buildAdminResponse(roadmapDOList);
    }

    public KeysetPageResponse<RoadmapAdminDTO> listByFilter(Long roadmapId, Long roleId, Long creatorId, Long lastId) {
        List<RoadmapDO> roadmapDOList = domainService.listByFilter(roadmapId, roleId, creatorId, lastId, DEFAULT_ADMIN_PAGE_SIZE + 1);
        return buildAdminResponse(roadmapDOList);
    }

    private KeysetPageResponse<RoadmapAdminDTO> buildAdminResponse(List<RoadmapDO> roadmapDOList) {
        boolean hasMore = roadmapDOList.size() > DEFAULT_ADMIN_PAGE_SIZE;
        if (hasMore) {
            roadmapDOList = roadmapDOList.subList(0, DEFAULT_ADMIN_PAGE_SIZE);
        }

        List<RoadmapAdminDTO> items = roadmapConverter.toAdminDTO(roadmapDOList);

        Set<Long> roleIds = roadmapDOList.stream()
            .map(RoadmapDO::getRoleId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, RoleDO> roleMap = roleIds.isEmpty()
            ? Map.of()
            : roleDataService.getByIds(new ArrayList<>(roleIds)).stream()
                .collect(Collectors.toMap(RoleDO::getId, p -> p));

        Set<Long> creatorIds = roadmapDOList.stream()
            .map(RoadmapDO::getCreatorId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, UserDO> creatorMap = creatorIds.isEmpty()
            ? Map.of()
            : userDataService.getMapByIds(creatorIds);

        Map<Long, RoadmapDO> roadmapDOMap = roadmapDOList.stream()
            .collect(Collectors.toMap(RoadmapDO::getId, r -> r));

        for (RoadmapAdminDTO dto : items) {
            RoadmapDO roadmapDO = roadmapDOMap.get(dto.getId());
            if (roadmapDO != null) {
                if (roadmapDO.getRoleId() != null) {
                    RoleDO role = roleMap.get(roadmapDO.getRoleId());
                    if (role != null) {
                        dto.setRole(roleConverter.toBriefDTO(role));
                    }
                }
                if (roadmapDO.getCreatorId() != null) {
                    UserDO creator = creatorMap.get(roadmapDO.getCreatorId());
                    if (creator != null) {
                        dto.setCreator(userConverter.toBriefDTO(creator));
                    }
                }
                // 填充 reason（最近一条 REJECTED revision 的 reject_reason）
                ContentRevisionDO latest = revisionDataService.getLatest(CONTENT_TYPE, roadmapDO.getId());
                if (latest != null && RevisionStatus.REJECTED_VALUE.equals(latest.getStatus())) {
                    dto.setReason(latest.getRejectReason());
                }
            }
        }

        // 批量统计
        List<Long> roadmapIds = roadmapDOList.stream().map(RoadmapDO::getId).collect(Collectors.toList());
        if (!roadmapIds.isEmpty()) {
            List<ContentStatsDO> statsList = contentStatsDataService.batchGetByContentIds(ContentType.roadmap, roadmapIds);
            Map<Long, ContentStatsDO> statsMap = statsList.stream()
                .collect(Collectors.toMap(ContentStatsDO::getContentId, s -> s));
            for (RoadmapAdminDTO dto : items) {
                ContentStatsDO stats = statsMap.get(dto.getId());
                if (stats != null) {
                    dto.setViewCount(stats.getViewCount());
                    dto.setLikeCount(stats.getLikeCount());
                    dto.setCommentCount(stats.getCommentCount());
                    dto.setBookmarkCount(stats.getBookmarkCount());
                    dto.setLearnerCount(stats.getLearnerCount());
                    dto.setCompletedUserCount(stats.getCompletedUserCount());
                    dto.setRejectCount(stats.getRejectCount());
                }
            }
        }

        Long nextLastId = hasMore && !items.isEmpty() ? items.get(items.size() - 1).getId() : null;
        return KeysetPageResponse.of(items, hasMore, null, nextLastId);
    }

    /**
     * 审核员通过 pending revision。
     */
    @Transactional
    public RoadmapSummaryDTO approve(long id, UserDO operator) {
        RoadmapDO roadmap = roadmapDataService.validateAndGet(id);
        Long pendingId = roadmap.getPendingRevisionId();

        domainService.approve(id, operator.getId());

        // 重新读，拿到 approve 之后的最新主表（state、current_revision_id 已变）
        RoadmapDO updated = roadmapDataService.getById(id);
        RoleDO role = roleDataService.getById(updated.getRoleId());

        eventPublisher.publishEvent(RoadmapApprovedEvent.of(
            updated.getCreatorId(), operator.getId(), updated.getId(), pendingId,
            updated.getRoleId(), role != null ? role.getName() : null));

        return toSummaryDTO(updated);
    }

    /**
     * 审核员驳回 pending revision。
     */
    @Transactional
    public RoadmapSummaryDTO reject(long id, String reason, UserDO operator) {
        RoadmapDO roadmap = roadmapDataService.validateAndGet(id);
        Long pendingId = roadmap.getPendingRevisionId();

        domainService.reject(id, reason, operator.getId());

        RoleDO role = roleDataService.getById(roadmap.getRoleId());
        eventPublisher.publishEvent(RoadmapRejectedEvent.of(
            roadmap.getCreatorId(), operator.getId(), id, pendingId,
            roadmap.getRoleId(), role != null ? role.getName() : null, reason));

        // reject 不动主表 state；返回最新主表（draft 已回填）
        return toSummaryDTO(roadmapDataService.getById(id));
    }

    /**
     * 封禁 roadmap：state → BANNED；如有 pending 连带 REJECTED。
     */
    @Transactional
    public RoadmapSummaryDTO ban(long id, String reason, UserDO operator) {
        RoadmapDO roadmap = roadmapDataService.validateAndGet(id);
        NewContentState previousState = NewContentState.getByValue(roadmap.getState());
        RoleDO role = roleDataService.getById(roadmap.getRoleId());

        domainService.ban(id, reason, operator.getId());

        eventPublisher.publishEvent(RoadmapBannedEvent.of(
            roadmap.getCreatorId(), operator.getId(), id, previousState,
            roadmap.getRoleId(), role != null ? role.getName() : null, reason));

        log.info("路线图 {} 被封禁，操作者: {}, 原因: {}", id, operator.getId(), reason);
        return toSummaryDTO(roadmapDataService.getById(id));
    }

    /**
     * 解封：BANNED → PUBLISHED 或 NEVER_PUBLISHED（按 currentRevisionId 决定）。
     */
    @Transactional
    public RoadmapSummaryDTO restore(long id, String reason, UserDO operator) {
        RoadmapDO roadmap = roadmapDataService.validateAndGet(id);
        if (!NewContentState.BANNED_VALUE.equals(roadmap.getState())) {
            throw StatusCode.INVALID_PARAMETER.exception("仅可对 BANNED 状态的路线图执行解封");
        }
        if (!operator.hasRole(UserRole.ADMIN)) {
            throw StatusCode.PERMISSION_DENIED.exception("只有管理员可以解封内容");
        }

        domainService.restore(id);

        RoadmapDO updated = roadmapDataService.getById(id);
        RoleDO role = roleDataService.getById(updated.getRoleId());

        eventPublisher.publishEvent(RoadmapRestoredEvent.of(
            updated.getCreatorId(), operator.getId(), id,
            NewContentState.getByValue(updated.getState()),
            updated.getRoleId(), role != null ? role.getName() : null, reason));

        log.info("路线图 {} 被恢复，操作者: {}, 新状态: {}", id, operator.getId(), updated.getState());
        return toSummaryDTO(updated);
    }

    /**
     * 更新 description（admin 操作）。
     */
    @Transactional
    public RoadmapSummaryDTO updateDescription(long id, String description, UserDO operator) {
        RoadmapDO roadmap = roadmapDataService.validateAndGet(id);
        domainService.updateDescription(id, description);
        roadmap.setDescription(description != null ? description : "");
        return toSummaryDTO(roadmap);
    }

    public RoadmapWithStatusDTO getRoadmapWithContent(Long id, long userId) {
        validateRoadmapId(id);
        validateUserId(userId);
        RoadmapWithStatusDTO roadmapDTO = getById(id, userId);
        if (roadmapDTO == null) {
            throw StatusCode.ROADMAP_NOT_FOUND.exception();
        }
        return roadmapDTO;
    }

    // ========== 引用完整性校验 ==========

    private RoadmapSaveResult.InvalidReferences findMissingReferences(String cleanedContent, boolean throwOnMissing) {
        RoadmapDomainService.BoundIds bound = domainService.collectBoundIds(cleanedContent);

        List<Long> missingCourseIds = new ArrayList<>();
        List<Long> missingNodeIds = new ArrayList<>();

        if (!bound.getCourseIds().isEmpty()) {
            Map<Long, CourseDO> courseMap = courseDataService.getMapByIds(bound.getCourseIds());
            missingCourseIds = bound.getCourseIds().stream()
                .filter(cid -> !courseMap.containsKey(cid))
                .collect(Collectors.toList());
        }
        if (!bound.getNodeIds().isEmpty()) {
            Map<Long, NodeDO> nodeMap = nodeDataService.getMapByIds(bound.getNodeIds());
            missingNodeIds = bound.getNodeIds().stream()
                .filter(nid -> !nodeMap.containsKey(nid))
                .collect(Collectors.toList());
        }

        if (missingCourseIds.isEmpty() && missingNodeIds.isEmpty()) {
            return null;
        }
        if (throwOnMissing) {
            Map<String, Object> details = Map.of(
                "missingCourseIds", missingCourseIds,
                "missingNodeIds", missingNodeIds);
            throw StatusCode.ROADMAP_CONTENT_INVALID.exception(
                "路线图引用了不存在的内容", details);
        }
        return new RoadmapSaveResult.InvalidReferences(missingCourseIds, missingNodeIds);
    }

    // ========== 私有辅助 ==========

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return CommonConstants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, CommonConstants.MAX_PAGE_SIZE);
    }

    private void validateUserId(long userId) {
        if (userId <= 0) throw StatusCode.INVALID_PARAMETER.exception();
    }

    private void validateRoadmapId(Long roadmapId) {
        if (roadmapId == null || roadmapId <= 0) throw StatusCode.INVALID_PARAMETER.exception();
    }

    private void validateRoleId(Long roleId) {
        if (roleId == null || roleId <= 0) throw StatusCode.INVALID_PARAMETER.exception();
    }

    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) throw StatusCode.INVALID_PARAMETER.exception();
    }

    private Set<Long> getLearningIds(long userId, List<Long> roadmapIds) {
        Map<Long, UserLearningDO> learningMap = userLearningDomainService.getBatch(userId, ContentType.roadmap, roadmapIds);
        return learningMap.keySet();
    }
}
