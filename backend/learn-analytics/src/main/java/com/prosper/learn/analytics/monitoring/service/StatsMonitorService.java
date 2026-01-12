package com.prosper.learn.analytics.monitoring.service;

import com.prosper.learn.analytics.stats.service.DailyStatsService;
import com.prosper.learn.shared.common.util.TimeZoneUtil;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsMonitorService {

    private final RedisTemplate<String, String> redisTemplate;
    private final DailyStatsService dailyStatsService;
    private final SystemProperties systemProperties;

    // 不变常量 - Redis键名相关
    private static final String STATS_KEY_PREFIX = "stats:";
    private static final String USER_STATS_SUFFIX = ":user";
    private static final String POST_STATS_SUFFIX = ":post";
    private static final String HEALTH_CHECK_KEY = "health_check";

    private volatile boolean redisHealthy = true;

    // ========== 私有辅助方法 ==========

    /**
     * 生成用户统计Redis键名
     */
    private String generateUserStatsKey(String dateStr) {
        return STATS_KEY_PREFIX + dateStr + USER_STATS_SUFFIX;
    }

    /**
     * 生成文章统计Redis键名
     */
    private String generatePostStatsKey(String dateStr) {
        return STATS_KEY_PREFIX + dateStr + POST_STATS_SUFFIX;
    }

    /**
     * 检查Redis连接健康状态
     */
    private void checkRedisConnection() {
        try {
            redisTemplate.opsForValue().get(HEALTH_CHECK_KEY);
        } catch (Exception e) {
            throw StatusCode.REDIS_CONNECTION_ERROR.exception(e);
        }
    }

    /**
     * 获取待同步数据数量
     */
    private int getPendingDataCount() {
        try {
            return redisTemplate.keys(STATS_KEY_PREFIX + "*").size();
        } catch (Exception e) {
            log.debug("无法检查待同步数据量", e);
            return 0;
        }
    }

    /**
     * 验证日期有效性
     */
    private void validateDate(LocalDate date) {
        if (date == null) {
            throw StatusCode.INVALID_DATE.exception();
        }
        if (date.isAfter(TimeZoneUtil.now())) {
            throw StatusCode.INVALID_DATE.exception();
        }
    }

    /**
     * 检查待同步数据是否超过阈值
     */
    private void checkPendingDataThreshold(int pendingCount) {
        int threshold = systemProperties.getStatsMonitor().getPendingDataThreshold();
        if (pendingCount > threshold) {
            log.warn("Redis中待同步的统计数据过多: {} 个key，阈值: {}", pendingCount, threshold);
        }
    }

    // ========== 定时任务方法 ==========

    /**
     * 每分钟检查Redis健康状态
     */
    @Scheduled(fixedRate = 60000)
    public void checkRedisHealth() {
        if (!systemProperties.getStatsMonitor().isEnableHealthMonitor()) {
            return;
        }
        
        try {
            checkRedisConnection();
            if (!redisHealthy) {
                log.info("Redis连接恢复正常");
                redisHealthy = true;
            }
        } catch (Exception e) {
            if (redisHealthy) {
                log.warn("Redis连接异常", e);
                redisHealthy = false;
            }
        }
    }

    /**
     * 每小时检查Redis内存使用情况
     */
    @Scheduled(fixedRate = 3600000)
    public void monitorRedisMemory() {
        if (!systemProperties.getStatsMonitor().isEnableMemoryMonitor()) {
            return;
        }
        
        try {
            String memoryInfo = redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .info("memory")
                .getProperty("used_memory_human");
                
            log.debug("Redis内存使用情况: {}", memoryInfo);
            
            int pendingDataCount = getPendingDataCount();
            checkPendingDataThreshold(pendingDataCount);
            
        } catch (Exception e) {
            log.error("监控Redis内存失败", e);
            throw StatusCode.REDIS_OPERATION_ERROR.exception(e);
        }
    }

    /**
     * 每天检查同步状态
     */
    @Scheduled(cron = "0 0 8 * * ?") // 每天早上8点
    public void checkSyncStatus() {
        if (!systemProperties.getStatsMonitor().isEnableSyncStatusCheck()) {
            return;
        }

        try {
            LocalDate yesterday = TimeZoneUtil.yesterday();
            validateDate(yesterday);
            
            String yesterdayStr = yesterday.toString();
            String userKey = generateUserStatsKey(yesterdayStr);
            String postKey = generatePostStatsKey(yesterdayStr);
            
            boolean hasUserData = redisTemplate.hasKey(userKey);
            boolean hasPostData = redisTemplate.hasKey(postKey);
            
            if (hasUserData || hasPostData) {
                log.warn("发现昨天({})的数据未同步完成，用户数据存在:{}, 文章数据存在:{}", 
                    yesterdayStr, hasUserData, hasPostData);
            } else {
                log.info("昨天({})的数据同步正常", yesterdayStr);
            }
            
        } catch (Exception e) {
            log.error("检查同步状态失败", e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 应用关闭时强制同步剩余数据
     */
    @EventListener(ContextClosedEvent.class)
    public void onShutdown(ContextClosedEvent event) {
        if (!systemProperties.getStatsMonitor().isEnableShutdownSync()) {
            return;
        }
        
        log.info("应用关闭，开始同步Redis剩余数据");
        try {
            int pendingCount = getPendingDataCount();
            if (pendingCount > 0) {
                log.warn("发现{}个未同步的统计数据，开始强制同步", pendingCount);

                LocalDate today = TimeZoneUtil.now();
                validateDate(today);
                String result = dailyStatsService.syncSpecificDate(today);
                log.info("强制同步结果: {}", result);
            } else {
                log.info("没有未同步的数据");
            }
        } catch (Exception e) {
            log.error("关闭时同步数据失败", e);
        }
    }

    /**
     * 获取Redis健康状态
     */
    public boolean isRedisHealthy() {
        return redisHealthy;
    }

    /**
     * 获取系统状态信息
     */
    public String getSystemStatus() {
        try {
            StringBuilder status = new StringBuilder();
            status.append("Redis状态: ").append(redisHealthy ? "正常" : "异常").append("\n");
            
            int pendingCount = getPendingDataCount();
            status.append("待同步数据: ").append(pendingCount).append(" 个key\n");
            
            return status.toString();
        } catch (Exception e) {
            log.error("获取系统状态失败", e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }
}