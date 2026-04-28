package com.twicemax.application.service;

import com.twicemax.analytics.dto.ContentStatsDTO;
import com.twicemax.application.dto.v2.Cursor;
import com.twicemax.analytics.stats.dataservice.ContentStatsDataService;
import com.twicemax.analytics.stats.mapper.ContentStatsDO;
import com.twicemax.analytics.stats.service.ContentStatsDomainService;
import com.twicemax.application.converter.RoleConverter;
import com.twicemax.application.converter.RoadmapConverter;
import com.twicemax.application.converter.UserConverter;
import com.twicemax.application.dto.response.KeysetPageResponse;
import com.twicemax.application.dto.response.roadmap.RoadmapAdminDTO;
import com.twicemax.application.dto.response.roadmap.RoadmapBriefDTO;
import com.twicemax.application.dto.response.roadmap.RoadmapDetailDTO;
import com.twicemax.application.dto.response.roadmap.RoadmapSummaryDTO;
import com.twicemax.application.dto.response.roadmap.RoadmapWithStatusDTO;
import com.twicemax.content.course.CourseDO;
import com.twicemax.content.course.CourseDataService;
import com.twicemax.content.node.NodeDO;
import com.twicemax.content.node.NodeDataService;
import com.twicemax.content.role.RoleDO;
import com.twicemax.content.role.RoleDataService;
import com.twicemax.content.roadmap.RoadmapDO;
import com.twicemax.content.roadmap.RoadmapDataService;
import com.twicemax.content.roadmap.RoadmapDomainService;
import com.twicemax.interaction.upvote.UpvoteDomainService;
import com.twicemax.learning.enrollment.UserLearningDO;
import com.twicemax.learning.enrollment.UserLearningDomainService;
import com.twicemax.shared.common.constants.CommonConstants;
import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.domain.event.content.lifecycle.ContentApprovedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.ContentBannedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.ContentRejectedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.ContentRemovedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.ContentRestoredEvent;

