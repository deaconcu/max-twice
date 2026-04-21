package com.twicemax.web.application;

import com.twicemax.analytics.stats.scheduler.StatsSyncScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 统一调度器 - 负责所有定时任务的配置
 * 所有定时任务都在这里统一管理，实际业务逻辑委托给对应的 Scheduler
 */
@Component
@RequiredArgsConstructor
public class Scheduler {

    private final StatsSyncScheduler statsSyncScheduler;

    /**
     * 每天凌晨2:30同步昨天的统计数据
     */
    @Scheduled(cron = "0 30 2 * * ?")
    public void syncYesterdayStats() {
        statsSyncScheduler.syncYesterdayStats();
    }
}