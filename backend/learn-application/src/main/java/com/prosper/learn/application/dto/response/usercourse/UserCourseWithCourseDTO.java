package com.prosper.learn.application.dto.response.usercourse;

import com.prosper.learn.application.dto.response.course.CourseSummaryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户课程（含课程信息）DTO
 *
 * 用途：包含课程详细信息的学习记录
 * 使用场景：个人学习中心、课程详情页等需要展示课程信息的场景
 *
 * 替代：原 UserCourseDTO V1
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserCourseWithCourseDTO extends UserCourseSummaryDTO {

    /**
     * 课程摘要信息
     * 说明：包含课程的基本信息（id, name, description, 分类等）
     */
    private CourseSummaryDTO course;
}
