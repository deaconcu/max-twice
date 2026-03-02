package com.prosper.learn.content.node;

import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.domain.Enums;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.prosper.learn.shared.domain.Enums.ContentState;

/**
 * 节点领域服务
 *
 * 只依赖 content 域，处理节点的核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeDomainService {

    private final NodeDataService nodeDataService;

    // ========== Command 方法 ==========

    /**
     * 审批通过节点
     *
     * @param nodeId 节点ID
     */
    @Transactional
    public void approve(long nodeId) {
        nodeDataService.validateExists(nodeId);

        NodeDO nodeDO = nodeDataService.getById(nodeId);
        Utils.validateStateTransition(nodeDO.getState(), ContentState.PUBLISHED);

        nodeDataService.approve(nodeId);
        log.info("Node {} approved", nodeId);
    }

    /**
     * 拒绝节点（审核不通过）
     *
     * 注意：节点的审核通常跟随 Post 的审核，不单独发送通知
     *
     * @param nodeId 节点ID
     * @param reason 拒绝原因
     */
    @Transactional
    public void reject(long nodeId, String reason) {
        nodeDataService.validateExists(nodeId);

        NodeDO nodeDO = nodeDataService.getById(nodeId);
        Utils.validateStateTransition(nodeDO.getState(), ContentState.REJECTED);

        nodeDataService.reject(nodeId, reason);
        log.info("Node {} rejected, reason: {}", nodeId, reason);
    }

    /**
     * 封禁节点（违规封禁）
     *
     * 注意：节点的审核通常跟随 Post 的审核，不单独发送通知
     *
     * @param nodeId 节点ID
     * @param reason 封禁原因
     */
    @Transactional
    public void ban(long nodeId, String reason) {
        nodeDataService.validateExists(nodeId);

        NodeDO nodeDO = nodeDataService.getById(nodeId);
        Utils.validateStateTransition(nodeDO.getState(), ContentState.BANNED);

        nodeDataService.ban(nodeId, reason);
        log.info("Node {} banned, reason: {}", nodeId, reason);
    }

    /**
     * 修改节点状态（编排方法）
     *
     * @param nodeId 节点ID
     * @param state 目标状态
     * @param reason 原因（拒绝或封禁时需要）
     */
    @Transactional
    public void updateNodeState(long nodeId, ContentState state, String reason) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }

        nodeDataService.validateExists(nodeId);

        // 根据状态调用相应的方法
        switch (state) {
            case REJECTED -> reject(nodeId, reason);
            case BANNED -> ban(nodeId, reason);
            case PUBLISHED -> approve(nodeId);
            default -> throw new IllegalArgumentException("Unsupported state: " + state);
        }
    }
}
