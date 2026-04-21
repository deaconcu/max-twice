package com.twicemax.analytics.dto;

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
    private Integer viewCount;

    /**
     * 总"两次能懂"点赞数
     */
    private Integer twiceCount;

    /**
     * 总"有用"点赞数
     */
    private Integer likeCount;

    /**
     * 总评论数
     */
    private Integer commentCount;

    /**
     * 创建空的统计对象
     */
    public static UserDailyStatsDTO empty() {
        return UserDailyStatsDTO.builder()
                .viewCount(0)
                .twiceCount(0)
                .likeCount(0)
                .commentCount(0)
                .build();
    }
}
