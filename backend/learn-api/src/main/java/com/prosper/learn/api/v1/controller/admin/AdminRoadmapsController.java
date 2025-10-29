package com.prosper.learn.api.v1.controller.admin;

import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums.UserRole;
import com.prosper.learn.api.v1.annotation.RequireRole;
import com.prosper.learn.domain.service.business.RoadmapService;
import com.prosper.learn.dto.response.RoadmapDTO;
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
    public ApiResponse<List<RoadmapDTO>> getAdminRoadmaps(
            @RequestParam(required = false) @Min(value = 0, message = "状态必须大于等于0") Byte state,
            @RequestParam(required = false) @Positive(message = "职业ID必须大于0") Long professionId,
            @RequestParam(required = false) @Positive(message = "创建者ID必须大于0") Long creatorId,
            @RequestParam(required = false) Long lastId) {

        List<RoadmapDTO> roadmaps = roadmapService.listByFilter(state, professionId, creatorId, lastId);
        return ApiResponse.success(roadmaps);
    }
}
