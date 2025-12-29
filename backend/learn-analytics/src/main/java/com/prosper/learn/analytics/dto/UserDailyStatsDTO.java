package com.prosper.learn.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户当日统计数据 DTO
 * 用于返回用户今日/昨日的基础统计数据（仅包含浏览、点赞、评论等基础数据）
 * 与 UserStatsDTO 区别：
 * - UserStatsDTO: 完整的用户统计数据（包含学习进度、社交关系、创作内容等）
 * - UserDailyStatsDTO: 仅包含当日基础统计（views, twices, likes, comments）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDailyStatsDTO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 总浏览量
     */
    private Integer views;

    /**
     * 总"两次能懂"点赞数
     */
    private Integer twices;

    /**
     * 总"有用"点赞数
     */
    private Integer likes;

    /**
     * 总评论数
     */
    private Integer comments;

    /**
     * 创建空的统计对象
     */
    public static UserDailyStatsDTO empty() {
        return UserDailyStatsDTO.builder()
                .views(0)
                .twices(0)
                .likes(0)
                .comments(0)
                .build();
    }
}
