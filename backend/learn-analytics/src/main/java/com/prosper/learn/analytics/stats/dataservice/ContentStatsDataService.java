package com.prosper.learn.analytics.stats.dataservice;

import com.prosper.learn.analytics.stats.mapper.ContentStatsDO;
import com.prosper.learn.analytics.stats.mapper.ContentStatsMapper;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 内容统计数据服务
 *
 * 提供内容统计数据的核心操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentStatsDataService {

    private final ContentStatsMapper contentStatsMapper;

    // ==================== 基础CRUD操作 ====================

    /**
     * 根据内容类型和ID查询统计记录
     */
    public Optional<ContentStatsDO> getByContent(Enums.ContentType contentType, long contentId) {
        validateContentParams(contentType, contentId);

        ContentStatsDO stats = contentStatsMapper.getByContent(contentType.value(), contentId);
        return Optional.ofNullable(stats);
    }

    /**
     * 创建或获取内容统计记录
     * 如果记录不存在则创建一个初始记录
     */
    public ContentStatsDO getOrCreate(Enums.ContentType contentType, long contentId) {
        validateContentParams(contentType, contentId);

        return getByContent(contentType, contentId)
            .orElseGet(() -> createInitialStats(contentType, contentId));
    }

    /**
     * 创建初始统计记录
     */
    private ContentStatsDO createInitialStats(Enums.ContentType contentType, long contentId) {
        ContentStatsDO stats = new ContentStatsDO();
        stats.setContentType(contentType.value());
        stats.setContentId(contentId);
        stats.setViewCount(0);
        stats.setTwiceCount(0);
        stats.setLikeCount(0);
        stats.setCommentCount(0);
        stats.setShareCount(0);
        stats.setBookmarkCount(0);
        stats.setCompletedUserCount(0);
        stats.setLearnerCount(0);

        // 对象维度统计字段
        stats.setPostCount(0);
        stats.setArticleCount(0);
        stats.setIndexCount(0);
        stats.setRoadmapCount(0);
        stats.setCardDeckCount(0);

        // 违规统计字段
        stats.setRejectCount(0);

        int result = contentStatsMapper.insert(stats);
        if (result <= 0) {
            log.error("创建内容统计记录失败: contentType={}, contentId={}", contentType, contentId);
            throw StatusCode.DATABASE_ERROR.exception("创建内容统计记录失败");
        }

        log.debug("创建内容统计记录: contentType={}, contentId={}", contentType, contentId);
        return stats;
    }

    // ==================== 原子增量更新操作 ====================

    /**
     * 原子性增量更新指定字段（内部方法）
     *
     * 用于非按日统计字段的更新：shares, bookmarks, in_progress_users, completed_users
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @param field 字段名
     * @param delta 增量值（可正可负）
     */
    private boolean atomicIncrement(Enums.ContentType contentType, long contentId, String field, int delta) {
        validateContentParams(contentType, contentId);

        if (delta == 0) {
            return true;
        }

        // 确保统计记录存在
        getOrCreate(contentType, contentId);

        int result = contentStatsMapper.atomicIncrement(contentType.value(), contentId, field, delta);
        if (result > 0) {
            log.debug("原子增量更新: contentType={}, contentId={}, field={}, delta={}",
                contentType, contentId, field, delta);
            return true;
        }

        log.warn("原子增量更新失败: contentType={}, contentId={}, field={}, delta={}",
            contentType, contentId, field, delta);
        return false;
    }

    /** 增量更新分享数 */
    public boolean incrementShares(Enums.ContentType contentType, long contentId, int delta) {
        return atomicIncrement(contentType, contentId, "share_count", delta);
    }

    /** 增量更新收藏数 */
    public boolean incrementBookmarks(Enums.ContentType contentType, long contentId, int delta) {
        return atomicIncrement(contentType, contentId, "bookmark_count", delta);
    }

    /** 增量更新学习中用户数 */
    public boolean incrementInProgressUsers(Enums.ContentType contentType, long contentId, int delta) {
        return atomicIncrement(contentType, contentId, "learner_count", delta);
    }

    /** 增量更新已完成用户数 */
    public boolean incrementCompletedUsers(Enums.ContentType contentType, long contentId, int delta) {
        return atomicIncrement(contentType, contentId, "completed_user_count", delta);
    }

    /** 增量更新帖子数 */
    public boolean incrementPosts(Enums.ContentType contentType, long contentId, int delta) {
        return atomicIncrement(contentType, contentId, "post_count", delta);
    }

    /** 增量更新文章数 */
    public boolean incrementArticles(Enums.ContentType contentType, long contentId, int delta) {
        return atomicIncrement(contentType, contentId, "article_count", delta);
    }

    /** 增量更新目录数 */
    public boolean incrementIndexes(Enums.ContentType contentType, long contentId, int delta) {
        return atomicIncrement(contentType, contentId, "index_count", delta);
    }

    /** 增量更新路线图数 */
    public boolean incrementRoadmaps(Enums.ContentType contentType, long contentId, int delta) {
        return atomicIncrement(contentType, contentId, "roadmap_count", delta);
    }

    /** 增量更新卡片组数 */
    public boolean incrementCardDecks(Enums.ContentType contentType, long contentId, int delta) {
        return atomicIncrement(contentType, contentId, "card_deck_count", delta);
    }

    /** 增量更新评论数 */
    public boolean incrementComments(Enums.ContentType contentType, long contentId, int delta) {
        return atomicIncrement(contentType, contentId, "comment_count", delta);
    }

    /** 增量更新拒绝次数 */
    public boolean incrementRejectCount(Enums.ContentType contentType, long contentId, int delta) {
        return atomicIncrement(contentType, contentId, "reject_count", delta);
    }

    /**
     * 增量更新多个统计字段（用于 Redis 同步）
     *
     * 一次性更新 views, twices, likes, comments 四个按日统计字段
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @param viewsDelta 浏览量增量
     * @param twicesDelta 两次能懂增量
     * @param likesDelta 有用点赞增量
     * @param commentsDelta 评论数增量
     */
    public boolean increase(Enums.ContentType contentType, long contentId,
                           int viewsDelta, int twicesDelta, int likesDelta, int commentsDelta) {
        if (viewsDelta == 0 && twicesDelta == 0 && likesDelta == 0 && commentsDelta == 0) {
            return true;
        }

        validateContentParams(contentType, contentId);

        // 确保记录存在
        getOrCreate(contentType, contentId);

        int result = contentStatsMapper.increase(
            contentType.value(), contentId, viewsDelta, twicesDelta, likesDelta, commentsDelta);

        if (result > 0) {
            log.debug("增量更新统计字段: contentType={}, contentId={}, views+={}, twices+={}, likes+={}, comments+={}",
                contentType, contentId, viewsDelta, twicesDelta, likesDelta, commentsDelta);
            return true;
        }

        log.warn("增量更新统计字段失败: contentType={}, contentId={}", contentType, contentId);
        return false;
    }

    // ==================== 查询操作 ====================

    /**
     * 获取热门内容ID列表（按综合热度排序）
     * 综合热度 = 收藏数 + 学习中人数 + 已完成人数
     */
    public List<Long> getTopContentIdsByPopularity(Enums.ContentType contentType, int limit) {
        if (contentType == null) {
            throw StatusCode.INVALID_PARAMETER.exception("内容类型不能为空");
        }
        validateLimit(limit);

        return contentStatsMapper.getTopContentIdsByPopularity(contentType.value(), limit);
    }

    /**
     * 根据内容ID列表批量查询统计
     */
    public List<ContentStatsDO> batchGetByContentIds(Enums.ContentType contentType, List<Long> contentIds) {
        if (contentType == null) {
            throw StatusCode.INVALID_PARAMETER.exception("内容类型不能为空");
        }
        if (contentIds == null || contentIds.isEmpty()) {
            throw StatusCode.INVALID_PARAMETER.exception("内容ID列表不能为空");
        }

        return contentStatsMapper.batchGetByContentIds(contentType.name(), contentIds);
    }

    // ==================== 参数验证 ====================

    private void validateContentParams(Enums.ContentType contentType, long contentId) {
        if (contentType == null) {
            throw StatusCode.INVALID_PARAMETER.exception("内容类型不能为空");
        }
        if (contentId <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("内容ID必须大于0");
        }
    }

    private void validateLimit(int limit) {
        if (limit <= 0 || limit > 1000) {
            throw StatusCode.INVALID_PARAMETER.exception("限制数量必须在1-1000之间");
        }
    }
}
