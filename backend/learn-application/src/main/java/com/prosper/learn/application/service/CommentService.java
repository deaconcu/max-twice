package com.prosper.learn.application.service;

import com.prosper.learn.analytics.dto.ContentStatsDTO;
import com.prosper.learn.analytics.stats.dataservice.ContentStatsDataService;
import com.prosper.learn.analytics.stats.mapper.ContentStatsDO;
import com.prosper.learn.analytics.stats.service.ContentStatsDomainService;
import com.prosper.learn.application.converter.CommentConverter;
import com.prosper.learn.application.converter.UserConverter;
import com.prosper.learn.application.dto.request.CreateCommentRequest;
import com.prosper.learn.application.dto.response.comment.CommentDTO;
import com.prosper.learn.application.dto.response.KeysetPageResponse;
import com.prosper.learn.application.dto.response.comment.CommentAdminDTO;
import com.prosper.learn.application.dto.response.comment.CommentBasicDTO;
import com.prosper.learn.application.dto.response.comment.CommentContextDTO;
import com.prosper.learn.application.dto.response.comment.CommentDetailDTO;
import com.prosper.learn.application.dto.response.comment.CommentSummaryDTO;
import com.prosper.learn.application.dto.response.comment.CommentWithRepliesDTO;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.content.roadmap.RoadmapDO;
import com.prosper.learn.content.roadmap.RoadmapDataService;
import com.prosper.learn.interaction.comment.CommentDO;
import com.prosper.learn.interaction.comment.CommentDataService;
import com.prosper.learn.interaction.comment.CommentDomainService;
import com.prosper.learn.interaction.upvote.UpvoteDO;
import com.prosper.learn.interaction.upvote.UpvoteDataService;
import com.prosper.learn.shared.domain.event.content.lifecycle.CommentCreatedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.CommentDeletedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentApprovedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentBannedEvent;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.domain.Enums.*;
import static com.prosper.learn.shared.domain.Enums.ContentType.*;

