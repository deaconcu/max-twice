package com.prosper.learn.application.dto.response.home;

import com.prosper.learn.application.dto.response.PlatformStatsDTO;
import com.prosper.learn.application.dto.response.ReviewSummaryDTO;
import com.prosper.learn.application.dto.response.course.CourseFullDTO;
import com.prosper.learn.application.dto.response.profession.ProfessionDTO;
import com.prosper.learn.application.dto.response.userlearning.UserLearningDTO;
import lombok.Data;

import java.util.List;

/**
 * 首页聚合数据 DTO
 * 一次性返回首页所需的所有数据
 */
@Data
public class HomePageDTO {

    /**
     * 平台统计数据
     */
    private PlatformStatsDTO platformStats;

    /**
     * 用户学习统计
     */
    private UserLearningStatsDTO userStats;

    /**
     * 正在学习的职业路线（带进度）
     */
    private List<UserLearningDTO<Object>> learningCareers;

    /**
     * 正在学习的课程（带进度）
     */
    private List<UserLearningDTO<Object>> learningCourses;

    /**
     * 复习概览
     */
    private ReviewSummaryDTO reviewSummary;

    /**
     * 推荐职业列表
     */
    private List<ProfessionDTO> recommendedCareers;

    /**
     * 推荐课程列表
     */
    private List<CourseFullDTO> recommendedCourses;
}
