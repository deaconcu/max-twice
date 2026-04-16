package com.prosper.learn.learning.enrollment;

import com.prosper.learn.shared.common.utils.ValidationUtils;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.Enums.UserProgressState;
import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户学习记录领域服务
 * 统一处理 roadmap 和 course 的学习记录
 * objectType 使用 Enums.ContentType (roadmap=4, course=8)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLearningDomainService {

    private final UserLearningDataService userLearningDataService;

    // ========== 常量定义 ==========
    private static final int MIN_PROGRESS = 0;
    private static final int MAX_PROGRESS = 10000; // 精度到万分位
    private static final long DEFAULT_MAX_ID = Long.MAX_VALUE;

    // ========== Query 方法 ==========

    /**
     * 根据用户和对象类型获取学习记录
     */
    public UserLearningDO getByUserAndType(Long userId, Enums.ContentType objectType, Long objectId) {
        ValidationUtils.requirePositiveId(userId);
        ValidationUtils.requirePositiveId(objectId);
        return userLearningDataService.getByUserAndObject(userId, objectType.byteValue(), objectId);
    }

    /**
     * 获取用户的学习记录列表（支持按状态和类型过滤）
     *
     * @param userId 用户ID
     * @param objectType 对象类型（roadmap/course，null=全部）
     * @param state 状态过滤（null=全部, 1=进行中, 2=已完成）
     * @param lastId 分页游标（null 返回第一页）
     * @param limit 每页数量
     */
    public List<UserLearningDO> getByUser(
            Long userId, Enums.ContentType objectType, Byte state, Long lastId, int limit) {
        ValidationUtils.requirePositiveId(userId);

        if (objectType != null) {
            return userLearningDataService.getByUserAndType(userId, objectType.byteValue(), state, lastId, limit);
        } else {
            return userLearningDataService.getByUserId(userId, state, lastId, limit);
        }
    }

    /**
     * 根据用户、对象类型和父对象查询学习记录
     * 用于查询：某个 role 下正在学习的 roadmap
     *
     * @param lastId 分页游标（null 返回第一页）
     */
    public List<UserLearningDO> getByUserAndTypeAndParent(
            Long userId, Enums.ContentType objectType, Long parentId, Byte state, Long lastId, int limit) {
        ValidationUtils.requirePositiveId(userId);
        ValidationUtils.requirePositiveId(parentId);

        return userLearningDataService.getByUserAndTypeAndParent(
                userId, objectType.byteValue(), parentId, state, lastId, limit);
    }

    /**
     * 批量查询用户对多个对象的学习进度
     */
    public Map<Long, UserLearningDO> getBatch(long userId, Enums.ContentType objectType, List<Long> objectIds) {
        ValidationUtils.requirePositiveId(userId);

        if (objectIds == null || objectIds.isEmpty()) {
            return Map.of();
        }

        List<UserLearningDO> learnings = userLearningDataService.batchGetByUserAndObjects(
            userId, objectType.byteValue(), objectIds
        );

        return learnings.stream()
            .collect(Collectors.toMap(UserLearningDO::getObjectId, l -> l));
    }

    /**
     * 获取学习进度百分比
     */
    public Integer getProgress(long userId, Enums.ContentType objectType, Long objectId) {
        UserLearningDO learning = userLearningDataService.getByUserAndObject(userId, objectType.byteValue(), objectId);
        return learning != null ? learning.getProgressPercent() : 0;
    }

    /**
     * 检查是否正在学习
     */
    public boolean isLearning(long userId, Enums.ContentType objectType, long objectId) {
        return userLearningDataService.exists(userId, objectType.byteValue(), objectId);
    }

    // ========== Command 方法 ==========

    /**
     * 开始学习（roadmap 或 node）
     * 如果已存在学习记录，抛出异常
     *
     * @param userId 用户ID
     * @param objectType 对象类型（node 或 roadmap）
     * @param objectId 对象ID（nodeId 或 roadmapId）
     * @param isRootNode 是否为课程根节点（仅 objectType=node 时有意义，1=课程根节点，0=普通节点）
     */
    @Transactional
    public void startLearning(Long userId, Enums.ContentType objectType, Long objectId, Byte isRootNode) {
        ValidationUtils.requirePositiveId(userId);
        ValidationUtils.requirePositiveId(objectId);

        byte type = objectType.byteValue();
        if (userLearningDataService.exists(userId, type, objectId)) {
            throw StatusCode.USER_COURSE_ALREADY_STARTED.exception();
        }

        UserLearningDO learning = new UserLearningDO();
        learning.setUserId(userId);
        learning.setObjectType(type);
        learning.setObjectId(objectId);
        learning.setIsRootNode(isRootNode != null ? isRootNode : (byte) 0);
        learning.setProgressPercent(MIN_PROGRESS);
        learning.setStartedAt(LocalDateTime.now());

        userLearningDataService.insert(learning);

        log.info("用户 {} 开始学习 {}(type={}, isRootNode={})", userId, objectId, objectType, isRootNode);
    }

    /**
     * 取消学习（删除学习记录）
     * 如果不存在学习记录，抛出异常
     */
    @Transactional
    public void cancelLearning(Long userId, Enums.ContentType objectType, Long objectId) {
        ValidationUtils.requirePositiveId(userId);
        ValidationUtils.requirePositiveId(objectId);

        byte type = objectType.byteValue();
        if (!userLearningDataService.exists(userId, type, objectId)) {
            throw StatusCode.USER_COURSE_NOT_STARTED.exception();
        }

        userLearningDataService.deleteByUserAndObject(userId, type, objectId);

        log.info("用户 {} 取消学习 {}(type={})", userId, objectId, objectType);
    }

    /**
     * 更新学习进度
     */
    @Transactional
    public void updateProgress(Long userId, Enums.ContentType objectType, Long objectId, Integer progressPercent) {
        ValidationUtils.requirePositiveId(userId);
        ValidationUtils.requirePositiveId(objectId);

        if (progressPercent == null || progressPercent < MIN_PROGRESS || progressPercent > MAX_PROGRESS) {
            throw StatusCode.INVALID_PARAMETER.exception("进度百分比必须在 0-10000 之间");
        }

        byte type = objectType.byteValue();
        UserLearningDO learning = userLearningDataService.getByUserAndObject(userId, type, objectId);
        if (learning == null) {
            throw StatusCode.USER_COURSE_NOT_FOUND.exception();
        }

        learning.setProgressPercent(progressPercent);

        // 根据进度自动设置 completedAt
        if (progressPercent >= MAX_PROGRESS) {
            if (learning.getCompletedAt() == null) {
                learning.setCompletedAt(LocalDateTime.now());
            }
        } else {
            // 进度小于100%，清除完成时间
            learning.setCompletedAt(null);
        }

        userLearningDataService.update(learning);

        log.info("更新用户 {} 学习 {}(type={}) 进度为 {}", userId, objectId, objectType, progressPercent);
    }

    /**
     * 查询包含指定节点的课程学习记录
     * 用于节点完成时，反向查找需要更新进度的课程
     *
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @return 包含该节点的学习记录列表
     */
    public List<UserLearningDO> findByNodeContained(Long userId, Long nodeId) {
        ValidationUtils.requirePositiveId(userId);
        ValidationUtils.requirePositiveId(nodeId);

        return userLearningDataService.findByNodeContained(userId, nodeId);
    }

    /**
     * 查询用户学习的所有课程（objectType=node 且是课程根节点）
     * 通过 course 表 JOIN 过滤出课程
     *
     * @param userId 用户ID
     * @param state 状态过滤（null=全部, 1=进行中, 2=已完成）
     * @param lastId 分页游标（null=第一页）
     * @param limit 每页数量
     */
    public List<UserLearningDO> getCoursesByUser(Long userId, Byte state, Long lastId, int limit) {
        ValidationUtils.requirePositiveId(userId);
        return userLearningDataService.getCoursesByUser(userId, state, lastId, limit);
    }

    /**
     * 更新学习记录的 nodes 字段
     */
    @Transactional
    public void updateNodes(Long userId, Enums.ContentType objectType, Long objectId, String nodes) {
        ValidationUtils.requirePositiveId(userId);
        ValidationUtils.requirePositiveId(objectId);

        userLearningDataService.updateNodes(userId, objectType.byteValue(), objectId, nodes);

        log.debug("更新用户 {} 的学习记录 {} 节点列表", userId, objectId);
    }
}
