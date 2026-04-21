package com.twicemax.web.v1.controller.admin;

import com.twicemax.analytics.monitoring.ErrorLogDO;
import com.twicemax.analytics.monitoring.service.ErrorLogService;
import com.twicemax.application.dto.ApiResponse;
import com.twicemax.application.dto.response.ErrorLogDTO;
import com.twicemax.shared.common.utils.Utils;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v1.annotation.RequireRole;
import com.twicemax.shared.domain.Enums;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 错误日志管理接口
 */
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Slf4j
@RequireRole(Enums.UserRole.ADMIN)
@Validated
public class AdminErrorLogController {

    private final ErrorLogService errorLogService;

    /**
     * 查询错误日志列表
     * GET /api/v1/admin/error-logs
     */
    @GetMapping("/error-logs")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<List<ErrorLogDTO>> list(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") int limit) {

        List<ErrorLogDO> logs = errorLogService.queryLogs(source, status, lastId, limit);
        List<ErrorLogDTO> dtos = logs.stream().map(this::toDTO).collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    /**
     * 获取错误详情
     * GET /api/v1/admin/error-logs/{id}
     */
    @GetMapping("/error-logs/{id}")
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<ErrorLogDTO> get(@PathVariable Long id) {
        ErrorLogDO errorLog = errorLogService.getById(id);
        if (errorLog == null) {
            return ApiResponse.fail("错误日志不存在");
        }
        return ApiResponse.success(toDTO(errorLog));
    }

    /**
     * 更新状态
     * PUT /api/v1/admin/error-logs/{id}/status
     */
    @PutMapping("/error-logs/{id}/status")
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String status = body.get("status");
        if (status == null || (!status.equals("new") && !status.equals("ignored") && !status.equals("resolved"))) {
            return ApiResponse.fail("无效的状态值");
        }

        errorLogService.updateStatus(id, status);
        return ApiResponse.success();
    }

    /**
     * 统计未处理的错误数量
     * GET /api/v1/admin/error-logs/count/new
     */
    @GetMapping("/error-logs/count/new")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Integer> countNew() {
        return ApiResponse.success(errorLogService.countNew());
    }

    /**
     * 删除过期日志
     * DELETE /api/v1/admin/error-logs/expired
     */
    @DeleteMapping("/error-logs/expired")
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Integer> deleteExpired(@RequestParam(defaultValue = "30") int days) {
        int deleted = errorLogService.deleteExpired(days);
        return ApiResponse.success(deleted);
    }

    /**
     * 转换为 DTO
     */
    private ErrorLogDTO toDTO(ErrorLogDO errorLog) {
        ErrorLogDTO dto = new ErrorLogDTO();
        dto.setId(errorLog.getId());
        dto.setFingerprint(errorLog.getFingerprint());
        dto.setSource(errorLog.getSource());
        dto.setErrorType(errorLog.getErrorType());
        dto.setMessage(errorLog.getMessage());
        dto.setStackTrace(errorLog.getStackTrace());
        dto.setUrl(errorLog.getUrl());
        dto.setUserId(errorLog.getUserId());
        dto.setIp(errorLog.getIp());
        dto.setUserAgent(errorLog.getUserAgent());
        dto.setExtraData(errorLog.getExtraData());
        dto.setCount(errorLog.getCount());
        dto.setFirstSeenAt(Utils.getTimeString(errorLog.getFirstSeenAt()));
        dto.setLastSeenAt(Utils.getTimeString(errorLog.getLastSeenAt()));
        dto.setStatus(errorLog.getStatus());
        return dto;
    }
}
