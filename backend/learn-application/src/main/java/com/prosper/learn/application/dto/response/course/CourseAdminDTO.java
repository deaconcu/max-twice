package com.prosper.learn.application.dto.response.course;

import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import lombok.Data;

/**
 * 课程管理 DTO
 *
 * 用途：管理后台使用的课程信息
 *
 * 使用场景：
 * - 管理后台课程审核列表
 * - 需要显示拒绝/封禁原因的场景
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
public class CourseAdminDTO {

    private Long id;

    private String name;

    private String description;

    private Integer mainCategory;

    private Integer subCategory;

    private Long creatorId;

    private UserBriefDTO creator;

    private Long rootNodeId;

    private Long parentCourseId;

    private CourseBriefDTO parentCourse;

    private Byte state;

    /**
     * 拒绝/封禁原因
     */
    private String reason;

    private String createdAt;

    private String updatedAt;
}
