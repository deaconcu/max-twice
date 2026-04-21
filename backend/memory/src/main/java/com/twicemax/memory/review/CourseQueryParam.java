package com.twicemax.memory.review;

import lombok.Data;

/**
 * 课程统计查询参数
 */
@Data
public class CourseQueryParam {
    private Long courseId;
    private Integer newLimit;      // 新卡查询上限
    private Integer reviewLimit;   // 复习查询上限

    public CourseQueryParam(Long courseId, Integer newLimit, Integer reviewLimit) {
        this.courseId = courseId;
        this.newLimit = newLimit;
        this.reviewLimit = reviewLimit;
    }
}
