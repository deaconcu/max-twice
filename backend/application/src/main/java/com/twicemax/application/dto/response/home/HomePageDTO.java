package com.twicemax.application.dto.response.home;

import com.twicemax.application.dto.response.PlatformStatsDTO;
import com.twicemax.application.dto.response.ReviewSummaryDTO;
import com.twicemax.application.dto.response.course.CourseBriefDTO;
import com.twicemax.application.dto.response.course.CourseFullDTO;
import com.twicemax.application.dto.response.role.RoleBriefDTO;
import com.twicemax.application.dto.response.role.RoleDTO;
import com.twicemax.application.dto.response.roadmap.RoadmapSummaryDTO;
import com.twicemax.application.dto.response.userlearning.UserLearningDTO;
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
     * 收藏的角色列表（最新收藏的在前，最多10个）
     */
    private List<RoleDTO> bookmarkedRoles;

    /**
     * 正在学习的角色路线（带进度）
     */
    private List<UserLearningDTO<Object>> learningRoles;

    /**
     * 正在学习的课程（带进度）
     */
    private List<UserLearningDTO<Object>> learningCourses;

    /**
     * 复习概览
     */
    private ReviewSummaryDTO reviewSummary;

    /**
     * 热门角色榜单
     */
    private List<RoleDTO> hotRoles;

    /**
     * 热门课程榜单
     */
    private List<CourseFullDTO> hotCourses;

    /**
     * 新手推荐角色（从系统配置读取）
     */
    private List<RoleBriefDTO> beginnerRoles;

    /**
     * 新手推荐路线图（从系统配置读取）
     */
    private List<RoadmapSummaryDTO> beginnerRoadmaps;

    /**
     * 新手推荐课程（从系统配置读取）
     */
    private List<CourseBriefDTO> beginnerCourses;
}
