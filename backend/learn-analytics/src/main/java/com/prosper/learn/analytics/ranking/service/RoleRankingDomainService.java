package com.prosper.learn.analytics.ranking.service;

import com.prosper.learn.shared.domain.exception.StatusCode;
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
public class RoleRankingDomainService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SystemProperties systemProperties;
    
    private static final String HOT_ROLES_KEY = "role:hot:ranking";
    private static final String ROLE_LEARNING_PREFIX = "role:learning:";

    /**
     * 获取热门职业ID列表（按学习人数降序）
     */
    public List<Long> getHotRoleIds(int limit) {
        validateLimit(limit);
        try {
            Set<String> roleIds = redisTemplate.opsForZSet().reverseRange(HOT_ROLES_KEY, 0, limit - 1);
            if (roleIds == null || roleIds.isEmpty()) {
                return List.of();
            }
            
            return roleIds.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("职业排行榜 获取热门职业失败，limit: {}", limit, e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 获取职业的学习人数
     */
    public long getRoleLearningCount(long roleId) {
        validateRoleId(roleId);
        try {
            String learningKey = generateLearningKey(roleId);
            String learningCountStr = redisTemplate.opsForValue().get(learningKey);
            return learningCountStr != null ? Long.parseLong(learningCountStr) : 0;
        } catch (Exception e) {
            log.error("职业排行榜 获取职业学习人数失败，roleId: {}", roleId, e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 批量初始化职业统计数据（用于定时任务）
     */
    public void initializeRoleStats(long roleId, long learningCount) {
        validateRoleId(roleId);
        try {
            String learningKey = generateLearningKey(roleId);
            redisTemplate.opsForValue().set(learningKey, String.valueOf(learningCount));
            redisTemplate.opsForZSet().add(HOT_ROLES_KEY, String.valueOf(roleId), learningCount);
            
            log.debug("职业排行榜 初始化统计: roleId={}，learningCount={}",
                     roleId, learningCount);
        } catch (Exception e) {
            log.error("职业排行榜 初始化统计失败，roleId: {}", roleId, e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 清空所有统计数据（用于重新同步）
     */
    public void clearAllStats() {
        try {
            // 清空热门职业排行榜
            redisTemplate.delete(HOT_ROLES_KEY);
            
            // 删除所有职业的统计数据
            Set<String> learningKeys = redisTemplate.keys(ROLE_LEARNING_PREFIX + "*");
            
            if (learningKeys != null && !learningKeys.isEmpty()) {
                redisTemplate.delete(learningKeys);
            }
            
            log.info("职业排行榜 已清空所有 Redis 统计数据");
        } catch (Exception e) {
            log.error("职业排行榜 清空统计数据失败", e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 生成职业学习数Redis键名
     */
    private String generateLearningKey(long roleId) {
        return ROLE_LEARNING_PREFIX + roleId;
    }

    /**
     * 验证职业ID有效性
     */
    private void validateRoleId(long roleId) {
        if (roleId <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception();
        }
    }

    /**
     * 验证限制数量有效性
     */
    private void validateLimit(int limit) {
        if (limit <= 0 || limit > systemProperties.getCourseRanking().getMaxHotCoursesLimit()) {
            throw StatusCode.INVALID_PARAMETER.exception();
        }
    }
}