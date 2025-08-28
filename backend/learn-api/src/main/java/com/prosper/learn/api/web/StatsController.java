package com.prosper.learn.api.web;

import com.prosper.learn.api.client.StatsClient;
import com.prosper.learn.common.exception.ErrorCode;
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
    public Response<Void> recordView(Long articleId, Long userId, String ipAddress) {
        articleViewService.recordView(articleId, userId, ipAddress);
        return new Response<>(Response.SUCCESS, "success", null);
    }

    /**
     * 获取用户今日统计
     */
    @Override
    public Response<UserStatsDTO> getUserTodayStats(Long userId) {
        UserStatsDTO stats = dailyStatsService.getUserTodayStats(userId);
        return new Response<>(stats);
    }

    /**
     * 获取用户昨日统计
     */
    @Override
    public Response<UserStatsDTO> getUserYesterdayStats(Long userId) {
        UserStatsDTO stats = dailyStatsService.getUserYesterdayStats(userId);
        return new Response<>(stats);
    }

    /**
     * 获取用户历史统计
     */
    @Override
    public Response<UserStatsDTO> getUserHistoryStats(Long userId, Integer days) {
        UserStatsDTO stats = dailyStatsService.getUserHistoryStats(userId, days);
        return new Response<>(stats);
    }

    /**
     * 获取用户时间段统计（包含每日明细）
     */
    @Override
    public Response<UserStatsDTO> getUserPeriodStats(Long userId, Integer days) {
        UserStatsDTO stats = dailyStatsService.getUserPeriodStatsWithDaily(userId, days);
        return new Response<>(stats);
    }

    /**
     * 获取用户全部时间统计
     */
    @Override
    public Response<UserStatsDTO> getUserAllTimeStats(Long userId) {
        UserStatsDTO stats = dailyStatsService.getUserAllTimeStats(userId);
        return new Response<>(stats);
    }

    /**
     * 手动触发同步（仅用于测试）
     */
    @Override
    public Response<Void> manualSync() {
        dailyStatsService.syncYesterdayStats();
        return new Response<>(Response.SUCCESS, "同步成功", null);
    }

    /**
     * 获取系统健康状态
     */
    @Override
    public Response<String> getHealthStatus() {
        String status = statsMonitorService.getSystemStatus();
        return new Response<>(status);
    }

    /**
     * 手动触发指定日期的数据同步
     */
    @Override
    public Response<String> syncSpecificDate(String date) {
        LocalDate targetDate = null;
        if (date != null && !date.isEmpty()) {
            try {
                targetDate = LocalDate.parse(date);
            } catch (Exception e) {
                throw ErrorCode.SYSTEM_ERROR.exception();
            }
        }
        
        String result = dailyStatsService.syncSpecificDate(targetDate);
        return new Response<>(Response.SUCCESS, result, result);
    }
}