import static com.twicemax.shared.domain.Enums.*;
import static com.twicemax.shared.domain.Enums.ContentState;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.user.profile.UserDO;
import com.twicemax.user.profile.UserDataService;
import com.twicemax.user.profile.UserDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 路线图应用服务
 *
 * 负责协调跨领域逻辑、DTO转换、事件发布
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoadmapService {

    private final RoadmapDomainService domainService;
    private final RoadmapDataService roadmapDataService;
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
    
    /**
     * 转换单个对象为摘要DTO
     */
    public RoadmapSummaryDTO toSummaryDTO(RoadmapDO roadmapDO) {
        return roadmapConverter.toSummaryDTO(roadmapDO);
    }

    /**
     * 转换列表为摘要DTO列表
     */
    public List<RoadmapSummaryDTO> toSummaryDTO(List<RoadmapDO> roadmapDOList) {
        return roadmapConverter.toSummaryDTO(roadmapDOList);
    }

    /**
     * 转换为简要DTO（包含role name）
     */
    public RoadmapBriefDTO toBriefDTO(RoadmapDO roadmapDO) {
        if (roadmapDO == null) {
            return null;
        }

        RoadmapBriefDTO dto = new RoadmapBriefDTO();
        dto.setId(roadmapDO.getId());
        dto.setNodeCount(roadmapDO.getNodeCount());

        // 填充role name
        if (roadmapDO.getRoleId() != null) {
            RoleDO role = roleDataService.getById(roadmapDO.getRoleId());
            if (role != null) {
                dto.setRoleName(role.getName());
            }
        }

        return dto;
    }

    /**
     * 批量转换为简要DTO列表
     */
    public List<RoadmapBriefDTO> toBriefDTO(List<RoadmapDO> roadmapDOList) {
        if (roadmapDOList == null || roadmapDOList.isEmpty()) {
            return new ArrayList<>();
        }

        // 提取所有 role IDs
        List<Long> roleIds = roadmapDOList.stream()
            .map(RoadmapDO::getRoleId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        // 批量查询 role
        Map<Long, String> roleNameMap = roleIds.isEmpty()
            ? Map.of()
            : roleDataService.getByIds(roleIds).stream()
                .collect(Collectors.toMap(RoleDO::getId, RoleDO::getName));

        // 组装 DTO
        return roadmapDOList.stream()
            .map(roadmap -> {
                RoadmapBriefDTO dto = new RoadmapBriefDTO();
                dto.setId(roadmap.getId());
                dto.setNodeCount(roadmap.getNodeCount());
                if (roadmap.getRoleId() != null) {
                    dto.setRoleName(roleNameMap.get(roadmap.getRoleId()));
                }
                return dto;
            })
            .collect(Collectors.toList());
    }

    /**
     * 转换列表为详情DTO列表（包含role信息）
     */
    public List<RoadmapDetailDTO> toDetailDTO(List<RoadmapDO> roadmapDOList) {
        if (roadmapDOList == null || roadmapDOList.isEmpty()) {
            return new ArrayList<>();
        }

        List<RoadmapDetailDTO> dtoList = roadmapConverter.toDetailDTO(roadmapDOList);

        // 批量填充 role 信息
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

    /**
     * 转换为路线图（包含完整业务信息）
     * 包含：creator + role + liked + learning + likeCount + commentCount + learnerCount + formatted content
     */
    public RoadmapWithStatusDTO toRoadmapWithStatus(RoadmapDO roadmapDO, long userId) {
        if (roadmapDO == null) return null;

        RoadmapWithStatusDTO dto = roadmapConverter.toWithStatusDTO(roadmapDO);

        // 设置创建者信息
        if (roadmapDO.getCreatorId() != null) {
            dto.setCreator(userConverter.toBriefDTO(userDataService.getById(roadmapDO.getCreatorId())));
        }

        // 设置专业信息
        if (roadmapDO.getRoleId() != null) {
            RoleDO roleDO = roleDataService.getById(roadmapDO.getRoleId());
            dto.setRole(roleConverter.toBriefDTO(roleDO));
        }

        // 设置点赞状态
        dto.setLiked(upvoteDomainService.hasUpvoted(roadmapDO.getId(), ContentType.roadmap.value(), userId));

        // 设置学习状态
        boolean isLearning = userLearningDomainService.isLearning(userId, ContentType.roadmap, roadmapDO.getId());
        dto.setLearning(isLearning);

        // 设置收藏状态
        dto.setBookmarked(bookmarkService.isBookmarked(userId, roadmapDO.getId(), ContentType.roadmap));

        // 查询统计数据
        ContentStatsDTO stats = contentStatsDomainService.getContentStats(ContentType.roadmap, roadmapDO.getId());
        dto.setLikeCount(stats.getLikeCount() != null ? stats.getLikeCount() : 0);
        dto.setCommentCount(stats.getCommentCount() != null ? stats.getCommentCount() : 0);
        dto.setLearnerCount(stats.getInProgressUserCount() != null ? stats.getInProgressUserCount() : 0);

        // 设置格式化内容（回填 c/n 节点的 label）
        if (roadmapDO.getContent() != null) {
            dto.setContent(enrichContent(roadmapDO.getContent()));
        }

        return dto;
    }

    /**
     * 统计路线图内容中的 c/n 叶子节点数量
     * @param content 路线图内容JSON字符串
     * @return 节点数量
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

    /**
     * 转换为路线图（包含完整业务信息）
     * 包含：creator + role + liked + learning + formatted content + likes
     */
    private List<RoadmapWithStatusDTO> toRoadmapWithFullInfo(List<RoadmapDO> roadmapList, long userId, Long roleId) {
        List<RoadmapWithStatusDTO> dtoList = roadmapConverter.toWithStatusDTO(roadmapList);

        if (!dtoList.isEmpty()) {
            List<Long> roadmapIds = dtoList.stream()
                    .map(RoadmapSummaryDTO::getId)
                    .collect(Collectors.toList());

            // 收集所有创建者ID
            Set<Long> creatorIds = roadmapList.stream()
                    .map(RoadmapDO::getCreatorId)
                    .collect(Collectors.toSet());

            // 批量查询创建者信息
            Map<Long, UserDO> creatorMap = userDataService.getMapByIds(creatorIds);

            RoleDO roleDO = roleDataService.getById(roleId);
            Set<Long> upvotedIds = upvoteDomainService.getUpvotedIds(roadmapIds, ContentType.roadmap.value(), userId);
            Set<Long> learningIds = getLearningIds(userId, roadmapIds);
            List<Long> bookmarkedIds = bookmarkService.getBookmarkedIds(userId, roadmapIds, ContentType.roadmap);
            Set<Long> bookmarkedSet = new HashSet<>(bookmarkedIds);

            // 批量获取统计数据
            Map<Long, ContentStatsDTO> statsMap = contentStatsDomainService.batchGetContentStats(ContentType.roadmap, roadmapIds);

            // 创建 roadmapDO 的映射，方便查找 creatorId
            Map<Long, RoadmapDO> roadmapDOMap = roadmapList.stream()
                    .collect(Collectors.toMap(RoadmapDO::getId, r -> r));

            for (RoadmapWithStatusDTO dto : dtoList) {
                RoadmapDO roadmapDO = roadmapDOMap.get(dto.getId());
                UserDO creator = creatorMap.get(roadmapDO.getCreatorId());

                dto.setRole(roleConverter.toBriefDTO(roleDO));
                dto.setCreator(creator != null ? userConverter.toBriefDTO(creator) : null);
                dto.setLiked(upvotedIds.contains(dto.getId()));
                dto.setLearning(learningIds.contains(dto.getId()));
                dto.setBookmarked(bookmarkedSet.contains(dto.getId()));

                // 设置统计数据
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

                // 列表场景不返回 content，详情页才需要回填 trunk
                dto.setContent(null);
            }
        }
        return dtoList;
    }

    // ========== 公共方法 ==========

    /**
     * 获取角色路线图列表（公开接口，无个性化信息）
     * 用于匿名用户浏览
     */
    public List<RoadmapSummaryDTO> getRoadmapsByRolePublic(Long roleId, Long lastId, Integer pageSize) {
        validateRoleId(roleId);

        int limit = normalizePageSize(pageSize);

        // 委托给 DomainService 查询
        List<RoadmapDO> roadmapList = domainService.getRoadmapsByRolePublic(roleId, lastId, limit);

        // 转换为DTO，只包含基础信息
        return toSummaryDTO(roadmapList);
    }

    /**
     * return RoadmapDTO
     */
    public RoadmapSummaryDTO getById(long id) {
        validateRoadmapId(id);

        RoadmapDO roadmapDO = roadmapDataService.getById(id);
        if (roadmapDO == null) {
            return null;
        }
        return toSummaryDTO(roadmapDO);
    }

    /**
     * return RoadmapWithStatusDTO (替代旧的 v1)
     */
    public RoadmapWithStatusDTO getById(long id, long userId) {
        validateRoadmapId(id);
        validateUserId(userId);

        RoadmapDO roadmapDO = roadmapDataService.getById(id);
        if (roadmapDO == null) {
            return null;
        }

        // 检查路线图及其角色的可见性
        contentVisibilityService.validateVisibility(ContentType.roadmap, id, userId);

        return toRoadmapWithStatus(roadmapDO, userId);
    }

    /**
     * 把内容中的 c/n 节点 label 用数据库中的课程/节点名称回填，输出可供前端渲染的 JSON。
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

    /**
     * 获取角色路线图列表（带置顶和状态信息）
     */
    public List<RoadmapWithStatusDTO> getRoadmapsByRole(Long roleId, String cursor, String sortBy, Integer pageSize, UserDO currentUser) {
        validateRoleId(roleId);

        // 默认按 score 排序
        if (sortBy == null || sortBy.isEmpty() || (!sortBy.equals("latest") && !sortBy.equals("score"))) {
            sortBy = "score";
        }

        int limit = normalizePageSize(pageSize);

        // 委托给 DomainService 查询路线图列表
        List<RoadmapDO> roadmapList = domainService.getRoadmapsByRole(
            roleId, Cursor.decode(cursor).id(), limit, sortBy);

        // 转换为完整DTO（包含跨域信息）
        return toRoadmapWithFullInfo(roadmapList, currentUser.getId(), roleId);
    }

    /**
     * 获取用户创建的路线图列表（所有状态）
     * @param userId 用户ID
     * @param lastId 分页游标
     * @param pageSize 每页数量
     * @return 路线图列表（包含专业信息）
     */
    public List<RoadmapDetailDTO> getUserRoadmaps(Long userId, String cursor, ContentState state, Integer pageSize) {
        validateUserId(userId);

        int limit = normalizePageSize(pageSize);

        // 委托给 DomainService 查询
        List<RoadmapDO> roadmapList = domainService.getUserRoadmaps(
            userId, Cursor.decode(cursor).id(), limit, state == null ? null : state.value());

        return toDetailDTO(roadmapList);
    }

    /**
     * 根据ID列表获取路线图（包含用户状态）
     * @param roadmapIds 路线图ID列表
     * @param userId 用户ID
     * @return 路线图列表（包含用户状态信息）
     */
    public List<RoadmapWithStatusDTO> getRoadmapsByIdsWithStatus(List<Long> roadmapIds, Long userId) {
        if (roadmapIds == null || roadmapIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 从 DomainService 获取路线图列表
        List<RoadmapDO> roadmapList = domainService.getRoadmapsByIds(roadmapIds);

        // 转换为完整DTO（包含跨域信息），roleId 为 null
        return toRoadmapWithFullInfo(roadmapList, userId, null);
    }

    /**
     * 删除路线图（软删除）
     * @param id 路线图ID
     * @param operator 操作用户
     */
    @Transactional
    public void deleteRoadmap(Long id, UserDO operator) {
        validateRoadmapId(id);

        RoadmapDO roadmapDO = roadmapDataService.validateAndGet(id);

        // 验证权限：只能删除自己创建的路线图，除非是管理员
        if (!roadmapDO.getCreatorId().equals(operator.getId()) && !operator.hasRole(UserRole.ADMIN)) {
            throw StatusCode.PERMISSION_DENIED.exception();
        }

        // 委托给 DomainService
        domainService.deleteRoadmap(id);
    }

    /**
     * 更新路线图
     */
    @Transactional
    public void updateRoadmap(Long id, String content, String description, Byte state, UserDO operator) {
        validateRoadmapId(id);
        validateContent(content);

        // 先验证路线图是否存在
        RoadmapDO roadmapDO = roadmapDataService.validateAndGet(id);

        // 验证权限：只有所有者或管理员可以修改
        if (!roadmapDO.getCreatorId().equals(operator.getId()) && !operator.hasRole(UserRole.ADMIN)) {
            throw StatusCode.PERMISSION_DENIED.exception();
        }

        // 验证状态：只能是草稿或提交审核
        if (!state.equals(ContentState.DRAFT.value()) && !state.equals(ContentState.SUBMITTED.value())) {
            throw StatusCode.INVALID_PARAMETER.exception("状态只能是草稿(0)或提交审核(1)");
        }

        // 状态转换验证：已发布的内容不能变回草稿
        if (roadmapDO.getState().equals(ContentState.PUBLISHED.value()) && state.equals(ContentState.DRAFT.value())) {
            throw StatusCode.INVALID_PARAMETER.exception("已发布的内容不能变回草稿");
        }

        // 校验内容、剥离 c/n label，并校验引用的课程/节点存在
        String cleanedContent = validateAndCheckReferences(content);

        // 计算节点数量（c+n 叶子）
        Integer nodeCount = countNodesInContent(cleanedContent);

        // 委托给 DomainService 更新内容和节点数量
        domainService.updateRoadmap(id, cleanedContent, nodeCount);

        // 更新状态：如果原状态是已发布，用户修改后强制变为提交审核
        Byte newState = roadmapDO.getState().equals(ContentState.PUBLISHED.value())
            ? ContentState.SUBMITTED.value()
            : state;
        roadmapDO.setState(newState);
        roadmapDataService.update(roadmapDO);

        // 如果提供了 description，则更新描述
        if (description != null) {
            domainService.updateDescription(id, description);
        }
    }

    /**
     * 创建路线图
     */
    @Transactional
    public Long createRoadmap(Long roleId, String content, String description, long userId, Byte state) {
        validateRoleId(roleId);
        validateContent(content);
        validateUserId(userId);

        // 跨域验证：验证专业和用户存在
        roleDataService.validateExists(roleId);
        userDataService.validateExists(userId);

        // 验证角色是否为 PUBLISHED
        contentVisibilityService.validateCanCreateOn(ContentType.role, roleId);

        // 验证状态：只能是草稿或提交审核
        if (!state.equals(ContentState.DRAFT.value()) && !state.equals(ContentState.SUBMITTED.value())) {
            throw StatusCode.INVALID_PARAMETER.exception("状态只能是草稿(0)或提交审核(1)");
        }

        // 校验内容、剥离 c/n label，并校验引用的课程/节点存在
        String cleanedContent = validateAndCheckReferences(content);

        // 计算节点数量
        Integer nodeCount = countNodesInContent(cleanedContent);

        // 委托给 DomainService
        return domainService.createRoadmap(roleId, cleanedContent, description, userId, nodeCount, state);
    }

    /**
     * 校验内容格式 + 剥离 c/n 节点 label + 校验引用的 courseId/nodeId 在库中存在。
     * @return 标准化（已剥离 label）的 JSON 字符串
     */
    private String validateAndCheckReferences(String content) {
        // 1. 结构校验 + 剥离 c/n 的 label
        String cleaned = domainService.validateAndStrip(content);

        // 2. 引用完整性校验
        RoadmapDomainService.BoundIds bound = domainService.collectBoundIds(cleaned);

        if (!bound.getCourseIds().isEmpty()) {
            Map<Long, CourseDO> courseMap = courseDataService.getMapByIds(bound.getCourseIds());
            List<Long> missing = bound.getCourseIds().stream()
                .filter(cid -> !courseMap.containsKey(cid))
                .collect(Collectors.toList());
            if (!missing.isEmpty()) {
                throw StatusCode.ROADMAP_CONTENT_INVALID.exception("路线图中包含不存在的课程: " + missing);
            }
        }

        if (!bound.getNodeIds().isEmpty()) {
            Map<Long, NodeDO> nodeMap = nodeDataService.getMapByIds(bound.getNodeIds());
            List<Long> missing = bound.getNodeIds().stream()
                .filter(nid -> !nodeMap.containsKey(nid))
                .collect(Collectors.toList());
            if (!missing.isEmpty()) {
                throw StatusCode.ROADMAP_CONTENT_INVALID.exception("路线图中包含不存在的节点: " + missing);
            }
        }

        return cleaned;
    }

    /**
     * 获取路线图详情（带格式化内容）
     */
    public RoadmapWithStatusDTO getRoadmapWithContent(Long id, long userId) {
        validateRoadmapId(id);
        validateUserId(userId);

        RoadmapWithStatusDTO roadmapDTO = getById(id, userId);

        if (roadmapDTO == null) {
            throw StatusCode.ROADMAP_NOT_FOUND.exception();
        }

        return roadmapDTO;
    }

    // ========== 私有辅助方法 ==========

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return CommonConstants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, CommonConstants.MAX_PAGE_SIZE);
    }

    private void validateUserId(long userId) {
        if (userId <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception();
        }
    }
    
    private void validateRoadmapId(Long roadmapId) {
        if (roadmapId == null || roadmapId <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception();
        }
    }
    
    private void validateRoleId(Long roleId) {
        if (roleId == null || roleId <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception();
        }
    }
    
    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw StatusCode.INVALID_PARAMETER.exception();
        }
    }

    private Set<Long> getLearningIds(long userId, List<Long> roadmapIds) {
        Map<Long, UserLearningDO> learningMap = userLearningDomainService.getBatch(userId, ContentType.roadmap, roadmapIds);
        return learningMap.keySet();
    }

    // ========== Admin管理方法 ==========

    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Admin管理：按状态获取路线图列表
     */
    public KeysetPageResponse<RoadmapAdminDTO> listByState(ContentState state, Long lastId) {
        List<RoadmapDO> roadmapDOList = domainService.listByState(state, lastId, DEFAULT_PAGE_SIZE + 1);
        return buildAdminResponse(roadmapDOList);
    }

    /**
     * Admin管理：高级筛选路线图列表
     */
    public KeysetPageResponse<RoadmapAdminDTO> listByFilter(Long roadmapId, Long roleId, Long creatorId, Long lastId) {
        List<RoadmapDO> roadmapDOList = domainService.listByFilter(roadmapId, roleId, creatorId, lastId, DEFAULT_PAGE_SIZE + 1);
        return buildAdminResponse(roadmapDOList);
    }

    private KeysetPageResponse<RoadmapAdminDTO> buildAdminResponse(List<RoadmapDO> roadmapDOList) {
        boolean hasMore = roadmapDOList.size() > DEFAULT_PAGE_SIZE;
        if (hasMore) {
            roadmapDOList = roadmapDOList.subList(0, DEFAULT_PAGE_SIZE);
        }

        List<RoadmapAdminDTO> items = roadmapConverter.toAdminDTO(roadmapDOList);

        // 批量填充 role 信息
        Set<Long> roleIds = roadmapDOList.stream()
            .map(RoadmapDO::getRoleId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Long, RoleDO> roleMap = roleIds.isEmpty()
            ? Map.of()
            : roleDataService.getByIds(new ArrayList<>(roleIds)).stream()
                .collect(Collectors.toMap(RoleDO::getId, p -> p));

        // 批量填充 creator 信息
        Set<Long> creatorIds = roadmapDOList.stream()
            .map(RoadmapDO::getCreatorId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Long, UserDO> creatorMap = creatorIds.isEmpty()
            ? Map.of()
            : userDataService.getMapByIds(creatorIds);

        // 创建 roadmapDO 映射
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
            }
        }

        // 批量填充统计数据
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
     * 批准路线图（直接通过，保留描述）
     */
    @Transactional
    public RoadmapSummaryDTO approve(long id, UserDO operator) {
        RoadmapDO roadmap = roadmapDataService.getById(id);
        if (roadmap == null) {
            throw StatusCode.ROADMAP_NOT_FOUND.exception();
        }

        // 委托给 DomainService 执行状态变更
        domainService.approve(id);
        roadmap.setState(ContentState.PUBLISHED.value());

        // 获取角色信息
        RoleDO role = roleDataService.getById(roadmap.getRoleId());

        // 发布审核通过事件，触发统计更新（不发送消息）
        eventPublisher.publishEvent(ContentApprovedEvent.forRoadmap(
            roadmap.getCreatorId(),
            roadmap.getId(),
            role != null ? role.getId() : null,
            role != null ? role.getName() : null
        ));

        return toSummaryDTO(roadmap);
    }

    /**
     * 拒绝路线图
     */
    @Transactional
    public RoadmapSummaryDTO reject(long id, String reason, UserDO operator) {
        RoadmapDO roadmap = roadmapDataService.getById(id);
        if (roadmap == null) {
            throw StatusCode.ROADMAP_NOT_FOUND.exception();
        }

        // 委托给 DomainService 执行状态变更
        domainService.reject(id, reason);
        roadmap.setState(ContentState.REJECTED.value());

        // 获取角色信息用于通知
        RoleDO role = roleDataService.getById(roadmap.getRoleId());

        // 发布审核拒绝事件，触发消息通知
        eventPublisher.publishEvent(ContentRejectedEvent.forRoadmap(
            roadmap.getCreatorId(),
            roadmap.getId(),
            role != null ? role.getId() : null,
            role != null ? role.getName() : null,
            reason
        ));

        return toSummaryDTO(roadmap);
    }

    /**
     * 封禁路线图
     */
    @Transactional
    public RoadmapSummaryDTO ban(long id, String reason, UserDO operator) {
        RoadmapDO roadmap = roadmapDataService.getById(id);
        if (roadmap == null) {
            throw StatusCode.ROADMAP_NOT_FOUND.exception();
        }

        // 记录之前的状态
        Byte previousState = roadmap.getState();

        // 获取角色信息用于通知
        RoleDO role = roleDataService.getById(roadmap.getRoleId());

        // 委托给 DomainService 执行状态变更
        domainService.ban(id, reason);

        // 发布内容封禁事件，触发统计更新和消息通知
        eventPublisher.publishEvent(ContentBannedEvent.forRoadmap(
            roadmap.getCreatorId(),
            roadmap.getId(),
            ContentState.getByValue(previousState),
            roadmap.getRoleId(),
            role != null ? role.getName() : null,
            reason
        ));

        log.info("路线图 {} 被封禁，操作者: {}, 原因: {}", id, operator.getId(), reason);

        roadmap.setState(ContentState.BANNED.value());
        return toSummaryDTO(roadmap);
    }

    /**
     * 下架路线图（已发布内容违规，降级为REJECTED状态）
     */
    @Transactional
    public RoadmapSummaryDTO remove(long id, String reason, UserDO operator) {
        RoadmapDO roadmap = roadmapDataService.getById(id);
        if (roadmap == null) {
            throw StatusCode.ROADMAP_NOT_FOUND.exception();
        }

        // 检查状态：只能下架已发布的内容
        if (roadmap.getState() != ContentState.PUBLISHED.value()) {
            throw StatusCode.INVALID_PARAMETER.exception("只能下架已发布的内容");
        }

        // 获取角色信息用于通知
        RoleDO role = roleDataService.getById(roadmap.getRoleId());

        // 委托给 DomainService 执行状态变更
        domainService.reject(id, reason);

        // 发布内容下架事件，触发统计更新和消息通知
        eventPublisher.publishEvent(ContentRemovedEvent.forRoadmap(
            roadmap.getCreatorId(),
            roadmap.getId(),
            roadmap.getRoleId(),
            role != null ? role.getName() : null,
            reason
        ));

        log.info("路线图 {} 被下架，操作者: {}, 原因: {}", id, operator.getId(), reason);

        roadmap.setState(ContentState.REJECTED.value());
        return toSummaryDTO(roadmap);
    }

    /**
     * 恢复路线图（管理员撤销误操作）
     */
    @Transactional
    public RoadmapSummaryDTO restore(long id, String reason, UserDO operator) {
        RoadmapDO roadmap = roadmapDataService.getById(id);
        if (roadmap == null) {
            throw StatusCode.ROADMAP_NOT_FOUND.exception();
        }

        // 记录之前的状态
        Byte previousState = roadmap.getState();

        // 检查状态：只能恢复 REJECTED 或 BANNED 的内容
        if (previousState != ContentState.REJECTED.value() && previousState != ContentState.BANNED.value()) {
            throw StatusCode.INVALID_PARAMETER.exception("只能恢复被拒绝或被封禁的内容");
        }

        // 从 BANNED 恢复需要 ADMIN 权限
        if (previousState == ContentState.BANNED.value() && !operator.hasRole(UserRole.ADMIN)) {
            throw StatusCode.PERMISSION_DENIED.exception("只有管理员可以解封内容");
        }

        // 获取角色信息用于通知
        RoleDO role = roleDataService.getById(roadmap.getRoleId());

        // 委托给 DomainService 执行状态变更
        domainService.approve(id);

        // 发布内容恢复事件，触发统计恢复和消息通知
        eventPublisher.publishEvent(ContentRestoredEvent.forRoadmap(
            operator.getId(),  // operatorId
            roadmap.getCreatorId(),
            roadmap.getId(),
            ContentState.getByValue(previousState),
            roadmap.getRoleId(),
            role != null ? role.getName() : null,
            reason
        ));

        log.info("路线图 {} 被恢复，操作者: {}, 原因: {}", id, operator.getId(), reason);

        roadmap.setState(ContentState.PUBLISHED.value());
        return toSummaryDTO(roadmap);
    }

    /**
     * 更新路线图描述（管理员操作）
     */
    @Transactional
    public RoadmapSummaryDTO updateDescription(long id, String description, UserDO operator) {
        RoadmapDO roadmap = roadmapDataService.getById(id);
        if (roadmap == null) {
            throw StatusCode.ROADMAP_NOT_FOUND.exception();
        }

        // 委托给 DomainService
        domainService.updateDescription(id, description);
        roadmap.setDescription(description != null ? description : "");

        return toSummaryDTO(roadmap);
    }
}
