package com.prosper.learn.api.application;

import com.prosper.learn.domain.service.CourseRankingScheduler;
import com.prosper.learn.domain.service.DailyStatsService;
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
    @Scheduled(initialDelay = 5000)
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
     * 每天凌晨4:00执行补偿同步任务
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void compensationSync() {
        dailyStatsService.compensationSync();
    }
}