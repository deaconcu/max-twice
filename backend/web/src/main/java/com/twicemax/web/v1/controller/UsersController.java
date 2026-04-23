package com.twicemax.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.request.UpdateUserRequest;
import com.twicemax.application.dto.response.ImageUploadResponse;
import com.twicemax.application.dto.response.user.UserBriefDTO;
import com.twicemax.application.dto.response.user.UserProfileDTO;
import com.twicemax.application.dto.response.user.UserPublicDTO;
import com.twicemax.application.service.ImageUploadService;
import com.twicemax.application.service.UserService;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v1.annotation.CurrentUser;
import com.twicemax.application.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户管理接口
 */
@RestController
@RequestMapping("/v1")
@Slf4j
@RequiredArgsConstructor
@Validated
public class UsersController {

    private final UserService userService;
    private final ImageUploadService imageUploadService;

    /**
     * 获取当前用户信息
     * 映射: GET /self → GET /api/v1/users/current
     */
    @GetMapping("/users/current")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<UserProfileDTO> getCurrentUser(@CurrentUser UserDO currentUser) {
        UserProfileDTO userDTO = userService.getUser(currentUser.getId());
        return ApiResponse.success(userDTO);
    }

    /**
     * 修改当前用户信息
     * 映射: POST /self → PUT /api/v1/users/current
     */
    @PutMapping("/users/current")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> updateCurrentUser(
            @RequestBody @Valid UpdateUserRequest request,
            @CurrentUser UserDO currentUser) {
        userService.updateCurrentUser(currentUser.getId(), request.getName(), request.getBiography(), request.getTimezone());
        return ApiResponse.success();
    }

    /**
     * 更新用户头像
     * POST /api/v1/users/avatar
     */
    @PostMapping("/users/avatar")
    @SaCheckLogin
    @RateLimit(capacity = 5, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<String> updateAvatar(
            @RequestParam("file") @NotNull(message = "文件不能为空") MultipartFile file,
            @CurrentUser UserDO currentUser) {
        log.info("用户 {} 开始更新头像", currentUser.getId());

        // 上传图片
        ImageUploadResponse response = imageUploadService.upload(file, currentUser.getId(), "avatar");

        // 更新用户头像
        userService.updateUserAvatar(currentUser.getId(), response.getFileUrl());

        log.info("用户 {} 头像更新成功: {}", currentUser.getId(), response.getFileUrl());
        return ApiResponse.success(response.getFileUrl());
    }

    /**
     * 获取用户信息
     * 映射: GET /user/{username} → GET /api/v1/users/{username}
     */
    @GetMapping("/users/{username}")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<UserPublicDTO> getUser(
            @PathVariable @NotBlank(message = "用户名不能为空") String username,
            @CurrentUser UserDO currentUser) {
        UserPublicDTO userDTO = userService.getUserByUsername(username, currentUser.getId());
        return ApiResponse.success(userDTO);
    }

    /**
     * 搜索用户
     * 映射: GET /user?name=xxx → GET /api/v1/users/search?name=xxx
     */
    @GetMapping("/users/search")
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public ApiResponse<List<UserBriefDTO>> searchUsers(@RequestParam @NotBlank(message = "搜索名称不能为空") String name) {
        List<UserBriefDTO> users = userService.searchUsers(name);
        return ApiResponse.success(users);
    }
}