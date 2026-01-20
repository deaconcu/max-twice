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
     * 内容类型（主键字段1）
     * 对应 Enums.ContentType 枚举值
     * 1=post, 2=node, 3=comment, 4=roadmap, 5=memory_card_deck, 6=memory_card, 7=profession, 8=course
     */
    private Integer contentType;

    /**
     * 内容ID（主键字段2）
     * 对应各个内容表的主键ID
     */
    private Long contentId;

    // ==================== 核心统计字段 ====================

    /**
     * 总浏览量
     * 记录该内容被访问查看的总次数
     */
    private Integer viewCount;

    /**
     * 总两次能懂数
     * 记录该内容获得的"两次能懂"点赞总数
     */
    private Integer twiceCount;

    /**
     * 总点赞数
     * 记录该内容获得的普通点赞总数
     */
    private Integer likeCount;

    /**
     * 总评论数
     * 记录该内容下的评论总数
     */
    private Integer commentCount;

    /**
     * 总分享数
     * 记录该内容被分享的总次数
     */
    private Integer shareCount;

    /**
     * 总收藏数
     * 记录该内容被收藏的总次数
     */
    private Integer bookmarkCount;

    /**
     * 总完成人数
     * 主要用于课程、路线图等学习内容，记录完成学习的用户总数
     */
    private Integer completedUserCount;

    /**
     * 正在学习人数
     * 主要用于课程、路线图等学习内容，记录当前正在学习的用户数
     */
    private Integer learnerCount;

    // ==================== 对象维度统计字段 ====================

    /**
     * 帖子总数
     * 用于 Node 统计其下的帖子总数（articles + indexes）
     */
    private Integer postCount;

    /**
     * 文章数量
     * 用于 Node 统计其下的文章类型帖子数量
     */
    private Integer articleCount;

    /**
     * 目录数量
     * 用于 Node 统计其下的目录类型帖子数量
     */
    private Integer indexCount;

    /**
     * 节点被引用次数
     * 用于 Node 统计被多少个 contents 类型的 post 引用
     */
    private Integer nodeReferenceCount;

    /**
     * 路线图数量
     * 用于 Profession 统计其下的路线图数量
     */
    private Integer roadmapCount;

    /**
     * 记忆卡片组数量
     * 用于 Post/Node 统计其下的卡片组数量
     */
    private Integer cardDeckCount;

    // ==================== 违规统计字段 ====================

    /**
     * 被拒绝/下架次数
     * 用于判断是否需要自动升级为 BANNED 状态（≥3次）
     */
    private Integer rejectCount;

    // ==================== 时间戳字段 ====================

    /**
     * 最后同步日期
     * 记录最后一次从 Redis 同步数据的日期（格式：YYYY-MM-DD）
     * 用于防止重复同步导致数据累加错误
     */
    private String lastSyncDate;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;
}