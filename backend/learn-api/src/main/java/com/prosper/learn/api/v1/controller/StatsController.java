package com.prosper.learn.api.v1.controller;

import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.ArticleViewService;
import com.prosper.learn.domain.service.DailyStatsService;
import com.prosper.learn.domain.service.StatsMonitorService;
import com.prosper.learn.dto.UserStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 统计接口
 * 从StatsClient迁移而来
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
public class StatsController {

    private final DailyStatsService dailyStatsService;
    private final ArticleViewService articleViewService;
    private final StatsMonitorService statsMonitorService;

    /**
     * 记录访问
     * 映射: POST /api/stats/view → POST /api/v1/stats/views
     */
    @PostMapping("/stats/views")
    public ResponseEntity<ApiResponse<Void>> recordView(
            @RequestParam Long articleId, 
            @RequestParam Long userId, 
            @RequestParam String ipAddress) {
        
        articleViewService.recordView(articleId, userId, ipAddress);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 用户今日统计
     * 映射: GET /api/stats/user/{userId}/today → GET /api/v1/stats/users/{userId}/today
     */
    @GetMapping("/stats/users/{userId}/today")
    public ResponseEntity<ApiResponse<UserStatsDTO>> getUserTodayStats(@PathVariable Long userId) {
        UserStatsDTO stats = dailyStatsService.getUserTodayStats(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 用户昨日统计
     * 映射: GET /api/stats/user/{userId}/yesterday → GET /api/v1/stats/users/{userId}/yesterday
     */
    @GetMapping("/stats/users/{userId}/yesterday")
    public ResponseEntity<ApiResponse<UserStatsDTO>> getUserYesterdayStats(@PathVariable Long userId) {
        UserStatsDTO stats = dailyStatsService.getUserYesterdayStats(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 用户历史统计
     * 映射: GET /api/stats/user/{userId}/history → GET /api/v1/stats/users/{userId}/history?days=7
     */
    @GetMapping("/stats/users/{userId}/history")
    public ResponseEntity<ApiResponse<UserStatsDTO>> getUserHistoryStats(
            @PathVariable Long userId, 
            @RequestParam(defaultValue = "7") int days) {
        
        UserStatsDTO stats = dailyStatsService.getUserHistoryStats(userId, days);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 用户时间段统计
     * 映射: GET /api/stats/user/{userId}/period → GET /api/v1/stats/users/{userId}/period?days=7
     */
    @GetMapping("/stats/users/{userId}/period")
    public ResponseEntity<ApiResponse<UserStatsDTO>> getUserPeriodStats(
            @PathVariable Long userId, 
            @RequestParam(defaultValue = "7") int days) {

        UserStatsDTO stats = dailyStatsService.getUserPeriodStatsWithDaily(userId, days);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 用户全部时间统计
     * 映射: GET /api/stats/user/{userId}/all-time → GET /api/v1/stats/users/{userId}/all-time
     */
    @GetMapping("/stats/users/{userId}/all-time")
    public ResponseEntity<ApiResponse<UserStatsDTO>> getUserAllTimeStats(@PathVariable Long userId) {
        UserStatsDTO stats = dailyStatsService.getUserAllTimeStats(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 手动同步
     * 映射: POST /api/stats/sync/manual → POST /api/v1/stats/sync/manual
     */
    @PostMapping("/stats/sync/manual")
    public ResponseEntity<ApiResponse<Object>> manualSync() {
        dailyStatsService.syncYesterdayStats();
        return ResponseEntity.ok(ApiResponse.success("同步成功"));
    }

    /**
     * 健康状态
     * 映射: GET /api/stats/health → GET /api/v1/stats/health
     */
    @GetMapping("/stats/health")
    public ResponseEntity<ApiResponse<Object>> getHealthStatus() {
        String status = statsMonitorService.getSystemStatus();
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * 同步指定日期
     * 映射: POST /api/stats/sync/date → POST /api/v1/stats/sync/date?date=xxx
     */
    @PostMapping("/stats/sync/date")
    public ResponseEntity<ApiResponse<Object>> syncByDate(@RequestParam String date) {
        LocalDate targetDate = LocalDate.parse(date);
        dailyStatsService.syncSpecificDate(targetDate);
        return ResponseEntity.ok(ApiResponse.success("指定日期统计数据同步成功"));
    }
}