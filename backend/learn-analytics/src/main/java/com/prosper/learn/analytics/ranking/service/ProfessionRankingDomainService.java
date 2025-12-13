package com.prosper.learn.analytics.ranking.service;

import com.prosper.learn.shared.domain.exception.ErrorCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
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
public class ProfessionRankingDomainService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SystemProperties systemProperties;
    
    private static final String HOT_PROFESSIONS_KEY = "profession:hot:ranking";
    private static final String PROFESSION_LEARNING_PREFIX = "profession:learning:";

// --注释掉检查 START (2025/12/10 11:19):
//    /**
//     * 增加职业学习数
//     */
//    public void incrementLearning(long professionId) {
//        validateProfessionId(professionId);
//        String key = generateLearningKey(professionId);
//        redisTemplate.opsForValue().increment(key);
//        updateProfessionRanking(professionId);
//    }
// --注释掉检查 STOP (2025/12/10 11:19)

// --注释掉检查 START (2025/12/10 11:19):
//    /**
//     * 减少职业学习数
//     */
//    public void decrementLearning(long professionId) {
//        validateProfessionId(professionId);
//        String key = generateLearningKey(professionId);
//        Long count = redisTemplate.opsForValue().decrement(key);
//        if (count != null && count < 0) {
//            redisTemplate.opsForValue().set(key, "0");
//        }
//        updateProfessionRanking(professionId);
//    }
// --注释掉检查 STOP (2025/12/10 11:19)

// --注释掉检查 START (2025/12/10 11:35):
//    /**
//     * 更新职业排行榜
//     */
//    private void updateProfessionRanking(long professionId) {
//        try {
//            String learningKey = generateLearningKey(professionId);
//            String learningCountStr = redisTemplate.opsForValue().get(learningKey);
//            long learningCount = learningCountStr != null ? Long.parseLong(learningCountStr) : 0;
//
//            // 更新排行榜（这里只按学习人数排序）
//            redisTemplate.opsForZSet().add(HOT_PROFESSIONS_KEY, String.valueOf(professionId), learningCount);
//
//            log.debug("Updated profession ranking: professionId={}, learningCount={}",
//                     professionId, learningCount);
//        } catch (Exception e) {
//            log.error("Failed to update profession ranking for professionId: {}", professionId, e);
//        }
//    }
// --注释掉检查 STOP (2025/12/10 11:35)

    /**
     * 获取热门职业ID列表（按学习人数降序）
     */
    public List<Long> getHotProfessionIds(int limit) {
        validateLimit(limit);
        try {
            Set<String> professionIds = redisTemplate.opsForZSet().reverseRange(HOT_PROFESSIONS_KEY, 0, limit - 1);
            if (professionIds == null || professionIds.isEmpty()) {
                return List.of();
            }
            
            return professionIds.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get hot profession ids with limit: {}", limit, e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 获取职业的学习人数
     */
    public long getProfessionLearningCount(long professionId) {
        validateProfessionId(professionId);
        try {
            String learningKey = generateLearningKey(professionId);
            String learningCountStr = redisTemplate.opsForValue().get(learningKey);
            return learningCountStr != null ? Long.parseLong(learningCountStr) : 0;
        } catch (Exception e) {
            log.error("Failed to get profession learning count for professionId: {}", professionId, e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 批量初始化职业统计数据（用于定时任务）
     */
    public void initializeProfessionStats(long professionId, long learningCount) {
        validateProfessionId(professionId);
        try {
            String learningKey = generateLearningKey(professionId);
            redisTemplate.opsForValue().set(learningKey, String.valueOf(learningCount));
            redisTemplate.opsForZSet().add(HOT_PROFESSIONS_KEY, String.valueOf(professionId), learningCount);
            
            log.debug("Initialized profession stats: professionId={}, learningCount={}", 
                     professionId, learningCount);
        } catch (Exception e) {
            log.error("Failed to initialize profession stats for professionId: {}", professionId, e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
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
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 生成职业学习数Redis键名
     */
    private String generateLearningKey(long professionId) {
        return PROFESSION_LEARNING_PREFIX + professionId;
    }

    /**
     * 验证职业ID有效性
     */
    private void validateProfessionId(long professionId) {
        if (professionId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }

    /**
     * 验证限制数量有效性
     */
    private void validateLimit(int limit) {
        if (limit <= 0 || limit > systemProperties.getCourseRanking().getMaxHotCoursesLimit()) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
}