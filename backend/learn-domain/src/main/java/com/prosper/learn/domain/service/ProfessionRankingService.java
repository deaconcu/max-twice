package com.prosper.learn.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfessionRankingService {

    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String HOT_PROFESSIONS_KEY = "profession:hot:ranking";
    private static final String PROFESSION_LEARNING_PREFIX = "profession:learning:";

    /**
     * 增加职业学习数
     */
    public void incrementLearning(int professionId) {
        String key = PROFESSION_LEARNING_PREFIX + professionId;
        redisTemplate.opsForValue().increment(key);
        updateProfessionRanking(professionId);
    }

    /**
     * 减少职业学习数
     */
    public void decrementLearning(int professionId) {
        String key = PROFESSION_LEARNING_PREFIX + professionId;
        Long count = redisTemplate.opsForValue().decrement(key);
        if (count != null && count < 0) {
            redisTemplate.opsForValue().set(key, "0");
        }
        updateProfessionRanking(professionId);
    }

    /**
     * 更新职业排行榜
     */
    private void updateProfessionRanking(int professionId) {
        try {
            String learningKey = PROFESSION_LEARNING_PREFIX + professionId;
            String learningCountStr = redisTemplate.opsForValue().get(learningKey);
            long learningCount = learningCountStr != null ? Long.parseLong(learningCountStr) : 0;
            
            // 更新排行榜（这里只按学习人数排序）
            redisTemplate.opsForZSet().add(HOT_PROFESSIONS_KEY, String.valueOf(professionId), learningCount);
            
            log.debug("Updated profession ranking: professionId={}, learningCount={}", 
                     professionId, learningCount);
        } catch (Exception e) {
            log.error("Failed to update profession ranking for professionId: {}", professionId, e);
        }
    }

    /**
     * 获取热门职业ID列表（按学习人数降序）
     */
    public List<Integer> getHotProfessionIds(int limit) {
        try {
            Set<String> professionIds = redisTemplate.opsForZSet().reverseRange(HOT_PROFESSIONS_KEY, 0, limit - 1);
            if (professionIds == null || professionIds.isEmpty()) {
                return List.of();
            }
            
            return professionIds.stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get hot profession ids", e);
            return List.of();
        }
    }

    /**
     * 获取职业的学习人数
     */
    public long getProfessionLearningCount(int professionId) {
        try {
            String learningKey = PROFESSION_LEARNING_PREFIX + professionId;
            String learningCountStr = redisTemplate.opsForValue().get(learningKey);
            return learningCountStr != null ? Long.parseLong(learningCountStr) : 0;
        } catch (Exception e) {
            log.error("Failed to get profession learning count for professionId: {}", professionId, e);
            return 0;
        }
    }

    /**
     * 批量初始化职业统计数据（用于定时任务）
     */
    public void initializeProfessionStats(int professionId, long learningCount) {
        try {
            String learningKey = PROFESSION_LEARNING_PREFIX + professionId;
            redisTemplate.opsForValue().set(learningKey, String.valueOf(learningCount));
            redisTemplate.opsForZSet().add(HOT_PROFESSIONS_KEY, String.valueOf(professionId), learningCount);
            
            log.debug("Initialized profession stats: professionId={}, learningCount={}", 
                     professionId, learningCount);
        } catch (Exception e) {
            log.error("Failed to initialize profession stats for professionId: {}", professionId, e);
        }
    }

    /**
     * 清空所有统计数据（用于重新同步）
     */
    public void clearAllStats() {
        try {
            // 清空热门职业排行榜
            redisTemplate.delete(HOT_PROFESSIONS_KEY);
            
            // 删除所有职业的统计数据
            Set<String> learningKeys = redisTemplate.keys(PROFESSION_LEARNING_PREFIX + "*");
            
            if (learningKeys != null && !learningKeys.isEmpty()) {
                redisTemplate.delete(learningKeys);
            }
            
            log.info("Cleared all profession stats from Redis");
        } catch (Exception e) {
            log.error("Failed to clear profession stats", e);
        }
    }
}