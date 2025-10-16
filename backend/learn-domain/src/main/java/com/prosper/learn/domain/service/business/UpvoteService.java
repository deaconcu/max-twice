package com.prosper.learn.domain.service.business;

import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Enums.ObjectType;
import com.prosper.learn.common.exception.BusinessException;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.common.config.SystemProperties;
import com.prosper.learn.domain.service.basic.MessageService;
import com.prosper.learn.domain.service.basic.RedisStatsService;
import com.prosper.learn.domain.service.basic.ScoreCalculationService;
import com.prosper.learn.dto.response.UpvoteStatusDTO;
import com.prosper.learn.persistence.dataobject.CommentDO;
import com.prosper.learn.persistence.dataobject.PostDO;
import com.prosper.learn.persistence.dataobject.RoadmapDO;
import com.prosper.learn.persistence.dataobject.UpvoteDO;
import com.prosper.learn.persistence.dataobject.UserDO;
import com.prosper.learn.persistence.dataobject.MemoryCardDeckDO;
import com.prosper.learn.domain.service.data.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 点赞服务
 * 
 * 负责管理系统中的点赞功能，包括：
 * - 帖子点赞（支持多种点赞类型：once, twice, helpful）
 * - 评论点赞
 * - 路线图投票
 * - 点赞状态查询和批量查询
 * 
 * 核心功能：
 * - 支持点赞/取消点赞/切换点赞类型
 * - 自动更新统计数据和分数
 * - 发送点赞通知消息
 * - Redis统计数据记录
 * 
 * @author Claude
 * @since 2024-01-20
 */
@Service
@RequiredArgsConstructor
public class UpvoteService {

    /** 点赞类型名称常量 */
    private static final String UPVOTE_TYPE_NORMAL = "normal";
    private static final String UPVOTE_TYPE_TWICE = "twice";
    private static final String UPVOTE_TYPE_HELPFUL = "helpful";

    /** 用户数据访问接口 */
    private final UserDataService userDataService;
    
    /** 点赞数据访问接口 */
    private final UpvoteDataService upvoteDataService;
    
    /** 帖子数据访问接口 */
    private final PostDataService postDataService;
    
    /** 评论数据访问接口 */
    private final CommentDataService commentDataService;
    
    /** 路线图数据访问接口 */
    private final RoadmapDataService roadmapDataService;

    /** 记忆卡片组数据访问接口 */
    private final MemoryCardDeckDataService deckDataService;
    
    /** 消息服务，用于发送点赞通知 */
    private final MessageService messageService;
    
    /** Redis统计服务，用于记录点赞统计 */
    private final RedisStatsService redisStatsService;
    
    /** 分数计算服务，用于更新内容分数 */
    private final ScoreCalculationService scoreCalculationService;
    
    /** 系统配置属性 */
    private final SystemProperties systemProperties;

    /**
     * 验证用户ID有效性
     * 
     * @param userId 用户ID
     * @throws BusinessException 当用户ID无效时抛出异常
     */
    private void validateUserId(long userId) {
        if (userId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("用户ID无效: " + userId);
        }
    }
    
    /**
     * 验证用户存在性
     * 
     * @param userId 用户ID
     * @return 用户实体对象
     * @throws BusinessException 当用户不存在时抛出异常
     */
    private UserDO validateUserExists(long userId) {
        validateUserId(userId);
        UserDO userDO = userDataService.getById(userId);
        if (userDO == null) {
            throw ErrorCode.USER_NOT_FOUND.exception();
        }
        return userDO;
    }
    
    /**
     * 验证帖子存在性
     * 
     * @param postId 帖子ID
     * @return 帖子实体对象
     * @throws BusinessException 当帖子不存在时抛出异常
     */
    private PostDO validatePostExists(long postId) {
        if (postId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("帖子ID无效: " + postId);
        }
        PostDO postDO = postDataService.getById(postId);
        if (postDO == null) {
            throw ErrorCode.CONTENTS_POST_NOT_FOUND.exception();
        }
        return postDO;
    }
    
