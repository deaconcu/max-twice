package com.prosper.learn.dto.response;

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

    // ==================== 核心统计字段 ====================

    /**
     * 总浏览量
     */
    private Integer views;

    /**
     * twice类型点赞数
     */
    private Integer twiceUpvotes;

    /**
     * like类型点赞数
     */
    private Integer likeUpvotes;

    /**
     * 总评论数
     */
    private Integer comments;

    /**
     * 总分享数
     */
    private Integer shares;

    /**
     * 总收藏数
     */
    private Integer bookmarks;

    /**
     * 总完成人数
     * 主要用于课程、路线图等学习内容
     */
    private Integer completedUsers;

    /**
     * 正在学习人数
     * 主要用于课程、路线图等学习内容
     */
    private Integer inProgressUsers;
}