package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserStatsDO {

    private Long id;

    private Long userId;

    private LocalDate dailyStatDate;

    // 日度增量统计
    private Integer dailyViews;
    private Integer dailyTwice;
    private Integer dailyHelpful;
    private Integer dailyComments;

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