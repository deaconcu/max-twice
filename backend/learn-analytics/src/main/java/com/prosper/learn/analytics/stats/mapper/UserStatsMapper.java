package com.prosper.learn.analytics.stats.mapper;

import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface UserStatsMapper {

    // ===== 基础CRUD操作 =====

    @Insert("INSERT INTO user_stats (user_id, daily_stat_date, daily_views, daily_twice, daily_helpful, daily_comments, " +
            "learning_courses, completed_courses, in_progress_professions, completed_professions, " +
            "following_users, following_courses, following_professions, " +
            "created_articles, created_indexs, created_roadmaps, created_card_decks) " +
            "VALUES (#{userId}, #{dailyStatDate}, #{dailyViews}, #{dailyTwice}, #{dailyHelpful}, #{dailyComments}, " +
            "#{learningCourses}, #{completedCourses}, #{inProgressProfessions}, #{completedProfessions}, " +
            "#{followingUsers}, #{followingCourses}, #{followingProfessions}, " +
            "#{createdArticles}, #{createdIndexs}, #{createdRoadmaps}, #{createdCardDecks})")
    int insert(UserStatsDO userStats);

    @Select("SELECT * FROM user_stats WHERE user_id = #{userId} AND daily_stat_date = #{date}")
    UserStatsDO getByUserIdAndDate(long userId, LocalDate date);

    @Delete("DELETE FROM user_stats WHERE id = #{id}")
    int deleteById(long id);

    // ===== 原子增量更新操作 =====

    @Update("UPDATE user_stats SET ${field} = ${field} + #{delta} " +
            "WHERE user_id = #{userId} AND daily_stat_date = #{date}")
    int atomicIncrementDaily(@Param("userId") long userId, @Param("date") LocalDate date,
                           @Param("field") String field, @Param("delta") int delta);

    @Update("UPDATE user_stats SET ${field} = GREATEST(0, ${field} + #{delta}) " +
            "WHERE user_id = #{userId} AND daily_stat_date = #{date}")
    int atomicIncrementCumulative(@Param("userId") long userId, @Param("date") LocalDate date,
                                 @Param("field") String field, @Param("delta") int delta);

    @Update("UPDATE user_stats SET ${field} = #{newValue} " +
            "WHERE user_id = #{userId} AND daily_stat_date = #{date}")
    int setCumulativeStat(@Param("userId") long userId, @Param("date") LocalDate date,
                         @Param("field") String field, @Param("newValue") int newValue);

    // ===== 批量查询操作 =====

    @Select("<script>" +
            "SELECT * FROM user_stats " +
            "WHERE user_id IN " +
            "<foreach collection='userIds' item='userId' open='(' close=')' separator=','>" +
            "#{userId}" +
            "</foreach> " +
            "AND daily_stat_date = #{date}" +
            "</script>")
    List<UserStatsDO> batchGetCurrentStats(@Param("userIds") List<Long> userIds, @Param("date") LocalDate date);

    @Select("SELECT * FROM user_stats WHERE user_id = #{userId} AND daily_stat_date < #{beforeDate} " +
            "ORDER BY daily_stat_date ASC")
    List<UserStatsDO> getStaleStats(@Param("userId") long userId, @Param("beforeDate") LocalDate beforeDate);

    @Select("SELECT * FROM user_stats WHERE daily_stat_date < DATE_SUB(#{currentDate}, INTERVAL #{days} DAY) " +
            "ORDER BY daily_stat_date ASC")
    List<UserStatsDO> getStatsOlderThan(@Param("currentDate") LocalDate currentDate, @Param("days") int days);

    @Select("SELECT * FROM user_stats WHERE daily_stat_date = #{date} " +
            "ORDER BY ${field} DESC LIMIT #{limit}")
    List<UserStatsDO> getTopUsersByField(@Param("field") String field, @Param("limit") int limit, @Param("date") LocalDate date);
}