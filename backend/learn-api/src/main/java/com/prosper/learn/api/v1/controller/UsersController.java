package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.domain.service.business.PostService;
import com.prosper.learn.domain.service.business.UserService;
import com.prosper.learn.dto.response.UserDTO;
import com.prosper.learn.dto.request.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import java.util.List;

/**
 * 用户管理接口
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
@Validated
public class UsersController {

    private final UserService userService;
    private final PostService postService;

    /**
     * 获取当前用户信息
     * 映射: GET /self → GET /api/v1/users/current
     */
    @GetMapping("/users/current")
    public ApiResponse<UserDTO> getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserDTO userDTO = userService.getUser(userId);
        return ApiResponse.success(userDTO);
    }

    /**
     * 修改当前用户信息
     * 映射: POST /self → PUT /api/v1/users/current
     */
    @PutMapping("/users/current")
    public ApiResponse<Void> updateCurrentUser(@RequestBody @Valid UpdateUserRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        userService.updateCurrentUser(userId, request.getName(), request.getBiography());
        return ApiResponse.success();
    }

    /**
     * 获取用户信息
     * 映射: GET /user/{id} → GET /api/v1/users/{id}
     */
    @GetMapping("/users/{id}")
    public ApiResponse<UserDTO> getUser(
            @PathVariable @NotNull(message = "用户ID不能为空")
            @Positive(message = "用户ID必须大于0")
            Long id) {
        Long viewerId = StpUtil.getLoginIdAsLong();
        UserDTO userDTO = userService.getUser(id, viewerId);
        return ApiResponse.success(userDTO);
    }

    /**
     * 搜索用户
     * 映射: GET /user?name=xxx → GET /api/v1/users/search?name=xxx
     */
    @GetMapping("/users/search")
    public ApiResponse<List<UserDTO>> searchUsers(@RequestParam @NotBlank(message = "搜索名称不能为空") String name) {
        List<UserDTO> users = userService.searchUsers(name);
        return ApiResponse.success(users);
    }

    /**
     * 用户注册
     * 映射: POST /user → POST /api/v1/auth/register
     */
    @PostMapping("/auth/register")
    public ApiResponse<Void> register(@RequestBody @Valid RegisterRequest request) {
        userService.register(request.getEmail(), request.getPassword());
        return ApiResponse.success();
    }

    /**
     * 用户登录
     * 映射: POST /login → POST /api/v1/auth/login
     */
    @PostMapping("/auth/login")
    public ApiResponse<UserDTO> login(@RequestBody @Valid LoginRequest request) {
        // Service 负责业务验证
        UserDTO userDTO = userService.validateLogin(request.getEmail(), request.getPassword());
        
        // Controller 负责认证状态管理
        StpUtil.login(userDTO.getId());
        
        return ApiResponse.success(userDTO);
    }

    /**
     * 邮箱验证
     * 映射: POST /user/validate → POST /api/v1/auth/validate-email
     */
    @PostMapping("/auth/validate-email")
    public ApiResponse<UserDTO> validateEmail(@RequestBody @Valid VerifyEmailRequest request) {
        // Service 负责验证逻辑
        UserDTO userDTO = userService.validateEmail(request.getEmail(), request.getCode());
        
        // Controller 负责认证状态管理（验证成功后自动登录）
        StpUtil.login(userDTO.getId());
        
        return ApiResponse.success(userDTO);
    }

    /**
     * 获取用户文章或内容
     * 映射: GET /user/article → GET /api/v1/users/{userId}/posts?type=article
     * 映射: GET /user/contents → GET /api/v1/users/{userId}/posts?type=content
     */
    @GetMapping("/users/{userId}/posts")
    public ApiResponse<Object> getUserPosts(
            @PathVariable @NotNull(message = "用户ID不能为空") @Positive(message = "用户ID必须大于0") Long userId,
            @RequestParam @NotNull(message = "最后ID不能为空") @Min(value = 0, message = "最后ID不能小于0") Long lastId,
            @RequestParam(required = false, defaultValue = "article") String type) {

        Object posts = postService.getUserPosts(userId, lastId, type);
        return ApiResponse.success(posts);
    }

    /**
     * 管理员获取用户列表（分页）
     * 映射: GET /api/v1/admin/users?offsetId=0
     */
    @GetMapping("/admin/users")
    public ApiResponse<List<UserDTO>> getUsers(
            @RequestParam(required = false) @Min(value = 0, message = "偏移ID不能小于0") Long offsetId) {
        List<UserDTO> users = userService.getUsers(offsetId, 20);
        return ApiResponse.success(users);
    }

    /**
     * 管理员更新用户状态（屏蔽/恢复）
     * 映射: PUT /api/v1/admin/users/{id}/state?ban=true
     */
    @PutMapping("/admin/users/{id}/state")
    public ApiResponse<UserDTO> updateUserState(
            @PathVariable @NotNull(message = "用户ID不能为空") @Positive(message = "用户ID必须大于0") Long id,
            @RequestParam Boolean ban) {
        UserDTO userDTO = userService.updateUserState(id, ban);
        return ApiResponse.success(userDTO);
    }
}