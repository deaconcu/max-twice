package com.twicemax.web.v2.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.twicemax.analytics.monitoring.service.ErrorLogService;
import com.twicemax.application.dto.request.FrontendErrorRequest;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 错误上报接口
 */
@RestController
@RequestMapping("/errors")
@Slf4j
@RequiredArgsConstructor
public class ErrorReportController {

    private final ErrorLogService errorLogService;

    @PostMapping("/frontend")
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public ResponseEntity<Void> reportFrontendError(
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

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/test-exception")
    public ResponseEntity<Void> testException() {
        throw new RuntimeException("这是一个测试异常");
    }

    private Long getCurrentUserId() {
        try {
            if (StpUtil.isLogin()) {
                return StpUtil.getLoginIdAsLong();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
