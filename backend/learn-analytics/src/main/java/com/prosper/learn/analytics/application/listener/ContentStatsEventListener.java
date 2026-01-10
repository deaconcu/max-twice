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
}
