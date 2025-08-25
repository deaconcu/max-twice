package com.prosper.learn.api.web;

import com.prosper.learn.api.client.StatsClient;
import com.prosper.learn.domain.service.ArticleViewService;
import com.prosper.learn.domain.service.DailyStatsService;
import com.prosper.learn.domain.service.StatsMonitorService;
import com.prosper.learn.dto.Response;
import com.prosper.learn.dto.UserStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@Slf4j
@RequiredArgsConstructor
public class StatsController implements StatsClient {

    private final DailyStatsService dailyStatsService;
    private final ArticleViewService articleViewService;
    private final StatsMonitorService statsMonitorService;

    /**
     * 记录文章访问
     */
    @Override
    public Response<Void> recordView(Long articleId, Integer userId, String ipAddress) {
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
    @Override
    public Response<UserStatsDTO> getUserTodayStats(Integer userId) {
        try {
            UserStatsDTO stats = dailyStatsService.getUserTodayStats(userId);
            return new Response<>(stats);
        } catch (Exception e) {
            log.error("获取用户今日统计失败", e);
            return new Response<>(Response.FAILED, "获取统计失败", null);
        }
    }

    /**
     * 获取用户昨日统计
     */
    @Override
    public Response<UserStatsDTO> getUserYesterdayStats(Integer userId) {
        try {
            UserStatsDTO stats = dailyStatsService.getUserYesterdayStats(userId);
            return new Response<>(stats);
        } catch (Exception e) {
            log.error("获取用户昨日统计失败", e);
            return new Response<>(Response.FAILED, "获取统计失败", null);
        }
    }

    /**
     * 获取用户历史统计
     */
    @Override
    public Response<UserStatsDTO> getUserHistoryStats(Integer userId, int days) {
        try {
            UserStatsDTO stats = dailyStatsService.getUserHistoryStats(userId, days);
            return new Response<>(stats);
        } catch (Exception e) {
            log.error("获取用户历史统计失败", e);
            return new Response<>(Response.FAILED, "获取统计失败", null);
        }
    }

    /**
     * 获取用户时间段统计（包含每日明细）
     */
    @Override
    public Response<UserStatsDTO> getUserPeriodStats(Integer userId, int days) {
        try {
            UserStatsDTO stats = dailyStatsService.getUserPeriodStatsWithDaily(userId, days);
            return new Response<>(stats);
        } catch (Exception e) {
            log.error("获取用户时间段统计失败", e);
            return new Response<>(Response.FAILED, "获取统计失败", null);
        }
    }

    /**
     * 获取用户全部时间统计
     */
    @Override
    public Response<UserStatsDTO> getUserAllTimeStats(Integer userId) {
        try {
            UserStatsDTO stats = dailyStatsService.getUserAllTimeStats(userId);
            return new Response<>(stats);
        } catch (Exception e) {
            log.error("获取用户全部时间统计失败", e);
            return new Response<>(Response.FAILED, "获取统计失败", null);
        }
    }

    /**
     * 手动触发同步（仅用于测试）
     */
    @Override
    public Response<Void> manualSync() {
        try {
            dailyStatsService.syncYesterdayStats();
            return new Response<>(Response.SUCCESS, "同步成功", null);
        } catch (Exception e) {
            log.error("手动同步失败", e);
            return new Response<>(Response.FAILED, "同步失败", null);
        }
    }

    /**
     * 获取系统健康状态
     */
    @Override
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
     * 手动触发指定日期的数据同步
     */
    @Override
    public Response<String> syncSpecificDate(String date) {
        try {
            LocalDate targetDate = null;
            if (date != null && !date.isEmpty()) {
                try {
                    targetDate = LocalDate.parse(date);
                } catch (Exception e) {
                    return new Response<>(Response.FAILED, "日期格式错误，请使用 YYYY-MM-DD 格式", null);
                }
            }
            
            String result = dailyStatsService.syncSpecificDate(targetDate);
            return new Response<>(Response.SUCCESS, result, result);
        } catch (Exception e) {
            log.error("手动同步失败", e);
            return new Response<>(Response.FAILED, "同步失败: " + e.getMessage(), null);
        }
    }
}