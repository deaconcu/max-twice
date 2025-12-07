package com.prosper.learn.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDTO {

    // 用户基础信息
    private Long userId;

    // 累计统计字段（总计数）
    private Integer views;          // 总浏览量
    private Integer twices;         // 总两次能懂点赞数
    private Integer likes;          // 总有用点赞数
    private Integer comments;       // 总评论数

    // 学习进度统计
    private Integer learningCourses;
    private Integer completedCourses;
    private Integer inProgressProfessions;
    private Integer completedProfessions;

    // 社交关系统计
    private Integer followingUsers;
    private Integer followingCourses;
    private Integer followingProfessions;

    // 创作内容统计
    private Integer createdArticles;
    private Integer createdIndexs;
    private Integer createdRoadmaps;
    private Integer createdCardDecks;

    // 汇总信息
    private Integer totalLearningItems;
    private Integer totalCreatedItems;
    private LocalDateTime lastUpdated;

    public static UserStatsDTO empty() {
        return UserStatsDTO.builder()
            .views(0)
            .twices(0)
            .likes(0)
            .comments(0)
            .learningCourses(0)
            .completedCourses(0)
            .inProgressProfessions(0)
            .completedProfessions(0)
            .followingUsers(0)
            .followingCourses(0)
            .followingProfessions(0)
            .createdArticles(0)
            .createdIndexs(0)
            .createdRoadmaps(0)
            .createdCardDecks(0)
            .totalLearningItems(0)
            .totalCreatedItems(0)
            .build();
    }

    // 计算汇总统计
    public void calculateTotals() {
        this.totalLearningItems = (learningCourses != null ? learningCourses : 0) +
                                 (completedCourses != null ? completedCourses : 0) +
                                 (inProgressProfessions != null ? inProgressProfessions : 0) +
                                 (completedProfessions != null ? completedProfessions : 0);

        this.totalCreatedItems = (createdArticles != null ? createdArticles : 0) +
                                (createdIndexs != null ? createdIndexs : 0) +
                                (createdRoadmaps != null ? createdRoadmaps : 0) +
                                (createdCardDecks != null ? createdCardDecks : 0);
    }
}