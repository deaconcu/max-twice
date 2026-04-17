package com.prosper.learn.web.v1.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.application.dto.response.KeysetPageResponse;
import com.prosper.learn.application.dto.response.role.RoleDTO;
import com.prosper.learn.application.dto.response.course.CourseFullDTO;
import com.prosper.learn.application.dto.response.roadmap.RoadmapSummaryDTO;
import com.prosper.learn.application.service.CourseService;
import com.prosper.learn.application.service.PageService;
import com.prosper.learn.application.service.RoleService;
import com.prosper.learn.application.service.RoadmapService;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemDataService;
import com.prosper.learn.shared.infrastructure.config.SystemDomainService;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.application.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.prosper.learn.shared.domain.Enums.ContentState;

/**
 * 公开接口 - 无需登录
 */
@RestController
@RequestMapping("/v1/public")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PublicController {

    private final SystemDataService systemDataService;
    private final SystemDomainService systemDomainService;
    private final ObjectMapper objectMapper;
    private final RoleService roleService;
    private final RoadmapService roadmapService;
    private final PageService pageService;
    private final CourseService courseService;

    /**
     * 获取课程分类数据（公开接口，支持 ETag 缓存）
     * GET /api/v1/public/course-categories
     */
    @GetMapping("/course-categories")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public ResponseEntity<ApiResponse<JsonNode>> getCourseCategories(HttpServletRequest request) {
        try {
            // 从领域服务获取解析好的数据
            JsonNode categoryNode = systemDomainService.getCourseCategories();

            // Web层处理 ETag 缓存
            String etag = generateETag(categoryNode.toString());
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

        } catch (Exception e) {
            log.error("公开接口 获取课程分类失败", e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 获取角色分类数据（公开接口，支持 ETag 缓存）
     * GET /api/v1/public/role-categories
     */
    @GetMapping("/role-categories")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public ResponseEntity<ApiResponse<JsonNode>> getRoleCategories(HttpServletRequest request) {
        try {
            // 从领域服务获取解析好的数据
            JsonNode categoryNode = systemDomainService.getRoleCategories();

            // Web层处理 ETag 缓存
            String etag = generateETag(categoryNode.toString());
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

        } catch (Exception e) {
            log.error("公开接口 获取角色分类失败", e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
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
            log.error("公开接口 生成 ETag 失败", e);
            // 降级：使用 hashCode
            return "\"" + Integer.toHexString(content.hashCode()) + "\"";
        }
    }

    /**
     * 查询只读模式状态（公开接口，无需登录）
     * GET /api/v1/public/readonly-mode
     */
    @GetMapping("/readonly-mode")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public ApiResponse<Map<String, Object>> getReadOnlyMode() {
        try {
            boolean enabled = systemDataService.isReadOnlyMode();

            Map<String, Object> result = new HashMap<>();
            result.put("enabled", enabled);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("公开接口 获取只读模式状态失败", e);
            return ApiResponse.fail("查询失败");
        }
    }

    /**
     * 获取角色详情（公开接口，无需登录）
     * GET /api/v1/public/roles/{id}
     */
    @GetMapping("/roles/{id}")
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public ApiResponse<RoleDTO> getRole(
            @PathVariable @NotNull(message = "角色ID不能为空")
            @Positive(message = "角色ID必须大于0")
            Long id) {
        try {
            RoleDTO roleDTO = roleService.getById(id, true, null);
            if (roleDTO == null) {
                return ApiResponse.fail(StatusCode.ROLE_NOT_FOUND.getCode(), "角色不存在");
            }
            return ApiResponse.success(roleDTO);
        } catch (Exception e) {
            log.error("公开接口 获取角色详情失败: {}", id, e);
            throw e;
        }
    }

    /**
     * 获取角色的路线图列表（公开接口，无需登录）
     * GET /api/v1/public/roles/{roleId}/roadmaps?lastId=123&pageSize=20
     */
    @GetMapping("/roles/{roleId}/roadmaps")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public ApiResponse<List<RoadmapSummaryDTO>> getRoadmapsByRole(
            @PathVariable @NotNull(message = "角色ID不能为空")
            @Positive(message = "角色ID必须大于0")
            Long roleId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        try {
            List<RoadmapSummaryDTO> roadmaps = roadmapService.getRoadmapsByRolePublic(roleId, lastId, pageSize);
            return ApiResponse.success(roadmaps);
        } catch (Exception e) {
            log.error("公开接口 获取角色路线图列表失败: {}", roleId, e);
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
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public ApiResponse<Map<String, Object>> readPage(
            @RequestParam(required = true) @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0") Long courseId,
            @RequestParam(required = false) String path) {

        try {
            //Map<String, Object> result = pageService.readPageByPathPublic(courseId, path);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("公开接口 读取页面失败: courseId={}，path={}", courseId, path, e);
            throw e;
        }
    }

    /**
     * 获取课程列表（公开接口）
     * 不需要登录，不返回用户个性化数据（订阅状态、学习进度等）
     *
     * 参数组合：
     * 1. mainCategory（可选 subCategory）：按分类筛选
     * 2. parentId：获取子课程
     * 3. 无参数或只有 lastId：返回所有已发布课程（分页）
     */
    @GetMapping("/courses")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public ApiResponse<KeysetPageResponse<CourseFullDTO>> getCourses(
            @RequestParam(required = false) @Positive(message = "最后ID必须大于0") Long lastId,
            @RequestParam(required = false) @Positive(message = "主分类必须大于0") Integer mainCategory,
            @RequestParam(required = false) @Positive(message = "子分类必须大于0") Integer subCategory,
            @RequestParam(required = false) @Positive(message = "父课程ID必须大于0") Long parentId) {

        KeysetPageResponse<CourseFullDTO> response;

        // userId = null 表示未登录用户，不返回个性化数据
        if (mainCategory != null) {
            response = courseService.getListByCategoryPage(mainCategory, subCategory, lastId, null);
        } else if (parentId != null) {
            response = courseService.getListByParentPage(parentId, ContentState.PUBLISHED, lastId, null);
        } else {
            response = courseService.getListByStatePage(ContentState.PUBLISHED, lastId, null);
        }

        return ApiResponse.query(response);
    }

    /**
     * 获取课程详情（公开接口）
     * 不需要登录，不返回用户个性化数据（订阅状态、学习进度等）
     */
    @GetMapping("/courses/{id}")
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public ApiResponse<CourseFullDTO> getCourse(
            @PathVariable @Positive(message = "课程ID必须大于0") Long id) {
        // userId = null 表示未登录用户，不返回个性化数据
        CourseFullDTO course = courseService.getCourseById(id, null);
        return ApiResponse.query(course);
    }
}
