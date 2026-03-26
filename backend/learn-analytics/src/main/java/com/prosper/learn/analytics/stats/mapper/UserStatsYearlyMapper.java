package com.prosper.learn.analytics.stats.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserStatsYearlyMapper {

    // ===== 基础CRUD操作 =====

    @Insert("INSERT INTO user_stats_yearly (user_id, stats, stat_year) " +
            "VALUES (#{userId}, #{stats}, #{statYear})")
    int insert(UserStatsYearlyDO userStats);

    @Select("SELECT * FROM user_stats_yearly WHERE user_id = #{userId} AND stat_year = #{statYear}")
    UserStatsYearlyDO getByUserIdAndYear(long userId, int statYear);

    // ===== yearly表：更新JSON统计数据 =====

    /**
     * 更新创作统计数据（被浏览、点赞等）
     * JSON数组格式：[views, twice, like, comments, completedNodes, cancelCompletedNodes, reviewedCards]
     * 此方法只更新前4个字段，保留后3个字段不变
     */
    @Update("UPDATE user_stats_yearly " +
            "SET stats = JSON_SET(" +
            "  COALESCE(stats, JSON_OBJECT()), " +
            "  CONCAT('$.\"', #{dateKey}, '\"'), " +
            "  JSON_ARRAY(#{views}, #{twice}, #{like}, #{comments}, " +
            "    CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(stats, CONCAT('$.\"', #{dateKey}, '\"[4]'))), '0') AS SIGNED), " +
            "    CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(stats, CONCAT('$.\"', #{dateKey}, '\"[5]'))), '0') AS SIGNED), " +
            "    CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(stats, CONCAT('$.\"', #{dateKey}, '\"[6]'))), '0') AS SIGNED))" +
            ") " +
            "WHERE user_id = #{userId} AND stat_year = #{statYear}")
    int updateYearlyStatsArray(@Param("userId") long userId, @Param("statYear") int statYear,
                              @Param("dateKey") String dateKey, @Param("views") int views,
                              @Param("twice") int twice, @Param("like") int like,
                              @Param("comments") int comments);

    /**
     * 更新学习统计数据（完成节点、取消完成节点、复习卡片）
     * JSON数组格式：[views, twice, like, comments, completedNodes, cancelCompletedNodes, reviewedCards]
     * 此方法只更新后3个字段，保留前4个字段不变
     */
    @Update("UPDATE user_stats_yearly " +
            "SET stats = JSON_SET(" +
            "  COALESCE(stats, JSON_OBJECT()), " +
            "  CONCAT('$.\"', #{dateKey}, '\"'), " +
            "  JSON_ARRAY(" +
            "    CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(stats, CONCAT('$.\"', #{dateKey}, '\"[0]'))), '0') AS SIGNED), " +
            "    CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(stats, CONCAT('$.\"', #{dateKey}, '\"[1]'))), '0') AS SIGNED), " +
            "    CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(stats, CONCAT('$.\"', #{dateKey}, '\"[2]'))), '0') AS SIGNED), " +
            "    CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(stats, CONCAT('$.\"', #{dateKey}, '\"[3]'))), '0') AS SIGNED), " +
            "    #{completedNodes}, #{cancelCompletedNodes}, #{reviewedCards})" +
            ") " +
            "WHERE user_id = #{userId} AND stat_year = #{statYear}")
    int updateYearlyLearningStats(@Param("userId") long userId, @Param("statYear") int statYear,
                                  @Param("dateKey") String dateKey,
                                  @Param("completedNodes") int completedNodes,
                                  @Param("cancelCompletedNodes") int cancelCompletedNodes,
                                  @Param("reviewedCards") int reviewedCards);

    // 获取指定日期的统计数据
    @Select("SELECT JSON_EXTRACT(stats, CONCAT('$.\"', #{dayKey}, '\"')) " +
            "FROM user_stats_yearly " +
            "WHERE user_id = #{userId} AND stat_year = #{statYear}")
    String getDayStats(long userId, int statYear, String dayKey);

}