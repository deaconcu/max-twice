package com.prosper.learn.analytics.stats.mapper;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserStatsDO {

    // 主键：user_id
    private Long userId;

    // 累计统计（总计数）
    private Integer viewCount;          // 总浏览量
    private Integer twiceCount;         // 总两次能懂点赞数
    private Integer likeCount;          // 总有用点赞数
    private Integer commentCount;       // 总评论数

    // 学习统计（累计快照）
    private Integer learningCourseCount;
    private Integer completedCourseCount;
    private Integer inProgressRoleCount;
    private Integer completedRoleCount;

    // 社交统计（累计快照）
    private Integer followingUserCount;
    private Integer followingCourseCount;
    private Integer followingRoleCount;

    // 创作统计（累计快照）
    private Integer createdArticleCount;
    private Integer createdIndexCount;
    private Integer createdRoadmapCount;
    private Integer createdCardDeckCount;

    // 复习统计（记忆卡片）
    private Integer reviewStreakDays;       // 记忆卡片连续复习天数
    private LocalDate lastCardReviewDate;   // 记忆卡片最后复习日期

    // 学习统计（阅读文章）
    private Integer learningStreakDays;     // 连续学习天数
    private LocalDate lastLearningDate;     // 最后学习日期

    /**
     * 最后同步日期
     * 记录最后一次从 Redis 同步数据的日期（格式：YYYY-MM-DD）
     * 用于防止重复同步导致数据累加错误
     */
    private String lastSyncDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}