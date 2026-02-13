package com.prosper.learn.memory.review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 学习队列服务
 *
 * 管理 LEARNING/RELEARNING 阶段的卡片队列顺序
 * 队列存储在 Redis 中，支持刷新后恢复顺序
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserCardSrsDataService srsDataService;

    private static final String KEY_PREFIX = "learning_queue:";
    private static final long EXPIRE_HOURS = 24;

    /**
     * 获取 Redis key
     */
    private String getKey(Long userId) {
        return KEY_PREFIX + userId;
    }

    /**
     * 保存学习队列
     *
     * @param userId 用户ID
     * @param queue 队列（卡片ID列表）
     */
    public void saveQueue(Long userId, List<Long> queue) {
        if (userId == null || queue == null || queue.isEmpty()) {
            return;
        }

        String key = getKey(userId);
        try {
            redisTemplate.opsForValue().set(key, queue, EXPIRE_HOURS, TimeUnit.HOURS);
            log.debug("Saved learning queue for user {}: {} cards", userId, queue.size());
        } catch (Exception e) {
            log.warn("Failed to save learning queue for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * 获取学习队列
     *
     * @param userId 用户ID
     * @return 队列（卡片ID列表），不存在则返回空列表
     */
    @SuppressWarnings("unchecked")
    public List<Long> getQueue(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        String key = getKey(userId);
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof List) {
                return ((List<?>) value).stream()
                        .map(item -> {
                            if (item instanceof Long) {
                                return (Long) item;
                            } else if (item instanceof Integer) {
                                return ((Integer) item).longValue();
                            } else if (item instanceof Number) {
                                return ((Number) item).longValue();
                            }
                            return null;
                        })
                        .filter(id -> id != null)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Failed to get learning queue for user {}: {}", userId, e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * 清除学习队列
     *
     * @param userId 用户ID
     */
    public void clearQueue(Long userId) {
        if (userId == null) {
            return;
        }

        String key = getKey(userId);
        try {
            redisTemplate.delete(key);
            log.debug("Cleared learning queue for user {}", userId);
        } catch (Exception e) {
            log.warn("Failed to clear learning queue for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * 校验并过滤队列中的卡片ID
     * 只保留属于该用户的 LEARNING/RELEARNING 状态的卡片
     *
     * @param userId 用户ID
     * @param queue 前端传来的队列
     * @return 过滤后的合法队列
     */
    public List<Long> validateAndFilterQueue(Long userId, List<Long> queue) {
        if (userId == null || queue == null || queue.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询这些卡片的 SRS 状态
        List<UserCardSrsDO> srsStates = srsDataService.getByUserAndCards(userId, queue);

        // 只保留 LEARNING(1) 和 RELEARNING(3) 状态的卡片
        Set<Long> validCardIds = srsStates.stream()
                .filter(srs -> srs.getType() == UserCardSrsDO.TYPE_LEARNING
                            || srs.getType() == UserCardSrsDO.TYPE_RELEARNING)
                .map(UserCardSrsDO::getCardId)
                .collect(Collectors.toSet());

        // 按原队列顺序过滤
        List<Long> filteredQueue = queue.stream()
                .filter(validCardIds::contains)
                .collect(Collectors.toList());

        if (filteredQueue.size() != queue.size()) {
            log.debug("Filtered learning queue for user {}: {} -> {} cards",
                     userId, queue.size(), filteredQueue.size());
        }

        return filteredQueue;
    }
}
