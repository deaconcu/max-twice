package com.twicemax.web.v2.controller.admin;

import com.twicemax.application.dto.request.OperationLogRequest;
import com.twicemax.application.dto.response.OperationLogDTO;
import com.twicemax.application.service.OperationLogService;
import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.RequireRole;
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
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@RequireRole(Enums.UserRole.ADMIN)
@Validated
public class AdminOperationLogController {

    private final OperationLogService operationLogService;

    /**
     * 查询操作日志列表（分页）
     * GET /api/v2/admin/operation-logs
     */
    @GetMapping("/operation-logs")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Map<String, Object> getOperationLogs(OperationLogRequest query) {
        return operationLogService.queryLogs(query);
    }

    /**
     * 根据ID查询操作日志详情
     * GET /api/v2/admin/operation-logs/{id}
     */
    @GetMapping("/operation-logs/{id}")
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public OperationLogDTO getOperationLogById(
            @PathVariable @Positive(message = "日志ID必须大于0") Long id) {
        OperationLogDTO result = operationLogService.getLogById(id);
        if (result == null) {
            throw StatusCode.NOT_FOUND.exception("操作日志不存在");
        }
        return result;
    }
}
