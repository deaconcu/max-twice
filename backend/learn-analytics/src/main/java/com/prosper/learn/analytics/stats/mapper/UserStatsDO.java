package com.prosper.learn.analytics.stats.mapper;

import lombok.Data;

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
    private Integer inProgressProfessionCount;
    private Integer completedProfessionCount;

    // 社交统计（累计快照）
    private Integer followingUserCount;
    private Integer followingCourseCount;
    private Integer followingProfessionCount;

    // 创作统计（累计快照）
    private Integer createdArticleCount;
    private Integer createdIndexCount;
    private Integer createdRoadmapCount;
    private Integer createdCardDeckCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}