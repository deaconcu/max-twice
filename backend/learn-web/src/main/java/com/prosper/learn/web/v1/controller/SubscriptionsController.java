package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.application.service.UserService;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.application.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.prosper.learn.web.v1.annotation.JsonParam;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 订阅管理接口
 * 从UsersController拆分出来的订阅功能
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
@Validated
@RateLimit(capacity = 60, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public class SubscriptionsController {

    private final UserService userService;

    /**
     * 获取用户订阅
     * 映射: GET /user/subscription → GET /api/v1/users/{userId}/subscriptions
     */
    @GetMapping("/users/{userId}/subscriptions")
    public ApiResponse<Object> getUserSubscriptions(
            @PathVariable @NotNull(message = "用户ID不能为空")
            @Positive(message = "用户ID必须大于0")
            Long userId) {
        Object subscriptions = userService.getUserSubscriptions(userId);
        return ApiResponse.success(subscriptions);
    }

    /**
     * 添加订阅
     * 映射: POST /user/subscription → POST /api/v1/users/current/subscriptions
     */
    @PostMapping("/users/current/subscriptions")
    @SaCheckLogin
    public ApiResponse<Void> subscribe(
            @JsonParam("courseId") @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @CurrentUser UserDO currentUser) {
        userService.subscribe(currentUser.getId(), courseId);
        return ApiResponse.success();
    }

    /**
     * 取消订阅
     * 映射: DELETE /user/subscription → DELETE /api/v1/users/current/subscriptions/{courseId}
     */
    @DeleteMapping("/users/current/subscriptions/{courseId}")
    @SaCheckLogin
    public ApiResponse<Void> unsubscribe(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @CurrentUser UserDO currentUser) {
        userService.unsubscribe(currentUser.getId(), courseId);
        return ApiResponse.success();
    }
}