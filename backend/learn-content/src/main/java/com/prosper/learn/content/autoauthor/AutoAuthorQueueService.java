package com.prosper.learn.content.autoauthor;

import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

/**
 * AutoAuthor 就绪队列服务（基于 Redis ZSET）
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
public class AutoAuthorQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SystemProperties systemProperties;

    /** 计算 ready 集合的完整 key */
    private String readyKey() {
        return systemProperties.getAutoAuthor().getRedisKeyPrefix() + "ready";
    }

    /**
     * 入队：将节点加入就绪集合
     * @param nodeId 节点ID
     */
    public void enqueue(long nodeId) {
        String key = readyKey();
        double score = (double) Instant.now().toEpochMilli();
        // 节点内容生成任务格式：N:{nodeId}
        redisTemplate.opsForZSet().add(key, "N:" + nodeId, score);
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
     * 从集合中移除指定节点任务（兼容旧接口）
     * @param nodeId 节点ID
     */
    public void remove(long nodeId) {
        remove("N:" + nodeId);
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
}