    /**
     * 验证评论存在性
     * 
     * @param commentId 评论ID
     * @return 评论实体对象
     * @throws BusinessException 当评论不存在时抛出异常
     */
    private CommentDO validateCommentExists(long commentId) {
        if (commentId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("评论ID无效: " + commentId);
        }
        CommentDO commentDO = commentDataService.getById(commentId);
        if (commentDO == null) {
            throw ErrorCode.COMMENT_NOT_FOUND.exception();
        }
        return commentDO;
    }
    
    /**
     * 验证点赞类型有效性
     * 
     * @param type 点赞类型
     * @throws BusinessException 当点赞类型无效时抛出异常
     */
    private void validateVoteType(int type) {
        if (!Enums.VoteType.isValid(type)) {
            throw ErrorCode.INVALID_PARAMETER.exception("无效的点赞类型: " + type);
        }
    }

    /**
     * 获取点赞类型名称
     * 
     * @param type 点赞类型数值
     * @return 点赞类型名称
     */
    private String getUpvoteTypeName(int type) {
        if (type == Enums.VoteType.normal.value()) {
            return UPVOTE_TYPE_NORMAL;
        } else if (type == Enums.VoteType.twice.value()) {
            return UPVOTE_TYPE_TWICE;
        } else {
            return UPVOTE_TYPE_HELPFUL;
        }
    }
    
    /**
     * 更新帖子点赞计数
     * 
     * @param postDO 帖子对象
     * @param type 点赞类型
     * @param increment 增量（1为增加，-1为减少）
     */
    private void updatePostVoteCount(PostDO postDO, int type, int increment) {
        if (type == Enums.VoteType.twice.value()) {
            postDO.setTwice(postDO.getTwice() + increment);
        } else {
            postDO.setHelpful(postDO.getHelpful() + increment);
        }
    }
    
    /**
     * 发送帖子点赞通知消息
     * 
     * @param postDO 帖子对象
     * @param fromUserId 点赞用户ID
     * @param type 点赞类型
     */
    private void sendPostUpvoteMessage(PostDO postDO, long fromUserId, int type) {
        messageService.createUpvoteMessage(
            postDO.getCreatorId(), fromUserId, postDO.getNodeId(),
            postDO.getId(), ObjectType.post.value(), type);
    }

    /**
     * 帖子点赞
     * 
     * 支持多种点赞类型：once, twice, helpful
     * 包含点赞、取消点赞、切换点赞类型的完整逻辑
     * 
     * @param postingId 帖子ID
     * @param userId 用户ID
     * @param type 点赞类型（1-once, 2-twice, 3-helpful）
     * @throws BusinessException 当参数无效或对象不存在时抛出异常
     */
    @Transactional
    public void upvotePost(long postingId, long userId, int type) {
        if (type != Enums.VoteType.twice.value() && type != Enums.VoteType.helpful.value()) {
            throw ErrorCode.INVALID_PARAMETER.exception("帖子仅支持 twice 或 helpful 点赞类型: " + type);
        }

        UserDO fromUserDO = validateUserExists(userId);
        PostDO postDO = validatePostExists(postingId);

        UpvoteDO upvoteDO = upvoteDataService.getByUserAndObject(userId, postDO.getId(), ObjectType.post.value());
        String upvoteTypeName = getUpvoteTypeName(type);

        if (upvoteDO != null && upvoteDO.getType() == type) {
            // 取消点赞
            upvoteDataService.delete(upvoteDO.getId());
            
            // 减少对应点赞数
            updatePostVoteCount(postDO, upvoteDO.getType(), -1);
            
            // 记录到Redis统计
            redisStatsService.removeUpvote(postDO.getId(), userId, upvoteTypeName);
            
            postDataService.update(postDO);
            
            // 智能更新文章分数
            scoreCalculationService.checkAndUpdatePostScore(postDO);
            return;
        }

        if (upvoteDO == null) {
            // 新增点赞
            upvoteDO = new UpvoteDO();
            upvoteDO.setUserId(userId);
            upvoteDO.setObjectId(postDO.getId());
            upvoteDO.setObjectType(ObjectType.post.value());
            upvoteDO.setType(type);
            upvoteDataService.insert(upvoteDO);
        } else {
            // 切换点赞类型
            String oldUpvoteTypeName = getUpvoteTypeName(upvoteDO.getType());
            
            // 减少原类型的点赞数
            updatePostVoteCount(postDO, upvoteDO.getType(), -1);
            
            // 记录到Redis统计
            redisStatsService.removeUpvote(postDO.getId(), userId, oldUpvoteTypeName);
            
            upvoteDO.setType(type);
            upvoteDataService.update(upvoteDO);
        }

        // 增加新类型的点赞数
        updatePostVoteCount(postDO, type, 1);
        
        // 发送点赞通知消息
        sendPostUpvoteMessage(postDO, fromUserDO.getId(), type);
        
        // 记录到Redis统计
        redisStatsService.recordUpvote(postDO.getId(), userId, upvoteTypeName);
        
        postDataService.update(postDO);
        
        // 智能更新文章分数
        scoreCalculationService.checkAndUpdatePostScore(postDO);
    }

