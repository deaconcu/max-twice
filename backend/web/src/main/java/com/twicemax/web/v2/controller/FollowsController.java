package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.response.FolloweeDTO;
import com.twicemax.application.service.FollowService;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.CurrentUser;
import com.twicemax.web.v2.annotation.JsonParam;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 关注功能接口
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Validated
public class FollowsController {

    private final FollowService followService;

    @PostMapping("/follows")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> follow(
            @JsonParam("followeeId") @NotNull(message = "被关注用户ID不能为空")
            @Positive(message = "被关注用户ID必须大于0")
            Long followeeId,
            @CurrentUser UserDO currentUser) {
        followService.follow(currentUser, followeeId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/follows/{followeeId}")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> unfollow(
            @PathVariable @NotNull(message = "被关注用户ID不能为空")
            @Positive(message = "被关注用户ID必须大于0")
            Long followeeId,
            @CurrentUser UserDO currentUser) {
        followService.unfollow(currentUser.getId(), followeeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}/followees")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<FolloweeDTO> getFollowees(
            @PathVariable @NotNull(message = "用户ID不能为空")
            @Positive(message = "用户ID必须大于0")
            Long userId,
            @RequestParam(required = false) String cursor) {
        return followService.getFollowees(userId, cursor);
    }
}
