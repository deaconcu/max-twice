package com.prosper.learn.api.web;

import com.prosper.learn.domain.service.ArticleViewService;
import com.prosper.learn.domain.service.DailyStatsSyncService;
import com.prosper.learn.domain.service.StatsMonitorService;
import com.prosper.learn.dto.Response;
import com.prosper.learn.dto.UserStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
@Slf4j
@RequiredArgsConstructor
public class StatsController {

    private final DailyStatsSyncService dailyStatsSyncService;
    private final ArticleViewService articleViewService;
    private final StatsMonitorService statsMonitorService;

    /**
     * 记录文章访问
     */
    @PostMapping("/view")
    public Response<Void> recordView(@RequestParam Long articleId,
                                     @RequestParam(required = false) Integer userId,
                                     @RequestParam(required = false) String ipAddress) {
        try {
            articleViewService.recordView(articleId, userId, ipAddress);
            return new Response<>(Response.SUCCESS, "success", null);
        } catch (Exception e) {
            log.error("记录访问失败", e);
            return new Response<>(Response.FAILED, "记录访问失败", null);
        }
    }

    /**
     * 获取用户今日统计
     */
    @GetMapping("/user/{userId}/today")
    public Response<UserStatsDTO> getUserTodayStats(@PathVariable Integer userId) {
        try {
            UserStatsDTO stats = dailyStatsSyncService.getUserTodayStats(userId);
            return new Response<>(stats);
        } catch (Exception e) {
            log.error("获取用户今日统计失败", e);
            return new Response<>(Response.FAILED, "获取统计失败", null);
        }
    }

    /**
     * 获取用户昨日统计
     */
    @GetMapping("/user/{userId}/yesterday")
    public Response<UserStatsDTO> getUserYesterdayStats(@PathVariable Integer userId) {
        try {
            UserStatsDTO stats = dailyStatsSyncService.getUserYesterdayStats(userId);
            return new Response<>(stats);
        } catch (Exception e) {
            log.error("获取用户昨日统计失败", e);
            return new Response<>(Response.FAILED, "获取统计失败", null);
        }
    }

    /**
     * 获取用户历史统计
     */
    @GetMapping("/user/{userId}/history")
    public Response<UserStatsDTO> getUserHistoryStats(@PathVariable Integer userId,
                                                      @RequestParam(defaultValue = "7") int days) {
        try {
            UserStatsDTO stats = dailyStatsSyncService.getUserHistoryStats(userId, days);
            return new Response<>(stats);
        } catch (Exception e) {
            log.error("获取用户历史统计失败", e);
            return new Response<>(Response.FAILED, "获取统计失败", null);
        }
    }

    /**
     * 获取用户时间段统计（包含每日明细）
     */
    @GetMapping("/user/{userId}/period")
    public Response<UserStatsDTO> getUserPeriodStats(@PathVariable Integer userId,
                                                     @RequestParam(defaultValue = "7") int days) {
        try {
            UserStatsDTO stats = dailyStatsSyncService.getUserPeriodStatsWithDaily(userId, days);
            return new Response<>(stats);
        } catch (Exception e) {
            log.error("获取用户时间段统计失败", e);
            return new Response<>(Response.FAILED, "获取统计失败", null);
        }
    }

    /**
     * 手动触发同步（仅用于测试）
     */
    @PostMapping("/sync/manual")
    public Response<Void> manualSync() {
        try {
            dailyStatsSyncService.syncYesterdayStats();
            return new Response<>(Response.SUCCESS, "同步成功", null);
        } catch (Exception e) {
            log.error("手动同步失败", e);
            return new Response<>(Response.FAILED, "同步失败", null);
        }
    }

    /**
     * 获取系统健康状态
     */
    @GetMapping("/health")
    public Response<String> getHealthStatus() {
        try {
            String status = statsMonitorService.getSystemStatus();
            return new Response<>(status);
        } catch (Exception e) {
            log.error("获取健康状态失败", e);
            return new Response<>(Response.FAILED, "获取状态失败", null);
        }
    }

    /**
     * 手动触发补偿同步
     */
    @PostMapping("/sync/compensation")
    public Response<Void> compensationSync() {
        try {
            dailyStatsSyncService.compensationSync();
            return new Response<>(Response.SUCCESS, "补偿同步成功", null);
        } catch (Exception e) {
            log.error("补偿同步失败", e);
            return new Response<>(Response.FAILED, "补偿同步失败", null);
        }
    }
}