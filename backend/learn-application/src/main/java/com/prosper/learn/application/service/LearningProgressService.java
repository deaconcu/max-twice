package com.prosper.learn.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.prosper.learn.application.converter.NodeConverter;
import com.prosper.learn.application.dto.response.CourseCompletionResponseDTO;
import com.prosper.learn.application.dto.response.NodeProgressResponseDTO;
import com.prosper.learn.application.dto.response.node.NodeWithProgressDTO;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.toc.TocDomainService;
import com.prosper.learn.learning.enrollment.UserLearningDO;
import com.prosper.learn.learning.enrollment.UserLearningDomainService;
import com.prosper.learn.learning.progress.LearningProgressDomainService;
import com.prosper.learn.shared.common.util.TimeZoneUtil;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.domain.event.user.learning.LearningCompletedEvent;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final CourseDataService courseDataService;
    private final TocDomainService tocService;
    private final NodeConverter nodeConverter;
    private final UserLearningDomainService userLearningDomainService;
    private final UserLearningService userLearningService;
    private final ObjectMapper objectMapper;
    private final SystemProperties systemProperties;
    private final ApplicationEventPublisher eventPublisher;

    // ========== Command 方法（写操作）==========

    /**
     * 标记节点完成并返回完整的响应数据
     */
    @Transactional
    public NodeProgressResponseDTO markNodeCompletedWithResponse(long userId, long nodeId, long rootNodeId, LocalDate userToday) {
        // 验证节点是否存在
        nodeDataService.validateAndGet(nodeId);
        nodeDataService.validateAndGet(rootNodeId);

        // 调用领域服务处理核心逻辑（标记节点完成）
        domainService.markNodeCompleted(userId, nodeId, userToday);

        // 更新所有受影响课程的进度
        updateAffectedCoursesProgress(userId, nodeId);

        // 获取更新后的课程进度（使用根节点）
        UserLearningDO learning = userLearningDomainService.getByUserAndType(
                userId, Enums.ContentType.node, rootNodeId);
        Integer courseProgress = learning != null ? learning.getProgressPercent() : 0;

        // 计算可完成的节点列表（基于根节点的目录1）
        List<Long> completableNodeIds = findCompletableNodes(userId, rootNodeId);

        // 构建响应DTO
        return NodeProgressResponseDTO.builder()
                .nodeId(nodeId)
                .completed(true)
                .courseProgressPercent(courseProgress)
                .completableNodeIds(completableNodeIds)
                .build();
    }

    /**
     * 取消节点完成并返回完整的响应数据
     */
    @Transactional
    public NodeProgressResponseDTO unmarkNodeCompletedWithResponse(long userId, long nodeId, long rootNodeId, LocalDate userToday) {
        // 验证节点是否存在
        nodeDataService.validateAndGet(nodeId);
        nodeDataService.validateAndGet(rootNodeId);

        // 调用领域服务处理核心逻辑（取消节点完成）
        domainService.unmarkNodeCompleted(userId, nodeId, rootNodeId, userToday);

        // 更新所有受影响课程的进度
        updateAffectedCoursesProgress(userId, nodeId);

        // 获取更新后的课程进度（使用根节点）
        UserLearningDO learning = userLearningDomainService.getByUserAndType(
                userId, Enums.ContentType.node, rootNodeId);
        Integer courseProgress = learning != null ? learning.getProgressPercent() : 0;

        // 计算可完成的节点列表（基于根节点的目录1）
        List<Long> completableNodeIds = findCompletableNodes(userId, rootNodeId);

        // 构建响应DTO
        return NodeProgressResponseDTO.builder()
                .nodeId(nodeId)
                .completed(false)
                .courseProgressPercent(courseProgress)
                .completableNodeIds(completableNodeIds)
                .build();
    }

    // ========== Query 方法（读操作）==========

    /**
     * 获取节点完成状态响应数据
     */
    public NodeProgressResponseDTO getNodeCompletionStatusResponse(long userId, long nodeId) {
        // 验证节点存在（跨域依赖）
        NodeDO nodeDO = nodeDataService.getById(nodeId);
        if (nodeDO == null) {
            throw StatusCode.LEARNING_PROGRESS_INVALID_NODE_ID.exception();
        }

        // 检查完成状态
        boolean isCompleted = domainService.isNodeCompleted(userId, nodeId);

        // 构建响应DTO
        return NodeProgressResponseDTO.builder()
                .nodeId(nodeId)
                .completed(isCompleted)
                .build();
    }

    // ========== 委托方法（直接委托给DomainService）==========

    /**
     * 检查用户是否完成了指定节点
     */
    public boolean isNodeCompleted(Long userId, Long nodeId) {
        return domainService.isNodeCompleted(userId, nodeId);
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
        return domainService.getCompletedNodesInList(userId, nodeIds);
    }

    /**
     * 获取课程进度百分比
     */
    public Integer getCourseProgress(long userId, Long courseId) {
        return userLearningService.getCourseProgress(userId, courseId);
    }

    /**
     * 计算节点进度（实时计算）
     *
     * @param userId 用户ID
     * @param nodeId 节点ID（可以是课程根节点或普通节点）
     * @return 进度百分比 (0-10000，精度到万分位)
     */
    public Integer calculateNodeProgress(long userId, long nodeId) {
        try {
            // 1. 获取用户目录的内容
            String tocContent = tocService.getToc(userId, nodeId, 1);
            if (tocContent == null || tocContent.trim().isEmpty()) {
                return 0;
            }

            // 2. 解析目录结构
            JsonNode tocNode = objectMapper.readTree(tocContent);

            // 3. 收集目录中的所有节点ID（使用 TocDomainService）
            Set<Long> nodeIdsInToc = tocService.collectNodeIdsFromToc(tocContent);

            // 4. 批量检查哪些节点已完成
            Set<Long> completedNodes = domainService.getCompletedNodesInList(userId, nodeIdsInToc);

            // 5. 递归计算层级进度
            double progressPercent = calculateHierarchicalProgress(tocNode, completedNodes) * 10000;
            return (int) Math.floor(progressPercent);

        } catch (Exception e) {
            log.error("计算节点 {} 进度失败: {}", nodeId, e.getMessage());
            return 0;
        }
    }

    /**
     * 批量计算节点进度（实时计算）
     *
     * @param userId 用户ID
     * @param nodeIds 节点ID列表
     * @return Map<nodeId, progress> 进度百分比 (0-10000)
     */
    public Map<Long, Integer> batchCalculateNodeProgress(long userId, List<Long> nodeIds) {
        Map<Long, Integer> progressMap = new HashMap<>();

        if (nodeIds == null || nodeIds.isEmpty()) {
            return progressMap;
        }

        try {
            // 1. 批量获取所有节点的 ToC
            Map<Long, String> tocMap = tocService.batchGetToc(userId, nodeIds);

            // 2. 收集所有目录中的节点ID
            Set<Long> allNodeIdsInTocs = new HashSet<>();
            Map<Long, Set<Long>> tocNodeIdsMap = new HashMap<>();

            for (Long nodeId : nodeIds) {
                String tocContent = tocMap.get(nodeId);
                if (tocContent != null && !tocContent.trim().isEmpty()) {
                    try {
                        Set<Long> nodeIdsInToc = tocService.collectNodeIdsFromToc(tocContent);
                        tocNodeIdsMap.put(nodeId, nodeIdsInToc);
                        allNodeIdsInTocs.addAll(nodeIdsInToc);
                    } catch (Exception e) {
                        log.error("解析节点 {} 的目录失败: {}", nodeId, e.getMessage());
                    }
                }
            }

            // 3. 一次性批量查询所有相关节点的完成状态（性能优化）
            Set<Long> completedNodes = domainService.getCompletedNodesInList(userId, allNodeIdsInTocs);

            // 4. 遍历每个节点计算进度
            for (Long nodeId : nodeIds) {
                try {
                    String tocContent = tocMap.get(nodeId);
                    if (tocContent == null || tocContent.trim().isEmpty()) {
                        progressMap.put(nodeId, 0);
                        continue;
                    }

                    // 解析并计算进度
                    JsonNode tocNode = objectMapper.readTree(tocContent);
                    double progressPercent = calculateHierarchicalProgress(tocNode, completedNodes) * 10000;
                    progressMap.put(nodeId, (int) Math.floor(progressPercent));

                } catch (Exception e) {
                    log.error("计算节点 {} 进度失败: {}", nodeId, e.getMessage());
                    progressMap.put(nodeId, 0);
                }
            }

        } catch (Exception e) {
            log.error("批量计算节点进度失败", e);
        }

        return progressMap;
    }

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
            // 获取课程的根节点ID
            CourseDO course = courseDataService.validateAndGet(courseId);

            // 1. 获取用户目录1的内容（跨域依赖：content领域，使用根节点ID）
            String toc1Content = tocService.getToc(userId, course.getRootNodeId(), 1);
            if (toc1Content == null) return;

            // 2. 解析目录结构
            JsonNode tocNode = objectMapper.readTree(toc1Content);

            // 3. 收集目录中的所有节点ID（使用 TocDomainService）
            Set<Long> nodeIdsInToc = tocService.collectNodeIdsFromToc(toc1Content);

            // 4. 批量检查哪些节点已完成
            Set<Long> completedNodes = domainService.getCompletedNodesInList(userId, nodeIdsInToc);

            // 5. 递归计算层级进度
            double progressPercent = calculateHierarchicalProgress(tocNode, completedNodes) * 10000;
            int finalProgress = (int) Math.floor(progressPercent);

            // 5. 更新或创建用户节点学习记录（使用 rootNodeId）
            Long rootNodeId = course.getRootNodeId();
            UserLearningDO userCourse = userLearningDomainService.getByUserAndType(userId, Enums.ContentType.node, rootNodeId);

            if (userCourse == null) {
                userLearningDomainService.startLearning(userId, Enums.ContentType.node, rootNodeId, Enums.Bool.TRUE.value());
                userLearningDomainService.updateProgress(userId, Enums.ContentType.node, rootNodeId, finalProgress);

                if (finalProgress >= 10000) {
                    // 发布学习完成事件（新创建的记录，事件中使用 courseId）
                    eventPublisher.publishEvent(new LearningCompletedEvent(
                        userId,
                        courseId,
                        Enums.ContentType.course
                    ));
                }
            } else {
                int oldProgress = userCourse.getProgressPercent() != null ? userCourse.getProgressPercent() : 0;
                userLearningDomainService.updateProgress(userId, Enums.ContentType.node, rootNodeId, finalProgress);

                // 发布学习完成事件（从进行中变为完成，事件中使用 courseId）
                if (finalProgress >= 10000 && oldProgress < 10000) {
                    eventPublisher.publishEvent(new LearningCompletedEvent(
                        userId,
                        courseId,
                        Enums.ContentType.course
                    ));
                }
            }

            log.info("学习进度 课程 {} 层级进度更新: {}%", courseId, finalProgress);

        } catch (Exception e) {
            log.error("学习进度 课程进度更新失败: {}", e.getMessage());
        }
    }

    /**
     * 递归计算层级进度（支持父节点覆盖逻辑）
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

            // 关键：先检查节点本身是否已完成（覆盖逻辑）
            if (completedNodes.contains(nodeId)) {
                // 节点已完成 → 完成度 100%，不再递归子节点
                totalProgress += 1.0;
                continue;
            }

            // 节点未完成，检查是否有子节点
            if (childNode.isObject() && childNode.size() > 0) {
                // 有子节点，递归计算
                double childProgress = calculateHierarchicalProgress(childNode, completedNodes);
                totalProgress += childProgress;
            } else {
                // 叶子节点且未完成
                totalProgress += 0.0;
            }
        }

        // 返回平均进度
        return totalProgress / childNodeIds.size();
    }

    /**
     * 更新所有受影响课程的进度
     * 节点完成/取消完成后调用
     *
     * @param userId 用户ID
     * @param nodeId 节点ID
     */
    private void updateAffectedCoursesProgress(long userId, long nodeId) {
        try {
            // 1. 查询包含这个节点的所有课程学习记录（通过 nodes 字段）
            List<UserLearningDO> affectedLearnings = userLearningDomainService.findByNodeContained(userId, nodeId);

            if (affectedLearnings.isEmpty()) {
                log.debug("节点 {} 不在用户 {} 的任何课程中", nodeId, userId);
                return;
            }

            log.info("节点 {} 完成状态变化，影响用户 {} 的 {} 门课程", nodeId, userId, affectedLearnings.size());

            // 2. 遍历每门课程，重新计算并更新进度
            for (UserLearningDO learning : affectedLearnings) {
                updateSingleCourseProgress(userId, learning);
            }

        } catch (Exception e) {
            log.error("更新受影响课程进度失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 更新单个节点学习的进度
     * learning.objectId 就是 nodeId（可能是课程根节点或普通节点）
     *
     * @param userId 用户ID
     * @param learning 学习记录（objectType=node）
     */
    private void updateSingleCourseProgress(long userId, UserLearningDO learning) {
        try {
            Long nodeId = learning.getObjectId();  // 这里的 objectId 就是 nodeId

            // 计算新进度
            Integer newProgress = calculateNodeProgress(userId, nodeId);
            Integer oldProgress = learning.getProgressPercent() != null ? learning.getProgressPercent() : 0;

            // 更新进度到数据库
            userLearningDomainService.updateProgress(userId, Enums.ContentType.node, nodeId, newProgress);

            // 如果从未完成变为完成，发布完成事件
            // 注意：这里无法知道具体的 courseId，只能用 nodeId
            // 如果需要 courseId，可以查询 node.courseId，但对于普通节点可能没有意义
            if (newProgress >= 10000 && oldProgress < 10000) {
                // TODO: 是否需要发布事件？如果需要，可能需要查询 node.courseId
                log.info("用户 {} 完成节点 {} 的学习", userId, nodeId);
            }

            log.debug("更新节点 {} 进度：{} -> {}", nodeId, oldProgress, newProgress);

        } catch (Exception e) {
            log.error("更新节点 {} 进度失败：{}", learning.getObjectId(), e.getMessage(), e);
        }
    }

    /**
     * 查找可完成的节点列表
     * 这些节点的递归完成度达到100%但节点本身未完成
     *
     * @param userId 用户ID
     * @param rootNodeId 根节点ID
     * @return 可完成的节点ID列表
     */
    public List<Long> findCompletableNodes(long userId, long rootNodeId) {
        List<Long> completableNodeIds = new ArrayList<>();

        try {
            // 1. 获取用户目录1的内容
            String tocContent = tocService.getToc(userId, rootNodeId, 1);
            if (tocContent == null || tocContent.trim().isEmpty()) {
                return completableNodeIds;
            }

            // 2. 解析目录结构
            JsonNode tocNode = objectMapper.readTree(tocContent);

            // 3. 收集目录中的所有节点ID（使用 TocDomainService）
            Set<Long> nodeIdsInToc = tocService.collectNodeIdsFromToc(tocContent);

            // 4. 批量检查哪些节点已完成
            Set<Long> completedNodes = domainService.getCompletedNodesInList(userId, nodeIdsInToc);

            // 5. 递归查找可完成的节点
            findCompletableNodesRecursive(tocNode, completedNodes, completableNodeIds);

        } catch (Exception e) {
            log.error("查找可完成节点失败: {}", e.getMessage(), e);
        }

        return completableNodeIds;
    }

    /**
     * 递归查找可完成的节点
     *
     * @param node 当前节点
     * @param completedNodes 已完成的节点集合
     * @param completableNodeIds 用于收集可完成节点ID的列表
     */
    private void findCompletableNodesRecursive(
            JsonNode node,
            Set<Long> completedNodes,
            List<Long> completableNodeIds) {
        if (node == null || !node.isObject()) {
            return;
        }

        node.fieldNames().forEachRemaining(fieldName -> {
            // 跳过特殊字段
            if ("+".equals(fieldName) || "^".equals(fieldName)) {
                return;
            }

            try {
                long nodeId = Long.parseLong(fieldName);
                JsonNode childNode = node.get(fieldName);

                // 检查是否有子节点
                boolean hasChildren = childNode != null && childNode.isObject() && childNode.size() > 0;

                if (hasChildren) {
                    // 如果节点未完成，且所有子节点都完成了，标记为可完成
                    if (!completedNodes.contains(nodeId)) {
                        double progress = calculateHierarchicalProgress(childNode, completedNodes);
                        if (progress >= 0.999999) {
                            completableNodeIds.add(nodeId);
                        }
                    }

                    // 无论节点是否完成，都继续递归检查子节点
                    findCompletableNodesRecursive(childNode, completedNodes, completableNodeIds);
                }
            } catch (NumberFormatException e) {
                // 不是数字的字段名，跳过
            }
        });
    }
}