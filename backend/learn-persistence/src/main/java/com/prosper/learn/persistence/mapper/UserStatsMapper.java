package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.UserStatsDO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface UserStatsMapper {

    @Insert("INSERT INTO user_stats (user_id, stat_date, total_views, total_twice, total_helpful, total_comments, created_at, updated_at) " +
            "VALUES (#{userId}, #{statDate}, #{totalViews}, #{totalTwice}, #{totalHelpful}, #{totalComments}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserStatsDO userStats);

    @Update("UPDATE user_stats SET " +
            "total_views = #{totalViews}, " +
            "total_twice = #{totalTwice}, " +
            "total_helpful = #{totalHelpful}, " +
            "total_comments = #{totalComments}, " +
            "updated_at = NOW() " +
            "WHERE user_id = #{userId} AND stat_date = #{statDate}")
    int update(UserStatsDO userStats);

    @Select("SELECT * FROM user_stats WHERE user_id = #{userId} AND stat_date = #{statDate}")
    UserStatsDO getByUserIdAndDate(@Param("userId") Integer userId, @Param("statDate") LocalDate statDate);

    @Select("SELECT * FROM user_stats WHERE user_id = #{userId} " +
            "AND stat_date >= #{startDate} AND stat_date <= #{endDate} " +
            "ORDER BY stat_date ASC")
    List<UserStatsDO> getByUserIdAndDateRange(@Param("userId") Integer userId, 
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    @Select("SELECT " +
            "COALESCE(SUM(total_views), 0) as totalViews, " +
            "COALESCE(SUM(total_twice), 0) as totalTwice, " +
            "COALESCE(SUM(total_helpful), 0) as totalHelpful, " +
            "COALESCE(SUM(total_comments), 0) as totalComments " +
            "FROM user_stats " +
            "WHERE user_id = #{userId} AND stat_date >= #{startDate} AND stat_date <= #{endDate}")
    UserStatsDO sumStatsByDateRange(@Param("userId") Integer userId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    @Delete("DELETE FROM user_stats WHERE stat_date < #{beforeDate}")
    int deleteOldStats(@Param("beforeDate") LocalDate beforeDate);

    @Select("SELECT DISTINCT user_id FROM user_stats WHERE stat_date = #{statDate}")
    List<Integer> getUserIdsByDate(@Param("statDate") LocalDate statDate);
}