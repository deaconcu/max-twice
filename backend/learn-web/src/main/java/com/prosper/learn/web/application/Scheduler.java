package com.prosper.learn.web.application;

import com.prosper.learn.analytics.ranking.scheduler.CourseRankingScheduler;
import com.prosper.learn.analytics.stats.service.DailyStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 统一调度器 - 负责所有定时任务的配置
 */
@Component
public class Scheduler {
    
    @Autowired
    private CourseRankingScheduler courseRankingScheduler;
    
    @Autowired
    private DailyStatsService dailyStatsService;
    
    /**
     * 每小时同步一次课程统计数据到Redis
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void syncCourseStats() {
        courseRankingScheduler.syncCourseStats();
    }
    
    /**
     * 应用启动时执行课程统计数据初始化
     */
    @Scheduled(initialDelay = 5000, fixedDelay = Long.MAX_VALUE)
    public void initializeCourseStats() {
        courseRankingScheduler.initializeCourseStats();
    }
    
    /**
     * 每天凌晨2:30同步昨天的用户统计数据
     */
    @Scheduled(cron = "0 30 2 * * ?")
    public void syncYesterdayStats() {
        dailyStatsService.syncYesterdayStats();
    }
    
    /**
     * 每天凌晨4:00检查今天是否有遗留的Redis数据需要同步
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void checkTodayRedisData() {
        // 检查今天是否有Redis数据需要同步（通常不应该有，除非系统异常）
        dailyStatsService.syncSpecificDate(java.time.LocalDate.now());
    }
}