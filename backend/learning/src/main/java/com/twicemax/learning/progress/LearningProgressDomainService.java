package com.twicemax.learning.progress;

import com.twicemax.learning.enrollment.UserLearningDomainService;
import com.twicemax.shared.domain.event.user.learning.NodeCompletedEvent;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.shared.common.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * 学习进度领域服务
 * 使用 user_node_completion 表，每个完成的节点一条记录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningProgressDomainService {

    private final UserNodeCompletionDataService userNodeCompletionDataService;
    private final UserLearningDomainService userLearningDomainService;
    private final ApplicationEventPublisher eventPublisher;

    // ========== Command 方法（写操作）==========

    /**
     * 标记节点为已完成
     *
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @param userToday 用户时区的今天日期
     */
    public void markNodeCompleted(long userId, long nodeId, LocalDate userToday) {
        ValidationUtils.requirePositiveId(userId);
        ValidationUtils.requirePositiveId(nodeId);

        // 检查是否已完成
        if (userNodeCompletionDataService.isCompleted(userId, nodeId)) {
            throw StatusCode.NODE_ALREADY_COMPLETED.exception();
        }

        // 标记为完成
        int inserted = userNodeCompletionDataService.markCompleted(userId, nodeId);
        if (inserted > 0) {
            log.info("用户 {} 标记节点 {} 为已完成", userId, nodeId);
            // 发布节点完成事件
            eventPublisher.publishEvent(NodeCompletedEvent.completed(userId, nodeId, userToday));
        } else {
            throw StatusCode.DATABASE_ERROR.exception();
        }
    }

    /**
     * 取消标记节点为已完成
     *
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @param rootNodeId 根节点ID（用于验证）
     * @param userToday 用户时区的今天日期
     */
    public void unmarkNodeCompleted(long userId, long nodeId, long rootNodeId, LocalDate userToday) {
        ValidationUtils.requirePositiveId(userId);
        ValidationUtils.requirePositiveId(nodeId);
        ValidationUtils.requirePositiveId(rootNodeId);

        // 检查是否未完成
        if (!userNodeCompletionDataService.isCompleted(userId, nodeId)) {
            throw StatusCode.NODE_ALREADY_NOT_COMPLETED.exception();
        }

        // 取消完成
        int deleted = userNodeCompletionDataService.unmarkCompleted(userId, nodeId);
        if (deleted > 0) {
            log.info("用户 {} 取消标记节点 {} 为已完成", userId, nodeId);
            // 发布取消完成事件
            eventPublisher.publishEvent(NodeCompletedEvent.uncompleted(userId, nodeId, userToday));
        } else {
            throw StatusCode.DATABASE_ERROR.exception();
        }
    }

    // ========== Query 方法（读操作）==========

    /**
     * 检查用户是否完成了指定节点
     *
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @return 是否已完成
     */
    public boolean isNodeCompleted(Long userId, Long nodeId) {
        ValidationUtils.requirePositiveId(userId);
        ValidationUtils.requirePositiveId(nodeId);

        return userNodeCompletionDataService.isCompleted(userId, nodeId);
    }

    /**
     * 批量检查哪些节点已完成
     * 用于显示目录树时标记完成状态
     *
     * @param userId 用户ID
     * @param nodeIds 要检查的节点ID列表
     * @return 已完成的节点ID集合
     */
    public Set<Long> getCompletedNodesInList(long userId, Collection<Long> nodeIds) {
        ValidationUtils.requirePositiveId(userId);

        if (nodeIds == null || nodeIds.isEmpty()) {
            return Set.of();
        }

        List<Long> completedIds = userNodeCompletionDataService.getCompletedNodeIds(userId, nodeIds);
        return new HashSet<>(completedIds);
    }
}