package com.prosper.learn.web.v1.controller.admin;

import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.web.v1.annotation.JsonParam;
import com.prosper.learn.web.v1.annotation.OperationLog;
import com.prosper.learn.web.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Enums.UserRole;
import com.prosper.learn.web.v1.annotation.RequireRole;
import com.prosper.learn.business.service.application.RoadmapService;
import com.prosper.learn.dto.response.roadmap.RoadmapSummaryDTO;
import com.prosper.learn.persistence.dataobject.UserDO;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 路线图管理后台接口
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@RequireRole(UserRole.ADMIN)
@Validated
public class AdminRoadmapsController {

    private final RoadmapService roadmapService;

    /**
     * 管理后台：按条件获取路线图列表
     * 映射: GET /api/v1/admin/roadmaps?state=0&professionId=1&creatorId=2&lastId=123
     */
    @GetMapping("/roadmaps")
    @RequireRole(UserRole.MODERATOR)
    public ApiResponse<List<RoadmapSummaryDTO>> getAdminRoadmaps(
            @RequestParam(required = false) @Min(value = 0, message = "状态必须大于等于0") Byte state,
            @RequestParam(required = false) @Positive(message = "职业ID必须大于0") Long professionId,
            @RequestParam(required = false) @Positive(message = "创建者ID必须大于0") Long creatorId,
            @RequestParam(required = false) Long lastId) {

        List<RoadmapSummaryDTO> roadmaps = roadmapService.listByFilter(state, professionId, creatorId, lastId);
        return ApiResponse.success(roadmaps);
    }

    /**
     * 管理后台：更新路线图描述
     * 映射: PUT /api/v1/admin/roadmaps/{id}
     */
    @PutMapping("/roadmaps/{id}")
    @RequireRole(UserRole.MODERATOR)
    @OperationLog(
        module = "内容管理",
        type = "更新路线图信息",
        level = Enums.OperationLevel.MEDIUM,
        targetType = "Roadmap",
        targetId = "#id"
    )
    public ApiResponse<RoadmapSummaryDTO> updateRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空")
            @Positive(message = "路线图ID必须大于0")
            Long id,
            @JsonParam("description") @Size(max = 500, message = "描述长度不能超过500个字符") String description,
            @CurrentUser UserDO currentUser) {

        RoadmapSummaryDTO roadmap = roadmapService.updateDescription(id, description, currentUser);
        return ApiResponse.success(roadmap);
    }
}
