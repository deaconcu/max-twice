package com.prosper.learn.application.service;

import com.prosper.learn.application.converter.CommentConverter;
import com.prosper.learn.application.dto.request.CreateCommentRequest;
import com.prosper.learn.application.dto.response.CommentDTO;
import com.prosper.learn.application.dto.response.KeysetPageResponse;
import com.prosper.learn.application.dto.response.comment.CommentAdminDTO;
import com.prosper.learn.application.dto.response.comment.CommentDetailDTO;
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
import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.domain.event.content.lifecycle.CommentCreatedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentApprovedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentBannedEvent;
import com.prosper.learn.shared.domain.exception.ErrorCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
    private final UserDataService userDataService;
    private final UpvoteDataService upvoteDataService;
    private final PostDataService postDataService;
    private final NodeDataService nodeDataService;
    private final RoadmapDataService roadmapDataService;
    private final SystemProperties systemProperties;
    private final CommentConverter commentConverter;
    private final ApplicationEventPublisher eventPublisher;

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
            throw ErrorCode.INVALID_PARAMETER.exception("评论请求不能为空");
        }

        validateCommentType(request.getObjectType());

        // 验证 toUser 用户是否存在（跨域验证）
        if (request.getToUser() != null && request.getToUser() > 0) {
            userDataService.validateAndGet(request.getToUser());
        }

        // 验证被评论的对象是否存在（跨域验证）
        if (request.getObjectType() == post.value()) {
            postDataService.validateAndGet(request.getObjectId());
        } else if (request.getObjectType() == node.value()) {
            nodeDataService.validateAndGet(request.getObjectId());
        } else if (request.getObjectType() == roadmap.value()) {
            roadmapDataService.validateAndGet(request.getObjectId());
        } else {
            throw ErrorCode.COMMENT_INVALID_TYPE.exception();
        }

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
        return toCommentWithUserNames(savedComment);
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
        CommentDO commentDO = commentDomainService.getById(id);
        if (commentDO == null) {
            throw ErrorCode.COMMENT_NOT_FOUND.exception();
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
            previousState,
            commentTargetType,
            commentDO.getObjectId(),
            reason
        ));
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

        // DTO转换 + 填充点赞状态 + 填充用户名
        List<CommentDetailDTO> commentDTOList = commentConverter.toDetailDTO(commentDOList);
        commentDTOList = toCommentsWithVoteStatus(commentDTOList, currentUser.getId());

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
     * 根据对象类型、对象ID、创建者和状态筛选评论列表
     * 仅管理员调用
     * @return List<CommentAdminDTO> 管理员视图（含审核原因）
     */
    public List<CommentAdminDTO> getCommentsByFilter(Integer objectType, Long objectId, Long creatorId, Long lastId, Byte state) {
        int pageSize = systemProperties.getComment().getDefaultPageSize();

        // 调用 DomainService 获取评论列表
        List<CommentDO> commentDOList = commentDomainService.getCommentsByFilter(objectType, objectId, creatorId, lastId, state, pageSize);

        // DTO转换
        return commentConverter.toAdminDTO(commentDOList);
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
     * 给commentDTOList查询子评论并填充，每条评论只填充一条子评论，选取分数最高的子评论，并且填充upvoted状态位
     * 转换为 CommentWithRepliesDTO（含子评论+点赞状态+用户名）
     *
     * @param commentDOList 评论DO列表
     * @param userId 当前用户ID
     * @return 包含子评论的评论DTO列表
     */
    private List<CommentWithRepliesDTO> toCommentsWithReplies(List<CommentDO> commentDOList, Long userId) {
        List<Long> ids = new ArrayList<>();
        for (CommentDO commentDO : commentDOList) {
            ids.add(commentDO.getId());
        }

        List<CommentWithRepliesDTO> commentDTOList = commentConverter.toWithRepliesDTO(commentDOList);

        // 调用 DomainService 获取子评论
        List<CommentDO> children = commentDomainService.getChildren(ids);

        HashMap<Long, CommentDetailDTO> map = new HashMap<>();
        for (CommentDO commentDO : children) {
            map.put(commentDO.getReplyToCommentId(), commentConverter.toDetailDTO(commentDO));
        }

        for (CommentWithRepliesDTO commentDTO: commentDTOList) {
            if (map.containsKey(commentDTO.getId())) {
                CommentDetailDTO childCommentDTO = map.get(commentDTO.getId());
                ids.add(childCommentDTO.getId());
                // 设置子评论列表
                List<CommentDetailDTO> childrenList = new ArrayList<>();
                childrenList.add(childCommentDTO);
                commentDTO.setChildren(childrenList);
            }
        }

        fillUserNames(commentDTOList, map);

        return processUpvoteStatusWithChildren(commentDTOList, map, userId);
    }

    /**
     * 给查询到的评论列表设置点赞状态 upvoted
     * 转换为 CommentDetailDTO（点赞状态+用户名）
     * 在加载更多子评论时调用，因为它已经是子评论了，所以它没有子评论列表
     *
     * @param commentDTOList 评论DTO列表
     * @param userId 当前用户ID
     * @return 填充了点赞状态和用户名的评论DTO列表
     */
    private List<CommentDetailDTO> toCommentsWithVoteStatus(List<CommentDetailDTO> commentDTOList, Long userId) {
        if (commentDTOList.isEmpty()) {
            return commentDTOList;
        }

        List<Long> ids = commentDTOList.stream().map(CommentDetailDTO::getId).toList();
        List<UpvoteDO> upvoteList = upvoteDataService.getList(userId, ids, comment.value());

        Set<Long> upvotedSet = new HashSet<>();
        for (UpvoteDO upvoteDO : upvoteList) {
            upvotedSet.add(upvoteDO.getObjectId());
        }

        for (CommentDetailDTO commentDTO : commentDTOList) {
            commentDTO.setUpvoted(upvotedSet.contains(commentDTO.getId()));
        }

        // 填充用户名（传入空map表示没有子评论）
        fillUserNames(commentDTOList, new HashMap<>());

        return commentDTOList;
    }

    /**
     * 转换单个评论为 DTO 并填充 toUserName（用于创建评论后返回）
     * 转换为 CommentDetailDTO（用户名）
     *
     * @param commentDO 评论DO
     * @return 填充了用户名的评论DTO
     */
    private CommentDetailDTO toCommentWithUserNames(CommentDO commentDO) {
        CommentDetailDTO commentDTO = commentConverter.toDetailDTO(commentDO);

        // 填充 toUserName
        if (commentDTO.getToUserId() != null && commentDTO.getToUserId() > 0) {
            UserDO toUser = userDataService.getById(commentDTO.getToUserId());
            if (toUser != null) {
                commentDTO.setToUserName(toUser.getName());
            }
        }

        return commentDTO;
    }

    /**
     * 给查询到的评论列表及其子评论设置点赞状态 upvoted
     * @param commentDTOList 父评论列表
     * @param childrenMap 子评论映射（parentId -> childDTO）
     * @param userId 当前用户ID
     * @return 填充了点赞状态的父评论列表
     */
    private List<CommentWithRepliesDTO> processUpvoteStatusWithChildren(
            List<CommentWithRepliesDTO> commentDTOList, HashMap<Long, CommentDetailDTO> childrenMap, Long userId) {
        List<Long> allIds = new ArrayList<>();

        // 收集所有评论ID（父评论和子评论）
        for (CommentWithRepliesDTO commentDTO : commentDTOList) {
            allIds.add(commentDTO.getId());
        }
        allIds.addAll(childrenMap.values().stream().map(CommentDetailDTO::getId).toList());

        if (allIds.isEmpty()) {
            return commentDTOList;
        }

        List<UpvoteDO> upvoteList = upvoteDataService.getList(userId, allIds, comment.value());

        Set<Long> upvotedSet = new HashSet<>();
        for (UpvoteDO upvoteDO : upvoteList) {
            upvotedSet.add(upvoteDO.getObjectId());
        }

        // 设置父评论点赞状态
        for (CommentWithRepliesDTO commentDTO : commentDTOList) {
            commentDTO.setUpvoted(upvotedSet.contains(commentDTO.getId()));
        }

        // 设置子评论点赞状态
        for (CommentDetailDTO commentDTO: childrenMap.values()) {
            commentDTO.setUpvoted(upvotedSet.contains(commentDTO.getId()));
        }
        return commentDTOList;
    }

    /**
     * 填充评论列表中的 creatorName 和 toUserName 字段
     * 通用方法，支持 CommentDetailDTO 及其子类（CommentWithRepliesDTO）
     *
     * @param commentDTOList 评论列表（父评论）
     * @param childrenMap 子评论映射（parentId -> childDTO），如果没有子评论传空map
     */
    private void fillUserNames(List<? extends CommentDetailDTO> commentDTOList, HashMap<Long, CommentDetailDTO> childrenMap) {
        Set<Long> userIds = new HashSet<>();

        // 收集所有需要查询的用户ID（包括 creatorId 和 toUserId）
        for (CommentDetailDTO commentDTO : commentDTOList) {
            if (commentDTO.getCreatorId() != null && commentDTO.getCreatorId() > 0) {
                userIds.add(commentDTO.getCreatorId());
            }
            if (commentDTO.getToUserId() != null && commentDTO.getToUserId() > 0) {
                userIds.add(commentDTO.getToUserId());
            }
        }

        for (CommentDetailDTO commentDTO : childrenMap.values()) {
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

        Map<Long, UserDO> userMap = userDataService.getMapByIds(userIds);

        // 填充 creatorName 和 toUserName
        for (CommentDetailDTO commentDTO : commentDTOList) {
            if (commentDTO.getCreatorId() != null && commentDTO.getCreatorId() > 0 && userMap.containsKey(commentDTO.getCreatorId())) {
                UserDO creator = userMap.get(commentDTO.getCreatorId());
                commentDTO.setCreatorName(creator.getName());
            }
            if (commentDTO.getToUserId() != null && commentDTO.getToUserId() > 0 && userMap.containsKey(commentDTO.getToUserId())) {
                UserDO user = userMap.get(commentDTO.getToUserId());
                commentDTO.setToUserName(user.getName());
            }
        }

        for (CommentDetailDTO commentDTO : childrenMap.values()) {
            if (commentDTO.getCreatorId() != null && commentDTO.getCreatorId() > 0 && userMap.containsKey(commentDTO.getCreatorId())) {
                UserDO creator = userMap.get(commentDTO.getCreatorId());
                commentDTO.setCreatorName(creator.getName());
            }
            if (commentDTO.getToUserId() != null && commentDTO.getToUserId() > 0 && userMap.containsKey(commentDTO.getToUserId())) {
                UserDO user = userMap.get(commentDTO.getToUserId());
                commentDTO.setToUserName(user.getName());
            }
        }
    }

    // ========== Private 辅助方法 ==========

    /**
     * 验证评论类型
     */
    private void validateCommentType(int type) {
        if (type != post.value() &&
            type != node.value() &&
            type != roadmap.value()) {
            throw ErrorCode.COMMENT_INVALID_TYPE.exception();
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
}
