package com.twicemax.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.request.ReviewCardRequest;
import com.twicemax.application.dto.response.ReviewSubmitResultDTO;
import com.twicemax.application.dto.response.card.CardWithSrsDTO;
import com.twicemax.application.service.ReviewService;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v1.annotation.CurrentUser;
import com.twicemax.application.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 复习功能控制器
 */
@RestController
@RequestMapping("/v1/memory/review")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 获取卡片列表（管理界面用）
     */
    @GetMapping("/cards")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<List<CardWithSrsDTO>> getCardList(
            @RequestParam(required = false) @Positive(message = "课程ID必须大于0") Long courseId,
            @RequestParam(required = false) @Positive(message = "lastId必须大于0") Long lastId,
            @CurrentUser UserDO currentUser) {

        List<CardWithSrsDTO> result = reviewService.getCardList(currentUser, courseId, lastId);
        return ApiResponse.success(result);
    }

    /**
     * 获取下一张待复习卡片
     */
    @GetMapping("/next")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<ReviewSubmitResultDTO> getNextCard(
            @RequestParam(required = false) @Positive(message = "课程ID必须大于0") Long courseId,
            @CurrentUser UserDO currentUser) {

        ReviewSubmitResultDTO result = reviewService.getNextCard(currentUser, courseId);
        return ApiResponse.success(result);
    }

    /**
     * 提交复习结果并返回下一张卡片
     */
    @PostMapping("/submit")
    @SaCheckLogin
    @RateLimit(capacity = 60, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<ReviewSubmitResultDTO> submitReview(
            @Valid @RequestBody ReviewCardRequest request,
            @CurrentUser UserDO currentUser) {
        ReviewSubmitResultDTO result = reviewService.submitReview(currentUser, request);
        return ApiResponse.success(result);
    }

}
