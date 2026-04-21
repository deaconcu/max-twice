package com.twicemax.analytics.stats.service;

import com.twicemax.analytics.dto.ContentStatsDTO;
import com.twicemax.analytics.dto.DailyStatsDTO;
import com.twicemax.analytics.stats.dataservice.ContentStatsDataService;
import com.twicemax.analytics.stats.mapper.ContentStatsDO;
import com.twicemax.shared.domain.event.content.lifecycle.ContentApprovedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.ContentBannedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.ContentRemovedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.ContentRestoredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.twicemax.shared.domain.Enums.ContentType;
import static com.twicemax.shared.domain.Enums.PostType;

/**
 * 内容统计业务服务
 *
 * 负责内容统计相关的业务逻辑，包括：
 * - 统计数据查询
 * - 统计数据汇总
 * - 排行榜业务逻辑
 * - 统计数据展示格式化
 * - 监听内容审核事件，更新对象维度的统计（content_stats表）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentStatsDomainService {

    private final ContentStatsDataService contentStatsDataService;
    private final RedisStatsDomainService redisStatsDomainService;

    // ==================== 查询方法（从 DailyStatsService 迁移）====================

    /**
     * 批量获取内容统计数据（统一查询入口）
     *
     * 逻辑：
     * - 按日统计字段（views, twice, likes, comments）: content_stats 累计 + 今日 Redis 增量
     * - 非按日统计字段（shares, bookmarks, completedUsers, inProgressUsers）: 直接从 content_stats 获取
     *
     * @param contentType 内容类型
     * @param contentIds 内容ID列表
     * @return Map<ContentId, ContentStatsDTO>
     */
    public Map<Long, ContentStatsDTO> batchGetContentStats(ContentType contentType, List<Long> contentIds) {
        Map<Long, ContentStatsDTO> result = new HashMap<>();

        if (contentIds == null || contentIds.isEmpty()) {
            return result;
        }

        log.debug("批量获取 {} 个{}的统计数据", contentIds.size(), contentType);

        try {
            // 1. 批量查询 content_stats 表的累计数据
            List<ContentStatsDO> contentStatsList =
                contentStatsDataService.batchGetByContentIds(contentType, contentIds);

            Map<Long, ContentStatsDO> statsMap = contentStatsList.stream()
                .collect(Collectors.toMap(ContentStatsDO::getContentId, stats -> stats));

            // 2. 批量获取今日 Redis 实时增量（只有 views, twice, likes, comments 四个字段）
            Map<Long, DailyStatsDTO> todayStatsMap = redisStatsDomainService.batchGetTodayStatsForContent(contentType, contentIds);

            // 3. 组装返回
            for (Long contentId : contentIds) {
                ContentStatsDO baseStats = statsMap.get(contentId);
                DailyStatsDTO todayStats = todayStatsMap.get(contentId);

                // 按日统计字段：累计 + 今日增量
                int baseViews = baseStats != null && baseStats.getViewCount() != null ? baseStats.getViewCount() : 0;
                int baseTwice = baseStats != null && baseStats.getTwiceCount() != null ? baseStats.getTwiceCount() : 0;
                int baseLikes = baseStats != null && baseStats.getLikeCount() != null ? baseStats.getLikeCount() : 0;
                int baseComments = baseStats != null && baseStats.getCommentCount() != null ? baseStats.getCommentCount() : 0;

                int todayViews = todayStats != null && todayStats.getViewCount() != null ? todayStats.getViewCount() : 0;
                int todayTwice = todayStats != null && todayStats.getTwiceCount() != null ? todayStats.getTwiceCount() : 0;
                int todayLikes = todayStats != null && todayStats.getLikeCount() != null ? todayStats.getLikeCount() : 0;
                int todayComments = todayStats != null && todayStats.getCommentCount() != null ? todayStats.getCommentCount() : 0;

                // 非按日统计字段：直接从 content_stats 获取（不加今日增量）
                int baseShares = baseStats != null && baseStats.getShareCount() != null ? baseStats.getShareCount() : 0;
                int baseBookmarks = baseStats != null && baseStats.getBookmarkCount() != null ? baseStats.getBookmarkCount() : 0;
                int baseCompleted = baseStats != null && baseStats.getCompletedUserCount() != null ? baseStats.getCompletedUserCount() : 0;
                int baseInProgress = baseStats != null && baseStats.getLearnerCount() != null ? baseStats.getLearnerCount() : 0;
                int baseNodeRefCount = baseStats != null && baseStats.getNodeReferenceCount() != null ? baseStats.getNodeReferenceCount() : 0;

                // 构建 DTO
                ContentStatsDTO dto = ContentStatsDTO.builder()
                    .contentId(contentId)
                    .viewCount(baseViews + todayViews)              // 累计 + 今日增量
                    .twiceCount(baseTwice + todayTwice)             // 累计 + 今日增量
                    .likeCount(baseLikes + todayLikes)              // 累计 + 今日增量
                    .commentCount(baseComments + todayComments)     // 累计 + 今日增量
                    .shareCount(baseShares)                         // 只用累计值
                    .bookmarkCount(baseBookmarks)                   // 只用累计值
                    .completedUserCount(baseCompleted)              // 只用累计值
                    .inProgressUserCount(baseInProgress)            // 只用累计值
                    .nodeReferenceCount(baseNodeRefCount)           // 只用累计值
                    .build();

                result.put(contentId, dto);
            }

            log.debug("成功获取 {} 个{}的统计数据", contentIds.size(), contentType);

        } catch (Exception e) {
            log.error("批量获取统计数据失败: contentType={}", contentType, e);
        }

        return result;
    }

    /**
     * 获取单个内容的统计数据（包含数据库 + 今日Redis增量）
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 统计数据，如果没有则返回空对象（各字段为0）
     */
    public ContentStatsDTO getContentStats(ContentType contentType, Long contentId) {
       // 1. 查询数据库累计数据
        ContentStatsDO baseStats = contentStatsDataService.getByContent(contentType, contentId).orElse(null);

        // 2. 查询今日Redis增量
        Map<Long, DailyStatsDTO> todayStatsMap = redisStatsDomainService.batchGetTodayStatsForContent(contentType, List.of(contentId));
        DailyStatsDTO todayStats = todayStatsMap.get(contentId);

        // 3. 构建返回的DTO（按日统计字段 = 累计 + 今日增量）
        return ContentStatsDTO.builder()
            .contentId(contentId)
            .viewCount((baseStats != null && baseStats.getViewCount() != null ? baseStats.getViewCount() : 0) +
                      (todayStats != null && todayStats.getViewCount() != null ? todayStats.getViewCount() : 0))
            .twiceCount((baseStats != null && baseStats.getTwiceCount() != null ? baseStats.getTwiceCount() : 0) +
                       (todayStats != null && todayStats.getTwiceCount() != null ? todayStats.getTwiceCount() : 0))
            .likeCount((baseStats != null && baseStats.getLikeCount() != null ? baseStats.getLikeCount() : 0) +
                      (todayStats != null && todayStats.getLikeCount() != null ? todayStats.getLikeCount() : 0))
            .commentCount((baseStats != null && baseStats.getCommentCount() != null ? baseStats.getCommentCount() : 0) +
                         (todayStats != null && todayStats.getCommentCount() != null ? todayStats.getCommentCount() : 0))
            .shareCount(baseStats != null && baseStats.getShareCount() != null ? baseStats.getShareCount() : 0)
            .bookmarkCount(baseStats != null && baseStats.getBookmarkCount() != null ? baseStats.getBookmarkCount() : 0)
            .completedUserCount(baseStats != null && baseStats.getCompletedUserCount() != null ? baseStats.getCompletedUserCount() : 0)
            .inProgressUserCount(baseStats != null && baseStats.getLearnerCount() != null ? baseStats.getLearnerCount() : 0)
            .nodeReferenceCount(baseStats != null && baseStats.getNodeReferenceCount() != null ? baseStats.getNodeReferenceCount() : 0)
            .build();
    }

    // ==================== 事件处理方法（由 ContentStatsEventListener 调用）====================

    public void handlePostApproved(ContentApprovedEvent event) {
        if (event.getNodeId() == null || event.getPostType() == null) {
            log.warn("Post审核通过事件缺少参数: nodeId={}, postType={}", event.getNodeId(), event.getPostType());
            return;
        }

        contentStatsDataService.incrementPosts(ContentType.node, event.getNodeId(), 1);

        if (event.getPostType() == PostType.index) {
            contentStatsDataService.incrementIndexes(ContentType.node, event.getNodeId(), 1);

            // 批量更新被引用节点的引用数统计
            if (event.getReferencedNodeIds() != null && !event.getReferencedNodeIds().isEmpty()) {
                contentStatsDataService.batchIncrementNodeReferences(ContentType.node, event.getReferencedNodeIds(), 1);
                log.debug("批量更新 {} 个节点引用数 +1（被目录帖子 {} 引用）",
                    event.getReferencedNodeIds().size(), event.getContentId());
            }
        } else if (event.getPostType() == PostType.article) {
            contentStatsDataService.incrementArticles(ContentType.node, event.getNodeId(), 1);
        }

        log.debug("Post统计已增加: nodeId={}, postType={}", event.getNodeId(), event.getPostType());
    }

    public void handlePostRemoved(ContentRemovedEvent event) {
        if (event.getNodeId() == null || event.getPostType() == null) return;

        contentStatsDataService.incrementPosts(ContentType.node, event.getNodeId(), -1);

        if (event.getPostType() == PostType.index) {
            contentStatsDataService.incrementIndexes(ContentType.node, event.getNodeId(), -1);

            // 批量更新被引用节点的引用数统计
            if (event.getReferencedNodeIds() != null && !event.getReferencedNodeIds().isEmpty()) {
                contentStatsDataService.batchIncrementNodeReferences(ContentType.node, event.getReferencedNodeIds(), -1);
                log.debug("批量更新 {} 个节点引用数 -1（目录帖子 {} 被下架）",
                    event.getReferencedNodeIds().size(), event.getContentId());
            }
        } else if (event.getPostType() == PostType.article) {
            contentStatsDataService.incrementArticles(ContentType.node, event.getNodeId(), -1);
        }

        log.debug("Post统计已减少: nodeId={}, postType={}", event.getNodeId(), event.getPostType());
    }

    public void handlePostBanned(ContentBannedEvent event) {
        if (event.getNodeId() == null || event.getPostType() == null) return;

        contentStatsDataService.incrementPosts(ContentType.node, event.getNodeId(), -1);

        if (event.getPostType() == PostType.index) {
            contentStatsDataService.incrementIndexes(ContentType.node, event.getNodeId(), -1);

            // 批量更新被引用节点的引用数统计
            if (event.getReferencedNodeIds() != null && !event.getReferencedNodeIds().isEmpty()) {
                contentStatsDataService.batchIncrementNodeReferences(ContentType.node, event.getReferencedNodeIds(), -1);
                log.debug("批量更新 {} 个节点引用数 -1（目录帖子 {} 被封禁）",
                    event.getReferencedNodeIds().size(), event.getContentId());
            }
        } else if (event.getPostType() == PostType.article) {
            contentStatsDataService.incrementArticles(ContentType.node, event.getNodeId(), -1);
        }
    }

    // ==================== Roadmap 处理 ====================

    public void handleRoadmapApproved(ContentApprovedEvent event) {
        if (event.getRoleId() == null) return;
        contentStatsDataService.incrementRoadmaps(ContentType.role, event.getRoleId(), 1);
    }

    public void handleRoadmapRemoved(ContentRemovedEvent event) {
        if (event.getRoleId() == null) return;
        contentStatsDataService.incrementRoadmaps(ContentType.role, event.getRoleId(), -1);
    }

    public void handleRoadmapBanned(ContentBannedEvent event) {
        if (event.getRoleId() == null) return;
        contentStatsDataService.incrementRoadmaps(ContentType.role, event.getRoleId(), -1);
    }

    // ==================== MemoryCardDeck 处理 ====================

    public void handleCardDeckApproved(ContentApprovedEvent event) {
        // CardDeck 同时属于 Post 和 Node，需要同时更新两者的统计
        if (event.getPostId() != null) {
            contentStatsDataService.incrementCardDecks(ContentType.post, event.getPostId(), 1);
        }
        if (event.getNodeId() != null) {
            contentStatsDataService.incrementCardDecks(ContentType.node, event.getNodeId(), 1);
        }
    }

    public void handleCardDeckRemoved(ContentRemovedEvent event) {
        // CardDeck 同时属于 Post 和 Node，需要同时减少两者的统计
        if (event.getPostId() != null) {
            contentStatsDataService.incrementCardDecks(ContentType.post, event.getPostId(), -1);
        }
        if (event.getNodeId() != null) {
            contentStatsDataService.incrementCardDecks(ContentType.node, event.getNodeId(), -1);
        }
    }

    public void handleCardDeckBanned(ContentBannedEvent event) {
        // CardDeck 同时属于 Post 和 Node，需要同时减少两者的统计
        if (event.getPostId() != null) {
            contentStatsDataService.incrementCardDecks(ContentType.post, event.getPostId(), -1);
        }
        if (event.getNodeId() != null) {
            contentStatsDataService.incrementCardDecks(ContentType.node, event.getNodeId(), -1);
        }
    }

    // ==================== 恢复处理 ====================

    public void handleRestoreFromBanned(ContentRestoredEvent event) {
        switch (event.getContentType()) {
            case post -> {
                if (event.getNodeId() != null && event.getPostType() != null) {
                    contentStatsDataService.incrementPosts(ContentType.node, event.getNodeId(), 1);
                    if (event.getPostType() == PostType.index) {
                        contentStatsDataService.incrementIndexes(ContentType.node, event.getNodeId(), 1);
                    } else if (event.getPostType() == PostType.article) {
                        contentStatsDataService.incrementArticles(ContentType.node, event.getNodeId(), 1);
                    }
                }
            }
            case roadmap -> {
                if (event.getRoleId() != null) {
                    contentStatsDataService.incrementRoadmaps(ContentType.role, event.getRoleId(), 1);
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