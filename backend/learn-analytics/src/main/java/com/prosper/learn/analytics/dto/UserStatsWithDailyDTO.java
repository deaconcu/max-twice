package com.prosper.learn.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户统计数据（包含总计和每日明细）
 * 用于历史统计和时间段统计接口
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsWithDailyDTO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 总浏览量（累计）
     */
    private Integer views;

    /**
     * 总"两次能懂"点赞数（累计）
     */
    private Integer twices;

    /**
     * 总"有用"点赞数（累计）
     */
    private Integer likes;

    /**
     * 总评论数（累计）
     */
    private Integer comments;

    /**
     * 每日明细列表
     */
    private List<DailyStatsDTO> dailyStats;

    /**
     * 创建空的统计对象
     */
    public static UserStatsWithDailyDTO empty() {
        return UserStatsWithDailyDTO.builder()
                .views(0)
                .twices(0)
                .likes(0)
                .comments(0)
                .dailyStats(List.of())
                .build();
    }
}
