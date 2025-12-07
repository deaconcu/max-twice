package com.prosper.learn.analytics.stats.mapper;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 内容统计数据对象
 * 用于存储各种内容类型（文章、路线图、课程、评论等）的统计数据
 */
@Data
public class ContentStatsDO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 内容类型
     * 对应 Enums.ContentType 枚举值
     * 1=post, 2=node, 3=comment, 4=roadmap, 5=memory_card_deck, 6=memory_card, 7=profession, 8=course
     */
    private Integer contentType;

    /**
     * 内容ID
     * 对应各个内容表的主键ID
     */
    private Long contentId;

    // ==================== 核心统计字段 ====================

    /**
     * 总浏览量
     * 记录该内容被访问查看的总次数
     */
    private Integer views;

    /**
     * 总两次能懂数
     * 记录该内容获得的"两次能懂"点赞总数
     */
    private Integer twices;

    /**
     * 总点赞数
     * 记录该内容获得的普通点赞总数
     */
    private Integer likes;

    /**
     * 总评论数
     * 记录该内容下的评论总数
     */
    private Integer comments;

    /**
     * 总分享数
     * 记录该内容被分享的总次数
     */
    private Integer shares;

    /**
     * 总收藏数
     * 记录该内容被收藏的总次数
     */
    private Integer bookmarks;

    /**
     * 总完成人数
     * 主要用于课程、路线图等学习内容，记录完成学习的用户总数
     */
    private Integer completedUsers;

    /**
     * 正在学习人数
     * 主要用于课程、路线图等学习内容，记录当前正在学习的用户数
     */
    private Integer inProgressUsers;

    // ==================== 时间戳字段 ====================

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;
}