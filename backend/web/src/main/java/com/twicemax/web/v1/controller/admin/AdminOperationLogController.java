package com.twicemax.web.v1.controller.admin;

import com.twicemax.application.dto.request.OperationLogRequest;
import com.twicemax.application.dto.response.OperationLogDTO;
import com.twicemax.application.service.OperationLogService;
import com.twicemax.web.v1.annotation.RequireRole;
import com.twicemax.application.dto.ApiResponse;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.shared.domain.Enums;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 操作日志管理接口
 */
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Slf4j
@RequireRole(Enums.UserRole.ADMIN)
@Validated
public class AdminOperationLogController {

    private final OperationLogService operationLogService;

    /**
     * 查询操作日志列表（分页）
     * GET /api/v1/admin/operation-logs
     */
    @GetMapping("/operation-logs")
    @RequireRole(Enums.UserRole.ADMIN)
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Map<String, Object>> getOperationLogs(OperationLogRequest query) {
        Map<String, Object> result = operationLogService.queryLogs(query);
        return ApiResponse.success(result);
    }

    /**
     * 根据ID查询操作日志详情
     * GET /api/v1/admin/operation-logs/{id}
     */
    @GetMapping("/operation-logs/{id}")
    @RequireRole(Enums.UserRole.ADMIN)
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<OperationLogDTO> getOperationLogById(
            @PathVariable @Positive(message = "日志ID必须大于0") Long id) {
        OperationLogDTO log = operationLogService.getLogById(id);
        if (log == null) {
            return ApiResponse.fail("操作日志不存在");
        }
        return ApiResponse.success(log);
    }
}
