package com.prosper.learn.analytics.application.listener;

import com.prosper.learn.analytics.stats.dataservice.ContentStatsDataService;
import com.prosper.learn.shared.domain.event.content.interaction.ContentBookmarkedEvent;
import com.prosper.learn.shared.domain.event.content.interaction.ContentSharedEvent;
import com.prosper.learn.shared.domain.event.content.interaction.ContentUnbookmarkedEvent;
import com.prosper.learn.shared.domain.event.content.interaction.ContentViewedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.CommentCreatedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.CommentDeletedEvent;
import com.prosper.learn.shared.domain.event.content.voting.*;
import com.prosper.learn.shared.domain.event.user.learning.LearningCompletedEvent;
import com.prosper.learn.shared.domain.event.user.learning.LearningStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 内容统计事件监听器
 *
 * 监听各种内容相关事件，自动更新对应的统计数据：
 * - 浏览事件 -> 增加浏览量
 * - 点赞事件 -> 增加点赞数
 * - 评论事件 -> 增加评论数
 * - 分享事件 -> 增加分享数
 * - 收藏事件 -> 增加收藏数
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContentStatsEventListener {

    private final ContentStatsDataService contentStatsDataService;

    // ==================== 浏览事件 ====================

    /**
     * 内容浏览事件 - 增加浏览量
     */
    @EventListener
    @Async
    public void onContentViewed(ContentViewedEvent event) {
        try {
            contentStatsDataService.incrementViews(event.getContentType(), event.getContentId(), 1);
            log.debug("增加内容浏览量: contentType={}, contentId={}", event.getContentType(), event.getContentId());
        } catch (Exception e) {
            log.error("处理内容浏览事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    // ==================== 点赞事件 ====================

    /**
     * 两次能懂点赞事件
     */
    @EventListener
    @Async
    public void onTwiceUpvoted(TwiceUpvotedEvent<?> event) {
        try {
            contentStatsDataService.incrementTwice(event.getContentType(), event.getContentId(), 1);
            log.debug("增加两次能懂点赞: contentType={}, contentId={}", event.getContentType(), event.getContentId());
        } catch (Exception e) {
            log.error("处理两次能懂点赞事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    /**
     * 点赞事件（原helpful）- 统一处理所有内容类型的点赞统计
     */
    @EventListener
    @Async
    public void onLikeUpvoted(LikeUpvotedEvent<?> event) {
        try {
            // 统一使用 contentStats 表的 likes 字段处理所有类型的点赞统计
            // 包括：post点赞、comment点赞、roadmap投票、memory_card_deck点赞
            contentStatsDataService.incrementLikes(event.getContentType(), event.getContentId(), 1);
            log.debug("增加点赞统计: contentType={}, contentId={}", event.getContentType(), event.getContentId());
        } catch (Exception e) {
            log.error("处理点赞事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    /**
     * 取消两次能懂点赞事件
     */
    @EventListener
    @Async
    public void onTwiceUpvoteCancelled(TwiceUpvoteCancelledEvent<?> event) {
        try {
            contentStatsDataService.incrementTwice(event.getContentType(), event.getContentId(), -1);
            log.debug("取消两次能懂点赞: contentType={}, contentId={}", event.getContentType(), event.getContentId());
        } catch (Exception e) {
            log.error("处理取消两次能懂点赞事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    /**
     * 取消点赞事件 - 统一处理所有内容类型的取消点赞统计
     */
    @EventListener
    @Async
    public void onLikeUpvoteCancelled(LikeUpvoteCancelledEvent<?> event) {
        try {
            // 统一使用 contentStats 表的 likes 字段处理所有类型的取消点赞统计
            // 包括：post取消点赞、comment取消点赞、roadmap取消投票、memory_card_deck取消点赞
            contentStatsDataService.incrementLikes(event.getContentType(), event.getContentId(), -1);
            log.debug("取消点赞统计: contentType={}, contentId={}", event.getContentType(), event.getContentId());
        } catch (Exception e) {
            log.error("处理取消点赞事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    /**
     * 点赞类型切换事件
     * 一次性处理统计更新：减少旧类型，增加新类型
     */
    @EventListener
    @Async
    public void onUpvoteTypeSwitched(UpvoteTypeSwitchedEvent<?> event) {
        try {
            // 减少旧类型统计
            if (event.getFromType() == VoteType.twice.value()) {
                contentStatsDataService.incrementTwice(event.getContentType(), event.getContentId(), -1);
            } else {
                contentStatsDataService.incrementLikes(event.getContentType(), event.getContentId(), -1);
            }

            // 增加新类型统计
            if (event.getToType() == VoteType.twice.value()) {
                contentStatsDataService.incrementTwice(event.getContentType(), event.getContentId(), 1);
            } else {
                contentStatsDataService.incrementLikes(event.getContentType(), event.getContentId(), 1);
            }

            log.debug("切换点赞类型统计: contentType={}, contentId={}, from={}, to={}",
                event.getContentType(), event.getContentId(), event.getFromType(), event.getToType());
        } catch (Exception e) {
            log.error("处理点赞类型切换事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    // ==================== 评论事件 ====================

    /**
     * 评论创建事件
     */
    @EventListener
    @Async
    public void onCommentCreated(CommentCreatedEvent event) {
        try {
            contentStatsDataService.incrementComments(event.getContentType(), event.getContentId(), 1);
            log.debug("增加评论数: contentType={}, contentId={}", event.getContentType(), event.getContentId());
        } catch (Exception e) {
            log.error("处理评论创建事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    /**
     * 评论删除事件
     */
    @EventListener
    @Async
    public void onCommentDeleted(CommentDeletedEvent event) {
        try {
            contentStatsDataService.incrementComments(event.getContentType(), event.getContentId(), -1);
            log.debug("减少评论数: contentType={}, contentId={}", event.getContentType(), event.getContentId());
        } catch (Exception e) {
            log.error("处理评论删除事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    // ==================== 分享事件 ====================

    /**
     * 内容分享事件
     */
    @EventListener
    @Async
    public void onContentShared(ContentSharedEvent event) {
        try {
            contentStatsDataService.atomicIncrement(event.getContentType(), event.getContentId(), "shares", 1);
            log.debug("增加分享数: contentType={}, contentId={}", event.getContentType(), event.getContentId());
        } catch (Exception e) {
            log.error("处理内容分享事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    // ==================== 收藏事件 ====================

    /**
     * 内容收藏事件
     */
    @EventListener
    @Async
    public void onContentBookmarked(ContentBookmarkedEvent event) {
        try {
            contentStatsDataService.atomicIncrement(event.getContentType(), event.getContentId(), "bookmarks", 1);
            log.debug("增加收藏数: contentType={}, contentId={}", event.getContentType(), event.getContentId());
        } catch (Exception e) {
            log.error("处理内容收藏事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    /**
     * 取消内容收藏事件
     */
    @EventListener
    @Async
    public void onContentUnbookmarked(ContentUnbookmarkedEvent event) {
        try {
            contentStatsDataService.atomicIncrement(event.getContentType(), event.getContentId(), "bookmarks", -1);
            log.debug("减少收藏数: contentType={}, contentId={}", event.getContentType(), event.getContentId());
        } catch (Exception e) {
            log.error("处理取消内容收藏事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    // ==================== 学习进度事件 ====================

    /**
     * 课程/路线图学习开始事件
     */
    @EventListener
    @Async
    public void onLearningStarted(LearningStartedEvent event) {
        try {
            contentStatsDataService.atomicIncrement(event.getContentType(), event.getContentId(), "in_progress_users", 1);
            log.debug("增加学习中用户数: contentType={}, contentId={}", event.getContentType(), event.getContentId());
        } catch (Exception e) {
            log.error("处理学习开始事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    /**
     * 课程/路线图学习完成事件
     */
    @EventListener
    @Async
    public void onLearningCompleted(LearningCompletedEvent event) {
        try {
            // 学习中用户减1，完成用户加1
            contentStatsDataService.atomicIncrement(event.getContentType(), event.getContentId(), "in_progress_users", -1);
            contentStatsDataService.atomicIncrement(event.getContentType(), event.getContentId(), "completed_users", 1);
            log.debug("学习完成状态转换: contentType={}, contentId={}", event.getContentType(), event.getContentId());
        } catch (Exception e) {
            log.error("处理学习完成事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }
}