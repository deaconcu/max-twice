package com.prosper.learn.infrastructure.image;

import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 图片上传配额服务
 * 使用Redis Lua脚本实现原子性的配额检查和记录，避免TOCTOU竞态条件
 *
 * <p><b>配额设计说明：</b>
 * <ul>
 *   <li>最小间隔：防止恶意刷接口，设为0表示不限制</li>
 *   <li>每分钟限制：适应富文本编辑器批量上传场景（建议 ≥ 30）</li>
 *   <li>每小时限制：防止长时间高频上传（建议 = 分钟限制 × 5）</li>
 *   <li>每天限制：防止存储滥用（建议 = 小时限制 × 2）</li>
 * </ul>
 *
 * <p><b>配置示例：</b>
 * <pre>
 * # 普通用户配额（推荐）
 * upload.quota.min-interval=0
 * upload.quota.minute-limit=30
 * upload.quota.hour-limit=150
 * upload.quota.daily-limit=300
 *
 * # 严格限制（防滥用）
 * upload.quota.min-interval=1
 * upload.quota.minute-limit=20
 * upload.quota.hour-limit=100
 * upload.quota.daily-limit=200
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageQuotaService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${upload.quota.min-interval:0}")
    private int minInterval; // 最小上传间隔（秒），0表示不限制

    @Value("${upload.quota.minute-limit:30}")
    private int minuteLimit; // 每分钟限制

    @Value("${upload.quota.hour-limit:150}")
    private int hourLimit; // 每小时限制

    @Value("${upload.quota.daily-limit:300}")
    private int dailyLimit; // 每天限制

    private static final String KEY_PREFIX_INTERVAL = "upload:interval:";
    private static final String KEY_PREFIX_MINUTE = "upload:minute:";
    private static final String KEY_PREFIX_HOUR = "upload:hour:";
    private static final String KEY_PREFIX_DAILY = "upload:daily:";

    /**
     * 原子性地检查配额并记录上传
     * 使用 Redis Lua 脚本确保检查和记录是原子操作，避免 TOCTOU 竞态条件
     *
     * @param userId 用户ID
     * @throws BusinessException 超过配额时抛出异常
     */
    public void checkAndRecordQuota(Long userId) {
        // Lua 脚本：原子性地检查并记录
        String luaScript = """
                local intervalKey = KEYS[1]
                local minuteKey = KEYS[2]
                local hourKey = KEYS[3]
                local dailyKey = KEYS[4]

                local minInterval = tonumber(ARGV[1])
                local minuteLimit = tonumber(ARGV[2])
                local hourLimit = tonumber(ARGV[3])
                local dailyLimit = tonumber(ARGV[4])

                -- 1. 检查最小间隔（如果 minInterval > 0）
                if minInterval > 0 and redis.call('exists', intervalKey) == 1 then
                    local ttl = redis.call('ttl', intervalKey)
                    return {'false', 'interval', ttl}
                end

                -- 2. 检查分钟限制
                local minuteCount = tonumber(redis.call('get', minuteKey) or '0')
                if minuteCount >= minuteLimit then
                    return {'false', 'minute', minuteCount}
                end

                -- 3. 检查小时限制
                local hourCount = tonumber(redis.call('get', hourKey) or '0')
                if hourCount >= hourLimit then
                    return {'false', 'hour', hourCount}
                end

                -- 4. 检查每天限制
                local dailyCount = tonumber(redis.call('get', dailyKey) or '0')
                if dailyCount >= dailyLimit then
                    return {'false', 'daily', dailyCount}
                end

                -- 所有检查通过，记录上传
                -- 设置最小间隔锁（如果 minInterval > 0）
                if minInterval > 0 then
                    redis.call('setex', intervalKey, minInterval, '1')
                end

                -- 增加分钟计数
                local newMinuteCount = redis.call('incr', minuteKey)
                if newMinuteCount == 1 then
                    redis.call('expire', minuteKey, 60)
                end

                -- 增加小时计数
                local newHourCount = redis.call('incr', hourKey)
                if newHourCount == 1 then
                    redis.call('expire', hourKey, 3600)
                end

                -- 增加每天计数
                local newDailyCount = redis.call('incr', dailyKey)
                if newDailyCount == 1 then
                    redis.call('expire', dailyKey, 86400)
                end

                return {'true', newMinuteCount, newHourCount, newDailyCount}
                """;

        // 准备参数
        List<String> keys = Arrays.asList(
                KEY_PREFIX_INTERVAL + userId,
                KEY_PREFIX_MINUTE + userId + ":" + getCurrentMinute(),
                KEY_PREFIX_HOUR + userId + ":" + getCurrentHour(),
                KEY_PREFIX_DAILY + userId + ":" + getCurrentDate()
        );

        // 将配额参数转换为字符串（RedisTemplate<String, String> 要求所有参数都是String）
        Object[] args = {
                String.valueOf(minInterval),
                String.valueOf(minuteLimit),
                String.valueOf(hourLimit),
                String.valueOf(dailyLimit)
        };

        // 执行 Lua 脚本
        DefaultRedisScript<List> script = new DefaultRedisScript<>(luaScript, List.class);
        List<Object> result = redisTemplate.execute(script, keys, args);

        if (result == null || result.isEmpty()) {
            log.error("Lua脚本执行失败，返回结果为空");
            throw StatusCode.SYSTEM_ERROR.exception("配额检查失败");
        }

        // 解析结果
        String success = result.get(0).toString();
        if ("false".equals(success)) {
            String limitType = result.get(1).toString();
            Object limitValue = result.get(2);

            switch (limitType) {
                case "interval" -> throw StatusCode.UPLOAD_TOO_FREQUENT
                        .exception("上传过于频繁，请" + limitValue + "秒后重试");
                case "minute" -> throw StatusCode.UPLOAD_QUOTA_EXCEEDED
                        .exception("每分钟最多上传" + minuteLimit + "张图片");
                case "hour" -> throw StatusCode.UPLOAD_QUOTA_EXCEEDED
                        .exception("每小时最多上传" + hourLimit + "张图片");
                case "daily" -> throw StatusCode.UPLOAD_QUOTA_EXCEEDED
                        .exception("每天最多上传" + dailyLimit + "张图片");
                default -> throw StatusCode.SYSTEM_ERROR.exception("未知的限制类型");
            }
        }

        // 记录日志
        log.info("用户{}上传配额记录成功，当前计数: 分钟={}, 小时={}, 每天={}",
                userId, result.get(1), result.get(2), result.get(3));
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
