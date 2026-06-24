package com.twicemax.web.v2.controller.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twicemax.application.converter.SystemConfigConverter;
import com.twicemax.application.dto.response.SystemConfigDTO;
import com.twicemax.application.service.AsyncTaskService;
import com.twicemax.application.service.CourseService;
import com.twicemax.infrastructure.datasource.DataSourceContextHolder;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.shared.infrastructure.config.SystemDO;
import com.twicemax.shared.infrastructure.config.SystemDataService;
import com.twicemax.shared.infrastructure.config.SystemDomainService;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.JsonParam;
import com.twicemax.web.v2.annotation.OperationLog;
import com.twicemax.web.v2.annotation.RequireRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.twicemax.shared.domain.Enums.*;

/**
 * 系统配置管理后台接口
 */
@RestController
@RequestMapping("/admin")
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
     * GET /api/v2/admin/system
     */
    @GetMapping("/system")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public JsonNode getSystemConfig(@RequestParam(required = false) String part) {
        try {
            if (part != null && !part.isEmpty()) {
                return getSystemConfigPart(part);
            } else {
                List<SystemDO> allConfigs = systemDataService.getAllConfigsWithMeta();
                if (allConfigs.isEmpty()) {
                    throw StatusCode.NOT_FOUND.exception("系统配置不存在");
                }
                List<SystemConfigDTO> result = systemConfigConverter.toDTOList(allConfigs);
                return objectMapper.valueToTree(result);
            }
        } catch (Exception e) {
            log.error("系统配置 获取配置失败", e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    private JsonNode getSystemConfigPart(String part) throws IOException {
        String configValue = systemDataService.getValue(part);
        if (configValue == null) {
            throw StatusCode.NOT_FOUND.exception("配置部分不存在: " + part);
        }

        try {
            JsonNode partNode = objectMapper.readTree(configValue);
            if ("courseCategories".equals(part) && partNode.has("courseCategories")) {
                partNode = partNode.get("courseCategories");
            }
            return partNode;
        } catch (IOException e) {
            return objectMapper.valueToTree(configValue);
        }
    }

    /**
     * 更新系统配置
     * POST /api/v2/admin/system
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
    public ResponseEntity<Void> updateSystemConfig(
            @RequestParam @NotBlank(message = "配置键不能为空") String key,
            @JsonParam("value") @NotBlank(message = "配置值不能为空") String value) {
        try {
            try {
                objectMapper.readTree(value);
            } catch (IOException e) {
                // 非JSON格式也可接受
            }
            systemDataService.setValue(key, value);
            systemDomainService.reload();
            log.info("系统配置 更新成功: key={}", key);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("系统配置 更新失败: key={}", key, e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 删除系统配置
     * DELETE /api/v2/admin/system
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
    public ResponseEntity<Void> deleteSystemConfig(
            @RequestParam @NotBlank(message = "配置键不能为空") String key) {
        try {
            if (!systemDataService.exists(key)) {
                throw StatusCode.NOT_FOUND.exception("配置不存在: " + key);
            }
            systemDataService.deleteConfig(key);
            log.info("系统配置 删除成功: key={}", key);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("系统配置 删除失败: key={}", key, e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 获取单个配置值
     * GET /api/v2/admin/system/{key}
     */
    @GetMapping("/system/{key}")
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public String getConfigByKey(
            @PathVariable @NotBlank(message = "配置键不能为空") String key) {
        try {
            String value = systemDataService.getValue(key);
            if (value == null) {
                throw StatusCode.NOT_FOUND.exception("配置不存在: " + key);
            }
            return value;
        } catch (Exception e) {
            log.error("系统配置 获取配置失败: key={}", key, e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 设置只读模式
     * POST /api/v2/admin/system/readonly-mode
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
    public ResponseEntity<Void> setReadOnlyMode(
            @JsonParam("enable") @NotNull(message = "enable参数不能为空") Boolean enable) {
        try {
            if (enable) {
                systemDataService.enableReadOnlyMode();
                log.info("只读模式已开启");
            } else {
                systemDataService.disableReadOnlyMode();
                log.info("只读模式已关闭");
            }
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("系统配置 只读模式设置失败", e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    // ==================== 后台任务接口 ====================

    /**
     * 启动重算课程子课程数量任务
     * POST /api/v2/admin/tasks/recalculate-sub-course-counts
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
    public Map<String, String> startRecalculateSubCourseCounts() {
        String taskId = asyncTaskService.generateTaskId();
        String language = DataSourceContextHolder.getLanguage();
        asyncTaskService.initTask(taskId);
        asyncTaskService.runAsyncWithProgress(taskId, language, progressCallback -> {
            Map<String, Integer> result = courseService.recalculateAllSubCourseCounts(progressCallback);
            asyncTaskService.completeTask(taskId, result);
        });

        log.info("[{}] 启动重算子课程数量任务: {}", language, taskId);
        return Map.of("taskId", taskId, "status", "RUNNING");
    }

    /**
     * 查询任务状态
     * GET /api/v2/admin/tasks/{taskId}
     */
    @GetMapping("/tasks/{taskId}")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Map<String, Object> getTaskResult(
            @PathVariable @NotBlank(message = "任务ID不能为空") String taskId) {
        Map<String, Object> result = asyncTaskService.getTaskResult(taskId);
        if (result == null) {
            throw StatusCode.NOT_FOUND.exception("任务不存在或已过期");
        }
        return result;
    }
}
