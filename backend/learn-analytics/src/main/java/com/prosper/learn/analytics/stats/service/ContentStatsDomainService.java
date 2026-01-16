package com.prosper.learn.analytics.stats.service;

import com.prosper.learn.analytics.stats.dataservice.ContentStatsDataService;
import com.prosper.learn.analytics.stats.mapper.ContentStatsDO;
import com.prosper.learn.shared.domain.event.content.lifecycle.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.domain.Enums.ContentState;
import static com.prosper.learn.shared.domain.Enums.ContentType;

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

    /**
     * 获取内容的点赞数
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 点赞数，如果没有统计数据则返回0
     */
    public Integer getLikesCount(ContentType contentType, Long contentId) {
        return contentStatsDataService.getByContent(contentType, contentId)
            .map(stats -> stats.getLikeCount() != null ? stats.getLikeCount() : 0)
            .orElse(0);
    }

    /**
     * 获取内容的完整统计数据（包含数据库 + 今日Redis点赞增量）
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 统计数据，如果没有则返回空对象（各字段为0）
     */
    public ContentStatsDO getContentStats(ContentType contentType, Long contentId) {
        // 从数据库获取基础统计数据
        ContentStatsDO stats = contentStatsDataService.getByContent(contentType, contentId).orElse(null);

        log.debug("getContentStats - contentType={}, contentId={}, dbStats={}",
            contentType, contentId, stats != null ? "存在" : "不存在");

        if (stats == null) {
            // 没有统计数据，创建空对象
            stats = new ContentStatsDO();
            stats.setContentId(contentId);
            stats.setContentType(contentType.value());
            stats.setLikeCount(0);
            stats.setCommentCount(0);
            stats.setLearnerCount(0);
            log.debug("数据库无记录，创建空对象");
            // 继续执行，检查Redis增量
        }

        // 从Redis获取今日点赞增量
        Map<Long, Integer> redisLikesIncrement = redisStatsDomainService.getTodayLikesIncrement(
            contentType, List.of(contentId));
        int redisIncrement = redisLikesIncrement.getOrDefault(contentId, 0);

        log.debug("Redis增量 - contentId={}, increment={}", contentId, redisIncrement);

        if (redisIncrement != 0) {
            // 需要加上Redis增量（可正可负），创建新对象避免修改原对象
            ContentStatsDO adjustedStats = new ContentStatsDO();
            adjustedStats.setContentType(stats.getContentType());
            adjustedStats.setContentId(stats.getContentId());
            adjustedStats.setViewCount(stats.getViewCount());
            adjustedStats.setTwiceCount(stats.getTwiceCount());
            // 确保最终结果不为负
            int baseLikeCount = stats.getLikeCount() != null ? stats.getLikeCount() : 0;
            adjustedStats.setLikeCount(Math.max(0, baseLikeCount + redisIncrement));
            adjustedStats.setCommentCount(stats.getCommentCount());
            adjustedStats.setShareCount(stats.getShareCount());
            adjustedStats.setBookmarkCount(stats.getBookmarkCount());
            adjustedStats.setCompletedUserCount(stats.getCompletedUserCount());
            adjustedStats.setLearnerCount(stats.getLearnerCount());
            log.debug("返回调整后的统计 - likeCount={}", adjustedStats.getLikeCount());
            return adjustedStats;
        }

        log.debug("返回原始统计 - likeCount={}", stats.getLikeCount());
        return stats;
    }

    /**
     * 批量获取内容的点赞数（包含数据库 + 今日Redis增量）
     *
     * @param contentType 内容类型
     * @param contentIds 内容ID列表
     * @return 内容ID到点赞数的映射
     */
    public Map<Long, Integer> getBatchLikesCount(ContentType contentType, List<Long> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) {
            return new HashMap<>();
        }

        // 1. 从数据库获取基础统计数据
        List<ContentStatsDO> statsList = contentStatsDataService.batchGetByContentIds(contentType, contentIds);
        Map<Long, Integer> dbLikesMap = statsList.stream()
            .collect(Collectors.toMap(
                ContentStatsDO::getContentId,
                stats -> stats.getLikeCount() != null ? stats.getLikeCount() : 0
            ));

        // 2. 从Redis获取今日增量
        Map<Long, Integer> redisIncrementMap = redisStatsDomainService.getTodayLikesIncrement(contentType, contentIds);

        // 3. 合并数据库基数 + Redis增量
        Map<Long, Integer> result = new HashMap<>();
        for (Long contentId : contentIds) {
            int dbCount = dbLikesMap.getOrDefault(contentId, 0);
            int redisIncrement = redisIncrementMap.getOrDefault(contentId, 0);
            result.put(contentId, dbCount + redisIncrement);
        }

        return result;
    }

    /**
     * 批量获取内容的完整统计数据（包含数据库 + 今日Redis点赞增量）
     *
     * @param contentType 内容类型
     * @param contentIds 内容ID列表
     * @return 内容ID到统计数据的映射
     */
    public Map<Long, ContentStatsDO> getBatchContentStats(ContentType contentType, List<Long> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) {
            return new HashMap<>();
        }

        // 从数据库获取基础统计数据
        List<ContentStatsDO> statsList = contentStatsDataService.batchGetByContentIds(contentType, contentIds);

        // 从Redis获取今日点赞增量
        Map<Long, Integer> redisLikesIncrement = redisStatsDomainService.getTodayLikesIncrement(contentType, contentIds);

        // 构建结果，将点赞数加上Redis增量
        Map<Long, ContentStatsDO> result = new HashMap<>();
        for (ContentStatsDO stats : statsList) {
            int redisIncrement = redisLikesIncrement.getOrDefault(stats.getContentId(), 0);
            if (redisIncrement != 0) {
                // 需要加上Redis增量（可以是正数或负数），创建新对象避免修改原对象
                ContentStatsDO adjustedStats = new ContentStatsDO();
                adjustedStats.setContentType(stats.getContentType());
                adjustedStats.setContentId(stats.getContentId());
                adjustedStats.setViewCount(stats.getViewCount());
                adjustedStats.setTwiceCount(stats.getTwiceCount());
                adjustedStats.setLikeCount(Math.max(0, (stats.getLikeCount() != null ? stats.getLikeCount() : 0) + redisIncrement));
                adjustedStats.setCommentCount(stats.getCommentCount());
                adjustedStats.setShareCount(stats.getShareCount());
                adjustedStats.setBookmarkCount(stats.getBookmarkCount());
                adjustedStats.setCompletedUserCount(stats.getCompletedUserCount());
                adjustedStats.setLearnerCount(stats.getLearnerCount());
                result.put(stats.getContentId(), adjustedStats);
            } else {
                result.put(stats.getContentId(), stats);
            }
        }

        // 对于没有统计数据的内容，创建空的统计对象并检查Redis增量
        for (Long contentId : contentIds) {
            if (!result.containsKey(contentId)) {
                int redisIncrement = redisLikesIncrement.getOrDefault(contentId, 0);
                ContentStatsDO emptyStats = new ContentStatsDO();
                emptyStats.setContentId(contentId);
                emptyStats.setContentType(contentType.value());
                emptyStats.setLikeCount(Math.max(0, redisIncrement));
                emptyStats.setCommentCount(0);
                emptyStats.setLearnerCount(0);
                result.put(contentId, emptyStats);
            }
        }

        return result;
    }

    /**
     * 获取内容统计数据
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 统计数据DTO
     */
    /*
    public ContentStatsDTO getContentStats(Enums.ContentType contentType, Long contentId) {
        if (contentId == null || contentId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("内容ID无效: " + contentId);
        }

        Optional<ContentStatsDO> statsOpt = contentStatsDataService.getByContent(contentType, contentId);
        ContentStatsDO stats = statsOpt.orElse(new ContentStatsDO());

        // 构建统计DTO
        ContentStatsDTO.ContentStatsDTOBuilder builder = ContentStatsDTO.builder()
                .views(stats.getViews() != null ? stats.getViews() : 0)
                .comments(stats.getComments() != null ? stats.getComments() : 0)
                .shares(stats.getShares() != null ? stats.getShares() : 0)
                .bookmarks(stats.getBookmarks() != null ? stats.getBookmarks() : 0)
                .completedUsers(stats.getCompletedUsers() != null ? stats.getCompletedUsers() : 0)
                .inProgressUsers(stats.getInProgressUsers() != null ? stats.getInProgressUsers() : 0);

        // 帖子特殊处理：支持 twice 和 like 分别统计
        if (contentType == Enums.ContentType.post) {
            builder.twiceUpvotes(stats.getTwices() != null ? stats.getTwices() : 0)
                   .likeUpvotes(stats.getLikes() != null ? stats.getLikes() : 0);
        } else {
            // 其他内容类型只有 like 统计
            builder.twiceUpvotes(null)
                   .likeUpvotes(stats.getLikes() != null ? stats.getLikes() : 0);
        }

        return builder.build();
    }
     */

    // ==================== 事件监听：对象维度统计更新 ====================

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
                case comment -> handleCommentApproved(event);
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
            ContentStatsDO stats = contentStatsDataService.getByContent(event.getContentType(), event.getContentId()).orElse(null);
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
            ContentStatsDO stats = contentStatsDataService.getByContent(event.getContentType(), event.getContentId()).orElse(null);
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
                case comment -> handleCommentBanned(event);
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

    // ==================== Comment 处理 ====================

    private void handleCommentApproved(ContentApprovedEvent event) {
        if (event.getCommentTargetType() == null || event.getCommentTargetId() == null) return;
        contentStatsDataService.incrementComments(event.getCommentTargetType(), event.getCommentTargetId(), 1);
    }

    private void handleCommentBanned(ContentBannedEvent event) {
        if (event.getCommentTargetType() == null || event.getCommentTargetId() == null) return;
        contentStatsDataService.incrementComments(event.getCommentTargetType(), event.getCommentTargetId(), -1);
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
            case comment -> {
                if (event.getCommentTargetType() != null && event.getCommentTargetId() != null) {
                    contentStatsDataService.incrementComments(event.getCommentTargetType(), event.getCommentTargetId(), 1);
                }
            }
        }
    }
}