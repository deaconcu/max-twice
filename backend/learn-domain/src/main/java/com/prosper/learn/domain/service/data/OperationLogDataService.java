package com.prosper.learn.domain.service.data;

import com.prosper.learn.persistence.dataobject.OperationLogDO;
import com.prosper.learn.persistence.mapper.OperationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志数据服务
 */
@Slf4j
@Service
public class OperationLogDataService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    /**
     * 插入操作日志
     */
    public void insert(OperationLogDO logDO) {
        try {
            operationLogMapper.insert(logDO);
        } catch (Exception e) {
            log.error("Failed to insert operation log", e);
            // 操作日志记录失败不应影响主业务，只记录错误日志
        }
    }

    /**
     * 根据ID查询
     */
    public OperationLogDO getById(Long id) {
        return operationLogMapper.getById(id);
    }

    /**
     * 查询操作日志（keyset分页）
     */
    public List<OperationLogDO> queryLogs(Long operatorId, String module, String operationType,
                                          Integer operationLevel, String targetType, Long targetId,
                                          LocalDateTime startTime, LocalDateTime endTime,
                                          Long lastId, int limit) {
        return operationLogMapper.queryLogsByLastId(operatorId, module, operationType, operationLevel,
                targetType, targetId, startTime, endTime, lastId, limit);
    }
}
