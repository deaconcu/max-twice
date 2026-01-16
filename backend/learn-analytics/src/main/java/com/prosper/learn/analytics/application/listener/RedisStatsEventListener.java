package com.prosper.learn.analytics.application.listener;

import com.prosper.learn.analytics.stats.service.RedisStatsDomainService;
import com.prosper.learn.shared.common.constants.RedisStatsConstants;
import com.prosper.learn.shared.common.util.TimeZoneUtil;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.event.content.interaction.ContentViewedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.CommentCreatedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.CommentDeletedEvent;
import com.prosper.learn.shared.domain.event.content.voting.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * Redis 统计事件监听器
 *
 * 负责将按日统计数据写入 Redis，包括：
 *
 * 【内容维度统计】:
 * - 浏览量（views）
 * - 两次能懂点赞数（twice）
 * - 有用点赞数（like）
 * - 评论数（comments）
 *
 * 【用户维度统计】（内容创建者获得）:
 * - 浏览量（views）
 * - 两次能懂点赞数（twice）
 * - 有用点赞数（like）
 * - 评论数（comments）
 *
 * 【点赞关系记录】:
 * - 记录用户点赞行为（用于查询点赞状态）
 *
 * Redis 数据结构:
 * - stats:YYYY-MM-DD:content -> {contentType:contentId:statType: count}
 * - stats:YYYY-MM-DD:user -> {userId:statType: count}
 *
 * 定时同步:
 * - 每天凌晨同步到数据库的 yearly 表和总计表
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStatsEventListener {

    private final RedisStatsDomainService redisStatsService;
    private final RedisTemplate<String, String> redisTemplate;

    // ==================== 浏览事件 ====================

    /**
     * 内容浏览事件
     * 同时更新内容统计和用户统计（创建者获得浏览量）
     */
    @EventListener
    //@Async
    public void onContentViewed(ContentViewedEvent event) {
        try {
            // 内容维度统计
            incrementContentStat(event.getContentType(), event.getContentId(),
                RedisStatsConstants.STAT_TYPE_VIEW, 1);

            // 用户维度统计（创建者获得浏览量）
            if (event.getCreatorId() != null && event.getCreatorId() > 0) {
                incrementUserStat(event.getCreatorId(), RedisStatsConstants.STAT_TYPE_VIEW, 1);
            }

            log.debug("Redis记录浏览: contentType={}, contentId={}, creatorId={}",
                event.getContentType(), event.getContentId(), event.getCreatorId());
        } catch (Exception e) {
            log.error("Redis记录浏览失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    // ==================== 点赞事件 ====================

    /**
     * 两次能懂点赞事件
     * 同时更新：1) 内容统计 2) 用户统计 3) 点赞关系
     */
    @EventListener
    //@Async
    public void onTwiceUpvoted(TwiceUpvotedEvent<?> event) {
        try {
            // 1. 内容维度统计
            incrementContentStat(event.getContentType(), event.getContentId(),
                RedisStatsConstants.STAT_TYPE_TWICE, 1);

            // 2. 用户维度统计（创建者获得点赞）
            if (event.getCreatorId() != null && event.getCreatorId() > 0) {
                incrementUserStat(event.getCreatorId(), RedisStatsConstants.STAT_TYPE_TWICE, 1);
            }

            // 3. 点赞关系记录 - 已废弃，统计已由incrementContentStat处理
            // redisStatsService.recordUpvote(event.getContentId(), event.getVoterId(), VoteType.twice);

            log.debug("Redis记录两次能懂点赞: contentType={}, contentId={}, voterId={}, creatorId={}",
                event.getContentType(), event.getContentId(), event.getVoterId(), event.getCreatorId());
        } catch (Exception e) {
            log.error("Redis记录两次能懂点赞失败: contentId={}, voterId={}",
                event.getContentId(), event.getVoterId(), e);
        }
    }

    /**
     * 有用点赞事件
     * 同时更新：1) 内容统计 2) 用户统计 3) 点赞关系
     */
    @EventListener
    //@Async
    public void onLikeUpvoted(LikeUpvotedEvent<?> event) {
        try {
            // 1. 内容维度统计
            incrementContentStat(event.getContentType(), event.getContentId(),
                RedisStatsConstants.STAT_TYPE_LIKE, 1);

            // 2. 用户维度统计（创建者获得点赞）
            if (event.getCreatorId() != null && event.getCreatorId() > 0) {
                incrementUserStat(event.getCreatorId(), RedisStatsConstants.STAT_TYPE_LIKE, 1);
            }

            // 3. 点赞关系记录 - 已废弃，统计已由incrementContentStat处理
            // redisStatsService.recordUpvote(event.getContentId(), event.getVoterId(), VoteType.like);

            log.debug("Redis记录有用点赞: contentType={}, contentId={}, voterId={}, creatorId={}",
                event.getContentType(), event.getContentId(), event.getVoterId(), event.getCreatorId());
        } catch (Exception e) {
            log.error("Redis记录有用点赞失败: contentId={}, voterId={}",
                event.getContentId(), event.getVoterId(), e);
        }
    }

    /**
     * 取消两次能懂点赞事件
     * 同时更新：1) 内容统计 2) 用户统计 3) 点赞关系
     */
    @EventListener
    //@Async
    public void onTwiceUpvoteCancelled(TwiceUpvoteCancelledEvent<?> event) {
        try {
            // 1. 内容维度统计
            incrementContentStat(event.getContentType(), event.getContentId(),
                RedisStatsConstants.STAT_TYPE_TWICE, -1);

            // 2. 用户维度统计（创建者失去点赞）
            if (event.getCreatorId() != null && event.getCreatorId() > 0) {
                incrementUserStat(event.getCreatorId(), RedisStatsConstants.STAT_TYPE_TWICE, -1);
            }

            // 3. 点赞关系记录 - 已废弃，统计已由incrementContentStat处理
            // redisStatsService.removeUpvote(event.getContentId(), event.getVoterId(), VoteType.twice);

            log.debug("Redis移除两次能懂点赞: contentType={}, contentId={}, voterId={}, creatorId={}",
                event.getContentType(), event.getContentId(), event.getVoterId(), event.getCreatorId());
        } catch (Exception e) {
            log.error("Redis移除两次能懂点赞失败: contentId={}, voterId={}",
                event.getContentId(), event.getVoterId(), e);
        }
    }

    /**
     * 取消有用点赞事件
     * 同时更新：1) 内容统计 2) 用户统计 3) 点赞关系
     */
    @EventListener
    //@Async
    public void onLikeUpvoteCancelled(LikeUpvoteCancelledEvent<?> event) {
        try {
            // 1. 内容维度统计
            incrementContentStat(event.getContentType(), event.getContentId(),
                RedisStatsConstants.STAT_TYPE_LIKE, -1);

            // 2. 用户维度统计（创建者失去点赞）
            if (event.getCreatorId() != null && event.getCreatorId() > 0) {
                incrementUserStat(event.getCreatorId(), RedisStatsConstants.STAT_TYPE_LIKE, -1);
            }

            // 3. 点赞关系记录 - 已废弃，统计已由incrementContentStat处理
            // redisStatsService.removeUpvote(event.getContentId(), event.getVoterId(), VoteType.like);

            log.debug("Redis移除有用点赞: contentType={}, contentId={}, voterId={}, creatorId={}",
                event.getContentType(), event.getContentId(), event.getVoterId(), event.getCreatorId());
        } catch (Exception e) {
            log.error("Redis移除有用点赞失败: contentId={}, voterId={}",
                event.getContentId(), event.getVoterId(), e);
        }
    }

    /**
     * 点赞类型切换事件
     * 同时更新：1) 内容统计 2) 用户统计 3) 点赞关系
     */
    @EventListener
    //@Async
    public void onUpvoteTypeSwitched(UpvoteTypeSwitchedEvent<?> event) {
        try {
            // 1. 内容维度统计
            if (event.getFromType() == VoteType.twice.value()) {
                incrementContentStat(event.getContentType(), event.getContentId(),
                    RedisStatsConstants.STAT_TYPE_TWICE, -1);
            } else {
                incrementContentStat(event.getContentType(), event.getContentId(),
                    RedisStatsConstants.STAT_TYPE_LIKE, -1);
            }

            if (event.getToType() == VoteType.twice.value()) {
                incrementContentStat(event.getContentType(), event.getContentId(),
                    RedisStatsConstants.STAT_TYPE_TWICE, 1);
            } else {
                incrementContentStat(event.getContentType(), event.getContentId(),
                    RedisStatsConstants.STAT_TYPE_LIKE, 1);
            }

            // 2. 用户维度统计（创建者）
            if (event.getCreatorId() != null && event.getCreatorId() > 0) {
                if (event.getFromType() == VoteType.twice.value()) {
                    incrementUserStat(event.getCreatorId(), RedisStatsConstants.STAT_TYPE_TWICE, -1);
                } else {
                    incrementUserStat(event.getCreatorId(), RedisStatsConstants.STAT_TYPE_LIKE, -1);
                }

                if (event.getToType() == VoteType.twice.value()) {
                    incrementUserStat(event.getCreatorId(), RedisStatsConstants.STAT_TYPE_TWICE, 1);
                } else {
                    incrementUserStat(event.getCreatorId(), RedisStatsConstants.STAT_TYPE_LIKE, 1);
                }
            }

            // 3. 点赞关系记录 - 已废弃，统计已由incrementContentStat处理
            // VoteType oldType = getVoteTypeFromInt(event.getFromType());
            // VoteType newType = getVoteTypeFromInt(event.getToType());
            // redisStatsService.removeUpvote(event.getContentId(), event.getVoterId(), oldType);
            // redisStatsService.recordUpvote(event.getContentId(), event.getVoterId(), newType);

            log.debug("Redis切换点赞类型: contentType={}, contentId={}, voterId={}, from={}, to={}",
                event.getContentType(), event.getContentId(), event.getVoterId(),
                event.getFromType(), event.getToType());
        } catch (Exception e) {
            log.error("Redis切换点赞类型失败: contentId={}, voterId={}",
                event.getContentId(), event.getVoterId(), e);
        }
    }

    // ==================== 评论事件 ====================

    /**
     * 评论创建事件
     * 同时更新内容统计和用户统计（创建者获得评论）
     */
    @EventListener
    //@Async
    public void onCommentCreated(CommentCreatedEvent event) {
        try {
            // 内容维度统计
            incrementContentStat(event.getContentType(), event.getContentId(),
                RedisStatsConstants.STAT_TYPE_COMMENT, 1);

            // 用户维度统计（内容创建者获得评论）
            if (event.getContentCreatorId() != null && event.getContentCreatorId() > 0) {
                incrementUserStat(event.getContentCreatorId(), RedisStatsConstants.STAT_TYPE_COMMENT, 1);
            }

            log.debug("Redis记录评论: contentType={}, contentId={}, creatorId={}",
                event.getContentType(), event.getContentId(), event.getContentCreatorId());
        } catch (Exception e) {
            log.error("Redis记录评论失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    /**
     * 评论删除事件
     * 同时更新内容统计和用户统计（创建者失去评论）
     */
    @EventListener
    //@Async
    public void onCommentDeleted(CommentDeletedEvent event) {
        try {
            // 内容维度统计
            incrementContentStat(event.getContentType(), event.getContentId(),
                RedisStatsConstants.STAT_TYPE_COMMENT, -1);

            // 用户维度统计（内容创建者失去评论）
            if (event.getContentCreatorId() != null && event.getContentCreatorId() > 0) {
                incrementUserStat(event.getContentCreatorId(), RedisStatsConstants.STAT_TYPE_COMMENT, -1);
            }

            log.debug("Redis删除评论统计: contentType={}, contentId={}, creatorId={}",
                event.getContentType(), event.getContentId(), event.getContentCreatorId());
        } catch (Exception e) {
            log.error("Redis删除评论统计失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 增量更新内容统计到 Redis
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @param statType 统计类型（view/twice/like/comment）
     * @param delta 增量值
     */
    private void incrementContentStat(Enums.ContentType contentType, Long contentId, String statType, int delta) {
        String today = TimeZoneUtil.todayString();
        String contentKey = RedisStatsConstants.STATS_KEY_PREFIX + today + RedisStatsConstants.CONTENT_STATS_SUFFIX;
        String contentField = contentType.value() + ":" + contentId + ":" + statType;

        redisTemplate.opsForHash().increment(contentKey, contentField, delta);

        // 设置过期时间（只在增加时设置）
        if (delta > 0) {
            redisTemplate.expire(contentKey, Duration.ofDays(RedisStatsConstants.DEFAULT_EXPIRE_DAYS));
        }
    }

    /**
     * 增量更新用户统计到 Redis
     *
     * @param userId 用户ID（内容创建者）
     * @param statType 统计类型（view/twice/like/comment）
     * @param delta 增量值
     */
    private void incrementUserStat(Long userId, String statType, int delta) {
        String today = TimeZoneUtil.todayString();
        String userKey = RedisStatsConstants.STATS_KEY_PREFIX + today + RedisStatsConstants.USER_STATS_SUFFIX;
        String userField = userId + ":" + statType;

        redisTemplate.opsForHash().increment(userKey, userField, delta);

        // 设置过期时间（只在增加时设置）
        if (delta > 0) {
            redisTemplate.expire(userKey, Duration.ofDays(RedisStatsConstants.DEFAULT_EXPIRE_DAYS));
        }
    }

    /**
     * 根据整数值获取点赞类型枚举
     */
    private VoteType getVoteTypeFromInt(Integer type) {
        if (type == VoteType.twice.value()) {
            return VoteType.twice;
        } else {
            return VoteType.like;
        }
    }
}
