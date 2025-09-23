package com.prosper.learn.domain.service.autoauthor;

import com.prosper.learn.domain.config.SystemProperties;
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
        redisTemplate.opsForZSet().add(key, Long.toString(nodeId), score);
    }

    /**
     * 取队首但不删除；用于执行器轮询
     * @return 最早入队的 nodeId；队列空返回 null
     */
    public Long peek() {
        String key = readyKey();
        Set<Object> set = redisTemplate.opsForZSet().range(key, 0, 0);
        if (set == null || set.isEmpty()) return null;
        Object first = set.iterator().next();
        if (first instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /**
     * 从集合中移除指定节点
     * @param nodeId 节点ID
     */
    public void remove(long nodeId) {
        String key = readyKey();
        redisTemplate.opsForZSet().remove(key, Long.toString(nodeId));
    }
}
