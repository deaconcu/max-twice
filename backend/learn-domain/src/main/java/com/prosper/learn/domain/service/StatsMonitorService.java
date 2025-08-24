package com.prosper.learn.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class StatsMonitorService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private DailyStatsService dailyStatsService;

    private volatile boolean redisHealthy = true;

    /**
     * 每分钟检查Redis健康状态
     */
    @Scheduled(fixedRate = 60000)
    public void checkRedisHealth() {
        try {
            redisTemplate.opsForValue().get("health_check");
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
        try {
            // 检查Redis内存使用情况
            String memoryInfo = redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .info("memory")
                .getProperty("used_memory_human");
                
            log.debug("Redis内存使用情况: {}", memoryInfo);
            
            // 检查待同步的数据量
            int pendingDataCount = 0;
            try {
                pendingDataCount = redisTemplate.keys("stats:*").size();
                if (pendingDataCount > 1000) {
                    log.warn("Redis中待同步的统计数据过多: {} 个key", pendingDataCount);
                }
            } catch (Exception e) {
                log.debug("无法检查待同步数据量", e);
            }
            
        } catch (Exception e) {
            log.error("监控Redis内存失败", e);
        }
    }

    /**
     * 每天检查同步状态
     */
    @Scheduled(cron = "0 0 8 * * ?") // 每天早上8点
    public void checkSyncStatus() {
        try {
            // 检查昨天的数据是否已同步
            String yesterday = java.time.LocalDate.now().minusDays(1).toString();
            String userKey = "stats:" + yesterday + ":user";
            String postKey = "stats:" + yesterday + ":post";
            
            boolean hasUserData = redisTemplate.hasKey(userKey);
            boolean hasPostData = redisTemplate.hasKey(postKey);
            
            if (hasUserData || hasPostData) {
                log.warn("发现昨天({})的数据未同步完成，用户数据存在:{}, 文章数据存在:{}", 
                    yesterday, hasUserData, hasPostData);
            } else {
                log.info("昨天({})的数据同步正常", yesterday);
            }
            
        } catch (Exception e) {
            log.error("检查同步状态失败", e);
        }
    }

    /**
     * 应用关闭时强制同步剩余数据
     */
    @EventListener(ContextClosedEvent.class)
    public void onShutdown(ContextClosedEvent event) {
        log.info("应用关闭，开始同步Redis剩余数据");
        try {
            // 检查是否有未同步的数据
            int pendingCount = redisTemplate.keys("stats:*").size();
            if (pendingCount > 0) {
                log.warn("发现{}个未同步的统计数据，开始强制同步", pendingCount);
                
                // 触发今天的数据同步
                String result = dailyStatsService.syncSpecificDate(LocalDate.now());
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
            
            int pendingCount = redisTemplate.keys("stats:*").size();
            status.append("待同步数据: ").append(pendingCount).append(" 个key\n");
            
            return status.toString();
        } catch (Exception e) {
            return "获取状态失败: " + e.getMessage();
        }
    }
}