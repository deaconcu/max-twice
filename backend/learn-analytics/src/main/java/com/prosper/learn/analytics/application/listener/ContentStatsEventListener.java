package com.prosper.learn.analytics.application.listener;

import com.prosper.learn.analytics.stats.dataservice.ContentStatsDataService;
import com.prosper.learn.analytics.stats.dataservice.UserStatsDataService;
import com.prosper.learn.shared.domain.event.content.interaction.ContentBookmarkedEvent;
import com.prosper.learn.shared.domain.event.content.interaction.ContentSharedEvent;
import com.prosper.learn.shared.domain.event.content.interaction.ContentUnbookmarkedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.*;
import com.prosper.learn.shared.domain.event.user.learning.LearningCompletedEvent;
import com.prosper.learn.shared.domain.event.user.learning.LearningStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static com.prosper.learn.shared.domain.Enums.ContentState;
import static com.prosper.learn.shared.domain.Enums.ContentType;

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
    private final UserStatsDataService userStatsDataService;

    // ==================== 分享事件 ====================

    /**
     * 内容分享事件 - 直接写数据库
     */
    @EventListener
    //@Async
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
    //@Async
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
    //@Async
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
    //@Async
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
     * 课程/路线图学习完成事件 - 直接写数据库
     * 学习中用户减1，完成用户加1
     */
    @EventListener
    //@Async
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
    //@Async
    public void onContentApproved(ContentApprovedEvent event) {
        try {
            switch (event.getContentType()) {
                case post -> handlePostApproved(event);
                case roadmap -> handleRoadmapApproved(event);
                case memory_card_deck -> handleCardDeckApproved(event);
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
    //@Async
    public void onContentRejected(ContentRejectedEvent event) {
        try {
            // 增加 reject_count
            contentStatsDataService.incrementRejectCount(event.getContentType(), event.getContentId(), 1);

            // 检查是否需要自动升级为 BANNED
            com.prosper.learn.analytics.stats.mapper.ContentStatsDO stats =
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
    //@Async
    public void onContentRemoved(ContentRemovedEvent event) {
        try {
            // 增加 reject_count
            contentStatsDataService.incrementRejectCount(event.getContentType(), event.getContentId(), 1);

            // 检查是否需要自动升级为 BANNED
            com.prosper.learn.analytics.stats.mapper.ContentStatsDO stats =
                contentStatsDataService.getByContent(event.getContentType(), event.getContentId()).orElse(null);
            if (stats != null && stats.getRejectCount() != null && stats.getRejectCount() >= 3) {
                log.warn("内容 {} 违规次数达到 {}，需要自动升级为 BANNED (需要Service层处理)",
                    event.getContentId(), stats.getRejectCount());
            }

            // 减少对象统计
            switch (event.getContentType()) {
                case post -> handlePostRemoved(event);
                case roadmap -> handleRoadmapRemoved(event);
                case memory_card_deck -> handleCardDeckRemoved(event);
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
    //@Async
    public void onContentBanned(ContentBannedEvent event) {
        try {
            // 只有之前是 PUBLISHED 状态才需要减少统计
            if (event.getPreviousState() != ContentState.PUBLISHED.value()) {
                log.debug("内容之前不是PUBLISHED状态，无需减少对象统计: contentType={}, contentId={}, previousState={}",
                    event.getContentType(), event.getContentId(), event.getPreviousState());
                return;
            }

            // 减少对象统计
            switch (event.getContentType()) {
                case post -> handlePostBanned(event);
                case roadmap -> handleRoadmapBanned(event);
                case memory_card_deck -> handleCardDeckBanned(event);
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
    //@Async
    public void onContentRestored(ContentRestoredEvent event) {
        try {
            // 从 BANNED 恢复，需要恢复统计
            if (event.getPreviousState() == ContentState.BANNED.value()) {
                handleRestoreFromBanned(event);
                return;
            }

            // 从 REJECTED 恢复，只需 reject_count--
            if (event.getPreviousState() == ContentState.REJECTED.value()) {
                contentStatsDataService.incrementRejectCount(event.getContentType(), event.getContentId(), -1);
                log.debug("从REJECTED恢复，reject_count--: contentType={}, contentId={}",
                    event.getContentType(), event.getContentId());
            }
        } catch (Exception e) {
            log.error("处理内容恢复事件失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }

    // ==================== Post 处理 ====================

    private void handlePostApproved(ContentApprovedEvent event) {
        if (event.getNodeId() == null || event.getPostType() == null) {
            log.warn("Post审核通过事件缺少参数: nodeId={}, postType={}", event.getNodeId(), event.getPostType());
            return;
        }

        contentStatsDataService.incrementPosts(ContentType.node, event.getNodeId(), 1);

        if (event.getPostType() == 1) { // ARTICLE
            contentStatsDataService.incrementArticles(ContentType.node, event.getNodeId(), 1);
        } else if (event.getPostType() == 2) { // INDEX
            contentStatsDataService.incrementIndexes(ContentType.node, event.getNodeId(), 1);
        }

        log.debug("Post统计已增加: nodeId={}, postType={}", event.getNodeId(), event.getPostType());
    }

    private void handlePostRemoved(ContentRemovedEvent event) {
        if (event.getNodeId() == null || event.getPostType() == null) return;

        contentStatsDataService.incrementPosts(ContentType.node, event.getNodeId(), -1);

        if (event.getPostType() == 1) {
            contentStatsDataService.incrementArticles(ContentType.node, event.getNodeId(), -1);
        } else if (event.getPostType() == 2) {
            contentStatsDataService.incrementIndexes(ContentType.node, event.getNodeId(), -1);
        }

        log.debug("Post统计已减少: nodeId={}, postType={}", event.getNodeId(), event.getPostType());
    }

    private void handlePostBanned(ContentBannedEvent event) {
        if (event.getNodeId() == null || event.getPostType() == null) return;

        contentStatsDataService.incrementPosts(ContentType.node, event.getNodeId(), -1);

        if (event.getPostType() == 1) {
            contentStatsDataService.incrementArticles(ContentType.node, event.getNodeId(), -1);
        } else if (event.getPostType() == 2) {
            contentStatsDataService.incrementIndexes(ContentType.node, event.getNodeId(), -1);
        }
    }

    // ==================== Roadmap 处理 ====================

    private void handleRoadmapApproved(ContentApprovedEvent event) {
        if (event.getProfessionId() == null) return;
        contentStatsDataService.incrementRoadmaps(ContentType.profession, event.getProfessionId(), 1);
    }

    private void handleRoadmapRemoved(ContentRemovedEvent event) {
        if (event.getProfessionId() == null) return;
        contentStatsDataService.incrementRoadmaps(ContentType.profession, event.getProfessionId(), -1);
    }

    private void handleRoadmapBanned(ContentBannedEvent event) {
        if (event.getProfessionId() == null) return;
        contentStatsDataService.incrementRoadmaps(ContentType.profession, event.getProfessionId(), -1);
    }

    // ==================== MemoryCardDeck 处理 ====================

    private void handleCardDeckApproved(ContentApprovedEvent event) {
        // CardDeck 同时属于 Post 和 Node，需要同时更新两者的统计
        if (event.getPostId() != null) {
            contentStatsDataService.incrementCardDecks(ContentType.post, event.getPostId(), 1);
        }
        if (event.getNodeId() != null) {
            contentStatsDataService.incrementCardDecks(ContentType.node, event.getNodeId(), 1);
        }
    }

    private void handleCardDeckRemoved(ContentRemovedEvent event) {
        // CardDeck 同时属于 Post 和 Node，需要同时减少两者的统计
        if (event.getPostId() != null) {
            contentStatsDataService.incrementCardDecks(ContentType.post, event.getPostId(), -1);
        }
        if (event.getNodeId() != null) {
            contentStatsDataService.incrementCardDecks(ContentType.node, event.getNodeId(), -1);
        }
    }

    private void handleCardDeckBanned(ContentBannedEvent event) {
        // CardDeck 同时属于 Post 和 Node，需要同时减少两者的统计
        if (event.getPostId() != null) {
            contentStatsDataService.incrementCardDecks(ContentType.post, event.getPostId(), -1);
        }
        if (event.getNodeId() != null) {
            contentStatsDataService.incrementCardDecks(ContentType.node, event.getNodeId(), -1);
        }
    }

    // ==================== 恢复处理 ====================

    private void handleRestoreFromBanned(ContentRestoredEvent event) {
        switch (event.getContentType()) {
            case post -> {
                if (event.getNodeId() != null && event.getPostType() != null) {
                    contentStatsDataService.incrementPosts(ContentType.node, event.getNodeId(), 1);
                    if (event.getPostType() == 1) {
                        contentStatsDataService.incrementArticles(ContentType.node, event.getNodeId(), 1);
                    } else if (event.getPostType() == 2) {
                        contentStatsDataService.incrementIndexes(ContentType.node, event.getNodeId(), 1);
                    }
                }
            }
            case roadmap -> {
                if (event.getProfessionId() != null) {
                    contentStatsDataService.incrementRoadmaps(ContentType.profession, event.getProfessionId(), 1);
                }
            }
            case memory_card_deck -> {
                // CardDeck 同时属于 Post 和 Node，需要同时恢复两者的统计
                if (event.getPostId() != null) {
                    contentStatsDataService.incrementCardDecks(ContentType.post, event.getPostId(), 1);
                }
                if (event.getNodeId() != null) {
                    contentStatsDataService.incrementCardDecks(ContentType.node, event.getNodeId(), 1);
                }
            }
            // comment 的统计由 RedisStatsEventListener 处理，不在这里直接写数据库
        }
    }
}
