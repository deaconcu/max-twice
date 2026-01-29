package com.prosper.learn.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.analytics.dto.ContentStatsDTO;
import com.prosper.learn.analytics.stats.mapper.ContentStatsDO;
import com.prosper.learn.analytics.stats.service.ContentStatsDomainService;
import com.prosper.learn.application.converter.ProfessionConverter;
import com.prosper.learn.application.converter.RoadmapConverter;
import com.prosper.learn.application.converter.UserConverter;
import com.prosper.learn.application.dto.response.roadmap.RoadmapBriefDTO;
import com.prosper.learn.application.dto.response.roadmap.RoadmapDetailDTO;
import com.prosper.learn.application.dto.response.roadmap.RoadmapSummaryDTO;
import com.prosper.learn.application.dto.response.roadmap.RoadmapWithStatusDTO;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.profession.ProfessionDO;
import com.prosper.learn.content.profession.ProfessionDataService;
import com.prosper.learn.content.roadmap.RoadmapDO;
import com.prosper.learn.content.roadmap.RoadmapDataService;
import com.prosper.learn.content.roadmap.RoadmapDomainService;
import com.prosper.learn.interaction.upvote.UpvoteDomainService;
import com.prosper.learn.learning.enrollment.UserLearningDO;
import com.prosper.learn.learning.enrollment.UserLearningDomainService;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentApprovedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentBannedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentRejectedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentRemovedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentRestoredEvent;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import com.prosper.learn.user.profile.UserDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 路线图应用服务
 *
 * 负责协调跨领域逻辑、DTO转换、事件发布
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoadmapService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final RoadmapDomainService domainService;
    private final RoadmapDataService roadmapDataService;
    private final NodeDataService nodeDataService;
    private final UserDataService userDataService;
    private final UserDomainService userDomainService;
    private final UserLearningDomainService userLearningDomainService;
    private final ProfessionDataService professionDataService;
    private final UpvoteDomainService upvoteDomainService;
    private final ContentStatsDomainService contentStatsDomainService;
    private final BookmarkService bookmarkService;
    private final LearningProgressService learningProgressService;
    private final ApplicationEventPublisher eventPublisher;
    private final RoadmapConverter roadmapConverter;
    private final UserConverter userConverter;
    private final ProfessionConverter professionConverter;
    private final SystemProperties systemProperties;

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
     * 转换为简要DTO（包含profession name）
     */
    public RoadmapBriefDTO toBriefDTO(RoadmapDO roadmapDO) {
        if (roadmapDO == null) {
            return null;
        }

        RoadmapBriefDTO dto = new RoadmapBriefDTO();
        dto.setId(roadmapDO.getId());
        dto.setNodeCount(roadmapDO.getNodeCount());

        // 填充profession name
        if (roadmapDO.getProfessionId() != null) {
            ProfessionDO profession = professionDataService.getById(roadmapDO.getProfessionId());
            if (profession != null) {
                dto.setProfessionName(profession.getName());
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

        // 提取所有 profession IDs
        List<Long> professionIds = roadmapDOList.stream()
            .map(RoadmapDO::getProfessionId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        // 批量查询 profession
        Map<Long, String> professionNameMap = professionIds.isEmpty()
            ? Map.of()
            : professionDataService.getByIds(professionIds).stream()
                .collect(Collectors.toMap(ProfessionDO::getId, ProfessionDO::getName));

        // 组装 DTO
        return roadmapDOList.stream()
            .map(roadmap -> {
                RoadmapBriefDTO dto = new RoadmapBriefDTO();
                dto.setId(roadmap.getId());
                dto.setNodeCount(roadmap.getNodeCount());
                if (roadmap.getProfessionId() != null) {
                    dto.setProfessionName(professionNameMap.get(roadmap.getProfessionId()));
                }
                return dto;
            })
            .collect(Collectors.toList());
    }

    /**
     * 转换列表为详情DTO列表（包含profession信息）
     */
    public List<RoadmapDetailDTO> toDetailDTO(List<RoadmapDO> roadmapDOList) {
        if (roadmapDOList == null || roadmapDOList.isEmpty()) {
            return new ArrayList<>();
        }

        List<RoadmapDetailDTO> dtoList = roadmapConverter.toDetailDTO(roadmapDOList);

        // 批量填充 profession 信息
        Set<Long> professionIds = roadmapDOList.stream()
            .map(RoadmapDO::getProfessionId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (!professionIds.isEmpty()) {
            List<ProfessionDO> professions = professionDataService.getByIds(new ArrayList<>(professionIds));
            Map<Long, ProfessionDO> professionMap = professions.stream()
                .collect(Collectors.toMap(ProfessionDO::getId, p -> p));

            for (RoadmapDetailDTO dto : dtoList) {
                if (dto.getProfessionId() != null) {
                    ProfessionDO profession = professionMap.get(dto.getProfessionId());
                    if (profession != null) {
                        dto.setProfession(professionConverter.toBriefDTO(profession));
                    }
                }
            }
        }

        return dtoList;
    }

    /**
     * 转换为路线图（包含完整业务信息）
     * 包含：creator + profession + liked + learning + likeCount + commentCount + learnerCount + formatted content
     */
    public RoadmapWithStatusDTO toRoadmapWithStatus(RoadmapDO roadmapDO, long userId) {
        if (roadmapDO == null) return null;

        RoadmapWithStatusDTO dto = roadmapConverter.toWithStatusDTO(roadmapDO);

        // 设置创建者信息
        if (roadmapDO.getCreatorId() != null) {
            dto.setCreator(userConverter.toBriefDTO(userDataService.getById(roadmapDO.getCreatorId())));
        }

        // 设置专业信息
        if (roadmapDO.getProfessionId() != null) {
            ProfessionDO professionDO = professionDataService.getById(roadmapDO.getProfessionId());
            dto.setProfession(professionConverter.toBriefDTO(professionDO));
        }

        // 设置点赞状态
        dto.setLiked(upvoteDomainService.hasUpvoted(roadmapDO.getId(), ContentType.roadmap.value(), userId));

        // 设置学习状态
        boolean isLearning = userLearningDomainService.isLearning(userId, Enums.ContentType.roadmap, roadmapDO.getId());
        dto.setLearning(isLearning);

        // 设置收藏状态
        dto.setBookmarked(bookmarkService.isBookmarked(userId, roadmapDO.getId(), ContentType.roadmap));

        // 查询统计数据
        ContentStatsDTO stats = contentStatsDomainService.getContentStats(ContentType.roadmap, roadmapDO.getId());
        dto.setLikeCount(stats.getLikeCount() != null ? stats.getLikeCount() : 0);
        dto.setCommentCount(stats.getCommentCount() != null ? stats.getCommentCount() : 0);
        dto.setLearnerCount(stats.getInProgressUserCount() != null ? stats.getInProgressUserCount() : 0);

        // 设置格式化内容
        if (roadmapDO.getContent() != null) {
            dto.setContent(parseContentToGraphFormat(roadmapDO.getContent(), userId));
        }

        return dto;
    }

    /**
     * 统计路线图内容中的节点数量
     * @param content 路线图内容JSON字符串
     * @return 节点数量
     */
    public Integer countNodesInContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return 0;
        }

        try {
            List<List<Object>> contentData = objectMapper.readValue(content, new TypeReference<>() {});
            if (contentData.size() >= 2) {
                List<Object> nodeIdsRaw = contentData.get(1);
                return nodeIdsRaw.size();
            }
            return 0;
        } catch (Exception e) {
            log.error("Failed to count nodes in content", e);
            return 0;
        }
    }

    /**
     * 转换为路线图（包含完整业务信息）
     * 包含：creator + profession + liked + learning + formatted content + likes
     */
    private List<RoadmapWithStatusDTO> toRoadmapWithFullInfo(List<RoadmapDO> roadmapList, long userId, Long professionId) {
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

            ProfessionDO professionDO = professionDataService.getById(professionId);
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

                dto.setProfession(professionConverter.toBriefDTO(professionDO));
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

                if (dto.getContent() != null) {
                    String formattedContent = parseContentToGraphFormat(dto.getContent(), userId);
                    dto.setContent(formattedContent);
                }
            }
        }
        return dtoList;
    }

    // ========== 公共方法 ==========

    /**
     * 获取职业路线图列表（公开接口，无个性化信息）
     * 用于匿名用户浏览
     */
    public List<RoadmapSummaryDTO> getRoadmapsByProfessionPublic(Long professionId, Long lastId, Integer pageSize) {
        validateProfessionId(professionId);

        int limit = pageSize != null && pageSize > 0 ? pageSize : systemProperties.getRoadmap().getDefaultPageSize();

        // 委托给 DomainService 查询
        List<RoadmapDO> roadmapList = domainService.getRoadmapsByProfessionPublic(professionId, lastId, limit);

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
        return toRoadmapWithStatus(roadmapDO, userId);
    }

    public String parseContentToGraphFormat(String content, long userId) {
        validateContent(content);
        validateUserId(userId);

        try {
            // 解析内容获取节点ID列表
            List<List<Object>> contentData = objectMapper.readValue(content, new TypeReference<>() {});
            List<Long> nodeIds = extractNodeIds(contentData);

            // 批量查询节点信息
            Map<Long, NodeDO> nodeMap = nodeDataService.getMapByIds(nodeIds);

            // 跨域查询：获取节点进度（返回 Map<nodeId, progressPercent>）
            Map<Long, Integer> nodeProgress = learningProgressService.batchCalculateNodeProgress(userId, nodeIds);

            // 委托给 DomainService 进行纯粹的内容转换
            return domainService.parseContentToGraphFormat(content, nodeProgress, nodeMap);
        } catch (Exception e) {
            log.error("内容解析为图形格式失败", e);
            throw StatusCode.JSON_PROCESSING_ERROR.exception(e);
        }
    }

    /**
     * 从内容数据中提取节点ID列表
     */
    private List<Long> extractNodeIds(List<List<Object>> contentData) {
        List<Long> nodeIds = new ArrayList<>();
        if (contentData.size() >= 2) {
            List<Object> nodeIdsRaw = contentData.get(1);
            for (Object nodeIdObj : nodeIdsRaw) {
                if (nodeIdObj instanceof Number) {
                    nodeIds.add(((Number) nodeIdObj).longValue());
                }
            }
        }
        return nodeIds;
    }

    /**
     * 获取职业路线图列表（带置顶和状态信息）
     */
    public List<RoadmapWithStatusDTO> getRoadmapsByProfession(Long professionId, Long lastId, String sortBy, UserDO currentUser) {
        validateProfessionId(professionId);

        // 默认按 score 排序
        if (sortBy == null || sortBy.isEmpty() || (!sortBy.equals("latest") && !sortBy.equals("score"))) {
            sortBy = "score";
        }

        int limit = systemProperties.getRoadmap().getDefaultPageSize();

        // 委托给 DomainService 查询路线图列表
        List<RoadmapDO> roadmapList = domainService.getRoadmapsByProfession(
            professionId, lastId, limit, sortBy);

        // 转换为完整DTO（包含跨域信息）
        return toRoadmapWithFullInfo(roadmapList, currentUser.getId(), professionId);
    }

    /**
     * 获取用户创建的路线图列表（所有状态）
     * @param userId 用户ID
     * @param lastId 分页游标
     * @return 路线图列表（包含专业信息）
     */
    public List<RoadmapDetailDTO> getUserRoadmaps(Long userId, Long lastId, ContentState state) {
        validateUserId(userId);

        int limit = systemProperties.getRoadmap().getDefaultPageSize();

        // 委托给 DomainService 查询
        List<RoadmapDO> roadmapList = domainService.getUserRoadmaps(
            userId, lastId, limit, state == null ? null : state.value());

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

        // 转换为完整DTO（包含跨域信息），professionId 为 null
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

        // 根据状态进行不同的内容格式验证
        if (state.equals(ContentState.DRAFT.value())) {
            // 草稿模式：验证基本格式只（允许孤立节点）
            if (!domainService.isValidContentBasicFormat(content)) {
                throw StatusCode.ROADMAP_CONTENT_INVALID.exception();
            }
        } else {
            // 提交审核模式：验证完整的树结构
            if (!domainService.isValidContentFormat(content)) {
                throw StatusCode.ROADMAP_CONTENT_INVALID.exception();
            }
        }

        // 计算节点数量
        Integer nodeCount = countNodesInContent(content);

        // 委托给 DomainService 更新内容和节点数量
        domainService.updateRoadmap(id, content, nodeCount);

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
    public Long createRoadmap(Long professionId, String content, String description, long userId, Byte state) {
        validateProfessionId(professionId);
        validateContent(content);
        validateUserId(userId);

        // 跨域验证：验证专业和用户存在
        professionDataService.validateExists(professionId);
        userDataService.validateExists(userId);

        // 验证状态：只能是草稿或提交审核
        if (!state.equals(ContentState.DRAFT.value()) && !state.equals(ContentState.SUBMITTED.value())) {
            throw StatusCode.INVALID_PARAMETER.exception("状态只能是草稿(0)或提交审核(1)");
        }

        // 验证内容格式
        if (state.equals(ContentState.DRAFT.value())) {
            // 草稿模式：验证基本格式和边的节点存在性（允许孤立节点）
            if (!domainService.isValidContentBasicFormat(content)) {
                throw StatusCode.ROADMAP_CONTENT_INVALID.exception();
            }
        } else {
            // 提交审核模式：验证完整的树结构
            if (!domainService.isValidContentFormat(content)) {
                throw StatusCode.ROADMAP_CONTENT_INVALID.exception();
            }
        }

        // 计算节点数量
        Integer nodeCount = countNodesInContent(content);

        // 委托给 DomainService
        return domainService.createRoadmap(professionId, content, description, userId, nodeCount, state);
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
    
    private void validateProfessionId(Long professionId) {
        if (professionId == null || professionId <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception();
        }
    }
    
    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw StatusCode.INVALID_PARAMETER.exception();
        }
    }

    private Set<Long> getLearningIds(long userId, List<Long> roadmapIds) {
        Map<Long, UserLearningDO> learningMap = userLearningDomainService.getBatch(userId, Enums.ContentType.roadmap, roadmapIds);
        return learningMap.keySet();
    }

    // ========== Admin管理方法 ==========

    /**
     * Admin管理：按条件获取路线图列表
     */
    public List<RoadmapSummaryDTO> listByFilter(ContentState state, Long professionId, Long creatorId, Long lastId) {
        List<RoadmapDO> roadmapDOList = domainService.listByFilter(state, professionId, creatorId, lastId);
        return toSummaryDTO(roadmapDOList);
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

        // 获取职业信息
        ProfessionDO profession = professionDataService.getById(roadmap.getProfessionId());

        // 发布审核通过事件，触发统计更新（不发送消息）
        eventPublisher.publishEvent(ContentApprovedEvent.forRoadmap(
            roadmap.getCreatorId(),
            roadmap.getId(),
            profession != null ? profession.getId() : null,
            profession != null ? profession.getName() : null
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

        // 获取职业信息用于通知
        ProfessionDO profession = professionDataService.getById(roadmap.getProfessionId());

        // 发布审核拒绝事件，触发消息通知
        eventPublisher.publishEvent(ContentRejectedEvent.forRoadmap(
            roadmap.getCreatorId(),
            roadmap.getId(),
            profession != null ? profession.getId() : null,
            profession != null ? profession.getName() : null,
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

        // 获取职业信息用于通知
        ProfessionDO profession = professionDataService.getById(roadmap.getProfessionId());

        // 委托给 DomainService 执行状态变更
        domainService.ban(id, reason);

        // 发布内容封禁事件，触发统计更新和消息通知
        eventPublisher.publishEvent(ContentBannedEvent.forRoadmap(
            roadmap.getCreatorId(),
            roadmap.getId(),
            previousState,
            roadmap.getProfessionId(),
            profession != null ? profession.getName() : null,
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

        // 获取职业信息用于通知
        ProfessionDO profession = professionDataService.getById(roadmap.getProfessionId());

        // 委托给 DomainService 执行状态变更
        domainService.reject(id, reason);

        // 发布内容下架事件，触发统计更新和消息通知
        eventPublisher.publishEvent(ContentRemovedEvent.forRoadmap(
            roadmap.getCreatorId(),
            roadmap.getId(),
            roadmap.getProfessionId(),
            profession != null ? profession.getName() : null,
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

        // 获取职业信息用于通知
        ProfessionDO profession = professionDataService.getById(roadmap.getProfessionId());

        // 委托给 DomainService 执行状态变更
        domainService.approve(id);

        // 发布内容恢复事件，触发统计恢复和消息通知
        eventPublisher.publishEvent(ContentRestoredEvent.forRoadmap(
            operator.getId(),  // operatorId
            roadmap.getCreatorId(),
            roadmap.getId(),
            previousState,
            roadmap.getProfessionId(),
            profession != null ? profession.getName() : null,
            reason
        ));

        log.info("路线图 {} 被恢复，操作者: {}, 原因: {}", id, operator.getId(), reason);

        roadmap.setState(ContentState.PUBLISHED.value());
        return toSummaryDTO(roadmap);
    }

// --注释掉检查 START (2025/12/10 11:24):
//    /**
//     * 清除描述并批准路线图
//     */
//    @Transactional
//    public RoadmapSummaryDTO approveAndClearDescription(long id, UserDO operator) {
//        RoadmapDO roadmap = roadmapDataService.getById(id);
//        if (roadmap == null) {
//            throw ErrorCode.ROADMAP_NOT_FOUND.exception();
//        }
//
//        // 委托给 DomainService
//        domainService.approveAndClearDescription(id);
//        roadmap.setDescription("");
//        roadmap.setState(ContentState.PUBLISHED.value());
//
//        return toSummaryDTO(roadmap);
//    }
// --注释掉检查 STOP (2025/12/10 11:24)

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
