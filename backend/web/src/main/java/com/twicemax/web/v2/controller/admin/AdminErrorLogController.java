package com.twicemax.web.v2.controller.admin;

import com.twicemax.analytics.monitoring.ErrorLogDO;
import com.twicemax.analytics.monitoring.service.ErrorLogService;
import com.twicemax.application.dto.response.ErrorLogDTO;
import com.twicemax.shared.common.utils.Utils;
import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.RequireRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@RequireRole(Enums.UserRole.ADMIN)
@Validated
public class AdminErrorLogController {

    private final ErrorLogService errorLogService;

    /**
     * 查询错误日志列表
     * GET /api/v2/admin/error-logs
     */
    @GetMapping("/error-logs")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<ErrorLogDTO> list(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") int limit) {

        List<ErrorLogDO> logs = errorLogService.queryLogs(source, status, lastId, limit);
        return logs.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 获取错误详情
     * GET /api/v2/admin/error-logs/{id}
     */
    @GetMapping("/error-logs/{id}")
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ErrorLogDTO get(@PathVariable Long id) {
        ErrorLogDO errorLog = errorLogService.getById(id);
        if (errorLog == null) {
            throw StatusCode.NOT_FOUND.exception("错误日志不存在");
        }
        return toDTO(errorLog);
    }

    /**
     * 更新状态
     * PUT /api/v2/admin/error-logs/{id}/status
     */
    @PutMapping("/error-logs/{id}/status")
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String status = body.get("status");
        if (status == null || (!status.equals("new") && !status.equals("ignored") && !status.equals("resolved"))) {
            throw StatusCode.INVALID_PARAMETER.exception("无效的状态值");
        }

        errorLogService.updateStatus(id, status);
        return ResponseEntity.noContent().build();
    }

    /**
     * 统计未处理的错误数量
     * GET /api/v2/admin/error-logs/count/new
     */
    @GetMapping("/error-logs/count/new")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public int countNew() {
        return errorLogService.countNew();
    }

    /**
     * 删除过期日志
     * DELETE /api/v2/admin/error-logs/expired
     */
    @DeleteMapping("/error-logs/expired")
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public int deleteExpired(@RequestParam(defaultValue = "30") int days) {
        return errorLogService.deleteExpired(days);
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
