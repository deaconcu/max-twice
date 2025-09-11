package com.prosper.learn.dto.response;

import lombok.Data;

/**
 * 复习统计信息响应DTO
 */
@Data
public class ReviewStatsDTO {

    private Integer totalReviews;

    private Integer streakDays;

    private Double averageScore;

    private Integer timeSpent;

}