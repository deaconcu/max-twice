package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.PostStatsDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PostStatsMapper {

    @Insert("INSERT INTO post_stats (type, object_id, stats, stat_year, created_at, updated_at) " +
            "VALUES (#{type}, #{objectId}, #{stats}, #{statYear}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PostStatsDO stats);

    @Update("UPDATE post_stats SET stats = #{stats}, updated_at = NOW() " +
            "WHERE type = #{type} AND object_id = #{objectId} AND stat_year = #{statYear}")
    int updateStats(PostStatsDO stats);

    @Select("SELECT * FROM post_stats WHERE type = #{type} AND object_id = #{objectId} AND stat_year = #{statYear}")
    PostStatsDO getByTypeAndObjectIdAndYear(@Param("type") String type,
                                            @Param("objectId") Long objectId,
                                            @Param("statYear") Integer statYear);

    @Select("SELECT * FROM post_stats WHERE type = #{type} AND object_id = #{objectId} " +
            "AND stat_year >= #{startYear} ORDER BY stat_year DESC")
    List<PostStatsDO> getStatsInYearRange(@Param("type") String type,
                                          @Param("objectId") Long objectId,
                                          @Param("startYear") Integer startYear);

    @Select("SELECT DISTINCT object_id FROM post_stats WHERE type = #{type}")
    List<Long> getAllObjectIdsByType(@Param("type") String type);

    // 使用MySQL JSON函数直接增加计数
    @Update("UPDATE post_stats SET " +
            "stats = JSON_SET(" +
            "  COALESCE(stats, JSON_OBJECT()), " +
            "  CONCAT('$.\"', #{dayKey}, '\"'), " +
            "  JSON_SET(" +
            "    COALESCE(JSON_EXTRACT(stats, CONCAT('$.\"', #{dayKey}, '\"')), JSON_OBJECT('once', 0, 'twice', 0, 'helpful', 0)), " +
            "    CONCAT('$.', #{upvoteType}), " +
            "    CAST(COALESCE(JSON_EXTRACT(stats, CONCAT('$.\"', #{dayKey}, '\".', #{upvoteType})), 0) AS SIGNED) + 1" +
            "  )" +
            "), " +
            "updated_at = NOW() " +
            "WHERE type = #{type} AND object_id = #{objectId} AND stat_year = #{statYear}")
    int incrementUpvoteCount(@Param("type") String type,
                            @Param("objectId") Long objectId,
                            @Param("statYear") Integer statYear,
                            @Param("dayKey") String dayKey,
                            @Param("upvoteType") String upvoteType);

    // 使用MySQL JSON函数直接减少计数
    @Update("UPDATE post_stats SET " +
            "stats = JSON_SET(" +
            "  stats, " +
            "  CONCAT('$.\"', #{dayKey}, '\".', #{upvoteType}), " +
            "  GREATEST(0, CAST(COALESCE(JSON_EXTRACT(stats, CONCAT('$.\"', #{dayKey}, '\".', #{upvoteType})), 0) AS SIGNED) - 1)" +
            "), " +
            "updated_at = NOW() " +
            "WHERE type = #{type} AND object_id = #{objectId} AND stat_year = #{statYear} " +
            "AND JSON_EXTRACT(stats, CONCAT('$.\"', #{dayKey}, '\"')) IS NOT NULL")
    int decrementUpvoteCount(@Param("type") String type,
                            @Param("objectId") Long objectId,
                            @Param("statYear") Integer statYear,
                            @Param("dayKey") String dayKey,
                            @Param("upvoteType") String upvoteType);

    // 获取指定日期的统计数据
    @Select("SELECT JSON_EXTRACT(stats, CONCAT('$.\"', #{dayKey}, '\"')) " +
            "FROM upvote_stats " +
            "WHERE type = #{type} AND object_id = #{objectId} AND stat_year = #{statYear}")
    String getDayStats(@Param("type") String type,
                      @Param("objectId") Long objectId,
                      @Param("statYear") Integer statYear,
                      @Param("dayKey") String dayKey);
}
