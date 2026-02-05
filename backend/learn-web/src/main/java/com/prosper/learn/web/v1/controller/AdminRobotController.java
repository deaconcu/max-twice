package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.application.dto.response.RobotQueueStatsDTO;
import com.prosper.learn.application.service.robot.RobotGenerationService;
import com.prosper.learn.application.service.robot.RobotQueueService;
import com.prosper.learn.application.service.robot.RobotScanner;
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
 * Robot 内容生成管理接口
 */
@RestController
@RequestMapping("/api/v1/admin/robot")
@RequiredArgsConstructor
@Validated
public class AdminRobotController {

    private final RobotScanner scanner;
    private final RobotQueueService queueService;
    private final RobotGenerationService generationService;
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

    @PostMapping("/enqueue/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> enqueue(
            @PathVariable("id") @NotNull(message = "ID不能为空")
            @Positive(message = "ID必须大于0")
            long id,
            @RequestParam(value = "idType", defaultValue = "node") String idType,
            @RequestParam(value = "contentType", defaultValue = "auto") String contentType,
            @RequestParam(value = "recursive", defaultValue = "false") boolean recursive,
            @RequestParam(value = "deleteExisting", defaultValue = "true") boolean deleteExisting) {
        if (!systemProperties.getRobot().isEnabled()) return ApiResponse.success();
        if ("course".equals(idType)) {
            queueService.enqueueByCourseId(id, contentType, recursive, deleteExisting);
        } else {
            queueService.enqueue(id, contentType, recursive, deleteExisting);
        }
        return ApiResponse.success();
    }

    @PostMapping("/session/reset")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> resetSession() {
        generationService.resetSession();
        return ApiResponse.success();
    }

    @PostMapping("/session/summarize")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> summarizeSession() {
        generationService.summarizeCurrentSession();
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

    @GetMapping("/queue/stats")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<RobotQueueStatsDTO> getQueueStats() {
        RobotQueueStatsDTO stats = RobotQueueStatsDTO.builder()
            .pendingCount(queueService.getPendingCount())
            .todayCompletedCount(queueService.getTodayCompletedCount())
            .lastExecuteTime(queueService.getLastExecuteTime())
            .status(queueService.getStatus())
            .build();
        return ApiResponse.success(stats);
    }

    @PostMapping("/queue/pause")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> pauseQueue() {
        queueService.pause();
        return ApiResponse.success();
    }

    @PostMapping("/queue/resume")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> resumeQueue() {
        queueService.resume();
        return ApiResponse.success();
    }
}
