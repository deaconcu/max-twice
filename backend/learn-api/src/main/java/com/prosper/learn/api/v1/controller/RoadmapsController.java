package com.prosper.learn.api.v1.controller;

import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.business.RoadmapService;
import com.prosper.learn.dto.response.RoadmapDTO;
import com.prosper.learn.dto.response.old.RoadmapDTOV1;
import com.prosper.learn.dto.response.ApprovalResponseDTO;
import com.prosper.learn.dto.request.*;
import com.prosper.learn.api.v1.annotation.JsonParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import cn.dev33.satoken.stp.StpUtil;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import java.util.List;

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
     * 映射: GET /roadmap/list/{professionId} → GET /api/v1/professions/{professionId}/roadmaps?lastId=123
     */
    @GetMapping("/professions/{professionId}/roadmaps")
    public ApiResponse<List<RoadmapDTO>> getRoadmapsByProfession(
            @PathVariable @NotNull(message = "职业ID不能为空")
            @Positive(message = "职业ID必须大于0")
            Long professionId,
            @RequestParam(required = false, defaultValue = "0")
            @Min(value = 0, message = "最后ID不能小于0")
            Long lastId) {
        
        if (!StpUtil.isLogin()) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }

        long userId = StpUtil.getLoginIdAsLong();
        List<RoadmapDTO> roadmaps = roadmapService.getRoadmapsByProfession(professionId, lastId, userId);

        return ApiResponse.success(roadmaps);
    }

    /**
     * 更新路线图
     * 映射: PUT /roadmap/{id} → PUT /api/v1/roadmaps/{id}
     */
    @PutMapping("/roadmaps/{id}")
    public ApiResponse<Void> updateRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空")
            @Positive(message = "路线图ID必须大于0")
            Long id,
            @JsonParam("content") @NotBlank(message = "内容不能为空") String content) {
        if (!StpUtil.isLogin()) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }

        long userId = StpUtil.getLoginIdAsLong();
        roadmapService.updateRoadmap(id, content, userId);
        return ApiResponse.success();
    }

    /**
     * 创建路线图
     * 映射: POST /roadmap → POST /api/v1/roadmaps
     */
    @PostMapping("/roadmaps")
    public ApiResponse<Long> createRoadmap(@RequestBody @Valid CreateRoadmapRequest request) {
        
        if (!StpUtil.isLogin()) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }

        long userId = StpUtil.getLoginIdAsLong();
        Long roadmapId = roadmapService.createRoadmap(request.getProfessionId(), request.getContent(), request.getDescription(), userId);
        return ApiResponse.success(roadmapId);
    }

    /**
     * 获取路线图详情
     * 映射: GET /roadmap/{id} → GET /api/v1/roadmaps/{id}
     */
    @GetMapping("/roadmaps/{id}")
    public ApiResponse<RoadmapDTO> getRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空")
            @Positive(message = "路线图ID必须大于0")
            Long id) {
        long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : 0;
        RoadmapDTO roadmapDTOV1 = roadmapService.getRoadmapWithContent(id, userId);

        return ApiResponse.success(roadmapDTOV1);
    }

    /**
     * 置顶路线图
     * 映射: POST /roadmap/pin → POST /api/v1/roadmaps/pin
     */
    @PostMapping("/roadmaps/pin")
    public ApiResponse<Boolean> pinRoadmap(@RequestBody @Valid SetRoadmapProgressRequest request) {
        if (!StpUtil.isLogin()) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }

        long userId = StpUtil.getLoginIdAsLong();
        Boolean pinned = roadmapService.pinRoadmap(request.getProfessionId(), request.getRoadmapId(), userId);

        return ApiResponse.success(pinned);
    }

    /**
     * 管理后台：按条件获取路线图列表
     * 映射: GET /api/v1/admin/roadmaps?state=0&professionId=1&creatorId=2&lastId=123
     */
    @GetMapping("/admin/roadmaps")
    public ApiResponse<List<RoadmapDTO>> getAdminRoadmaps(
            @RequestParam(required = false) @Min(value = 0, message = "状态必须大于等于0") Byte state,
            @RequestParam(required = false) @Positive(message = "职业ID必须大于0") Long professionId,
            @RequestParam(required = false) @Positive(message = "创建者ID必须大于0") Long creatorId,
            @RequestParam(required = false) Long lastId) {

        List<RoadmapDTO> roadmaps = roadmapService.listByFilter(state, professionId, creatorId, lastId);
        return ApiResponse.success(roadmaps);
    }

    /**
     * 路线图审核操作
     * 映射: POST /api/v1/roadmaps/{id}/approve
     */
    @PostMapping("/roadmaps/{id}/approve")
    public ApiResponse<RoadmapDTO> approveRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空")
            @Positive(message = "路线图ID必须大于0")
            Long id,
            @RequestBody @Valid OperateRequest request) {

        RoadmapDTO roadmap = switch (request.getAction().toLowerCase()) {
            case "approve" -> roadmapService.approve(id);
            case "approve_clear" -> roadmapService.approveAndClearDescription(id);
            case "reject" -> roadmapService.reject(id);
            case "ban" -> roadmapService.ban(id);
            default -> throw ErrorCode.SYSTEM_ERROR.exception();
        };

        return ApiResponse.success(roadmap);
    }

    /**
     * 更新路线图描述
     * 映射: PUT /api/v1/roadmaps/{id}/description
     */
    @PutMapping("/roadmaps/{id}/description")
    public ApiResponse<RoadmapDTO> updateRoadmapDescription(
            @PathVariable @NotNull(message = "路线图ID不能为空")
            @Positive(message = "路线图ID必须大于0")
            Long id,
            @JsonParam("description") String description) {

        RoadmapDTO roadmap = roadmapService.updateDescription(id, description);
        return ApiResponse.success(roadmap);
    }
}