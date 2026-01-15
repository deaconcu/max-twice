package com.prosper.learn.learning.enrollment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.event.user.learning.LearningStartedEvent;
import com.prosper.learn.shared.domain.event.user.learning.LearningCompletedEvent;
import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 用户路线图领域服务
 *
 * 负责用户路线图领域内的核心业务逻辑，只依赖learning领域模块
 * 处理用户路线图的创建、更新、删除等核心功能
 *
 * @author Claude
 * @since 2024-12-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoadmapDomainService {

    private final UserRoadmapDataService userRoadmapDataService;
    private final ApplicationEventPublisher eventPublisher;

    // 不变常量 - 进度相关
    private static final int INITIAL_PROGRESS = 0;
    // 同一职业下最多同时学习的路线图数量
    private static final int MAX_LEARNING_ROADMAPS_PER_PROFESSION = 10;

    // ========== Command 方法（写操作）==========

    /**
     * 用户开始学习路线图（核心领域逻辑）
     * 如果已存在学习记录，抛出异常
     *
     * @param userId 用户ID
     * @param roadmapId 路线图ID
     * @param professionId 职业ID（冗余字段）
     * @throws BusinessException 如果路线图已开始学习
     */
    public void startRoadmap(Long userId, Long roadmapId, Long professionId) {
        // 检查是否已经存在学习记录
        UserRoadmapDO existing = userRoadmapDataService.getByUserAndRoadmap(userId, roadmapId);

        if (existing != null) {
            throw StatusCode.USER_ROADMAP_ALREADY_STARTED.exception();
        }

        // 检查该职业下正在学习的路线图数量
        int learningCount = userRoadmapDataService.countLearningByProfession(userId, professionId);
        if (learningCount >= MAX_LEARNING_ROADMAPS_PER_PROFESSION) {
            throw StatusCode.LEARNING_ROADMAP_LIMIT_EXCEEDED.exception(
                "同一职业下最多同时学习" + MAX_LEARNING_ROADMAPS_PER_PROFESSION + "条路线图");
        }

        // 创建新的学习记录
        UserRoadmapDO userRoadmapDO = createInitialUserRoadmap(userId, roadmapId, professionId);
        userRoadmapDataService.insert(userRoadmapDO);

        // 发布学习开始事件
        eventPublisher.publishEvent(new LearningStartedEvent(
            userId,
            roadmapId,
            ContentType.roadmap
        ));

        log.info("用户 {} 开始学习路线图 {}", userId, roadmapId);
    }

    /**
     * 取消学习路线图（删除学习记录）
     * 如果不存在学习记录，抛出异常
     *
     * @param userId 用户ID
     * @param roadmapId 路线图ID
     * @throws BusinessException 如果路线图尚未开始学习
     */
    public void cancelRoadmap(Long userId, Long roadmapId) {
        UserRoadmapDO existing = userRoadmapDataService.getByUserAndRoadmap(userId, roadmapId);

        if (existing == null) {
            throw StatusCode.USER_ROADMAP_NOT_STARTED.exception();
        }

        userRoadmapDataService.deleteByUserAndRoadmap(userId, roadmapId);

        log.info("用户 {} 取消学习路线图 {}", userId, roadmapId);
    }

    /**
     * 更新路线图学习进度（核心领域逻辑）
     * @param userId 用户ID
     * @param roadmapId 路线图ID
     * @param progressPercent 进度百分比
     * @return 更新后的学习进度记录
     */
    public UserRoadmapDO updateProgress(Long userId, Long roadmapId, Integer progressPercent) {
        UserRoadmapDO userRoadmapDO = userRoadmapDataService.getByUserAndRoadmap(userId, roadmapId);

        if (userRoadmapDO == null) {
            throw StatusCode.USER_ROADMAP_NOT_FOUND.exception();
        }

        userRoadmapDO.setProgressPercent(progressPercent);

        // 如果进度达到100%，标记为完成
        if (progressPercent >= 100) {
            boolean wasNotCompleted = userRoadmapDO.getState() != UserProgressState.COMPLETED.value();
            userRoadmapDO.setState(UserProgressState.COMPLETED.value());
            userRoadmapDO.setCompletedAt(LocalDateTime.now());

            // 如果是首次完成，发布学习完成事件
            if (wasNotCompleted) {
                eventPublisher.publishEvent(new LearningCompletedEvent(
                    userId,
                    roadmapId,
                    ContentType.roadmap
                ));
            }
        } else if (progressPercent > 0) {
            userRoadmapDO.setState(UserProgressState.IN_PROGRESS.value());
        }

        userRoadmapDataService.update(userRoadmapDO);
        return userRoadmapDO;
    }

    /**
     * 批量更新用户路线图记录（核心领域逻辑）
     * @param toUpdateList 需要更新的记录列表
     */
    public void updateBatch(List<UserRoadmapDO> toUpdateList) {
        if (!toUpdateList.isEmpty()) {
            userRoadmapDataService.updateBatch(toUpdateList);
            log.debug("批量更新 {} 条用户路线图记录", toUpdateList.size());
        }
    }

    /**
     * 更新路线图完成状态（核心领域逻辑）
     * @param userRoadmapDO 用户路线图记录
     * @param overallProgress 整体进度
     * @return 是否有状态更新
     */
    public boolean updateRoadmapCompletionStatus(UserRoadmapDO userRoadmapDO, double overallProgress) {
        // 如果已经是COMPLETED状态，无需再检查
        if (UserProgressState.COMPLETED.value() == userRoadmapDO.getState()) {
            return false;
        }

        boolean needUpdate = false;

        // 如果完成度达到100%，更新状态为COMPLETED
        if (overallProgress >= 100.0) {
            boolean wasNotCompleted = userRoadmapDO.getState() != UserProgressState.COMPLETED.value();
            userRoadmapDO.setState(UserProgressState.COMPLETED.value());
            userRoadmapDO.setProgressPercent(100);
            if (userRoadmapDO.getCompletedAt() == null) {
                userRoadmapDO.setCompletedAt(LocalDateTime.now());
            }

            // 如果是首次完成，发布学习完成事件
            if (wasNotCompleted) {
                eventPublisher.publishEvent(new LearningCompletedEvent(
                    userRoadmapDO.getUserId(),
                    userRoadmapDO.getRoadmapId(),
                    ContentType.roadmap
                ));
            }

            needUpdate = true;
        } else if (overallProgress > 0 && UserProgressState.NOT_STARTED.value() == userRoadmapDO.getState()) {
            // 如果有进度但状态还是NOT_STARTED，更新为IN_PROGRESS
            userRoadmapDO.setState(UserProgressState.IN_PROGRESS.value());
            userRoadmapDO.setProgressPercent((int) Math.round(overallProgress));
            needUpdate = true;
        }

        return needUpdate;
    }

    /**
     * 根据路线图内容更新进度状态（核心业务逻辑）
     * @param userRoadmapDO 用户路线图记录
     * @param content 路线图内容JSON
     * @return 是否需要更新
     */
    public boolean updateRoadmapProgressFromContent(UserRoadmapDO userRoadmapDO, String content) {
        try {
            // 如果已经是COMPLETED状态，无需再检查
            if (UserProgressState.COMPLETED.value() == userRoadmapDO.getState()) {
                return false;
            }

            // 解析JSON获取进度信息
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(content);
            JsonNode nodesNode = rootNode.get("nodes");

            if (nodesNode != null && nodesNode.isArray()) {
                int totalCourses = 0;
                double totalProgress = 0.0;

                for (JsonNode node : nodesNode) {
                    totalCourses++;
                    boolean finished = node.get("finished").asBoolean(false);
                    double progress = node.get("progress").asDouble(0.0);

                    if (finished) {
                        totalProgress += 100.0;
                    } else {
                        totalProgress += progress;
                    }
                }

                // 计算整体完成度
                double overallProgress = totalCourses > 0 ? totalProgress / totalCourses : 0.0;

                // 更新状态
                return updateRoadmapCompletionStatus(userRoadmapDO, overallProgress);
            }
        } catch (Exception e) {
            log.warn("Failed to parse roadmap content for progress update: roadmapId={}",
                    userRoadmapDO.getRoadmapId(), e);
        }
        return false;
    }

    // ========== Query 方法（读操作）==========

    /**
     * 获取用户的路线图学习进度记录
     * @param userId 用户ID
     * @param roadmapId 路线图ID
     * @return 学习进度记录，如果不存在返回null
     */
    public UserRoadmapDO getByUserAndRoadmap(Long userId, Long roadmapId) {
        return userRoadmapDataService.getByUserAndRoadmap(userId, roadmapId);
    }

    /**
     * 获取用户的全部路线图学习进度记录
     * @param userId 用户ID
     * @return 用户所有路线图学习进度列表
     */
    public List<UserRoadmapDO> getByUser(Long userId) {
        return userRoadmapDataService.getByUser(userId);
    }

    /**
     * 获取用户正在学习的职业路线图
     * @param userId 用户ID
     * @param professionId 职业ID
     * @param limit 最大返回数量
     * @return 正在学习的路线图列表
     */
    public List<UserRoadmapDO> getLearningByProfession(Long userId, Long professionId, int limit) {
        return userRoadmapDataService.getLearningByProfession(userId, professionId, limit);
    }

    // ========== Private 辅助方法 ==========

    /**
     * 创建初始用户路线图记录
     */
    private UserRoadmapDO createInitialUserRoadmap(Long userId, Long roadmapId, Long professionId) {
        UserRoadmapDO userRoadmapDO = new UserRoadmapDO();
        userRoadmapDO.setUserId(userId);
        userRoadmapDO.setRoadmapId(roadmapId);
        userRoadmapDO.setProfessionId(professionId);
        userRoadmapDO.setProgressPercent(INITIAL_PROGRESS);
        userRoadmapDO.setState(UserProgressState.IN_PROGRESS.value());
        userRoadmapDO.setStartedAt(LocalDateTime.now());
        return userRoadmapDO;
    }
}