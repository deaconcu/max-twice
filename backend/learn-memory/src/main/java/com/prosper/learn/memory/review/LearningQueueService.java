package com.prosper.learn.memory.review;

import com.prosper.learn.infrastructure.redis.RedisKeyPrefix;
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
 * Key 设计：{lang}:learning_queue:{userId}
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
     * 获取 Redis key（带语言前缀）
     */
    private String getKey(Long userId) {
        return RedisKeyPrefix.prefix(KEY_PREFIX + userId);
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
            log.debug("学习队列保存成功: userId={}，cardCount={}", userId, queue.size());
        } catch (Exception e) {
            log.warn("学习队列保存失败: userId={}，error={}", userId, e.getMessage());
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
            log.warn("学习队列获取失败: userId={}，error={}", userId, e.getMessage());
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
            log.debug("学习队列清除成功: userId={}", userId);
        } catch (Exception e) {
            log.warn("学习队列清除失败: userId={}，error={}", userId, e.getMessage());
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
            log.debug("学习队列过滤: userId={}，{} -> {} 张卡片",
                     userId, queue.size(), filteredQueue.size());
        }

        return filteredQueue;
    }

    // ========== 队列操作 ==========

    /**
     * LEARNING 阶段的插入间隔配置
     */
    private static final int OFFSET_AGAIN = 3;  // 忘记：插入到 3 张后
    private static final int OFFSET_HARD = 5;   // 困难：插入到 5 张后
    private static final int OFFSET_GOOD = 8;   // 良好：插入到 8 张后
    public static final int LOAD_THRESHOLD = 9; // 低于此值需要加载更多

    /**
     * 获取队列中的第一张卡片ID
     *
     * @param userId 用户ID
     * @return 第一张卡片ID，队列为空返回 null
     */
    public Long getFirstCardId(Long userId) {
        List<Long> queue = getQueue(userId);
        return queue.isEmpty() ? null : queue.get(0);
    }

    /**
     * 从队列中移除卡片（毕业时调用）
     *
     * @param userId 用户ID
     * @param cardId 卡片ID
     */
    public void removeFromQueue(Long userId, Long cardId) {
        List<Long> queue = getQueue(userId);
        if (queue.isEmpty()) return;

        List<Long> newQueue = new ArrayList<>(queue);
        newQueue.remove(cardId);

        if (newQueue.isEmpty()) {
            clearQueue(userId);
        } else {
            saveQueue(userId, newQueue);
        }
    }

    /**
     * 重排队列：将卡片从队首移到指定位置
     *
     * @param userId 用户ID
     * @param cardId 卡片ID
     * @param rating 评分（1-4）
     */
    public void reorderQueue(Long userId, Long cardId, int rating) {
        List<Long> queue = getQueue(userId);
        if (queue.isEmpty()) return;

        List<Long> newQueue = new ArrayList<>(queue);

        // 从队列中移除当前卡片
        newQueue.remove(cardId);

        if (newQueue.isEmpty()) {
            // 只剩这一张卡，放回队首
            newQueue.add(cardId);
        } else {
            // 计算插入位置
            int offset = switch (rating) {
                case 1 -> OFFSET_AGAIN;
                case 2 -> OFFSET_HARD;
                case 3 -> OFFSET_GOOD;
                default -> OFFSET_AGAIN;
            };

            // 插入到指定位置（不超过队列长度）
            int insertPos = Math.min(offset - 1, newQueue.size());
            newQueue.add(insertPos, cardId);
        }

        saveQueue(userId, newQueue);
        log.debug("学习队列重排: userId={}，cardId={}，rating={}", userId, cardId, rating);
    }

    /**
     * 检查是否需要加载更多卡片
     *
     * @param userId 用户ID
     * @return true 如果队列长度小于阈值
     */
    public boolean needLoadMore(Long userId) {
        return getQueueSize(userId) < LOAD_THRESHOLD;
    }

    /**
     * 加载更多卡片到队列末尾
     *
     * @param userId 用户ID
     * @param cardIds 新加载的卡片ID列表
     */
    public void loadMore(Long userId, List<Long> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) return;

        List<Long> queue = getQueue(userId);
        List<Long> newQueue = new ArrayList<>(queue);

        // 只添加不重复的卡片
        for (Long cardId : cardIds) {
            if (!newQueue.contains(cardId)) {
                newQueue.add(cardId);
            }
        }

        saveQueue(userId, newQueue);
        log.debug("学习队列加载更多: userId={}，size={}", userId, newQueue.size());
    }

    /**
     * 获取队列大小
     *
     * @param userId 用户ID
     * @return 队列大小
     */
    public int getQueueSize(Long userId) {
        return getQueue(userId).size();
    }

    /**
     * 初始化队列
     *
     * @param userId 用户ID
     * @param cardIds 卡片ID列表
     */
    public void initQueue(Long userId, List<Long> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) {
            clearQueue(userId);
            return;
        }
        saveQueue(userId, new ArrayList<>(cardIds));
        log.debug("学习队列初始化: userId={}，cardCount={}", userId, cardIds.size());
    }
}
