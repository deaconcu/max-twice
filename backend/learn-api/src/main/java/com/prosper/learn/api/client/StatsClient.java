package com.prosper.learn.api.client;

import com.prosper.learn.dto.Response;
import com.prosper.learn.dto.UserStatsDTO;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 统计数据接口
 * 
 * @author Claude
 * @since 2024-08-24
 */
public interface StatsClient {
    
    /**
     * 记录文章访问
     */
    @PostMapping("/api/stats/view")
    Response<Void> recordView(@RequestParam Long articleId,
                             @RequestParam(required = false) Integer userId,
                             @RequestParam(required = false) String ipAddress);

    /**
     * 获取用户今日统计
     */
    @GetMapping("/api/stats/user/{userId}/today")
    Response<UserStatsDTO> getUserTodayStats(@PathVariable Integer userId);

    /**
     * 获取用户昨日统计
     */
    @GetMapping("/api/stats/user/{userId}/yesterday")
    Response<UserStatsDTO> getUserYesterdayStats(@PathVariable Integer userId);

    /**
     * 获取用户历史统计
     */
    @GetMapping("/api/stats/user/{userId}/history")
    Response<UserStatsDTO> getUserHistoryStats(@PathVariable Integer userId,
                                              @RequestParam(defaultValue = "7") int days);

    /**
     * 获取用户时间段统计（包含每日明细）
     */
    @GetMapping("/api/stats/user/{userId}/period")
    Response<UserStatsDTO> getUserPeriodStats(@PathVariable Integer userId,
                                             @RequestParam(defaultValue = "7") int days);

    /**
     * 获取用户全部时间统计
     */
    @GetMapping("/api/stats/user/{userId}/all-time")
    Response<UserStatsDTO> getUserAllTimeStats(@PathVariable Integer userId);

    /**
     * 手动触发同步（仅用于测试）
     */
    @PostMapping("/api/stats/sync/manual")
    Response<Void> manualSync();

    /**
     * 获取系统健康状态
     */
    @GetMapping("/api/stats/health")
    Response<String> getHealthStatus();

    /**
     * 手动触发指定日期的数据同步
     */
    @PostMapping("/api/stats/sync/date")
    Response<String> syncSpecificDate(@RequestParam(required = false) String date);
}