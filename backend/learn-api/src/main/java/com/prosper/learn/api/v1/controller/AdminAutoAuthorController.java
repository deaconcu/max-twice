package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.domain.config.SystemProperties;
import com.prosper.learn.domain.service.autoauthor.AutoAuthorGenerationService;
import com.prosper.learn.domain.service.autoauthor.AutoAuthorQueueService;
import com.prosper.learn.domain.service.autoauthor.AutoAuthorScanner;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AutoAuthor 管理接口
 */
@RestController
@RequestMapping("/api/v1/admin/auto-author")
@RequiredArgsConstructor
public class AdminAutoAuthorController {

    private final AutoAuthorScanner scanner;
    private final AutoAuthorQueueService queueService;
    private final AutoAuthorGenerationService generationService;
    private final SystemProperties systemProperties;

    @PostMapping("/scan")
    @SaCheckLogin
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
    public ApiResponse<Void> enqueue(@PathVariable("nodeId") long nodeId) {
        if (!systemProperties.getAutoAuthor().isEnabled()) return ApiResponse.success();
        queueService.enqueue(nodeId);
        return ApiResponse.success();
    }

    @PostMapping("/session/reset")
    @SaCheckLogin
    public ApiResponse<Void> resetSession() {
        generationService.resetSession();
        return ApiResponse.success();
    }

    @DeleteMapping("/queue")
    @SaCheckLogin
    public ApiResponse<String> clearQueue() {
        long count = queueService.clear();
        String message = count > 0 ?
            String.format("已清空队列，共删除 %d 个待处理节点", count) :
            "队列已经是空的";
        return ApiResponse.success(message);
    }
}
