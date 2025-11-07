package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.api.ratelimit.LimitType;
import com.prosper.learn.api.ratelimit.RateLimit;
import com.prosper.learn.api.v1.annotation.CurrentUser;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums;
import com.prosper.learn.domain.service.business.RoadmapService;
import com.prosper.learn.dto.response.RoadmapDTO;
import com.prosper.learn.dto.response.old.RoadmapDTOV1;
import com.prosper.learn.dto.request.*;
import com.prosper.learn.api.v1.annotation.JsonParam;
import com.prosper.learn.persistence.dataobject.UserDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 路线图接口
 * 从RoadmapClient迁移而来
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@RateLimit(capacity = 40, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public class RoadmapsController {

    private final RoadmapService roadmapService;

    /**
     * 获取职业路线图
     * 映射: GET /roadmap/list/{professionId} → GET /api/v1/professions/{professionId}/roadmaps?lastId=123
     */
    @GetMapping("/professions/{professionId}/roadmaps")
    @SaCheckLogin
    public ApiResponse<List<RoadmapDTO>> getRoadmapsByProfession(
            @PathVariable @NotNull(message = "职业ID不能为空")
            @Positive(message = "职业ID必须大于0")
            Long professionId,
            @RequestParam(required = false)
            Long lastId,
            @CurrentUser UserDO currentUser) {

        List<RoadmapDTO> roadmaps = roadmapService.getRoadmapsByProfession(professionId, lastId, currentUser);

        return ApiResponse.success(roadmaps);
    }

    /**
     * 更新路线图
     * 映射: PUT /roadmap/{id} → PUT /api/v1/roadmaps/{id}
     */
    @PutMapping("/roadmaps/{id}")
    @SaCheckLogin
    public ApiResponse<Void> updateRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空")
            @Positive(message = "路线图ID必须大于0")
            Long id,
            @JsonParam("content") @NotBlank(message = "内容不能为空") String content,
            @CurrentUser UserDO currentUser) {

        roadmapService.updateRoadmap(id, content, currentUser);
        return ApiResponse.success();
    }

    /**
     * 创建路线图
     * 映射: POST /roadmap → POST /api/v1/roadmaps
     */
    @PostMapping("/roadmaps")
    @SaCheckLogin
    public ApiResponse<Long> createRoadmap(
            @RequestBody @Valid CreateRoadmapRequest request,
            @CurrentUser UserDO currentUser) {

        Long roadmapId = roadmapService.createRoadmap(request.getProfessionId(), request.getContent(), request.getDescription(), currentUser.getId());
        return ApiResponse.success(roadmapId);
    }

    /**
     * 获取路线图详情
     * 映射: GET /roadmap/{id} → GET /api/v1/roadmaps/{id}
     */
    @GetMapping("/roadmaps/{id}")
    @SaCheckLogin
    public ApiResponse<RoadmapDTO> getRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空")
            @Positive(message = "路线图ID必须大于0")
            Long id,
            @CurrentUser UserDO currentUser) {
        RoadmapDTO roadmapDTOV1 = roadmapService.getRoadmapWithContent(id, currentUser.getId());

        return ApiResponse.success(roadmapDTOV1);
    }

    /**
     * 置顶路线图
     * 映射: POST /roadmap/pin → POST /api/v1/roadmaps/pin
     */
    @PostMapping("/roadmaps/pin")
    @SaCheckLogin
    public ApiResponse<Boolean> pinRoadmap(
            @RequestBody @Valid SetRoadmapProgressRequest request,
            @CurrentUser UserDO currentUser) {

        Boolean pinned = roadmapService.pinRoadmap(request.getProfessionId(), request.getRoadmapId(), currentUser.getId());

        return ApiResponse.success(pinned);
    }

    /**
     * 获取当前登录用户创建的路线图（所有状态）
     * 包含：待审核、已发布、审核拒绝、已屏蔽
     * GET /api/v1/users/me/roadmaps?lastId=0
     */
    @GetMapping("/users/me/roadmaps")
    @SaCheckLogin
    public ApiResponse<List<RoadmapDTO>> getCurrentUserRoadmaps(
            @RequestParam @NotNull(message = "最后ID不能为空") @Min(value = 0, message = "最后ID不能小于0") Long lastId,
            @CurrentUser UserDO currentUser) {

        List<RoadmapDTO> roadmaps = roadmapService.getUserRoadmaps(currentUser.getId(), lastId, null);
        return ApiResponse.success(roadmaps);
    }

    /**
     * 获取指定用户创建的路线图（仅已发布）
     * GET /api/v1/users/{userId}/roadmaps?lastId=0
     */
    @GetMapping("/users/{userId}/roadmaps")
    public ApiResponse<List<RoadmapDTO>> getUserRoadmaps(
            @PathVariable @NotNull(message = "用户ID不能为空") @Positive(message = "用户ID必须大于0") Long userId,
            @RequestParam @NotNull(message = "最后ID不能为空") @Min(value = 0, message = "最后ID不能小于0") Long lastId) {

        List<RoadmapDTO> roadmaps = roadmapService.getUserRoadmaps(userId, lastId, Enums.ContentState.PUBLISHED);
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