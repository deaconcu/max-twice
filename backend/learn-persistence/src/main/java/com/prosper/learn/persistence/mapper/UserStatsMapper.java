package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.UserStatsDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserStatsMapper {

    // ===== 基础CRUD操作 =====
    
    @Insert("INSERT INTO user_stats (user_id, stats, stat_year, created_at, updated_at) " +
            "VALUES (#{userId}, #{stats}, #{statYear}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserStatsDO userStats);

    @Update("UPDATE user_stats SET stats = #{stats}, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND stat_year = #{statYear}")
    int updateStats(UserStatsDO userStats);

    @Select("SELECT * FROM user_stats WHERE user_id = #{userId} AND stat_year = #{statYear}")
    UserStatsDO getByUserIdAndYear(Integer userId, Integer statYear);

    @Select("SELECT * FROM user_stats WHERE user_id = #{userId} " +
            "AND stat_year >= #{startYear} ORDER BY stat_year DESC")
    List<UserStatsDO> getStatsInYearRange(Integer userId, Integer startYear);

    // ===== 实时统计操作（增量更新）=====
    
    // 使用MySQL JSON函数直接增加计数（用于实时统计）
    @Update("UPDATE user_stats SET " +
            "stats = JSON_SET(" +
            "  COALESCE(stats, JSON_OBJECT()), " +
            "  CONCAT('$.\"', #{dayKey}, '\"'), " +
            "  JSON_SET(" +
            "    COALESCE(JSON_EXTRACT(stats, CONCAT('$.\"', #{dayKey}, '\"')), JSON_OBJECT('views', 0, 'twice', 0, 'helpful', 0, 'comments', 0)), " +
            "    CONCAT('$.', #{statType}), " +
            "    CAST(COALESCE(JSON_EXTRACT(stats, CONCAT('$.\"', #{dayKey}, '\".', #{statType})), 0) AS SIGNED) + #{count}" +
            "  )" +
            "), " +
            "updated_at = NOW() " +
            "WHERE user_id = #{userId} AND stat_year = #{statYear}")
    int incrementUserStatsCount(Integer userId, Integer statYear, String dayKey, String statType, Integer count);

    // ===== 同步操作（直接覆盖）=====
    
    // 直接设置当天完整统计数据（用于同步）
    @Update("UPDATE user_stats SET " +
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
            "WHERE user_id = #{userId} AND stat_year = #{statYear}")
    int setUserDayStats(Integer userId, Integer statYear, String dayKey, Integer views, Integer twice,
                        Integer helpful, Integer comments);

    // ===== 查询操作 =====
    
    // 获取指定日期的统计数据
    @Select("SELECT JSON_EXTRACT(stats, CONCAT('$.\"', #{dayKey}, '\"')) " +
            "FROM user_stats " +
            "WHERE user_id = #{userId} AND stat_year = #{statYear}")
    String getDayStats(Integer userId, Integer statYear, String dayKey);

}