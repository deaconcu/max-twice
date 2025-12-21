package com.prosper.learn.infrastructure.image;

import com.prosper.learn.shared.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * 图片上传配额服务
 * 使用Redis实现高性能的频率限制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageQuotaService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${upload.quota.min-interval:1}")
    private int minInterval; // 最小上传间隔（秒）

    @Value("${upload.quota.minute-limit:20}")
    private int minuteLimit; // 每分钟限制

    @Value("${upload.quota.hour-limit:100}")
    private int hourLimit; // 每小时限制

    @Value("${upload.quota.daily-limit:200}")
    private int dailyLimit; // 每天限制

    private static final String KEY_PREFIX_INTERVAL = "upload:interval:";
    private static final String KEY_PREFIX_MINUTE = "upload:minute:";
    private static final String KEY_PREFIX_HOUR = "upload:hour:";
    private static final String KEY_PREFIX_DAILY = "upload:daily:";

    /**
     * 检查用户是否可以上传
     * 检查所有限制条件
     *
     * @param userId 用户ID
     * @throws BusinessException 超过配额时抛出异常
     */
    public void checkQuota(Long userId) {
        // 1. 检查最小间隔
        String intervalKey = KEY_PREFIX_INTERVAL + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(intervalKey))) {
            Long ttl = redisTemplate.getExpire(intervalKey, TimeUnit.SECONDS);
            throw ErrorCode.UPLOAD_TOO_FREQUENT.exception("上传过于频繁，请" + ttl + "秒后重试");
        }

        // 2. 检查每分钟限制
        String minuteKey = KEY_PREFIX_MINUTE + userId + ":" + getCurrentMinute();
        Integer minuteCount = getCount(minuteKey);
        if (minuteCount != null && minuteCount >= minuteLimit) {
            throw ErrorCode.UPLOAD_QUOTA_EXCEEDED.exception("每分钟最多上传" + minuteLimit + "张图片");
        }

        // 3. 检查每小时限制
        String hourKey = KEY_PREFIX_HOUR + userId + ":" + getCurrentHour();
        Integer hourCount = getCount(hourKey);
        if (hourCount != null && hourCount >= hourLimit) {
            throw ErrorCode.UPLOAD_QUOTA_EXCEEDED.exception("每小时最多上传" + hourLimit + "张图片");
        }

        // 4. 检查每天限制
        String dailyKey = KEY_PREFIX_DAILY + userId + ":" + getCurrentDate();
        Integer dailyCount = getCount(dailyKey);
        if (dailyCount != null && dailyCount >= dailyLimit) {
            throw ErrorCode.UPLOAD_QUOTA_EXCEEDED.exception("每天最多上传" + dailyLimit + "张图片");
        }

        log.debug("用户{}配额检查通过: 分钟{}/{}, 小时{}/{}, 每天{}/{}",
                userId,
                minuteCount == null ? 0 : minuteCount, minuteLimit,
                hourCount == null ? 0 : hourCount, hourLimit,
                dailyCount == null ? 0 : dailyCount, dailyLimit);
    }

    /**
     * 记录上传
     * 更新所有维度的计数器
     *
     * @param userId 用户ID
     */
    public void recordUpload(Long userId) {
        // 1. 设置最小间隔锁
        String intervalKey = KEY_PREFIX_INTERVAL + userId;
        redisTemplate.opsForValue().set(intervalKey, "1", Duration.ofSeconds(minInterval));

        // 2. 增加分钟计数
        String minuteKey = KEY_PREFIX_MINUTE + userId + ":" + getCurrentMinute();
        increment(minuteKey, 60); // 60秒过期

        // 3. 增加小时计数
        String hourKey = KEY_PREFIX_HOUR + userId + ":" + getCurrentHour();
        increment(hourKey, 3600); // 1小时过期

        // 4. 增加每天计数
        String dailyKey = KEY_PREFIX_DAILY + userId + ":" + getCurrentDate();
        increment(dailyKey, 86400); // 24小时过期

        log.info("记录用户{}上传", userId);
    }

    /**
     * 获取计数
     */
    private Integer getCount(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? null : Integer.parseInt(value);
    }

    /**
     * 增加计数并设置过期时间
     */
    private void increment(String key, long expireSeconds) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 首次创建时设置过期时间
            redisTemplate.expire(key, Duration.ofSeconds(expireSeconds));
        }
    }

    /**
     * 获取当前分钟标识: yyyyMMddHHmm
     */
    private String getCurrentMinute() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%04d%02d%02d%02d%02d",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                now.getHour(), now.getMinute());
    }

    /**
     * 获取当前小时标识: yyyyMMddHH
     */
    private String getCurrentHour() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%04d%02d%02d%02d",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                now.getHour());
    }

    /**
     * 获取当前日期标识: yyyyMMdd
     */
    private String getCurrentDate() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%04d%02d%02d",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth());
    }

    /**
     * 获取用户当前配额使用情况
     *
     * @param userId 用户ID
     * @return 配额使用信息
     */
    public QuotaUsage getQuotaUsage(Long userId) {
        String minuteKey = KEY_PREFIX_MINUTE + userId + ":" + getCurrentMinute();
        String hourKey = KEY_PREFIX_HOUR + userId + ":" + getCurrentHour();
        String dailyKey = KEY_PREFIX_DAILY + userId + ":" + getCurrentDate();

        Integer minuteUsed = getCount(minuteKey);
        Integer hourUsed = getCount(hourKey);
        Integer dailyUsed = getCount(dailyKey);

        return new QuotaUsage(
                minuteUsed == null ? 0 : minuteUsed, minuteLimit,
                hourUsed == null ? 0 : hourUsed, hourLimit,
                dailyUsed == null ? 0 : dailyUsed, dailyLimit
        );
    }

    /**
     * 配额使用情况
     */
    public static class QuotaUsage {
        public final int minuteUsed;
        public final int minuteLimit;
        public final int hourUsed;
        public final int hourLimit;
        public final int dailyUsed;
        public final int dailyLimit;

        public QuotaUsage(int minuteUsed, int minuteLimit,
                          int hourUsed, int hourLimit,
                          int dailyUsed, int dailyLimit) {
            this.minuteUsed = minuteUsed;
            this.minuteLimit = minuteLimit;
            this.hourUsed = hourUsed;
            this.hourLimit = hourLimit;
            this.dailyUsed = dailyUsed;
            this.dailyLimit = dailyLimit;
        }
    }
}
