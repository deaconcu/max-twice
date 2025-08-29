package com.prosper.learn.domain.service.business;

import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Enums.ObjectType;
import com.prosper.learn.common.exception.BusinessException;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.config.SystemProperties;
import com.prosper.learn.domain.service.basic.MessageService;
import com.prosper.learn.domain.service.basic.RedisStatsService;
import com.prosper.learn.domain.service.basic.ScoreCalculationService;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.CommentDTO;
import com.prosper.learn.dto.PostDTO;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private static final String UPVOTE_TYPE_ONCE = "once";
    private static final String UPVOTE_TYPE_TWICE = "twice";
    private static final String UPVOTE_TYPE_HELPFUL = "helpful";

    /** 用户数据访问接口 */
    private final UserMapper userMapper;
    
    /** 点赞数据访问接口 */
    private final UpvoteMapper upvoteMapper;
    
    /** 帖子数据访问接口 */
    private final PostMapper postMapper;
    
    /** 评论数据访问接口 */
    private final CommentMapper commentMapper;
    
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
        UserDO userDO = userMapper.getById(userId);
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
        PostDO postDO = postMapper.get(postId);
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
        CommentDO commentDO = commentMapper.get(commentId);
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
        if (type == Enums.VoteType.once.value()) {
            return UPVOTE_TYPE_ONCE;
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
        if (type == Enums.VoteType.once.value()) {
            postDO.setOnce(postDO.getOnce() + increment);
        } else if (type == Enums.VoteType.twice.value()) {
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
            postDO.getCreator(), fromUserId, postDO.getNodeId(), 
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
        validateVoteType(type);
        UserDO fromUserDO = validateUserExists(userId);
        PostDO postDO = validatePostExists(postingId);

        UpvoteDO upvoteDO = upvoteMapper.get(userId, postDO.getId(), ObjectType.post.value());
        String upvoteTypeName = getUpvoteTypeName(type);

        if (upvoteDO != null && upvoteDO.getType() == type) {
            // 取消点赞
            upvoteMapper.delete(upvoteDO.getId());
            
            // 减少对应点赞数
            updatePostVoteCount(postDO, upvoteDO.getType(), -1);
            
            // 记录到Redis统计
            redisStatsService.removeUpvote(postDO.getId(), userId, upvoteTypeName);
            
            postMapper.update(postDO);
            
            // 智能更新文章分数
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
            updatePostVoteCount(postDO, upvoteDO.getType(), -1);
            
            // 记录到Redis统计
            redisStatsService.removeUpvote(postDO.getId(), userId, oldUpvoteTypeName);
            
            upvoteDO.setType(type);
            upvoteMapper.update(upvoteDO);
        }

        // 增加新类型的点赞数
        updatePostVoteCount(postDO, type, 1);
        
        // 发送点赞通知消息
        sendPostUpvoteMessage(postDO, fromUserDO.getId(), type);
        
        // 记录到Redis统计
        redisStatsService.recordUpvote(postDO.getId(), userId, upvoteTypeName);
        
        postMapper.update(postDO);
        
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
        if (commentDO.getType() == ObjectType.node.value()) {
            return commentDO.getObjectId();
        } else if (commentDO.getType() == ObjectType.post.value()) {
            PostDO postDO = validatePostExists(commentDO.getObjectId());
            return postDO.getNodeId();
        } else {
            throw ErrorCode.INVALID_PARAMETER.exception("无效的评论类型: " + commentDO.getType());
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

        UpvoteDO upvoteDO = upvoteMapper.get(userId, commentId, ObjectType.comment.value());
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
        upvoteDO.setObjectType(Enums.ObjectType.comment.value());
        upvoteDO.setType(0);
        upvoteMapper.insert(upvoteDO);

        commentDO.setUpvoteCount(commentDO.getUpvoteCount() + 1);

        // 更新评论分数
        scoreCalculationService.checkAndUpdateCommentScore(commentDO);
        commentMapper.update(commentDO);

        messageService.createUpvoteMessage(
                commentDO.getFromUser(), userId, nodeId, commentId, ObjectType.comment.value(), 1);
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
        
        // 检查是否已经投过票
        UpvoteDO existingUpvote = upvoteMapper.get(userId, roadmapId, ObjectType.roadmap.value());

        if (existingUpvote != null) {
            // 如果已经投过票，则取消投票
            upvoteMapper.delete(existingUpvote.getId());

            return false; // 返回false表示取消投票
        } else {
            // 如果没有投过票，则投票
            UpvoteDO upvoteDO = new UpvoteDO();
            upvoteDO.setUserId(userId);
            upvoteDO.setObjectId(roadmapId);
            upvoteDO.setObjectType(ObjectType.roadmap.value());
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
    public boolean hasUpvotedRoadmap(long roadmapId, int userId) {
        validateUserId(userId);
        if (roadmapId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("路线图ID无效: " + roadmapId);
        }
        
        UpvoteDO upvoteDO = upvoteMapper.get(userId, roadmapId, ObjectType.roadmap.value());
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

        List<UpvoteDO> upvotes = upvoteMapper.getList(userId, roadmapIds, ObjectType.roadmap.value());
        return upvotes.stream()
                .map(UpvoteDO::getObjectId)
                .collect(Collectors.toSet());
    }

    /**
     * 取消点赞
     * @param postingId 帖子ID
     * @param userId 用户ID
     * @throws BusinessException 当参数无效或对象不存在时抛出异常
     */
    @Transactional
    public void cancelVote(int postingId, int userId) {
        validateUserId(userId);
        if (postingId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("帖子ID无效: " + postingId);
        }
        
        UpvoteDO upvoteDO = upvoteMapper.get(userId, postingId, ObjectType.post.value());
        if (upvoteDO == null) return;

        PostDO postDO = validatePostExists(postingId);

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

    /**
     * 获取完整的点赞对象信息(带用户点赞状态)
     * @param objectId 对象ID
     * @param objectType 对象类型
     * @param userId 用户ID
     * @return 返回完整的对象信息
     * @throws BusinessException 当参数无效或对象不存在时抛出异常
     */
    public Object getUpvoteObjectWithStatus(Long objectId, int objectType, long userId) {
        validateUserId(userId);
        if (objectId == null || objectId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("对象ID无效: " + objectId);
        }
        
        if (objectType == ObjectType.post.value()) {
            PostDO postDO = validatePostExists(objectId);
            
            PostDTO postDTO = Converter.INSTANCE.toPostDTO(postDO);
            UpvoteDO upvoteDO = upvoteMapper.get(userId, objectId, ObjectType.post.value());
            if (upvoteDO != null) {
                postDTO.setVoteType(upvoteDO.getType());
            }
            return postDTO;
        } else if (objectType == ObjectType.comment.value()) {
            CommentDO commentDO = validateCommentExists(objectId);
            
            CommentDTO commentDTO = Converter.INSTANCE.toCommentDTO(commentDO);
            UpvoteDO upvoteDO = upvoteMapper.get(userId, objectId, ObjectType.comment.value());
            if (upvoteDO != null) {
                commentDTO.setUpvoted(1);
            }
            return commentDTO;
        } else {
            throw ErrorCode.INVALID_PARAMETER.exception("不支持的对象类型: " + objectType);
        }
    }

    /**
     * 获取用户对指定对象的点赞状态
     * @param objectId 对象ID
     * @param objectType 对象类型
     * @param userId 用户ID
     * @return 包含点赞状态的Map
     * @throws BusinessException 当参数无效时抛出异常
     */
    public Map<String, Object> getUpvoteStatus(Long objectId, int objectType, long userId) {
        validateUserId(userId);
        if (objectId == null || objectId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("对象ID无效: " + objectId);
        }
        
        UpvoteDO upvoteDO = upvoteMapper.get(userId, objectId, objectType);
        
        Map<String, Object> result = new HashMap<>();
        result.put("objectId", objectId);
        result.put("objectType", objectType);
        result.put("upvoted", upvoteDO != null);
        if (upvoteDO != null) {
            result.put("type", upvoteDO.getType());
        }
        
        return result;
    }
}
