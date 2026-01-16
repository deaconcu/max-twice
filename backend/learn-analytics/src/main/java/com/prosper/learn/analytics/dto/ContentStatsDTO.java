package com.prosper.learn.analytics.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 内容统计响应DTO
 *
 * 用于返回内容的完整统计信息
 * 由 ContentStatsService 负责提供数据
 */
@Data
@Builder
public class ContentStatsDTO {

    /**
     * 内容ID
     */
    private Long contentId;

    // ==================== 核心统计字段 ====================

    /**
     * 总浏览量
     */
    private Integer viewCount;

    /**
     * twice类型点赞数
     */
    private Integer twiceCount;

    /**
     * like类型点赞数
     */
    private Integer likeCount;

    /**
     * 总评论数
     */
    private Integer commentCount;

    /**
     * 总分享数
     */
    private Integer shareCount;

    /**
     * 总收藏数
     */
    private Integer bookmarkCount;

    /**
     * 总完成人数
     * 主要用于课程、路线图等学习内容
     */
    private Integer completedUserCount;

    /**
     * 正在学习人数
     * 主要用于课程、路线图等学习内容
     */
    private Integer inProgressUserCount;
}