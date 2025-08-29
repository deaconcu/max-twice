package com.prosper.learn.domain.service.business;

import com.prosper.learn.common.Enums;
import com.prosper.learn.common.exception.ErrorCode;
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

    @Transactional
    public CommentDO createComment(CommentDTO commentDTO, Long userId) {
        commentDTO.setFromUser(userId);
        UserDO fromUser = userMapper.getById(userId);

        PostDO postDO = null;
        NodeDO nodeDO = null;
        RoadmapDO roadmapDO = null;

        if (commentDTO.getType() == Enums.ObjectType.post.value()) {
            postDO = postMapper.get(commentDTO.getObjectId());
            if (postDO == null) throw new IllegalArgumentException("帖子不存在");
        } else if (commentDTO.getType() == Enums.ObjectType.node.value()) {
            nodeDO = nodeMapper.getById(commentDTO.getObjectId());
            if (nodeDO == null) throw new IllegalArgumentException("节点不存在");
        } else if (commentDTO.getType() == Enums.ObjectType.roadmap.value()) {
            roadmapDO = roadmapMapper.get(commentDTO.getObjectId());
            if (roadmapDO == null) throw new IllegalArgumentException("路线图不存在");
        } else {
            throw new IllegalArgumentException("评论类型不正确");
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

    private void handleReplyComment(CommentDTO commentDTO, CommentDO commentDO, UserDO fromUser, 
                                   PostDO postDO, NodeDO nodeDO, RoadmapDO roadmapDO) {
        CommentDO parentCommentDO = commentMapper.get(commentDTO.getReplyTo());
        if (parentCommentDO == null) throw new IllegalArgumentException("父评论不存在");
        
        parentCommentDO.setReplyCount(parentCommentDO.getReplyCount() + 1);

        if (scoreCalculationService.checkAndUpdateCommentScore(parentCommentDO)) {
            commentMapper.update(parentCommentDO);
        } else {
            commentMapper.update(parentCommentDO);
        }

        if (commentDTO.getType() == Enums.ObjectType.node.value() && nodeDO != null) {
            messageService.createCommentMessage(
                    parentCommentDO.getFromUser(), fromUser.getId(), nodeDO.getId(), commentDO.getId(), replyNodeComment.value());
        } else if (commentDTO.getType() == Enums.ObjectType.post.value() && postDO != null) {
            messageService.createCommentMessage(
                    parentCommentDO.getFromUser(), fromUser.getId(), postDO.getNodeId(), commentDO.getId(), replyPostingComment.value());
        } else if (commentDTO.getType() == Enums.ObjectType.roadmap.value() && roadmapDO != null) {
            messageService.createCommentMessage(
                    parentCommentDO.getFromUser(), fromUser.getId(), roadmapDO.getId(), commentDO.getId(), replyRoadmapComment.value());
        }
    }

    private void handleObjectComment(CommentDTO commentDTO, CommentDO commentDO, UserDO fromUser,
                                   PostDO postDO, NodeDO nodeDO, RoadmapDO roadmapDO, Long userId) {
        if (commentDTO.getType() == Enums.ObjectType.post.value() && postDO != null) {
            postDO.setCommentCount(postDO.getCommentCount() + 1);
            postMapper.update(postDO);
            
            redisStatsService.recordComment((long) postDO.getId(), userId);
            
            messageService.createCommentMessage(postDO.getCreator(), fromUser.getId(), postDO.getNodeId(), commentDO.getId(), postComment.value());
        } else if (commentDTO.getType() == Enums.ObjectType.node.value() && nodeDO != null) {
            nodeDO.setCommentCount(nodeDO.getCommentCount() + 1);
            nodeMapper.update(nodeDO);
            
            redisStatsService.recordComment((long) nodeDO.getId(), userId);
            
            messageService.createCommentMessage(nodeDO.getCreator(), fromUser.getId(), nodeDO.getId(), commentDO.getId(), nodeComment.value());
        } else if (commentDTO.getType() == Enums.ObjectType.roadmap.value() && roadmapDO != null) {
            roadmapDO.setComment(roadmapDO.getComment() + 1);
            roadmapMapper.update(roadmapDO);
            
            redisStatsService.recordComment((long) roadmapDO.getId(), userId);
            
            messageService.createCommentMessage(roadmapDO.getCreatorId(), fromUser.getId(), roadmapDO.getId(), commentDO.getId(), roadmapComment.value());
        }
    }

    public List<CommentDTO> getCommentsByObject(Long objectId, int type, Long offsetId, Long userId) {
        List<CommentDO> commentDOList;
        if (offsetId == 0) {
            commentDOList = commentMapper.getByObjectId(objectId, type, 10);
        } else {
            CommentDO lastComment = commentMapper.get(offsetId);
            if (lastComment == null) {
                return new ArrayList<>();
            }
            commentDOList = commentMapper.getByObjectIdPaginated(objectId, type, lastComment.getScore(), offsetId, 10);
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

        List<UpvoteDO> upvoteList = upvoteMapper.getList(userId.intValue(), ids, Enums.ObjectType.comment.value());

        Set<Long> set = new HashSet<>();
        for (UpvoteDO upvoteDO : upvoteList) {
            set.add(upvoteDO.getObjectId());
        }

        for (CommentDTO commentDTO : commentDTOList) {
            commentDTO.setUpvoted(set.contains(commentDTO.getId()) ? 1 : 0);
        }

        for (CommentDTO commentDTO: map.values()) {
            commentDTO.setUpvoted(set.contains(commentDTO.getId()) ? 1 : 0);
        }
    }

    public List<CommentDTO> getCommentReplies(Long id, Long offsetId, Long userId) {
        List<CommentDO> commentDOList;
        if (offsetId == 0) {
            commentDOList = commentMapper.getByTopic(id, 10);
        } else {
            CommentDO lastComment = commentMapper.get(offsetId);
            if (lastComment == null) {
                return new ArrayList<>();
            }
            commentDOList = commentMapper.getByTopicPaginated(id, lastComment.getScore(), offsetId, 10);
        }

        List<Long> ids = commentDOList.stream().map(CommentDO::getId).toList();

        List<UpvoteDO> upvoteList = upvoteMapper.getList(userId.intValue(), ids, Enums.ObjectType.comment.value());

        Set<Long> set = new HashSet<>();
        for (UpvoteDO upvoteDO : upvoteList) {
            set.add(upvoteDO.getObjectId());
        }

        List<CommentDTO> commentDTOList = Converter.INSTANCE.toCommentDTO(commentDOList);
        for (CommentDTO commentDTO : commentDTOList) {
            commentDTO.setUpvoted(set.contains(commentDTO.getId()) ? 1 : 0);
        }
        return commentDTOList;
    }

    public List<CommentDTOV1> getPendingComments() {
        List<CommentDO> commentDOList = commentMapper.getListByState(submited.value(), 50);
        return Converter.INSTANCE.toCommentDTOV1(commentDOList);
    }

    @Transactional
    public CommentDTOV1 approveComment(Long id, boolean approve) {
        CommentDO commentDO = commentMapper.get(id);
        if (commentDO == null) {
            throw ErrorCode.COMMENT_NOT_FOUND.exception();
        }

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