    /**
     * 获取评论对应的节点ID
     * 
     * @param commentDO 评论对象
     * @return 节点ID
     * @throws BusinessException 当评论类型无效时抛出异常
     */
    private long getNodeIdFromComment(CommentDO commentDO) {
        if (commentDO.getObjectType() == ObjectType.node.value()) {
            return commentDO.getObjectId();
        } else if (commentDO.getObjectType() == ObjectType.post.value()) {
            PostDO postDO = validatePostExists(commentDO.getObjectId());
            return postDO.getNodeId();
        } else {
            throw ErrorCode.INVALID_PARAMETER.exception("无效的评论类型: " + commentDO.getObjectType());
        }
    }

    /**
     * 评论点赞
     * 
     * @param commentId 评论ID
     * @param userId 用户ID
     * @throws BusinessException 当参数无效或对象不存在时抛出异常
     */
    @Transactional
    public void upvoteComment(long commentId, long userId) {
        UserDO fromUserDO = validateUserExists(userId);
        CommentDO commentDO = validateCommentExists(commentId);

        // get node id
        long nodeId = getNodeIdFromComment(commentDO);

        UpvoteDO upvoteDO = upvoteDataService.getByUserAndObject(userId, commentId, ObjectType.comment.value());
        if (upvoteDO != null) {
            upvoteDataService.delete(upvoteDO.getId());
            commentDO.setUpvoteCount(commentDO.getUpvoteCount() - 1);

            // 更新评论分数
            scoreCalculationService.checkAndUpdateCommentScore(commentDO);
            commentDataService.update(commentDO);
            return;
        }

        upvoteDO = new UpvoteDO();
        upvoteDO.setUserId(userId);
        upvoteDO.setObjectId(commentDO.getId());
        upvoteDO.setObjectType(Enums.ObjectType.comment.value());
        upvoteDO.setType(0);
        upvoteDataService.insert(upvoteDO);

        commentDO.setUpvoteCount(commentDO.getUpvoteCount() + 1);

        // 更新评论分数
        scoreCalculationService.checkAndUpdateCommentScore(commentDO);
        commentDataService.update(commentDO);

        messageService.createUpvoteMessage(
                commentDO.getFromUserId(), userId, nodeId, commentId, ObjectType.comment.value(), 1);
    }

