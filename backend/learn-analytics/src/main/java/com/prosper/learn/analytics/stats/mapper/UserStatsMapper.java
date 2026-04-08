package com.prosper.learn.analytics.stats.mapper;

import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface UserStatsMapper {

    // ===== 基础CRUD操作 =====

    @Insert("INSERT INTO user_stats (user_id, view_count, twice_count, like_count, comment_count, " +
            "learning_course_count, completed_course_count, in_progress_role_count, completed_role_count, " +
            "following_user_count, following_course_count, following_role_count, " +
            "created_article_count, created_index_count, created_roadmap_count, created_card_deck_count, " +
            "review_streak_days, last_card_review_date, learning_streak_days, last_learning_date) " +
            "VALUES (#{userId}, #{viewCount}, #{twiceCount}, #{likeCount}, #{commentCount}, " +
            "#{learningCourseCount}, #{completedCourseCount}, #{inProgressRoleCount}, #{completedRoleCount}, " +
            "#{followingUserCount}, #{followingCourseCount}, #{followingRoleCount}, " +
            "#{createdArticleCount}, #{createdIndexCount}, #{createdRoadmapCount}, #{createdCardDeckCount}, " +
            "#{reviewStreakDays}, #{lastCardReviewDate}, #{learningStreakDays}, #{lastLearningDate})")
    int insert(UserStatsDO userStats);

    @Select("SELECT * FROM user_stats WHERE user_id = #{userId}")
    UserStatsDO getByUserId(long userId);

    // ===== 原子增量更新操作 =====

    @Update("UPDATE user_stats SET ${field} = ${field} + #{delta} " +
            "WHERE user_id = #{userId}")
    int atomicIncrement(@Param("userId") long userId,
                       @Param("field") String field, @Param("delta") int delta);

    @Update("UPDATE user_stats SET ${field} = #{newValue} " +
            "WHERE user_id = #{userId}")
    int setField(@Param("userId") long userId,
                @Param("field") String field, @Param("newValue") int newValue);

    @Update("UPDATE user_stats SET " +
            "view_count = view_count + #{viewsDelta}, " +
            "twice_count = twice_count + #{twicesDelta}, " +
            "like_count = like_count + #{likesDelta}, " +
            "comment_count = comment_count + #{commentsDelta}, " +
            "last_sync_date = #{syncDate}, " +
            "updated_at = NOW() " +
            "WHERE user_id = #{userId}")
    int increase(@Param("userId") long userId,
                 @Param("viewsDelta") int viewsDelta,
                 @Param("twicesDelta") int twicesDelta,
                 @Param("likesDelta") int likesDelta,
                 @Param("commentsDelta") int commentsDelta,
                 @Param("syncDate") String syncDate);

    // ===== 批量查询操作 =====

    @Select("<script>" +
            "SELECT * FROM user_stats " +
            "WHERE user_id IN " +
            "<foreach collection='userIds' item='userId' open='(' close=')' separator=','>" +
            "#{userId}" +
            "</foreach>" +
            "</script>")
    List<UserStatsDO> batchGetByUserIds(@Param("userIds") List<Long> userIds);

    @Select("SELECT * FROM user_stats ORDER BY ${field} DESC LIMIT #{limit}")
    List<UserStatsDO> getTopUsersByField(@Param("field") String field, @Param("limit") int limit);

    // ===== 记忆卡片复习统计 =====

    @Update("UPDATE user_stats SET review_streak_days = #{streakDays}, last_card_review_date = #{lastReviewDate}, " +
            "updated_at = NOW() WHERE user_id = #{userId}")
    int updateReviewStreak(@Param("userId") long userId,
                           @Param("streakDays") int streakDays,
                           @Param("lastReviewDate") LocalDate lastReviewDate);

    // ===== 学习统计（阅读文章）=====

    @Update("UPDATE user_stats SET learning_streak_days = #{streakDays}, last_learning_date = #{lastLearningDate}, " +
            "updated_at = NOW() WHERE user_id = #{userId}")
    int updateLearningStreak(@Param("userId") long userId,
                             @Param("streakDays") int streakDays,
                             @Param("lastLearningDate") LocalDate lastLearningDate);
}