package com.prosper.learn.analytics.stats.dataservice;

import com.prosper.learn.analytics.stats.mapper.UserStatsDO;
import com.prosper.learn.analytics.stats.mapper.UserStatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
    public UserStatsDO getByUserId(long userId) {
        return userStatsMapper.getByUserId(userId);
    }

    /**
     * 获取或创建用户统计记录
     */
    public UserStatsDO getOrCreate(long userId) {
        UserStatsDO stats = getByUserId(userId);
        if (stats == null) {
            stats = createInitialStats(userId);
        }
        return stats;
    }

    /**
     * 创建初始统计记录
     */
    private UserStatsDO createInitialStats(long userId) {
        UserStatsDO stats = new UserStatsDO();
        stats.setUserId(userId);

        // 初始化累计统计字段
        stats.setViewCount(0);
        stats.setTwiceCount(0);
        stats.setLikeCount(0);
        stats.setCommentCount(0);

        // 初始化其他统计字段
        stats.setLearningCourseCount(0);
        stats.setCompletedCourseCount(0);
        stats.setInProgressProfessionCount(0);
        stats.setCompletedProfessionCount(0);
        stats.setFollowingUserCount(0);
        stats.setFollowingCourseCount(0);
        stats.setFollowingProfessionCount(0);
        stats.setCreatedArticleCount(0);
        stats.setCreatedIndexCount(0);
        stats.setCreatedRoadmapCount(0);
        stats.setCreatedCardDeckCount(0);
        stats.setReviewStreakDays(0);
        stats.setLastCardReviewDate(null);
        stats.setLearningStreakDays(0);
        stats.setLastLearningDate(null);

        int result = userStatsMapper.insert(stats);
        if (result <= 0) {
            log.error("创建用户统计记录失败: userId={}", userId);
            throw new RuntimeException("创建用户统计记录失败");
        }

        log.debug("创建用户统计记录: userId={}", userId);
        return stats;
    }

    /**
     * 原子性增量更新指定字段（内部方法）
     */
    private boolean atomicIncrement(long userId, String field, int delta) {
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

    // ==================== 关注类统计字段更新方法 ====================

    /** 增量更新关注用户数 */
    public boolean incrementFollowingUsers(long userId, int delta) {
        return atomicIncrement(userId, "following_user_count", delta);
    }

    /** 增量更新关注课程数 */
    public boolean incrementFollowingCourses(long userId, int delta) {
        return atomicIncrement(userId, "following_course_count", delta);
    }

    /** 增量更新关注职业数 */
    public boolean incrementFollowingProfessions(long userId, int delta) {
        return atomicIncrement(userId, "following_profession_count", delta);
    }

    // ==================== 学习类统计字段更新方法 ====================

    /** 增量更新学习中课程数 */
    public boolean incrementLearningCourses(long userId, int delta) {
        return atomicIncrement(userId, "learning_course_count", delta);
    }

    /** 增量更新已完成课程数 */
    public boolean incrementCompletedCourses(long userId, int delta) {
        return atomicIncrement(userId, "completed_course_count", delta);
    }

    /** 增量更新进行中职业数 */
    public boolean incrementInProgressProfessions(long userId, int delta) {
        return atomicIncrement(userId, "in_progress_profession_count", delta);
    }

    /** 增量更新已完成职业数 */
    public boolean incrementCompletedProfessions(long userId, int delta) {
        return atomicIncrement(userId, "completed_profession_count", delta);
    }

    // ==================== 创作类统计字段更新方法 ====================

    /** 增量更新创建文章数 */
    public boolean incrementCreatedArticles(long userId, int delta) {
        return atomicIncrement(userId, "created_article_count", delta);
    }

    /** 增量更新创建目录数 */
    public boolean incrementCreatedIndexs(long userId, int delta) {
        return atomicIncrement(userId, "created_index_count", delta);
    }

    /** 增量更新创建路线图数 */
    public boolean incrementCreatedRoadmaps(long userId, int delta) {
        return atomicIncrement(userId, "created_roadmap_count", delta);
    }

    /** 增量更新创建卡片组数 */
    public boolean incrementCreatedCardDecks(long userId, int delta) {
        return atomicIncrement(userId, "created_card_deck_count", delta);
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
     * 设置字段绝对值（内部方法）
     */
    private boolean setField(long userId, String field, int newValue) {
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

    /** 设置关注用户数 */
    public boolean setFollowingUsers(long userId, int newValue) {
        return setField(userId, "following_user_count", newValue);
    }

    /** 设置关注课程数 */
    public boolean setFollowingCourses(long userId, int newValue) {
        return setField(userId, "following_course_count", newValue);
    }

    /** 设置关注职业数 */
    public boolean setFollowingProfessions(long userId, int newValue) {
        return setField(userId, "following_profession_count", newValue);
    }

    /** 设置学习中课程数 */
    public boolean setLearningCourses(long userId, int newValue) {
        return setField(userId, "learning_course_count", newValue);
    }

    /** 设置已完成课程数 */
    public boolean setCompletedCourses(long userId, int newValue) {
        return setField(userId, "completed_course_count", newValue);
    }

    /** 设置进行中职业数 */
    public boolean setInProgressProfessions(long userId, int newValue) {
        return setField(userId, "in_progress_profession_count", newValue);
    }

    /** 设置已完成职业数 */
    public boolean setCompletedProfessions(long userId, int newValue) {
        return setField(userId, "completed_profession_count", newValue);
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
     * 获取排行榜数据（内部方法）
     */
    private List<UserStatsDO> getTopUsersByField(String field, int limit) {
        return userStatsMapper.getTopUsersByField(field, limit);
    }

    /** 按浏览量排行 */
    public List<UserStatsDO> getTopUsersByViews(int limit) {
        return getTopUsersByField("view_count", limit);
    }

    /** 按两次能懂数排行 */
    public List<UserStatsDO> getTopUsersByTwices(int limit) {
        return getTopUsersByField("twice_count", limit);
    }

    /** 按有用点赞数排行 */
    public List<UserStatsDO> getTopUsersByLikes(int limit) {
        return getTopUsersByField("like_count", limit);
    }

    /** 按评论数排行 */
    public List<UserStatsDO> getTopUsersByComments(int limit) {
        return getTopUsersByField("comment_count", limit);
    }

    /** 按创建文章数排行 */
    public List<UserStatsDO> getTopUsersByCreatedArticles(int limit) {
        return getTopUsersByField("created_article_count", limit);
    }

    /** 按创建目录数排行 */
    public List<UserStatsDO> getTopUsersByCreatedIndexs(int limit) {
        return getTopUsersByField("created_index_count", limit);
    }

    /** 按创建路线图数排行 */
    public List<UserStatsDO> getTopUsersByCreatedRoadmaps(int limit) {
        return getTopUsersByField("created_roadmap_count", limit);
    }

    /** 按创建卡片组数排行 */
    public List<UserStatsDO> getTopUsersByCreatedCardDecks(int limit) {
        return getTopUsersByField("created_card_deck_count", limit);
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
    public boolean incrementViews(long userId, int count) {
        return atomicIncrement(userId, "view_count", count);
    }

    /**
     * 增加两次能懂数
     */
    public boolean incrementTwices(long userId, int count) {
        return atomicIncrement(userId, "twice_count", count);
    }

    /**
     * 增加有用点赞数
     */
    public boolean incrementLikes(long userId, int count) {
        return atomicIncrement(userId, "like_count", count);
    }

    /**
     * 增加评论数
     */
    public boolean incrementComments(long userId, int count) {
        return atomicIncrement(userId, "comment_count", count);
    }

    /**
     * 增量更新多个统计字段并更新同步日期
     * 用于从 Redis 同步数据时防止重复累加
     *
     * @param syncDate 同步日期（格式：YYYY-MM-DD）
     */
    public boolean increase(long userId, int viewsDelta, int twicesDelta, int likesDelta, int commentsDelta, String syncDate) {
        if (viewsDelta == 0 && twicesDelta == 0 && likesDelta == 0 && commentsDelta == 0) {
            return true;
        }

        // 确保统计记录存在
        getOrCreate(userId);

        int result = userStatsMapper.increase(userId, viewsDelta, twicesDelta, likesDelta, commentsDelta, syncDate);
        if (result > 0) {
            log.debug("增量更新统计字段: userId={}, syncDate={}, views+={}, twices+={}, likes+={}, comments+={}",
                userId, syncDate, viewsDelta, twicesDelta, likesDelta, commentsDelta);
            return true;
        }

        log.warn("增量更新统计字段失败: userId={}", userId);
        return false;
    }

    // ==================== 记忆卡片复习统计 ====================

    /**
     * 更新复习连续天数和最后复习日期
     */
    public boolean updateReviewStreak(long userId, int streakDays, LocalDate lastReviewDate) {
        getOrCreate(userId);
        int result = userStatsMapper.updateReviewStreak(userId, streakDays, lastReviewDate);
        if (result > 0) {
            log.debug("更新复习连续天数: userId={}, streakDays={}, lastReviewDate={}", userId, streakDays, lastReviewDate);
            return true;
        }
        log.warn("更新复习连续天数失败: userId={}", userId);
        return false;
    }

    // ==================== 学习统计（阅读文章）====================

    /**
     * 更新学习连续天数和最后学习日期
     */
    public boolean updateLearningStreak(long userId, int streakDays, LocalDate lastLearningDate) {
        getOrCreate(userId);
        int result = userStatsMapper.updateLearningStreak(userId, streakDays, lastLearningDate);
        if (result > 0) {
            log.debug("更新学习连续天数: userId={}, streakDays={}, lastLearningDate={}", userId, streakDays, lastLearningDate);
            return true;
        }
        log.warn("更新学习连续天数失败: userId={}", userId);
        return false;
    }
}