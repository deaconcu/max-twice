package com.twicemax.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.twicemax.analytics.monitoring.service.ErrorLogService;
import com.twicemax.application.dto.ApiResponse;
import com.twicemax.application.dto.request.FrontendErrorRequest;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 错误上报接口
 */
@RestController
@RequestMapping("/v1/errors")
@Slf4j
@RequiredArgsConstructor
public class ErrorReportController {

    private final ErrorLogService errorLogService;

    /**
     * 前端错误上报
     */
    @PostMapping("/frontend")
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public ApiResponse<Void> reportFrontendError(
            @RequestBody FrontendErrorRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getCurrentUserId();
        String ip = IpUtils.getIpAddress(httpRequest);

        errorLogService.recordFrontendError(
                request.getErrorType(),
                request.getMessage(),
                request.getStackTrace(),
                request.getUrl(),
                userId,
                ip,
                request.getUserAgent(),
                request.getExtraData()
        );

        return ApiResponse.success();
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            if (StpUtil.isLogin()) {
                return StpUtil.getLoginIdAsLong();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 测试后端异常（仅开发环境使用）
     */
    @GetMapping("/test-exception")
    public ApiResponse<Void> testException() {
        throw new RuntimeException("这是一个测试异常");
    }
}
