package com.twicemax.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.response.FolloweeDTO;
import com.twicemax.application.service.FollowService;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v1.annotation.CurrentUser;
import com.twicemax.application.dto.ApiResponse;
import com.twicemax.web.v1.annotation.JsonParam;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 关注功能接口
 * 从UsersController拆分出来的关注功能
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Validated
public class FollowsController {

    private final FollowService followService;

    /**
     * 关注用户
     * 映射: POST /user/follow → POST /api/v1/follows
     */
    @PostMapping("/follows")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
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
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
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
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<List<FolloweeDTO>> getFollowees(
            @PathVariable @NotNull(message = "用户ID不能为空")
            @Positive(message = "用户ID必须大于0")
            Long userId,
            @RequestParam(required = false) Long lastId) {
        // lastId 为 null 时查询第一页
        List<FolloweeDTO> followees = followService.getFollowees(userId, lastId);
        return ApiResponse.success(followees);
    }
}