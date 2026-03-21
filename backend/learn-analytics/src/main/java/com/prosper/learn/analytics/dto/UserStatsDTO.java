package com.prosper.learn.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDTO {

    // 用户基础信息
    private Long userId;

    // 累计统计字段（总计数）
    private Integer viewCount;          // 总浏览量
    private Integer twiceCount;         // 总两次能懂点赞数
    private Integer likeCount;          // 总有用点赞数
    private Integer commentCount;       // 总评论数

    // 学习进度统计
    private Integer learningCourseCount;
    private Integer completedCourseCount;
    private Integer inProgressProfessionCount;
    private Integer completedProfessionCount;

    // 社交关系统计
    private Integer followingUserCount;
    private Integer followingCourseCount;
    private Integer followingProfessionCount;

    // 创作内容统计
    private Integer createdArticleCount;
    private Integer createdIndexCount;
    private Integer createdRoadmapCount;
    private Integer createdCardDeckCount;

    // 连续天数统计
    private Integer learningStreakDays;     // 连续学习天数
    private Integer reviewStreakDays;       // 连续复习天数

    private String updatedAt;

    public static UserStatsDTO empty() {
        return UserStatsDTO.builder()
            .viewCount(0)
            .twiceCount(0)
            .likeCount(0)
            .commentCount(0)
            .learningCourseCount(0)
            .completedCourseCount(0)
            .inProgressProfessionCount(0)
            .completedProfessionCount(0)
            .followingUserCount(0)
            .followingCourseCount(0)
            .followingProfessionCount(0)
            .createdArticleCount(0)
            .createdIndexCount(0)
            .createdRoadmapCount(0)
            .createdCardDeckCount(0)
            .learningStreakDays(0)
            .reviewStreakDays(0)
            .build();
    }
}