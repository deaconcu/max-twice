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
    @Insert("INSERT INTO content_stats (content_type, content_id, total_views, total_twice, total_likes, " +
            "total_comments, total_shares, total_bookmarks, total_completed_users, total_in_progress_users) " +
            "VALUES (#{contentType}, #{contentId}, #{totalViews}, #{totalTwice}, #{totalLikes}, " +
            "#{totalComments}, #{totalShares}, #{totalBookmarks}, #{totalCompletedUsers}, #{totalInProgressUsers})")
    int insert(ContentStatsDO contentStats);

    /**
     * 根据内容类型和ID查询统计记录
     */
    @Select("SELECT * FROM content_stats WHERE content_type = #{contentType} AND content_id = #{contentId}")
    ContentStatsDO getByContent(@Param("contentType") Integer contentType, @Param("contentId") Long contentId);

    /**
     * 根据主键ID删除记录
     */
    @Delete("DELETE FROM content_stats WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 根据内容类型和ID删除记录
     */
    @Delete("DELETE FROM content_stats WHERE content_type = #{contentType} AND content_id = #{contentId}")
    int deleteByContent(@Param("contentType") Integer contentType, @Param("contentId") Long contentId);

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

    // ==================== 排行榜查询 ====================

    /**
     * 根据指定字段获取热门内容排行榜
     * @param contentType 内容类型
     * @param orderField 排序字段（如：views, likes等）
     * @param limit 返回记录数限制
     */
    @Select("SELECT * FROM content_stats WHERE content_type = #{contentType} " +
            "ORDER BY ${orderField} DESC LIMIT #{limit}")
    List<ContentStatsDO> getTopByField(@Param("contentType") Integer contentType,
                                      @Param("orderField") String orderField,
                                      @Param("limit") int limit);

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
     * 获取指定内容类型的所有统计记录（分页）
     */
    @Select("SELECT * FROM content_stats WHERE content_type = #{contentType} " +
            "ORDER BY updated_at DESC LIMIT #{offset}, #{limit}")
    List<ContentStatsDO> getByContentTypeWithPaging(@Param("contentType") String contentType,
                                                    @Param("offset") int offset,
                                                    @Param("limit") int limit);

    /**
     * 统计指定内容类型的记录总数
     */
    @Select("SELECT COUNT(*) FROM content_stats WHERE content_type = #{contentType}")
    int countByContentType(@Param("contentType") String contentType);

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

    // ==================== 数据维护 ====================

    /**
     * 清理统计值全为0的记录
     */
    @Delete("DELETE FROM content_stats WHERE total_views = 0 AND total_twice = 0 AND total_likes = 0 " +
            "AND total_comments = 0 AND total_shares = 0 AND total_bookmarks = 0 " +
            "AND total_completed_users = 0 AND total_in_progress_users = 0")
    int cleanupEmptyStats();
}