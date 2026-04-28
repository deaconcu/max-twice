package com.twicemax.web.v2.controller.admin;

import com.twicemax.application.dto.response.KeysetPageResponse;
import com.twicemax.application.dto.response.user.UserAdminDTO;
import com.twicemax.application.service.UserService;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.v2.annotation.CurrentUser;
import com.twicemax.web.v2.annotation.OperationLog;
import com.twicemax.web.v2.annotation.RequireRole;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.twicemax.shared.domain.Enums.*;

/**
 * 用户管理后台接口
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@RequireRole(UserRole.ADMIN)
@Validated
public class AdminUserController {

    private final UserService userService;

    /**
     * 获取用户列表（分页）
     * GET /api/v2/admin/users?offsetId=0
     */
    @GetMapping("/users")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public KeysetPageResponse<UserAdminDTO> getUsers(
            @RequestParam(required = false) @Min(value = 0, message = "偏移ID不能小于0") Long offsetId,
            @CurrentUser UserDO currentUser) {
        return userService.getUsers(offsetId, 20);
    }

    /**
     * 按ID获取用户详情（管理员）
     * GET /api/v2/admin/users/{userId}
     */
    @GetMapping("/users/{userId}")
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public UserAdminDTO getUserById(
            @PathVariable @NotNull(message = "用户ID不能为空")
            @Positive(message = "用户ID必须大于0")
            Long userId,
            @CurrentUser UserDO currentUser) {
        return userService.getUserForAdmin(userId);
    }

    /**
     * 搜索用户（管理员）
     * GET /api/v2/admin/users/search?name=xxx
     */
    @GetMapping("/users/search")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<UserAdminDTO> searchUsers(
            @RequestParam @NotNull(message = "搜索名称不能为空") String name,
            @CurrentUser UserDO currentUser) {
        return userService.searchUsersForAdmin(name);
    }

    /**
     * 修改用户角色
     * POST /api/v2/admin/users/{id}/role
     */
    @PostMapping("/users/{id}/role")
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    @Transactional
    @OperationLog(
        module = "用户管理",
        type = "修改用户角色",
        level = OperationLevel.HIGH,
        targetType = "User",
        targetId = "#id"
    )
    public UserAdminDTO setUserRole(
            @PathVariable @NotNull(message = "用户ID不能为空")
            @Positive(message = "用户ID必须大于0")
            Long id,
            @RequestParam @NotNull(message = "角色代码不能为空") Integer roleCode,
            @CurrentUser UserDO currentUser) {
        return userService.setUserRole(id, roleCode, currentUser);
    }

    /**
     * 更新用户状态（封禁/解封）
     * PUT /api/v2/admin/users/{id}/state?ban=true
     */
    @PutMapping("/users/{id}/state")
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    @Transactional
    @OperationLog(
        module = "用户管理",
        type = "#ban ? '封禁用户' : '解封用户'",
        level = OperationLevel.HIGH,
        targetType = "User",
        targetId = "#id"
    )
    public UserAdminDTO updateUserState(
            @PathVariable @NotNull(message = "用户ID不能为空")
            @Positive(message = "用户ID必须大于0")
            Long id,
            @RequestParam @NotNull(message = "封禁状态不能为空") Boolean ban,
            @CurrentUser UserDO currentUser) {
        return userService.updateUserState(id, ban, currentUser);
    }

    /**
     * 封禁/解封用户（备用接口，兼容旧版本）
     * POST /api/v2/admin/users/{id}/ban
     */
    @PostMapping("/users/{id}/ban")
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    @Transactional
    @OperationLog(
        module = "用户管理",
        type = "#ban ? '封禁用户' : '解封用户'",
        level = OperationLevel.HIGH,
        targetType = "User",
        targetId = "#id"
    )
    public UserAdminDTO banUser(
            @PathVariable @NotNull(message = "用户ID不能为空")
            @Positive(message = "用户ID必须大于0")
            Long id,
            @RequestParam @NotNull(message = "封禁状态不能为空") Boolean ban,
            @CurrentUser UserDO currentUser) {
        return userService.updateUserState(id, ban, currentUser);
    }
}
