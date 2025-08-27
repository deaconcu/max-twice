package com.prosper.learn.api.web;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.client.CommentClient;
import com.prosper.learn.common.Enums;
import com.prosper.learn.domain.service.MessageService;
import com.prosper.learn.domain.service.ScoreCalculationService;
import com.prosper.learn.domain.service.RedisStatsService;
import com.prosper.learn.dto.CommentDTO;
import com.prosper.learn.dto.CommentDTOV1;
import com.prosper.learn.dto.Response;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

import static com.prosper.learn.common.Enums.CommentState.submited;
import static com.prosper.learn.common.Enums.MessageType.*;

@RestController
//@SaCheckLogin
@RequiredArgsConstructor
public class CommentController implements CommentClient {

    private final UserMapper userMapper;
    private final UpvoteMapper upvoteMapper;
    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final NodeMapper nodeMapper;
    private final RoadmapMapper roadmapMapper;
    private final MessageService messageService;
    private final ScoreCalculationService scoreCalculationService;
    private final RedisStatsService redisStatsService;

    @Override
    @Transactional
    public Response<Object> create(CommentDTO commentDTO) {
        int userId = StpUtil.getLoginIdAsInt();
        commentDTO.setFromUser(userId);
        UserDO fromUser = userMapper.getById(userId);

        // update posting comment count
        PostDO postDO = null;
        NodeDO nodeDO = null;
        RoadmapDO roadmapDO = null;

        if (commentDTO.getType() == Enums.ObjectType.post.value) {
            postDO = postMapper.get(commentDTO.getObjectId());
            if (postDO == null) throw new IllegalArgumentException("帖子不存在");
        } else if (commentDTO.getType() == Enums.ObjectType.node.value) {
            nodeDO = nodeMapper.getById(commentDTO.getObjectId());
            if (nodeDO == null) throw new IllegalArgumentException("节点不存在");
        } else if (commentDTO.getType() == Enums.ObjectType.roadmap.value) {
            roadmapDO = roadmapMapper.get(commentDTO.getObjectId());
            if (roadmapDO == null) throw new IllegalArgumentException("路线图不存在");
        } else {
            throw new IllegalArgumentException("评论类型不正确");
        }

        // insert comment
        CommentDO commentDO = Converter.INSTANCE.toCommentDO(commentDTO);
        // 初始化评论分数
        commentDO.setScore(scoreCalculationService.calculateCommentScore(commentDO));
        commentMapper.insert(commentDO);

        // update parent comment reply count
        if (commentDTO.getReplyTo() != 0) {
            CommentDO parentCommentDO = commentMapper.get(commentDTO.getReplyTo());
            if (parentCommentDO == null) throw new IllegalArgumentException("父评论不存在");
            parentCommentDO.setReplyCount(parentCommentDO.getReplyCount() + 1);

            // 更新父评论分数（由于回复数增加）
            if (scoreCalculationService.checkAndUpdateCommentScore(parentCommentDO)) {
                commentMapper.update(parentCommentDO);
            } else {
                commentMapper.update(parentCommentDO);
            }

            if (commentDTO.getType() == Enums.ObjectType.node.value && nodeDO != null) {
                messageService.createCommentMessage(
                        parentCommentDO.getFromUser(), fromUser.getId(), nodeDO.getId(), commentDO.getId(), replyNodeComment.value);
            } else if (commentDTO.getType() == Enums.ObjectType.post.value && postDO != null) {
                messageService.createCommentMessage(
                        parentCommentDO.getFromUser(), fromUser.getId(), postDO.getNodeId(), commentDO.getId(), replyPostingComment.value);
            } else if (commentDTO.getType() == Enums.ObjectType.roadmap.value && roadmapDO != null) {
                messageService.createCommentMessage(
                        parentCommentDO.getFromUser(), fromUser.getId(), roadmapDO.getId(), commentDO.getId(), replyRoadmapComment.value);
            }
        }

        if (commentDTO.getType() == Enums.ObjectType.post.value && postDO != null) {
            postDO.setCommentCount(postDO.getCommentCount() + 1);
            postMapper.update(postDO);
            
            // 记录到Redis统计
            redisStatsService.recordComment((long) postDO.getId(), userId);
            
            messageService.createCommentMessage(postDO.getCreator(), fromUser.getId(), postDO.getNodeId(), commentDO.getId(), postComment.value);
        } else if (commentDTO.getType() == Enums.ObjectType.node.value && nodeDO != null) {
            nodeDO.setCommentCount(nodeDO.getCommentCount() + 1);
            nodeMapper.update(nodeDO);
            
            // 记录到Redis统计
            redisStatsService.recordComment((long) nodeDO.getId(), userId);
            
            messageService.createCommentMessage(nodeDO.getCreator(), fromUser.getId(), nodeDO.getId(), commentDO.getId(), nodeComment.value);
        } else if (commentDTO.getType() == Enums.ObjectType.roadmap.value && roadmapDO != null) {
            roadmapDO.setComment(roadmapDO.getComment() + 1);
            roadmapMapper.update(roadmapDO);
            
            // 记录到Redis统计
            redisStatsService.recordComment((long) roadmapDO.getId(), userId);
            
            messageService.createCommentMessage(roadmapDO.getCreatorId(), fromUser.getId(), roadmapDO.getId(), commentDO.getId(), roadmapComment.value);
        }

        commentDO = commentMapper.get(commentDO.getId());
        return new Response<>(commentDO);
    }

