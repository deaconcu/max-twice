package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.UserStatsYearlyDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserStatsYearlyMapper {

    // ===== 基础CRUD操作 =====

    @Insert("INSERT INTO user_stats_yearly (user_id, stats, stat_year) " +
            "VALUES (#{userId}, #{stats}, #{statYear})")
    int insert(UserStatsYearlyDO userStats);

    @Update("UPDATE user_stats_yearly SET stats = #{stats} " +
            "WHERE user_id = #{userId} AND stat_year = #{statYear}")
    int updateStats(UserStatsYearlyDO userStats);

    @Select("SELECT * FROM user_stats_yearly WHERE user_id = #{userId} AND stat_year = #{statYear}")
    UserStatsYearlyDO getByUserIdAndYear(long userId, int statYear);

    @Select("SELECT * FROM user_stats_yearly WHERE user_id = #{userId} " +
            "AND stat_year >= #{startYear} ORDER BY stat_year DESC")
    List<UserStatsYearlyDO> getStatsInYearRange(long userId, int startYear);

    // ===== yearly表：更新JSON统计数据 =====

    @Update("UPDATE user_stats_yearly " +
            "SET stats = JSON_SET(" +
            "  COALESCE(stats, JSON_OBJECT()), " +
            "  CONCAT('$.', #{dateKey}), " +
            "  JSON_ARRAY(#{views}, #{twice}, #{helpful}, #{comments})" +
            ") " +
            "WHERE user_id = #{userId} AND stat_year = #{statYear}")
    int updateYearlyStatsArray(@Param("userId") long userId, @Param("statYear") int statYear,
                              @Param("dateKey") String dateKey, @Param("views") int views,
                              @Param("twice") int twice, @Param("helpful") int helpful,
                              @Param("comments") int comments);

    // 获取指定日期的统计数据
    @Select("SELECT JSON_EXTRACT(stats, CONCAT('$.\"', #{dayKey}, '\"')) " +
            "FROM user_stats_yearly " +
            "WHERE user_id = #{userId} AND stat_year = #{statYear}")
    String getDayStats(long userId, int statYear, String dayKey);

}