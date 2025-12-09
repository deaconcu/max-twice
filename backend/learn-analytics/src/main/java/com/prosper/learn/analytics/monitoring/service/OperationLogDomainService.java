package com.prosper.learn.analytics.monitoring.service;

import com.prosper.learn.analytics.monitoring.OperationLogDO;
import com.prosper.learn.analytics.monitoring.OperationLogDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志领域服务
 *
 * 只依赖 analytics 模块，处理操作日志的核心业务逻辑
 *
 * 职责：
 * - 记录操作日志
 * - 查询操作日志
 * - 操作日志数据管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogDomainService {

    private final OperationLogDataService operationLogDataService;

    // ========== Command 方法（写操作）==========

    /**
     * 记录操作日志
     *
     * @param logDO 操作日志对象
     */
    public void recordLog(OperationLogDO logDO) {
        try {
            // 设置创建时间
            if (logDO.getCreatedAt() == null) {
                logDO.setCreatedAt(LocalDateTime.now());
            }

            // 插入数据库
            operationLogDataService.insert(logDO);

            log.debug("Operation log recorded: operator={}, module={}, type={}, target={}:{}",
                    logDO.getOperatorId(), logDO.getModule(), logDO.getOperationType(),
                    logDO.getTargetType(), logDO.getTargetId());
        } catch (Exception e) {
            log.error("Failed to record operation log", e);
            // 操作日志记录失败不应抛出异常影响主业务
        }
    }

    // ========== Query 方法（读操作）==========

    /**
     * 根据ID查询操作日志
     *
     * @param id 日志ID
     * @return 操作日志对象，不存在返回null
     */
    public OperationLogDO getById(Long id) {
        return operationLogDataService.getById(id);
    }

    /**
     * 查询操作日志（keyset分页）
     *
     * @param operatorId 操作人ID（可选）
     * @param module 模块名称（可选）
     * @param operationType 操作类型（可选）
     * @param operationLevel 操作级别（可选）
     * @param targetType 目标类型（可选）
     * @param targetId 目标ID（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param lastId 最后一条记录ID（分页游标）
     * @param limit 每页数量
     * @return 操作日志列表
     */
    public List<OperationLogDO> queryLogs(Long operatorId, String module,
                                          String operationType, Integer operationLevel,
                                          String targetType, Long targetId,
                                          LocalDateTime startTime, LocalDateTime endTime,
                                          Long lastId, int limit) {
        return operationLogDataService.queryLogs(operatorId, module, operationType,
                operationLevel, targetType, targetId, startTime, endTime, lastId, limit);
    }
}
