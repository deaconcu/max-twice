package com.prosper.learn.analytics.application.listener;

import com.prosper.learn.analytics.stats.service.UserStatsDomainService;
import com.prosper.learn.analytics.stats.service.UserLearningStatsService;
import com.prosper.learn.shared.domain.event.content.interaction.ContentBookmarkedEvent;
import com.prosper.learn.shared.domain.event.content.interaction.ContentUnbookmarkedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentApprovedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentDeletedEvent;
import com.prosper.learn.shared.domain.event.user.learning.LearningCancelledEvent;
import com.prosper.learn.shared.domain.event.user.learning.LearningCompletedEvent;
import com.prosper.learn.shared.domain.event.user.learning.LearningStartedEvent;
import com.prosper.learn.shared.domain.event.user.learning.NodeCompletedEvent;
import com.prosper.learn.shared.domain.event.user.relationship.UserFollowedEvent;
import com.prosper.learn.shared.domain.event.user.relationship.UserUnfollowedEvent;
import com.prosper.learn.shared.domain.event.user.review.CardReviewedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 用户统计事件监听器
 *
 * 负责监听用户相关事件，更新 user_stats 表的累计统计字段（直接写数据库）
 *
 * 【直接写数据库的字段】:
 * - learning_courses: 正在学习的课程数
 * - completed_courses: 已完成的课程数
 * - following_users: 关注的用户数
 * - following_courses: 关注的课程数
 * - following_roles: 关注的职业数
 * - created_articles: 创建的文章数
 * - created_indexs: 创建的目录数
 * - created_roadmaps: 创建的路线图数
 * - created_card_decks: 创建的卡片组数
 *
 * 【由 RedisStatsEventListener 处理的字段】（写 Redis，定时同步）:
 * - views: 内容创建者获得的总浏览量
 * - twices: 内容创建者获得的总两次能懂点赞数
 * - likes: 内容创建者获得的总有用点赞数
 * - comments: 内容创建者获得的总评论数
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserStatsEventListener {

    private final UserStatsDomainService userStatsService;
    private final UserLearningStatsService userLearningStatsService;

    // ==================== 社交关系统计 ====================

    /**
     * 关注用户事件 - 直接写数据库
     */
    @EventListener
    //@Async
    public void onUserFollowed(UserFollowedEvent event) {
        try {
            userStatsService.incrementFollowingUsers(event.getFollowerId(), 1);
            log.debug("增加关注用户统计，用户ID: {}", event.getFollowerId());
        } catch (Exception e) {
            log.error("处理关注用户事件失败，用户ID: {}", event.getFollowerId(), e);
        }
    }

    /**
     * 取关用户事件 - 直接写数据库
     */
    @EventListener
    //@Async
    public void onUserUnfollowed(UserUnfollowedEvent event) {
        try {
            userStatsService.incrementFollowingUsers(event.getFollowerId(), -1);
            log.debug("减少关注用户统计，用户ID: {}", event.getFollowerId());
        } catch (Exception e) {
            log.error("处理取关用户事件失败，用户ID: {}", event.getFollowerId(), e);
        }
    }

    /**
     * 内容收藏事件 - 直接写数据库
     * 统一处理课程关注、职业关注等
     */
    @EventListener
    //@Async
    public void onContentBookmarked(ContentBookmarkedEvent event) {
        try {
            switch (event.getContentType()) {
                case course:
                    userStatsService.incrementFollowingCourses(event.getUserId(), 1);
                    log.debug("增加关注课程统计，用户ID: {}", event.getUserId());
                    break;
                case role:
                    userStatsService.incrementFollowingRoles(event.getUserId(), 1);
                    log.debug("增加关注职业统计，用户ID: {}", event.getUserId());
                    break;
                default:
                    log.debug("内容收藏事件，暂不统计: contentType={}, userId={}",
                        event.getContentType(), event.getUserId());
            }
        } catch (Exception e) {
            log.error("处理内容收藏事件失败，用户ID: {}", event.getUserId(), e);
        }
    }

    /**
     * 取消内容收藏事件 - 直接写数据库
     */
    @EventListener
    //@Async
    public void onContentUnbookmarked(ContentUnbookmarkedEvent event) {
        try {
            switch (event.getContentType()) {
                case course:
                    userStatsService.incrementFollowingCourses(event.getUserId(), -1);
                    log.debug("减少关注课程统计，用户ID: {}", event.getUserId());
                    break;
                case role:
                    userStatsService.incrementFollowingRoles(event.getUserId(), -1);
                    log.debug("减少关注职业统计，用户ID: {}", event.getUserId());
                    break;
                default:
                    log.debug("取消内容收藏事件，暂不统计: contentType={}, userId={}",
                        event.getContentType(), event.getUserId());
            }
        } catch (Exception e) {
            log.error("处理取消内容收藏事件失败，用户ID: {}", event.getUserId(), e);
        }
    }

    // ==================== 学习进度统计 ====================

    /**
     * 学习开始事件 - 直接写数据库
     */
    @EventListener
    //@Async
    public void onLearningStarted(LearningStartedEvent event) {
        try {
            userStatsService.incrementLearningCourses(event.getUserId(), 1);
            log.debug("增加学习课程统计，用户ID: {}", event.getUserId());
        } catch (Exception e) {
            log.error("处理学习开始事件失败，用户ID: {}", event.getUserId(), e);
        }
    }

    /**
     * 学习取消事件 - 直接写数据库
     * 学习中的课程减1
     */
    @EventListener
    //@Async
    public void onLearningCancelled(LearningCancelledEvent event) {
        try {
            userStatsService.incrementLearningCourses(event.getUserId(), -1);
            log.debug("减少学习课程统计，用户ID: {}", event.getUserId());
        } catch (Exception e) {
            log.error("处理学习取消事件失败，用户ID: {}", event.getUserId(), e);
        }
    }

    /**
     * 学习完成事件 - 直接写数据库
     * 学习中的课程减1，已完成课程加1
     */
    @EventListener
    //@Async
    public void onLearningCompleted(LearningCompletedEvent event) {
        try {
            userStatsService.incrementLearningCourses(event.getUserId(), -1);
            userStatsService.incrementCompletedCourses(event.getUserId(), 1);
            log.debug("学习完成状态转换，用户ID: {}", event.getUserId());
        } catch (Exception e) {
            log.error("处理学习完成事件失败，用户ID: {}", event.getUserId(), e);
        }
    }

    // ==================== 内容创作统计 ====================

    /**
     * 内容审核通过事件 - 直接写数据库
     * 统一处理文章、路线图、目录、卡片组创建等
     */
    @EventListener
    //@Async
    public void onContentApproved(ContentApprovedEvent event) {
        try {
            switch (event.getContentType()) {
                case post:
                    userStatsService.incrementCreatedArticles(event.getCreatorId(), 1);
                    log.debug("增加创建文章统计，创建者ID: {}", event.getCreatorId());
                    break;
                case roadmap:
                    userStatsService.incrementCreatedRoadmaps(event.getCreatorId(), 1);
                    log.debug("增加创建路线图统计，创建者ID: {}", event.getCreatorId());
                    break;
                case memory_card_deck:
                    userStatsService.incrementCreatedCardDecks(event.getCreatorId(), 1);
                    log.debug("增加创建卡片组统计，创建者ID: {}", event.getCreatorId());
                    break;
                case node:
                    userStatsService.incrementCreatedIndexs(event.getCreatorId(), 1);
                    log.debug("增加创建目录统计，创建者ID: {}", event.getCreatorId());
                    break;
                default:
                    log.debug("内容创建事件，暂不统计: contentType={}, creatorId={}",
                        event.getContentType(), event.getCreatorId());
            }
        } catch (Exception e) {
            log.error("处理内容创建事件失败，创建者ID: {}", event.getCreatorId(), e);
        }
    }

    /**
     * 内容删除事件 - 直接写数据库
     */
    @EventListener
    //@Async
    public void onContentDeleted(ContentDeletedEvent event) {
        try {
            switch (event.getContentType()) {
                case post:
                    userStatsService.incrementCreatedArticles(event.getCreatorId(), -1);
                    log.debug("减少创建文章统计，创建者ID: {}", event.getCreatorId());
                    break;
                case roadmap:
                    userStatsService.incrementCreatedRoadmaps(event.getCreatorId(), -1);
                    log.debug("减少创建路线图统计，创建者ID: {}", event.getCreatorId());
                    break;
                case memory_card_deck:
                    userStatsService.incrementCreatedCardDecks(event.getCreatorId(), -1);
                    log.debug("减少创建卡片组统计，创建者ID: {}", event.getCreatorId());
                    break;
                case node:
                    userStatsService.incrementCreatedIndexs(event.getCreatorId(), -1);
                    log.debug("减少创建目录统计，创建者ID: {}", event.getCreatorId());
                    break;
                default:
                    log.debug("内容删除事件，暂不统计: contentType={}, creatorId={}",
                        event.getContentType(), event.getCreatorId());
            }
        } catch (Exception e) {
            log.error("处理内容删除事件失败，创建者ID: {}", event.getCreatorId(), e);
        }
    }

    // ==================== 记忆卡片复习统计 ====================

    /**
     * 记忆卡片复习完成事件 - 更新连续复习天数 + 记录热力图数据
     */
    @EventListener
    public void onCardReviewed(CardReviewedEvent event) {
        try {
            userStatsService.updateReviewStreak(event.getUserId(), event.getUserToday());
            // 记录热力图数据（使用用户时区的今天日期）
            userLearningStatsService.recordReviewedCards(event.getUserId(), event.getUserToday(), 1);
            log.debug("更新复习统计，用户ID: {}, 日期: {}", event.getUserId(), event.getUserToday());
        } catch (Exception e) {
            log.error("处理卡片复习事件失败，用户ID: {}", event.getUserId(), e);
        }
    }

    // ==================== 节点完成统计（热力图 + 连续学习天数） ====================

    /**
     * 节点完成状态变更事件 - 记录热力图数据 + 更新连续学习天数
     */
    @EventListener
    public void onNodeCompleted(NodeCompletedEvent event) {
        try {
            if (event.isCompleted()) {
                // 记录热力图数据（使用用户时区的今天日期）
                userLearningStatsService.recordCompletedNode(event.getUserId(), event.getUserToday());
                // 更新连续学习天数
                userStatsService.updateLearningStreak(event.getUserId(), event.getUserToday());
                log.debug("记录完成节点，用户ID: {}, 节点ID: {}, 日期: {}", event.getUserId(), event.getNodeId(), event.getUserToday());
            } else {
                userLearningStatsService.recordUncompletedNode(event.getUserId(), event.getUserToday());
                log.debug("记录取消完成节点，用户ID: {}, 节点ID: {}, 日期: {}", event.getUserId(), event.getNodeId(), event.getUserToday());
            }
        } catch (Exception e) {
            log.error("处理节点完成事件失败，用户ID: {}", event.getUserId(), e);
        }
    }
}
