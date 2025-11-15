package com.prosper.learn.domain.service.business;

import com.prosper.learn.common.Enums;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.common.config.SystemProperties;
import com.prosper.learn.domain.service.basic.MessageService;
import com.prosper.learn.domain.service.basic.RedisStatsService;
import com.prosper.learn.domain.service.basic.ScoreCalculationService;
import com.prosper.learn.domain.util.converter.CommentConverter;
import com.prosper.learn.dto.request.CreateCommentRequest;
import com.prosper.learn.dto.response.CommentDTO;
import com.prosper.learn.dto.response.KeysetPageResponse;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.domain.service.data.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.prosper.learn.common.Enums.MessageType.*;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final UserDataService userDataService;
    private final UpvoteDataService upvoteDataService;
    private final CommentDataService commentDataService;
    private final PostDataService postDataService;
    private final NodeDataService nodeDataService;
    private final RoadmapDataService roadmapDataService;
    private final MessageService messageService;
    private final ScoreCalculationService scoreCalculationService;
    private final RedisStatsService redisStatsService;
    private final SystemProperties systemProperties;
    private final CommentConverter commentConverter;

    // ========== 私有验证方法 ==========
    
    /**
     * 验证用户ID
     */
    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    /**
     * 验证对象ID
     */
    private void validateObjectId(Long objectId) {
        if (objectId == null || objectId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    /**
     * 验证评论ID
     */
    private void validateCommentId(Long commentId) {
        if (commentId == null || commentId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }

    /**
     * 验证评论类型
     */
    private void validateCommentType(int type) {
        if (type != Enums.ContentType.post.value() &&
            type != Enums.ContentType.node.value() &&
            type != Enums.ContentType.roadmap.value()) {
            throw ErrorCode.COMMENT_INVALID_TYPE.exception();
        }
    }
    
    /**
     * 验证并获取帖子
     */
    private PostDO validateAndGetPost(Long postId) {
        validateObjectId(postId);
        PostDO postDO = postDataService.getById(postId);
        if (postDO == null) {
            throw ErrorCode.CONTENTS_POST_NOT_FOUND.exception();
        }
        return postDO;
    }
    
    /**
     * 验证并获取节点
     */
    private NodeDO validateAndGetNode(Long nodeId) {
        validateObjectId(nodeId);
        NodeDO nodeDO = nodeDataService.getById(nodeId);
        if (nodeDO == null) {
            throw ErrorCode.COMMENT_OBJECT_NOT_FOUND.exception();
        }
        return nodeDO;
    }
    
    /**
     * 验证并获取路线图
     */
    private RoadmapDO validateAndGetRoadmap(Long roadmapId) {
        validateObjectId(roadmapId);
        RoadmapDO roadmapDO = roadmapDataService.getById(roadmapId);
        if (roadmapDO == null) {
            throw ErrorCode.ROADMAP_NOT_FOUND.exception();
        }
        return roadmapDO;
    }
    
    /**
     * 验证并获取评论
     */
    private CommentDO validateAndGetComment(Long commentId) {
        validateCommentId(commentId);
        CommentDO commentDO = commentDataService.getById(commentId);
        if (commentDO == null) {
            throw ErrorCode.COMMENT_NOT_FOUND.exception();
        }
        return commentDO;
    }
    
    /**
     * 验证并获取用户
     */
    private UserDO validateAndGetUser(Long userId) {
        validateUserId(userId);
        UserDO userDO = userDataService.getById(userId);
        if (userDO == null) {
            throw ErrorCode.USER_NOT_FOUND.exception();
        }
        return userDO;
    }

    /**
     * 创建评论，用户提交评论时调用
     * @param request 评论请求
     * @param commentor 评论用户
     * @return
     */
    @Transactional
    public CommentDTO createComment(CreateCommentRequest request, UserDO commentor) {
        // 先验证参数
        if (request == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("评论请求不能为空");
        }

        validateObjectId(request.getObjectId());
        validateCommentType(request.getObjectType());

        // 验证 replyTo 评论是否存在
        if (request.getReplyTo() != null && request.getReplyTo() > 0) {
            CommentDO replyToComment = validateAndGetComment(request.getReplyTo());
            // 确保回复的评论属于同一个对象
            if (!replyToComment.getObjectId().equals(request.getObjectId()) ||
                !replyToComment.getObjectType().equals(request.getObjectType())) {
                throw ErrorCode.INVALID_PARAMETER.exception("回复的评论不属于当前对象");
            }
        }

        // 验证 toUser 用户是否存在
        if (request.getToUser() != null && request.getToUser() > 0) {
            validateAndGetUser(request.getToUser());
        }

        PostDO postDO = null;
        NodeDO nodeDO = null;
        RoadmapDO roadmapDO = null;

        if (request.getObjectType() == Enums.ContentType.post.value()) {
            postDO = validateAndGetPost(request.getObjectId());
        } else if (request.getObjectType() == Enums.ContentType.node.value()) {
            nodeDO = validateAndGetNode(request.getObjectId());
        } else if (request.getObjectType() == Enums.ContentType.roadmap.value()) {
            roadmapDO = validateAndGetRoadmap(request.getObjectId());
        } else {
            throw ErrorCode.COMMENT_INVALID_TYPE.exception();
        }

        // 直接创建 CommentDO
        CommentDO commentDO = new CommentDO();
        commentDO.setObjectId(request.getObjectId());
        commentDO.setObjectType(request.getObjectType());
        commentDO.setReplyToCommentId(request.getReplyTo() == null ? 0 : request.getReplyTo());
        commentDO.setToUserId(request.getToUser() == null ? 0 : request.getToUser());
        commentDO.setContent(request.getContent());
        commentDO.setCreatorId(commentor.getId());
        commentDO.setState(Enums.ContentState.SUBMITTED.value());  // 设置为待审核状态
        //commentDO.setScore(scoreCalculationService.calculateCommentScore(commentDO));
        commentDO.setScore(0.0);
        commentDataService.insert(commentDO);

        // 处理回复和通知 - 重构现有方法以直接使用 request 和 commentDO
        if (request.getReplyTo() != null && request.getReplyTo() > 0) {
            handleReplyComment(request, commentDO, commentor, postDO, nodeDO, roadmapDO);
        }

        // 创建评论通知（不更新评论数，等审核通过后再更新）
        createCommentNotification(request, commentDO, commentor, postDO, nodeDO, roadmapDO);

        // 重新查询并转换为 DTO，填充 toUserName
        CommentDO savedComment = commentDataService.getById(commentDO.getId());
        return toDTOV3(savedComment);
    }

    /**
     * 创建评论消息通知，提交评论时调用
     * 主要功能是给被评论的用户发送评论通知
     */
    private void createCommentNotification(CreateCommentRequest request, CommentDO commentDO, UserDO fromUser,
                                         PostDO postDO, NodeDO nodeDO, RoadmapDO roadmapDO) {
        if (request.getObjectType() == Enums.ContentType.post.value() && postDO != null) {
            messageService.createCommentMessage(postDO.getCreatorId(), fromUser.getId(),
                                              postDO.getNodeId(), commentDO.getId(), postComment.value());
        } else if (request.getObjectType() == Enums.ContentType.node.value() && nodeDO != null) {
            messageService.createCommentMessage(nodeDO.getCreatorId(), fromUser.getId(),
                                              nodeDO.getId(), commentDO.getId(), nodeComment.value());
        } else if (request.getObjectType() == Enums.ContentType.roadmap.value() && roadmapDO != null) {
            messageService.createCommentMessage(roadmapDO.getCreatorId(), fromUser.getId(), 
                                              roadmapDO.getId(), commentDO.getId(), roadmapComment.value());
        }
    }
    
    /**
     * 创建回复评论消息通知，回复评论时调用
     */
    private void createReplyNotification(CreateCommentRequest request, CommentDO commentDO, UserDO fromUser,
                                       PostDO postDO, NodeDO nodeDO, RoadmapDO roadmapDO, Long parentUserId) {
        if (request.getObjectType() == Enums.ContentType.node.value() && nodeDO != null) {
            messageService.createCommentMessage(parentUserId, fromUser.getId(), 
                                              nodeDO.getId(), commentDO.getId(), replyNodeComment.value());
        } else if (request.getObjectType() == Enums.ContentType.post.value() && postDO != null) {
            messageService.createCommentMessage(parentUserId, fromUser.getId(), 
                                              postDO.getNodeId(), commentDO.getId(), replyPostingComment.value());
        } else if (request.getObjectType() == Enums.ContentType.roadmap.value() && roadmapDO != null) {
            messageService.createCommentMessage(parentUserId, fromUser.getId(), 
                                              roadmapDO.getId(), commentDO.getId(), replyRoadmapComment.value());
        }
    }

    /**
     * 处理回复评论的一些逻辑，回复评论时调用
     * 主要功能：
     * 1. 更新父评论的回复数和分数
     * 2. 创建回复通知
     */
    private void handleReplyComment(CreateCommentRequest request, CommentDO commentDO, UserDO fromUser, 
                                   PostDO postDO, NodeDO nodeDO, RoadmapDO roadmapDO) {
        CommentDO parentCommentDO = validateAndGetComment(request.getReplyTo());
        parentCommentDO.setReplyCount(parentCommentDO.getReplyCount() + 1);

        // 更新评论分数并保存
        scoreCalculationService.checkAndUpdateCommentScore(parentCommentDO);
        commentDataService.update(parentCommentDO);

        createReplyNotification(request, commentDO, fromUser, postDO, nodeDO, roadmapDO, parentCommentDO.getCreatorId());
    }

    /**
     * 给查询到的评论列表及其子评论设置点赞状态 upvoted
     */
    private List<CommentDTO> processUpvoteStatusWithChildren(List<CommentDTO> commentDTOList, HashMap<Long, CommentDTO> childrenMap, Long userId) {
        List<Long> allIds = new ArrayList<>();
        
        // 收集所有评论ID（父评论和子评论）
        for (CommentDTO commentDTO : commentDTOList) {
            allIds.add(commentDTO.getId());
        }
        allIds.addAll(childrenMap.values().stream().map(CommentDTO::getId).toList());
        
        if (allIds.isEmpty()) {
            return commentDTOList;
        }

        List<UpvoteDO> upvoteList = upvoteDataService.getList(userId, allIds, Enums.ContentType.comment.value());

        Set<Long> upvotedSet = new HashSet<>();
        for (UpvoteDO upvoteDO : upvoteList) {
            upvotedSet.add(upvoteDO.getObjectId());
        }

        // 设置父评论点赞状态
        for (CommentDTO commentDTO : commentDTOList) {
            commentDTO.setUpvoted(upvotedSet.contains(commentDTO.getId()));
        }

        // 设置子评论点赞状态
        for (CommentDTO commentDTO: childrenMap.values()) {
            commentDTO.setUpvoted(upvotedSet.contains(commentDTO.getId()));
        }
        return commentDTOList;
    }

    /**
     * 获取对象(post, node, comment ...)评论，分页查询，按分数和ID倒序排列
     * @param lastScore 上一页最后一条记录的分数，首页传null
     * @param lastId 上一页最后一条记录的ID，首页传null
     */
    public KeysetPageResponse<CommentDTO> getCommentsByObject(Long objectId, Integer type, Double lastScore, Long lastId, UserDO currentUser) {
        validateObjectId(objectId);
        validateCommentType(type);

        int pageSize = systemProperties.getComment().getDefaultPageSize();
        // 多查询一条以判断是否还有更多数据
        int querySize = pageSize + 1;

        List<CommentDO> commentDOList;
        // 第一页：lastScore 和 lastId 都为 null
        if (lastScore == null && lastId == null) {
            commentDOList = commentDataService.getByObjectId(objectId, type, querySize);
        } else {
            // 后续页：需要同时传递 lastScore 和 lastId
            if (lastScore == null || lastId == null) {
                throw ErrorCode.INVALID_PARAMETER.exception("分页查询需要同时提供 lastScore 和 lastId");
            }
            commentDOList = commentDataService.getByObjectIdPaginated(objectId, type, lastScore, lastId, querySize);
        }

        // 判断是否还有更多数据
        boolean hasMore = commentDOList.size() > pageSize;
        if (hasMore) {
            // 移除多查询的那一条
            commentDOList = commentDOList.subList(0, pageSize);
        }

        List<CommentDTO> commentDTOList = toDTOV1(commentDOList, currentUser.getId());

        // 构建响应
        if (commentDTOList.isEmpty()) {
            return KeysetPageResponse.of(commentDTOList, false, null, null);
        }

        CommentDTO lastComment = commentDTOList.get(commentDTOList.size() - 1);
        return KeysetPageResponse.of(commentDTOList, hasMore, lastComment.getScore(), lastComment.getId());
    }



    /**
     * 获取子评论列表，分页查询，按分数和ID倒序排列
     * @param lastScore 上一页最后一条记录的分数，首页传null
     * @param lastId 上一页最后一条记录的ID，首页传null
     */
    public KeysetPageResponse<CommentDTO> getCommentReplies(Long id, Double lastScore, Long lastId, UserDO currentUser) {
        validateCommentId(id);

        int pageSize = systemProperties.getComment().getDefaultPageSize();
        // 多查询一条以判断是否还有更多数据
        int querySize = pageSize + 1;

        List<CommentDO> commentDOList;
        // 第一页：lastScore 和 lastId 都为 null
        if (lastScore == null && lastId == null) {
            commentDOList = commentDataService.getByTopic(id, querySize);
        } else {
            // 后续页：需要同时传递 lastScore 和 lastId
            if (lastScore == null || lastId == null) {
                throw ErrorCode.INVALID_PARAMETER.exception("分页查询需要同时提供 lastScore 和 lastId");
            }
            commentDOList = commentDataService.getByTopicPaginated(id, lastScore, lastId, querySize);
        }

        // 判断是否还有更多数据
        boolean hasMore = commentDOList.size() > pageSize;
        if (hasMore) {
            // 移除多查询的那一条
            commentDOList = commentDOList.subList(0, pageSize);
        }

        List<CommentDTO> commentDTOList = commentConverter.toDTO(commentDOList);
        commentDTOList = toDTOV2(commentDTOList, currentUser.getId());

        // 构建响应
        if (commentDTOList.isEmpty()) {
            return KeysetPageResponse.of(commentDTOList, false, null, null);
        }

        CommentDTO lastComment = commentDTOList.get(commentDTOList.size() - 1);
        return KeysetPageResponse.of(commentDTOList, hasMore, lastComment.getScore(), lastComment.getId());
    }


    /**
     * 根据状态获取评论列表（分页），按ID倒序排列（越新的越靠前）
     * 仅管理员调用
     * @param state pending(待审核), approved(已通过), rejected(已拒绝), banned(已封禁)
     * @param lastId 最后一条记录的ID，为null时加载第一页
     */
    public List<CommentDTO> getCommentsByState(String state, Long lastId) {
        int pageSize = systemProperties.getComment().getDefaultPageSize();

        byte stateValue;
        switch (state.toLowerCase()) {
            case "pending":
                stateValue = Enums.ContentState.SUBMITTED.value();
                break;
            case "approved":
                stateValue = Enums.ContentState.PUBLISHED.value();
                break;
            case "rejected":
                stateValue = Enums.ContentState.REJECTED.value();
                break;
            case "banned":
                stateValue = Enums.ContentState.BANNED.value();
                break;
            default:
                throw ErrorCode.INVALID_PARAMETER.exception("无效的状态参数: " + state);
        }

        List<CommentDO> commentDOList = commentDataService.getListByState(stateValue, lastId, pageSize);
        return commentConverter.toDTO(commentDOList);
    }

    /**
     * 根据对象类型、对象ID、创建者和状态筛选评论列表
     * 仅管理员调用
     */
    public List<CommentDTO> getCommentsByFilter(Integer objectType, Long objectId, Long creatorId, Long lastId, Byte state) {
        int pageSize = systemProperties.getComment().getDefaultPageSize();
        List<CommentDO> commentDOList = commentDataService.getListByFilter(objectType, objectId, creatorId, lastId, state, pageSize);
        return commentConverter.toDTO(commentDOList);
    }

    /**
     * 审核评论，批准或拒绝评论
     * 仅管理员调用
     */
    @Transactional
    public CommentDTO approveComment(Long id, boolean approve) {
        validateCommentId(id);
        CommentDO commentDO = validateAndGetComment(id);
        int oldState = commentDO.getState();

        if (approve && oldState != Enums.ContentState.PUBLISHED.value()) {
            commentDO.setState(Enums.ContentState.PUBLISHED.value());
            commentDataService.update(commentDO);

            // 通过审核，评论数+1
            if (oldState == Enums.ContentState.SUBMITTED.value() || oldState == Enums.ContentState.REJECTED.value()) {
                updateObjectCommentCount(commentDO, 1);
            }
        }

        if (!approve && oldState != Enums.ContentState.REJECTED.value()) {
            commentDO.setState(Enums.ContentState.REJECTED.value());
            commentDataService.update(commentDO);

            // 拒绝评论，评论数-1
            if (oldState == Enums.ContentState.PUBLISHED.value()) {
                updateObjectCommentCount(commentDO, -1);
            }
        }

        return toDTO(commentDO);
    }

    /**
     * 批准评论
     */
    @Transactional
    public void approve(Long id, UserDO operator) {
        validateCommentId(id);
        CommentDO commentDO = validateAndGetComment(id);
        int oldState = commentDO.getState();

        if (oldState != Enums.ContentState.PUBLISHED.value()) {
            commentDO.setState(Enums.ContentState.PUBLISHED.value());
            commentDO.setReason(null);  // 清空拒绝原因
            commentDataService.update(commentDO);

            // 批准评论，评论数+1
            updateObjectCommentCount(commentDO, 1);
        }
    }

    /**
     * 拒绝评论（审核不通过，带原因）
     */
    @Transactional
    public void reject(Long id, String reason, UserDO operator) {
        validateCommentId(id);
        CommentDO commentDO = validateAndGetComment(id);
        int oldState = commentDO.getState();

        // 获取评论对象信息用于通知
        String objectType = "node"; // 默认为 node
        if (commentDO.getObjectType() == Enums.ContentType.post.value()) {
            objectType = "post";
        } else if (commentDO.getObjectType() == Enums.ContentType.roadmap.value()) {
            objectType = "roadmap";
        }

        String objectTitle = "";

        if (commentDO.getObjectType() == Enums.ContentType.post.value()) {
            PostDO postDO = postDataService.getById(commentDO.getObjectId());
            if (postDO != null && postDO.getContent() != null) {
                objectTitle = com.prosper.learn.domain.util.Util.stripFormatting(postDO.getContent());
                if (objectTitle.length() > 50) {
                    objectTitle = objectTitle.substring(0, 50) + "...";
                }
            }
        } else if (commentDO.getObjectType() == Enums.ContentType.node.value()) {
            NodeDO nodeDO = nodeDataService.getById(commentDO.getObjectId());
            if (nodeDO != null) objectTitle = nodeDO.getName();
        } else if (commentDO.getObjectType() == Enums.ContentType.roadmap.value()) {
            objectTitle = "路线图";
        }

        // 截取评论预览（前50个字符）
        String preview = commentDO.getContent();
        if (preview != null && preview.length() > 50) {
            preview = preview.substring(0, 50) + "...";
        }

        commentDataService.reject(id, reason);

        // 拒绝评论，如果之前是已批准状态，评论数-1
        if (oldState == Enums.ContentState.PUBLISHED.value()) {
            updateObjectCommentCount(commentDO, -1);
        }

        // 发送拒绝通知
        messageService.sendCommentModeration(
            commentDO.getCreatorId(),
            commentDO.getId(),
            preview,
            objectType,
            commentDO.getObjectId(),
            objectTitle,
            Enums.ModerationAction.REJECTED,
            reason
        );
    }

    /**
     * 封禁评论（违规封禁，带原因）
     */
    @Transactional
    public void ban(Long id, String reason, UserDO operator) {
        validateCommentId(id);
        CommentDO commentDO = validateAndGetComment(id);
        int oldState = commentDO.getState();

        // 获取评论对象信息用于通知
        String objectType = "node"; // 默认为 node
        if (commentDO.getObjectType() == Enums.ContentType.post.value()) {
            objectType = "post";
        } else if (commentDO.getObjectType() == Enums.ContentType.roadmap.value()) {
            objectType = "roadmap";
        }

        String objectTitle = "";

        if (commentDO.getObjectType() == Enums.ContentType.post.value()) {
            PostDO postDO = postDataService.getById(commentDO.getObjectId());
            if (postDO != null && postDO.getContent() != null) {
                objectTitle = com.prosper.learn.domain.util.Util.stripFormatting(postDO.getContent());
                if (objectTitle.length() > 50) {
                    objectTitle = objectTitle.substring(0, 50) + "...";
                }
            }
        } else if (commentDO.getObjectType() == Enums.ContentType.node.value()) {
            NodeDO nodeDO = nodeDataService.getById(commentDO.getObjectId());
            if (nodeDO != null) objectTitle = nodeDO.getName();
        } else if (commentDO.getObjectType() == Enums.ContentType.roadmap.value()) {
            objectTitle = "路线图";
        }

        // 截取评论预览（前50个字符）
        String preview = commentDO.getContent();
        if (preview != null && preview.length() > 50) {
            preview = preview.substring(0, 50) + "...";
        }

        commentDataService.ban(id, reason);

        // 封禁评论，如果之前是已批准状态，评论数-1
        if (oldState == Enums.ContentState.PUBLISHED.value()) {
            updateObjectCommentCount(commentDO, -1);
        }

        // 发送封禁通知
        messageService.sendCommentModeration(
            commentDO.getCreatorId(),
            commentDO.getId(),
            preview,
            objectType,
            commentDO.getObjectId(),
            objectTitle,
            Enums.ModerationAction.BANNED,
            reason
        );
    }

    /**
     * 拒绝评论（审核不通过）- 无原因版本
     */
    @Transactional
    public void rejectComment(Long id) {
        validateCommentId(id);
        CommentDO commentDO = validateAndGetComment(id);
        int oldState = commentDO.getState();

        commentDataService.reject(id);

        // 拒绝评论，如果之前是已批准状态，评论数-1
        if (oldState == Enums.ContentState.PUBLISHED.value()) {
            updateObjectCommentCount(commentDO, -1);
        }
    }

    /**
     * 封禁评论（违规封禁）- 无原因版本
     */
    @Transactional
    public void banComment(Long id) {
        validateCommentId(id);
        CommentDO commentDO = validateAndGetComment(id);
        int oldState = commentDO.getState();

        commentDataService.ban(id);

        // 封禁评论，如果之前是已批准状态，评论数-1
        if (oldState == Enums.ContentState.PUBLISHED.value()) {
            updateObjectCommentCount(commentDO, -1);
        }
    }

    /**
     * 更新对象的评论数
     */
    private void updateObjectCommentCount(CommentDO commentDO, int delta) {
        if (commentDO.getObjectType() == Enums.ContentType.post.value()) {
            PostDO postDO = postDataService.getById(commentDO.getObjectId());
            if (postDO != null) {
                postDO.setCommentCount(Math.max(0, postDO.getCommentCount() + delta));
                postDataService.update(postDO);
            }
        } else if (commentDO.getObjectType() == Enums.ContentType.node.value()) {
            NodeDO nodeDO = nodeDataService.getById(commentDO.getObjectId());
            if (nodeDO != null) {
                nodeDO.setCommentCount(Math.max(0, nodeDO.getCommentCount() + delta));
                nodeDataService.update(nodeDO);
            }
        } else if (commentDO.getObjectType() == Enums.ContentType.roadmap.value()) {
            RoadmapDO roadmapDO = roadmapDataService.getById(commentDO.getObjectId());
            if (roadmapDO != null) {
                roadmapDO.setComment(Math.max(0, roadmapDO.getComment() + delta));
                roadmapDataService.update(roadmapDO);
            }
        }
    }

    // ========== toDTV ==========

    /**
     * 单个 CommentDO 转换为 CommentDTO，默认方法
     */
    public CommentDTO toDTO(CommentDO commentDO) {
        return commentConverter.toDTO(commentDO);
    }

    /**
     * 给commentDTOList查询子评论并填充，每条评论只填充一条子评论，选取分数最高的子评论，并且填充upvoted状态位
     * dtoV1 = dto + childComments + upvoted + toUserName
     */
    private List<CommentDTO> toDTOV1(List<CommentDO> commentDOList, Long userId) {
        List<Long> ids = new ArrayList<>();
        for (CommentDO commentDO : commentDOList) {
            ids.add(commentDO.getId());
        }

        List<CommentDTO> commentDTOList = commentConverter.toDTO(commentDOList);
        List<CommentDO> children = commentDataService.getChildren(ids);

        HashMap<Long, CommentDTO> map = new HashMap<>();
        for (CommentDO commentDO : children) {
            map.put(commentDO.getReplyToCommentId(), commentConverter.toDTO(commentDO));
        }

        for (CommentDTO commentDTO: commentDTOList) {
            if (map.containsKey(commentDTO.getId())) {
                CommentDTO childCommentDTO = map.get(commentDTO.getId());
                ids.add(childCommentDTO.getId());
                commentDTO.addChild(childCommentDTO);
            }
        }

        fillToUserNames(commentDTOList, map);

        return processUpvoteStatusWithChildren(commentDTOList, map, userId);
    }

    /**
     * 给查询到的评论列表设置点赞状态 upvoted
     * dtoV2 = dto + upvoted + toUserName
     * 在加载更多子评论时调用，因为它已经是子评论了，所以它没有子评论列表
     */
    private List<CommentDTO> toDTOV2(List<CommentDTO> commentDTOList, Long userId) {
        if (commentDTOList.isEmpty()) {
            return commentDTOList;
        }

        List<Long> ids = commentDTOList.stream().map(CommentDTO::getId).toList();
        List<UpvoteDO> upvoteList = upvoteDataService.getList(userId, ids, Enums.ContentType.comment.value());

        Set<Long> upvotedSet = new HashSet<>();
        for (UpvoteDO upvoteDO : upvoteList) {
            upvotedSet.add(upvoteDO.getObjectId());
        }

        for (CommentDTO commentDTO : commentDTOList) {
            commentDTO.setUpvoted(upvotedSet.contains(commentDTO.getId()));
        }

        fillToUserNames(commentDTOList, new HashMap<>());

        return commentDTOList;
    }

    /**
     * 转换单个评论为 DTO 并填充 toUserName（用于创建评论后返回）
     * dtoV3 = dto + toUserName
     */
    private CommentDTO toDTOV3(CommentDO commentDO) {
        CommentDTO commentDTO = commentConverter.toDTO(commentDO);

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
     * 填充评论列表中的 creatorName 和 toUserName 字段
     */
    private void fillToUserNames(List<CommentDTO> commentDTOList, HashMap<Long, CommentDTO> childrenMap) {
        Set<Long> userIds = new HashSet<>();

        // 收集所有需要查询的用户ID（包括 creatorId 和 toUserId）
        for (CommentDTO commentDTO : commentDTOList) {
            if (commentDTO.getCreatorId() != null && commentDTO.getCreatorId() > 0) {
                userIds.add(commentDTO.getCreatorId());
            }
            if (commentDTO.getToUserId() != null && commentDTO.getToUserId() > 0) {
                userIds.add(commentDTO.getToUserId());
            }
        }

        for (CommentDTO commentDTO : childrenMap.values()) {
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
        for (CommentDTO commentDTO : commentDTOList) {
            if (commentDTO.getCreatorId() != null && commentDTO.getCreatorId() > 0 && userMap.containsKey(commentDTO.getCreatorId())) {
                UserDO creator = userMap.get(commentDTO.getCreatorId());
                commentDTO.setCreatorName(creator.getName());
            }
            if (commentDTO.getToUserId() != null && commentDTO.getToUserId() > 0 && userMap.containsKey(commentDTO.getToUserId())) {
                UserDO user = userMap.get(commentDTO.getToUserId());
                commentDTO.setToUserName(user.getName());
            }
        }

        for (CommentDTO commentDTO : childrenMap.values()) {
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


}