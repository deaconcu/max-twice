package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums;
import com.prosper.learn.domain.service.business.PostService;
import com.prosper.learn.domain.service.business.UserService;
import com.prosper.learn.dto.response.PostDTO;
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
     * 映射: GET /user/{username} → GET /api/v1/users/{username}
     */
    @GetMapping("/users/{username}")
    public ApiResponse<UserDTO> getUser(
            @PathVariable @NotBlank(message = "用户名不能为空") String username) {
        Long viewerId = StpUtil.getLoginIdAsLong();
        UserDTO userDTO = userService.getUserByUsername(username, viewerId);
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
     * 获取用户文章或内容（仅已发布）
     * 映射: GET /user/article → GET /api/v1/users/{userId}/posts?type=2
     * 映射: GET /user/contents → GET /api/v1/users/{userId}/posts?type=1
     */
    @GetMapping("/users/{userId}/posts")
    public ApiResponse<List<PostDTO>> getUserPosts(
            @PathVariable @NotNull(message = "用户ID不能为空") @Positive(message = "用户ID必须大于0") Long userId,
            @RequestParam @NotNull(message = "最后ID不能为空") @Min(value = 0, message = "最后ID不能小于0") Long lastId,
            @RequestParam(required = false, defaultValue = "2") Integer type) {

        Enums.PostType postType = Enums.PostType.getByValue(type);
        if (postType == null) {
            return ApiResponse.error(400, "无效的帖子类型");
        }
        List<PostDTO> posts = postService.getUserPosts(userId, lastId, postType, Enums.ContentState.PUBLISHED.value());
        return ApiResponse.success(posts);
    }

    /**
     * 获取当前登录用户所有状态的文章或目录（用于个人中心内容管理）
     * 包含：待审核、已发布、审核拒绝、已屏蔽
     * GET /api/v1/users/me/posts?lastId=0&type=2
     */
    @GetMapping("/users/me/posts")
    public ApiResponse<List<PostDTO>> getCurrentUserAllPosts(
            @RequestParam @NotNull(message = "最后ID不能为空") @Min(value = 0, message = "最后ID不能小于0") Long lastId,
            @RequestParam(required = false, defaultValue = "2") Integer type) {

        Long currentUserId = StpUtil.getLoginIdAsLong();
        Enums.PostType postType = Enums.PostType.getByValue(type);
        if (postType == null) {
            return ApiResponse.error(400, "无效的帖子类型");
        }
        List<PostDTO> posts = postService.getUserPosts(currentUserId, lastId, postType, null);
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