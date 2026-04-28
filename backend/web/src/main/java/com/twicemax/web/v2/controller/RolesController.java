package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.request.CreateRoleRequest;
import com.twicemax.application.dto.response.role.RoleDTO;
import com.twicemax.application.dto.v2.CreateAcceptedResponse;
import com.twicemax.application.dto.v2.Cursor;
import com.twicemax.application.dto.v2.CursorPage;
import com.twicemax.application.service.RoleService;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.CurrentUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 角色（专业）接口（v2）
 *
 * <p>遵循 v2 API 规范：
 * <ul>
 *   <li>路径前缀 {@code /v2/roles}</li>
 *   <li>响应 body 直接为资源对象 / {@link CursorPage} / 数组，不再使用 ApiResponse wrapper</li>
 *   <li>POST 申请类创建返回 {@code 202 Accepted + {id}}（v2 规范 2.5）</li>
 *   <li>错误由 {@code V2ApiExceptionHandler} 统一为 {@code {error: {code, message}}}</li>
 * </ul>
 *
 * <p>说明：admin 接口暂未迁移，仍由原 v1 路径处理。
 */
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Slf4j
@Validated
public class RolesController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final RoleService roleService;

    /**
     * 获取角色列表（已发布）
     * 支持按主分类 / 主+子分类筛选，游标分页
     */
    @GetMapping
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CursorPage<RoleDTO> list(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) @Positive(message = "主分类必须大于0") Integer mainCategory,
            @RequestParam(required = false) @Positive(message = "子分类必须大于0") Integer subCategory) {

        int pageSize = DEFAULT_PAGE_SIZE;
        int fetchLimit = pageSize + 1;
        List<RoleDTO> roleList;

        if (mainCategory != null && subCategory != null) {
            roleList = roleService.getListByCategoryAndLastId(mainCategory, subCategory, cursor, fetchLimit);
        } else if (mainCategory != null) {
            roleList = roleService.getListByMainCategoryAndLastId(mainCategory, cursor, fetchLimit);
        } else {
            roleList = roleService.getApprovedByLastId(cursor, fetchLimit);
        }

        boolean hasMore = roleList.size() > pageSize;
        if (hasMore) {
            roleList = roleList.subList(0, pageSize);
        }

        String nextCursor = (hasMore && !roleList.isEmpty())
                ? Cursor.of(roleList.get(roleList.size() - 1).getId()).encode()
                : null;

        return CursorPage.of(roleList, hasMore, nextCursor);
    }

    /**
     * 获取角色详情
     */
    @GetMapping("/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public RoleDTO get(
            @PathVariable @NotNull(message = "角色ID不能为空")
            @Positive(message = "角色ID必须大于0") Long id,
            @CurrentUser UserDO currentUser) {
        return roleService.getById(id, true, currentUser.getId());
    }

    /**
     * 搜索角色（不分页，固定数量）
     */
    @GetMapping("/search")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<RoleDTO> search(
            @RequestParam @NotBlank(message = "搜索关键词不能为空") String keyword) {
        return roleService.searchByKeyword(keyword);
    }

    /**
     * 热门角色（不分页，固定数量）
     */
    @GetMapping("/hot")
    public List<RoleDTO> hot(
            @RequestParam(value = "limit", defaultValue = "10")
            @Positive(message = "限制数量必须大于0") Integer limit) {
        return roleService.getHotRoles(limit);
    }

    /**
     * 创建角色（申请，需审核）
     * 按 v2 规范 2.5：返回 202 Accepted + {id}
     */
    @PostMapping
    @SaCheckLogin
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<CreateAcceptedResponse> create(
            @Valid @RequestBody CreateRoleRequest request,
            @CurrentUser UserDO currentUser) {
        Long id = roleService.create(request, currentUser);
        return ResponseEntity.accepted().body(new CreateAcceptedResponse(id));
    }
}
