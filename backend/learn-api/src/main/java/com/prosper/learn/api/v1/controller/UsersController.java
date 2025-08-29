package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.domain.service.business.UserService;
import com.prosper.learn.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理接口
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
public class UsersController {

    private final UserService userService;

    /**
     * 获取当前用户信息
     * 映射: GET /self → GET /api/v1/users/current
     */
    @GetMapping("/users/current")
    public ApiResponse<UserDTO> getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserDTO userDTO = userService.getCurrentUser(userId);
        return ApiResponse.success(userDTO);
    }

    /**
     * 修改当前用户信息
     * 映射: POST /self → PUT /api/v1/users/current
     */
    @PutMapping("/users/current")
    public ApiResponse<Void> updateCurrentUser(@RequestParam String name, @RequestParam String biography) {
        Long userId = StpUtil.getLoginIdAsLong();
        userService.updateCurrentUser(userId, name, biography);
        return ApiResponse.success();
    }

    /**
     * 获取用户信息
     * 映射: GET /user/{id} → GET /api/v1/users/{id}
     */
    @GetMapping("/users/{id}")
    public ApiResponse<UserDTOV3> getUser(@PathVariable Long id) {
        Long viewerId = StpUtil.getLoginIdAsLong();
        UserDTOV3 userDTO = userService.getUser(id, viewerId);
        return ApiResponse.success(userDTO);
    }

    /**
     * 搜索用户
     * 映射: GET /user?name=xxx → GET /api/v1/users/search?name=xxx
     */
    @GetMapping("/users/search")
    public ApiResponse<List<UserDTOV4>> searchUsers(@RequestParam String name) {
        List<UserDTOV4> users = userService.searchUsers(name);
        return ApiResponse.success(users);
    }

    /**
     * 用户注册
     * 映射: POST /user → POST /api/v1/auth/register
     */
    @PostMapping("/auth/register")
    public ApiResponse<Void> register(@RequestParam String userName, @RequestParam String email, @RequestParam String password) {
        userService.register(userName, email, password);
        return ApiResponse.success();
    }

    /**
     * 用户登录
     * 映射: POST /login → POST /api/v1/auth/login
     */
    @PostMapping("/auth/login")
    public ApiResponse<UserDTOV2> login(@RequestParam String email, @RequestParam String password) {
        // Service 负责业务验证
        UserDTOV2 userDTO = userService.validateLogin(email, password);
        
        // Controller 负责认证状态管理
        StpUtil.login(userDTO.getId());
        
        return ApiResponse.success(userDTO);
    }

    /**
     * 邮箱验证
     * 映射: POST /user/validate → POST /api/v1/auth/validate-email
     */
    @PostMapping("/auth/validate-email")
    public ApiResponse<UserDTO> validateEmail(@RequestParam String email, @RequestParam String code) {
        // Service 负责验证逻辑
        UserDTO userDTO = userService.validateEmail(email, code);
        
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
            @PathVariable Long userId, 
            @RequestParam Long lastId,
            @RequestParam(required = false, defaultValue = "article") String type) {
        
        Object posts = userService.getUserPosts(userId, lastId, type);
        return ApiResponse.success(posts);
    }
}