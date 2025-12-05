package com.prosper.learn.analytics.stats.dataservice;

import com.prosper.learn.analytics.stats.mapper.ContentStatsDO;
import com.prosper.learn.analytics.stats.mapper.ContentStatsMapper;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.shared.domain.Enums;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 内容统计数据服务
 *
 * 提供内容统计数据的CRUD操作，包括：
 * - 基础数据操作：增删查改
 * - 原子性增量更新：支持并发安全的统计字段更新
 * - 批量操作：提高查询效率
 * - 排行榜查询：支持各种统计维度的排序
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
    public Optional<ContentStatsDO> getByContent(Enums.ContentType contentType, Long contentId) {
        validateContentParams(contentType, contentId);

        ContentStatsDO stats = contentStatsMapper.getByContent(contentType.value(), contentId);
        return Optional.ofNullable(stats);
    }

    /**
     * 创建或获取内容统计记录
     * 如果记录不存在则创建一个初始记录
     */
    public ContentStatsDO getOrCreate(Enums.ContentType contentType, Long contentId) {
        validateContentParams(contentType, contentId);

        return getByContent(contentType, contentId)
            .orElseGet(() -> createInitialStats(contentType, contentId));
    }

    /**
     * 创建初始统计记录
     */
    private ContentStatsDO createInitialStats(Enums.ContentType contentType, Long contentId) {
        ContentStatsDO stats = new ContentStatsDO();
        stats.setContentType(contentType.value());
        stats.setContentId(contentId);
        stats.setViews(0);
        stats.setTwice(0);
        stats.setLikes(0);
        stats.setComments(0);
        stats.setShares(0);
        stats.setBookmarks(0);
        stats.setCompletedUsers(0);
        stats.setInProgressUsers(0);

        int result = contentStatsMapper.insert(stats);
        if (result <= 0) {
            log.error("创建内容统计记录失败: contentType={}, contentId={}", contentType, contentId);
            throw ErrorCode.DATABASE_ERROR.exception("创建内容统计记录失败");
        }

        log.debug("创建内容统计记录: contentType={}, contentId={}", contentType, contentId);
        return stats;
    }

    /**
     * 根据内容删除统计记录
     */
    public boolean deleteByContent(Enums.ContentType contentType, Long contentId) {
        validateContentParams(contentType, contentId);

        int result = contentStatsMapper.deleteByContent(contentType.value(), contentId);
        if (result > 0) {
            log.info("删除内容统计记录: contentType={}, contentId={}", contentType, contentId);
            return true;
        }
        return false;
    }

    // ==================== 原子增量更新操作 ====================

    /**
     * 原子性增量更新指定字段
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @param field 字段名（views, twice, likes, comments等）
     * @param delta 增量值（可正可负）
     */
    public boolean atomicIncrement(Enums.ContentType contentType, Long contentId, String field, int delta) {
        validateContentParams(contentType, contentId);
        validateField(field);

        if (delta == 0) {
            return true; // 无需更新
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

    /**
     * 增加浏览量
     */
    public boolean incrementViews(Enums.ContentType contentType, Long contentId, int count) {
        return atomicIncrement(contentType, contentId, "views", count);
    }

    /**
     * 增加两次能懂数
     */
    public boolean incrementTwice(Enums.ContentType contentType, Long contentId, int count) {
        return atomicIncrement(contentType, contentId, "twice", count);
    }

    /**
     * 增加点赞数
     */
    public boolean incrementLikes(Enums.ContentType contentType, Long contentId, int count) {
        return atomicIncrement(contentType, contentId, "likes", count);
    }

    /**
     * 增加评论数
     */
    public boolean incrementComments(Enums.ContentType contentType, Long contentId, int count) {
        return atomicIncrement(contentType, contentId, "comments", count);
    }

    /**
     * 批量原子增量更新（用于高效更新多个字段）
     */
    public boolean batchAtomicIncrement(Enums.ContentType contentType, Long contentId,
                                       int viewsDelta, int twiceDelta, int likesDelta, int commentsDelta) {
        validateContentParams(contentType, contentId);

        // 如果所有增量都为0，无需更新
        if (viewsDelta == 0 && twiceDelta == 0 && likesDelta == 0 && commentsDelta == 0) {
            return true;
        }

        // 确保统计记录存在
        getOrCreate(contentType, contentId);

        int result = contentStatsMapper.batchAtomicIncrement(
            contentType.value(), contentId, viewsDelta, twiceDelta, likesDelta, commentsDelta);

        if (result > 0) {
            log.debug("批量原子增量更新: contentType={}, contentId={}, views={}, twice={}, likes={}, comments={}",
                contentType, contentId, viewsDelta, twiceDelta, likesDelta, commentsDelta);
            return true;
        }

        log.warn("批量原子增量更新失败: contentType={}, contentId={}", contentType, contentId);
        return false;
    }

    // ==================== 排行榜查询 ====================

    /**
     * 根据指定字段获取热门内容排行榜
     */
    public List<ContentStatsDO> getTopByField(Enums.ContentType contentType, String orderField, int limit) {
        validateContentParams(contentType, null);
        validateField(orderField);
        validateLimit(limit);

        return contentStatsMapper.getTopByField(contentType.value(), orderField, limit);
    }

    /**
     * 获取浏览量排行榜
     */
    public List<ContentStatsDO> getTopByViews(Enums.ContentType contentType, int limit) {
        return getTopByField(contentType, "views", limit);
    }

    /**
     * 获取点赞数排行榜
     */
    public List<ContentStatsDO> getTopByLikes(Enums.ContentType contentType, int limit) {
        return getTopByField(contentType, "likes", limit);
    }

    /**
     * 获取两次能懂排行榜
     */
    public List<ContentStatsDO> getTopByTwice(Enums.ContentType contentType, int limit) {
        return getTopByField(contentType, "twice", limit);
    }

    // ==================== 批量查询 ====================

    /**
     * 根据内容ID列表批量查询统计
     */
    public List<ContentStatsDO> batchGetByContentIds(Enums.ContentType contentType, List<Long> contentIds) {
        validateContentParams(contentType, null);
        if (contentIds == null || contentIds.isEmpty()) {
            throw ErrorCode.INVALID_PARAMETER.exception("内容ID列表不能为空");
        }

        return contentStatsMapper.batchGetByContentIds(contentType.name(), contentIds);
    }

    /**
     * 分页查询指定内容类型的统计记录
     */
    public List<ContentStatsDO> getByContentTypeWithPaging(Enums.ContentType contentType, int offset, int limit) {
        validateContentParams(contentType, null);
        validatePaging(offset, limit);

        return contentStatsMapper.getByContentTypeWithPaging(contentType.name(), offset, limit);
    }

    /**
     * 统计指定内容类型的记录总数
     */
    public int countByContentType(Enums.ContentType contentType) {
        validateContentParams(contentType, null);
        return contentStatsMapper.countByContentType(contentType.name());
    }

    // ==================== 数据维护 ====================

    /**
     * 清理统计值全为0的记录
     */
    public int cleanupEmptyStats() {
        int result = contentStatsMapper.cleanupEmptyStats();
        log.info("清理空统计记录: 删除{}条记录", result);
        return result;
    }

    // ==================== 参数验证 ====================

    private void validateContentParams(Enums.ContentType contentType, Long contentId) {
        if (contentType == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("内容类型不能为空");
        }
        if (contentId != null && contentId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("内容ID必须大于0");
        }
    }

    private void validateField(String field) {
        if (field == null || field.trim().isEmpty()) {
            throw ErrorCode.INVALID_PARAMETER.exception("字段名不能为空");
        }

        // 验证字段名是否合法
        String[] validFields = {"views", "twice", "likes", "comments", "shares", "bookmarks", "completed_users", "in_progress_users"};
        boolean isValid = false;
        for (String validField : validFields) {
            if (validField.equals(field)) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            throw ErrorCode.INVALID_PARAMETER.exception("不支持的字段名: " + field);
        }
    }

    private void validateLimit(int limit) {
        if (limit <= 0 || limit > 1000) {
            throw ErrorCode.INVALID_PARAMETER.exception("限制数量必须在1-1000之间");
        }
    }

    private void validatePaging(int offset, int limit) {
        if (offset < 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("偏移量不能为负数");
        }
        validateLimit(limit);
    }
}