/**
 * 评论应用服务
 * 负责协调跨领域逻辑、DTO转换、事件发布
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentDomainService commentDomainService;
    private final CommentDataService commentDataService;
    private final UserDataService userDataService;
    private final UpvoteDataService upvoteDataService;
    private final PostDataService postDataService;
    private final ContentVisibilityService contentVisibilityService;
    private final NodeDataService nodeDataService;
    private final RoadmapDataService roadmapDataService;
    private final SystemProperties systemProperties;
    private final CommentConverter commentConverter;
    private final UserConverter userConverter;
    private final ApplicationEventPublisher eventPublisher;
    private final ContentStatsDataService contentStatsDataService;
    private final ContentStatsDomainService contentStatsDomainService;

    // ========== Command 方法（写操作）==========

    /**
     * 创建评论，用户提交评论时调用
     * @param request 评论请求
     * @param commentor 评论用户
     * @return CommentDetailDTO 包含 toUserName 的评论详情
     */
    @Transactional
    public CommentDetailDTO createComment(CreateCommentRequest request, UserDO commentor) {
        // 验证参数
        if (request == null) {
            throw StatusCode.INVALID_PARAMETER.exception("评论请求不能为空");
        }

        validateCommentType(request.getObjectType());

        // 验证 toUser 用户是否存在（跨域验证）
        if (request.getToUser() != null && request.getToUser() > 0) {
            userDataService.validateAndGet(request.getToUser());
        }

        // 验证被评论的对象及其祖先链是否全部为 PUBLISHED
        ContentType parentType = ContentType.getByValue(request.getObjectType());
        contentVisibilityService.validateCanCreateOn(parentType, request.getObjectId());

        // 调用 DomainService 创建评论（包含 interaction 领域内的验证和业务逻辑）
        CommentDO savedComment = commentDomainService.createComment(
            request.getObjectId(),
            request.getObjectType(),
            request.getReplyTo(),
            request.getToUser(),
            request.getContent(),
            commentor.getId()
        );

        // 注意：评论创建时为待审核状态，不发送通知、不更新统计
        // 审核通过后会发布 CommentCreatedEvent，由事件监听器处理通知和统计

        // 转换为 DTO，填充 toUserName
        return toDetailDTO(savedComment, commentor.getId());
    }

    /**
     * 批准评论
     */
    @Transactional
    public void approve(Long id, UserDO operator) {
        // 调用 DomainService 审核通过
        CommentDO commentDO = commentDomainService.approveComment(id);

        // 获取被评论内容的类型
        ContentType commentTargetType = ContentType.getByValue(commentDO.getObjectType());

        // 发布审核通过事件，触发统计更新（评论不发送消息）
        eventPublisher.publishEvent(ContentApprovedEvent.forComment(
            commentDO.getCreatorId(),
            commentDO.getId(),
            commentTargetType,
            commentDO.getObjectId()
        ));

        // 获取被评论内容的创建者ID（跨域逻辑）
        Long contentCreatorId = getContentCreatorId(commentDO);

        // 发布评论创建事件，触发 Redis 统计更新、消息通知、分数计算等副作用
        eventPublisher.publishEvent(new CommentCreatedEvent(
            commentDO.getCreatorId(),  // 评论者ID
            commentDO.getId(),         // 评论ID
            commentDO.getObjectId(),   // 被评论内容ID
            commentTargetType,         // 内容类型
            contentCreatorId           // 被评论内容的创建者ID
        ));

        // 如果是回复评论，还需要为父评论增加 commentCount
        if (commentDO.getReplyToCommentId() != null && commentDO.getReplyToCommentId() > 0) {
            CommentDO parentComment = commentDataService.getById(commentDO.getReplyToCommentId());
            if (parentComment != null) {
                eventPublisher.publishEvent(new CommentCreatedEvent(
                    commentDO.getCreatorId(),           // 评论者ID
                    commentDO.getId(),                  // 评论ID
                    commentDO.getReplyToCommentId(),    // 父评论ID
                    ContentType.comment,                // 内容类型为 comment
                    parentComment.getCreatorId()        // 父评论的创建者ID
                ));
            }
        }
    }

    /**
     * 拒绝评论（审核不通过，带原因）
     * 只能拒绝 SUBMITTED 状态的评论，拒绝后状态变为 REJECTED
     * 不涉及统计回滚（因为从未发布）
     * 不发送通知
     */
    @Transactional
    public void reject(Long id, String reason, UserDO operator) {
        commentDomainService.rejectComment(id, reason);
        // 评论不发送拒绝通知
    }

    /**
     * 封禁评论（违规封禁，带原因）
     * 可以封禁任何状态的评论，封禁后状态变为 BANNED
     * 需要统计回滚（如果之前是 PUBLISHED 状态）
     * 不发送通知
     */
    @Transactional
    public void ban(Long id, String reason, UserDO operator) {
        CommentDO commentDO = commentDataService.getById(id);
        if (commentDO == null) {
            throw StatusCode.COMMENT_NOT_FOUND.exception();
        }

        // 记录之前的状态
        Byte previousState = commentDO.getState();

        // 获取被评论内容的类型
        ContentType commentTargetType = ContentType.getByValue(commentDO.getObjectType());

        // 调用 DomainService 执行封禁
        commentDomainService.banComment(id, reason);

        // 发布内容封禁事件，触发统计更新（评论不发送消息）
        eventPublisher.publishEvent(ContentBannedEvent.forComment(
            commentDO.getCreatorId(),
            commentDO.getId(),
            ContentState.getByValue(previousState),
            commentTargetType,
            commentDO.getObjectId(),
            reason
        ));

        // 如果是回复评论且之前状态是 PUBLISHED，需要减少父评论的 commentCount
        if (previousState == ContentState.PUBLISHED.value() &&
            commentDO.getReplyToCommentId() != null && commentDO.getReplyToCommentId() > 0) {
            CommentDO parentComment = commentDataService.getById(commentDO.getReplyToCommentId());
            if (parentComment != null) {
                eventPublisher.publishEvent(new CommentDeletedEvent(
                    operator.getId(),                   // 操作者ID
                    commentDO.getId(),                  // 评论ID
                    commentDO.getReplyToCommentId(),    // 父评论ID
                    ContentType.comment,                // 内容类型为 comment
                    parentComment.getCreatorId()        // 父评论的创建者ID
                ));
            }
        }
    }

    // ========== Query 方法（读操作）==========

    /**
     * 获取对象(post, node, comment ...)评论，分页查询，按分数和ID倒序排列
     * @param lastScore 上一页最后一条记录的分数，首页传null
     * @param lastId 上一页最后一条记录的ID，首页传null
     * @return KeysetPageResponse<CommentWithRepliesDTO> 包含子评论的评论列表
     */
    public KeysetPageResponse<CommentWithRepliesDTO> getCommentsByObject(Long objectId, Integer type, Double lastScore, Long lastId, UserDO currentUser) {
        validateCommentType(type);

        int pageSize = systemProperties.getComment().getDefaultPageSize();

        // 调用 DomainService 获取评论列表
        List<CommentDO> commentDOList = commentDomainService.getCommentsByObject(objectId, type, lastScore, lastId, pageSize);

        // 判断是否还有更多数据
        boolean hasMore = commentDOList.size() > pageSize;
        if (hasMore) {
            // 移除多查询的那一条
            commentDOList = commentDOList.subList(0, pageSize);
        }

        // DTO转换 + 填充点赞状态 + 填充用户名
        List<CommentWithRepliesDTO> commentDTOList = toCommentsWithReplies(commentDOList, currentUser.getId());

        // 构建响应
        if (commentDTOList.isEmpty()) {
            return KeysetPageResponse.of(commentDTOList, false, null, null);
        }

        CommentWithRepliesDTO lastComment = commentDTOList.get(commentDTOList.size() - 1);
        return KeysetPageResponse.of(commentDTOList, hasMore, lastComment.getScore(), lastComment.getId());
    }

    /**
     * 获取子评论列表，分页查询，按分数和ID倒序排列
     * @param lastScore 上一页最后一条记录的分数，首页传null
     * @param lastId 上一页最后一条记录的ID，首页传null
     * @return KeysetPageResponse<CommentDetailDTO> 不包含子评论的评论详情列表
     */
    public KeysetPageResponse<CommentDetailDTO> getCommentReplies(Long id, Double lastScore, Long lastId, UserDO currentUser) {
        int pageSize = systemProperties.getComment().getDefaultPageSize();

        // 调用 DomainService 获取子评论列表
        List<CommentDO> commentDOList = commentDomainService.getCommentReplies(id, lastScore, lastId, pageSize);

        // 判断是否还有更多数据
        boolean hasMore = commentDOList.size() > pageSize;
        if (hasMore) {
            // 移除多查询的那一条
            commentDOList = commentDOList.subList(0, pageSize);
        }

        // DTO转换
        List<CommentDetailDTO> commentDTOList = commentConverter.toDetailDTO(commentDOList);

        // 填充统计字段
        fillStatsForComments(commentDTOList);

        // 填充用户名
        fillUserNamesForComments(commentDTOList);

        // 填充点赞状态
        fillUpvoteStatusForComments(commentDTOList, currentUser.getId());

        // 构建响应
        if (commentDTOList.isEmpty()) {
            return KeysetPageResponse.of(commentDTOList, false, null, null);
        }

        CommentDetailDTO lastComment = commentDTOList.get(commentDTOList.size() - 1);
        return KeysetPageResponse.of(commentDTOList, hasMore, lastComment.getScore(), lastComment.getId());
    }

    /**
     * 根据状态获取评论列表（分页），按ID倒序排列（越新的越靠前）
     * 仅管理员调用
     * @param state pending(待审核), approved(已通过), rejected(已拒绝), banned(已封禁)
     * @param lastId 最后一条记录的ID，为null时加载第一页
     * @return List<CommentAdminDTO> 管理员视图（含审核原因）
     */
    public List<CommentAdminDTO> getCommentsByState(String state, Long lastId) {
        int pageSize = systemProperties.getComment().getDefaultPageSize();

        // 调用 DomainService 获取评论列表（包含状态转换逻辑）
        List<CommentDO> commentDOList = commentDomainService.getCommentsByState(state, lastId, pageSize);

        // DTO转换
        return commentConverter.toAdminDTO(commentDOList);
    }

    /**
     * 管理后台：按状态获取评论列表
     */
    public KeysetPageResponse<CommentAdminDTO> listByState(ContentState state, Long lastId) {
        int pageSize = systemProperties.getComment().getDefaultPageSize();
        Byte stateValue = state != null ? state.value() : null;
        List<CommentDO> commentDOList = commentDomainService.listByState(stateValue, lastId, pageSize + 1);
        return buildAdminResponse(commentDOList, pageSize);
    }

    /**
     * 管理后台：高级筛选评论列表
     */
    public KeysetPageResponse<CommentAdminDTO> listByFilter(Integer objectType, Long objectId, Long creatorId, Long lastId) {
        int pageSize = systemProperties.getComment().getDefaultPageSize();
        List<CommentDO> commentDOList = commentDomainService.listByFilter(objectType, objectId, creatorId, lastId, pageSize + 1);
        return buildAdminResponse(commentDOList, pageSize);
    }

    private KeysetPageResponse<CommentAdminDTO> buildAdminResponse(List<CommentDO> commentDOList, int pageSize) {
        boolean hasMore = commentDOList.size() > pageSize;
        if (hasMore) {
            commentDOList = commentDOList.subList(0, pageSize);
        }

        List<CommentAdminDTO> items = commentConverter.toAdminDTO(commentDOList);

        // 批量填充 creator 和 toUser 信息
        Set<Long> userIds = new HashSet<>();
        commentDOList.forEach(c -> {
            if (c.getCreatorId() != null) userIds.add(c.getCreatorId());
            if (c.getToUserId() != null) userIds.add(c.getToUserId());
        });
        if (!userIds.isEmpty()) {
            Map<Long, UserDO> userMap = userDataService.getMapByIds(userIds);
            Map<Long, Long> commentCreatorMap = commentDOList.stream()
                    .collect(Collectors.toMap(CommentDO::getId, CommentDO::getCreatorId));
            Map<Long, Long> commentToUserMap = commentDOList.stream()
                    .filter(c -> c.getToUserId() != null)
                    .collect(Collectors.toMap(CommentDO::getId, CommentDO::getToUserId));
            for (CommentAdminDTO dto : items) {
                Long creatorId = commentCreatorMap.get(dto.getId());
                if (creatorId != null) {
                    UserDO user = userMap.get(creatorId);
                    if (user != null) {
                        dto.setCreator(userConverter.toBriefDTO(user));
                    }
                }
                Long toUserId = commentToUserMap.get(dto.getId());
                if (toUserId != null) {
                    UserDO toUser = userMap.get(toUserId);
                    if (toUser != null) {
                        dto.setToUser(userConverter.toBriefDTO(toUser));
                    }
                }
            }
        }

        // 批量填充统计数据
        List<Long> commentIds = commentDOList.stream().map(CommentDO::getId).collect(Collectors.toList());
        if (!commentIds.isEmpty()) {
            List<ContentStatsDO> statsList = contentStatsDataService.batchGetByContentIds(comment, commentIds);
            Map<Long, ContentStatsDO> statsMap = statsList.stream()
                    .collect(Collectors.toMap(ContentStatsDO::getContentId, s -> s));
            for (CommentAdminDTO dto : items) {
                ContentStatsDO stats = statsMap.get(dto.getId());
                if (stats != null) {
                    dto.setRejectCount(stats.getRejectCount());
                }
            }
        }

        Long nextLastId = hasMore && !items.isEmpty() ? items.get(items.size() - 1).getId() : null;

        return KeysetPageResponse.of(items, hasMore, null, nextLastId);
    }

    // ========== DTO 转换方法 ==========

    /**
     * 单个 CommentDO 转换为 CommentDTO，默认方法（保留兼容性）
     * @deprecated 请使用具体的转换方法
     */
    @Deprecated
    public CommentDTO toDTO(CommentDO commentDO) {
        return commentConverter.toDTO(commentDO);
    }

    /**
     * 给commentDTOList查询子评论并填充，每条评论只填充一条子评论，选取分数最高的子评论，并且填充liked状态位
     * 转换为 CommentWithRepliesDTO（含子评论+点赞状态+用户名）
     *
     * @param commentDOList 评论DO列表
     * @param userId 当前用户ID
     * @return 包含子评论的评论DTO列表
     */
    private List<CommentWithRepliesDTO> toCommentsWithReplies(List<CommentDO> commentDOList, Long userId) {
        // 转换为 DTO
        List<CommentWithRepliesDTO> commentDTOList = commentConverter.toWithRepliesDTO(commentDOList);

        // 收集父评论ID
        List<Long> parentIds = commentDTOList.stream()
                .map(CommentWithRepliesDTO::getId)
                .toList();

        // 调用 DomainService 获取所有子评论
        List<CommentDO> children = commentDomainService.getChildren(parentIds);
        List<CommentDetailDTO> childrenDTOList = commentConverter.toDetailDTO(children);

        // 合并所有评论（父+子）
        List<CommentDetailDTO> allComments = new ArrayList<>(commentDTOList);
        allComments.addAll(childrenDTOList);

        // 统一填充统计字段
        fillStatsForComments(allComments);

        // 统一填充用户名
        fillUserNamesForComments(allComments);

        // 统一填充点赞状态
        fillUpvoteStatusForComments(allComments, userId);

        // 按父评论ID分组子评论
        Map<Long, List<CommentDetailDTO>> childrenMap = new HashMap<>();
        for (CommentDetailDTO childDTO : childrenDTOList) {
            Long parentId = childDTO.getReplyToCommentId();
            if (parentId != null && parentId > 0) {
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(childDTO);
            }
        }

        // 组织父子关系
        for (CommentWithRepliesDTO commentDTO : commentDTOList) {
            List<CommentDetailDTO> childrenList = childrenMap.getOrDefault(commentDTO.getId(), new ArrayList<>());
            commentDTO.setChildren(childrenList);
        }

        return commentDTOList;
    }

    /**
     * 批量填充评论的统计字段（包含 Redis 增量）
     */
    private void fillStatsForComments(List<? extends CommentSummaryDTO> commentDTOList) {
        if (commentDTOList == null || commentDTOList.isEmpty()) {
            return;
        }

        // 收集所有评论ID
        List<Long> commentIds = commentDTOList.stream()
                .map(CommentSummaryDTO::getId)
                .toList();

        // 批量查询统计数据（包含 Redis 增量）
        Map<Long, ContentStatsDTO> statsMap = contentStatsDomainService.batchGetContentStats(comment, commentIds);

        // 填充每个评论的统计字段
        for (CommentSummaryDTO commentDTO : commentDTOList) {
            ContentStatsDTO stats = statsMap.get(commentDTO.getId());
            if (stats != null) {
                commentDTO.setLikeCount(stats.getLikeCount() != null ? stats.getLikeCount() : 0);
                commentDTO.setReplyCount(stats.getCommentCount() != null ? stats.getCommentCount() : 0);
            } else {
                // 没有统计记录，设置为0
                commentDTO.setLikeCount(0);
                commentDTO.setReplyCount(0);
            }
        }
    }

    /**
     * 批量填充评论的用户信息（creator 和 toUser）
     */
    private void fillUserNamesForComments(List<? extends CommentDetailDTO> commentDTOList) {
        if (commentDTOList == null || commentDTOList.isEmpty()) {
            return;
        }

        // 收集所有需要查询的用户ID
        Set<Long> userIds = new HashSet<>();
        for (CommentDetailDTO commentDTO : commentDTOList) {
            if (commentDTO.getCreatorId() != null && commentDTO.getCreatorId() > 0) {
                userIds.add(commentDTO.getCreatorId());
            }
            if (commentDTO.getToUserId() != null && commentDTO.getToUserId() > 0) {
                userIds.add(commentDTO.getToUserId());
            }
        }

        if (userIds.isEmpty()) {
            return;
        }

        // 批量查询用户信息
        Map<Long, UserDO> userMap = userDataService.getMapByIds(new ArrayList<>(userIds));

        // 填充用户信息
        for (CommentDetailDTO commentDTO : commentDTOList) {
            if (commentDTO.getCreatorId() != null && commentDTO.getCreatorId() > 0) {
                UserDO creator = userMap.get(commentDTO.getCreatorId());
                if (creator != null) {
                    commentDTO.setCreator(userConverter.toBriefDTO(creator));
                }
            }
            if (commentDTO.getToUserId() != null && commentDTO.getToUserId() > 0) {
                UserDO toUser = userMap.get(commentDTO.getToUserId());
                if (toUser != null) {
                    commentDTO.setToUser(userConverter.toBriefDTO(toUser));
                }
            }
        }
    }

    /**
     * 批量填充评论的点赞状态
     */
    private void fillUpvoteStatusForComments(List<? extends CommentDetailDTO> commentDTOList, Long userId) {
        if (commentDTOList == null || commentDTOList.isEmpty()) {
            return;
        }

        // 收集所有评论ID
        List<Long> commentIds = commentDTOList.stream()
                .map(CommentDetailDTO::getId)
                .toList();

        // 批量查询点赞记录
        List<UpvoteDO> upvoteList = upvoteDataService.getList(userId, commentIds, comment.value());
        Set<Long> upvotedSet = upvoteList.stream()
                .filter(upvote -> upvote.getType() == VoteType.like.value())
                .map(UpvoteDO::getObjectId)
                .collect(Collectors.toSet());

        // 填充点赞状态
        for (CommentDetailDTO commentDTO : commentDTOList) {
            commentDTO.setLiked(upvotedSet.contains(commentDTO.getId()));
        }
    }

    /**
     * 转换单个评论为 DTO 并填充用户信息（用于创建评论后返回）
     * 转换为 CommentDetailDTO（用户信息）
     *
     * @param commentDO 评论DO
     * @return 填充了用户信息的评论DTO
     */
    private CommentDetailDTO toDetailDTO(CommentDO commentDO, long userId) {
        CommentDetailDTO commentDTO = commentConverter.toDetailDTO(commentDO);

        commentDTO.setLikeCount(0);
        commentDTO.setReplyCount(0);

        // 填充统计字段（包含 Redis 增量）
        ContentStatsDTO stats = contentStatsDomainService.getContentStats(comment, commentDO.getId());
        if (stats != null) {
            commentDTO.setLikeCount(stats.getLikeCount() != null ? stats.getLikeCount() : 0);
            commentDTO.setReplyCount(stats.getCommentCount() != null ? stats.getCommentCount() : 0);
        }

        // 填充点赞状态
        commentDTO.setLiked(false);
        UpvoteDO upvoteDO = upvoteDataService.getByUserAndObject(userId, commentDO.getId(), comment.value());
        if (upvoteDO != null && upvoteDO.getType() == VoteType.like.value()) {
            commentDTO.setLiked(true);
        }

        // 批量查询用户信息（creator 和 toUser）
        List<Long> userIds = new ArrayList<>();
        if (commentDTO.getCreatorId() != null && commentDTO.getCreatorId() > 0) {
            userIds.add(commentDTO.getCreatorId());
        }
        if (commentDTO.getToUserId() != null && commentDTO.getToUserId() > 0) {
            userIds.add(commentDTO.getToUserId());
        }

        if (!userIds.isEmpty()) {
            Map<Long, UserDO> userMap = userDataService.getMapByIds(userIds);

            // 填充 creator
            if (commentDTO.getCreatorId() != null && commentDTO.getCreatorId() > 0) {
                UserDO creator = userMap.get(commentDTO.getCreatorId());
                if (creator != null) {
                    commentDTO.setCreator(userConverter.toBriefDTO(creator));
                }
            }

            // 填充 toUser
            if (commentDTO.getToUserId() != null && commentDTO.getToUserId() > 0) {
                UserDO toUser = userMap.get(commentDTO.getToUserId());
                if (toUser != null) {
                    commentDTO.setToUser(userConverter.toBriefDTO(toUser));
                }
            }
        }

        return commentDTO;
    }

    /**
     * 转换为完整的 CommentSummaryDTO（包含统计字段）
     * 用于需要返回基础统计信息的场景
     */
    private CommentSummaryDTO toSummaryDTO(CommentDO commentDO) {
        CommentSummaryDTO commentDTO = commentConverter.toSummaryDTO(commentDO);
        commentDTO.setLikeCount(0);
        commentDTO.setReplyCount(0);

        // 填充统计字段（包含 Redis 增量）
        ContentStatsDTO stats = contentStatsDomainService.getContentStats(comment, commentDO.getId());
        if (stats != null) {
            commentDTO.setLikeCount(stats.getLikeCount() != null ? stats.getLikeCount() : 0);
            commentDTO.setReplyCount(stats.getCommentCount() != null ? stats.getCommentCount() : 0);
        }

        return commentDTO;
    }

    // ========== Private 辅助方法 ==========

    /**
     * 验证评论类型
     */
    private void validateCommentType(int type) {
        if (type != post.value() &&
            type != node.value() &&
            type != roadmap.value()) {
            throw StatusCode.COMMENT_INVALID_TYPE.exception();
        }
    }

    /**
     * 获取被评论内容的创建者ID（跨域查询）
     */
    private Long getContentCreatorId(CommentDO commentDO) {
        if (commentDO.getObjectType() == post.value()) {
            PostDO postDO = postDataService.getById(commentDO.getObjectId());
            return postDO != null ? postDO.getCreatorId() : null;
        } else if (commentDO.getObjectType() == node.value()) {
            NodeDO nodeDO = nodeDataService.getById(commentDO.getObjectId());
            return nodeDO != null ? nodeDO.getCreatorId() : null;
        } else if (commentDO.getObjectType() == roadmap.value()) {
            RoadmapDO roadmapDO = roadmapDataService.getById(commentDO.getObjectId());
            return roadmapDO != null ? roadmapDO.getCreatorId() : null;
        }
        return null;
    }

    // ========== 评论上下文查询 ==========

    private static final int CONTEXT_BEFORE_COUNT = 5;
    private static final int CONTEXT_AFTER_COUNT = 5;

    /**
     * 获取评论上下文（目标评论及其前后评论）
     * 用于从外部链接跳转到特定评论
     *
     * 如果目标评论是主评论：返回主评论上下文（items）
     * 如果目标评论是子评论：返回子评论上下文（subItems）+ 父评论ID
     *
     * @param commentId 目标评论ID
     * @param currentUser 当前用户
     * @return CommentContextDTO 包含评论列表、目标评论ID、是否有更多等信息
     */
    public CommentContextDTO getCommentContext(Long commentId, UserDO currentUser) {
        // 获取目标评论
        CommentDO targetComment = commentDataService.validateAndGet(commentId);

        // 检查评论及其父内容的可见性
        contentVisibilityService.validateVisibility(comment, commentId, currentUser.getId());

        // 判断是主评论还是子评论
        if (targetComment.getReplyToCommentId() != null && targetComment.getReplyToCommentId() > 0) {
            // 子评论：返回子评论上下文
            return getSubCommentContext(targetComment, currentUser);
        } else {
            // 主评论：返回主评论上下文
            return getMainCommentContext(targetComment, currentUser);
        }
    }

    /**
     * 获取主评论上下文
     */
    private CommentContextDTO getMainCommentContext(CommentDO targetComment, UserDO currentUser) {
        // 获取评论上下文
        CommentDomainService.CommentContextResult contextResult = commentDomainService.getCommentContext(
            targetComment, CONTEXT_BEFORE_COUNT, CONTEXT_AFTER_COUNT);

        // 转换为带子评论的 DTO
        List<CommentWithRepliesDTO> commentDTOList = toCommentsWithReplies(contextResult.comments, currentUser.getId());

        // 构建响应
        CommentContextDTO dto = new CommentContextDTO();
        dto.setItems(commentDTOList);
        dto.setTargetCommentId(targetComment.getId());
        dto.setHasMoreBefore(contextResult.hasMoreBefore);
        dto.setHasMoreAfter(contextResult.hasMoreAfter);

        // 设置游标
        if (!commentDTOList.isEmpty()) {
            CommentWithRepliesDTO first = commentDTOList.get(0);
            dto.setFirstScore(first.getScore());
            dto.setFirstId(first.getId());

            CommentWithRepliesDTO last = commentDTOList.get(commentDTOList.size() - 1);
            dto.setLastScore(last.getScore());
            dto.setLastId(last.getId());
        }

        return dto;
    }

    /**
     * 获取子评论上下文
     */
    private CommentContextDTO getSubCommentContext(CommentDO targetSubComment, UserDO currentUser) {
        // 获取子评论上下文
        CommentDomainService.CommentContextResult contextResult = commentDomainService.getSubCommentContext(
            targetSubComment, CONTEXT_BEFORE_COUNT, CONTEXT_AFTER_COUNT);

        // 转换为 DTO
        List<CommentDetailDTO> subCommentDTOList = commentConverter.toDetailDTO(contextResult.comments);

        // 填充统计字段、用户名、点赞状态
        fillStatsForComments(subCommentDTOList);
        fillUserNamesForComments(subCommentDTOList);
        fillUpvoteStatusForComments(subCommentDTOList, currentUser.getId());

        // 构建响应
        CommentContextDTO dto = new CommentContextDTO();
        dto.setSubItems(subCommentDTOList);
        dto.setTargetCommentId(targetSubComment.getId());
        dto.setParentCommentId(targetSubComment.getReplyToCommentId());
        dto.setHasMoreBefore(contextResult.hasMoreBefore);
        dto.setHasMoreAfter(contextResult.hasMoreAfter);

        // 设置游标
        if (!subCommentDTOList.isEmpty()) {
            CommentDetailDTO first = subCommentDTOList.get(0);
            dto.setFirstScore(first.getScore());
            dto.setFirstId(first.getId());

            CommentDetailDTO last = subCommentDTOList.get(subCommentDTOList.size() - 1);
            dto.setLastScore(last.getScore());
            dto.setLastId(last.getId());
        }

        return dto;
    }

    /**
     * 获取评论基本信息
     */
    public CommentBasicDTO getCommentBasic(Long commentId, Long currentUserId) {
        CommentDO commentDO = commentDataService.validateAndGet(commentId);

        // 检查评论及其父内容的可见性
        contentVisibilityService.validateVisibility(comment, commentId, currentUserId);

        CommentBasicDTO dto = new CommentBasicDTO();
        dto.setId(commentDO.getId());
        dto.setObjectType(commentDO.getObjectType());
        dto.setObjectId(commentDO.getObjectId());
        dto.setReplyToCommentId(commentDO.getReplyToCommentId());
        return dto;
    }
}