    /**
     * 课程投票
     * @param roadmapId 课程ID
     * @param userId 用户ID
     * @throws BusinessException 当参数无效时抛出异常
     */
    @Transactional
    public boolean upvoteRoadmap(long roadmapId, long userId) {
        validateUserId(userId);
        if (roadmapId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("路线图ID无效: " + roadmapId);
        }

        RoadmapDO roadmapDO = roadmapDataService.getById(roadmapId);
        if (roadmapDO == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("路线图不存在: " + roadmapId);
        }

        // 检查是否已经投过票
        UpvoteDO existingUpvote = upvoteDataService.getByUserAndObject(userId, roadmapId, ObjectType.roadmap.value());

        if (existingUpvote != null) {
            // 如果已经投过票，则取消投票
            upvoteDataService.delete(existingUpvote.getId());

            // 减少投票数
            int currentVote = roadmapDO.getVote() != null ? roadmapDO.getVote() : 0;
            roadmapDO.setVote(Math.max(0, currentVote - 1));
            roadmapDataService.update(roadmapDO);

            return false; // 返回false表示取消投票
        } else {
            // 如果没有投过票，则投票
            UpvoteDO upvoteDO = new UpvoteDO();
            upvoteDO.setUserId(userId);
            upvoteDO.setObjectId(roadmapId);
            upvoteDO.setObjectType(ObjectType.roadmap.value());
            upvoteDO.setType(0); // roadmap投票类型设为0，对应"once"
            upvoteDataService.insert(upvoteDO);

            // 增加投票数
            int currentVote = roadmapDO.getVote() != null ? roadmapDO.getVote() : 0;
            roadmapDO.setVote(currentVote + 1);
            roadmapDataService.update(roadmapDO);

            return true; // 返回true表示投票成功
        }
    }

    /**
     * 检查用户是否已经给课程投过票
     * @param roadmapId 课程ID
     * @param userId 用户ID
     * @return true表示已投票，false表示未投票
     */
    public boolean hasUpvotedRoadmap(long roadmapId, long userId) {
        validateUserId(userId);
        if (roadmapId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("路线图ID无效: " + roadmapId);
        }
        
        UpvoteDO upvoteDO = upvoteDataService.getByUserAndObject(userId, roadmapId, ObjectType.roadmap.value());
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

        List<UpvoteDO> upvotes = upvoteDataService.getList(userId, roadmapIds, ObjectType.roadmap.value());
        return upvotes.stream()
                .map(UpvoteDO::getObjectId)
                .collect(Collectors.toSet());
    }

    /**
     * 记忆卡片组点赞
     * @param deckId 卡片组ID
     * @param userId 用户ID
     * @return true表示点赞成功，false表示取消点赞
     */
    @Transactional
    public boolean upvoteMemoryCardDeck(long deckId, long userId) {
        validateUserId(userId);
        if (deckId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("卡片组ID无效: " + deckId);
        }

        // 验证卡片组存在
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);

