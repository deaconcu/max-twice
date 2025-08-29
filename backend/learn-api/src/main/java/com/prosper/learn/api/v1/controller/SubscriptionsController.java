package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.domain.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订阅管理接口
 * 从UsersController拆分出来的订阅功能
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
public class SubscriptionsController {

    private final UserService userService;

    /**
     * 获取用户订阅
     * 映射: GET /user/subscription → GET /api/v1/users/{userId}/subscriptions
     */
    @GetMapping("/users/{userId}/subscriptions")
    public ApiResponse<Object> getUserSubscriptions(@PathVariable Long userId) {
        Object subscriptions = userService.getUserSubscriptions(userId);
        return ApiResponse.success(subscriptions);
    }

    /**
     * 添加订阅
     * 映射: POST /user/subscription → POST /api/v1/users/current/subscriptions
     */
    @PostMapping("/users/current/subscriptions")
    public ApiResponse<Object> subscribe(@RequestParam Long courseId) {
        long userId = StpUtil.getLoginIdAsLong();
        Object result = userService.subscribe(userId, courseId);
        return ApiResponse.success(result);
    }

    /**
     * 批量更新订阅
     * 映射: PUT /user/subscription → PUT /api/v1/users/current/subscriptions
     */
    @PutMapping("/users/current/subscriptions")
    public ApiResponse<Object> updateSubscriptions(@RequestParam String subscription) {
        long userId = StpUtil.getLoginIdAsLong();
        Object result = userService.updateSubscriptions(userId, subscription);
        return ApiResponse.success(result);
    }

    /**
     * 取消订阅
     * 映射: DELETE /user/subscription → DELETE /api/v1/users/current/subscriptions/{courseId}
     */
    @DeleteMapping("/users/current/subscriptions/{courseId}")
    public ApiResponse<Object> unsubscribe(@PathVariable Long courseId) {
        long userId = StpUtil.getLoginIdAsLong();
        Object result = userService.unsubscribe(userId, courseId);
        return ApiResponse.success(result);
    }
}