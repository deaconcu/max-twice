package com.twicemax.learning.progress;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 用户节点完成记录数据服务
 */
@Service
@RequiredArgsConstructor
public class UserNodeCompletionDataService {

    private final UserNodeCompletionMapper mapper;

    /**
     * 批量查询用户完成的节点ID
     */
    public List<Long> getCompletedNodeIds(long userId, Collection<Long> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return List.of();
        }
        return mapper.getCompletedNodeIds(userId, nodeIds);
    }

    /**
     * 检查节点是否完成
     */
    public boolean isCompleted(long userId, long nodeId) {
        return mapper.exists(userId, nodeId) > 0;
    }

    /**
     * 标记节点为已完成
     */
    public int markCompleted(long userId, long nodeId) {
        UserNodeCompletionDO record = new UserNodeCompletionDO();
        record.setUserId(userId);
        record.setNodeId(nodeId);
        record.setCompletedAt(LocalDateTime.now());
        return mapper.insert(record);
    }

    /**
     * 取消节点完成
     */
    public int unmarkCompleted(long userId, long nodeId) {
        return mapper.delete(userId, nodeId);
    }
}