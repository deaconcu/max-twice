package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.application.dto.request.AddDeckToMemoryBankRequest;
import com.prosper.learn.application.dto.request.UpdateCourseSettingRequest;
import com.prosper.learn.application.dto.response.ReviewSummaryDTO;
import com.prosper.learn.application.service.MemoryBankService;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.application.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 记忆库管理控制器
 */
@RestController
@RequestMapping("/v1/memory/memory-bank")
@RequiredArgsConstructor
@Slf4j
@Validated
public class MemoryBankController {

    private final MemoryBankService memoryBankService;

    /**
     * 添加卡片组到记忆库
     */
    @PostMapping("/decks")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> addDeckToMemoryBank(
            @Valid @RequestBody AddDeckToMemoryBankRequest request,
            @CurrentUser UserDO currentUser) {
        memoryBankService.addDeckToMemoryBank(currentUser.getId(), request);
        return ApiResponse.success();
    }

    /**
     * 获取复习概览（包含课程列表和统计数据）
     */
    @GetMapping("/courses")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<ReviewSummaryDTO> getReviewSummary(
            @RequestParam(required = false) @Min(value = 0, message = "状态不能小于0") Integer state,
            @CurrentUser UserDO currentUser) {

        ReviewSummaryDTO result = memoryBankService.getReviewSummary(currentUser.getId(), state);
        return ApiResponse.success(result);
    }

    /**
     * 更新课程复习策略
     */
    @PutMapping("/courses/{courseId}/settings")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> updateCourseSetting(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @Valid @RequestBody UpdateCourseSettingRequest request,
            @CurrentUser UserDO currentUser) {

        memoryBankService.updateCourseSetting(currentUser.getId(), courseId, request);
        return ApiResponse.success();
    }

    /**
     * 移除卡片组
     */
    @DeleteMapping("/courses/{courseId}/decks/{deckId}")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> removeDeckFromCourse(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0")
            Long deckId,
            @CurrentUser UserDO currentUser) {

        memoryBankService.removeDeckFromCourse(currentUser.getId(), courseId, deckId);
        return ApiResponse.success();
    }


}