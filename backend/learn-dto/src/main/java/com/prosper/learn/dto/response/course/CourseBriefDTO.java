package com.prosper.learn.dto.response.course;

import lombok.Data;

/**
 * 课程简要 DTO
 *
 * 用途：极简课程信息，仅包含 ID 和名称
 *
 * 使用场景：
 * - 课程搜索结果列表
 * - 作为父课程引用（嵌套在 CourseDetailDTO 中）
 * - 任何只需要显示课程名称的场景
 *
 * 替代关系：
 * - 替代原 V3（id + name）
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
public class CourseBriefDTO {

    /**
     * 课程ID
     * 说明：课程的唯一标识
     */
    private Long id;

    /**
     * 课程名称
     * 说明：课程的显示名称
     */
    private String name;
}
