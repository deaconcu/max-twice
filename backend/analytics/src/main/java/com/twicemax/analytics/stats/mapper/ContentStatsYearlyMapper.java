package com.twicemax.analytics.stats.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface ContentStatsYearlyMapper {

    // ===== 基础CRUD操作 =====
    
    @Insert("INSERT INTO content_stats_yearly (object_type, object_id, stats, stat_year, created_at, updated_at) " +
            "VALUES (#{objectType}, #{objectId}, #{stats}, #{statYear}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ContentStatsYearlyDO stats);

    @Select("SELECT * FROM content_stats_yearly WHERE object_type = #{objectType} AND object_id = #{objectId} AND stat_year = #{statYear}")
    ContentStatsYearlyDO getByTypeAndObjectIdAndYear(int objectType, long objectId, int statYear);

    // ===== 同步操作（直接覆盖）=====
    
    // 直接设置当天完整统计数据（用于同步）- 使用数组格式 [views, twice, like, comments]
    @Update("UPDATE content_stats_yearly SET " +
            "stats = JSON_SET(" +
            "  COALESCE(stats, JSON_OBJECT()), " +
            "  CONCAT('$.\"', #{dayKey}, '\"'), " +
            "  JSON_ARRAY(#{viewCount}, #{twiceCount}, #{likeCount}, #{commentCount})" +
            "), " +
            "updated_at = NOW() " +
            "WHERE object_type = #{objectType} AND object_id = #{objectId} AND stat_year = #{statYear}")
    int setDayStats(int objectType, long objectId, int statYear, String dayKey, int viewCount, int twiceCount,
                    int likeCount, int commentCount);

    // ===== 查询操作 =====
    
    // 获取指定日期的统计数据
    @Select("SELECT JSON_EXTRACT(stats, CONCAT('$.\"', #{dayKey}, '\"')) " +
            "FROM content_stats_yearly " +
            "WHERE object_type = #{objectType} AND object_id = #{objectId} AND stat_year = #{statYear}")
    String getDayStats(int objectType, long objectId, int statYear, String dayKey);
}
