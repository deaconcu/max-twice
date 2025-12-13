package com.prosper.learn.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.prosper.learn.application.converter.NodeConverter;
import com.prosper.learn.application.dto.response.CourseCompletionResponseDTO;
import com.prosper.learn.application.dto.response.NodeProgressResponseDTO;
import com.prosper.learn.application.dto.response.node.NodeWithProgressDTO;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.toc.TocDomainService;
import com.prosper.learn.learning.enrollment.UserCourseDO;
import com.prosper.learn.learning.enrollment.UserCourseDataService;
import com.prosper.learn.learning.progress.LearningProgressDomainService;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.exception.ErrorCode;
import com.prosper.learn.shared.domain.event.user.learning.LearningCompletedEvent;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 学习进度应用服务
 *
 * 负责跨域协调和DTO转换，依赖LearningProgressDomainService处理核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningProgressService {

    // 跨域依赖
    private final LearningProgressDomainService domainService;
    private final NodeDataService nodeDataService;
    private final TocDomainService tocService;
    private final NodeConverter nodeConverter;
    private final UserCourseDataService userCourseDataService;
    private final ObjectMapper objectMapper;
    private final SystemProperties systemProperties;
    private final ApplicationEventPublisher eventPublisher;

    // ========== Command 方法（写操作）==========

    /**
     * 标记节点完成并返回完整的响应数据
     */
    public NodeProgressResponseDTO markNodeCompletedWithResponse(long userId, long nodeId, long courseId) {
        // 调用领域服务处理核心逻辑
        boolean isNewlyCompleted = domainService.markNodeCompleted(userId, nodeId, courseId);

        // 如果启用层级进度，更新课程进度
        if (systemProperties.getLearningProgress().isEnableHierarchicalProgress()) {
            updateCourseProgress(userId, nodeId, courseId);
        }

        // 构建响应DTO
        UserCourseDO userCourse = userCourseDataService.getByUserIdAndCourseId(userId, courseId);
        Integer courseProgress = userCourse != null ? userCourse.getProgressPercent() : 0;
        long totalCompleted = domainService.getUserCompletedCount(userId);

        return NodeProgressResponseDTO.builder()
                .nodeId(nodeId)
                .completed(true)
                .isNewlyCompleted(isNewlyCompleted)
                .courseProgress(courseProgress)
                .totalCompletedNodes(totalCompleted)
                .build();
    }

    /**
     * 取消节点完成并返回完整的响应数据
     */
    public NodeProgressResponseDTO unmarkNodeCompletedWithResponse(long userId, long nodeId, long courseId) {
        // 调用领域服务处理核心逻辑
        boolean wasRemoved = domainService.unmarkNodeCompleted(userId, nodeId, courseId);

        // 如果启用层级进度，更新课程进度
        if (systemProperties.getLearningProgress().isEnableHierarchicalProgress()) {
            updateCourseProgress(userId, nodeId, courseId);
        }

        // 构建响应DTO
        UserCourseDO userCourse = userCourseDataService.getByUserIdAndCourseId(userId, courseId);
        Integer courseProgress = userCourse != null ? userCourse.getProgressPercent() : 0;
        long totalCompleted = domainService.getUserCompletedCount(userId);

        return NodeProgressResponseDTO.builder()
                .nodeId(nodeId)
                .completed(false)
                .wasRemoved(wasRemoved)
                .courseProgress(courseProgress)
                .totalCompletedNodes(totalCompleted)
                .build();
    }

    /**
     * 标记课程完成并返回完整的响应数据
     */
    public CourseCompletionResponseDTO markCourseCompletedWithResponse(long userId, long courseId) {
        // 调用领域服务处理核心逻辑
        boolean result = domainService.markCourseCompleted(userId, courseId);

        if (result) {
            return CourseCompletionResponseDTO.builder()
                    .courseId(courseId)
                    .completed(true)
                    .message("课程已标记为完成")
                    .build();
        } else {
            throw new RuntimeException("标记课程完成失败");
        }
    }

    // ========== Query 方法（读操作）==========

    /**
     * 获取节点完成状态响应数据
     */
    public NodeWithProgressDTO getNodeCompletionStatusResponse(long userId, long nodeId) {
        // 验证节点存在（跨域依赖）
        NodeDO nodeDO = nodeDataService.getById(nodeId);
        if (nodeDO == null) {
            throw ErrorCode.LEARNING_PROGRESS_INVALID_NODE_ID.exception();
        }

        // 检查完成状态
        boolean isCompleted = domainService.isNodeCompleted(userId, nodeId);

        // DTO转换
        return nodeConverter.toWithProgressDTO(nodeDO, isCompleted);
    }

    // ========== 委托方法（直接委托给DomainService）==========

    /**
     * 检查用户是否完成了指定节点
     */
    public boolean isNodeCompleted(Long userId, Long nodeId) {
        return domainService.isNodeCompleted(userId, nodeId);
    }

    /**
     * 获取用户完成的所有节点
     */
    public Set<Long> getUserCompletedNodes(long userId) {
        return domainService.getUserCompletedNodes(userId);
    }

// --注释掉检查 START (2025/12/10 11:12):
//    /**
//     * 获取用户完成的节点总数
//     */
//    public long getUserCompletedCount(long userId) {
//        return domainService.getUserCompletedCount(userId);
//    }
// --注释掉检查 STOP (2025/12/10 11:12)

// --注释掉检查 START (2025/12/10 11:13):
//    /**
//     * 标记课程为已完成
//     */
//    public boolean markCourseCompleted(long userId, long courseId) {
//        return domainService.markCourseCompleted(userId, courseId);
//    }
// --注释掉检查 STOP (2025/12/10 11:13)

// --注释掉检查 START (2025/12/10 11:12):
//    /**
//     * 获取课程进度百分比
//     */
//    public Integer getCourseProgress(long userId, Long courseId) {
//        return domainService.getCourseProgress(userId, courseId);
//    }
// --注释掉检查 STOP (2025/12/10 11:12)

// --注释掉检查 START (2025/12/10 11:13):
//    /**
//     * 手动触发数据同步（用于运维）
//     */
//    public boolean manualSync(Long userId) {
//        return domainService.manualSync(userId);
//    }
// --注释掉检查 STOP (2025/12/10 11:13)

// --注释掉检查 START (2025/12/10 11:12):
//    /**
//     * 获取当前失败队列中的用户数量（用于监控）
//     */
//    public long getFailedSyncQueueSize() {
//        return domainService.getFailedSyncQueueSize();
//    }
// --注释掉检查 STOP (2025/12/10 11:12)

    // ========== 私有辅助方法（跨域逻辑）==========

    /**
     * 更新课程进度
     * 基于用户目录1中的节点层级结构计算进度
     *
     * @param userId 用户ID
     * @param nodeId 节点ID（用于日志）
     * @param courseId 课程ID
     */
    private void updateCourseProgress(long userId, long nodeId, long courseId) {
        try {
            // 1. 获取用户目录1的内容（跨域依赖：content领域）
            String toc1Content = tocService.getToc(userId, courseId, 1);
            if (toc1Content == null) return;

            // 2. 解析目录结构
            JsonNode tocNode = objectMapper.readTree(toc1Content);

            // 3. 获取用户已完成的节点
            Set<Long> userCompletedNodes = domainService.getUserCompletedNodes(userId);

            // 4. 递归计算层级进度
            double progressPercent = calculateHierarchicalProgress(tocNode, userCompletedNodes) * 10000;
            int finalProgress = (int) Math.floor(progressPercent);

            // 5. 更新或创建用户课程记录
            UserCourseDO userCourse = userCourseDataService.getByUserIdAndCourseId(userId, courseId);

            if (userCourse == null) {
                userCourse = new UserCourseDO();
                userCourse.setUserId(userId);
                userCourse.setCourseId(courseId);
                userCourse.setProgressPercent(finalProgress);
                userCourse.setState(finalProgress >= 10000 ? Enums.UserProgressState.COMPLETED.value() : Enums.UserProgressState.IN_PROGRESS.value());
                userCourse.setStartedAt(LocalDateTime.now());
                if (finalProgress >= 10000) {
                    userCourse.setCompletedAt(LocalDateTime.now());
                    // 发布学习完成事件（新创建的记录）
                    eventPublisher.publishEvent(new LearningCompletedEvent(
                        userId,
                        courseId,
                        Enums.ContentType.course
                    ));
                }
                userCourseDataService.insert(userCourse);
            } else {
                int oldProgress = userCourse.getProgressPercent() != null ? userCourse.getProgressPercent() : 0;
                userCourse.setProgressPercent(finalProgress);
                userCourse.setState(finalProgress >= 10000 ? Enums.UserProgressState.COMPLETED.value() : Enums.UserProgressState.IN_PROGRESS.value());
                if (finalProgress >= 10000 && userCourse.getCompletedAt() == null) {
                    userCourse.setCompletedAt(LocalDateTime.now());
                    // 发布学习完成事件（从进行中变为完成）
                    if (oldProgress < 10000) {
                        eventPublisher.publishEvent(new LearningCompletedEvent(
                            userId,
                            courseId,
                            Enums.ContentType.course
                        ));
                    }
                }
                userCourseDataService.update(userCourse);
            }

            log.info("Updated course {} hierarchical progress: {}%", courseId, finalProgress);

        } catch (Exception e) {
            log.error("Error updating course progress: {}", e.getMessage());
        }
    }

    /**
     * 递归计算层级进度
     *
     * @param node 当前节点
     * @param completedNodes 已完成的节点集合
     * @return 当前节点的完成进度 (0.0 到 1.0)
     */
    private double calculateHierarchicalProgress(JsonNode node, Set<Long> completedNodes) {
        if (node == null || !node.isObject()) {
            return 0.0;
        }

        List<Long> childNodeIds = new ArrayList<>();
        List<JsonNode> childNodes = new ArrayList<>();

        // 遍历所有字段，收集子节点
        node.fieldNames().forEachRemaining(fieldName -> {
            // 跳过特殊字段
            if ("+".equals(fieldName) || "^".equals(fieldName)) {
                return;
            }

            try {
                // 尝试解析为节点ID
                long nodeId = Long.parseLong(fieldName);
                childNodeIds.add(nodeId);
                childNodes.add(node.get(fieldName));
            } catch (NumberFormatException e) {
                // 不是数字的字段名，跳过
            }
        });

        if (childNodeIds.isEmpty()) {
            return 0.0;
        }

        double totalProgress = 0.0;

        for (int i = 0; i < childNodeIds.size(); i++) {
            Long nodeId = childNodeIds.get(i);
            JsonNode childNode = childNodes.get(i);

            if (childNode.isObject() && childNode.size() > 0) {
                // 有子节点，递归计算
                double childProgress = calculateHierarchicalProgress(childNode, completedNodes);
                totalProgress += childProgress;
            } else {
                // 叶子节点，检查是否完成
                if (completedNodes.contains(nodeId)) {
                    totalProgress += 1.0;
                }
            }
        }

        // 返回平均进度
        return totalProgress / childNodeIds.size();
    }
}