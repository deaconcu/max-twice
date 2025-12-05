package com.prosper.learn.web.client;

import com.prosper.learn.dto.response.Response;
import com.prosper.learn.dto.response.UserStatsDTO;
import org.springframework.web.bind.annotation.*;

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
    //@PostMapping("/api/stats/view")
    Response<Void> recordView(@RequestParam Long articleId,
                             @RequestParam(required = false) Long userId,
                             @RequestParam(required = false) String ipAddress);

    /**
     * 获取用户今日统计
     */
    //@GetMapping("/api/stats/user/{userId}/today")
    Response<UserStatsDTO> getUserTodayStats(@PathVariable Long userId);

    /**
     * 获取用户昨日统计
     */
    //@GetMapping("/api/stats/user/{userId}/yesterday")
    Response<UserStatsDTO> getUserYesterdayStats(@PathVariable Long userId);

    /**
     * 获取用户历史统计
     */
    //@GetMapping("/api/stats/user/{userId}/history")
    Response<UserStatsDTO> getUserHistoryStats(@PathVariable Long userId,
                                              @RequestParam(defaultValue = "7") Integer days);

    /**
     * 获取用户时间段统计（包含每日明细）
     */
    //@GetMapping("/api/stats/user/{userId}/period")
    Response<UserStatsDTO> getUserPeriodStats(@PathVariable Long userId,
                                             @RequestParam(defaultValue = "7") Integer days);

    /**
     * 获取用户全部时间统计
     */
    //@GetMapping("/api/stats/user/{userId}/all-time")
    Response<UserStatsDTO> getUserAllTimeStats(@PathVariable Long userId);

    /**
     * 手动触发同步（仅用于测试）
     */
    //@PostMapping("/api/stats/sync/manual")
    Response<Void> manualSync();

    /**
     * 获取系统健康状态
     */
    //@GetMapping("/api/stats/health")
    Response<String> getHealthStatus();

    /**
     * 手动触发指定日期的数据同步
     */
    //@PostMapping("/api/stats/sync/date")
    Response<String> syncSpecificDate(@RequestParam(required = false) String date);
}