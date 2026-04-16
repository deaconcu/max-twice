package com.prosper.learn.analytics.stats.service;

import com.prosper.learn.infrastructure.redis.RedisKeyPrefix;
import com.prosper.learn.shared.common.constants.RedisStatsConstants;
import com.prosper.learn.shared.common.util.TimeZoneUtil;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.Enums.ContentType;
import com.prosper.learn.shared.domain.exception.BusinessException;
import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis统计数据服务
 * 
 * 该服务负责统计的Redis数据读写，包括用户和内容的每日统计数据的读写
 * 只写入当日数据，过期时间为3天
 * 读取时是为了合并今日数据和数据库中的历史数据
 * 
 * Redis数据结构:
 * - 用户统计: stats:YYYY-MM-DD:user -> {userId:statType: count}
 * - 内容统计: stats:YYYY-MM-DD:content -> {contentType:contentId:statType: count}
 * 
 * 统计类型包括:
 * - view: 浏览量
 * - twice: 两次能懂
 * - helpful: 有用点赞
 * - comment: 评论数
 * 
 * 数据流向: Redis实时统计 -> 定时同步 -> 数据库持久化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStatsDomainService {

    /** Redis模板，用于统计数据的存储和操作 */
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 生成用户统计Redis键名（带语言前缀）
     *
     * @param dateStr 日期字符串
     * @return 用户统计键名
     */
    private String generateUserStatsKey(String dateStr) {
        return RedisKeyPrefix.prefix(RedisStatsConstants.STATS_KEY_PREFIX + dateStr + RedisStatsConstants.USER_STATS_SUFFIX);
    }

    /**
     * 生成内容统计Redis键名（带语言前缀）
     *
     * @param dateStr 日期字符串
     * @return 内容统计键名
     */
    private String generateContentStatsKey(String dateStr) {
        return RedisKeyPrefix.prefix(RedisStatsConstants.STATS_KEY_PREFIX + dateStr + RedisStatsConstants.CONTENT_STATS_SUFFIX);
    }
    
    /**
     * 生成内容统计字段名
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @param statType 统计类型
     * @return 内容统计字段名，格式：contentType:contentId:statType
     */
    private String generateContentStatField(ContentType contentType, long contentId, String statType) {
        return contentType.value() + ":" + contentId + ":" + statType;
    }

    /**
     * 生成用户统计字段名
     *
     * @param userId 用户ID
     * @param statType 统计类型
     * @return 用户统计字段名，格式：userId:statType
     */
    private String generateUserStatField(long userId, String statType) {
        return userId + ":" + statType;
    }
    
    /**
     * 安全地执行Redis操作并记录统计
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @param userId 用户ID（可选）
     * @param statType 统计类型
     * @param increment 增量（正数为增加，负数为减少）
     * @param operation 操作描述（用于日志）
     */
    private void performStatsOperation(ContentType contentType, long contentId, Long userId, String statType,
                                       int increment, String operation) {
        String today = TimeZoneUtil.todayString();

        try {
            // 内容维度统计
            String contentKey = generateContentStatsKey(today);
            String contentField = generateContentStatField(contentType, contentId, statType);
            redisTemplate.opsForHash().increment(contentKey, contentField, increment);

            // 设置过期时间（只在增加时设置，避免重置已有数据的过期时间）
            if (increment > 0) {
                redisTemplate.expire(contentKey, Duration.ofDays(RedisStatsConstants.DEFAULT_EXPIRE_DAYS));
            }

            // 用户维度统计（如果提供了用户ID）
            if (userId != null && userId > 0) {
                String userKey = generateUserStatsKey(today);
                String userField = generateUserStatField(userId, statType);
                redisTemplate.opsForHash().increment(userKey, userField, increment);

                if (increment > 0) {
                    redisTemplate.expire(userKey, Duration.ofDays(RedisStatsConstants.DEFAULT_EXPIRE_DAYS));
                }
            }

            log.debug("{}: contentType={}, contentId={}, userId={}, statType={}",
                    operation, contentType, contentId, userId, statType);
        } catch (Exception e) {
            log.error("{}失败: contentType={}, contentId={}, userId={}, statType={}",
                    operation, contentType, contentId, userId, statType, e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 记录文章访问统计
     *
     * 当用户访问文章时调用此方法，实时增加访问统计计数。
     * 同时更新文章维度和用户维度的统计数据。
     *
     * Redis操作说明:
     * 1. 文章维度: stats:2024-08-24:post -> {123:view: 1, 124:view: 5, ...}
     * 2. 用户维度: stats:2024-08-24:user -> {456:view: 3, 789:view: 7, ...}
     * 3. 设置3天过期时间，确保异常情况下Redis数据会自动清理
     *
     * @param articleId 文章ID
     * @param userId 用户ID，可以为0（匿名访问）
     */
    public void recordArticleView(long articleId, long userId) {
        Long userIdForStats = userId > 0 ? userId : null;
        performStatsOperation(ContentType.post, articleId, userIdForStats, RedisStatsConstants.STAT_TYPE_VIEW, 1, "记录文章访问");
    }

    /**
     * 批量获取今日 Redis 中的点赞增量
     *
     * @param contentType 内容类型
     * @param contentIds 内容ID列表
     * @return 内容ID到今日点赞增量的映射
     */
    public Map<Long, Integer> getTodayLikesIncrement(ContentType contentType, List<Long> contentIds) {
        Map<Long, Integer> result = new HashMap<>();
        if (contentIds == null || contentIds.isEmpty()) {
            return result;
        }

        String todayStr = TimeZoneUtil.todayString();
        String contentStatsKey = generateContentStatsKey(todayStr);

        for (Long contentId : contentIds) {
            String fieldName = generateContentStatField(contentType, contentId, RedisStatsConstants.STAT_TYPE_LIKE);
            try {
                Object value = redisTemplate.opsForHash().get(contentStatsKey, fieldName);
                int increment = (value != null) ? Integer.parseInt(value.toString()) : 0;
                result.put(contentId, increment);
            } catch (Exception e) {
                log.warn("获取Redis点赞增量失败: contentType={}, contentId={}", contentType, contentId, e);
                result.put(contentId, 0);
            }
        }

        return result;
    }

    // ==================== 统计增量方法（供事件监听器调用）====================

    /**
     * 增量更新内容统计到 Redis
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @param statType 统计类型（view/twice/like/comment）
     * @param delta 增量值
     */
    private void incrementContentStat(ContentType contentType, Long contentId, String statType, int delta) {
        String today = TimeZoneUtil.todayString();
        String contentKey = generateContentStatsKey(today);
        String contentField = generateContentStatField(contentType, contentId, statType);

        redisTemplate.opsForHash().increment(contentKey, contentField, delta);

        // 设置过期时间（只在增加时设置）
        if (delta > 0) {
            redisTemplate.expire(contentKey, Duration.ofDays(RedisStatsConstants.DEFAULT_EXPIRE_DAYS));
        }
    }

    /**
     * 增量更新用户统计到 Redis
     *
     * @param userId 用户ID（内容创建者）
     * @param statType 统计类型（view/twice/like/comment）
     * @param delta 增量值
     */
    private void incrementUserStat(Long userId, String statType, int delta) {
        String today = TimeZoneUtil.todayString();
        String userKey = generateUserStatsKey(today);
        String userField = generateUserStatField(userId, statType);

        redisTemplate.opsForHash().increment(userKey, userField, delta);

        // 设置过期时间（只在增加时设置）
        if (delta > 0) {
            redisTemplate.expire(userKey, Duration.ofDays(RedisStatsConstants.DEFAULT_EXPIRE_DAYS));
        }
    }

    /**
     * 记录内容浏览
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @param creatorId 内容创建者ID
     * @param delta 增量值（通常为1或-1）
     */
    public void incrementView(ContentType contentType, Long contentId, Long creatorId, int delta) {
        try {
            // 内容维度统计
            incrementContentStat(contentType, contentId, RedisStatsConstants.STAT_TYPE_VIEW, delta);

            // 用户维度统计（内容创建者获得浏览量）
            if (creatorId != null && creatorId > 0) {
                incrementUserStat(creatorId, RedisStatsConstants.STAT_TYPE_VIEW, delta);
            }

            log.debug("Redis记录浏览: contentType={}, contentId={}, creatorId={}, delta={}",
                contentType, contentId, creatorId, delta);
        } catch (Exception e) {
            log.error("Redis记录浏览失败: contentType={}, contentId={}",
                contentType, contentId, e);
        }
    }

    /**
     * 记录两次能懂点赞
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @param voterId 点赞用户ID
     * @param creatorId 内容创建者ID
     * @param delta 增量值（通常为1或-1）
     */
    public void incrementTwice(ContentType contentType, Long contentId, Long voterId, Long creatorId, int delta) {
        try {
            // 内容维度统计
            incrementContentStat(contentType, contentId, RedisStatsConstants.STAT_TYPE_TWICE, delta);

            // 用户维度统计（内容创建者获得点赞）
            if (creatorId != null && creatorId > 0) {
                incrementUserStat(creatorId, RedisStatsConstants.STAT_TYPE_TWICE, delta);
            }

            log.debug("Redis记录两次能懂点赞: contentType={}, contentId={}, voterId={}, creatorId={}, delta={}",
                contentType, contentId, voterId, creatorId, delta);
        } catch (Exception e) {
            log.error("Redis记录两次能懂点赞失败: contentId={}, voterId={}",
                contentId, voterId, e);
        }
    }

    /**
     * 记录有用点赞
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @param voterId 点赞用户ID
     * @param creatorId 内容创建者ID
     * @param delta 增量值（通常为1或-1）
     */
    public void incrementLike(ContentType contentType, Long contentId, Long voterId, Long creatorId, int delta) {
        try {
            // 内容维度统计
            incrementContentStat(contentType, contentId, RedisStatsConstants.STAT_TYPE_LIKE, delta);

            // 用户维度统计（内容创建者获得点赞）
            if (creatorId != null && creatorId > 0) {
                incrementUserStat(creatorId, RedisStatsConstants.STAT_TYPE_LIKE, delta);
            }

            log.debug("Redis记录有用点赞: contentType={}, contentId={}, voterId={}, creatorId={}, delta={}",
                contentType, contentId, voterId, creatorId, delta);
        } catch (Exception e) {
            log.error("Redis记录有用点赞失败: contentId={}, voterId={}",
                contentId, voterId, e);
        }
    }

    /**
     * 记录评论
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @param creatorId 内容创建者ID
     * @param delta 增量值（通常为1或-1）
     */
    public void incrementComment(ContentType contentType, Long contentId, Long creatorId, int delta) {
        try {
            // 内容维度统计
            incrementContentStat(contentType, contentId, RedisStatsConstants.STAT_TYPE_COMMENT, delta);

            // 用户维度统计（内容创建者获得评论）
            if (creatorId != null && creatorId > 0) {
                incrementUserStat(creatorId, RedisStatsConstants.STAT_TYPE_COMMENT, delta);
            }

            log.debug("Redis记录评论: contentType={}, contentId={}, creatorId={}, delta={}",
                contentType, contentId, creatorId, delta);
        } catch (Exception e) {
            log.error("Redis记录评论失败: contentType={}, contentId={}",
                contentType, contentId, e);
        }
    }

    // ==================== Redis 读取方法（从 DailyStatsService 迁移）====================

    /**
     * 获取用户今日统计（从Redis）
     *
     * @param userId 用户ID
     * @return 用户今日统计数据
     */
    public com.prosper.learn.analytics.dto.UserDailyStatsDTO getUserTodayStats(long userId) {
        String today = TimeZoneUtil.todayString();
        String userKey = generateUserStatsKey(today);

        try {
            Map<Object, Object> stats = redisTemplate.opsForHash().entries(userKey);
            return parseUserDailyStatsFromRedis(userId, stats);
        } catch (Exception e) {
            log.error("获取用户{}今日统计失败", userId, e);
            return com.prosper.learn.analytics.dto.UserDailyStatsDTO.empty();
        }
    }

    /**
     * 批量获取内容今日 Redis 统计增量
     *
     * @param contentType 内容类型
     * @param contentIds 内容ID列表
     * @return Map<ContentId, DailyStatsDTO>
     */
    public Map<Long, com.prosper.learn.analytics.dto.DailyStatsDTO> batchGetTodayStatsForContent(
            ContentType contentType, List<Long> contentIds) {
        Map<Long, com.prosper.learn.analytics.dto.DailyStatsDTO> result = new HashMap<>();

        if (contentIds == null || contentIds.isEmpty()) {
            return result;
        }

        String today = TimeZoneUtil.todayString();
        String redisKey = RedisStatsConstants.STATS_KEY_PREFIX + today + RedisStatsConstants.CONTENT_STATS_SUFFIX;

        // 检查 Redis key 是否存在
        Boolean keyExists = redisTemplate.hasKey(redisKey);
        if (keyExists == null || !keyExists) {
            log.debug("今日 Redis 统计数据不存在: {}", redisKey);
            return result;
        }

        int contentTypeValue = contentType.value();

        // 构建所有需要查询的字段名列表
        List<Object> fields = new ArrayList<>(contentIds.size() * 4);
        for (Long contentId : contentIds) {
            fields.add(contentTypeValue + ":" + contentId + ":" + RedisStatsConstants.STAT_TYPE_VIEW);
            fields.add(contentTypeValue + ":" + contentId + ":" + RedisStatsConstants.STAT_TYPE_TWICE);
            fields.add(contentTypeValue + ":" + contentId + ":" + RedisStatsConstants.STAT_TYPE_LIKE);
            fields.add(contentTypeValue + ":" + contentId + ":" + RedisStatsConstants.STAT_TYPE_COMMENT);
        }

        try {
            // 使用 HMGET 批量获取所有字段值
            List<Object> values = redisTemplate.opsForHash().multiGet(redisKey, fields);

            // 解析返回的结果
            for (int i = 0; i < contentIds.size(); i++) {
                Long contentId = contentIds.get(i);
                int baseIndex = i * 4;

                int views = parseRedisValue(values.get(baseIndex));
                int twice = parseRedisValue(values.get(baseIndex + 1));
                int likes = parseRedisValue(values.get(baseIndex + 2));
                int comments = parseRedisValue(values.get(baseIndex + 3));

                com.prosper.learn.analytics.dto.DailyStatsDTO stats =
                    com.prosper.learn.analytics.dto.DailyStatsDTO.builder()
                        .viewCount(views)
                        .twiceCount(twice)
                        .likeCount(likes)
                        .commentCount(comments)
                        .build();

                result.put(contentId, stats);
            }

            log.debug("批量获取{}个内容的今日统计，使用HMGET一次查询完成", contentIds.size());

        } catch (Exception e) {
            log.error("批量获取今日统计失败: contentType={}, contentIds.size={}",
                contentType, contentIds.size(), e);
        }

        return result;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 从 Redis 解析用户当日统计数据
     */
    private com.prosper.learn.analytics.dto.UserDailyStatsDTO parseUserDailyStatsFromRedis(
            long userId, Map<Object, Object> stats) {
        int totalViews = 0;
        int totalTwice = 0;
        int totalLikes = 0;
        int totalComments = 0;

        String userPrefix = userId + ":";

        for (Map.Entry<Object, Object> entry : stats.entrySet()) {
            String field = (String) entry.getKey();
            Integer count = Integer.parseInt((String) entry.getValue());

            if (!field.startsWith(userPrefix)) continue;

            String statType = field.substring(userPrefix.length());
            switch (statType) {
                case RedisStatsConstants.STAT_TYPE_VIEW:
                    totalViews += count;
                    break;
                case RedisStatsConstants.STAT_TYPE_TWICE:
                    totalTwice += count;
                    break;
                case RedisStatsConstants.STAT_TYPE_LIKE:
                    totalLikes += count;
                    break;
                case RedisStatsConstants.STAT_TYPE_COMMENT:
                    totalComments += count;
                    break;
            }
        }

        return com.prosper.learn.analytics.dto.UserDailyStatsDTO.builder()
            .userId(userId)
            .viewCount(totalViews)
            .twiceCount(totalTwice)
            .likeCount(totalLikes)
            .commentCount(totalComments)
            .build();
    }

    /**
     * 解析 Redis 返回值为整数
     */
    private int parseRedisValue(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.debug("解析Redis值失败: value={}", value);
            return 0;
        }
    }

    /**
     * 从 Redis Hash 中获取整数字段值
     */
    private int getRedisHashFieldAsInt(String key, String field) {
        try {
            Object value = redisTemplate.opsForHash().get(key, field);
            if (value != null) {
                return Integer.parseInt(value.toString());
            }
        } catch (Exception e) {
            log.debug("获取 Redis 字段失败: key={}, field={}, error={}", key, field, e.getMessage());
        }
        return 0;
    }

    /**
     * 生成日期键（月-日格式）
     */
    private String generateDayKey(LocalDate date) {
        return date.getMonthValue() + "-" + date.getDayOfMonth();
    }
}