package com.prosper.learn.analytics.ranking.service;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.common.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 课程排行榜业务服务
 *
 * 负责课程相关的排行榜和热度统计功能，包括：
 * - 热门课程排行榜维护
 * - 课程收藏数统计
 * - 课程学习人数统计
 * - 基于Redis的实时统计缓存
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseRankingDomainService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SystemProperties systemProperties;
    
    private static final String HOT_COURSES_KEY = "course:hot:ranking";
    private static final String COURSE_SUBSCRIPTION_PREFIX = "course:subscription:";
    private static final String COURSE_LEARNING_PREFIX = "course:learning:";

    /**
     * 增加课程收藏数
     */
    public void incrementSubscription(long courseId) {
        validateCourseId(courseId);
        String key = COURSE_SUBSCRIPTION_PREFIX + courseId;
        redisTemplate.opsForValue().increment(key);
        updateCourseRanking(courseId);
    }

    /**
     * 减少课程收藏数
     */
    public void decrementSubscription(long courseId) {
        validateCourseId(courseId);
        String key = COURSE_SUBSCRIPTION_PREFIX + courseId;
        Long count = redisTemplate.opsForValue().decrement(key);
        if (count != null && count < 0) {
            redisTemplate.opsForValue().set(key, "0");
        }
        updateCourseRanking(courseId);
    }

    /**
     * 增加课程学习数
     */
    public void incrementLearning(long courseId) {
        validateCourseId(courseId);
        String key = COURSE_LEARNING_PREFIX + courseId;
        redisTemplate.opsForValue().increment(key);
        updateCourseRanking(courseId);
    }

    /**
     * 减少课程学习数
     */
    public void decrementLearning(long courseId) {
        validateCourseId(courseId);
        String key = COURSE_LEARNING_PREFIX + courseId;
        Long count = redisTemplate.opsForValue().decrement(key);
        if (count != null && count < 0) {
            redisTemplate.opsForValue().set(key, "0");
        }
        updateCourseRanking(courseId);
    }

    /**
     * 更新课程排行榜
     */
    private void updateCourseRanking(long courseId) {
        try {
            String subscriptionKey = COURSE_SUBSCRIPTION_PREFIX + courseId;
            String learningKey = COURSE_LEARNING_PREFIX + courseId;
            
            String subscriptionCountStr = redisTemplate.opsForValue().get(subscriptionKey);
            String learningCountStr = redisTemplate.opsForValue().get(learningKey);
            
            long subscriptionCount = subscriptionCountStr != null ? Long.parseLong(subscriptionCountStr) : 0;
            long learningCount = learningCountStr != null ? Long.parseLong(learningCountStr) : 0;
            
            long totalScore = subscriptionCount + learningCount;
            
            // 更新排行榜
            redisTemplate.opsForZSet().add(HOT_COURSES_KEY, String.valueOf(courseId), totalScore);
            
            log.debug("Updated course ranking: courseId={}, subscriptionCount={}, learningCount={}, totalScore={}", 
                     courseId, subscriptionCount, learningCount, totalScore);
        } catch (Exception e) {
            log.error("Failed to update course ranking for courseId: {}", courseId, e);
        }
    }

    /**
     * 获取热门课程ID列表（按分数降序）
     */
    public List<Long> getHotCourseIds(int limit) {
        if (limit <= 0) return Collections.emptyList();
        if (limit > 200) limit = 200;
        try {
            Set<String> courseIds = redisTemplate.opsForZSet().reverseRange(HOT_COURSES_KEY, 0, limit - 1);
            if (courseIds == null || courseIds.isEmpty()) {
                return List.of();
            }
            
            return courseIds.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get hot course ids with limit: {}", limit, e);
            throw ErrorCode.COURSE_RANKING_REDIS_OPERATION_FAILED.exception(e);
        }
    }

    /**
     * 获取课程的收藏数和学习数
     */
    public CourseStats getCourseStats(long courseId) {
        validateCourseId(courseId);
        try {
            String subscriptionKey = COURSE_SUBSCRIPTION_PREFIX + courseId;
            String learningKey = COURSE_LEARNING_PREFIX + courseId;
            
            String subscriptionCountStr = redisTemplate.opsForValue().get(subscriptionKey);
            String learningCountStr = redisTemplate.opsForValue().get(learningKey);
            
            long subscriptionCount = subscriptionCountStr != null ? Long.parseLong(subscriptionCountStr) : 0;
            long learningCount = learningCountStr != null ? Long.parseLong(learningCountStr) : 0;
            
            return new CourseStats(subscriptionCount, learningCount);
        } catch (Exception e) {
            log.error("Failed to get course stats for courseId: {}", courseId, e);
            return new CourseStats(0, 0);
        }
    }

    /**
     * 批量初始化课程统计数据（用于定时任务）
     */
    public void initializeCourseStats(long courseId, long subscriptionCount, long learningCount) {
        validateCourseId(courseId);
        try {
            String subscriptionKey = COURSE_SUBSCRIPTION_PREFIX + courseId;
            String learningKey = COURSE_LEARNING_PREFIX + courseId;
            
            redisTemplate.opsForValue().set(subscriptionKey, String.valueOf(subscriptionCount));
            redisTemplate.opsForValue().set(learningKey, String.valueOf(learningCount));
            
            long totalScore = subscriptionCount + learningCount;
            redisTemplate.opsForZSet().add(HOT_COURSES_KEY, String.valueOf(courseId), totalScore);
            
            log.debug("Initialized course stats: courseId={}, subscriptionCount={}, learningCount={}", 
                     courseId, subscriptionCount, learningCount);
        } catch (Exception e) {
            log.error("Failed to initialize course stats for courseId: {}", courseId, e);
        }
    }

    /**
     * 清空所有统计数据（用于重新同步）
     */
    public void clearAllStats() {
        try {
            // 清空热门课程排行榜
            redisTemplate.delete(HOT_COURSES_KEY);
            
            // 删除所有课程的统计数据
            Set<String> subscriptionKeys = redisTemplate.keys(COURSE_SUBSCRIPTION_PREFIX + "*");
            Set<String> learningKeys = redisTemplate.keys(COURSE_LEARNING_PREFIX + "*");
            
            if (subscriptionKeys != null && !subscriptionKeys.isEmpty()) {
                redisTemplate.delete(subscriptionKeys);
            }
            if (learningKeys != null && !learningKeys.isEmpty()) {
                redisTemplate.delete(learningKeys);
            }
            
            log.info("Cleared all course stats from Redis");
        } catch (Exception e) {
            log.error("Failed to clear course stats", e);
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 验证课程ID有效性
     */
    private void validateCourseId(long courseId) {
        if (courseId <= 0) {
            throw ErrorCode.COURSE_NOT_FOUND.exception();
        }
    }

    /**
     * 课程统计数据类
     */
    public static class CourseStats {
        private final long subscriptionCount;
        private final long learningCount;

        public CourseStats(long subscriptionCount, long learningCount) {
            this.subscriptionCount = subscriptionCount;
            this.learningCount = learningCount;
        }

        public long getSubscriptionCount() {
            return subscriptionCount;
        }

        public long getLearningCount() {
            return learningCount;
        }

        public long getTotalCount() {
            return subscriptionCount + learningCount;
        }
    }
}