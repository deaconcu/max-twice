package com.prosper.learn.analytics.stats.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserStatsYearlyMapper {

    // ===== 基础CRUD操作 =====

    @Insert("INSERT INTO user_stats_yearly (user_id, stats, stat_year) " +
            "VALUES (#{userId}, #{stats}, #{statYear})")
    int insert(UserStatsYearlyDO userStats);

// --注释掉检查 START (2025/12/10 12:05):
//    @Update("UPDATE user_stats_yearly SET stats = #{stats} " +
//            "WHERE user_id = #{userId} AND stat_year = #{statYear}")
//    int updateStats(UserStatsYearlyDO userStats);
// --注释掉检查 STOP (2025/12/10 12:05)

    @Select("SELECT * FROM user_stats_yearly WHERE user_id = #{userId} AND stat_year = #{statYear}")
    UserStatsYearlyDO getByUserIdAndYear(long userId, int statYear);

// --注释掉检查 START (2025/12/10 12:05):
//    @Select("SELECT * FROM user_stats_yearly WHERE user_id = #{userId} " +
//            "AND stat_year >= #{startYear} ORDER BY stat_year DESC")
//    List<UserStatsYearlyDO> getStatsInYearRange(long userId, int startYear);
// --注释掉检查 STOP (2025/12/10 12:05)

    // ===== yearly表：更新JSON统计数据 =====

    @Update("UPDATE user_stats_yearly " +
            "SET stats = JSON_SET(" +
            "  COALESCE(stats, JSON_OBJECT()), " +
            "  CONCAT('$.\"', #{dateKey}, '\"'), " +
            "  JSON_ARRAY(#{views}, #{twice}, #{like}, #{comments})" +
            ") " +
            "WHERE user_id = #{userId} AND stat_year = #{statYear}")
    int updateYearlyStatsArray(@Param("userId") long userId, @Param("statYear") int statYear,
                              @Param("dateKey") String dateKey, @Param("views") int views,
                              @Param("twice") int twice, @Param("like") int like,
                              @Param("comments") int comments);

    // 获取指定日期的统计数据
    @Select("SELECT JSON_EXTRACT(stats, CONCAT('$.\"', #{dayKey}, '\"')) " +
            "FROM user_stats_yearly " +
            "WHERE user_id = #{userId} AND stat_year = #{statYear}")
    String getDayStats(long userId, int statYear, String dayKey);

}