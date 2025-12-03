package com.prosper.learn.business.service.scheduler;

import com.prosper.learn.business.service.UserStatsService;
import com.prosper.learn.business.service.data.UserStatsDataService;
import com.prosper.learn.persistence.dataobject.UserStatsDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户统计定时任务服务
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserStatsSchedulerService {

    private final UserStatsDataService userStatsDataService;

    /**
     * 兜底清理任务 - 每日1:30执行
     * 处理超过1天的老数据，确保数据最终一致性
     */
    @Scheduled(cron = "0 30 1 * * ?")
    @Transactional
    public void cleanupStaleStats() {
        try {
            log.info("开始执行兜底同步任务");

            // 处理超过1天的老数据
            LocalDate today = LocalDate.now();
            List<UserStatsDO> staleStats = userStatsDataService.getStatsOlderThan(today, 1);

            if (staleStats.isEmpty()) {
                log.info("没有发现待同步数据");
                return;
            }

            log.info("发现{}条待同步数据，开始兜底同步", staleStats.size());

            int successCount = 0;
            int failCount = 0;

            for (UserStatsDO stats : staleStats) {
                try {
                    syncToYearlyAndDelete(stats);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("同步失败，userId: {}, date: {}",
                            stats.getUserId(), stats.getDailyStatDate(), e);
                    // 继续处理下一条，不因单条失败而中断
                }
            }

            log.info("兜底同步完成，成功: {}, 失败: {}", successCount, failCount);

        } catch (Exception e) {
            log.error("兜底同步任务执行失败", e);
        }
    }

    /**
     * 系统健康检查 - 每小时执行
     * 检查是否有堆积的待同步数据
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void healthCheck() {
        try {
            LocalDate today = LocalDate.now();
            List<UserStatsDO> staleStats = userStatsDataService.getStatsOlderThan(today, 0);

            if (staleStats.size() > 1000) {
                log.warn("发现大量待同步数据: {} 条，可能存在同步问题", staleStats.size());
            } else if (staleStats.size() > 100) {
                log.info("发现待同步数据: {} 条", staleStats.size());
            }

        } catch (Exception e) {
            log.error("健康检查失败", e);
        }
    }

    /**
     * 同步到yearly表并删除
     */
    private void syncToYearlyAndDelete(UserStatsDO stats) {
        // 1. 提取日度增量数据，转换为数组格式
        int[] dailyStats = extractDailyStatsArray(stats);

        // 2. 更新yearly表的JSON（数组格式）
        // TODO: 调用 userStatsYearlyDataService.updateYearlyStatsArray

        // 3. 删除已同步的数据
        userStatsDataService.deleteById(stats.getId());

        log.debug("同步完成，userId: {}, date: {}", stats.getUserId(), stats.getDailyStatDate());
    }

    /**
     * 提取日度统计数据为数组
     */
    private int[] extractDailyStatsArray(UserStatsDO stats) {
        // 转换为固定顺序的数组：[views, twice, helpful, comments]
        return new int[] {
            stats.getDailyViews() != null ? stats.getDailyViews() : 0,
            stats.getDailyTwice() != null ? stats.getDailyTwice() : 0,
            stats.getDailyHelpful() != null ? stats.getDailyHelpful() : 0,
            stats.getDailyComments() != null ? stats.getDailyComments() : 0
        };
    }
}