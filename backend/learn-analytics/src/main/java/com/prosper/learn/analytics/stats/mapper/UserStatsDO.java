package com.prosper.learn.analytics.stats.mapper;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserStatsDO {

    private Long id;

    private Long userId;

    // 累计统计（总计数）
    private Integer views;          // 总浏览量
    private Integer twices;         // 总两次能懂点赞数
    private Integer likes;          // 总有用点赞数
    private Integer comments;       // 总评论数

    // 学习统计（累计快照）
    private Integer learningCourses;
    private Integer completedCourses;
    private Integer inProgressProfessions;
    private Integer completedProfessions;

    // 社交统计（累计快照）
    private Integer followingUsers;
    private Integer followingCourses;
    private Integer followingProfessions;

    // 创作统计（累计快照）
    private Integer createdArticles;
    private Integer createdIndexs;
    private Integer createdRoadmaps;
    private Integer createdCardDecks;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}