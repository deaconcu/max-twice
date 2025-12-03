package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.api.ratelimit.LimitType;
import com.prosper.learn.api.ratelimit.RateLimit;
import com.prosper.learn.api.v1.annotation.CurrentUser;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.business.service.application.FollowService;
import com.prosper.learn.dto.response.FolloweeDTO;
import com.prosper.learn.api.v1.annotation.JsonParam;
import com.prosper.learn.persistence.dataobject.UserDO;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 关注功能接口
 * 从UsersController拆分出来的关注功能
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@RateLimit(capacity = 60, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public class FollowsController {

    private final FollowService followService;

    /**
     * 关注用户
     * 映射: POST /user/follow → POST /api/v1/follows
     */
    @PostMapping("/follows")
    @SaCheckLogin
    public ApiResponse<Void> follow(
            @JsonParam("followeeId") @NotNull(message = "被关注用户ID不能为空")
            @Positive(message = "被关注用户ID必须大于0")
            Long followeeId,
            @CurrentUser UserDO currentUser) {
        followService.follow(currentUser, followeeId);
        return ApiResponse.success();
    }

    /**
     * 取消关注
     * 映射: DELETE /user/follow → DELETE /api/v1/follows/{followeeId}
     */
    @DeleteMapping("/follows/{followeeId}")
    @SaCheckLogin
    public ApiResponse<Void> unfollow(
            @PathVariable @NotNull(message = "被关注用户ID不能为空")
            @Positive(message = "被关注用户ID必须大于0")
            Long followeeId,
            @CurrentUser UserDO currentUser) {
        followService.unfollow(currentUser.getId(), followeeId);
        return ApiResponse.success();
    }

    /**
     * 获取关注列表
     * 映射: GET /user/followee → GET /api/v1/users/{userId}/followees
     */
    @GetMapping("/users/{userId}/followees")
    public ApiResponse<List<FolloweeDTO>> getFollowees(
            @PathVariable @NotNull(message = "用户ID不能为空")
            @Positive(message = "用户ID必须大于0")
            Long userId,
            @RequestParam(required = false) String lastCreateTime) {
        LocalDateTime time;
        if (lastCreateTime == null || lastCreateTime.isBlank()) {
            // 如果没有提供lastCreateTime，使用当前时间（加载第一页）
            time = LocalDateTime.now();
        } else {
            // 解析提供的时间
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            time = LocalDateTime.parse(lastCreateTime, formatter);
        }

        List<FolloweeDTO> followees = followService.getFollowees(userId, time);
        return ApiResponse.success(followees);
    }
}