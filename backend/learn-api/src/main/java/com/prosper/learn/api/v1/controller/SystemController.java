package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.api.v1.annotation.JsonParam;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.data.SystemDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * 系统配置接口 V1 - Key-Value模式
 * 映射原有的 /system 接口到 /api/v1/system
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@SaCheckLogin
@Validated
public class SystemController {

    private final SystemDataService systemDataService;
    private final ObjectMapper objectMapper;

    /**
     * 获取系统配置
     * 映射: GET /system → GET /api/v1/system
     */
    @GetMapping("/system")
    public ApiResponse<JsonNode> getSystemConfig(@RequestParam(required = false) String part) {
        try {
            if (part != null && !part.isEmpty()) {
                // 获取配置的特定部分
                return getSystemConfigPart(part);
            } else {
                // 获取完整系统配置
                Map<String, String> allConfigs = systemDataService.getAllConfigs();
                if (allConfigs.isEmpty()) {
                    return ApiResponse.error("系统配置不存在");
                }
                JsonNode config = objectMapper.valueToTree(allConfigs);
                return ApiResponse.success(config);
            }
        } catch (Exception e) {
            log.error("Failed to get system config", e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 获取系统配置的特定部分
     * 映射: GET /system?part=courseCategories → GET /api/v1/system?part=courseCategories
     */
    private ApiResponse<JsonNode> getSystemConfigPart(String part) throws IOException {
        String configValue = systemDataService.getValue(part);
        if (configValue == null) {
            return ApiResponse.error("配置部分不存在: " + part);
        }

        try {
            // 尝试解析为JSON
            JsonNode partNode = objectMapper.readTree(configValue);
            
            // 处理嵌套的 courseCategories 结构
            if ("courseCategories".equals(part) && partNode.has("courseCategories")) {
                partNode = partNode.get("courseCategories");
            }
            
            return ApiResponse.success(partNode);
        } catch (IOException e) {
            // 如果不是JSON格式，直接返回字符串值
            return ApiResponse.success(objectMapper.valueToTree(configValue));
        }
    }

    /**
     * 更新系统配置
     * 映射: POST /system → POST /api/v1/system
     */
    @PostMapping("/system")
    public ApiResponse<String> updateSystemConfig(
            @RequestParam @NotBlank(message = "配置键不能为空") String key,
            @JsonParam("value") @NotBlank(message = "配置值不能为空") String value) {
        try {
            // 如果value是JSON格式，验证其有效性
            try {
                objectMapper.readTree(value);
            } catch (IOException e) {
                // 不是JSON格式也没关系，可能是普通字符串
            }
            
            systemDataService.setValue(key, value);
            
            log.info("System config updated successfully for key: {}", key);
            return ApiResponse.success("配置更新成功");
        } catch (Exception e) {
            log.error("Failed to update system config for key: {}", key, e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 删除系统配置
     */
    @DeleteMapping("/system")
    public ApiResponse<String> deleteSystemConfig(
            @RequestParam @NotBlank(message = "配置键不能为空") String key) {
        try {
            if (!systemDataService.exists(key)) {
                return ApiResponse.error("配置不存在: " + key);
            }
            
            systemDataService.deleteConfig(key);
            
            log.info("System config deleted successfully for key: {}", key);
            return ApiResponse.success("配置删除成功");
        } catch (Exception e) {
            log.error("Failed to delete system config for key: {}", key, e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 获取单个配置值
     */
    @GetMapping("/system/{key}")
    public ApiResponse<String> getConfigByKey(
            @PathVariable @NotBlank(message = "配置键不能为空") String key) {
        try {
            String value = systemDataService.getValue(key);
            if (value == null) {
                return ApiResponse.error("配置不存在: " + key);
            }
            return ApiResponse.success(value);
        } catch (Exception e) {
            log.error("Failed to get system config for key: {}", key, e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 设置只读模式（管理员）
     * 映射: POST /api/v1/system/readonly-mode
     * @param enable true=开启，false=关闭
     */
    @PostMapping("/system/readonly-mode")
    public ApiResponse<String> setReadOnlyMode(
            @JsonParam("enable") @NotNull(message = "enable参数不能为空") Boolean enable) {
        try {
            if (enable) {
                systemDataService.enableReadOnlyMode();
                log.info("只读模式已开启");
                return ApiResponse.success("只读模式已开启");
            } else {
                systemDataService.disableReadOnlyMode();
                log.info("只读模式已关闭");
                return ApiResponse.success("只读模式已关闭");
            }
        } catch (Exception e) {
            log.error("Failed to set readonly mode", e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }
}