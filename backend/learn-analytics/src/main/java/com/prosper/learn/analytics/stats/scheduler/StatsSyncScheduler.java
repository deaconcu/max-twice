package com.prosper.learn.analytics.stats.scheduler;

import com.prosper.learn.analytics.stats.service.ContentStatsSyncService;
import com.prosper.learn.analytics.stats.service.UserStatsSyncService;
import com.prosper.learn.analytics.stats.service.UserLearningSyncService;
import com.prosper.learn.shared.common.util.TimeZoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 统计数据同步调度器
 *
 * 负责定时将统计数据同步到数据库（Redis 统计数据 + daily 学习数据）
 * 实际的定时触发在 Scheduler.java 中配置
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsSyncScheduler {

    private final UserStatsSyncService userStatsSyncService;
    private final ContentStatsSyncService contentStatsSyncService;
    private final UserLearningSyncService userLearningSyncService;

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

            // 同步学习统计（热力图数据）
            int learningStatsCount = userLearningSyncService.syncLearningStats(date);

            log.info("同步{}的数据完成: 用户统计{}条, 内容统计{}条, 学习统计{}条",
                dateStr, userStatsCount, contentStatsCount, learningStatsCount);

        } catch (Exception e) {
            log.error("同步{}的数据失败", dateStr, e);
            throw e;
        }
    }
}
