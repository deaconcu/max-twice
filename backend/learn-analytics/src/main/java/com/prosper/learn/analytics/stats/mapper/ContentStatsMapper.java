package com.prosper.learn.analytics.stats.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 内容统计数据映射器
 */
@Mapper
public interface ContentStatsMapper {

    // ==================== 基础CRUD操作 ====================

    /**
     * 插入内容统计记录
     */
    @Insert("INSERT INTO content_stats (content_type, content_id, views, twices, likes, " +
            "comments, shares, bookmarks, completed_users, in_progress_users, " +
            "posts, articles, indexes, roadmaps, card_decks, reject_count) " +
            "VALUES (#{contentType}, #{contentId}, #{views}, #{twices}, #{likes}, " +
            "#{comments}, #{shares}, #{bookmarks}, #{completedUsers}, #{inProgressUsers}, " +
            "#{posts}, #{articles}, #{indexes}, #{roadmaps}, #{cardDecks}, #{rejectCount})")
    int insert(ContentStatsDO contentStats);

    /**
     * 根据内容类型和ID查询统计记录
     */
    @Select("SELECT * FROM content_stats WHERE content_type = #{contentType} AND content_id = #{contentId}")
    ContentStatsDO getByContent(@Param("contentType") Integer contentType, @Param("contentId") Long contentId);

    // ==================== 原子增量更新操作 ====================

    /**
     * 原子增量更新指定字段
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @param field 字段名（如：views, likes等）
     * @param delta 增量值（可正可负）
     */
    @Update("UPDATE content_stats SET ${field} = GREATEST(0, ${field} + #{delta}), updated_at = NOW() " +
            "WHERE content_type = #{contentType} AND content_id = #{contentId}")
    int atomicIncrement(@Param("contentType") Integer contentType,
                       @Param("contentId") Long contentId,
                       @Param("field") String field,
                       @Param("delta") int delta);

    /**
     * 增量更新多个统计字段
     */
    @Update("UPDATE content_stats SET " +
            "views = views + #{viewsDelta}, " +
            "twices = twices + #{twicesDelta}, " +
            "likes = likes + #{likesDelta}, " +
            "comments = comments + #{commentsDelta}, " +
            "updated_at = NOW() " +
            "WHERE content_type = #{contentType} AND content_id = #{contentId}")
    int increase(@Param("contentType") Integer contentType,
                 @Param("contentId") Long contentId,
                 @Param("viewsDelta") int viewsDelta,
                 @Param("twicesDelta") int twicesDelta,
                 @Param("likesDelta") int likesDelta,
                 @Param("commentsDelta") int commentsDelta);


    // ==================== 批量查询 ====================

    /**
     * 根据内容ID列表批量查询统计
     */
    @Select("<script>" +
            "SELECT * FROM content_stats " +
            "WHERE content_type = #{contentType} AND content_id IN " +
            "<foreach collection='contentIds' item='contentId' open='(' close=')' separator=','>" +
            "#{contentId}" +
            "</foreach>" +
            "</script>")
    List<ContentStatsDO> batchGetByContentIds(@Param("contentType") String contentType,
                                             @Param("contentIds") List<Long> contentIds);

    /**
     * 获取热门课程ID列表（按综合热度排序）
     * 综合热度 = 收藏数 + 学习中人数 + 已完成人数
     */
    @Select("SELECT content_id " +
            "FROM content_stats " +
            "WHERE content_type = #{contentType} " +
            "ORDER BY (bookmarks + in_progress_users + completed_users) DESC " +
            "LIMIT #{limit}")
    List<Long> getTopContentIdsByPopularity(@Param("contentType") Integer contentType,
                                            @Param("limit") int limit);
}
