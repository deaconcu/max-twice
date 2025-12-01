package com.prosper.learn.domain.service.listener;

import com.prosper.learn.common.Enums.DailyStatType;
import com.prosper.learn.common.Enums.CumulativeStatType;
import com.prosper.learn.domain.event.content.interaction.*;
import com.prosper.learn.domain.event.content.voting.*;
import com.prosper.learn.domain.event.content.lifecycle.*;
import com.prosper.learn.domain.event.user.relationship.*;
import com.prosper.learn.domain.event.user.learning.*;
import com.prosper.learn.domain.service.UserStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 用户统计事件监听器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserStatsEventListener {

    private final UserStatsService userStatsService;

    // ==================== 日度增量统计事件 ====================

    /**
     * 浏览事件 - 日度增量
     */
    @EventListener
    @Async
    public void onContentViewed(ContentViewedEvent event) {
        if (event.getCreatorId() != null) {
            try {
                userStatsService.incrementDailyStat(event.getCreatorId(), DailyStatType.VIEWS, 1);
                log.debug("增加浏览量统计，创建者ID: {}", event.getCreatorId());
            } catch (Exception e) {
                log.error("处理浏览事件失败，创建者ID: {}", event.getCreatorId(), e);
            }
        }
    }

    /**
     * 评论事件 - 日度增量
     */
    @EventListener
    @Async
    public void onCommentCreated(CommentCreatedEvent event) {
        if (event.getContentCreatorId() != null) {
            try {
                userStatsService.incrementDailyStat(event.getContentCreatorId(), DailyStatType.COMMENTS, 1);
                log.debug("增加评论统计，创建者ID: {}", event.getContentCreatorId());
            } catch (Exception e) {
                log.error("处理评论事件失败，创建者ID: {}", event.getContentCreatorId(), e);
            }
        }
    }

    /**
     * 两次能懂点赞事件 - 日度增量
     */
    @EventListener
    @Async
    public void onTwiceUpvoted(TwiceUpvotedEvent event) {
        if (event.getContentCreatorId() != null) {
            try {
                userStatsService.incrementDailyStat(event.getContentCreatorId(), DailyStatType.TWICE, 1);
                log.debug("增加两次能懂点赞统计，创建者ID: {}", event.getContentCreatorId());
            } catch (Exception e) {
                log.error("处理两次能懂点赞事件失败，创建者ID: {}", event.getContentCreatorId(), e);
            }
        }
    }

    /**
     * 点赞事件 - 日度增量
     */
    @EventListener
    @Async
    public void onLikeUpvoted(LikeUpvotedEvent event) {
        if (event.getCreatorId() != null) {
            try {
                userStatsService.incrementDailyStat(event.getCreatorId(), DailyStatType.HELPFUL, 1);
                log.debug("增加点赞统计，创建者ID: {}", event.getCreatorId());
            } catch (Exception e) {
                log.error("处理点赞事件失败，创建者ID: {}", event.getCreatorId(), e);
            }
        }
    }

    // ==================== 累计统计事件 ====================

    /**
     * 关注用户事件 - 累计增量
     */
    @EventListener
    @Async
    public void onUserFollowed(UserFollowedEvent event) {
        try {
            userStatsService.incrementCumulativeStat(event.getFollowerId(), CumulativeStatType.FOLLOWING_USERS, 1);
            log.debug("增加关注用户统计，用户ID: {}", event.getFollowerId());
        } catch (Exception e) {
            log.error("处理关注用户事件失败，用户ID: {}", event.getFollowerId(), e);
        }
    }

    /**
     * 取关用户事件 - 累计增量
     */
    @EventListener
    @Async
    public void onUserUnfollowed(UserUnfollowedEvent event) {
        try {
            userStatsService.incrementCumulativeStat(event.getFollowerId(), CumulativeStatType.FOLLOWING_USERS, -1);
            log.debug("减少关注用户统计，用户ID: {}", event.getFollowerId());
        } catch (Exception e) {
            log.error("处理取关用户事件失败，用户ID: {}", event.getFollowerId(), e);
        }
    }

    /**
     * 内容收藏事件 - 累计增量
     * 统一处理课程关注、职业关注等
     */
    @EventListener
    @Async
    public void onContentBookmarked(ContentBookmarkedEvent event) {
        try {
            // 根据内容类型更新不同的统计
            switch (event.getContentType()) {
                case course:
                    userStatsService.incrementCumulativeStat(event.getUserId(), CumulativeStatType.FOLLOWING_COURSES, 1);
                    log.debug("增加关注课程统计，用户ID: {}", event.getUserId());
                    break;
                case profession:
                    userStatsService.incrementCumulativeStat(event.getUserId(), CumulativeStatType.FOLLOWING_PROFESSIONS, 1);
                    log.debug("增加关注职业统计，用户ID: {}", event.getUserId());
                    break;
                default:
                    log.debug("内容收藏事件，暂不统计: contentType={}, userId={}", event.getContentType(), event.getUserId());
            }
        } catch (Exception e) {
            log.error("处理内容收藏事件失败，用户ID: {}", event.getUserId(), e);
        }
    }

    /**
     * 取消内容收藏事件 - 累计增量
     */
    @EventListener
    @Async
    public void onContentUnbookmarked(ContentUnbookmarkedEvent event) {
        try {
            // 根据内容类型更新不同的统计
            switch (event.getContentType()) {
                case course:
                    userStatsService.incrementCumulativeStat(event.getUserId(), CumulativeStatType.FOLLOWING_COURSES, -1);
                    log.debug("减少关注课程统计，用户ID: {}", event.getUserId());
                    break;
                case profession:
                    userStatsService.incrementCumulativeStat(event.getUserId(), CumulativeStatType.FOLLOWING_PROFESSIONS, -1);
                    log.debug("减少关注职业统计，用户ID: {}", event.getUserId());
                    break;
                default:
                    log.debug("取消内容收藏事件，暂不统计: contentType={}, userId={}", event.getContentType(), event.getUserId());
            }
        } catch (Exception e) {
            log.error("处理取消内容收藏事件失败，用户ID: {}", event.getUserId(), e);
        }
    }

    /**
     * 学习开始事件 - 累计增量
     */
    @EventListener
    @Async
    public void onLearningStarted(LearningStartedEvent event) {
        try {
            userStatsService.incrementCumulativeStat(event.getUserId(), CumulativeStatType.LEARNING_COURSES, 1);
            log.debug("增加学习课程统计，用户ID: {}", event.getUserId());
        } catch (Exception e) {
            log.error("处理学习开始事件失败，用户ID: {}", event.getUserId(), e);
        }
    }

    /**
     * 学习完成事件 - 状态转换
     */
    @EventListener
    @Async
    public void onLearningCompleted(LearningCompletedEvent event) {
        try {
            // 学习中的课程减1，已完成课程加1
            userStatsService.incrementCumulativeStat(event.getUserId(), CumulativeStatType.LEARNING_COURSES, -1);
            userStatsService.incrementCumulativeStat(event.getUserId(), CumulativeStatType.COMPLETED_COURSES, 1);
            log.debug("学习完成状态转换，用户ID: {}", event.getUserId());
        } catch (Exception e) {
            log.error("处理学习完成事件失败，用户ID: {}", event.getUserId(), e);
        }
    }

    /**
     * 内容创建事件 - 累计增量
     * 统一处理文章、路线图、目录、卡片组创建等
     */
    @EventListener
    @Async
    public void onContentCreated(ContentCreatedEvent event) {
        try {
            // 根据内容类型更新不同的统计
            switch (event.getContentType()) {
                case post:
                    userStatsService.incrementCumulativeStat(event.getCreatorId(), CumulativeStatType.CREATED_ARTICLES, 1);
                    log.debug("增加创建文章统计，创建者ID: {}", event.getCreatorId());
                    break;
                case roadmap:
                    userStatsService.incrementCumulativeStat(event.getCreatorId(), CumulativeStatType.CREATED_ROADMAPS, 1);
                    log.debug("增加创建路线图统计，创建者ID: {}", event.getCreatorId());
                    break;
                case memory_card_deck:
                    userStatsService.incrementCumulativeStat(event.getCreatorId(), CumulativeStatType.CREATED_CARD_DECKS, 1);
                    log.debug("增加创建卡片组统计，创建者ID: {}", event.getCreatorId());
                    break;
                case node:
                    userStatsService.incrementCumulativeStat(event.getCreatorId(), CumulativeStatType.CREATED_INDEXS, 1);
                    log.debug("增加创建目录统计，创建者ID: {}", event.getCreatorId());
                    break;
                default:
                    log.debug("内容创建事件，暂不统计: contentType={}, creatorId={}", event.getContentType(), event.getCreatorId());
            }
        } catch (Exception e) {
            log.error("处理内容创建事件失败，创建者ID: {}", event.getCreatorId(), e);
        }
    }

    /**
     * 内容删除事件 - 累计增量
     */
    @EventListener
    @Async
    public void onContentDeleted(ContentDeletedEvent event) {
        try {
            // 根据内容类型更新不同的统计
            switch (event.getContentType()) {
                case post:
                    userStatsService.incrementCumulativeStat(event.getCreatorId(), CumulativeStatType.CREATED_ARTICLES, -1);
                    log.debug("减少创建文章统计，创建者ID: {}", event.getCreatorId());
                    break;
                case roadmap:
                    userStatsService.incrementCumulativeStat(event.getCreatorId(), CumulativeStatType.CREATED_ROADMAPS, -1);
                    log.debug("减少创建路线图统计，创建者ID: {}", event.getCreatorId());
                    break;
                case memory_card_deck:
                    userStatsService.incrementCumulativeStat(event.getCreatorId(), CumulativeStatType.CREATED_CARD_DECKS, -1);
                    log.debug("减少创建卡片组统计，创建者ID: {}", event.getCreatorId());
                    break;
                case node:
                    userStatsService.incrementCumulativeStat(event.getCreatorId(), CumulativeStatType.CREATED_INDEXS, -1);
                    log.debug("减少创建目录统计，创建者ID: {}", event.getCreatorId());
                    break;
                default:
                    log.debug("内容删除事件，暂不统计: contentType={}, creatorId={}", event.getContentType(), event.getCreatorId());
            }
        } catch (Exception e) {
            log.error("处理内容删除事件失败，创建者ID: {}", event.getCreatorId(), e);
        }
    }
}