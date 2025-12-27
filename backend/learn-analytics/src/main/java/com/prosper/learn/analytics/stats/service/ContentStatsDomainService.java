package com.prosper.learn.analytics.stats.service;

import com.prosper.learn.analytics.stats.dataservice.ContentStatsDataService;
import com.prosper.learn.analytics.stats.mapper.ContentStatsDO;
import com.prosper.learn.shared.domain.event.content.lifecycle.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
    @Async
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
    @Async
    public void onContentRejected(ContentRejectedEvent event) {
        try {
            // 增加 reject_count
            contentStatsDataService.atomicIncrement(event.getContentType(), event.getContentId(), "reject_count", 1);

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
    @Async
    public void onContentRemoved(ContentRemovedEvent event) {
        try {
            // 增加 reject_count
            contentStatsDataService.atomicIncrement(event.getContentType(), event.getContentId(), "reject_count", 1);

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
    @Async
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
    @Async
    public void onContentRestored(ContentRestoredEvent event) {
        try {
            // 从 BANNED 恢复，需要恢复统计
            if (event.getPreviousState() == ContentState.BANNED.value()) {
                handleRestoreFromBanned(event);
                return;
            }

            // 从 REJECTED 恢复，只需 reject_count--
            if (event.getPreviousState() == ContentState.REJECTED.value()) {
                contentStatsDataService.atomicIncrement(event.getContentType(), event.getContentId(), "reject_count", -1);
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

        contentStatsDataService.atomicIncrement(ContentType.node, event.getNodeId(), "posts", 1);

        if (event.getPostType() == 1) { // ARTICLE
            contentStatsDataService.atomicIncrement(ContentType.node, event.getNodeId(), "articles", 1);
        } else if (event.getPostType() == 2) { // INDEX
            contentStatsDataService.atomicIncrement(ContentType.node, event.getNodeId(), "indexes", 1);
        }

        log.debug("Post统计已增加: nodeId={}, postType={}", event.getNodeId(), event.getPostType());
    }

    private void handlePostRemoved(ContentRemovedEvent event) {
        if (event.getNodeId() == null || event.getPostType() == null) return;

        contentStatsDataService.atomicIncrement(ContentType.node, event.getNodeId(), "posts", -1);

        if (event.getPostType() == 1) {
            contentStatsDataService.atomicIncrement(ContentType.node, event.getNodeId(), "articles", -1);
        } else if (event.getPostType() == 2) {
            contentStatsDataService.atomicIncrement(ContentType.node, event.getNodeId(), "indexes", -1);
        }

        log.debug("Post统计已减少: nodeId={}, postType={}", event.getNodeId(), event.getPostType());
    }

    private void handlePostBanned(ContentBannedEvent event) {
        if (event.getNodeId() == null || event.getPostType() == null) return;

        contentStatsDataService.atomicIncrement(ContentType.node, event.getNodeId(), "posts", -1);

        if (event.getPostType() == 1) {
            contentStatsDataService.atomicIncrement(ContentType.node, event.getNodeId(), "articles", -1);
        } else if (event.getPostType() == 2) {
            contentStatsDataService.atomicIncrement(ContentType.node, event.getNodeId(), "indexes", -1);
        }
    }

    // ==================== Roadmap 处理 ====================

    private void handleRoadmapApproved(ContentApprovedEvent event) {
        if (event.getProfessionId() == null) return;
        contentStatsDataService.atomicIncrement(ContentType.profession, event.getProfessionId(), "roadmaps", 1);
    }

    private void handleRoadmapRemoved(ContentRemovedEvent event) {
        if (event.getProfessionId() == null) return;
        contentStatsDataService.atomicIncrement(ContentType.profession, event.getProfessionId(), "roadmaps", -1);
    }

    private void handleRoadmapBanned(ContentBannedEvent event) {
        if (event.getProfessionId() == null) return;
        contentStatsDataService.atomicIncrement(ContentType.profession, event.getProfessionId(), "roadmaps", -1);
    }

    // ==================== MemoryCardDeck 处理 ====================

    private void handleCardDeckApproved(ContentApprovedEvent event) {
        // CardDeck 同时属于 Post 和 Node，需要同时更新两者的统计
        if (event.getPostId() != null) {
            contentStatsDataService.atomicIncrement(ContentType.post, event.getPostId(), "card_decks", 1);
        }
        if (event.getNodeId() != null) {
            contentStatsDataService.atomicIncrement(ContentType.node, event.getNodeId(), "card_decks", 1);
        }
    }

    private void handleCardDeckRemoved(ContentRemovedEvent event) {
        // CardDeck 同时属于 Post 和 Node，需要同时减少两者的统计
        if (event.getPostId() != null) {
            contentStatsDataService.atomicIncrement(ContentType.post, event.getPostId(), "card_decks", -1);
        }
        if (event.getNodeId() != null) {
            contentStatsDataService.atomicIncrement(ContentType.node, event.getNodeId(), "card_decks", -1);
        }
    }

    private void handleCardDeckBanned(ContentBannedEvent event) {
        // CardDeck 同时属于 Post 和 Node，需要同时减少两者的统计
        if (event.getPostId() != null) {
            contentStatsDataService.atomicIncrement(ContentType.post, event.getPostId(), "card_decks", -1);
        }
        if (event.getNodeId() != null) {
            contentStatsDataService.atomicIncrement(ContentType.node, event.getNodeId(), "card_decks", -1);
        }
    }

    // ==================== Comment 处理 ====================

    private void handleCommentApproved(ContentApprovedEvent event) {
        if (event.getCommentTargetType() == null || event.getCommentTargetId() == null) return;
        contentStatsDataService.atomicIncrement(event.getCommentTargetType(), event.getCommentTargetId(), "comments", 1);
    }

    private void handleCommentBanned(ContentBannedEvent event) {
        if (event.getCommentTargetType() == null || event.getCommentTargetId() == null) return;
        contentStatsDataService.atomicIncrement(event.getCommentTargetType(), event.getCommentTargetId(), "comments", -1);
    }

    // ==================== 恢复处理 ====================

    private void handleRestoreFromBanned(ContentRestoredEvent event) {
        switch (event.getContentType()) {
            case post -> {
                if (event.getNodeId() != null && event.getPostType() != null) {
                    contentStatsDataService.atomicIncrement(ContentType.node, event.getNodeId(), "posts", 1);
                    if (event.getPostType() == 1) {
                        contentStatsDataService.atomicIncrement(ContentType.node, event.getNodeId(), "articles", 1);
                    } else if (event.getPostType() == 2) {
                        contentStatsDataService.atomicIncrement(ContentType.node, event.getNodeId(), "indexes", 1);
                    }
                }
            }
            case roadmap -> {
                if (event.getProfessionId() != null) {
                    contentStatsDataService.atomicIncrement(ContentType.profession, event.getProfessionId(), "roadmaps", 1);
                }
            }
            case memory_card_deck -> {
                // CardDeck 同时属于 Post 和 Node，需要同时恢复两者的统计
                if (event.getPostId() != null) {
                    contentStatsDataService.atomicIncrement(ContentType.post, event.getPostId(), "card_decks", 1);
                }
                if (event.getNodeId() != null) {
                    contentStatsDataService.atomicIncrement(ContentType.node, event.getNodeId(), "card_decks", 1);
                }
            }
            case comment -> {
                if (event.getCommentTargetType() != null && event.getCommentTargetId() != null) {
                    contentStatsDataService.atomicIncrement(event.getCommentTargetType(), event.getCommentTargetId(), "comments", 1);
                }
            }
        }
    }
}