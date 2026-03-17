package com.prosper.learn.memory.review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 每日学习限制服务
 *
 * 使用 Redis 管理每日新卡/复习卡计数
 * Key 设计：memory:daily:{userId}:{courseId}:{type}:{date}
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
     * 生成 Redis key
     */
    private String getKey(Long userId, Long courseId, String type) {
        String date = LocalDate.now().toString();
        return KEY_PREFIX + userId + ":" + courseId + ":" + type + ":" + date;
    }

    /**
     * 计算到当天 24:00 的剩余秒数
     */
    private Duration getTtlUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT);
        long seconds = Duration.between(now, midnight).getSeconds();
        // 至少 1 秒，防止边界情况
        return Duration.ofSeconds(Math.max(1, seconds));
    }

    /**
     * 获取今日新卡计数
     */
    public int getTodayNewCount(Long userId, Long courseId) {
        return getCount(userId, courseId, TYPE_NEW);
    }

    /**
     * 获取今日复习计数
     */
    public int getTodayReviewCount(Long userId, Long courseId) {
        return getCount(userId, courseId, TYPE_REVIEW);
    }

    /**
     * 增加今日新卡计数
     */
    public int incrementNewCount(Long userId, Long courseId) {
        return incrementCount(userId, courseId, TYPE_NEW);
    }

    /**
     * 增加今日复习计数
     */
    public int incrementReviewCount(Long userId, Long courseId) {
        return incrementCount(userId, courseId, TYPE_REVIEW);
    }

    /**
     * 获取计数
     */
    private int getCount(Long userId, Long courseId, String type) {
        if (userId == null || courseId == null) {
            return 0;
        }

        String key = getKey(userId, courseId, type);
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
            log.warn("Failed to get daily count for user {} course {} type {}: {}",
                    userId, courseId, type, e.getMessage());
            return 0;
        }
    }

    /**
     * 增加计数
     */
    private int incrementCount(Long userId, Long courseId, String type) {
        if (userId == null || courseId == null) {
            return 0;
        }

        String key = getKey(userId, courseId, type);
        try {
            Long newValue = redisTemplate.opsForValue().increment(key);
            if (newValue != null && newValue == 1) {
                // 首次设置，添加过期时间
                redisTemplate.expire(key, getTtlUntilMidnight());
            }
            return newValue != null ? newValue.intValue() : 0;
        } catch (Exception e) {
            log.warn("Failed to increment daily count for user {} course {} type {}: {}",
                    userId, courseId, type, e.getMessage());
            return 0;
        }
    }
}
