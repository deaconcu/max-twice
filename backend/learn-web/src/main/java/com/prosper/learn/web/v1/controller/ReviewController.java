package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.application.dto.request.ReviewCardRequest;
import com.prosper.learn.application.dto.request.ReviewSessionRequest;
import com.prosper.learn.application.dto.response.ReviewStatsDTO;
import com.prosper.learn.application.dto.response.card.CardWithSrsDTO;
import com.prosper.learn.application.service.ReviewService;
import com.prosper.learn.shared.domain.Enums;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 复习功能控制器
 */
@RestController
@RequestMapping("/api/v1/memory/review")
@RequiredArgsConstructor
@Slf4j
@Validated
@RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 获取复习队列 - 只查询到期的卡片，限制100个
     */
    @GetMapping("/queue")
    @SaCheckLogin
    public ApiResponse<List<CardWithSrsDTO>> getReviewQueue(
            @RequestParam(required = false) @Positive(message = "课程ID必须大于0") Long courseId,
            @CurrentUser UserDO currentUser) {

        // 固定参数：只查询到期的，限制100个
        List<CardWithSrsDTO> result = reviewService.getReviewQueue(currentUser.getId(), true, courseId, 100, null);
        return ApiResponse.success(result);
    }

    /**
     * 获取卡片列表 - 支持分页查询全部卡片
     */
    @GetMapping("/cards")
    @SaCheckLogin
    public ApiResponse<List<CardWithSrsDTO>> getCardList(
            @RequestParam(required = false) @Positive(message = "课程ID必须大于0") Long courseId,
            @RequestParam(defaultValue = "20") @Positive(message = "限制数量必须大于0") Integer limit,
            @RequestParam(required = false) @Positive(message = "最后ID必须大于0") Long lastId,
            @CurrentUser UserDO currentUser) {

        // 限制最大数量
        if (limit > 100) {
            limit = 100;
        }

        // 固定参数：查询全部卡片（不限制到期）
        List<CardWithSrsDTO> result = reviewService.getReviewQueue(currentUser.getId(), false, courseId, limit, lastId);
        return ApiResponse.success(result);
    }

    /**
     * 提交复习结果
     */
    @PostMapping("/submit")
    @SaCheckLogin
    public ApiResponse<Void> submitReview(
            @Valid @RequestBody ReviewCardRequest request,
            @CurrentUser UserDO currentUser) {
        reviewService.submitReview(currentUser.getId(), request);
        return ApiResponse.success();
    }

    /**
     * 批量提交复习结果
     *
     * @deprecated 此接口未被前端使用，且后端实现存在严重bug。
     *             前端已采用单次提交模式 (POST /submit)，体验更好。
     *             如需重新启用，必须先修复 ReviewService.batchSubmitReview() 的实现。
     */
//    @PostMapping("/batch-submit")
//    @SaCheckLogin
//    public ApiResponse<Void> batchSubmitReview(
//            @Valid @RequestBody ReviewSessionRequest session,
//            @CurrentUser UserDO currentUser) {
//        reviewService.batchSubmitReview(currentUser.getId(), session);
//        return ApiResponse.success();
//    }

    /**
     * 获取复习统计
     */
    @GetMapping("/stats")
    @SaCheckLogin
    public ApiResponse<ReviewStatsDTO> getReviewStats(
            @RequestParam(defaultValue = "WEEK") Enums.Period period,
            @CurrentUser UserDO currentUser) {

        ReviewStatsDTO result = reviewService.getReviewStats(currentUser.getId(), period);
        return ApiResponse.success(result);
    }

}