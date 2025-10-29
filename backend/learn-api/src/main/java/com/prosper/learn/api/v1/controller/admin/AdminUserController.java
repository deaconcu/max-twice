package com.prosper.learn.api.v1.controller.admin;

import com.prosper.learn.api.v1.annotation.CurrentUser;
import com.prosper.learn.api.v1.annotation.OperationLog;
import com.prosper.learn.api.v1.annotation.RequireRole;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums.OperationLevel;
import com.prosper.learn.common.Enums.UserRole;
import com.prosper.learn.domain.service.business.UserService;
import com.prosper.learn.dto.response.UserDTO;
import com.prosper.learn.persistence.dataobject.UserDO;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理后台接口
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@RequireRole(UserRole.ADMIN)
@Validated
public class AdminUserController {

    private final UserService userService;

    /**
     * 获取用户列表（分页）
     * GET /api/v1/admin/users?offsetId=0
     */
    @GetMapping("/users")
    @RequireRole(UserRole.ADMIN)
    public ApiResponse<List<UserDTO>> getUsers(
            @RequestParam(required = false) @Min(value = 0, message = "偏移ID不能小于0") Long offsetId,
            @CurrentUser UserDO currentUser) {
        List<UserDTO> users = userService.getUsers(offsetId, 20);
        return ApiResponse.success(users);
    }

    /**
     * 按ID获取用户详情（管理员）
     * GET /api/v1/admin/users/{userId}
     */
    @GetMapping("/users/{userId}")
    @RequireRole(UserRole.ADMIN)
    public ApiResponse<UserDTO> getUserById(
            @PathVariable @NotNull(message = "用户ID不能为空")
            @Positive(message = "用户ID必须大于0")
            Long userId,
            @CurrentUser UserDO currentUser) {
        // 复用现有方法：返回用户详情（包括被屏蔽用户）
        UserDTO user = userService.getUser(userId);
        return ApiResponse.success(user);
    }

    /**
     * 搜索用户（管理员）
     * GET /api/v1/admin/users/search?name=xxx
     * TODO: 当前实现可能不会过滤被屏蔽用户，需要验证。如果过滤了，需要新增 Service 方法
     */
    @GetMapping("/users/search")
    @RequireRole(UserRole.ADMIN)
    public ApiResponse<List<UserDTO>> searchUsers(
            @RequestParam @NotNull(message = "搜索名称不能为空") String name,
            @CurrentUser UserDO currentUser) {
        // 复用现有方法
        List<UserDTO> users = userService.searchUsers(name);
        return ApiResponse.success(users);
    }

    /**
     * 修改用户角色
     * POST /api/v1/admin/users/{id}/role
     */
    @PostMapping("/users/{id}/role")
    @RequireRole(UserRole.ADMIN)
    @OperationLog(
        module = "用户管理",
        type = "修改用户角色",
        level = OperationLevel.HIGH,
        targetType = "User",
        targetId = "#id"
    )
    public ApiResponse<UserDTO> setUserRole(
            @PathVariable @NotNull(message = "用户ID不能为空")
            @Positive(message = "用户ID必须大于0")
            Long id,
            @RequestParam @NotNull(message = "角色代码不能为空") Integer roleCode,
            @CurrentUser UserDO currentUser) {

        UserDTO user = userService.setUserRole(id, roleCode, currentUser);
        return ApiResponse.success(user);
    }

    /**
     * 更新用户状态（封禁/解封）
     * PUT /api/v1/admin/users/{id}/state?ban=true
     */
    @PutMapping("/users/{id}/state")
    @RequireRole(UserRole.ADMIN)
    @OperationLog(
        module = "用户管理",
        type = "#ban ? '封禁用户' : '解封用户'",
        level = OperationLevel.HIGH,
        targetType = "User",
        targetId = "#id"
    )
    public ApiResponse<UserDTO> updateUserState(
            @PathVariable @NotNull(message = "用户ID不能为空")
            @Positive(message = "用户ID必须大于0")
            Long id,
            @RequestParam @NotNull(message = "封禁状态不能为空") Boolean ban,
            @CurrentUser UserDO currentUser) {

        UserDTO user = userService.updateUserState(id, ban, currentUser);
        return ApiResponse.success(user);
    }

    /**
     * 封禁/解封用户（备用接口，兼容旧版本）
     * POST /api/v1/admin/users/{id}/ban
     */
    @PostMapping("/users/{id}/ban")
    @RequireRole(UserRole.ADMIN)
    @OperationLog(
        module = "用户管理",
        type = "#ban ? '封禁用户' : '解封用户'",
        level = OperationLevel.HIGH,
        targetType = "User",
        targetId = "#id"
    )
    public ApiResponse<UserDTO> banUser(
            @PathVariable @NotNull(message = "用户ID不能为空")
            @Positive(message = "用户ID必须大于0")
            Long id,
            @RequestParam @NotNull(message = "封禁状态不能为空") Boolean ban,
            @CurrentUser UserDO currentUser) {

        UserDTO user = userService.updateUserState(id, ban, currentUser);
        return ApiResponse.success(user);
    }
}
