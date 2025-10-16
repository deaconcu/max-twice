package com.prosper.learn.domain.service.basic;

import com.prosper.learn.common.exception.BusinessException;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.common.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;

/**
 * Redis统计数据服务
 * 
 * 负责实时记录用户行为统计数据到Redis，提供高性能的统计数据收集功能。
 * 
 * 设计思路:
 * 1. 使用Redis Hash结构存储统计数据，支持原子性的增减操作
 * 2. 数据按日期分组，便于后续的批量同步处理
 * 3. 设置合理的过期时间，避免Redis内存无限增长
 * 4. 双维度统计：既统计文章维度，也统计用户维度的数据
 * 
 * Redis数据结构:
 * - 用户统计: stats:YYYY-MM-DD:user -> {userId:statType: count}
 * - 文章统计: stats:YYYY-MM-DD:post -> {postId:statType: count}
 * 
 * 统计类型包括:
 * - view: 浏览量
 * - twice: 两次能懂
 * - helpful: 有用点赞
 * - comment: 评论数
 * 
 * 数据流向: Redis实时统计 -> 定时同步 -> 数据库持久化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStatsService {

    /** Redis键前缀常量 */
    private static final String STATS_KEY_PREFIX = "stats:";
    
    /** 用户统计键后缀常量 */
    private static final String USER_STATS_SUFFIX = ":user";
    
    /** 帖子统计键后缀常量 */
    private static final String POST_STATS_SUFFIX = ":post";
    
    /** 统计类型常量 */
    private static final String STAT_TYPE_VIEW = "view";
    private static final String STAT_TYPE_TWICE = "twice";
    private static final String STAT_TYPE_HELPFUL = "helpful";
    private static final String STAT_TYPE_COMMENT = "comment";
    
    /** Redis过期时间（天数）常量 */
    private static final int DEFAULT_EXPIRE_DAYS = 3;

    /** Redis模板，用于统计数据的存储和操作 */
    private final RedisTemplate<String, String> redisTemplate;
    
    /** 系统配置属性 */
    private final SystemProperties systemProperties;

    /**
     * 验证文章ID有效性
     * 
     * @param articleId 文章ID
     * @throws BusinessException 当文章ID无效时抛出异常
     */
    private void validateArticleId(long articleId) {
        if (articleId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("文章ID无效: " + articleId);
        }
    }
    
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
     * 验证点赞类型有效性
     * 
     * @param upvoteType 点赞类型
     * @throws BusinessException 当点赞类型无效时抛出异常
     */
    private void validateUpvoteType(String upvoteType) {
        if (upvoteType == null || upvoteType.trim().isEmpty()) {
            throw ErrorCode.INVALID_PARAMETER.exception("点赞类型不能为空");
        }
        if (!STAT_TYPE_TWICE.equals(upvoteType) && !STAT_TYPE_HELPFUL.equals(upvoteType)) {
            throw ErrorCode.INVALID_PARAMETER.exception("无效的点赞类型: " + upvoteType);
        }
    }
    
    /**
     * 生成用户统计Redis键名
     * 
     * @param dateStr 日期字符串
     * @return 用户统计键名
     */
    private String generateUserStatsKey(String dateStr) {
        return STATS_KEY_PREFIX + dateStr + USER_STATS_SUFFIX;
    }
    
    /**
     * 生成帖子统计Redis键名
     * 
     * @param dateStr 日期字符串
     * @return 帖子统计键名
     */
    private String generatePostStatsKey(String dateStr) {
        return STATS_KEY_PREFIX + dateStr + POST_STATS_SUFFIX;
    }
    
    /**
     * 生成统计字段名
     * 
     * @param id 对象ID（文章ID或用户ID）
     * @param statType 统计类型
     * @return 统计字段名
     */
    private String generateStatField(long id, String statType) {
        return id + ":" + statType;
    }
    
    /**
     * 安全地执行Redis操作并记录统计
     * 
     * @param articleId 文章ID
     * @param userId 用户ID（可选）
     * @param statType 统计类型
     * @param increment 增量（正数为增加，负数为减少）
     * @param operation 操作描述（用于日志）
     */
    private void performStatsOperation(long articleId, Long userId, String statType, int increment, String operation) {
        String today = LocalDate.now().toString();
        
        try {
            // 文章维度统计
            String postKey = generatePostStatsKey(today);
            String postField = generateStatField(articleId, statType);
            redisTemplate.opsForHash().increment(postKey, postField, increment);
            
            // 设置过期时间（只在增加时设置，避免重置已有数据的过期时间）
            if (increment > 0) {
                redisTemplate.expire(postKey, Duration.ofDays(DEFAULT_EXPIRE_DAYS));
            }
            
            // 用户维度统计（如果提供了用户ID）
            if (userId != null && userId > 0) {
                String userKey = generateUserStatsKey(today);
                String userField = generateStatField(userId, statType);
                redisTemplate.opsForHash().increment(userKey, userField, increment);
                
                if (increment > 0) {
                    redisTemplate.expire(userKey, Duration.ofDays(DEFAULT_EXPIRE_DAYS));
                }
            }
            
            log.debug("{}: articleId={}, userId={}, type={}", operation, articleId, userId, statType);
        } catch (Exception e) {
            log.error("{}失败: articleId={}, userId={}, type={}", operation, articleId, userId, statType, e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 记录文章访问统计
     * 
     * 当用户访问文章时调用此方法，实时增加访问统计计数。
     * 同时更新文章维度和用户维度的统计数据。
     * 
     * Redis操作说明:
     * 1. 文章维度: stats:2024-08-24:post -> {123:view: 1, 124:view: 5, ...}
     * 2. 用户维度: stats:2024-08-24:user -> {456:view: 3, 789:view: 7, ...}
     * 3. 设置3天过期时间，确保异常情况下Redis数据会自动清理
     * 
     * @param articleId 文章ID
     * @param userId 用户ID，可以为null（匿名访问）
     */
    /**
     * 记录文章访问统计
     * 
     * @param articleId 文章ID
     * @param userId 用户ID，可以为0（匿名访问）
     * @throws BusinessException 当参数无效时抛出异常
     */
    public void recordArticleView(long articleId, long userId) {
        validateArticleId(articleId);
        // 用户ID为0表示匿名访问，不需要验证
        if (userId != 0) {
            validateUserId(userId);
        }
        
        Long userIdForStats = userId > 0 ? userId : null;
        performStatsOperation(articleId, userIdForStats, STAT_TYPE_VIEW, 1, "记录文章访问");
    }

    /**
     * 记录点赞统计
     * 
     * 当用户对文章进行点赞操作时调用此方法。
     * 支持多种点赞类型：twice（两次能懂）、helpful（有用点赞）。
     * 
     * 使用场景:
     * - 用户点击"两次能懂"按钮 -> upvoteType="twice"
     * - 用户点击"有用"按钮 -> upvoteType="helpful"
     * 
     * 统计维度:
     * 1. 文章维度：统计每篇文章获得了多少个不同类型的点赞
     * 2. 用户维度：统计每个用户点赞了多少次
     * 
     * @param articleId 文章ID
     * @param userId 用户ID
     * @param upvoteType 点赞类型（twice, helpful）
     */
    /**
     * 记录点赞统计
     * 
     * @param articleId 文章ID
     * @param userId 用户ID
     * @param upvoteType 点赞类型（twice, helpful）
     * @throws BusinessException 当参数无效时抛出异常
     */
    public void recordUpvote(long articleId, long userId, String upvoteType) {
        validateArticleId(articleId);
        validateUserId(userId);
        validateUpvoteType(upvoteType);
        
        performStatsOperation(articleId, userId, upvoteType, 1, "记录点赞");
    }

    /**
     * 撤销点赞统计
     * 
     * 当用户取消之前的点赞操作时调用此方法。
     * 通过对计数器减1来实现撤销效果。
     * 
     * 业务场景:
     * - 用户重复点击同一个点赞按钮取消点赞
     * - 用户切换点赞类型（先撤销旧类型，再添加新类型）
     * 
     * 注意事项:
     * - 只减少当天的统计数据，不设置过期时间（避免重置已有数据的过期时间）
     * - 不检查计数器是否为负数，由业务层保证调用的正确性
     * 
     * @param articleId 文章ID
     * @param userId 用户ID
     * @param upvoteType 点赞类型（twice, helpful）
     */
    /**
     * 撤销点赞统计
     * 
     * @param articleId 文章ID
     * @param userId 用户ID
     * @param upvoteType 点赞类型（twice, helpful）
     * @throws BusinessException 当参数无效时抛出异常
     */
    public void removeUpvote(long articleId, long userId, String upvoteType) {
        validateArticleId(articleId);
        validateUserId(userId);
        validateUpvoteType(upvoteType);
        
        performStatsOperation(articleId, userId, upvoteType, -1, "撤销点赞");
    }

    /**
     * 记录评论统计
     * 
     * 当用户发表评论时调用此方法，增加评论统计计数。
     * 评论是重要的互动指标，同时统计文章和用户维度的数据。
     * 
     * 统计意义:
     * - 文章维度：反映文章的讨论热度和参与度
     * - 用户维度：反映用户的活跃程度和参与度
     * 
     * 业务价值:
     * - 热门文章识别：评论数多的文章可能更有价值
     * - 用户活跃度分析：评论多的用户更活跃
     * - 内容质量评估：评论数是内容质量的重要指标之一
     * 
     * @param articleId 文章ID
     * @param userId 用户ID
     */
    /**
     * 记录评论统计
     * 
     * @param articleId 文章ID
     * @param userId 用户ID
     * @throws BusinessException 当参数无效时抛出异常
     */
    public void recordComment(long articleId, long userId) {
        validateArticleId(articleId);
        validateUserId(userId);
        
        performStatsOperation(articleId, userId, STAT_TYPE_COMMENT, 1, "记录评论");
    }

    /**
     * 删除评论统计
     * 
     * 当用户删除之前发表的评论时调用此方法，减少评论统计计数。
     * 
     * 使用场景:
     * - 用户主动删除自己的评论
     * - 管理员删除不当评论
     * - 系统自动清理垃圾评论
     * 
     * 实现原理:
     * - 通过increment(-1)实现计数器减1
     * - 只操作当天的Redis数据
     * - 不重新设置过期时间，保持原有的数据生命周期
     * 
     * 注意事项:
     * - 确保在删除评论后及时调用此方法，保持数据一致性
     * - 不校验计数器是否为负，由业务层保证调用正确性
     * 
     * @param articleId 文章ID
     * @param userId 用户ID
     */
    /**
     * 删除评论统计
     * 
     * @param articleId 文章ID
     * @param userId 用户ID
     * @throws BusinessException 当参数无效时抛出异常
     */
    public void removeComment(Long articleId, Long userId) {
        // 参数类型转换和验证
        if (articleId == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("文章ID不能为空");
        }
        if (userId == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("用户ID不能为空");
        }
        
        validateArticleId(articleId);
        validateUserId(userId);
        
        performStatsOperation(articleId, userId.longValue(), STAT_TYPE_COMMENT, -1, "删除评论");
    }
}