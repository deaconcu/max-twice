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
    @Insert("INSERT INTO content_stats (content_type, content_id, view_count, twice_count, like_count, " +
            "comment_count, share_count, bookmark_count, completed_user_count, learner_count, " +
            "post_count, article_count, index_count, roadmap_count, card_deck_count, node_reference_count, reject_count) " +
            "VALUES (#{contentType}, #{contentId}, #{viewCount}, #{twiceCount}, #{likeCount}, " +
            "#{commentCount}, #{shareCount}, #{bookmarkCount}, #{completedUserCount}, #{learnerCount}, " +
            "#{postCount}, #{articleCount}, #{indexCount}, #{roadmapCount}, #{cardDeckCount}, #{nodeReferenceCount}, #{rejectCount})")
    int insert(ContentStatsDO contentStats);

    /**
     * 根据内容类型和ID查询统计记录
     */
    @Select("SELECT * FROM content_stats WHERE content_type = #{contentType} AND content_id = #{contentId}")
    ContentStatsDO getByContent(@Param("contentType") int contentType, @Param("contentId") long contentId);

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
    int atomicIncrement(@Param("contentType") int contentType,
                       @Param("contentId") long contentId,
                       @Param("field") String field,
                       @Param("delta") int delta);

    /**
     * 批量原子增量更新指定字段
     * @param contentType 内容类型
     * @param contentIds 内容ID列表
     * @param field 字段名（如：node_reference_count等）
     * @param delta 增量值（可正可负）
     */
    @Update("<script>" +
            "UPDATE content_stats SET ${field} = GREATEST(0, ${field} + #{delta}), updated_at = NOW() " +
            "WHERE content_type = #{contentType} AND content_id IN " +
            "<foreach collection='contentIds' item='contentId' open='(' close=')' separator=','>" +
            "#{contentId}" +
            "</foreach>" +
            "</script>")
    int batchAtomicIncrement(@Param("contentType") int contentType,
                            @Param("contentIds") List<Long> contentIds,
                            @Param("field") String field,
                            @Param("delta") int delta);

    /**
     * 直接设置指定字段的值（用于重新计算统计）
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @param field 字段名
     * @param value 新值
     */
    @Update("UPDATE content_stats SET ${field} = #{value}, updated_at = NOW() " +
            "WHERE content_type = #{contentType} AND content_id = #{contentId}")
    int setFieldValue(@Param("contentType") int contentType,
                     @Param("contentId") long contentId,
                     @Param("field") String field,
                     @Param("value") int value);

    /**
     * 增量更新多个统计字段并更新同步日期
     * 用于从 Redis 同步数据时防止重复累加
     */
    @Update("UPDATE content_stats SET " +
            "view_count = view_count + #{viewsDelta}, " +
            "twice_count = twice_count + #{twicesDelta}, " +
            "like_count = like_count + #{likesDelta}, " +
            "comment_count = comment_count + #{commentsDelta}, " +
            "last_sync_date = #{syncDate}, " +
            "updated_at = NOW() " +
            "WHERE content_type = #{contentType} AND content_id = #{contentId}")
    int increase(@Param("contentType") int contentType,
                 @Param("contentId") long contentId,
                 @Param("viewsDelta") int viewsDelta,
                 @Param("twicesDelta") int twicesDelta,
                 @Param("likesDelta") int likesDelta,
                 @Param("commentsDelta") int commentsDelta,
                 @Param("syncDate") String syncDate);


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
            "ORDER BY (bookmark_count + learner_count + completed_user_count) DESC " +
            "LIMIT #{limit}")
    List<Long> getTopContentIdsByPopularity(@Param("contentType") int contentType,
                                            @Param("limit") int limit);
}
