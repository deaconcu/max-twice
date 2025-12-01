package com.prosper.learn.domain.service.listener;

import com.prosper.learn.domain.event.content.interaction.*;
import com.prosper.learn.domain.event.content.voting.*;
import com.prosper.learn.domain.event.content.lifecycle.*;
import com.prosper.learn.domain.event.user.relationship.*;
import com.prosper.learn.domain.event.user.learning.*;
import com.prosper.learn.domain.service.basic.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 消息事件监听器
 *
 * 专门负责处理各种业务事件产生的消息通知：
 * - 内容互动通知（点赞、评论、分享等）
 * - 用户关系通知（关注、取关等）
 * - 内容创建通知
 * - 学习进度通知
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageEventListener {

    private final MessageService messageService;

    // ==================== 内容互动通知 ====================

    /**
     * 内容被浏览事件 - 暂不发送通知
     * 浏览量通知通常不需要，避免消息过多
     */
    // @EventListener - 暂不启用浏览通知

    /**
     * 两次能懂点赞通知
     */
    @EventListener
    @Async
    public void onTwiceUpvoted(TwiceUpvotedEvent event) {
        try {
            // 发送点赞通知给内容创建者
            messageService.createUpvoteMessage(
                event.getCreatorId(),          // 接收者（内容创建者）
                event.getVoterId(),            // 发送者（点赞者）
                event.getContentId(),
                event.getContentType().value(),
                2  // 两次能懂类型
            );
            log.debug("发送两次能懂点赞通知: contentId={}, voterId={}, creatorId={}",
                event.getContentId(), event.getVoterId(), event.getCreatorId());
        } catch (Exception e) {
            log.error("发送两次能懂点赞通知失败", e);
        }
    }

    /**
     * 点赞通知
     */
    @EventListener
    @Async
    public void onLikeUpvoted(LikeUpvotedEvent event) {
        try {
            // 发送点赞通知给内容创建者
            messageService.createUpvoteMessage(
                event.getCreatorId(),          // 接收者
                event.getVoterId(),            // 发送者
                event.getContentId(),
                event.getContentType().value(),
                3  // 有帮助点赞类型
            );
            log.debug("发送点赞通知: contentId={}, voterId={}, creatorId={}",
                event.getContentId(), event.getVoterId(), event.getCreatorId());
        } catch (Exception e) {
            log.error("发送点赞通知失败", e);
        }
    }

    /**
     * 评论创建通知
     */
    @EventListener
    @Async
    public void onCommentCreated(CommentCreatedEvent event) {
        try {
            // 发送评论通知给内容创建者
            messageService.createCommentMessage(
                event.getContentCreatorId(),   // 接收者（内容创建者）
                event.getCommenterId(),        // 发送者（评论者）
                event.getContentId(),
                event.getCommentId(),
                event.getContentType().value()
            );
            log.debug("发送评论通知: contentId={}, commenterId={}, creatorId={}",
                event.getContentId(), event.getCommenterId(), event.getContentCreatorId());
        } catch (Exception e) {
            log.error("发送评论通知失败", e);
        }
    }

    /**
     * 内容分享通知
     */
    @EventListener
    @Async
    public void onContentShared(ContentSharedEvent event) {
        try {
            // 发送分享通知给内容创建者
            messageService.createShareMessage(
                event.getCreatorId(),          // 接收者
                event.getSharerId(),            // 发送者
                event.getContentId(),
                event.getContentType().name()
            );
            log.debug("发送分享通知: contentId={}, sharerId={}, creatorId={}",
                event.getContentId(), event.getSharerId(), event.getCreatorId());
        } catch (Exception e) {
            log.error("发送分享通知失败", e);
        }
    }

    // ==================== 用户关系通知 ====================

    /**
     * 用户关注通知
     */
    @EventListener
    @Async
    public void onUserFollowed(UserFollowedEvent event) {
        try {
            // 发送关注通知给被关注者
            messageService.createFollowMessage(
                event.getFolloweeId(),         // 接收者（被关注者）
                event.getFollowerId()          // 发送者（关注者）
            );
            log.debug("发送关注通知: followerId={}, followeeId={}",
                event.getFollowerId(), event.getFolloweeId());
        } catch (Exception e) {
            log.error("发送关注通知失败", e);
        }
    }

    // ==================== 学习进度通知 ====================

    /**
     * 学习完成通知
     */
    @EventListener
    @Async
    public void onLearningCompleted(LearningCompletedEvent event) {
        try {
            // 发送学习完成祝贺消息
            messageService.createLearningCompletionMessage(
                event.getUserId(),
                event.getContentId(),
                event.getContentType().name()
            );
            log.debug("发送学习完成通知: userId={}, contentId={}, contentType={}",
                event.getUserId(), event.getContentId(), event.getContentType());
        } catch (Exception e) {
            log.error("发送学习完成通知失败", e);
        }
    }

    // ==================== 内容创建通知 ====================

    /**
     * 内容创建通知
     * 通知关注者有新内容发布
     */
    @EventListener
    @Async
    public void onContentCreated(ContentCreatedEvent event) {
        try {
            // 根据内容类型发送不同的创建通知
            switch (event.getContentType()) {
                case post:
                    messageService.createArticlePublishMessage(
                        event.getCreatorId(),
                        event.getContentId()
                    );
                    break;
                case roadmap:
                    messageService.createRoadmapPublishMessage(
                        event.getCreatorId(),
                        event.getContentId()
                    );
                    break;
                case memory_card_deck:
                    messageService.createCardDeckPublishMessage(
                        event.getCreatorId(),
                        event.getContentId()
                    );
                    break;
                default:
                    log.debug("内容创建通知，暂不支持类型: {}", event.getContentType());
            }
            log.debug("发送内容创建通知: creatorId={}, contentId={}, contentType={}",
                event.getCreatorId(), event.getContentId(), event.getContentType());
        } catch (Exception e) {
            log.error("发送内容创建通知失败", e);
        }
    }
}