package com.prosper.learn.application.listener;

import com.prosper.learn.application.service.MessageService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.shared.domain.event.content.lifecycle.CommentCreatedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentApprovedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentRejectedEvent;
import com.prosper.learn.shared.domain.event.content.voting.LikeUpvotedEvent;
import com.prosper.learn.shared.domain.event.content.voting.TwiceUpvotedEvent;
import com.prosper.learn.shared.domain.event.content.voting.UpvoteTypeSwitchedEvent;
import com.prosper.learn.shared.domain.event.user.relationship.UserFollowedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 消息事件监听器
 *
 * 专门负责处理用户互动产生的消息通知：
 * - 点赞通知（帖子、评论、路线图、记忆卡片组）
 * - 评论通知
 * - 关注通知
 *
 * 注意：
 * - 内容创建/分享/学习完成等通知由页面提示处理，不发送消息
 * - 只在用户与内容或其他用户互动时通知对方
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
    //@Async
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
    //@Async
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
    //@Async
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
    //@Async
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

    // ==================== 用户关系通知 ====================

    /**
     * 用户关注通知
     */
    @EventListener
    //@Async
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

    // ==================== 内容审核通知 ====================

    /**
     * 内容审核通过通知
     * 只有 profession 和 course 需要发送审核通过消息
     * 其他内容类型（post, roadmap, memory_card_deck）不发送，用户可以看到内容已发布
     */
    @EventListener
    //@Async
    public void onContentApproved(ContentApprovedEvent event) {
        try {
            switch (event.getContentType()) {
                case profession -> {
                    messageService.sendProfessionModeration(
                        event.getCreatorId(),
                        event.getContentId(),
                        event.getProfessionName(),
                        ModerationAction.APPROVED,
                        null
                    );
                    log.debug("发送职业审核通过通知: professionId={}, creatorId={}",
                        event.getContentId(), event.getCreatorId());
                }
                case course -> {
                    messageService.sendCourseModeration(
                        event.getCreatorId(),
                        event.getContentId(),
                        event.getCourseName(),
                        ModerationAction.APPROVED,
                        null
                    );
                    log.debug("发送课程审核通过通知: courseId={}, creatorId={}",
                        event.getContentId(), event.getCreatorId());
                }
                // post, roadmap, memory_card_deck 等不发送审核通过消息
                default -> log.debug("内容审核通过，不发送消息: contentType={}, contentId={}",
                        event.getContentType(), event.getContentId());
            }
        } catch (Exception e) {
            log.error("发送审核通过通知失败", e);
        }
    }

    /**
     * 内容审核拒绝通知
     * 处理所有内容类型的审核拒绝事件
     */
    @EventListener
    //@Async
    public void onContentRejected(ContentRejectedEvent event) {
        try {
            switch (event.getContentType()) {
                case profession -> {
                    messageService.sendProfessionModeration(
                        event.getCreatorId(),
                        event.getContentId(),
                        event.getProfessionName(),
                        ModerationAction.REJECTED,
                        event.getReason()
                    );
                    log.debug("发送职业审核拒绝通知: professionId={}, creatorId={}, reason={}",
                        event.getContentId(), event.getCreatorId(), event.getReason());
                }
                case course -> {
                    messageService.sendCourseModeration(
                        event.getCreatorId(),
                        event.getContentId(),
                        event.getCourseName(),
                        ModerationAction.REJECTED,
                        event.getReason()
                    );
                    log.debug("发送课程审核拒绝通知: courseId={}, creatorId={}, reason={}",
                        event.getContentId(), event.getCreatorId(), event.getReason());
                }
                case post -> {
                    messageService.sendPostModeration(
                        event.getCreatorId(),
                        event.getContentId(),
                        event.getPostContentPreview(),
                        event.getNodeId(),
                        event.getNodeName(),
                        event.getCourseName(),
                        ModerationAction.REJECTED,
                        event.getReason()
                    );
                    log.debug("发送帖子审核拒绝通知: postId={}, creatorId={}, reason={}",
                        event.getContentId(), event.getCreatorId(), event.getReason());
                }
                case roadmap -> {
                    messageService.sendRoadmapModeration(
                        event.getCreatorId(),
                        event.getContentId(),
                        event.getProfessionId(),
                        event.getProfessionName(),
                        ModerationAction.REJECTED,
                        event.getReason()
                    );
                    log.debug("发送路线图审核拒绝通知: roadmapId={}, creatorId={}, reason={}",
                        event.getContentId(), event.getCreatorId(), event.getReason());
                }
                case memory_card_deck -> {
                    messageService.sendMemoryDeckModeration(
                        event.getCreatorId(),
                        event.getContentId(),
                        event.getDeckTitle(),
                        event.getPostId(),
                        event.getPostContentPreview(),
                        ModerationAction.REJECTED,
                        event.getReason()
                    );
                    log.debug("发送记忆卡片组审核拒绝通知: deckId={}, creatorId={}, reason={}",
                        event.getContentId(), event.getCreatorId(), event.getReason());
                }
                default -> log.warn("不支持的审核拒绝通知类型: contentType={}", event.getContentType());
            }
        } catch (Exception e) {
            log.error("发送审核拒绝通知失败", e);
        }
    }
}