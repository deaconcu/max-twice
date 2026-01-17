package com.prosper.learn.analytics.stats.scheduler;

import com.prosper.learn.analytics.stats.service.ContentStatsSyncService;
import com.prosper.learn.analytics.stats.service.UserStatsSyncService;
import com.prosper.learn.shared.common.util.TimeZoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 统计数据同步调度器
 *
 * 负责定时将 Redis 中的统计数据同步到数据库
 * 实际的定时触发在 Scheduler.java 中配置
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsSyncScheduler {

    private final UserStatsSyncService userStatsSyncService;
    private final ContentStatsSyncService contentStatsSyncService;

    /**
     * 同步昨天的统计数据（由 Scheduler 定时调用）
     */
    public void syncYesterdayStats() {
        syncStatsForDate(TimeZoneUtil.yesterday());
    }

    /**
     * 同步指定日期的统计数据（供手动调用）
     *
     * @param date 要同步的日期
     */
    public void syncStatsForDate(LocalDate date) {
        String dateStr = date.toString();

        log.info("开始同步{}的统计数据", dateStr);

        try {
            // 同步用户统计
            int userStatsCount = userStatsSyncService.syncUserStats(dateStr);

            // 同步内容统计
            int contentStatsCount = contentStatsSyncService.syncPostStats(dateStr);

            log.info("同步{}的数据完成: 用户统计{}条, 内容统计{}条", dateStr, userStatsCount, contentStatsCount);

        } catch (Exception e) {
            log.error("同步{}的数据失败", dateStr, e);
            throw e;
        }
    }

    /**
     * 检查今天是否有遗留的Redis数据需要同步（由 Scheduler 定时调用）
     * 正常情况下今天的数据应该在Redis中，只有系统异常时才会需要同步
     */
    public void checkTodayRedisData() {
        LocalDate today = TimeZoneUtil.now();
        String dateStr = today.toString();

        log.info("检查{}是否有遗留的Redis数据需要同步", dateStr);

        try {
            // 同步用户统计（如果有数据）
            int userStatsCount = userStatsSyncService.syncUserStats(dateStr);

            // 同步内容统计（如果有数据）
            int contentStatsCount = contentStatsSyncService.syncPostStats(dateStr);

            if (userStatsCount > 0 || contentStatsCount > 0) {
                log.warn("同步{}的遗留数据: 用户统计{}条, 内容统计{}条（可能存在系统异常）",
                    dateStr, userStatsCount, contentStatsCount);
            } else {
                log.info("{}没有遗留的Redis数据", dateStr);
            }

        } catch (Exception e) {
            log.error("同步{}的遗留数据失败", dateStr, e);
        }
    }
}