        // 检查是否已经点过赞
        UpvoteDO existingUpvote = upvoteDataService.getByUserAndObject(userId, deckId, ObjectType.memory_card_deck.value());
        if (existingUpvote != null) {
            // 如果已经点过赞，则取消点赞
            upvoteDataService.delete(existingUpvote.getId());

            // 使用SQL原子操作减少点赞数
            deckDataService.decrementUpvoteCount(deckId);

            // 更新卡片组分数
            scoreCalculationService.checkAndUpdateMemoryCardDeckScore(deck);

            return false; // 返回false表示取消点赞
        } else {
            // 如果没有点过赞，则点赞
            UpvoteDO upvoteDO = new UpvoteDO();
            upvoteDO.setUserId(userId);
            upvoteDO.setObjectId(deckId);
            upvoteDO.setObjectType(ObjectType.memory_card_deck.value());
            upvoteDO.setType(0); // 卡片组点赞类型设为0
            upvoteDataService.insert(upvoteDO);

            // 使用SQL原子操作增加点赞数
            deckDataService.incrementUpvoteCount(deckId);

            // 更新卡片组分数
            scoreCalculationService.checkAndUpdateMemoryCardDeckScore(deck);

            return true; // 返回true表示点赞成功
        }
    }

    /**
     * 取消点赞
     * @param postingId 帖子ID
     * @param userId 用户ID
     * @throws BusinessException 当参数无效或对象不存在时抛出异常
     */
    @Transactional
    public void cancelVote(long postingId, long userId) {
        validateUserId(userId);
        if (postingId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("帖子ID无效: " + postingId);
        }
        
        UpvoteDO upvoteDO = upvoteDataService.getByUserAndObject(userId, postingId, ObjectType.post.value());
        if (upvoteDO == null) return;

        PostDO postDO = validatePostExists(postingId);

        cancelVote(postDO, upvoteDO);
    }

    /**
     * 取消点赞
     */
    public void cancelVote(PostDO postDO, UpvoteDO upvoteDO) {
        if (postDO == null || upvoteDO == null) return;
        upvoteDataService.delete(upvoteDO.getId());

        //postingDO.setVote(postingDO.getVote() - 1);
        postDataService.update(postDO);
    }

    /**
     * 获取用户对指定对象的点赞状态
     * @param objectId 对象ID
     * @param objectType 对象类型
     * @param userId 用户ID
     * @return 点赞状态DTO
     * @throws BusinessException 当参数无效时抛出异常
     */
    public UpvoteStatusDTO getUpvoteStatus(Long objectId, int objectType, long userId) {
        validateUserId(userId);
        if (objectId == null || objectId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("对象ID无效: " + objectId);
        }
        
        UpvoteDO upvoteDO = upvoteDataService.getByUserAndObject(userId, objectId, objectType);
        
        // 获取对象的点赞数据
        Integer upvotes = 0;
        Integer twiceUpvotes = null;
        Integer helpfulUpvotes = null;
        Boolean upvoted = false;
        Boolean twiceUpvoted = false;
        Boolean helpfulUpvoted = false;
        
        if (objectType == ObjectType.post.value()) {
            PostDO postDO = postDataService.getById(objectId);
            if (postDO != null) {
                // PostDO 使用 once, twice, helpful 统计
                upvotes = (postDO.getOnce() != null ? postDO.getOnce() : 0) +
                          (postDO.getTwice() != null ? postDO.getTwice() : 0) +
                          (postDO.getHelpful() != null ? postDO.getHelpful() : 0);
                twiceUpvotes = postDO.getTwice() != null ? postDO.getTwice() : 0;
                helpfulUpvotes = postDO.getHelpful() != null ? postDO.getHelpful() : 0;
                
                // 检查用户点赞状态
                if (upvoteDO != null) {
                    upvoted = true;
                    twiceUpvoted = upvoteDO.getType() == Enums.VoteType.twice.value();
                    helpfulUpvoted = upvoteDO.getType() == Enums.VoteType.helpful.value();
                }
            }
        } else if (objectType == ObjectType.comment.value()) {
            CommentDO commentDO = commentDataService.getById(objectId);
            if (commentDO != null) {
                upvotes = commentDO.getUpvoteCount() != null ? commentDO.getUpvoteCount() : 0;
                upvoted = upvoteDO != null;
            }
        } else if (objectType == ObjectType.roadmap.value()) {
            RoadmapDO roadmapDO = roadmapDataService.getById(objectId);
            if (roadmapDO != null) {
                upvotes = roadmapDO.getVote() != null ? roadmapDO.getVote() : 0;
                upvoted = upvoteDO != null;
            }
        } else if (objectType == ObjectType.memory_card_deck.value()) {
            MemoryCardDeckDO deckDO = deckDataService.getById(objectId);
            if (deckDO != null) {
                upvotes = deckDO.getUpvoteCount() != null ? deckDO.getUpvoteCount() : 0;
                upvoted = upvoteDO != null;
            }
        }
        
        return UpvoteStatusDTO.builder()
                .objectId(objectId)
                .objectType(objectType)
                .upvotes(upvotes)
                .upvoted(upvoted)
                .twiceUpvotes(twiceUpvotes)
                .twiceUpvoted(twiceUpvoted)
                .helpfulUpvotes(helpfulUpvotes)
                .helpfulUpvoted(helpfulUpvoted)
                .build();
    }
}
