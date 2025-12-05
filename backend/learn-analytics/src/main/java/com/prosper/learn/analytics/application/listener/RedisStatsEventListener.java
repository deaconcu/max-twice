package com.prosper.learn.analytics.application.listener;

import com.prosper.learn.analytics.stats.service.RedisStatsDomainService;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.event.content.voting.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * Redis 统计事件监听器
 *
 * 专门负责处理点赞相关的 Redis 统计更新：
 * - 记录用户点赞行为到 Redis
 * - 移除用户取消点赞记录
 *
 * Redis 统计主要用于：
 * - 快速查询用户点赞状态
 * - 实时统计数据展示
 * - 防止重复点赞检查
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStatsEventListener {

    private final RedisStatsDomainService redisStatsService;

    // ==================== 点赞事件 ====================

    /**
     * 两次能懂点赞 - 记录到Redis
     */
    @EventListener
    @Async
    public void onTwiceUpvoted(TwiceUpvotedEvent<?> event) {
        try {
            redisStatsService.recordUpvote(event.getContentId(), event.getVoterId(), VoteType.twice);
            log.debug("Redis记录两次能懂点赞: contentId={}, userId={}", event.getContentId(), event.getVoterId());
        } catch (Exception e) {
            log.error("Redis记录两次能懂点赞失败: contentId={}, userId={}",
                event.getContentId(), event.getVoterId(), e);
        }
    }

    /**
     * 点赞 - 记录到Redis
     */
    @EventListener
    @Async
    public void onLikeUpvoted(LikeUpvotedEvent<?> event) {
        try {
            redisStatsService.recordUpvote(event.getContentId(), event.getVoterId(), VoteType.like);
            log.debug("Redis记录点赞: contentId={}, userId={}", event.getContentId(), event.getVoterId());
        } catch (Exception e) {
            log.error("Redis记录点赞失败: contentId={}, userId={}",
                event.getContentId(), event.getVoterId(), e);
        }
    }

    // ==================== 取消点赞事件 ====================

    /**
     * 取消两次能懂点赞 - 从Redis移除
     */
    @EventListener
    @Async
    public void onTwiceUpvoteCancelled(TwiceUpvoteCancelledEvent<?> event) {
        try {
            redisStatsService.removeUpvote(event.getContentId(), event.getVoterId(), VoteType.twice);
            log.debug("Redis移除两次能懂点赞: contentId={}, userId={}", event.getContentId(), event.getVoterId());
        } catch (Exception e) {
            log.error("Redis移除两次能懂点赞失败: contentId={}, userId={}",
                event.getContentId(), event.getVoterId(), e);
        }
    }

    /**
     * 取消点赞 - 从Redis移除
     */
    @EventListener
    @Async
    public void onLikeUpvoteCancelled(LikeUpvoteCancelledEvent<?> event) {
        try {
            redisStatsService.removeUpvote(event.getContentId(), event.getVoterId(), VoteType.like);
            log.debug("Redis移除点赞: contentId={}, userId={}", event.getContentId(), event.getVoterId());
        } catch (Exception e) {
            log.error("Redis移除点赞失败: contentId={}, userId={}",
                event.getContentId(), event.getVoterId(), e);
        }
    }

    /**
     * 点赞类型切换事件 - 一次性更新Redis
     */
    @EventListener
    @Async
    public void onUpvoteTypeSwitched(UpvoteTypeSwitchedEvent<?> event) {
        try {
            // 移除旧类型记录
            VoteType oldType = getVoteTypeFromInt(event.getFromType());
            redisStatsService.removeUpvote(event.getContentId(), event.getVoterId(), oldType);

            // 添加新类型记录
            VoteType newType = getVoteTypeFromInt(event.getToType());
            redisStatsService.recordUpvote(event.getContentId(), event.getVoterId(), newType);

            log.debug("Redis切换点赞类型: contentId={}, userId={}, from={}, to={}",
                event.getContentId(), event.getVoterId(), oldType, newType);
        } catch (Exception e) {
            log.error("Redis切换点赞类型失败: contentId={}, userId={}",
                event.getContentId(), event.getVoterId(), e);
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