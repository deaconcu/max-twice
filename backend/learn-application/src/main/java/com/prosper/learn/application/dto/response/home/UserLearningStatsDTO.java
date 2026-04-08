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
     * 进行中的角色路线数
     */
    private Integer rolesInProgress;

    public UserLearningStatsDTO() {
        this.learningDays = 0;
        this.coursesInProgress = 0;
        this.rolesInProgress = 0;
    }

    public UserLearningStatsDTO(Integer learningDays, Integer coursesInProgress, Integer rolesInProgress) {
        this.learningDays = learningDays;
        this.coursesInProgress = coursesInProgress;
        this.rolesInProgress = rolesInProgress;
    }
}
