package com.twicemax.web.v2.controller;

import com.twicemax.analytics.dto.HeatmapDataDTO;
import com.twicemax.analytics.dto.UserDailyStatsDTO;
import com.twicemax.analytics.dto.UserStatsDTO;
import com.twicemax.analytics.dto.UserStatsWithDailyDTO;
import com.twicemax.analytics.stats.scheduler.StatsSyncScheduler;
import com.twicemax.analytics.stats.service.RedisStatsDomainService;
import com.twicemax.analytics.stats.service.UserLearningStatsService;
import com.twicemax.analytics.stats.service.UserStatsDomainService;
import com.twicemax.application.dto.request.RecordViewRequest;
import com.twicemax.application.dto.response.PlatformStatsDTO;
import com.twicemax.application.service.PlatformStatsService;
import com.twicemax.application.service.StatsService;
import com.twicemax.application.service.UserStatsService;
import com.twicemax.shared.domain.Enums.UserRole;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.user.profile.UserDO;
import com.twicemax.user.profile.UserDataService;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.CurrentUser;
import com.twicemax.web.v2.annotation.JsonParam;
import com.twicemax.web.v2.annotation.RequireRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * 统计接口
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Validated
public class StatsController {

    private final UserStatsDomainService userStatsDomainService;
    private final RedisStatsDomainService redisStatsDomainService;
    private final UserLearningStatsService userLearningStatsService;
    private final StatsService statsService;
    private final UserStatsService userStatsService;
    private final StatsSyncScheduler statsSyncScheduler;
    private final PlatformStatsService platformStatsService;
    private final UserDataService userDataService;

    @PostMapping("/stats/views")
    @RateLimit(capacity = 200, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> recordView(
            @RequestBody @Valid RecordViewRequest request,
            @CurrentUser UserDO currentUser) {
        statsService.recordPostView(request.getArticleId(), currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/users/{userId}/today")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public UserDailyStatsDTO getUserTodayStats(
            @PathVariable @NotNull(message = "用户ID不能为空") @Positive(message = "用户ID必须大于0") Long userId) {
        return redisStatsDomainService.getUserTodayStats(userId);
    }

    @GetMapping("/stats/users/{userId}/history")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public UserStatsWithDailyDTO getUserHistoryStats(
            @PathVariable @NotNull(message = "用户ID不能为空") @Positive(message = "用户ID必须大于0") Long userId,
            @RequestParam(defaultValue = "7") @Positive(message = "天数必须大于0") @Max(value = 365, message = "天数不能超过365天") int days) {
        return userStatsDomainService.getUserHistoryStats(userId, days);
    }

    @GetMapping("/stats/users/{userId}/all-time")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public UserStatsDTO getUserAllTimeStats(
            @PathVariable @NotNull(message = "用户ID不能为空") @Positive(message = "用户ID必须大于0") Long userId) {
        return userStatsService.getUserAllTimeStats(userId);
    }

    @RequireRole(UserRole.ADMIN)
    @PostMapping("/stats/sync/manual")
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> manualSync() {
        statsSyncScheduler.syncStatsForDate(LocalDate.now().minusDays(1));
        return ResponseEntity.noContent().build();
    }

    @RequireRole(UserRole.ADMIN)
    @PostMapping("/stats/sync/date")
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> syncByDate(
            @JsonParam("date") @NotBlank(message = "日期不能为空") String date) {
        statsSyncScheduler.syncStatsForDate(LocalDate.parse(date));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/platform")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public PlatformStatsDTO getPlatformStats() {
        return platformStatsService.getPlatformStats();
    }

    @GetMapping("/stats/users/{userId}/heatmap")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public HeatmapDataDTO getUserHeatmap(
            @PathVariable @NotNull(message = "用户ID不能为空") @Positive(message = "用户ID必须大于0") Long userId,
            @RequestParam(defaultValue = "12") @Positive(message = "月数必须大于0") @Max(value = 24, message = "月数不能超过24个月") int months) {
        UserDO user = userDataService.getById(userId);
        if (user == null) {
            throw StatusCode.USER_NOT_FOUND.exception();
        }
        LocalDate joinedDate = user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate() : null;
        return userLearningStatsService.getHeatmapData(userId, months, joinedDate, user.getTimezone());
    }
}
