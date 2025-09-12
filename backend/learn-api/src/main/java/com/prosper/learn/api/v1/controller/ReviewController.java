package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums;
import com.prosper.learn.domain.service.business.ReviewService;
import com.prosper.learn.dto.request.ReviewCardRequest;
import com.prosper.learn.dto.response.MemoryCardViewDTO;
import com.prosper.learn.dto.response.ReviewSessionDTO;
import com.prosper.learn.dto.response.ReviewStatsDTO;
import jakarta.validation.Valid;
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
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 获取复习队列
     */
    @GetMapping("/queue")
    public ApiResponse<List<MemoryCardViewDTO>> getReviewQueue(
            @RequestParam(defaultValue = "true") Boolean dueOnly,
            @RequestParam(required = false) Long courseId,
            @RequestParam(defaultValue = "50") Integer limit) {
        
        long userId = StpUtil.getLoginIdAsLong();
        
        // 限制最大数量
        if (limit > 500) {
            limit = 500;
        }
        
        List<MemoryCardViewDTO> result = reviewService.getReviewQueue(userId, dueOnly, courseId, limit);
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
    public ApiResponse<Void> batchSubmitReview(@Valid @RequestBody ReviewSessionDTO session) {
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