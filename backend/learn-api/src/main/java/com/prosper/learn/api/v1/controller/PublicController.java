package com.prosper.learn.api.v1.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.api.ratelimit.LimitType;
import com.prosper.learn.api.ratelimit.RateLimit;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.business.PageService;
import com.prosper.learn.domain.service.business.ProfessionService;
import com.prosper.learn.domain.service.business.RoadmapService;
import com.prosper.learn.domain.service.data.SystemDataService;
import com.prosper.learn.dto.response.ProfessionDTO;
import com.prosper.learn.dto.response.RoadmapDTO;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 公开接口 - 无需登录
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Slf4j
@Validated
@RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
public class PublicController {

    private final SystemDataService systemDataService;
    private final ObjectMapper objectMapper;
    private final ProfessionService professionService;
    private final RoadmapService roadmapService;
    private final PageService pageService;

    /**
     * 获取课程分类数据（公开接口）
     * GET /api/v1/public/course-categories
     */
    @GetMapping("/course-categories")
    public ApiResponse<JsonNode> getCourseCategories() {
        try {
            String configValue = systemDataService.getValue("courseCategories");
            if (configValue == null) {
                return ApiResponse.error("课程分类配置不存在");
            }

            try {
                JsonNode categoryNode = objectMapper.readTree(configValue);

                // 处理嵌套的 courseCategories 结构
                if (categoryNode.has("courseCategories")) {
                    categoryNode = categoryNode.get("courseCategories");
                }

                return ApiResponse.success(categoryNode);
            } catch (IOException e) {
                log.error("Failed to parse course categories config", e);
                return ApiResponse.error("课程分类配置格式错误");
            }
        } catch (Exception e) {
            log.error("Failed to get course categories", e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 获取职业分类数据（公开接口）
     * GET /api/v1/public/profession-categories
     */
    @GetMapping("/profession-categories")
    public ApiResponse<JsonNode> getProfessionCategories() {
        try {
            String configValue = systemDataService.getValue("professionCategories");
            if (configValue == null) {
                return ApiResponse.error("职业分类配置不存在");
            }

            try {
                JsonNode categoryNode = objectMapper.readTree(configValue);
                return ApiResponse.success(categoryNode);
            } catch (IOException e) {
                log.error("Failed to parse profession categories config", e);
                return ApiResponse.error("职业分类配置格式错误");
            }
        } catch (Exception e) {
            log.error("Failed to get profession categories", e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 查询只读模式状态（公开接口，无需登录）
     * GET /api/v1/public/readonly-mode
     */
    @GetMapping("/readonly-mode")
    public ApiResponse<Map<String, Object>> getReadOnlyMode() {
        try {
            boolean enabled = systemDataService.isReadOnlyMode();

            Map<String, Object> result = new HashMap<>();
            result.put("enabled", enabled);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("Failed to get readonly mode status", e);
            return ApiResponse.error("查询失败");
        }
    }

    /**
     * 获取职业详情（公开接口，无需登录）
     * GET /api/v1/public/professions/{id}
     */
    @GetMapping("/professions/{id}")
    public ApiResponse<ProfessionDTO> getProfession(
            @PathVariable @NotNull(message = "职业ID不能为空")
            @Positive(message = "职业ID必须大于0")
            Long id) {
        try {
            ProfessionDTO profession = professionService.getById(id, true);
            if (profession == null) {
                return ApiResponse.error(ErrorCode.PROFESSION_NOT_FOUND.getCode(), "职业不存在");
            }
            return ApiResponse.success(profession);
        } catch (Exception e) {
            log.error("Failed to get profession: {}", id, e);
            throw e;
        }
    }

    /**
     * 获取职业的路线图列表（公开接口，无需登录）
     * GET /api/v1/public/professions/{professionId}/roadmaps?lastId=123&pageSize=20
     */
    @GetMapping("/professions/{professionId}/roadmaps")
    public ApiResponse<List<RoadmapDTO>> getRoadmapsByProfession(
            @PathVariable @NotNull(message = "职业ID不能为空")
            @Positive(message = "职业ID必须大于0")
            Long professionId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        try {
            List<RoadmapDTO> roadmaps = roadmapService.getRoadmapsByProfessionPublic(professionId, lastId, pageSize);
            return ApiResponse.success(roadmaps);
        } catch (Exception e) {
            log.error("Failed to get roadmaps for profession: {}", professionId, e);
            throw e;
        }
    }

    /**
     * 读取页面数据（公开接口，无需登录）
     * GET /api/v1/public/pages/read?courseId=123&path=1-xxx
     * GET /api/v1/public/pages/read?courseId=123
     *
     * 注意：公开接口返回的数据不包含个性化信息（学习进度、订阅状态等均为默认值）
     */
    @GetMapping("/pages/read")
    public ApiResponse<Map<String, Object>> readPage(
            @RequestParam(required = true) @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0") Long courseId,
            @RequestParam(required = false) String path) {

        try {
            Map<String, Object> result = pageService.readPageByPathPublic(courseId, path);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("Failed to read page: courseId={}, path={}", courseId, path, e);
            throw e;
        }
    }
}
