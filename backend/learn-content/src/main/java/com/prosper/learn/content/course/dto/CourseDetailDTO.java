package com.prosper.learn.content.course.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课程详情 DTO
 *
 * 用途：完整课程详情信息，包含所有管理和状态字段
 *
 * 使用场景：
 * - 课程详情页面
 * - 课程编辑页面
 * - 管理后台课程审核
 * - 需要完整课程信息的任何场景
 *
 * 替代关系：
 * - 替代原 V4（dto + parentCourse）
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CourseDetailDTO extends CourseSummaryDTO {

    /**
     * 创建者ID
     * 说明：创建该课程的用户ID
     * 何时填充：始终填充，用于权限校验和显示创建者信息
     */
    private Long creatorId;

    /**
     * 根节点ID
     * 说明：课程树的根节点ID，用于构建课程内容树
     * 何时填充：始终填充
     */
    private Long rootNodeId;

    /**
     * 父课程ID
     * 说明：如果是子课程，指向父课程的ID；0 表示是根课程
     * 何时填充：始终填充，用于判断课程层级关系
     */
    private Long parentCourseId;

    /**
     * 父课程信息
     * 说明：如果是子课程，包含父课程的简要信息（仅 id 和 name）
     * 何时填充：当 parentCourseId > 0 时动态查询并填充，否则为 null
     * 类型：使用 CourseBriefDTO 避免返回过多字段
     */
    private CourseBriefDTO parentCourse;

    /**
     * 课程状态
     * 说明：SUBMITTED(0-待审核), PUBLISHED(1-已发布), REJECTED(2-已拒绝), BANNED(3-已封禁)
     * 何时填充：始终填充，用于权限控制和显示逻辑
     */
    private Byte state;

    /**
     * 审核原因
     * 说明：拒绝或封禁的原因
     * 何时填充：仅在课程被拒绝或封禁时填充，其他状态为 null
     */
    private String reason;

    /**
     * 创建时间
     * 说明：课程创建时间，ISO 8601 格式字符串
     * 何时填充：始终填充
     */
    private String createdAt;

    /**
     * 更新时间
     * 说明：课程最后更新时间，ISO 8601 格式字符串
     * 何时填充：始终填充
     */
    private String updatedAt;
}
