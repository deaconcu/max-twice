package com.prosper.learn.application.dto.response.home;

import lombok.Data;

/**
 * 用户学习统计 DTO
 * 用于首页展示用户的学习统计数据
 */
@Data
public class UserLearningStatsDTO {

    /**
     * 连续学习天数
     */
    private Integer learningDays;

    /**
     * 进行中的课程数
     */
    private Integer coursesInProgress;

    /**
     * 进行中的职业路线数
     */
    private Integer careersInProgress;

    public UserLearningStatsDTO() {
        this.learningDays = 0;
        this.coursesInProgress = 0;
        this.careersInProgress = 0;
    }

    public UserLearningStatsDTO(Integer learningDays, Integer coursesInProgress, Integer careersInProgress) {
        this.learningDays = learningDays;
        this.coursesInProgress = coursesInProgress;
        this.careersInProgress = careersInProgress;
    }
}
