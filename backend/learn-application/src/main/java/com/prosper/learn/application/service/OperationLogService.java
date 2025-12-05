package com.prosper.learn.application.service;

import com.prosper.learn.analytics.monitoring.OperationLogDO;
import com.prosper.learn.analytics.monitoring.OperationLogDataService;
import com.prosper.learn.application.converter.OperationLogConverter;
import com.prosper.learn.application.dto.request.OperationLogRequest;
import com.prosper.learn.application.dto.response.OperationLogDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogDataService operationLogDataService;
    private final OperationLogConverter operationLogConverter;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 记录操作日志
     */
    public void recordLog(OperationLogDTO logDTO) {
        try {
            OperationLogDO logDO = operationLogConverter.toDataObject(logDTO);
            logDO.setCreatedAt(LocalDateTime.now());
            operationLogDataService.insert(logDO);
            log.info("Operation log recorded: operator={}, module={}, type={}, target={}:{}",
                    logDTO.getOperatorName(), logDTO.getModule(), logDTO.getOperationType(),
                    logDTO.getTargetType(), logDTO.getTargetId());
        } catch (Exception e) {
            log.error("Failed to record operation log", e);
            // 操作日志记录失败不应抛出异常影响主业务
        }
    }

    /**
     * 查询操作日志（keyset分页）
     */
    public Map<String, Object> queryLogs(OperationLogRequest query) {
        // 参数验证和默认值
        int limit = query.getLimit() != null && query.getLimit() > 0 ? query.getLimit() : 20;
        if (limit > 100) {
            limit = 100; // 限制最大每页数量
        }

        Long lastId = query.getLastId();

        // 时间转换
        LocalDateTime startTime = parseDateTime(query.getStartTime());
        LocalDateTime endTime = parseDateTime(query.getEndTime());

        // 查询数据（多查一条用于判断是否还有更多数据）
        List<OperationLogDO> logs = operationLogDataService.queryLogs(
                query.getOperatorId(),
                query.getModule(),
                query.getOperationType(),
                query.getOperationLevel(),
                query.getTargetType(),
                query.getTargetId(),
                startTime,
                endTime,
                lastId,
                limit + 1
        );

        // 判断是否还有更多数据
        boolean hasMore = logs.size() > limit;
        if (hasMore) {
            logs = logs.subList(0, limit);
        }

        // 获取下一页的lastId
        Long nextLastId = null;
        if (hasMore && !logs.isEmpty()) {
            nextLastId = logs.get(logs.size() - 1).getId();
        }

        // 转换为DTO
        List<OperationLogDTO> logDTOs = logs.stream()
                .map(operationLogConverter::toDTO)
                .collect(Collectors.toList());

        // 构造返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("items", logDTOs);
        result.put("hasMore", hasMore);
        result.put("nextLastId", nextLastId);

        return result;
    }

    /**
     * 根据ID查询操作日志详情
     */
    public OperationLogDTO getLogById(Long id) {
        OperationLogDO logDO = operationLogDataService.getById(id);
        return logDO != null ? operationLogConverter.toDTO(logDO) : null;
    }

    /**
     * 解析时间字符串
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            log.warn("Failed to parse datetime: {}", dateTimeStr);
            return null;
        }
    }
}
