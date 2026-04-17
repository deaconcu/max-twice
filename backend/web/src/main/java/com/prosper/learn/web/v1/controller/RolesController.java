package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.analytics.ranking.scheduler.RoleRankingScheduler;
import com.prosper.learn.application.dto.request.CreateRoleRequest;
import com.prosper.learn.application.dto.response.KeysetPageResponse;
import com.prosper.learn.application.dto.response.role.RoleDTO;
import com.prosper.learn.application.service.RoleService;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.application.dto.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 角色管理接口
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
@Validated
public class RolesController {

    private final RoleService roleService;
    private final RoleRankingScheduler roleRankingScheduler;

    /**
     * 获取角色列表
     * 支持按分类筛选和游标分页
     * 不传分类参数时返回所有已发布角色
     */
    @GetMapping("/roles")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<KeysetPageResponse<RoleDTO>> getRoles(
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false) @Positive(message = "主分类必须大于0") Integer mainCategory,
            @RequestParam(required = false) @Positive(message = "子分类必须大于0") Integer subCategory) {

        int pageSize = 20;
        int limit = pageSize + 1;
        List<RoleDTO> roleList;

        if (mainCategory != null && subCategory != null) {
            // 按主分类+子分类获取
            roleList = roleService.getListByCategoryAndLastId(mainCategory, subCategory, lastId, limit);
        } else if (mainCategory != null) {
            // 按主分类获取
            roleList = roleService.getListByMainCategoryAndLastId(mainCategory, lastId, limit);
        } else {
            // 获取所有已发布角色
            roleList = roleService.getApprovedByLastId(lastId, limit);
        }

        // 判断是否有更多数据
        boolean hasMore = roleList.size() > pageSize;

        // 如果有更多数据，只返回 pageSize 条
        if (hasMore) {
            roleList = roleList.subList(0, pageSize);
        }

        // 提取下一页的游标
        Long nextLastId = null;
        if (hasMore && !roleList.isEmpty()) {
            nextLastId = roleList.get(roleList.size() - 1).getId();
        }

        KeysetPageResponse<RoleDTO> response = KeysetPageResponse.of(roleList, hasMore, null, nextLastId);
        return ApiResponse.query(response);
    }

    /**
     * 获取角色详情
     * 映射: GET /role?id=123 → GET /api/v1/roles/{id}
     */
    @GetMapping("/roles/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<RoleDTO> getRole(
            @PathVariable @NotNull(message = "角色ID不能为空")
            @Positive(message = "角色ID必须大于0")
            Long id,
            @CurrentUser UserDO currentUser) {
        RoleDTO role = roleService.getById(id, true, currentUser.getId());
        return ApiResponse.query(role);
    }

    /**
     * 搜索角色
     */
    @GetMapping("/roles/search")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Object> searchRoles(
            @RequestParam @NotBlank(message = "搜索关键词不能为空") String keyword) {
        List<RoleDTO> roleList = roleService.searchByKeyword(keyword);
        return ApiResponse.query(roleList);
    }

    /**
     * 创建角色
     * 映射: POST /role → POST /api/v1/roles
     */
    @PostMapping("/roles")
    @SaCheckLogin
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Object> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            @CurrentUser UserDO currentUser) {
        roleService.create(request, currentUser);
        return ApiResponse.success();
    }
    /**
     * 热门角色
     * 映射: GET /role/hot → GET /api/v1/roles/hot?limit=10
     */
    @GetMapping("/roles/hot")
    public ApiResponse<Object> getHotRoles(
            @RequestParam(value = "limit", defaultValue = "10")
            @Positive(message = "限制数量必须大于0")
            Integer limit) {
        log.info("开始获取热门角色，limit: {}", limit);
        List<RoleDTO> hotRoles = roleService.getHotRoles(limit);
        log.info("成功获取热门角色数量: {}", hotRoles.size());
        return ApiResponse.query(hotRoles);
    }
}