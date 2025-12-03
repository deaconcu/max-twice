package com.prosper.learn.business.service.listener;

import com.prosper.learn.business.event.content.voting.*;
import com.prosper.learn.business.service.domain.RedisStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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

    private final RedisStatsService redisStatsService;

    // ==================== 点赞事件 ====================

    /**
     * 两次能懂点赞 - 记录到Redis
     */
    @EventListener
    @Async
    public void onTwiceUpvoted(TwiceUpvotedEvent<?> event) {
        try {
            redisStatsService.recordUpvote(event.getContentId(), event.getVoterId(), "twice");
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
            redisStatsService.recordUpvote(event.getContentId(), event.getVoterId(), "like");
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
            redisStatsService.removeUpvote(event.getContentId(), event.getVoterId(), "twice");
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
            redisStatsService.removeUpvote(event.getContentId(), event.getVoterId(), "like");
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
            String oldTypeName = getUpvoteTypeName(event.getFromType());
            redisStatsService.removeUpvote(event.getContentId(), event.getVoterId(), oldTypeName);

            // 添加新类型记录
            String newTypeName = getUpvoteTypeName(event.getToType());
            redisStatsService.recordUpvote(event.getContentId(), event.getVoterId(), newTypeName);

            log.debug("Redis切换点赞类型: contentId={}, userId={}, from={}, to={}",
                event.getContentId(), event.getVoterId(), oldTypeName, newTypeName);
        } catch (Exception e) {
            log.error("Redis切换点赞类型失败: contentId={}, userId={}",
                event.getContentId(), event.getVoterId(), e);
        }
    }

    /**
     * 根据点赞类型获取类型名称
     */
    private String getUpvoteTypeName(Integer type) {
        if (type == com.prosper.learn.common.Enums.VoteType.twice.value()) {
            return "twice";
        } else {
            return "like";
        }
    }
}