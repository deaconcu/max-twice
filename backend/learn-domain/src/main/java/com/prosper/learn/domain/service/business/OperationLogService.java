package com.prosper.learn.domain.service.business;

import com.prosper.learn.domain.service.data.OperationLogDataService;
import com.prosper.learn.domain.util.converter.OperationLogConverter;
import com.prosper.learn.dto.response.OperationLogDTO;
import com.prosper.learn.dto.request.OperationLogRequest;
import com.prosper.learn.persistence.dataobject.OperationLogDO;
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
     * 查询操作日志（分页）
     */
    public Map<String, Object> queryLogs(OperationLogRequest query) {
        // 参数验证和默认值
        int page = query.getPage() != null && query.getPage() > 0 ? query.getPage() : 1;
        int size = query.getSize() != null && query.getSize() > 0 ? query.getSize() : 20;
        if (size > 100) {
            size = 100; // 限制最大每页数量
        }

        // 时间转换
        LocalDateTime startTime = parseDateTime(query.getStartTime());
        LocalDateTime endTime = parseDateTime(query.getEndTime());

        // 查询数据
        List<OperationLogDO> logs = operationLogDataService.queryLogs(
                query.getOperatorId(),
                query.getModule(),
                query.getOperationType(),
                query.getOperationLevel(),
                query.getTargetType(),
                query.getTargetId(),
                startTime,
                endTime,
                page,
                size
        );

        // 统计总数
        int total = operationLogDataService.countLogs(
                query.getOperatorId(),
                query.getModule(),
                query.getOperationType(),
                query.getOperationLevel(),
                query.getTargetType(),
                query.getTargetId(),
                startTime,
                endTime
        );

        // 转换为DTO
        List<OperationLogDTO> logDTOs = logs.stream()
                .map(operationLogConverter::toDTO)
                .collect(Collectors.toList());

        // 构造返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("data", logDTOs);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", (total + size - 1) / size);

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
