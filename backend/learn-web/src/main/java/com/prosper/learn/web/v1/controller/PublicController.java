package com.prosper.learn.web.v1.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.application.dto.response.ProfessionDTO;
import com.prosper.learn.application.dto.response.roadmap.RoadmapSummaryDTO;
import com.prosper.learn.application.service.PageService;
import com.prosper.learn.application.service.ProfessionService;
import com.prosper.learn.application.service.RoadmapService;
import com.prosper.learn.shared.domain.exception.ErrorCode;
import com.prosper.learn.shared.infrastructure.config.SystemDataService;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
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
     * 获取课程分类数据（公开接口，支持 ETag 缓存）
     * GET /api/v1/public/course-categories
     */
    @GetMapping("/course-categories")
    public ResponseEntity<ApiResponse<JsonNode>> getCourseCategories(HttpServletRequest request) {
        try {
            String configValue = systemDataService.getValue("courseCategories");
            if (configValue == null) {
                return ResponseEntity.ok(ApiResponse.error("课程分类配置不存在"));
            }

            try {
                JsonNode categoryNode = objectMapper.readTree(configValue);

                // 处理嵌套的 courseCategories 结构
                if (categoryNode.has("courseCategories")) {
                    categoryNode = categoryNode.get("courseCategories");
                }

                // 计算 ETag
                String etag = generateETag(categoryNode.toString());

                // 检查客户端 If-None-Match 头
                String clientETag = request.getHeader("If-None-Match");
                if (etag.equals(clientETag)) {
                    // 数据未变化，返回 304
                    return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                            .eTag(etag)
                            .build();
                }

                // 返回数据并设置 ETag
                return ResponseEntity.ok()
                        .eTag(etag)
                        .body(ApiResponse.success(categoryNode));

            } catch (IOException e) {
                log.error("Failed to parse course categories config", e);
                return ResponseEntity.ok(ApiResponse.error("课程分类配置格式错误"));
            }
        } catch (Exception e) {
            log.error("Failed to get course categories", e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 获取职业分类数据（公开接口，支持 ETag 缓存）
     * GET /api/v1/public/profession-categories
     */
    @GetMapping("/profession-categories")
    public ResponseEntity<ApiResponse<JsonNode>> getProfessionCategories(HttpServletRequest request) {
        try {
            String configValue = systemDataService.getValue("professionCategories");
            if (configValue == null) {
                return ResponseEntity.ok(ApiResponse.error("职业分类配置不存在"));
            }

            try {
                JsonNode categoryNode = objectMapper.readTree(configValue);

                // 计算 ETag
                String etag = generateETag(categoryNode.toString());

                // 检查客户端 If-None-Match 头
                String clientETag = request.getHeader("If-None-Match");
                if (etag.equals(clientETag)) {
                    // 数据未变化，返回 304
                    return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                            .eTag(etag)
                            .build();
                }

                // 返回数据并设置 ETag
                return ResponseEntity.ok()
                        .eTag(etag)
                        .body(ApiResponse.success(categoryNode));

            } catch (IOException e) {
                log.error("Failed to parse profession categories config", e);
                return ResponseEntity.ok(ApiResponse.error("职业分类配置格式错误"));
            }
        } catch (Exception e) {
            log.error("Failed to get profession categories", e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 生成 ETag（使用 MD5 哈希）
     */
    private String generateETag(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(content.getBytes());
            return "\"" + HexFormat.of().formatHex(hash) + "\"";
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate ETag", e);
            // 降级：使用 hashCode
            return "\"" + Integer.toHexString(content.hashCode()) + "\"";
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
    public ApiResponse<List<RoadmapSummaryDTO>> getRoadmapsByProfession(
            @PathVariable @NotNull(message = "职业ID不能为空")
            @Positive(message = "职业ID必须大于0")
            Long professionId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        try {
            List<RoadmapSummaryDTO> roadmaps = roadmapService.getRoadmapsByProfessionPublic(professionId, lastId, pageSize);
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
