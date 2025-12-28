package com.prosper.learn.learning.progress;

import com.prosper.learn.learning.enrollment.UserCourseDO;
import com.prosper.learn.learning.enrollment.UserCourseDataService;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.event.user.learning.LearningCompletedEvent;
import com.prosper.learn.shared.domain.exception.BusinessException;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.shared.common.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学习进度领域服务
 *
 * 负责学习进度的核心业务逻辑，只依赖 learning 领域内的组件
 * 使用Redis Set存储用户完成的节点，MySQL作为持久化备份
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningProgressDomainService {

    private final RedisTemplate<String, String> redisTemplate;
    private final UserProgressDataService userProgressDataService;
    private final UserCourseDataService userCourseDataService;
    private final SystemProperties systemProperties;
    private final ApplicationEventPublisher eventPublisher;

    private static final String USER_COMPLETED_KEY_PREFIX = "user:completed:";
    private static final String SYNC_FAILED_USERS_KEY = "sync:failed:users";

    // ========== 私有辅助方法 ==========

    /**
     * 获取Redis缓存过期时间
     */
    private Duration getCacheExpireTime() {
        int days = systemProperties.getLearningProgress().getCacheExpireDays();
        return Duration.ofDays(days);
    }

    /**
     * 生成用户完成进度Redis键
     */
    private String generateUserCompletedKey(long userId) {
        return USER_COMPLETED_KEY_PREFIX + userId;
    }

    /**
     * 确保Redis中有用户数据（懒加载）
     */
    private void ensureUserDataInRedis(long userId) {
        String key = generateUserCompletedKey(userId);
        if (!redisTemplate.hasKey(key)) {
            loadUserDataToRedis(userId);
        }
    }

    /**
     * 从数据库加载用户数据到Redis（私有方法，懒加载）
     */
    private void loadUserDataToRedis(Long userId) {
        try {
            log.debug("Loading user {} data from database to Redis", userId);

            UserProgressDO record = userProgressDataService.getByUserId(userId);
            if (record == null || record.getNodeIds() == null || record.getNodeIds().isEmpty()) {
                log.debug("No learning data found for user {} in database", userId);
                return;
            }

            String key = USER_COMPLETED_KEY_PREFIX + userId;

            // 解析节点ID字符串
            String[] nodeIds = record.getNodeIds().split(",");
            String[] trimmedNodeIds = Arrays.stream(nodeIds)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

            if (trimmedNodeIds.length > 0) {
                // 批量添加到Redis Set
                redisTemplate.opsForSet().add(key, trimmedNodeIds);
                redisTemplate.expire(key, getCacheExpireTime());

                log.debug("Loaded {} completed nodes for user {} from database to Redis",
                    trimmedNodeIds.length, userId);
            }

        } catch (Exception e) {
            log.error("Error loading user {} data to Redis: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 将用户在Redis中的数据同步到数据库
     */
    private void syncUserToDatabase(long userId) {
        try {
            String key = USER_COMPLETED_KEY_PREFIX + userId;

            // 从Redis获取用户完成的节点
            Set<String> nodeStrings = redisTemplate.opsForSet().members(key);
            if (nodeStrings == null || nodeStrings.isEmpty()) {
                log.debug("No data to sync for user {}", userId);
                return;
            }

            // 转换为逗号分隔的字符串
            String completedNodeIds = String.join(",", nodeStrings);

            // 更新数据库
            UserProgressDO record = new UserProgressDO();
            record.setUserId(userId);
            record.setNodeIds(completedNodeIds);
            record.setCount(nodeStrings.size());

            int updated = userProgressDataService.upsert(record);

            if (updated > 0) {
                log.debug("Successfully synced {} completed nodes for user {} to database",
                    nodeStrings.size(), userId);
            } else {
                throw new RuntimeException("Database upsert returned 0, sync may have failed");
            }

        } catch (Exception e) {
            log.error("Error syncing user {} data to database: {}", userId, e.getMessage(), e);
            throw e; // 重新抛出异常，让调用方知道同步失败
        }
    }

    /**
     * Redis失败时的降级处理：直接操作数据库
     */
    private boolean fallbackToDatabase(long userId, long nodeId) {
        try {
            // 从数据库读取现有数据
            UserProgressDO record = userProgressDataService.getByUserId(userId);

            Set<Long> completedNodes = new HashSet<>();
            if (record != null && record.getNodeIds() != null && !record.getNodeIds().isEmpty()) {
                completedNodes = parseNodeIds(record.getNodeIds());
            }

            // 检查是否已存在
            if (completedNodes.contains(nodeId)) {
                return false;
            }

            // 添加新节点并保存
            completedNodes.add(nodeId);
            String nodeIdsString = completedNodes.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

            UserProgressDO newRecord = new UserProgressDO();
            newRecord.setUserId(userId);
            newRecord.setNodeIds(nodeIdsString);
            newRecord.setCount(completedNodes.size());

            int updated = userProgressDataService.upsert(newRecord);

            if (updated > 0) {
                log.info("Fallback: Successfully marked node {} as completed for user {} in database",
                    nodeId, userId);
                return true;
            } else {
                log.error("Fallback: Failed to update database for user {} node {}", userId, nodeId);
                return false;
            }

        } catch (Exception e) {
            log.error("Fallback: Error updating database for user {} node {}: {}",
                userId, nodeId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Redis失败时的降级处理：直接操作数据库取消完成
     */
    private boolean fallbackUnmarkToDatabase(long userId, long nodeId) {
        try {
            // 从数据库读取现有数据
            UserProgressDO record = userProgressDataService.getByUserId(userId);

            if (record == null || record.getNodeIds() == null || record.getNodeIds().isEmpty()) {
                return false; // 没有完成记录
            }

            Set<Long> completedNodes = parseNodeIds(record.getNodeIds());

            // 检查是否存在
            if (!completedNodes.contains(nodeId)) {
                return false;
            }

            // 移除节点并保存
            completedNodes.remove(nodeId);
            String nodeIdsString = "";
            if (!completedNodes.isEmpty()) {
                nodeIdsString = completedNodes.stream()
                    .sorted()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            }

            UserProgressDO newRecord = new UserProgressDO();
            newRecord.setUserId(userId);
            newRecord.setNodeIds(nodeIdsString);
            newRecord.setCount(completedNodes.size());

            int updated = userProgressDataService.upsert(newRecord);

            if (updated > 0) {
                log.info("Fallback: Successfully unmarked node {} as completed for user {} in database",
                    nodeId, userId);
                return true;
            } else {
                log.error("Fallback: Failed to update database for user {} node {}", userId, nodeId);
                return false;
            }

        } catch (Exception e) {
            log.error("Fallback: Error updating database for user {} node {}: {}",
                userId, nodeId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 解析逗号分隔的节点ID字符串
     */
    private Set<Long> parseNodeIds(String nodeIdsString) {
        if (nodeIdsString == null || nodeIdsString.trim().isEmpty()) {
            return new HashSet<>();
        }

        try {
            return Arrays.stream(nodeIdsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
        } catch (NumberFormatException e) {
            log.error("Error parsing node IDs string: {}", nodeIdsString, e);
            return new HashSet<>();
        }
    }

    // ========== Command 方法（写操作）==========

    /**
     * 标记节点为已完成
     * 如果节点已完成，抛出异常
     *
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @param courseId 课程ID
     * @throws BusinessException 如果节点已是完成状态
     */
    public void markNodeCompleted(long userId, long nodeId, long courseId) {
        ValidationUtils.requirePositiveId(userId);
        ValidationUtils.requirePositiveId(nodeId);
        ValidationUtils.requirePositiveId(courseId);

        try {
            String key = generateUserCompletedKey(userId);

            ensureUserDataInRedis(userId);

            // 1. 立即更新Redis（用户立即看到结果）
            Long added = redisTemplate.opsForSet().add(key, Long.toString(nodeId));
            redisTemplate.expire(key, getCacheExpireTime());

            if (added == null || added == 0) {
                // 节点已在完成集合中，抛出异常
                throw StatusCode.NODE_ALREADY_COMPLETED.exception();
            }

            log.info("Successfully marked node {} as completed for user {} in Redis", nodeId, userId);

            // 2. 尝试同步更新数据库
            try {
                syncUserToDatabase(userId);
            } catch (Exception dbException) {
                // 数据库更新失败，记录到待补偿队列
                log.warn("Database sync failed for user {}, added to retry queue: {}",
                    userId, dbException.getMessage());
                redisTemplate.opsForSet().add(SYNC_FAILED_USERS_KEY, Long.toString(userId));
            }

        } catch (BusinessException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception redisException) {
            // Redis失败，降级到只写数据库
            if (systemProperties.getLearningProgress().isEnableDatabaseFallback()) {
                log.error("Redis update failed for user {} node {}, fallback to database only: {}",
                    userId, nodeId, redisException.getMessage());
                fallbackToDatabase(userId, nodeId);
            } else {
                log.error("Redis update failed and database fallback disabled", redisException);
                throw StatusCode.LEARNING_PROGRESS_REDIS_FAILED.exception();
            }
        }
    }

    /**
     * 取消标记节点为已完成
     * 如果节点未完成，抛出异常
     *
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @param courseId 课程ID
     * @throws BusinessException 如果节点已是未完成状态
     */
    public void unmarkNodeCompleted(long userId, long nodeId, long courseId) {
        ValidationUtils.requirePositiveId(userId);
        ValidationUtils.requirePositiveId(nodeId);
        ValidationUtils.requirePositiveId(courseId);

        try {
            String key = USER_COMPLETED_KEY_PREFIX + userId;

            // 如果Redis中没有这个用户的数据，先加载
            if (!redisTemplate.hasKey(key)) {
                loadUserDataToRedis(userId);
            }

            // 1. 立即更新Redis
            Long removed = redisTemplate.opsForSet().remove(key, Long.toString(nodeId));

            if (removed == null || removed == 0) {
                // 节点不在完成集合中，抛出异常
                throw StatusCode.NODE_ALREADY_NOT_COMPLETED.exception();
            }

            log.info("Successfully unmarked node {} as completed for user {} in Redis", nodeId, userId);

            // 2. 尝试同步更新数据库
            try {
                syncUserToDatabase(userId);
            } catch (Exception dbException) {
                // 数据库更新失败，记录到待补偿队列
                log.warn("Database sync failed for user {}, added to retry queue: {}",
                    userId, dbException.getMessage());
                redisTemplate.opsForSet().add(SYNC_FAILED_USERS_KEY, Long.toString(userId));
            }

        } catch (BusinessException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception redisException) {
            // Redis失败，降级到只操作数据库
            log.error("Redis update failed for user {} node {}, fallback to database only: {}",
                userId, nodeId, redisException.getMessage());
            fallbackUnmarkToDatabase(userId, nodeId);
        }
    }

    /**
     * 标记课程为已完成
     *
     * @param userId 用户ID
     * @param courseId 课程ID
     * @return 是否成功标记
     */
    public boolean markCourseCompleted(long userId, long courseId) {
        ValidationUtils.requirePositiveId(userId);
        ValidationUtils.requirePositiveId(courseId);

        try {
            log.info("Marking course {} as completed for user {}", courseId, userId);

            // 查找用户课程记录
            UserCourseDO userCourse = userCourseDataService.getByUserIdAndCourseId(userId, courseId);

            if (userCourse == null) {
                // 如果没有记录，创建新的用户课程记录
                userCourse = new UserCourseDO();
                userCourse.setUserId(userId);
                userCourse.setCourseId(courseId);
                userCourse.setProgressPercent(100);
                userCourse.setState(Enums.UserProgressState.COMPLETED.value());
                userCourse.setStartedAt(LocalDateTime.now());
                userCourse.setCompletedAt(LocalDateTime.now());

                userCourseDataService.insert(userCourse);
                log.info("Successfully created and marked course {} as completed for user {}", courseId, userId);

                // 发布学习完成事件
                eventPublisher.publishEvent(new LearningCompletedEvent(
                    userId,
                    courseId,
                    Enums.ContentType.course
                ));

                return true;
            } else {
                // 如果已有记录，更新完成状态
                boolean wasAlreadyCompleted = userCourse.getState() != null &&
                    userCourse.getState() == Enums.UserProgressState.COMPLETED.value();

                userCourse.setProgressPercent(100);
                userCourse.setState(Enums.UserProgressState.COMPLETED.value());
                userCourse.setCompletedAt(LocalDateTime.now());

                int updated = userCourseDataService.update(userCourse);
                if (updated > 0) {
                    log.info("Successfully updated course {} as completed for user {}", courseId, userId);

                    // 只在首次完成时发布事件
                    if (!wasAlreadyCompleted) {
                        eventPublisher.publishEvent(new LearningCompletedEvent(
                            userId,
                            courseId,
                            Enums.ContentType.course
                        ));
                    }

                    return true;
                } else {
                    log.error("Failed to update user course record for user {} course {}", userId, courseId);
                    return false;
                }
            }

        } catch (Exception e) {
            log.error("Error marking course {} as completed for user {}: {}", courseId, userId, e.getMessage(), e);
            return false;
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

        try {
            String key = USER_COMPLETED_KEY_PREFIX + userId;

            // 检查Redis是否存在这个key
            if (!redisTemplate.hasKey(key)) {
                loadUserDataToRedis(userId);
            }

            return redisTemplate.opsForSet().isMember(key, nodeId.toString());

        } catch (Exception e) {
            log.error("Error checking if node {} is completed for user {}: {}", nodeId, userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取用户完成的所有节点
     *
     * @param userId 用户ID
     * @return 已完成的节点ID集合
     */
    public Set<Long> getUserCompletedNodes(long userId) {
        ValidationUtils.requirePositiveId(userId);

        try {
            String key = USER_COMPLETED_KEY_PREFIX + userId;

            if (!redisTemplate.hasKey(key)) {
                loadUserDataToRedis(userId);
            }

            Set<String> nodeStrings = redisTemplate.opsForSet().members(key);
            if (nodeStrings == null) {
                return Set.of();
            }

            return nodeStrings.stream()
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        } catch (Exception e) {
            log.error("Error getting completed nodes for user {}: {}", userId, e.getMessage(), e);
            return Set.of();
        }
    }

    /**
     * 获取用户完成的节点总数
     *
     * @param userId 用户ID
     * @return 完成的节点总数
     */
    public long getUserCompletedCount(long userId) {
        ValidationUtils.requirePositiveId(userId);

        try {
            String key = USER_COMPLETED_KEY_PREFIX + userId;

            if (!redisTemplate.hasKey(key)) {
                loadUserDataToRedis(userId);
            }

            Long count = redisTemplate.opsForSet().size(key);
            return count != null ? count : 0;

        } catch (Exception e) {
            log.error("Error getting completed count for user {}: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 获取课程进度百分比
     */
    public Integer getCourseProgress(long userId, Long courseId) {
        ValidationUtils.requirePositiveId(userId);
        ValidationUtils.requirePositiveId(courseId);

        UserCourseDO userCourse = userCourseDataService.getByUserIdAndCourseId(userId, courseId);
        return userCourse != null ? userCourse.getProgressPercent() : 0;
    }

    // ========== 运维方法 ==========

    /**
     * 定期补偿机制：重试之前失败的数据库同步
     * 每分钟执行一次，处理失败队列中的用户
     */
    @Scheduled(fixedRate = 60000)
    public void retryFailedSync() {
        try {
            Set<String> failedUsers = redisTemplate.opsForSet().members(SYNC_FAILED_USERS_KEY);

            if (failedUsers == null || failedUsers.isEmpty()) {
                return;
            }

            log.info("Retrying sync for {} failed users", failedUsers.size());

            for (String userIdStr : failedUsers) {
                try {
                    Long userId = Long.parseLong(userIdStr);
                    syncUserToDatabase(userId);

                    // 同步成功，从失败队列移除
                    redisTemplate.opsForSet().remove(SYNC_FAILED_USERS_KEY, userIdStr);
                    log.info("Successfully retried sync for user {}", userId);

                } catch (Exception e) {
                    log.warn("Retry sync failed for user {}, will try again later: {}",
                        userIdStr, e.getMessage());
                    // 继续保留在失败队列中，下次继续重试
                }
            }

        } catch (Exception e) {
            log.error("Error during retry failed sync: {}", e.getMessage(), e);
        }
    }

    /**
     * 手动触发数据同步（用于运维）
     *
     * @param userId 用户ID
     * @return 是否同步成功
     */
    public boolean manualSync(Long userId) {
        ValidationUtils.requirePositiveId(userId);

        try {
            syncUserToDatabase(userId);
            // 同步成功后从失败队列移除（如果存在）
            redisTemplate.opsForSet().remove(SYNC_FAILED_USERS_KEY, userId.toString());
            log.info("Manual sync completed for user {}", userId);
            return true;
        } catch (Exception e) {
            log.error("Manual sync failed for user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取当前失败队列中的用户数量（用于监控）
     *
     * @return 失败队列大小
     */
    public long getFailedSyncQueueSize() {
        try {
            Long size = redisTemplate.opsForSet().size(SYNC_FAILED_USERS_KEY);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("Error getting failed sync queue size: {}", e.getMessage(), e);
            return -1;
        }
    }

    /**
     * 应用启动时的数据一致性检查
     */
    public void initializeService() {
        log.info("LearningProgressDomainService initialized");

        // 可以在这里添加启动时的数据检查逻辑
        // 例如：检查是否有未完成的同步任务等
    }
}