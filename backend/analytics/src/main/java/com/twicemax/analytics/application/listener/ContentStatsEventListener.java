package com.twicemax.analytics.application.listener;

import com.twicemax.analytics.stats.dataservice.ContentStatsDataService;
import com.twicemax.analytics.stats.mapper.ContentStatsDO;
import com.twicemax.analytics.stats.service.ContentStatsDomainService;
import com.twicemax.shared.domain.event.content.interaction.ContentBookmarkedEvent;
import com.twicemax.shared.domain.event.content.interaction.ContentSharedEvent;
import com.twicemax.shared.domain.event.content.interaction.ContentUnbookmarkedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.*;
import com.twicemax.shared.domain.event.user.learning.LearningCancelledEvent;
import com.twicemax.shared.domain.event.user.learning.LearningCompletedEvent;
import com.twicemax.shared.domain.event.user.learning.LearningStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static com.twicemax.shared.domain.Enums.ContentState;

/**
 * 内容统计事件监听器
 *
 * 负责监听内容相关事件，更新 content_stats 表的非按日统计字段（直接写数据库）
 *
 * 【直接写数据库的字段】:
 * - shares: 分享数
 * - bookmarks: 收藏数
 * - in_progress_users: 学习中用户数
 * - completed_users: 已完成用户数
 * - posts, articles, indexes: 帖子统计（审核通过/删除时更新）
 * - roadmaps: 路线图统计（审核通过/删除时更新）
 * - card_decks: 卡片组统计（审核通过/删除时更新）
 * - reject_count: 被拒绝次数（reject/remove时更新）
 *
 * 【由 RedisStatsEventListener 处理的字段】（写 Redis，定时同步）:
 * - views: 浏览量
 * - twices: 两次能懂点赞数
 * - likes: 有用点赞数
 * - comments: 评论数
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContentStatsEventListener {

    private final ContentStatsDataService contentStatsDataService;
    private final ContentStatsDomainService contentStatsDomainService;

    // ==================== 分享事件 ====================

    /**
     * 内容分享事件 - 直接写数据库
     */
    @EventListener
    public void onContentShared(ContentSharedEvent event) {
        try {
            contentStatsDataService.incrementShares(event.getContentType(), event.getContentId(), 1);
            log.debug("增加分享数: contentType={}, contentId={}", event.getContentType(), event.getContentId());
        } catch (Exception e) {
            log.error("处理内容分享事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    // ==================== 收藏事件 ====================

    /**
     * 内容收藏事件 - 直接写数据库
     */
    @EventListener
    public void onContentBookmarked(ContentBookmarkedEvent event) {
        try {
            contentStatsDataService.incrementBookmarks(event.getContentType(), event.getContentId(), 1);
            log.debug("增加收藏数: contentType={}, contentId={}", event.getContentType(), event.getContentId());
        } catch (Exception e) {
            log.error("处理内容收藏事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    /**
     * 取消内容收藏事件 - 直接写数据库
     */
    @EventListener
    public void onContentUnbookmarked(ContentUnbookmarkedEvent event) {
        try {
            contentStatsDataService.incrementBookmarks(event.getContentType(), event.getContentId(), -1);
            log.debug("减少收藏数: contentType={}, contentId={}", event.getContentType(), event.getContentId());
        } catch (Exception e) {
            log.error("处理取消内容收藏事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    // ==================== 学习进度事件 ====================

    /**
     * 课程/路线图学习开始事件 - 直接写数据库
     */
    @EventListener
    public void onLearningStarted(LearningStartedEvent event) {
        try {
            contentStatsDataService.incrementInProgressUsers(event.getContentType(), event.getContentId(), 1);
            log.debug("增加学习中用户数: contentType={}, contentId={}", event.getContentType(), event.getContentId());
        } catch (Exception e) {
            log.error("处理学习开始事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    /**
     * 课程/路线图学习取消事件 - 直接写数据库
     * 学习中用户减1
     */
    @EventListener
    public void onLearningCancelled(LearningCancelledEvent event) {
        try {
            contentStatsDataService.incrementInProgressUsers(event.getContentType(), event.getContentId(), -1);
            log.debug("减少学习中用户数: contentType={}, contentId={}", event.getContentType(), event.getContentId());
        } catch (Exception e) {
            log.error("处理学习取消事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    /**
     * 课程/路线图学习完成事件 - 直接写数据库
     * 学习中用户减1，完成用户加1
     */
    @EventListener
    public void onLearningCompleted(LearningCompletedEvent event) {
        try {
            contentStatsDataService.incrementInProgressUsers(event.getContentType(), event.getContentId(), -1);
            contentStatsDataService.incrementCompletedUsers(event.getContentType(), event.getContentId(), 1);
            log.debug("学习完成状态转换: contentType={}, contentId={}", event.getContentType(), event.getContentId());
        } catch (Exception e) {
            log.error("处理学习完成事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    // ==================== 内容审核事件 - 对象维度统计更新 ====================

    /**
     * 监听内容审核通过事件 - 增加对象维度统计
     */
    @EventListener
    public void onContentApproved(ContentApprovedEvent event) {
        try {
            switch (event.getContentType()) {
                case post -> contentStatsDomainService.handlePostApproved(event);
                case memory_card_deck -> contentStatsDomainService.handleCardDeckApproved(event);
                // roadmap 走独立 RoadmapApprovedEvent
                // comment 的统计由 RedisStatsEventListener 写入 Redis，定时同步到数据库
                default -> log.debug("内容类型 {} 审核通过，无需更新对象统计", event.getContentType());
            }
        } catch (Exception e) {
            log.error("处理内容审核通过事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    /**
     * 监听内容审核拒绝事件 - 增加 reject_count
     */
    @EventListener
    public void onContentRejected(ContentRejectedEvent event) {
        try {
            // 增加 reject_count
            contentStatsDataService.incrementRejectCount(event.getContentType(), event.getContentId(), 1);

            // 检查是否需要自动升级为 BANNED
            ContentStatsDO stats =
                contentStatsDataService.getByContent(event.getContentType(), event.getContentId()).orElse(null);
            if (stats != null && stats.getRejectCount() != null && stats.getRejectCount() >= 3) {
                log.warn("内容 {} 违规次数达到 {}，需要自动升级为 BANNED (需要Service层处理)",
                    event.getContentId(), stats.getRejectCount());
            }

            log.debug("内容拒绝，reject_count++: contentType={}, contentId={}", event.getContentType(), event.getContentId());
        } catch (Exception e) {
            log.error("处理内容拒绝事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    /**
     * 监听内容下架事件 - 减少对象维度统计 + reject_count++
     */
    @EventListener
    public void onContentRemoved(ContentRemovedEvent event) {
        try {
            // 增加 reject_count
            contentStatsDataService.incrementRejectCount(event.getContentType(), event.getContentId(), 1);

            // 检查是否需要自动升级为 BANNED
            ContentStatsDO stats =
                contentStatsDataService.getByContent(event.getContentType(), event.getContentId()).orElse(null);
            if (stats != null && stats.getRejectCount() != null && stats.getRejectCount() >= 3) {
                log.warn("内容 {} 违规次数达到 {}，需要自动升级为 BANNED (需要Service层处理)",
                    event.getContentId(), stats.getRejectCount());
            }

            // 减少对象统计
            switch (event.getContentType()) {
                case post -> contentStatsDomainService.handlePostRemoved(event);
                case memory_card_deck -> contentStatsDomainService.handleCardDeckRemoved(event);
                default -> log.warn("内容类型 {} 不支持 REMOVE 操作", event.getContentType());
            }
        } catch (Exception e) {
            log.error("处理内容下架事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    /**
     * 监听内容封禁事件 - 减少对象维度统计（仅 PUBLISHED 状态）
     */
    @EventListener
    public void onContentBanned(ContentBannedEvent event) {
        try {
            // 只有之前是 PUBLISHED 状态才需要减少统计
            if (event.getPreviousState() != ContentState.PUBLISHED) {
                log.debug("内容之前不是PUBLISHED状态，无需减少对象统计: contentType={}, contentId={}, previousState={}",
                    event.getContentType(), event.getContentId(), event.getPreviousState());
                return;
            }

            // 减少对象统计
            switch (event.getContentType()) {
                case post -> contentStatsDomainService.handlePostBanned(event);
                case memory_card_deck -> contentStatsDomainService.handleCardDeckBanned(event);
                // roadmap 走独立 RoadmapBannedEvent
                // comment 的统计由 RedisStatsEventListener 处理
                default -> log.debug("内容类型 {} 封禁，无需更新对象统计", event.getContentType());
            }
        } catch (Exception e) {
            log.error("处理内容封禁事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    /**
     * 监听内容恢复事件 - 恢复对象维度统计 / reject_count--
     */
    @EventListener
    public void onContentRestored(ContentRestoredEvent event) {
        try {
            // 从 BANNED 恢复，需要恢复统计
            if (event.getPreviousState() == ContentState.BANNED) {
                contentStatsDomainService.handleRestoreFromBanned(event);
                return;
            }

            // 从 REJECTED 恢复，只需 reject_count--
            if (event.getPreviousState() == ContentState.REJECTED) {
                contentStatsDataService.incrementRejectCount(event.getContentType(), event.getContentId(), -1);
                log.debug("从REJECTED恢复，reject_count--: contentType={}, contentId={}",
                    event.getContentType(), event.getContentId());
            }
        } catch (Exception e) {
            log.error("处理内容恢复事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    // ==================== Roadmap revision 模型独立事件 ====================

    @EventListener
    public void onRoadmapApproved(RoadmapApprovedEvent event) {
        try {
            contentStatsDomainService.handleRoadmapApproved(event);
            log.debug("Roadmap 审核通过，统计++: roadmapId={}, roleId={}", event.getRoadmapId(), event.getRoleId());
        } catch (Exception e) {
            log.error("处理 Roadmap 审核通过事件失败: roadmapId={}", event.getRoadmapId(), e);
        }
    }

    @EventListener
    public void onRoadmapBanned(RoadmapBannedEvent event) {
        try {
            contentStatsDomainService.handleRoadmapBanned(event);
            log.debug("Roadmap 封禁，统计--: roadmapId={}, previousState={}",
                event.getRoadmapId(), event.getPreviousState());
        } catch (Exception e) {
            log.error("处理 Roadmap 封禁事件失败: roadmapId={}", event.getRoadmapId(), e);
        }
    }

    @EventListener
    public void onRoadmapRestored(RoadmapRestoredEvent event) {
        try {
            contentStatsDomainService.handleRoadmapRestored(event);
            log.debug("Roadmap 恢复: roadmapId={}, newState={}", event.getRoadmapId(), event.getNewState());
        } catch (Exception e) {
            log.error("处理 Roadmap 恢复事件失败: roadmapId={}", event.getRoadmapId(), e);
        }
    }
}
