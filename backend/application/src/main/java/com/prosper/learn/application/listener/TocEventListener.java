package com.prosper.learn.application.listener;

import com.prosper.learn.application.service.LearningProgressService;
import com.prosper.learn.content.toc.NodeTocDO;
import com.prosper.learn.content.toc.NodeTocDataService;
import com.prosper.learn.content.toc.TocDomainService;
import com.prosper.learn.learning.enrollment.UserLearningDO;
import com.prosper.learn.learning.enrollment.UserLearningDataService;
import com.prosper.learn.learning.enrollment.UserLearningDomainService;
import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.event.content.toc.TocChosenEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * ToC事件监听器
 * 处理目录更新相关的事件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TocEventListener {

    private final NodeTocDataService nodeTocDataService;
    private final UserLearningDataService userLearningDataService;
    private final TocDomainService tocDomainService;
    private final LearningProgressService learningProgressService;
    private final UserLearningDomainService userLearningDomainService;

    /**
     * 监听ToC更新事件
     * 当用户修改第一个目录时，更新 user_learning.nodes 字段
     */
    @EventListener
    public void onTocChosen(TocChosenEvent event) {
        try {
            Long userId = event.getUserId();
            Long nodeId = event.getNodeId();  // 可以是课程根节点，也可以是普通节点
            String newFirstTocHash = event.getNewFirstTocHash();

            // 1. 检查用户是否正在学习这个节点（objectType=node, objectId=nodeId）
            UserLearningDO learning = userLearningDataService.getByUserAndObject(
                userId,
                Enums.ContentType.node.byteValue(),
                nodeId
            );

            // 如果用户没有开始学习这个节点，或者已经完成，不需要更新 nodes 字段
            if (learning == null) {
                log.debug("用户 {} 未开始学习节点 {}，跳过更新nodes", userId, nodeId);
                return;
            }

            if (learning.getState() == Enums.UserProgressState.COMPLETED.value()) {
                log.debug("用户 {} 已完成节点 {} 的学习，跳过更新nodes", userId, nodeId);
                return;
            }

            // 2. 获取新目录的内容
            NodeTocDO nodeTocDO = nodeTocDataService.get(newFirstTocHash);
            if (nodeTocDO == null || nodeTocDO.getToc() == null) {
                log.warn("目录哈希 {} 不存在", newFirstTocHash);
                return;
            }

            // 3. 收集节点ID
            Set<Long> nodeIds = tocDomainService.collectNodeIdsFromToc(nodeTocDO.getToc());

            // 4. 转换为JSON数组字符串 "[1,2,3]"
            String nodesJson = Utils.nodeIdsToJsonArray(nodeIds);

            // 5. 更新 user_learning.nodes 字段
            int updated = userLearningDataService.updateNodes(
                userId,
                Enums.ContentType.node.byteValue(),
                nodeId,
                nodesJson
            );

            if (updated > 0) {
                log.info("更新节点 {} 的节点列表: {} 个节点", nodeId, nodeIds.size());
            }

            // 6. 重新计算并更新进度
            Integer newProgress = learningProgressService.calculateNodeProgress(userId, nodeId);
            userLearningDomainService.updateProgress(userId, Enums.ContentType.node, nodeId, newProgress);
            log.info("重新计算节点 {} 的进度: {}", nodeId, newProgress);

        } catch (Exception e) {
            log.error("处理ToC更新事件失败", e);
        }
    }
}
