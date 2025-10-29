package com.prosper.learn.api.v1.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.data.SystemDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 公开接口 - 无需登录
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Slf4j
public class PublicController {

    private final SystemDataService systemDataService;
    private final ObjectMapper objectMapper;

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
}
