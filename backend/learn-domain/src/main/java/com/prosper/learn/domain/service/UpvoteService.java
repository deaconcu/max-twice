package com.prosper.learn.domain.service;

import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Enums.ObjectType;
import com.prosper.learn.persistence.dataobject.CommentDO;
import com.prosper.learn.persistence.dataobject.PostDO;
import com.prosper.learn.persistence.dataobject.UpvoteDO;
import com.prosper.learn.persistence.dataobject.UserDO;
import com.prosper.learn.persistence.mapper.CommentMapper;
import com.prosper.learn.persistence.mapper.PostMapper;
import com.prosper.learn.persistence.mapper.UpvoteMapper;
import com.prosper.learn.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UpvoteService {

    private final UserMapper userMapper;
    private final UpvoteMapper upvoteMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final MessageService messageService;
    private final RedisStatsService redisStatsService;
    private final ScoreCalculationService scoreCalculationService;

    /**
     * 点赞
     * type 1 once 2 twice 3 helpful
     */
    @Transactional
    public void upvotePost(long postingId, long userId, int type) {
        if (!Enums.VoteType.isValid(type)) return;
        UserDO fromUserDO = userMapper.getById(userId);
        if (fromUserDO == null) return;

        PostDO postDO = postMapper.get(postingId);
        if (postDO == null) return;

        UpvoteDO upvoteDO = upvoteMapper.get(userId, postDO.getId(), ObjectType.post.value);

        // 获取点赞类型名称
        String upvoteTypeName = getUpvoteTypeName(type);

        if (upvoteDO != null && upvoteDO.getType() == type) {
            // 取消点赞
            upvoteMapper.delete(upvoteDO.getId());

            // 减少对应点赞数
            if (upvoteDO.getType() == Enums.VoteType.once.value) {
                postDO.setOnce(postDO.getOnce() - 1);
            } else if (upvoteDO.getType() == Enums.VoteType.twice.value) {
                postDO.setTwice(postDO.getTwice() - 1);
            } else {
                postDO.setHelpful(postDO.getHelpful() - 1);
            }

            // 记录到Redis统计
            redisStatsService.removeUpvote((long) postDO.getId(), userId, upvoteTypeName);

            postMapper.update(postDO);

            // 智能更新文章分数（检查时间间隔）
            scoreCalculationService.checkAndUpdatePostScore(postDO);
            return;
        }

        if (upvoteDO == null) {
            // 新增点赞
            upvoteDO = new UpvoteDO();
            upvoteDO.setUserId(userId);
            upvoteDO.setObjectId(postDO.getId());
            upvoteDO.setType(type);
            upvoteMapper.insert(upvoteDO);
        } else {
            // 切换点赞类型
            String oldUpvoteTypeName = getUpvoteTypeName(upvoteDO.getType());

            // 减少原类型的点赞数
            if (upvoteDO.getType() == Enums.VoteType.once.value) {
                postDO.setOnce(postDO.getOnce() - 1);
            } else if (upvoteDO.getType() == Enums.VoteType.twice.value) {
                postDO.setTwice(postDO.getTwice() - 1);
            } else {
                postDO.setHelpful(postDO.getHelpful() - 1);
            }

            // 记录到Redis统计
            redisStatsService.removeUpvote((long) postDO.getId(), userId, oldUpvoteTypeName);

            upvoteDO.setType(type);
            upvoteMapper.update(upvoteDO);
        }

        // 增加新类型的点赞数
        if (type == Enums.VoteType.once.value) {
            postDO.setOnce(postDO.getOnce() + 1);
            messageService.createUpvoteMessage(
                    postDO.getCreator(), fromUserDO.getId(), postDO.getNodeId(), postDO.getId(), ObjectType.post.value, 1);
        } else if (type == Enums.VoteType.twice.value) {
            postDO.setTwice(postDO.getTwice() + 1);
            messageService.createUpvoteMessage(
                    postDO.getCreator(), fromUserDO.getId(), postDO.getNodeId(), postDO.getId(), ObjectType.post.value, 2);
        } else {
            postDO.setHelpful(postDO.getHelpful() + 1);
            messageService.createUpvoteMessage(
                    postDO.getCreator(), fromUserDO.getId(), postDO.getNodeId(), postDO.getId(), ObjectType.post.value, 3);
        }

        // 记录到Redis统计
        redisStatsService.recordUpvote((long) postDO.getId(), userId, upvoteTypeName);

        postMapper.update(postDO);

        // 智能更新文章分数（检查时间间隔）
        scoreCalculationService.checkAndUpdatePostScore(postDO);
    }

    /**
     * 获取点赞类型名称
     */
    private String getUpvoteTypeName(int type) {
        if (type == Enums.VoteType.once.value) {
            return "once";
        } else if (type == Enums.VoteType.twice.value) {
            return "twice";
        } else {
            return "helpful";
        }
    }

    public void upvoteComment(long commentId, long userId) {
        UserDO fromUserDO = userMapper.getById(userId);
        if (fromUserDO == null) return;

        CommentDO commentDO = commentMapper.get(commentId);
        if (commentDO == null) return;

        // get node id
        int nodeId = 0;
        if (commentDO.getType() == ObjectType.node.value) {
            nodeId = commentDO.getObjectId();
        } else if (commentDO.getType() == ObjectType.post.value) {
            PostDO postDO = postMapper.get(commentDO.getObjectId());
            nodeId = postDO.getNodeId();
        } else {
            throw new RuntimeException("Invalid comment type");
        }

        UpvoteDO upvoteDO = upvoteMapper.get(userId, commentId, ObjectType.comment.value);
        if (upvoteDO != null) {
            upvoteMapper.delete(upvoteDO.getId());
            commentDO.setUpvoteCount(commentDO.getUpvoteCount() - 1);

            // 更新评论分数
            scoreCalculationService.checkAndUpdateCommentScore(commentDO);
            commentMapper.update(commentDO);
            return;
        }

        upvoteDO = new UpvoteDO();
        upvoteDO.setUserId(userId);
        upvoteDO.setObjectId(commentDO.getId());
        upvoteDO.setObjectType(Enums.ObjectType.comment.value);
        upvoteDO.setType(0);
        upvoteMapper.insert(upvoteDO);

        commentDO.setUpvoteCount(commentDO.getUpvoteCount() + 1);

        // 更新评论分数
        scoreCalculationService.checkAndUpdateCommentScore(commentDO);
        commentMapper.update(commentDO);

        messageService.createUpvoteMessage(
                commentDO.getFromUser(), userId, nodeId, commentId, ObjectType.comment.value, 1);
    }

    /**
     * 课程投票
     * @param roadmapId 课程ID
     * @param userId 用户ID
     */
    @Transactional
    public boolean upvoteRoadmap(int roadmapId, int userId) {
        // 检查是否已经投过票
        UpvoteDO existingUpvote = upvoteMapper.get(userId, roadmapId, ObjectType.roadmap.value);

        if (existingUpvote != null) {
            // 如果已经投过票，则取消投票
            upvoteMapper.delete(existingUpvote.getId());

            return false; // 返回false表示取消投票
        } else {
            // 如果没有投过票，则投票
            UpvoteDO upvoteDO = new UpvoteDO();
            upvoteDO.setUserId(userId);
            upvoteDO.setObjectId(roadmapId);
            upvoteDO.setObjectType(ObjectType.roadmap.value);
            upvoteDO.setType(0); // roadmap投票类型设为0，对应"once"
            upvoteMapper.insert(upvoteDO);

            return true; // 返回true表示投票成功
        }
    }

    /**
     * 检查用户是否已经给课程投过票
     * @param roadmapId 课程ID
     * @param userId 用户ID
     * @return true表示已投票，false表示未投票
     */
    public boolean hasUpvotedRoadmap(int roadmapId, int userId) {
        UpvoteDO upvoteDO = upvoteMapper.get(userId, roadmapId, ObjectType.roadmap.value);
        return upvoteDO != null;
    }

    /**
     * 批量检查用户对课程的投票状态
     * @param roadmapIds 课程ID列表
     * @param userId 用户ID
     * @return 已投票的课程ID集合
     */
    public Set<Long> getUpvotedRoadmapIds(List<Long> roadmapIds, long userId) {
        if (roadmapIds == null || roadmapIds.isEmpty() || userId <= 0) {
            return new HashSet<>();
        }

        List<UpvoteDO> upvotes = upvoteMapper.getList(userId, roadmapIds, ObjectType.roadmap.value);
        return upvotes.stream()
                .map(UpvoteDO::getObjectId)
                .collect(Collectors.toSet());
    }

    /**
     * 取消点赞
     * @param postingId
     * @param userId
     */
    @Transactional
    public void cancelVote(int postingId, int userId) {
        UpvoteDO upvoteDO = upvoteMapper.get(userId, postingId, ObjectType.post.value);
        if (upvoteDO == null) return;

        PostDO postDO = postMapper.get(postingId);
        if (postDO == null) return;

        cancelVote(postDO, upvoteDO);
    }

    /**
     * 取消点赞
     */
    public void cancelVote(PostDO postDO, UpvoteDO upvoteDO) {
        if (postDO == null || upvoteDO == null) return;
        upvoteMapper.delete(upvoteDO.getId());

        //postingDO.setVote(postingDO.getVote() - 1);
        postMapper.update(postDO);
    }
}
