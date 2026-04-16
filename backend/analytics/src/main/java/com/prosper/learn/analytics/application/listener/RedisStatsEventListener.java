package com.prosper.learn.analytics.application.listener;

import com.prosper.learn.analytics.stats.service.RedisStatsDomainService;
import com.prosper.learn.shared.domain.event.content.interaction.ContentViewedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.CommentCreatedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.CommentDeletedEvent;
import com.prosper.learn.shared.domain.event.content.voting.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Redis 统计事件监听器
 *
 * 负责监听各类用户行为事件，调用 RedisStatsDomainService 将统计数据写入 Redis。
 *
 * 职责：
 * - 纯粹的事件分发器，不包含业务逻辑
 * - 监听事件并调用对应的 Service 方法
 * - 所有业务逻辑都在 RedisStatsDomainService 中实现
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStatsEventListener {

    private final RedisStatsDomainService redisStatsDomainService;

    // ==================== 浏览事件 ====================

    /**
     * 内容浏览事件
     * 同时更新内容统计和用户统计（创建者获得浏览量）
     */
    @EventListener
    //@Async
    public void onContentViewed(ContentViewedEvent event) {
        redisStatsDomainService.incrementView(
            event.getContentType(),
            event.getContentId(),
            event.getCreatorId(),
            1
        );
    }

    // ==================== 点赞事件 ====================

    /**
     * 两次能懂点赞事件
     * 同时更新：1) 内容统计 2) 用户统计
     */
    @EventListener
    //@Async
    public void onTwiceUpvoted(TwiceUpvotedEvent<?> event) {
        redisStatsDomainService.incrementTwice(
            event.getContentType(),
            event.getContentId(),
            event.getVoterId(),
            event.getCreatorId(),
            1
        );
    }

    /**
     * 有用点赞事件
     * 同时更新：1) 内容统计 2) 用户统计
     */
    @EventListener
    //@Async
    public void onLikeUpvoted(LikeUpvotedEvent<?> event) {
        redisStatsDomainService.incrementLike(
            event.getContentType(),
            event.getContentId(),
            event.getVoterId(),
            event.getCreatorId(),
            1
        );
    }

    /**
     * 取消两次能懂点赞事件
     * 同时更新：1) 内容统计 2) 用户统计
     */
    @EventListener
    //@Async
    public void onTwiceUpvoteCancelled(TwiceUpvoteCancelledEvent<?> event) {
        redisStatsDomainService.incrementTwice(
            event.getContentType(),
            event.getContentId(),
            event.getVoterId(),
            event.getCreatorId(),
            -1
        );
    }

    /**
     * 取消有用点赞事件
     * 同时更新：1) 内容统计 2) 用户统计
     */
    @EventListener
    //@Async
    public void onLikeUpvoteCancelled(LikeUpvoteCancelledEvent<?> event) {
        redisStatsDomainService.incrementLike(
            event.getContentType(),
            event.getContentId(),
            event.getVoterId(),
            event.getCreatorId(),
            -1
        );
    }

    /**
     * 点赞类型切换事件
     * 同时更新：1) 内容统计 2) 用户统计
     */
    @EventListener
    //@Async
    public void onUpvoteTypeSwitched(UpvoteTypeSwitchedEvent<?> event) {
        // 减少旧类型
        if (event.getFromType() == 1) { // twice
            redisStatsDomainService.incrementTwice(
                event.getContentType(),
                event.getContentId(),
                event.getVoterId(),
                event.getCreatorId(),
                -1
            );
        } else { // like
            redisStatsDomainService.incrementLike(
                event.getContentType(),
                event.getContentId(),
                event.getVoterId(),
                event.getCreatorId(),
                -1
            );
        }

        // 增加新类型
        if (event.getToType() == 1) { // twice
            redisStatsDomainService.incrementTwice(
                event.getContentType(),
                event.getContentId(),
                event.getVoterId(),
                event.getCreatorId(),
                1
            );
        } else { // like
            redisStatsDomainService.incrementLike(
                event.getContentType(),
                event.getContentId(),
                event.getVoterId(),
                event.getCreatorId(),
                1
            );
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
        redisStatsDomainService.incrementComment(
            event.getContentType(),
            event.getContentId(),
            event.getContentCreatorId(),
            1
        );
    }

    /**
     * 评论删除事件
     * 同时更新内容统计和用户统计（创建者失去评论）
     */
    @EventListener
    //@Async
    public void onCommentDeleted(CommentDeletedEvent event) {
        redisStatsDomainService.incrementComment(
            event.getContentType(),
            event.getContentId(),
            event.getContentCreatorId(),
            -1
        );
    }
}