    @Override
    public Response<List<CommentDTO>> getByObject(int objectId, int type, int offsetId) {
        List<CommentDO> commentDOList;
        if (offsetId == 0) {
            // 首页加载，直接按分数排序
            commentDOList = commentMapper.getByObjectId(objectId, type, 10);
        } else {
            // 分页加载，需要先获取最后一条评论的分数
            CommentDO lastComment = commentMapper.get(offsetId);
            if (lastComment == null) {
                return new Response<>(new ArrayList<>());
            }
            commentDOList = commentMapper.getByObjectIdPaginated(objectId, type, lastComment.getScore(), offsetId, 10);
        }

        List<CommentDTO> commentDTOList = Converter.INSTANCE.toCommentDTO(commentDOList);

        int userId = StpUtil.getLoginIdAsInt();

        List<Integer> ids = new ArrayList<>();
        for (CommentDO commentDO : commentDOList) {
            ids.add(commentDO.getId());
        }

        if (!ids.isEmpty()) {
            List<CommentDO> children = commentMapper.getChildren(ids);

            HashMap<Integer, CommentDTO> map = new HashMap<>();
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

            List<UpvoteDO> upvoteList = upvoteMapper.getList(userId, ids, Enums.ObjectType.comment.value);

            Set<Integer> set = new HashSet<>();
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

        return new Response<>(commentDTOList);
    }

    @Override
    public Response<List<CommentDTO>> getByTopic(int commentId, int offsetId) {
        int userId = StpUtil.getLoginIdAsInt();
        List<CommentDO> commentDOList;
        if (offsetId == 0) {
            // 首页加载话题回复，直接按分数排序
            commentDOList = commentMapper.getByTopic(commentId, 10);
        } else {
            // 分页加载话题回复，需要先获取最后一条评论的分数
            CommentDO lastComment = commentMapper.get(offsetId);
            if (lastComment == null) {
                return new Response<>(new ArrayList<>());
            }
            commentDOList = commentMapper.getByTopicPaginated(commentId, lastComment.getScore(), offsetId, 10);
        }

        List<Integer> ids = commentDOList.stream().map(CommentDO::getId).toList();

        List<UpvoteDO> upvoteList = upvoteMapper.getList(userId, ids, Enums.ObjectType.comment.value);

        Set<Integer> set = new HashSet<>();
        for (UpvoteDO upvoteDO : upvoteList) {
            set.add(upvoteDO.getObjectId());
        }

        List<CommentDTO> commentDTOList = Converter.INSTANCE.toCommentDTO(commentDOList);
        for (CommentDTO commentDTO : commentDTOList) {
            commentDTO.setUpvoted(set.contains(commentDTO.getId()) ? 1 : 0);
        }
        return new Response<>(commentDTOList);
    }

    @Override
    public Response<List<CommentDTOV1>> getCensorList() {
        List<CommentDO> commentDOList = commentMapper.getListByState(submited.value, 50);
        return new Response<>(Converter.INSTANCE.toCommentDTOV1(commentDOList));
    }

    @Override
    public Response<Object> approve(int id, boolean approve) {
        CommentDO commentDO = commentMapper.get(id);
        if (commentDO == null) throw new IllegalArgumentException("评论不存在");

        if (approve && commentDO.getState() != Enums.CommentState.approved.value) {
            commentDO.setState(Enums.CommentState.approved.value);
            commentMapper.update(commentDO);
        }
        if (!approve && commentDO.getState() != Enums.CommentState.deleted.value) {
            commentDO.setState(Enums.CommentState.deleted.value);
            commentMapper.update(commentDO);
        }
        return new Response<>(Converter.INSTANCE.toCommentDTOV1(commentDO));
    }
}

