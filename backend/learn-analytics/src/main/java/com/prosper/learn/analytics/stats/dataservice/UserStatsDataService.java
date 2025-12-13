package com.prosper.learn.analytics.stats.dataservice;

import com.prosper.learn.analytics.stats.mapper.UserStatsDO;
import com.prosper.learn.analytics.stats.mapper.UserStatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 用户统计数据服务
 *
 * 提供用户累计统计数据的CRUD操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsDataService {

    private final UserStatsMapper userStatsMapper;

    /**
     * 根据用户ID获取统计记录
     */
    public UserStatsDO getByUserId(Long userId) {
        return userStatsMapper.getByUserId(userId);
    }

    /**
     * 获取或创建用户统计记录
     */
    public UserStatsDO getOrCreate(Long userId) {
        UserStatsDO stats = getByUserId(userId);
        if (stats == null) {
            stats = createInitialStats(userId);
        }
        return stats;
    }

    /**
     * 创建初始统计记录
     */
    private UserStatsDO createInitialStats(Long userId) {
        UserStatsDO stats = new UserStatsDO();
        stats.setUserId(userId);

        // 初始化累计统计字段
        stats.setViews(0);
        stats.setTwices(0);
        stats.setLikes(0);
        stats.setComments(0);

        // 初始化其他统计字段
        stats.setLearningCourses(0);
        stats.setCompletedCourses(0);
        stats.setInProgressProfessions(0);
        stats.setCompletedProfessions(0);
        stats.setFollowingUsers(0);
        stats.setFollowingCourses(0);
        stats.setFollowingProfessions(0);
        stats.setCreatedArticles(0);
        stats.setCreatedIndexs(0);
        stats.setCreatedRoadmaps(0);
        stats.setCreatedCardDecks(0);

        int result = userStatsMapper.insert(stats);
        if (result <= 0) {
            log.error("创建用户统计记录失败: userId={}", userId);
            throw new RuntimeException("创建用户统计记录失败");
        }

        log.debug("创建用户统计记录: userId={}", userId);
        return stats;
    }

    /**
     * 原子性增量更新指定字段
     */
    public boolean atomicIncrement(Long userId, String field, int delta) {
        if (delta == 0) {
            return true;
        }

        // 确保统计记录存在
        getOrCreate(userId);

        int result = userStatsMapper.atomicIncrement(userId, field, delta);
        if (result > 0) {
            log.debug("原子增量更新: userId={}, field={}, delta={}", userId, field, delta);
            return true;
        }

        log.warn("原子增量更新失败: userId={}, field={}, delta={}", userId, field, delta);
        return false;
    }

// --注释掉检查 START (2025/12/10 11:32):
//    /**
//     * 原子性增量更新（带下限保护，不会小于0）
//     */
//    public boolean atomicIncrementWithFloor(Long userId, String field, int delta) {
//        if (delta == 0) {
//            return true;
//        }
//
//        // 确保统计记录存在
//        getOrCreate(userId);
//
//        int result = userStatsMapper.atomicIncrementWithFloor(userId, field, delta);
//        if (result > 0) {
//            log.debug("原子增量更新（带下限）: userId={}, field={}, delta={}", userId, field, delta);
//            return true;
//        }
//
//        log.warn("原子增量更新（带下限）失败: userId={}, field={}, delta={}", userId, field, delta);
//        return false;
//    }
// --注释掉检查 STOP (2025/12/10 11:32)

    /**
     * 设置字段绝对值
     */
    public boolean setField(Long userId, String field, int newValue) {
        // 确保统计记录存在
        getOrCreate(userId);

        int result = userStatsMapper.setField(userId, field, newValue);
        if (result > 0) {
            log.debug("设置字段值: userId={}, field={}, newValue={}", userId, field, newValue);
            return true;
        }

        log.warn("设置字段值失败: userId={}, field={}, newValue={}", userId, field, newValue);
        return false;
    }

    /**
     * 批量获取用户统计
     */
    public Map<Long, UserStatsDO> batchGetByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        List<UserStatsDO> statsList = userStatsMapper.batchGetByUserIds(userIds);
        return statsList.stream()
                .collect(java.util.stream.Collectors.toMap(
                        UserStatsDO::getUserId,
                        stats -> stats
                ));
    }

    /**
     * 获取排行榜数据
     */
    public List<UserStatsDO> getTopUsersByField(String field, int limit) {
        return userStatsMapper.getTopUsersByField(field, limit);
    }

    /**
     * 插入新记录
     */
    public int insert(UserStatsDO userStats) {
        return userStatsMapper.insert(userStats);
    }

// --注释掉检查 START (2025/12/10 11:32):
//    /**
//     * 根据ID删除
//     */
//    public int deleteById(Long id) {
//        return userStatsMapper.deleteById(id);
//    }
// --注释掉检查 STOP (2025/12/10 11:32)

    // ==================== 便捷方法 ====================

    /**
     * 增加浏览量
     */
    public boolean incrementViews(Long userId, int count) {
        return atomicIncrement(userId, "views", count);
    }

    /**
     * 增加两次能懂数
     */
    public boolean incrementTwices(Long userId, int count) {
        return atomicIncrement(userId, "twices", count);
    }

    /**
     * 增加有用点赞数
     */
    public boolean incrementLikes(Long userId, int count) {
        return atomicIncrement(userId, "likes", count);
    }

    /**
     * 增加评论数
     */
    public boolean incrementComments(Long userId, int count) {
        return atomicIncrement(userId, "comments", count);
    }

    /**
     * 增量更新多个统计字段
     */
    public boolean increase(Long userId, int viewsDelta, int twicesDelta, int likesDelta, int commentsDelta) {
        if (viewsDelta == 0 && twicesDelta == 0 && likesDelta == 0 && commentsDelta == 0) {
            return true;
        }

        // 确保统计记录存在
        getOrCreate(userId);

        int result = userStatsMapper.increase(userId, viewsDelta, twicesDelta, likesDelta, commentsDelta);
        if (result > 0) {
            log.debug("增量更新统计字段: userId={}, views+={}, twices+={}, likes+={}, comments+={}",
                userId, viewsDelta, twicesDelta, likesDelta, commentsDelta);
            return true;
        }

        log.warn("增量更新统计字段失败: userId={}", userId);
        return false;
    }
}