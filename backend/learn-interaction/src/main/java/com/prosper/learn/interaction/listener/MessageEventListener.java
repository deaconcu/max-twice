package com.prosper.learn.interaction.listener;

import com.prosper.learn.interaction.message.MessageDomainService;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.event.content.voting.LikeUpvotedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static com.prosper.learn.shared.domain.Enums.*;

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

    private final MessageDomainService messageService;

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
    public void onTwiceUpvoted(TwiceUpvotedEvent<PostDO> event) {
        try {
            // 两次能懂点赞只对帖子有效
            PostDO post = event.getContentObject();

            messageService.createPostUpvoteMessage(
                event.getCreatorId(),          // 接收者（内容创建者）
                event.getVoterId(),            // 发送者（点赞者）
                post.getNodeId(),              // 节点ID
                event.getContentId(),          // 帖子ID
                VoteType.twice                 // 点赞类型
            );
            log.debug("发送两次能懂点赞通知: contentId={}, voterId={}, creatorId={}",
                event.getContentId(), event.getVoterId(), event.getCreatorId());
        } catch (Exception e) {
            log.error("发送两次能懂点赞通知失败", e);
        }
    }

    /**
     * 点赞通知 - 支持帖子、评论、路线图、记忆卡片组
     */
    @EventListener
    @Async
    public void onLikeUpvoted(LikeUpvotedEvent<?> event) {
        try {
            // 使用事件中的contextId，避免额外数据库查询
            Long contextId = event.getContextId();
            if (contextId == null) {
                log.warn("LikeUpvotedEvent缺少contextId: contentType={}, contentId={}",
                    event.getContentType(), event.getContentId());
                return;
            }

            if (event.getContentType() == ContentType.post) {
                // 处理帖子点赞通知
                messageService.createPostUpvoteMessage(
                    event.getCreatorId(),          // 接收者
                    event.getVoterId(),            // 发送者
                    contextId,                     // 节点ID
                    event.getContentId(),          // 帖子ID
                    VoteType.like                  // 点赞类型
                );
                log.debug("发送帖子点赞通知: contentId={}, voterId={}, creatorId={}",
                    event.getContentId(), event.getVoterId(), event.getCreatorId());

            } else if (event.getContentType() == ContentType.comment) {
                // 处理评论点赞通知
                messageService.createCommentUpvoteMessage(
                    event.getCreatorId(),          // 接收者
                    event.getVoterId(),            // 发送者
                    contextId,                     // 节点ID
                    event.getContentId()          // 评论ID
                );
                log.debug("发送评论点赞通知: contentId={}, voterId={}, creatorId={}",
                    event.getContentId(), event.getVoterId(), event.getCreatorId());

            } else if (event.getContentType() == ContentType.roadmap) {
                // 处理路线图投票通知
                messageService.createRoadmapUpvoteMessage(
                    event.getCreatorId(),          // 接收者
                    event.getVoterId(),            // 发送者
                    contextId,                     // 职业ID
                    event.getContentId()          // 路线图ID
                );
                log.debug("发送路线图投票通知: contentId={}, voterId={}, creatorId={}",
                    event.getContentId(), event.getVoterId(), event.getCreatorId());

            } else if (event.getContentType() == ContentType.memory_card_deck) {
                // 处理记忆卡片组点赞通知
                messageService.createMemoryDeckUpvoteMessage(
                    event.getCreatorId(),          // 接收者
                    event.getVoterId(),            // 发送者
                    contextId,                     // 节点ID
                    event.getContentId()          // 卡片组ID
                );
                log.debug("发送记忆卡片组点赞通知: contentId={}, voterId={}, creatorId={}",
                    event.getContentId(), event.getVoterId(), event.getCreatorId());

            } else {
                log.warn("不支持的内容类型点赞通知: contentType={}", event.getContentType());
            }
        } catch (Exception e) {
            log.error("发送点赞通知失败", e);
        }
    }

    /**
     * 点赞类型切换通知 - 只发送新类型的通知
     */
    @EventListener
    @Async
    public void onUpvoteTypeSwitched(UpvoteTypeSwitchedEvent<PostDO> event) {
        try {
            // 点赞类型切换只对帖子有效
            PostDO post = event.getContentObject();

            // 根据新类型发送通知
            VoteType newVoteType = VoteType.getByValue(event.getToType());

            messageService.createPostUpvoteMessage(
                event.getCreatorId(),          // 接收者
                event.getVoterId(),            // 发送者
                post.getNodeId(),              // 节点ID
                event.getContentId(),          // 帖子ID
                newVoteType                    // 新的点赞类型
            );
            log.debug("发送点赞类型切换通知: contentId={}, voterId={}, creatorId={}, newType={}",
                event.getContentId(), event.getVoterId(), event.getCreatorId(), newVoteType);
        } catch (Exception e) {
            log.error("发送点赞类型切换通知失败", e);
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