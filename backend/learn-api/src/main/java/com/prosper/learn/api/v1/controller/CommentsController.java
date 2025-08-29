package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums;
import com.prosper.learn.domain.service.MessageService;
import com.prosper.learn.domain.service.ScoreCalculationService;
import com.prosper.learn.domain.service.RedisStatsService;
import com.prosper.learn.dto.CommentDTO;
import com.prosper.learn.dto.CommentDTOV1;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.prosper.learn.common.Enums.CommentState.submited;
import static com.prosper.learn.common.Enums.MessageType.*;

/**
 * 评论管理接口
 * 从CommentClient迁移而来
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentsController {

    private final UserMapper userMapper;
    private final UpvoteMapper upvoteMapper;
    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final NodeMapper nodeMapper;
    private final RoadmapMapper roadmapMapper;
    private final MessageService messageService;
    private final ScoreCalculationService scoreCalculationService;
    private final RedisStatsService redisStatsService;

    /**
     * 创建评论
     * 映射: POST /comment → POST /api/v1/comments
     */
    @PostMapping("/comments")
    @Transactional
    public ApiResponse<Object> createComment(@RequestBody CommentDTO commentDTO) {
        long userId = StpUtil.getLoginIdAsLong();
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

        commentDO = commentMapper.get(commentDO.getId());
        return ApiResponse.success(commentDO);
    }

    /**
     * 获取对象评论
     * 映射: GET /comment → GET /api/v1/comments?objectId=123&type=1&offsetId=0
     */
    @GetMapping("/comments")
    public ApiResponse<List<CommentDTO>> getCommentsByObject(
            @RequestParam Long objectId, 
            @RequestParam int type, 
            @RequestParam Long offsetId) {
        
        List<CommentDO> commentDOList;
        if (offsetId == 0) {
            commentDOList = commentMapper.getByObjectId(objectId, type, 10);
        } else {
            CommentDO lastComment = commentMapper.get(offsetId);
            if (lastComment == null) {
                return ApiResponse.success(new ArrayList<>());
            }
            commentDOList = commentMapper.getByObjectIdPaginated(objectId, type, lastComment.getScore(), offsetId, 10);
        }

        List<CommentDTO> commentDTOList = Converter.INSTANCE.toCommentDTO(commentDOList);

        int userId = StpUtil.getLoginIdAsInt();

        List<Long> ids = new ArrayList<>();
        for (CommentDO commentDO : commentDOList) {
            ids.add(commentDO.getId());
        }

        if (!ids.isEmpty()) {
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

            List<UpvoteDO> upvoteList = upvoteMapper.getList(userId, ids, Enums.ObjectType.comment.value());

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

        return ApiResponse.success(commentDTOList);
    }

    /**
     * 获取评论回复
     * 映射: GET /comment/{id}/reply → GET /api/v1/comments/{id}/replies?offsetId=0
     */
    @GetMapping("/comments/{id}/replies")
    public ApiResponse<List<CommentDTO>> getCommentReplies(
            @PathVariable Long id, 
            @RequestParam Long offsetId) {
        
        int userId = StpUtil.getLoginIdAsInt();
        List<CommentDO> commentDOList;
        if (offsetId == 0) {
            commentDOList = commentMapper.getByTopic(id, 10);
        } else {
            CommentDO lastComment = commentMapper.get(offsetId);
            if (lastComment == null) {
                return ApiResponse.success(new ArrayList<>());
            }
            commentDOList = commentMapper.getByTopicPaginated(id, lastComment.getScore(), offsetId, 10);
        }

        List<Long> ids = commentDOList.stream().map(CommentDO::getId).toList();

        List<UpvoteDO> upvoteList = upvoteMapper.getList(userId, ids, Enums.ObjectType.comment.value());

        Set<Long> set = new HashSet<>();
        for (UpvoteDO upvoteDO : upvoteList) {
            set.add(upvoteDO.getObjectId());
        }

        List<CommentDTO> commentDTOList = Converter.INSTANCE.toCommentDTO(commentDOList);
        for (CommentDTO commentDTO : commentDTOList) {
            commentDTO.setUpvoted(set.contains(commentDTO.getId()) ? 1 : 0);
        }
        return ApiResponse.success(commentDTOList);
    }

    /**
     * 获取待审核评论
     * 映射: GET /comment/censor → GET /api/v1/admin/comments/pending
     */
    @GetMapping("/admin/comments/pending")
    public ApiResponse<List<CommentDTOV1>> getPendingComments() {
        List<CommentDO> commentDOList = commentMapper.getListByState(submited.value(), 50);
        return ApiResponse.success(Converter.INSTANCE.toCommentDTOV1(commentDOList));
    }

    /**
     * 审核评论
     * 映射: PUT /comment → PUT /api/v1/admin/comments/{id}/approve
     */
    @PutMapping("/admin/comments/{id}/approve")
    public ApiResponse<Object> approveComment(@PathVariable Long id, @RequestParam boolean approve) {
        CommentDO commentDO = commentMapper.get(id);
        if (commentDO == null) throw new IllegalArgumentException("评论不存在");

        if (approve && commentDO.getState() != Enums.CommentState.approved.value()) {
            commentDO.setState(Enums.CommentState.approved.value());
            commentMapper.update(commentDO);
        }
        if (!approve && commentDO.getState() != Enums.CommentState.deleted.value()) {
            commentDO.setState(Enums.CommentState.deleted.value());
            commentMapper.update(commentDO);
        }
        return ApiResponse.success(Converter.INSTANCE.toCommentDTOV1(commentDO));
    }
}