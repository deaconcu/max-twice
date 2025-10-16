package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.domain.service.business.FollowService;
import com.prosper.learn.dto.response.FolloweeDTO;
import com.prosper.learn.api.v1.annotation.JsonParam;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 关注功能接口
 * 从UsersController拆分出来的关注功能
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class FollowsController {

    private final FollowService followService;

    /**
     * 关注用户
     * 映射: POST /user/follow → POST /api/v1/follows
     */
    @PostMapping("/follows")
    public ApiResponse<Void> follow(
            @JsonParam("followeeId") @NotNull(message = "被关注用户ID不能为空")
            @Positive(message = "被关注用户ID必须大于0")
            Long followeeId) {
        Long followerId = StpUtil.getLoginIdAsLong();
        followService.follow(followerId, followeeId);
        return ApiResponse.success();
    }

    /**
     * 取消关注
     * 映射: DELETE /user/follow → DELETE /api/v1/follows/{followeeId}
     */
    @DeleteMapping("/follows/{followeeId}")
    public ApiResponse<Void> unfollow(
            @PathVariable @NotNull(message = "被关注用户ID不能为空")
            @Positive(message = "被关注用户ID必须大于0")
            Long followeeId) {
        Long followerId = StpUtil.getLoginIdAsLong();
        followService.unfollow(followerId, followeeId);
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
            @RequestParam @NotBlank(message = "最后创建时间不能为空") String lastCreateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime time = LocalDateTime.parse(lastCreateTime, formatter);

        List<FolloweeDTO> followees = followService.getFollowees(userId, time);
        return ApiResponse.success(followees);
    }
}