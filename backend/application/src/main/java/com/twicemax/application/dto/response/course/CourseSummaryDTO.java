package com.twicemax.application.dto.response.course;

import lombok.Data;

/**
 * 课程摘要 DTO
 *
 * 用途：课程列表信息，包含基础描述和分类
 *
 * 使用场景：
 * - 子课程列表展示
 * - 课程分类浏览列表
 * - 任何需要显示课程基本信息的列表
 *
 * 替代关系：
 * - 替代原 V2（id + name + description + mainCategory + subCategory）
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
public class CourseSummaryDTO {

    /**
     * 课程ID
     * 说明：课程的唯一标识
     */
    private Long id;

    /**
     * 根节点ID
     * 说明：课程的根节点ID，用于 Roadmap 等需要使用 nodeId 的场景
     */
    private Long rootNodeId;

    /**
     * 课程名称
     * 说明：课程的显示名称
     */
    private String name;

    /**
     * 课程描述
     * 说明：课程的详细介绍，可能包含 Markdown 格式
     */
    private String description;

    /**
     * 主分类
     * 说明：课程的主要分类（如：编程、设计、商业等）
     * 何时填充：始终填充，用于分类筛选
     */
    private Integer mainCategory;

    /**
     * 子分类
     * 说明：课程的子分类（如：Java、Python、前端等）
     * 何时填充：始终填充，用于精细化分类筛选
     */
    private Integer subCategory;

    /**
     * 课程图标
     * 说明：可以是 MDI 图标名（如 mdi-book）或图片 URL
     * 何时填充：从 course 表的 icon 字段读取
     */
    private String icon;

    /**
     * 父课程信息
     * 说明：如果是子课程，包含父课程的简要信息（仅 id + name + icon）
     * 何时填充：当 parentCourseId > 0 时动态查询并填充，否则为 null
     */
    private CourseBriefDTO parentCourse;

    /**
     * 子课程数量
     * 说明：该课程拥有的已发布子课程数量
     * 何时填充：从数据库读取
     */
    private Integer subCourseCount;
}
