package com.prosper.learn.domain.service.basic;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class RedisStatsService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

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
    public void recordArticleView(long articleId, long userId) {
        String today = LocalDate.now().toString();
        
        try {
            // 文章维度统计：记录哪些文章被访问了多少次
            String postKey = "stats:" + today + ":post";
            String postField = articleId + ":view";
            redisTemplate.opsForHash().increment(postKey, postField, 1);
            redisTemplate.expire(postKey, Duration.ofDays(3));
            
            // 用户维度统计：记录用户访问了多少篇文章（只有登录用户才统计）
            String userKey = "stats:" + today + ":user";
            String userField = userId + ":view";
            redisTemplate.opsForHash().increment(userKey, userField, 1);
            redisTemplate.expire(userKey, Duration.ofDays(3));

            log.debug("记录文章访问: articleId={}, userId={}", articleId, userId);
        } catch (Exception e) {
            log.error("记录文章访问失败: articleId={}, userId={}", articleId, userId, e);
        }
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
    public void recordUpvote(long articleId, long userId, String upvoteType) {
        String today = LocalDate.now().toString();
        
        try {
            // 文章维度统计：统计文章获得的点赞数量
            String postKey = "stats:" + today + ":post";
            String postField = articleId + ":" + upvoteType;
            redisTemplate.opsForHash().increment(postKey, postField, 1);
            redisTemplate.expire(postKey, Duration.ofDays(3));
            
            // 用户维度统计：统计用户的点赞行为
            String userKey = "stats:" + today + ":user";
            String userField = userId + ":" + upvoteType;
            redisTemplate.opsForHash().increment(userKey, userField, 1);
            redisTemplate.expire(userKey, Duration.ofDays(3));
            
            log.debug("记录点赞: articleId={}, userId={}, type={}", articleId, userId, upvoteType);
        } catch (Exception e) {
            log.error("记录点赞失败: articleId={}, userId={}, type={}", articleId, userId, upvoteType, e);
        }
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
    public void removeUpvote(long articleId, long userId, String upvoteType) {
        String today = LocalDate.now().toString();
        
        try {
            // 文章维度统计：减少文章的点赞计数
            String postKey = "stats:" + today + ":post";
            String postField = articleId + ":" + upvoteType;
            redisTemplate.opsForHash().increment(postKey, postField, -1);
            
            // 用户维度统计：减少用户的点赞计数
            String userKey = "stats:" + today + ":user";
            String userField = userId + ":" + upvoteType;
            redisTemplate.opsForHash().increment(userKey, userField, -1);
            
            log.debug("撤销点赞: articleId={}, userId={}, type={}", articleId, userId, upvoteType);
        } catch (Exception e) {
            log.error("撤销点赞失败: articleId={}, userId={}, type={}", articleId, userId, upvoteType, e);
        }
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
    public void recordComment(long articleId, long userId) {
        String today = LocalDate.now().toString();
        
        try {
            // 文章维度统计：增加文章的评论计数
            String postKey = "stats:" + today + ":post";
            String postField = articleId + ":comment";
            redisTemplate.opsForHash().increment(postKey, postField, 1);
            redisTemplate.expire(postKey, Duration.ofDays(3));
            
            // 用户维度统计：增加用户的评论计数
            String userKey = "stats:" + today + ":user";
            String userField = userId + ":comment";
            redisTemplate.opsForHash().increment(userKey, userField, 1);
            redisTemplate.expire(userKey, Duration.ofDays(3));
            
            log.debug("记录评论: articleId={}, userId={}", articleId, userId);
        } catch (Exception e) {
            log.error("记录评论失败: articleId={}, userId={}", articleId, userId, e);
        }
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
    public void removeComment(Long articleId, Integer userId) {
        String today = LocalDate.now().toString();
        
        try {
            // 文章维度统计：减少文章的评论计数
            String postKey = "stats:" + today + ":post";
            String postField = articleId + ":comment";
            redisTemplate.opsForHash().increment(postKey, postField, -1);
            
            // 用户维度统计：减少用户的评论计数
            String userKey = "stats:" + today + ":user";
            String userField = userId + ":comment";
            redisTemplate.opsForHash().increment(userKey, userField, -1);
            
            log.debug("删除评论: articleId={}, userId={}", articleId, userId);
        } catch (Exception e) {
            log.error("删除评论失败: articleId={}, userId={}", articleId, userId, e);
        }
    }
}