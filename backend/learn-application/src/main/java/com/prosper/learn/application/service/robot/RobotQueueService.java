package com.prosper.learn.application.service.robot;

import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.infrastructure.redis.RedisKeyPrefix;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Robot 就绪队列服务（基于 Redis ZSET）
 *
 * 职责：
 * - 入队：新节点创建后将 nodeId 放入有序集合，score 为当前时间戳（保证先入先出）
 * - 取队首：轮询时获取最早入队的一个 nodeId（不删除）
 * - 删除：处理成功后从集合中移除该 nodeId
 *
 * 说明：
 * - 使用现有 RedisTemplate，无需新客户端
 * - 不强制 NX 语义（ZADD NX）。重复入队将更新时间戳，不影响正确性
 */
@Service
@RequiredArgsConstructor
public class RobotQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SystemProperties systemProperties;
    private final CourseDataService courseDataService;

    private static final int MAX_CONSECUTIVE_FAILURES = 2;

    /** 计算 ready 集合的完整 key */
    private String readyKey() {
        return RedisKeyPrefix.prefix(systemProperties.getRobot().getRedisKeyPrefix() + "ready");
    }

    /** 今日完成数统计 key */
    private String todayCompletedKey() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return RedisKeyPrefix.prefix(systemProperties.getRobot().getRedisKeyPrefix() + "completed:" + date);
    }

    /** 最后执行时间 key */
    private String lastExecuteTimeKey() {
        return RedisKeyPrefix.prefix(systemProperties.getRobot().getRedisKeyPrefix() + "lastExecuteTime");
    }

    /** 暂停状态 key */
    private String pausedKey() {
        return RedisKeyPrefix.prefix(systemProperties.getRobot().getRedisKeyPrefix() + "paused");
    }

    /** 连续失败计数 key */
    private String consecutiveFailuresKey() {
        return RedisKeyPrefix.prefix(systemProperties.getRobot().getRedisKeyPrefix() + "consecutiveFailures");
    }

    /**
     * 入队：将节点加入就绪集合
     * @param nodeId 节点ID
     * @param contentType 内容类型 (auto/index/article)
     * @param recursive 是否递归生成子节点
     * @param deleteExisting 是否删除已存在的post
     */
    public void enqueue(long nodeId, String contentType, boolean recursive, boolean deleteExisting) {
        String key = readyKey();
        double score = (double) Instant.now().toEpochMilli();
        // 节点内容生成任务格式：N:{nodeId}:{contentType}:{recursive}:{deleteExisting}
        String taskId = String.format("N:%d:%s:%b:%b", nodeId, contentType, recursive, deleteExisting);
        redisTemplate.opsForZSet().add(key, taskId, score);
    }

    /**
     * 入队：通过courseId入队（转换为rootNodeId）
     * @param courseId 课程ID
     * @param contentType 内容类型 (auto/index/article)
     * @param recursive 是否递归生成子节点
     * @param deleteExisting 是否删除已存在的post
     */
    public void enqueueByCourseId(long courseId, String contentType, boolean recursive, boolean deleteExisting) {
        CourseDO course = courseDataService.validateAndGet(courseId);
        Long rootNodeId = course.getRootNodeId();
        if (rootNodeId == null) {
            throw new IllegalArgumentException("课程 " + courseId + " 没有根节点");
        }
        enqueue(rootNodeId, contentType, recursive, deleteExisting);
    }

    /**
     * 入队：将记忆卡片生成任务加入就绪集合
     * @param postId 帖子ID
     */
    public void enqueueMemoryCards(long postId) {
        String key = readyKey();
        double score = (double) Instant.now().toEpochMilli();
        // 记忆卡片生成任务格式：C:{postId}
        redisTemplate.opsForZSet().add(key, "C:" + postId, score);
    }

    /**
     * 取队首但不删除；用于执行器轮询
     * @return 最早入队的任务；队列空返回 null
     */
    public String peek() {
        String key = readyKey();
        Set<Object> set = redisTemplate.opsForZSet().range(key, 0, 0);
        if (set == null || set.isEmpty()) return null;
        Object first = set.iterator().next();
        return first instanceof String ? (String) first : null;
    }

    /**
     * 从集合中移除指定任务
     * @param taskId 任务ID（格式：N:nodeId 或 C:postId）
     */
    public void remove(String taskId) {
        String key = readyKey();
        redisTemplate.opsForZSet().remove(key, taskId);
    }

    /**
     * 清空所有队列
     * @return 清空的节点数量
     */
    public long clear() {
        String key = readyKey();
        Long size = redisTemplate.opsForZSet().zCard(key);
        if (size != null && size > 0) {
            redisTemplate.delete(key);
            return size;
        }
        return 0;
    }

    /**
     * 获取队列中待处理任务数量
     * @return 待处理任务数
     */
    public long getPendingCount() {
        String key = readyKey();
        Long size = redisTemplate.opsForZSet().zCard(key);
        return size != null ? size : 0;
    }

    /**
     * 记录任务完成
     */
    public void recordCompletion() {
        String key = todayCompletedKey();
        redisTemplate.opsForValue().increment(key);
        // 设置过期时间为第二天凌晨
        redisTemplate.expireAt(key,
            LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        // 更新最后执行时间
        String timeKey = lastExecuteTimeKey();
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(
            Instant.now().atZone(ZoneId.systemDefault())
        );
        redisTemplate.opsForValue().set(timeKey, now);
    }

    /**
     * 获取今日完成数
     */
    public long getTodayCompletedCount() {
        String key = todayCompletedKey();
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0;
    }

    /**
     * 获取最后执行时间
     */
    public String getLastExecuteTime() {
        String key = lastExecuteTimeKey();
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 暂停队列
     */
    public void pause() {
        redisTemplate.opsForValue().set(pausedKey(), "true");
    }

    /**
     * 恢复队列
     */
    public void resume() {
        redisTemplate.delete(pausedKey());
    }

    /**
     * 检查队列是否暂停
     */
    public boolean isPaused() {
        Object value = redisTemplate.opsForValue().get(pausedKey());
        return "true".equals(value);
    }

    /**
     * 获取队列状态
     * @return IDLE(空闲)/RUNNING(执行中)/PAUSED(已暂停)
     */
    public String getStatus() {
        if (isPaused()) {
            return "PAUSED";
        }
        long pending = getPendingCount();
        return pending > 0 ? "RUNNING" : "IDLE";
    }

    /**
     * 将任务移到队列尾部（重新入队）
     */
    public void moveToEnd(String taskId) {
        String key = readyKey();
        double score = (double) Instant.now().toEpochMilli();
        redisTemplate.opsForZSet().add(key, taskId, score);
    }

    /**
     * 记录连续失败
     * @return 连续失败次数
     */
    public int recordFailure() {
        String key = consecutiveFailuresKey();
        Long count = redisTemplate.opsForValue().increment(key);
        return count != null ? count.intValue() : 1;
    }

    /**
     * 重置连续失败计数
     */
    public void resetFailures() {
        redisTemplate.delete(consecutiveFailuresKey());
    }

    /**
     * 获取连续失败次数
     */
    public int getConsecutiveFailures() {
        String key = consecutiveFailuresKey();
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    /**
     * 检查是否应该自动暂停（连续失败过多）
     * @return true表示应该暂停
     */
    public boolean shouldAutoPause() {
        return getConsecutiveFailures() >= MAX_CONSECUTIVE_FAILURES;
    }
}
