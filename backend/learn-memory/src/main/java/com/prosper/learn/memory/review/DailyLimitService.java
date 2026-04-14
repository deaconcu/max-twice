package com.prosper.learn.memory.review;

import com.prosper.learn.infrastructure.redis.RedisKeyPrefix;
import com.prosper.learn.shared.common.util.TimeZoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * 每日学习限制服务
 *
 * 使用 Redis 管理每日新卡/复习卡计数
 * Key 设计：{lang}:memory:daily:{userId}:{courseId}:{type}:{date}
 * TTL：到当天 24:00 过期
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "memory:daily:";
    private static final String TYPE_NEW = "new";
    private static final String TYPE_REVIEW = "review";

    /**
     * 生成 Redis key（带语言前缀）
     */
    private String getKey(Long userId, Long courseId, String type, LocalDate userToday) {
        String date = userToday.toString();
        return RedisKeyPrefix.prefix(KEY_PREFIX + userId + ":" + courseId + ":" + type + ":" + date);
    }

    /**
     * 计算到用户时区当天 24:00 的剩余秒数
     *
     * @param userToday 用户时区的今天日期
     * @param userTimezone 用户时区字符串
     */
    private Duration getTtlUntilMidnight(LocalDate userToday, String userTimezone) {
        // 用户时区的明天 0 点
        ZonedDateTime midnight = userToday.plusDays(1).atStartOfDay(TimeZoneUtil.getZoneId(userTimezone));
        // 当前时刻
        Instant now = Instant.now();
        long seconds = Duration.between(now, midnight.toInstant()).getSeconds();
        // 至少 1 秒，防止边界情况
        return Duration.ofSeconds(Math.max(1, seconds));
    }

    /**
     * 获取今日新卡计数
     */
    public int getTodayNewCount(Long userId, Long courseId, LocalDate userToday) {
        return getCount(userId, courseId, TYPE_NEW, userToday);
    }

    /**
     * 获取今日复习计数
     */
    public int getTodayReviewCount(Long userId, Long courseId, LocalDate userToday) {
        return getCount(userId, courseId, TYPE_REVIEW, userToday);
    }

    /**
     * 增加今日新卡计数
     */
    public int incrementNewCount(Long userId, Long courseId, LocalDate userToday, String userTimezone) {
        return incrementCount(userId, courseId, TYPE_NEW, userToday, userTimezone);
    }

    /**
     * 增加今日复习计数
     */
    public int incrementReviewCount(Long userId, Long courseId, LocalDate userToday, String userTimezone) {
        return incrementCount(userId, courseId, TYPE_REVIEW, userToday, userTimezone);
    }

    /**
     * 获取计数
     */
    private int getCount(Long userId, Long courseId, String type, LocalDate userToday) {
        if (userId == null || courseId == null) {
            return 0;
        }

        String key = getKey(userId, courseId, type, userToday);
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return 0;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            log.warn("每日限额 获取计数失败: userId={}，courseId={}，type={}，error={}",
                    userId, courseId, type, e.getMessage());
            return 0;
        }
    }

    /**
     * 增加计数
     */
    private int incrementCount(Long userId, Long courseId, String type, LocalDate userToday, String userTimezone) {
        if (userId == null || courseId == null) {
            return 0;
        }

        String key = getKey(userId, courseId, type, userToday);
        try {
            Long newValue = redisTemplate.opsForValue().increment(key);
            if (newValue != null && newValue == 1) {
                // 首次设置，添加过期时间
                redisTemplate.expire(key, getTtlUntilMidnight(userToday, userTimezone));
            }
            return newValue != null ? newValue.intValue() : 0;
        } catch (Exception e) {
            log.warn("每日限额 增加计数失败: userId={}，courseId={}，type={}，error={}",
                    userId, courseId, type, e.getMessage());
            return 0;
        }
    }
}
