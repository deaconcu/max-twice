package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.request.AddDeckToMemoryBankRequest;
import com.twicemax.application.dto.request.UpdateCourseSettingRequest;
import com.twicemax.application.dto.response.ReviewSummaryDTO;
import com.twicemax.application.service.MemoryBankService;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 记忆库管理控制器
 */
@RestController
@RequestMapping("/memory/memory-bank")
@RequiredArgsConstructor
@Validated
public class MemoryBankController {

    private final MemoryBankService memoryBankService;

    @PostMapping("/decks")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> addDeckToMemoryBank(
            @Valid @RequestBody AddDeckToMemoryBankRequest request,
            @CurrentUser UserDO currentUser) {
        memoryBankService.addDeckToMemoryBank(currentUser.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/courses")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ReviewSummaryDTO getReviewSummary(
            @RequestParam(required = false) @Min(value = 0, message = "状态不能小于0") Integer state,
            @CurrentUser UserDO currentUser) {
        return memoryBankService.getReviewSummary(currentUser.getId(), state);
    }

    @PutMapping("/courses/{courseId}/settings")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> updateCourseSetting(
            @PathVariable @NotNull(message = "课程ID不能为空") @Positive(message = "课程ID必须大于0") Long courseId,
            @Valid @RequestBody UpdateCourseSettingRequest request,
            @CurrentUser UserDO currentUser) {
        memoryBankService.updateCourseSetting(currentUser.getId(), courseId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/courses/{courseId}/decks/{deckId}")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> removeDeckFromCourse(
            @PathVariable @NotNull(message = "课程ID不能为空") @Positive(message = "课程ID必须大于0") Long courseId,
            @PathVariable @NotNull(message = "卡片组ID不能为空") @Positive(message = "卡片组ID必须大于0") Long deckId,
            @CurrentUser UserDO currentUser) {
        memoryBankService.removeDeckFromCourse(currentUser.getId(), courseId, deckId);
        return ResponseEntity.noContent().build();
    }
}
