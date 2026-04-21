package com.twicemax.application.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 复习概览 DTO
 * 用于复习页面和首页展示复习统计
 */
@Data
public class ReviewSummaryDTO {

    /**
     * 今日待复习总数
     */
    private Integer todayTotal;

    /**
     * 今日已复习数
     */
    private Integer todayCompleted;

    /**
     * 连续复习天数
     */
    private Integer streakDays;

    /**
     * 复习课程列表
     */
    private List<CourseMemoryBankDTO> courses;

    public ReviewSummaryDTO() {
        this.todayTotal = 0;
        this.todayCompleted = 0;
        this.streakDays = 0;
    }
}
