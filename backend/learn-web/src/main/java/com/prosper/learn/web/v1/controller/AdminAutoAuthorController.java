package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.application.service.autoauthor.AutoAuthorGenerationService;
import com.prosper.learn.application.service.autoauthor.AutoAuthorQueueService;
import com.prosper.learn.application.service.autoauthor.AutoAuthorScanner;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.application.dto.ApiResponse;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * AutoAuthor 管理接口
 */
@RestController
@RequestMapping("/api/v1/admin/auto-author")
@RequiredArgsConstructor
@Validated
public class AdminAutoAuthorController {

    private final AutoAuthorScanner scanner;
    private final AutoAuthorQueueService queueService;
    private final AutoAuthorGenerationService generationService;
    private final SystemProperties systemProperties;

    @PostMapping("/scan")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> scan() {
        int total = 0;
        for (int i = 0; i < 10; i++) {
            int c = scanner.scanOnePage();
            total += c;
            if (c == 0) break;
        }
        return ApiResponse.success();
    }

    @PostMapping("/enqueue/{nodeId}")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> enqueue(
            @PathVariable("nodeId") @NotNull(message = "节点ID不能为空")
            @Positive(message = "节点ID必须大于0")
            long nodeId) {
        if (!systemProperties.getAutoAuthor().isEnabled()) return ApiResponse.success();
        queueService.enqueue(nodeId);
        return ApiResponse.success();
    }

    @PostMapping("/session/reset")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> resetSession() {
        generationService.resetSession();
        return ApiResponse.success();
    }

    @DeleteMapping("/queue")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<String> clearQueue() {
        long count = queueService.clear();
        String message = count > 0 ?
            String.format("已清空队列，共删除 %d 个待处理节点", count) :
            "队列已经是空的";
        return ApiResponse.success(message);
    }
}
