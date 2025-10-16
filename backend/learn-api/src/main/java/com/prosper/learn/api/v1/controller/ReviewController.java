package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums;
import com.prosper.learn.domain.service.business.ReviewService;
import com.prosper.learn.dto.request.ReviewCardRequest;
import com.prosper.learn.dto.response.MemoryCardViewDTO;
import com.prosper.learn.dto.request.ReviewSessionRequest;
import com.prosper.learn.dto.response.ReviewStatsDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 复习功能控制器
 */
@RestController
@RequestMapping("/api/v1/memory/review")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 获取复习队列 - 只查询到期的卡片，限制100个
     */
    @GetMapping("/queue")
    public ApiResponse<List<MemoryCardViewDTO>> getReviewQueue(
            @RequestParam(required = false) @Positive(message = "课程ID必须大于0") Long courseId) {
        
        long userId = StpUtil.getLoginIdAsLong();
        
        // 固定参数：只查询到期的，限制100个
        List<MemoryCardViewDTO> result = reviewService.getReviewQueue(userId, true, courseId, 100, null);
        return ApiResponse.success(result);
    }

    /**
     * 获取卡片列表 - 支持分页查询全部卡片
     */
    @GetMapping("/cards")
    public ApiResponse<List<MemoryCardViewDTO>> getCardList(
            @RequestParam(required = false) @Positive(message = "课程ID必须大于0") Long courseId,
            @RequestParam(defaultValue = "20") @Positive(message = "限制数量必须大于0") Integer limit,
            @RequestParam(required = false) @Positive(message = "最后ID必须大于0") Long lastId) {
        
        long userId = StpUtil.getLoginIdAsLong();
        
        // 限制最大数量
        if (limit > 100) {
            limit = 100;
        }
        
        // 固定参数：查询全部卡片（不限制到期）
        List<MemoryCardViewDTO> result = reviewService.getReviewQueue(userId, false, courseId, limit, lastId);
        return ApiResponse.success(result);
    }

    /**
     * 提交复习结果
     */
    @PostMapping("/submit")
    public ApiResponse<Void> submitReview(@Valid @RequestBody ReviewCardRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        reviewService.submitReview(userId, request);
        return ApiResponse.success();
    }

    /**
     * 批量提交复习结果
     */
    @PostMapping("/batch-submit")
    public ApiResponse<Void> batchSubmitReview(@Valid @RequestBody ReviewSessionRequest session) {
        long userId = StpUtil.getLoginIdAsLong();
        reviewService.batchSubmitReview(userId, session);
        return ApiResponse.success();
    }

    /**
     * 获取复习统计
     */
    @GetMapping("/stats")
    public ApiResponse<ReviewStatsDTO> getReviewStats(
            @RequestParam(defaultValue = "WEEK") Enums.Period period) {
        
        long userId = StpUtil.getLoginIdAsLong();
        ReviewStatsDTO result = reviewService.getReviewStats(userId, period);
        return ApiResponse.success(result);
    }

}