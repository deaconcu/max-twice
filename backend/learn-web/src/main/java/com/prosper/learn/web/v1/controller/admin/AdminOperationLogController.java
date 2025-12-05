package com.prosper.learn.web.v1.controller.admin;

import com.prosper.learn.application.dto.request.OperationLogRequest;
import com.prosper.learn.application.dto.response.OperationLogDTO;
import com.prosper.learn.application.service.OperationLogService;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.web.v1.annotation.RequireRole;
import com.prosper.learn.web.v1.dto.ApiResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 操作日志管理接口
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@RequireRole(UserRole.ADMIN)
@Validated
public class AdminOperationLogController {

    private final OperationLogService operationLogService;

    /**
     * 查询操作日志列表（分页）
     * GET /api/v1/admin/operation-logs
     */
    @GetMapping("/operation-logs")
    @RequireRole(UserRole.ADMIN)
    public ApiResponse<Map<String, Object>> getOperationLogs(OperationLogRequest query) {
        Map<String, Object> result = operationLogService.queryLogs(query);
        return ApiResponse.success(result);
    }

    /**
     * 根据ID查询操作日志详情
     * GET /api/v1/admin/operation-logs/{id}
     */
    @GetMapping("/operation-logs/{id}")
    @RequireRole(UserRole.ADMIN)
    public ApiResponse<OperationLogDTO> getOperationLogById(
            @PathVariable @Positive(message = "日志ID必须大于0") Long id) {
        OperationLogDTO log = operationLogService.getLogById(id);
        if (log == null) {
            return ApiResponse.error("操作日志不存在");
        }
        return ApiResponse.success(log);
    }
}
