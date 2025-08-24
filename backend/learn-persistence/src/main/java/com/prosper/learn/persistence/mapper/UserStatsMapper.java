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
    UserStatsDO getByUserIdAndYear(@Param("userId") Integer userId, @Param("statYear") Integer statYear);

    @Select("SELECT * FROM user_stats WHERE user_id = #{userId} " +
            "AND stat_year >= #{startYear} ORDER BY stat_year DESC")
    List<UserStatsDO> getStatsInYearRange(@Param("userId") Integer userId,
                                          @Param("startYear") Integer startYear);

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
    int incrementUserStatsCount(@Param("userId") Integer userId,
                               @Param("statYear") Integer statYear,
                               @Param("dayKey") String dayKey,
                               @Param("statType") String statType,
                               @Param("count") Integer count);

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
    int setUserDayStats(@Param("userId") Integer userId,
                       @Param("statYear") Integer statYear,
                       @Param("dayKey") String dayKey,
                       @Param("views") Integer views,
                       @Param("twice") Integer twice,
                       @Param("helpful") Integer helpful,
                       @Param("comments") Integer comments);

    // ===== 查询操作 =====
    
    // 获取指定日期的统计数据
    @Select("SELECT JSON_EXTRACT(stats, CONCAT('$.\"', #{dayKey}, '\"')) " +
            "FROM user_stats " +
            "WHERE user_id = #{userId} AND stat_year = #{statYear}")
    String getDayStats(@Param("userId") Integer userId,
                      @Param("statYear") Integer statYear,
                      @Param("dayKey") String dayKey);

    // ===== 管理方法 =====
    
    @Delete("DELETE FROM user_stats WHERE stat_year < #{beforeYear}")
    int deleteOldStats(@Param("beforeYear") Integer beforeYear);

    @Select("SELECT DISTINCT user_id FROM user_stats WHERE stat_year = #{statYear}")
    List<Integer> getUserIdsByYear(@Param("statYear") Integer statYear);

    @Update("UPDATE user_stats SET " +
            "stats = COALESCE(stats, JSON_OBJECT()) " +
            "WHERE user_id = #{userId} AND stat_year = #{statYear} AND stats IS NULL")
    int initializeStatsIfNull(@Param("userId") Integer userId, @Param("statYear") Integer statYear);
}