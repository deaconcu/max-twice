package com.prosper.learn.web.v1.controller.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.infrastructure.datasource.DataSourceContextHolder;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemDO;
import com.prosper.learn.shared.infrastructure.config.SystemDataService;
import com.prosper.learn.application.dto.response.SystemConfigDTO;
import com.prosper.learn.application.converter.SystemConfigConverter;
import com.prosper.learn.shared.infrastructure.config.SystemDomainService;
import com.prosper.learn.application.dto.ApiResponse;
import com.prosper.learn.application.service.AsyncTaskService;
import com.prosper.learn.application.service.CourseService;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.JsonParam;
import com.prosper.learn.web.v1.annotation.OperationLog;
import com.prosper.learn.web.v1.annotation.RequireRole;
import jakarta.validation.constraints.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 系统配置管理后台接口
 * 映射原有的 /system 接口到 /api/v1/admin/system
 */
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Slf4j
@SaCheckLogin
@RequireRole(UserRole.ADMIN)
@Validated
public class AdminSystemController {

    private final SystemDataService systemDataService;
    private final SystemDomainService systemDomainService;
    private final SystemConfigConverter systemConfigConverter;
    private final ObjectMapper objectMapper;
    private final AsyncTaskService asyncTaskService;
    private final CourseService courseService;

    /**
     * 获取系统配置
     * 映射: GET /system → GET /api/v1/admin/system
     */
    @GetMapping("/system")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<JsonNode> getSystemConfig(@RequestParam(required = false) String part) {
        try {
            if (part != null && !part.isEmpty()) {
                // 获取配置的特定部分
                return getSystemConfigPart(part);
            } else {
                // 获取完整系统配置
                List<SystemDO> allConfigs = systemDataService.getAllConfigsWithMeta();
                if (allConfigs.isEmpty()) {
                    return ApiResponse.fail("系统配置不存在");
                }
                List<SystemConfigDTO> result = systemConfigConverter.toDTOList(allConfigs);
                JsonNode config = objectMapper.valueToTree(result);
                return ApiResponse.success(config);
            }
        } catch (Exception e) {
            log.error("系统配置 获取配置失败", e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 获取系统配置的特定部分
     * 映射: GET /system?part=courseCategories → GET /api/v1/admin/system?part=courseCategories
     */
    private ApiResponse<JsonNode> getSystemConfigPart(String part) throws IOException {
        String configValue = systemDataService.getValue(part);
        if (configValue == null) {
            return ApiResponse.fail("配置部分不存在: " + part);
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
     * 映射: POST /system → POST /api/v1/admin/system
     */
    @PostMapping("/system")
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    @Transactional
    @OperationLog(
        module = "系统配置",
        type = "修改系统配置",
        level = OperationLevel.HIGH,
        targetType = "System",
        targetId = "0",
        targetName = "#key"
    )
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

            systemDomainService.reload();
            log.info("系统配置 更新成功: key={}", key);
            return ApiResponse.success("配置更新成功");
        } catch (Exception e) {
            log.error("系统配置 更新失败: key={}", key, e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 删除系统配置
     */
    @DeleteMapping("/system")
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    @Transactional
    @OperationLog(
        module = "系统配置",
        type = "删除系统配置",
        level = OperationLevel.HIGH,
        targetType = "System",
        targetId = "0",
        targetName = "#key"
    )
    public ApiResponse<String> deleteSystemConfig(
            @RequestParam @NotBlank(message = "配置键不能为空") String key) {
        try {
            if (!systemDataService.exists(key)) {
                return ApiResponse.fail("配置不存在: " + key);
            }

            systemDataService.deleteConfig(key);

            log.info("系统配置 删除成功: key={}", key);
            return ApiResponse.success("配置删除成功");
        } catch (Exception e) {
            log.error("系统配置 删除失败: key={}", key, e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 获取单个配置值
     */
    @GetMapping("/system/{key}")
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<String> getConfigByKey(
            @PathVariable @NotBlank(message = "配置键不能为空") String key) {
        try {
            String value = systemDataService.getValue(key);
            if (value == null) {
                return ApiResponse.fail("配置不存在: " + key);
            }
            return ApiResponse.success(value);
        } catch (Exception e) {
            log.error("系统配置 获取配置失败: key={}", key, e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 设置只读模式（管理员）
     * 映射: POST /api/v1/admin/system/readonly-mode
     * @param enable true=开启，false=关闭
     */
    @PostMapping("/system/readonly-mode")
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    @Transactional
    @OperationLog(
        module = "系统配置",
        type = "#enable ? '开启只读模式' : '关闭只读模式'",
        level = OperationLevel.HIGH,
        targetType = "System",
        targetId = "0",
        targetName = "'readonly_mode'"
    )
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
            log.error("系统配置 只读模式设置失败", e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    // ==================== 后台任务接口 ====================

    /**
     * 启动重算课程子课程数量任务
     * POST /api/v1/admin/tasks/recalculate-sub-course-counts
     */
    @PostMapping("/tasks/recalculate-sub-course-counts")
    @RateLimit(capacity = 5, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    @Transactional
    @OperationLog(
        module = "系统任务",
        type = "重算子课程数量",
        level = OperationLevel.HIGH,
        targetType = "Task",
        targetId = "0",
        targetName = "'recalculate-sub-course-counts'"
    )
    public ApiResponse<Map<String, String>> startRecalculateSubCourseCounts() {
        String taskId = asyncTaskService.generateTaskId();
        String language = DataSourceContextHolder.getLanguage();
        asyncTaskService.initTask(taskId);
        asyncTaskService.runAsyncWithProgress(taskId, language, progressCallback -> {
            Map<String, Integer> result = courseService.recalculateAllSubCourseCounts(progressCallback);
            asyncTaskService.completeTask(taskId, result);
        });

        log.info("[{}] 启动重算子课程数量任务: {}", language, taskId);
        return ApiResponse.success(Map.of("taskId", taskId, "status", "RUNNING"));
    }

    /**
     * 查询任务状态
     * GET /api/v1/admin/tasks/{taskId}
     */
    @GetMapping("/tasks/{taskId}")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Map<String, Object>> getTaskResult(
            @PathVariable @NotBlank(message = "任务ID不能为空") String taskId) {
        Map<String, Object> result = asyncTaskService.getTaskResult(taskId);
        if (result == null) {
            return ApiResponse.fail("任务不存在或已过期");
        }
        return ApiResponse.success(result);
    }
}