package com.prosper.learn.business.service.application;

import com.prosper.learn.common.Enums;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.common.config.SystemProperties;
import com.prosper.learn.business.service.domain.ContentsService;
import com.prosper.learn.business.util.converter.NodeConverter;
import com.prosper.learn.dto.response.node.NodeWithProgressDTO;
import com.prosper.learn.persistence.dataobject.UserProgressDO;
import com.prosper.learn.persistence.dataobject.UserCourseDO;
import com.prosper.learn.business.service.data.UserProgressDataService;
import com.prosper.learn.business.service.data.UserCourseDataService;
import com.prosper.learn.business.service.data.NodeDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.prosper.learn.dto.response.NodeProgressResponseDTO;
import com.prosper.learn.dto.response.CourseCompletionResponseDTO;
import com.prosper.learn.persistence.dataobject.NodeDO;

/**
 * 学习进度服务
 * 使用Redis Set存储用户完成的节点，MySQL作为持久化备份
 * Redis优先策略：立即更新Redis，异步同步数据库，定期补偿失败的同步
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningProgressService {

    private final RedisTemplate<String, String> redisTemplate;
    private final UserProgressDataService userProgressDataService;
    private final UserCourseDataService userCourseDataService;
    private final NodeDataService nodeDataService;
    private final ContentsService contentsService;
    private final ObjectMapper objectMapper;
    private final SystemProperties systemProperties;
    private final NodeConverter nodeConverter;

    private static final String USER_COMPLETED_KEY_PREFIX = "user:completed:";
    private static final String SYNC_FAILED_USERS_KEY = "sync:failed:users";
    
    // 不再使用硬编码的Duration，后面会使用配置
    
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
     * 验证节点ID是否有效
     */
    private void validateNodeId(long nodeId) {
        if (nodeId <= 0) {
            throw ErrorCode.LEARNING_PROGRESS_INVALID_NODE_ID.exception();
        }
    }

    /**
     * 验证用户ID是否有效
     */
    private void validateUserId(long userId) {
        if (userId <= 0) {
            throw ErrorCode.USER_NOT_FOUND.exception();
        }
    }

    /**
     * 验证课程ID是否有效
     */
    private void validateCourseId(long courseId) {
        if (courseId <= 0) {
            throw ErrorCode.COURSE_NOT_FOUND.exception();
        }
    }

    /**
     * 验证进度百分比是否有效
     */
    private void validateProgressPercent(int progressPercent) {
        int threshold = systemProperties.getLearningProgress().getCompletionThreshold();
        if (progressPercent < 0 || progressPercent > threshold) {
            throw ErrorCode.USER_COURSE_PROGRESS_INVALID.exception();
        }
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
     * 验证数据库操作结果
     */
    private void validateDatabaseOperation(int rowsAffected, String operation) {
        if (rowsAffected == 0) {
            throw ErrorCode.LEARNING_PROGRESS_DATABASE_FAILED.exception();
        }
    }

    // ========== 公共业务方法 ==========

    /**
     * 标记节点为已完成
     * 主流程：Redis + 数据库都更新，数据库失败不影响用户体验，记录到补偿队列
     * 
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @return 是否成功添加（如果已存在则返回false）
     */
    public boolean markNodeCompleted(long userId, long nodeId, long courseId) {
        validateUserId(userId);
        validateNodeId(nodeId);
        validateCourseId(courseId);
        
        try {
            String key = generateUserCompletedKey(userId);
            
            ensureUserDataInRedis(userId);
            
            // 1. 立即更新Redis（用户立即看到结果）
            Long added = redisTemplate.opsForSet().add(key, Long.toString(nodeId));
            redisTemplate.expire(key, getCacheExpireTime());
            
            boolean isNewlyAdded = added > 0;
            
            if (isNewlyAdded) {
                log.info("Successfully marked node {} as completed for user {} in Redis", nodeId, userId);
                
                // 2. 尝试同步更新数据库
                try {
                    syncUserToDatabase(userId);
                    
                    // 3. 更新课程进度
                    if (systemProperties.getLearningProgress().isEnableHierarchicalProgress()) {
                        updateCourseProgress(userId, nodeId, courseId);
                    }
                } catch (Exception dbException) {
                    // 数据库更新失败，记录到待补偿队列
                    log.warn("Database sync failed for user {}, added to retry queue: {}", 
                        userId, dbException.getMessage());
                    redisTemplate.opsForSet().add(SYNC_FAILED_USERS_KEY, Long.toString(userId));
                }
            } else {
                log.debug("Node {} already completed by user {}", nodeId, userId);
            }
            
            return isNewlyAdded;
            
        } catch (Exception redisException) {
            // Redis失败，降级到只写数据库
            if (systemProperties.getLearningProgress().isEnableDatabaseFallback()) {
                log.error("Redis update failed for user {} node {}, fallback to database only: {}", 
                    userId, nodeId, redisException.getMessage());
                return fallbackToDatabase(userId, nodeId);
            } else {
                throw ErrorCode.LEARNING_PROGRESS_REDIS_FAILED.exception(redisException);
            }
        }
    }

    /**
     * 取消标记节点为已完成
     * 
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @return 是否成功移除（如果不存在则返回false）
     */
    public boolean unmarkNodeCompleted(long userId, long nodeId, long courseId) {
        try {
            String key = USER_COMPLETED_KEY_PREFIX + userId;
            
            // 如果Redis中没有这个用户的数据，先加载
            if (!redisTemplate.hasKey(key)) {
                loadUserDataToRedis(userId);
            }
            
            // 1. 立即更新Redis
            Long removed = redisTemplate.opsForSet().remove(key, Long.toString(nodeId));
            
            boolean wasRemoved = removed > 0;
            
            if (wasRemoved) {
                log.info("Successfully unmarked node {} as completed for user {} in Redis", nodeId, userId);
                
                // 2. 尝试同步更新数据库
                try {
                    syncUserToDatabase(userId);
                    
                    // 3. 更新课程进度
                    updateCourseProgress(userId, nodeId, courseId);
                } catch (Exception dbException) {
                    // 数据库更新失败，记录到待补偿队列
                    log.warn("Database sync failed for user {}, added to retry queue: {}", 
                        userId, dbException.getMessage());
                    redisTemplate.opsForSet().add(SYNC_FAILED_USERS_KEY, Long.toString(userId));
                }
            } else {
                log.debug("Node {} was not completed by user {}", nodeId, userId);
            }
            
            return wasRemoved;
            
        } catch (Exception redisException) {
            // Redis失败，降级到只操作数据库
            log.error("Redis update failed for user {} node {}, fallback to database only: {}", 
                userId, nodeId, redisException.getMessage());
            return fallbackUnmarkToDatabase(userId, nodeId);
        }
    }

    /**
     * Redis失败时的降级处理：直接操作数据库取消完成
     * 
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @return 是否成功移除
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
     * 检查用户是否完成了指定节点
     * 
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @return 是否已完成
     */
    public boolean isNodeCompleted(Long userId, Long nodeId) {
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
     * 从数据库加载用户数据到Redis（私有方法，懒加载）
     * 
     * @param userId 用户ID
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
     * 
     * @param userId 用户ID
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
     * 
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @return 是否成功添加
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
     * 应用启动时的数据一致性检查
     * 可以在这里添加启动时的数据修复逻辑
     */
    public void initializeService() {
        log.info("LearningProgressService initialized");
        
        // 可以在这里添加启动时的数据检查逻辑
        // 例如：检查是否有未完成的同步任务等
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

    /**
     * 手动触发数据同步（用于运维）
     * 
     * @param userId 用户ID
     * @return 是否同步成功
     */
    public boolean manualSync(Long userId) {
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
     * 标记课程为已完成
     * 
     * @param userId 用户ID
     * @param courseId 课程ID
     * @return 是否成功标记
     */
    public boolean markCourseCompleted(long userId, long courseId) {
        try {
            log.info("Marking course {} as completed for user {}", courseId, userId);
            
            // 查找用户课程记录
            UserCourseDO userCourse = userCourseDataService.getByUserIdAndCourseId((long)userId, (long)courseId);
            
            if (userCourse == null) {
                // 如果没有记录，创建新的用户课程记录
                userCourse = new UserCourseDO();
                userCourse.setUserId((long)userId);
                userCourse.setCourseId((long)courseId);
                userCourse.setProgressPercent(100);
                userCourse.setState(Enums.UserProgressState.COMPLETED.value());
                userCourse.setStartedAt(java.time.LocalDateTime.now());
                userCourse.setCompletedAt(java.time.LocalDateTime.now());
                
                userCourseDataService.insert(userCourse);
                log.info("Successfully created and marked course {} as completed for user {}", courseId, userId);
                return true;
            } else {
                // 如果已有记录，更新完成状态
                userCourse.setProgressPercent(100);
                userCourse.setState(Enums.UserProgressState.COMPLETED.value());
                userCourse.setCompletedAt(java.time.LocalDateTime.now());
                
                int updated = userCourseDataService.update(userCourse);
                if (updated > 0) {
                    log.info("Successfully updated course {} as completed for user {}", courseId, userId);
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
     * 更新课程进度
     * 基于用户目录1中的节点层级结构计算进度
     * 
     * @param userId 用户ID
     * @param nodeId 节点ID（用于日志）
     * @param courseId 课程ID
     */
    private void updateCourseProgress(long userId, long nodeId, long courseId) {
        try {
            // 1. 获取用户目录1的内容
            String toc1Content = contentsService.getToc(userId, courseId, 1);
            if (toc1Content == null) return;
            
            // 2. 解析目录结构
            JsonNode tocNode = objectMapper.readTree(toc1Content);
            
            // 3. 获取用户已完成的节点
            Set<Long> userCompletedNodes = getUserCompletedNodes(userId);
            
            // 4. 递归计算层级进度
            double progressPercent = calculateHierarchicalProgress(tocNode, userCompletedNodes) * 10000;
            int finalProgress = (int) Math.floor(progressPercent);
            
            // 5. 更新或创建用户课程记录
            UserCourseDO userCourse = userCourseDataService.getByUserIdAndCourseId((long)userId, (long)courseId);
            
            if (userCourse == null) {
                userCourse = new UserCourseDO();
                userCourse.setUserId((long)userId);
                userCourse.setCourseId((long)courseId);
                userCourse.setProgressPercent(finalProgress);
                userCourse.setState(finalProgress >= 10000 ? Enums.UserProgressState.COMPLETED.value() : Enums.UserProgressState.IN_PROGRESS.value());
                userCourse.setStartedAt(LocalDateTime.now());
                if (finalProgress >= 10000) {
                    userCourse.setCompletedAt(LocalDateTime.now());
                }
                userCourseDataService.insert(userCourse);
            } else {
                userCourse.setProgressPercent(finalProgress);
                userCourse.setState(finalProgress >= 10000 ? Enums.UserProgressState.COMPLETED.value() : Enums.UserProgressState.IN_PROGRESS.value());
                if (finalProgress >= 10000 && userCourse.getCompletedAt() == null) {
                    userCourse.setCompletedAt(LocalDateTime.now());
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

    /**
     * 标记节点完成并返回完整的响应数据
     */
    public NodeProgressResponseDTO markNodeCompletedWithResponse(long userId, long nodeId, long courseId) {
        boolean isNewlyCompleted = markNodeCompleted(userId, nodeId, courseId);
        
        UserCourseDO userCourse = userCourseDataService.getByUserIdAndCourseId(userId, courseId);
        Integer courseProgress = userCourse != null ? userCourse.getProgressPercent() : 0;
        
        long totalCompleted = getUserCompletedCount(userId);
        
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
        boolean wasRemoved = unmarkNodeCompleted(userId, nodeId, courseId);
        
        UserCourseDO userCourse = userCourseDataService.getByUserIdAndCourseId(userId, courseId);
        Integer courseProgress = userCourse != null ? userCourse.getProgressPercent() : 0;
        
        long totalCompleted = getUserCompletedCount(userId);
        
        return NodeProgressResponseDTO.builder()
                .nodeId(nodeId)
                .completed(false)
                .wasRemoved(wasRemoved)
                .courseProgress(courseProgress)
                .totalCompletedNodes(totalCompleted)
                .build();
    }

    /**
     * 获取节点完成状态响应数据
     */
    public NodeWithProgressDTO getNodeCompletionStatusResponse(long userId, long nodeId) {
        boolean isCompleted = isNodeCompleted(userId, nodeId);

        NodeDO nodeDO = nodeDataService.getById(nodeId);
        if (nodeDO == null) {
            throw ErrorCode.LEARNING_PROGRESS_INVALID_NODE_ID.exception();
        }

        return nodeConverter.toWithProgressDTO(nodeDO, isCompleted);
    }

    /**
     * 标记课程完成并返回完整的响应数据
     */
    public CourseCompletionResponseDTO markCourseCompletedWithResponse(long userId, long courseId) {
        boolean result = markCourseCompleted(userId, courseId);
        
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
}