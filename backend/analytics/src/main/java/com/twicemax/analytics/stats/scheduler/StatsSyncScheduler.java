package com.twicemax.analytics.stats.scheduler;

import com.twicemax.analytics.stats.service.ContentStatsSyncService;
import com.twicemax.analytics.stats.service.UserStatsSyncService;
import com.twicemax.analytics.stats.service.UserLearningSyncService;
import com.twicemax.infrastructure.datasource.DataSourceContextHolder;
import com.twicemax.shared.common.util.TimeZoneUtil;
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
     * 为每种语言执行一次
     */
    public void syncYesterdayStats() {
        DataSourceContextHolder.forEachLanguage(lang -> {
            try {
                syncStatsForDateInternal(TimeZoneUtil.yesterday(), lang);
            } catch (Exception e) {
                log.error("[{}] 同步昨天统计数据失败", lang, e);
            }
        });
    }

    /**
     * 同步指定日期的统计数据（供手动调用）
     * 为每种语言执行一次
     *
     * @param date 要同步的日期
     */
    public void syncStatsForDate(LocalDate date) {
        DataSourceContextHolder.forEachLanguage(lang -> {
            try {
                syncStatsForDateInternal(date, lang);
            } catch (Exception e) {
                log.error("[{}] 同步{}统计数据失败", lang, date, e);
            }
        });
    }

    /**
     * 内部同步方法（单语言）
     */
    private void syncStatsForDateInternal(LocalDate date, String lang) {
        String dateStr = date.toString();

        log.info("[{}] 开始同步{}的统计数据", lang, dateStr);

        try {
            // 同步用户统计
            int userStatsCount = userStatsSyncService.syncUserStats(dateStr);

            // 同步内容统计
            int contentStatsCount = contentStatsSyncService.syncPostStats(dateStr);

            // 同步学习统计（热力图数据）
            int learningStatsCount = userLearningSyncService.syncLearningStats(date);

            log.info("[{}] 同步{}的数据完成: 用户统计{}条, 内容统计{}条, 学习统计{}条",
                lang, dateStr, userStatsCount, contentStatsCount, learningStatsCount);

        } catch (Exception e) {
            log.error("[{}] 同步{}的数据失败", lang, dateStr, e);
            throw e;
        }
    }
}
