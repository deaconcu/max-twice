package com.prosper.learn.analytics.stats.mapper;

import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface UserStatsMapper {

    // ===== 基础CRUD操作 =====

    @Insert("INSERT INTO user_stats (user_id, views, twices, likes, comments, " +
            "learning_courses, completed_courses, in_progress_professions, completed_professions, " +
            "following_users, following_courses, following_professions, " +
            "created_articles, created_indexs, created_roadmaps, created_card_decks) " +
            "VALUES (#{userId}, #{views}, #{twices}, #{likes}, #{comments}, " +
            "#{learningCourses}, #{completedCourses}, #{inProgressProfessions}, #{completedProfessions}, " +
            "#{followingUsers}, #{followingCourses}, #{followingProfessions}, " +
            "#{createdArticles}, #{createdIndexs}, #{createdRoadmaps}, #{createdCardDecks})")
    int insert(UserStatsDO userStats);

    @Select("SELECT * FROM user_stats WHERE user_id = #{userId}")
    UserStatsDO getByUserId(long userId);

    @Delete("DELETE FROM user_stats WHERE id = #{id}")
    int deleteById(long id);

    // ===== 原子增量更新操作 =====

    @Update("UPDATE user_stats SET ${field} = ${field} + #{delta} " +
            "WHERE user_id = #{userId}")
    int atomicIncrement(@Param("userId") long userId,
                       @Param("field") String field, @Param("delta") int delta);

    @Update("UPDATE user_stats SET ${field} = GREATEST(0, ${field} + #{delta}) " +
            "WHERE user_id = #{userId}")
    int atomicIncrementWithFloor(@Param("userId") long userId,
                                  @Param("field") String field, @Param("delta") int delta);

    @Update("UPDATE user_stats SET ${field} = #{newValue} " +
            "WHERE user_id = #{userId}")
    int setField(@Param("userId") long userId,
                @Param("field") String field, @Param("newValue") int newValue);

    @Update("UPDATE user_stats SET " +
            "views = views + #{viewsDelta}, " +
            "twices = twices + #{twicesDelta}, " +
            "likes = likes + #{likesDelta}, " +
            "comments = comments + #{commentsDelta} " +
            "WHERE user_id = #{userId}")
    int increase(@Param("userId") long userId,
                 @Param("viewsDelta") int viewsDelta,
                 @Param("twicesDelta") int twicesDelta,
                 @Param("likesDelta") int likesDelta,
                 @Param("commentsDelta") int commentsDelta);

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
}