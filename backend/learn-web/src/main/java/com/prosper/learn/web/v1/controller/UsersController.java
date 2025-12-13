package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.application.dto.request.LoginRequest;
import com.prosper.learn.application.dto.request.RegisterRequest;
import com.prosper.learn.application.dto.request.UpdateUserRequest;
import com.prosper.learn.application.dto.request.VerifyEmailRequest;
import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import com.prosper.learn.application.dto.response.user.UserProfileDTO;
import com.prosper.learn.application.dto.response.user.UserPublicDTO;
import com.prosper.learn.application.service.UserService;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.application.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户管理接口
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
@Validated
@RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public class UsersController {

    private final UserService userService;

    /**
     * 获取当前用户信息
     * 映射: GET /self → GET /api/v1/users/current
     */
    @GetMapping("/users/current")
    @SaCheckLogin
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
    public ApiResponse<Void> updateCurrentUser(
            @RequestBody @Valid UpdateUserRequest request,
            @CurrentUser UserDO currentUser) {
        userService.updateCurrentUser(currentUser.getId(), request.getName(), request.getBiography());
        return ApiResponse.success();
    }

    /**
     * 获取用户信息
     * 映射: GET /user/{username} → GET /api/v1/users/{username}
     */
    @GetMapping("/users/{username}")
    @SaCheckLogin
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

    /**
     * 用户注册
     * 映射: POST /user → POST /api/v1/auth/register
     */
    @PostMapping("/auth/register")
    @RateLimit(capacity = 5, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public ApiResponse<Void> register(@RequestBody @Valid RegisterRequest request) {
        userService.register(request.getEmail(), request.getPassword());
        return ApiResponse.success();
    }

    /**
     * 用户登录
     * 映射: POST /login → POST /api/v1/auth/login
     */
    @PostMapping("/auth/login")
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public ApiResponse<UserBriefDTO> login(@RequestBody @Valid LoginRequest request) {
        // Service 负责业务验证
        UserBriefDTO userDTO = userService.validateLogin(request.getEmail(), request.getPassword());

        // Controller 负责认证状态管理
        StpUtil.login(userDTO.getId());

        return ApiResponse.success(userDTO);
    }

    /**
     * 邮箱验证
     * 映射: POST /user/validate → POST /api/v1/auth/validate-email
     */
    @PostMapping("/auth/validate-email")
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public ApiResponse<UserProfileDTO> validateEmail(@RequestBody @Valid VerifyEmailRequest request) {
        // Service 负责验证逻辑
        UserProfileDTO userDTO = userService.validateEmail(request.getEmail(), request.getCode());

        // Controller 负责认证状态管理（验证成功后自动登录）
        StpUtil.login(userDTO.getId());

        return ApiResponse.success(userDTO);
    }
}