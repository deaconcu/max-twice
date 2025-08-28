package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.PostStatsDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PostStatsMapper {

    // ===== 基础CRUD操作 =====
    
    @Insert("INSERT INTO post_stats (type, object_id, stats, stat_year, created_at, updated_at) " +
            "VALUES (#{type}, #{objectId}, #{stats}, #{statYear}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PostStatsDO stats);

    @Update("UPDATE post_stats SET stats = #{stats}, updated_at = NOW() " +
            "WHERE type = #{type} AND object_id = #{objectId} AND stat_year = #{statYear}")
    int updateStats(PostStatsDO stats);

    @Select("SELECT * FROM post_stats WHERE type = #{type} AND object_id = #{objectId} AND stat_year = #{statYear}")
    PostStatsDO getByTypeAndObjectIdAndYear(byte type, long objectId, Integer statYear);

    @Select("SELECT * FROM post_stats WHERE type = #{type} AND object_id = #{objectId} " +
            "AND stat_year >= #{startYear} ORDER BY stat_year DESC")
    List<PostStatsDO> getStatsInYearRange(byte type, long objectId, Integer startYear);

    @Select("SELECT DISTINCT object_id FROM post_stats WHERE type = #{type}")
    List<Long> getAllObjectIdsByType(byte type);

    // ===== 实时统计操作（增量更新）=====
    
    // 使用MySQL JSON函数直接增加计数（用于实时统计）
    @Update("UPDATE post_stats SET " +
            "stats = JSON_SET(" +
            "  COALESCE(stats, JSON_OBJECT()), " +
            "  CONCAT('$.\"', #{dayKey}, '\"'), " +
            "  JSON_SET(" +
            "    COALESCE(JSON_EXTRACT(stats, CONCAT('$.\"', #{dayKey}, '\"')), JSON_OBJECT('twice', 0, 'helpful', 0, 'views', 0, 'comments', 0)), " +
            "    CONCAT('$.', #{statType}), " +
            "    CAST(COALESCE(JSON_EXTRACT(stats, CONCAT('$.\"', #{dayKey}, '\".', #{statType})), 0) AS SIGNED) + #{count}" +
            "  )" +
            "), " +
            "updated_at = NOW() " +
            "WHERE type = #{type} AND object_id = #{objectId} AND stat_year = #{statYear}")
    int incrementStatsCount(byte type, long objectId, int statYear, String dayKey, String statType, int count);

    // 使用MySQL JSON函数直接减少计数（用于撤销操作）
    @Update("UPDATE post_stats SET " +
            "stats = JSON_SET(" +
            "  stats, " +
            "  CONCAT('$.\"', #{dayKey}, '\".', #{statType}), " +
            "  GREATEST(0, CAST(COALESCE(JSON_EXTRACT(stats, CONCAT('$.\"', #{dayKey}, '\".', #{statType})), 0) AS SIGNED) - #{count})" +
            "), " +
            "updated_at = NOW() " +
            "WHERE type = #{type} AND object_id = #{objectId} AND stat_year = #{statYear} " +
            "AND JSON_EXTRACT(stats, CONCAT('$.\"', #{dayKey}, '\"')) IS NOT NULL")
    int decrementStatsCount(byte type, long objectId, int statYear, String dayKey, String statType, int count);

    // ===== 同步操作（直接覆盖）=====
    
    // 直接设置当天完整统计数据（用于同步）
    @Update("UPDATE post_stats SET " +
            "stats = JSON_SET(" +
            "  COALESCE(stats, JSON_OBJECT()), " +
            "  CONCAT('$.\"', #{dayKey}, '\"'), " +
            "  JSON_OBJECT(" +
            "    'views', #{views}, " +
            "    'twice', #{twice}, " +
            "    'helpful', #{helpful}, " +
            "    'comments', #{comments}" +
            "  )" +
            "), " +
            "updated_at = NOW() " +
            "WHERE type = #{type} AND object_id = #{objectId} AND stat_year = #{statYear}")
    int setDayStats(byte type, long objectId, int statYear, String dayKey, int views, int twice,
                    int helpful, int comments);

    // ===== 查询操作 =====
    
    // 获取指定日期的统计数据
    @Select("SELECT JSON_EXTRACT(stats, CONCAT('$.\"', #{dayKey}, '\"')) " +
            "FROM post_stats " +
            "WHERE type = #{type} AND object_id = #{objectId} AND stat_year = #{statYear}")
    String getDayStats(byte type, long objectId, int statYear, String dayKey);
}
