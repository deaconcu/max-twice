package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.application.dto.request.CreateRoadmapRequest;
import com.prosper.learn.application.dto.request.UpdateRoadmapRequest;
import com.prosper.learn.application.dto.response.roadmap.RoadmapDetailDTO;
import com.prosper.learn.application.dto.response.roadmap.RoadmapWithStatusDTO;
import com.prosper.learn.application.service.RoadmapService;
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

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 路线图接口
 * 从RoadmapClient迁移而来
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class RoadmapsController {

    private final RoadmapService roadmapService;

    /**
     * 获取职业路线图
     * 映射: GET /roadmap/list/{roleId} → GET /api/v1/roles/{roleId}/roadmaps?lastId=123&sortBy=latest
     */
    @GetMapping("/roles/{roleId}/roadmaps")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<List<RoadmapWithStatusDTO>> getRoadmapsByRole(
            @PathVariable @NotNull(message = "职业ID不能为空")
            @Positive(message = "职业ID必须大于0")
            Long roleId,
            @RequestParam(required = false)
            Long lastId,
            @RequestParam(required = false, defaultValue = "score")
            String sortBy,
            @CurrentUser UserDO currentUser) {

        List<RoadmapWithStatusDTO> roadmaps = roadmapService.getRoadmapsByRole(roleId, lastId, sortBy, currentUser);

        return ApiResponse.success(roadmaps);
    }

    /**
     * 更新路线图
     * 映射: PUT /roadmap/{id} → PUT /api/v1/roadmaps/{id}
     */
    @PutMapping("/roadmaps/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> updateRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空")
            @Positive(message = "路线图ID必须大于0")
            Long id,
            @RequestBody @Valid UpdateRoadmapRequest request,
            @CurrentUser UserDO currentUser) {

        roadmapService.updateRoadmap(id, request.getContent(), request.getDescription(), request.getState(), currentUser);
        return ApiResponse.success();
    }

    /**
     * 创建路线图
     * 映射: POST /roadmap → POST /api/v1/roadmaps
     */
    @PostMapping("/roadmaps")
    @SaCheckLogin
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Long> createRoadmap(
            @RequestBody @Valid CreateRoadmapRequest request,
            @CurrentUser UserDO currentUser) {

        Long roadmapId = roadmapService.createRoadmap(
            request.getRoleId(),
            request.getContent(),
            request.getDescription(),
            currentUser.getId(),
            request.getState()
        );
        return ApiResponse.success(roadmapId);
    }

    /**
     * 获取路线图详情
     * 映射: GET /roadmap/{id} → GET /api/v1/roadmaps/{id}
     */
    @GetMapping("/roadmaps/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<RoadmapWithStatusDTO> getRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空")
            @Positive(message = "路线图ID必须大于0")
            Long id,
            @CurrentUser UserDO currentUser) {
        RoadmapWithStatusDTO roadmapDTOV1 = roadmapService.getRoadmapWithContent(id, currentUser.getId());

        return ApiResponse.success(roadmapDTOV1);
    }

    /**
     * 获取当前登录用户创建的路线图（所有状态）
     * 包含：待审核、已发布、审核拒绝、已屏蔽
     * GET /api/v1/users/me/roadmaps?lastId=123
     */
    @GetMapping("/users/me/roadmaps")
    @SaCheckLogin
    public ApiResponse<List<RoadmapDetailDTO>> getCurrentUserRoadmaps(
            @RequestParam(required = false) Long lastId,
            @CurrentUser UserDO currentUser) {

        List<RoadmapDetailDTO> roadmaps = roadmapService.getUserRoadmaps(currentUser.getId(), lastId, null);
        return ApiResponse.success(roadmaps);
    }

    /**
     * 获取指定用户创建的路线图（仅已发布）
     * GET /api/v1/users/{userId}/roadmaps?lastId=0
     */
    @GetMapping("/users/{userId}/roadmaps")
    public ApiResponse<List<RoadmapDetailDTO>> getUserRoadmaps(
            @PathVariable @NotNull(message = "用户ID不能为空") @Positive(message = "用户ID必须大于0") Long userId,
            @RequestParam(required = false) Long lastId) {

        List<RoadmapDetailDTO> roadmaps = roadmapService.getUserRoadmaps(userId, lastId, ContentState.PUBLISHED);
        return ApiResponse.success(roadmaps);
    }

    /**
     * 删除路线图（软删除）
     * DELETE /api/v1/roadmaps/{id}
     */
    @DeleteMapping("/roadmaps/{id}")
    @SaCheckLogin
    public ApiResponse<Void> deleteRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空") @Positive(message = "路线图ID必须大于0") Long id,
            @CurrentUser UserDO currentUser) {

        roadmapService.deleteRoadmap(id, currentUser);
        return ApiResponse.success();
    }
}