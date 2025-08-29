package com.prosper.learn.domain.service.business;

import com.prosper.learn.common.Enums;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.config.SystemProperties;
import com.prosper.learn.domain.service.basic.MessageService;
import com.prosper.learn.domain.service.basic.RedisStatsService;
import com.prosper.learn.domain.service.basic.ScoreCalculationService;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.CommentDTO;
import com.prosper.learn.dto.CommentDTOV1;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.prosper.learn.common.Enums.CommentState.submited;
import static com.prosper.learn.common.Enums.MessageType.*;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final UserMapper userMapper;
    private final UpvoteMapper upvoteMapper;
    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final NodeMapper nodeMapper;
    private final RoadmapMapper roadmapMapper;
    private final MessageService messageService;
    private final ScoreCalculationService scoreCalculationService;
    private final RedisStatsService redisStatsService;
    private final SystemProperties systemProperties;

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
        PostDO postDO = postMapper.get(postId);
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
        NodeDO nodeDO = nodeMapper.getById(nodeId);
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
        RoadmapDO roadmapDO = roadmapMapper.get(roadmapId);
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
        CommentDO commentDO = commentMapper.get(commentId);
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
        UserDO userDO = userMapper.getById(userId);
        if (userDO == null) {
            throw ErrorCode.USER_NOT_FOUND.exception();
        }
        return userDO;
    }

    @Transactional
    public CommentDO createComment(CommentDTO commentDTO, Long userId) {
        validateUserId(userId);
        validateObjectId(commentDTO.getObjectId());
        validateCommentType(commentDTO.getType());
        
        commentDTO.setFromUser(userId);
        UserDO fromUser = validateAndGetUser(userId);

        PostDO postDO = null;
        NodeDO nodeDO = null;
        RoadmapDO roadmapDO = null;

        if (commentDTO.getType() == Enums.ObjectType.post.value()) {
            postDO = validateAndGetPost(commentDTO.getObjectId());
        } else if (commentDTO.getType() == Enums.ObjectType.node.value()) {
            nodeDO = validateAndGetNode(commentDTO.getObjectId());
        } else if (commentDTO.getType() == Enums.ObjectType.roadmap.value()) {
            roadmapDO = validateAndGetRoadmap(commentDTO.getObjectId());
        } else {
            throw ErrorCode.COMMENT_INVALID_TYPE.exception();
        }

        CommentDO commentDO = Converter.INSTANCE.toCommentDO(commentDTO);
        commentDO.setScore(scoreCalculationService.calculateCommentScore(commentDO));
        commentMapper.insert(commentDO);

        if (commentDTO.getReplyTo() != 0) {
            handleReplyComment(commentDTO, commentDO, fromUser, postDO, nodeDO, roadmapDO);
        }

        handleObjectComment(commentDTO, commentDO, fromUser, postDO, nodeDO, roadmapDO, userId);

        commentDO = commentMapper.get(commentDO.getId());
        return commentDO;
    }

    /**
     * 更新对象评论数量
     */
    private void incrementCommentCount(CommentDTO commentDTO, PostDO postDO, NodeDO nodeDO, RoadmapDO roadmapDO, Long userId) {
        if (commentDTO.getType() == Enums.ObjectType.post.value() && postDO != null) {
            postDO.setCommentCount(postDO.getCommentCount() + 1);
            postMapper.update(postDO);
            redisStatsService.recordComment((long) postDO.getId(), userId);
        } else if (commentDTO.getType() == Enums.ObjectType.node.value() && nodeDO != null) {
            nodeDO.setCommentCount(nodeDO.getCommentCount() + 1);
            nodeMapper.update(nodeDO);
            redisStatsService.recordComment((long) nodeDO.getId(), userId);
        } else if (commentDTO.getType() == Enums.ObjectType.roadmap.value() && roadmapDO != null) {
            roadmapDO.setComment(roadmapDO.getComment() + 1);
            roadmapMapper.update(roadmapDO);
            redisStatsService.recordComment((long) roadmapDO.getId(), userId);
        }
    }
    
    /**
     * 创建评论消息通知
     */
    private void createCommentNotification(CommentDTO commentDTO, CommentDO commentDO, UserDO fromUser,
                                         PostDO postDO, NodeDO nodeDO, RoadmapDO roadmapDO) {
        if (commentDTO.getType() == Enums.ObjectType.post.value() && postDO != null) {
            messageService.createCommentMessage(postDO.getCreator(), fromUser.getId(), 
                                              postDO.getNodeId(), commentDO.getId(), postComment.value());
        } else if (commentDTO.getType() == Enums.ObjectType.node.value() && nodeDO != null) {
            messageService.createCommentMessage(nodeDO.getCreator(), fromUser.getId(), 
                                              nodeDO.getId(), commentDO.getId(), nodeComment.value());
        } else if (commentDTO.getType() == Enums.ObjectType.roadmap.value() && roadmapDO != null) {
            messageService.createCommentMessage(roadmapDO.getCreatorId(), fromUser.getId(), 
                                              roadmapDO.getId(), commentDO.getId(), roadmapComment.value());
        }
    }
    
    /**
     * 创建回复评论消息通知
     */
    private void createReplyNotification(CommentDTO commentDTO, CommentDO commentDO, UserDO fromUser,
                                       PostDO postDO, NodeDO nodeDO, RoadmapDO roadmapDO, Long parentUserId) {
        if (commentDTO.getType() == Enums.ObjectType.node.value() && nodeDO != null) {
            messageService.createCommentMessage(parentUserId, fromUser.getId(), 
                                              nodeDO.getId(), commentDO.getId(), replyNodeComment.value());
        } else if (commentDTO.getType() == Enums.ObjectType.post.value() && postDO != null) {
            messageService.createCommentMessage(parentUserId, fromUser.getId(), 
                                              postDO.getNodeId(), commentDO.getId(), replyPostingComment.value());
        } else if (commentDTO.getType() == Enums.ObjectType.roadmap.value() && roadmapDO != null) {
            messageService.createCommentMessage(parentUserId, fromUser.getId(), 
                                              roadmapDO.getId(), commentDO.getId(), replyRoadmapComment.value());
        }
    }

    private void handleReplyComment(CommentDTO commentDTO, CommentDO commentDO, UserDO fromUser, 
                                   PostDO postDO, NodeDO nodeDO, RoadmapDO roadmapDO) {
        CommentDO parentCommentDO = validateAndGetComment(commentDTO.getReplyTo());
        
        parentCommentDO.setReplyCount(parentCommentDO.getReplyCount() + 1);

        // 更新评论分数并保存
        scoreCalculationService.checkAndUpdateCommentScore(parentCommentDO);
        commentMapper.update(parentCommentDO);

        createReplyNotification(commentDTO, commentDO, fromUser, postDO, nodeDO, roadmapDO, parentCommentDO.getFromUser());
    }

    private void handleObjectComment(CommentDTO commentDTO, CommentDO commentDO, UserDO fromUser,
                                   PostDO postDO, NodeDO nodeDO, RoadmapDO roadmapDO, Long userId) {
        incrementCommentCount(commentDTO, postDO, nodeDO, roadmapDO, userId);
        createCommentNotification(commentDTO, commentDO, fromUser, postDO, nodeDO, roadmapDO);
    }

    /**
     * 处理点赞状态
     */
    private void processUpvoteStatus(List<CommentDTO> commentDTOList, Long userId) {
        if (commentDTOList.isEmpty()) {
            return;
        }
        
        List<Long> ids = commentDTOList.stream().map(CommentDTO::getId).toList();
        List<UpvoteDO> upvoteList = upvoteMapper.getList(userId.intValue(), ids, Enums.ObjectType.comment.value());

        Set<Long> upvotedSet = new HashSet<>();
        for (UpvoteDO upvoteDO : upvoteList) {
            upvotedSet.add(upvoteDO.getObjectId());
        }

        for (CommentDTO commentDTO : commentDTOList) {
            commentDTO.setUpvoted(upvotedSet.contains(commentDTO.getId()) ? 1 : 0);
        }
    }
    
    /**
     * 处理点赞状态（包含子评论）
     */
    private void processUpvoteStatusWithChildren(List<CommentDTO> commentDTOList, HashMap<Long, CommentDTO> childrenMap, Long userId) {
        List<Long> allIds = new ArrayList<>();
        
        // 收集所有评论ID（父评论和子评论）
        for (CommentDTO commentDTO : commentDTOList) {
            allIds.add(commentDTO.getId());
        }
        allIds.addAll(childrenMap.values().stream().map(CommentDTO::getId).toList());
        
        if (allIds.isEmpty()) {
            return;
        }

        List<UpvoteDO> upvoteList = upvoteMapper.getList(userId.intValue(), allIds, Enums.ObjectType.comment.value());

        Set<Long> upvotedSet = new HashSet<>();
        for (UpvoteDO upvoteDO : upvoteList) {
            upvotedSet.add(upvoteDO.getObjectId());
        }

        // 设置父评论点赞状态
        for (CommentDTO commentDTO : commentDTOList) {
            commentDTO.setUpvoted(upvotedSet.contains(commentDTO.getId()) ? 1 : 0);
        }

        // 设置子评论点赞状态
        for (CommentDTO commentDTO: childrenMap.values()) {
            commentDTO.setUpvoted(upvotedSet.contains(commentDTO.getId()) ? 1 : 0);
        }
    }

    public List<CommentDTO> getCommentsByObject(Long objectId, int type, Long offsetId, Long userId) {
        validateObjectId(objectId);
        validateCommentType(type);
        validateOffsetId(offsetId);
        validateUserId(userId);
        
        List<CommentDO> commentDOList;
        int pageSize = systemProperties.getComment().getDefaultPageSize();
        if (offsetId == 0) {
            commentDOList = commentMapper.getByObjectId(objectId, type, pageSize);
        } else {
            CommentDO lastComment = commentMapper.get(offsetId);
            if (lastComment == null) {
                return new ArrayList<>();
            }
            commentDOList = commentMapper.getByObjectIdPaginated(objectId, type, lastComment.getScore(), offsetId, pageSize);
        }

        List<CommentDTO> commentDTOList = Converter.INSTANCE.toCommentDTO(commentDOList);

        List<Long> ids = new ArrayList<>();
        for (CommentDO commentDO : commentDOList) {
            ids.add(commentDO.getId());
        }

        if (!ids.isEmpty()) {
            handleChildComments(commentDTOList, ids, userId);
        }

        return commentDTOList;
    }

    private void handleChildComments(List<CommentDTO> commentDTOList, List<Long> ids, Long userId) {
        List<CommentDO> children = commentMapper.getChildren(ids);

        HashMap<Long, CommentDTO> map = new HashMap<>();
        for (CommentDO commentDO : children) {
            map.put(commentDO.getReplyTo(), Converter.INSTANCE.toCommentDTO(commentDO));
        }

        for (CommentDTO commentDTO: commentDTOList) {
            if (map.containsKey(commentDTO.getId())) {
                CommentDTO childCommentDTO = map.get(commentDTO.getId());
                ids.add(childCommentDTO.getId());
                commentDTO.addChild(childCommentDTO);
            }
        }

        processUpvoteStatusWithChildren(commentDTOList, map, userId);
    }

    public List<CommentDTO> getCommentReplies(Long id, Long offsetId, Long userId) {
        validateCommentId(id);
        validateOffsetId(offsetId);
        validateUserId(userId);
        
        List<CommentDO> commentDOList;
        int pageSize = systemProperties.getComment().getDefaultPageSize();
        if (offsetId == 0) {
            commentDOList = commentMapper.getByTopic(id, pageSize);
        } else {
            CommentDO lastComment = commentMapper.get(offsetId);
            if (lastComment == null) {
                return new ArrayList<>();
            }
            commentDOList = commentMapper.getByTopicPaginated(id, lastComment.getScore(), offsetId, pageSize);
        }

        List<CommentDTO> commentDTOList = Converter.INSTANCE.toCommentDTO(commentDOList);
        processUpvoteStatus(commentDTOList, userId);
        return commentDTOList;
    }

    public List<CommentDTOV1> getPendingComments() {
        int limit = systemProperties.getComment().getPendingCommentsLimit();
        List<CommentDO> commentDOList = commentMapper.getListByState(submited.value(), limit);
        return Converter.INSTANCE.toCommentDTOV1(commentDOList);
    }

    @Transactional
    public CommentDTOV1 approveComment(Long id, boolean approve) {
        validateCommentId(id);
        
        CommentDO commentDO = validateAndGetComment(id);

        if (approve && commentDO.getState() != Enums.CommentState.approved.value()) {
            commentDO.setState(Enums.CommentState.approved.value());
            commentMapper.update(commentDO);
        }
        if (!approve && commentDO.getState() != Enums.CommentState.deleted.value()) {
            commentDO.setState(Enums.CommentState.deleted.value());
            commentMapper.update(commentDO);
        }
        return Converter.INSTANCE.toCommentDTOV1(commentDO);
    }
}