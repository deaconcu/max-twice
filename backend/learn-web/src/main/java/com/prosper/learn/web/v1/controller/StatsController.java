package com.prosper.learn.web.v1.controller;

import com.prosper.learn.application.service.PlatformStatsService;
import com.prosper.learn.analytics.stats.service.DailyStatsService;
import com.prosper.learn.analytics.stats.service.RedisStatsDomainService;
import com.prosper.learn.application.dto.request.RecordViewRequest;
import com.prosper.learn.application.dto.response.PlatformStatsDTO;
import com.prosper.learn.analytics.dto.UserDailyStatsDTO;
import com.prosper.learn.analytics.dto.UserStatsDTO;
import com.prosper.learn.analytics.dto.UserStatsWithDailyDTO;
import com.prosper.learn.shared.domain.Enums.UserRole;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.application.dto.ApiResponse;
import com.prosper.learn.web.v1.annotation.JsonParam;
import com.prosper.learn.web.v1.annotation.RequireRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * 统计接口
 * 从StatsClient迁移而来
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
@Validated
@RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public class StatsController {

    private final DailyStatsService dailyStatsService;
    private final RedisStatsDomainService redisStatsService;
    private final PlatformStatsService platformStatsService;

    /**
     * 记录访问
     * 映射: POST /api/stats/view → POST /api/v1/stats/views
     */
    @PostMapping("/stats/views")
    public ApiResponse<Void> recordView(@RequestBody @Valid RecordViewRequest request) {
        
        redisStatsService.recordArticleView(request.getArticleId(), request.getUserId());
        return ApiResponse.success();
    }

    /**
     * 用户今日统计
     * 映射: GET /api/stats/user/{userId}/today → GET /api/v1/stats/users/{userId}/today
     */
    @GetMapping("/stats/users/{userId}/today")
    public ApiResponse<UserDailyStatsDTO> getUserTodayStats(
            @PathVariable @NotNull(message = "用户ID不能为空")
            @Positive(message = "用户ID必须大于0")
            Long userId) {
        UserDailyStatsDTO stats = dailyStatsService.getUserTodayStats(userId);
        return ApiResponse.success(stats);
    }

    /**
     * 用户历史统计
     * 映射: GET /api/stats/user/{userId}/history → GET /api/v1/stats/users/{userId}/history?days=7
     */
    @GetMapping("/stats/users/{userId}/history")
    public ApiResponse<UserStatsWithDailyDTO> getUserHistoryStats(
            @PathVariable @NotNull(message = "用户ID不能为空")
            @Positive(message = "用户ID必须大于0")
            Long userId,
            @RequestParam(defaultValue = "7")
            @Positive(message = "天数必须大于0")
            int days) {

        UserStatsWithDailyDTO stats = dailyStatsService.getUserHistoryStats(userId, days);
        return ApiResponse.success(stats);
    }

    /**
     * 用户全部时间统计
     * 映射: GET /api/stats/user/{userId}/all-time → GET /api/v1/stats/users/{userId}/all-time
     */
    @GetMapping("/stats/users/{userId}/all-time")
    public ApiResponse<UserStatsDTO> getUserAllTimeStats(
            @PathVariable @NotNull(message = "用户ID不能为空")
            @Positive(message = "用户ID必须大于0")
            Long userId) {
        UserStatsDTO stats = dailyStatsService.getUserAllTimeStats(userId);
        return ApiResponse.success(stats);
    }

    /**
     * 手动同步
     * 映射: POST /api/stats/sync/manual → POST /api/v1/stats/sync/manual
     */
    @RequireRole(UserRole.ADMIN)
    @PostMapping("/stats/sync/manual")
    public ApiResponse<Object> manualSync() {
        dailyStatsService.syncYesterdayStats();
        return ApiResponse.success("同步成功");
    }

    /**
     * 同步指定日期
     * 映射: POST /api/stats/sync/date → POST /api/v1/stats/sync/date?date=xxx
     */
    @RequireRole(UserRole.ADMIN)
    @PostMapping("/stats/sync/date")
    public ApiResponse<Object> syncByDate(
            @JsonParam("date") @NotBlank(message = "日期不能为空") String date) {
        LocalDate targetDate = LocalDate.parse(date);
        dailyStatsService.syncSpecificDate(targetDate);
        return ApiResponse.success("指定日期统计数据同步成功");
    }

    /**
     * 获取平台统计数据
     * 映射: GET /api/platform/stats → GET /api/v1/stats/platform
     */
    @GetMapping("/stats/platform")
    public ApiResponse<PlatformStatsDTO> getPlatformStats() {
        PlatformStatsDTO stats = platformStatsService.getPlatformStats();
        return ApiResponse.success(stats);
    }
}