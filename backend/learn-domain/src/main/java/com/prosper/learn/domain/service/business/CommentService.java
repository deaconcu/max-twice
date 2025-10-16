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
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.domain.service.data.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.prosper.learn.common.Enums.CommentState.submited;
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
     * 验证偏移ID
     */
    private void validateOffsetId(Long offsetId) {
        if (offsetId == null || offsetId < 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    /**
     * 验证评论类型
     */
    private void validateCommentType(int type) {
        if (type != Enums.ObjectType.post.value() && 
            type != Enums.ObjectType.node.value() && 
            type != Enums.ObjectType.roadmap.value()) {
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
     * @param request
     * @param userId
     * @return
     */
    @Transactional
    public CommentDO createComment(CreateCommentRequest request, Long userId) {
        // 先验证参数
        if (request == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("评论请求不能为空");
        }
        
        validateUserId(userId);
        validateObjectId(request.getObjectId());
        validateCommentType(request.getObjectType());
        
        UserDO fromUser = validateAndGetUser(userId);

        PostDO postDO = null;
        NodeDO nodeDO = null;
        RoadmapDO roadmapDO = null;

        if (request.getObjectType() == Enums.ObjectType.post.value()) {
            postDO = validateAndGetPost(request.getObjectId());
        } else if (request.getObjectType() == Enums.ObjectType.node.value()) {
            nodeDO = validateAndGetNode(request.getObjectId());
        } else if (request.getObjectType() == Enums.ObjectType.roadmap.value()) {
            roadmapDO = validateAndGetRoadmap(request.getObjectId());
        } else {
            throw ErrorCode.COMMENT_INVALID_TYPE.exception();
        }

        // 直接创建 CommentDO
        CommentDO commentDO = new CommentDO();
        commentDO.setObjectId(request.getObjectId());
        commentDO.setObjectType(request.getObjectType());
        commentDO.setReplyToCommentId(request.getReplyTo());
        // TODO 检查这个toUser是否合理
        commentDO.setToUserId(request.getToUser());
        commentDO.setContent(request.getContent());
        commentDO.setFromUserId(userId);
        //commentDO.setScore(scoreCalculationService.calculateCommentScore(commentDO));
        commentDO.setScore(0.0);
        commentDataService.insert(commentDO);

        // 处理回复和通知 - 重构现有方法以直接使用 request 和 commentDO
        if (request.getReplyTo() != null && request.getReplyTo() != 0) {
            handleReplyComment(request, commentDO, fromUser, postDO, nodeDO, roadmapDO);
        }

        // 更新对象评论数和创建评论通知
        incrementCommentCount(request, postDO, nodeDO, roadmapDO, userId);
        createCommentNotification(request, commentDO, fromUser, postDO, nodeDO, roadmapDO);

        return commentDataService.getById(commentDO.getId());
    }

    /**
     * 更新对象评论数量，提交评论时调用
     * 主要功能是增加对象表的评论数和在Redis中记录评论行为
     */
    private void incrementCommentCount(CreateCommentRequest request, PostDO postDO, NodeDO nodeDO, RoadmapDO roadmapDO, Long userId) {
        if (request.getObjectType() == Enums.ObjectType.post.value() && postDO != null) {
            postDO.setCommentCount(postDO.getCommentCount() + 1);
            postDataService.update(postDO);
            redisStatsService.recordComment(postDO.getId(), userId);
        } else if (request.getObjectType() == Enums.ObjectType.node.value() && nodeDO != null) {
            nodeDO.setCommentCount(nodeDO.getCommentCount() + 1);
            nodeDataService.update(nodeDO);
            redisStatsService.recordComment(nodeDO.getId(), userId);
        } else if (request.getObjectType() == Enums.ObjectType.roadmap.value() && roadmapDO != null) {
            roadmapDO.setComment(roadmapDO.getComment() + 1);
            roadmapDataService.update(roadmapDO);
            redisStatsService.recordComment(roadmapDO.getId(), userId);
        }
    }
    
    /**
     * 创建评论消息通知，提交评论时调用
     * 主要功能是给被评论的用户发送评论通知
     */
    private void createCommentNotification(CreateCommentRequest request, CommentDO commentDO, UserDO fromUser,
                                         PostDO postDO, NodeDO nodeDO, RoadmapDO roadmapDO) {
        if (request.getObjectType() == Enums.ObjectType.post.value() && postDO != null) {
            messageService.createCommentMessage(postDO.getCreatorId(), fromUser.getId(),
                                              postDO.getNodeId(), commentDO.getId(), postComment.value());
        } else if (request.getObjectType() == Enums.ObjectType.node.value() && nodeDO != null) {
            messageService.createCommentMessage(nodeDO.getCreatorId(), fromUser.getId(),
                                              nodeDO.getId(), commentDO.getId(), nodeComment.value());
        } else if (request.getObjectType() == Enums.ObjectType.roadmap.value() && roadmapDO != null) {
            messageService.createCommentMessage(roadmapDO.getCreatorId(), fromUser.getId(), 
                                              roadmapDO.getId(), commentDO.getId(), roadmapComment.value());
        }
    }
    
    /**
     * 创建回复评论消息通知，回复评论时调用
     */
    private void createReplyNotification(CreateCommentRequest request, CommentDO commentDO, UserDO fromUser,
                                       PostDO postDO, NodeDO nodeDO, RoadmapDO roadmapDO, Long parentUserId) {
        if (request.getObjectType() == Enums.ObjectType.node.value() && nodeDO != null) {
            messageService.createCommentMessage(parentUserId, fromUser.getId(), 
                                              nodeDO.getId(), commentDO.getId(), replyNodeComment.value());
        } else if (request.getObjectType() == Enums.ObjectType.post.value() && postDO != null) {
            messageService.createCommentMessage(parentUserId, fromUser.getId(), 
                                              postDO.getNodeId(), commentDO.getId(), replyPostingComment.value());
        } else if (request.getObjectType() == Enums.ObjectType.roadmap.value() && roadmapDO != null) {
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

        createReplyNotification(request, commentDO, fromUser, postDO, nodeDO, roadmapDO, parentCommentDO.getFromUserId());
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

        List<UpvoteDO> upvoteList = upvoteDataService.getList(userId, allIds, Enums.ObjectType.comment.value());

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
     */
    public List<CommentDTO> getCommentsByObject(Long objectId, Integer type, Long offsetId, Long userId) {
        validateObjectId(objectId);
        validateCommentType(type);
        validateOffsetId(offsetId);
        validateUserId(userId);
        
        List<CommentDO> commentDOList;
        int pageSize = systemProperties.getComment().getDefaultPageSize();
        if (offsetId == 0) {
            commentDOList = commentDataService.getByObjectId(objectId, type, pageSize);
        } else {
            CommentDO lastComment = commentDataService.getById(offsetId);
            if (lastComment == null) {
                return new ArrayList<>();
            }
            commentDOList = commentDataService.getByObjectIdPaginated(objectId, type, lastComment.getScore(), offsetId, pageSize);
        }

        return toDTOV1(commentDOList, userId);
    }



    /**
     * 获取子评论列表，分页查询(lastId)，按分数和ID倒序排列
     */
    public List<CommentDTO> getCommentReplies(Long id, Long offsetId, Long userId) {
        validateCommentId(id);
        validateOffsetId(offsetId);
        validateUserId(userId);
        
        List<CommentDO> commentDOList;
        int pageSize = systemProperties.getComment().getDefaultPageSize();
        if (offsetId == 0) {
            commentDOList = commentDataService.getByTopic(id, pageSize);
        } else {
            CommentDO lastComment = commentDataService.getById(offsetId);
            if (lastComment == null) {
                return new ArrayList<>();
            }
            commentDOList = commentDataService.getByTopicPaginated(id, lastComment.getScore(), offsetId, pageSize);
        }

        List<CommentDTO> commentDTOList = commentConverter.toDTO(commentDOList);
        return toDTOV2(commentDTOList, userId);
    }


    /**
     * 获取待审核评论列表，按提交时间倒序排列，只返回有限数量（limit）
     * 仅管理员调用
     */
    public List<CommentDTO> getPendingComments() {
        int limit = systemProperties.getComment().getPendingCommentsLimit();
        List<CommentDO> commentDOList = commentDataService.getListByState(submited.value(), limit);
        return commentConverter.toDTO(commentDOList);
    }

    /**
     * 审核评论，批准或删除评论
     * 仅管理员调用
     */
    @Transactional
    public CommentDTO approveComment(Long id, boolean approve) {
        validateCommentId(id);
        CommentDO commentDO = validateAndGetComment(id);

        if (approve && commentDO.getState() != Enums.CommentState.approved.value()) {
            commentDO.setState(Enums.CommentState.approved.value());
            commentDataService.update(commentDO);
        }
        if (!approve && commentDO.getState() != Enums.CommentState.deleted.value()) {
            commentDO.setState(Enums.CommentState.deleted.value());
            commentDataService.update(commentDO);
        }
        return toDTO(commentDO);
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
     * dtoV1 = dto + childComments + upvoted
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

        return processUpvoteStatusWithChildren(commentDTOList, map, userId);
    }

    /**
     * 给查询到的评论列表设置点赞状态 upvoted
     * dtoV2 = dto + upvoted
     * 在加载更多子评论时调用，因为它已经是子评论了，所以它没有子评论列表
     */
    private List<CommentDTO> toDTOV2(List<CommentDTO> commentDTOList, Long userId) {
        if (commentDTOList.isEmpty()) {
            return commentDTOList;
        }

        List<Long> ids = commentDTOList.stream().map(CommentDTO::getId).toList();
        List<UpvoteDO> upvoteList = upvoteDataService.getList(userId, ids, Enums.ObjectType.comment.value());

        Set<Long> upvotedSet = new HashSet<>();
        for (UpvoteDO upvoteDO : upvoteList) {
            upvotedSet.add(upvoteDO.getObjectId());
        }

        for (CommentDTO commentDTO : commentDTOList) {
            commentDTO.setUpvoted(upvotedSet.contains(commentDTO.getId()));
        }
        return commentDTOList;
    }


}