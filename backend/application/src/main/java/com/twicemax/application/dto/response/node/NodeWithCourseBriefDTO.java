package com.twicemax.application.dto.response.node;

import com.twicemax.application.dto.response.course.CourseBriefDTO;
import lombok.Data;

/**
 * 节点（含课程简要信息）DTO
 *
 * 用途：节点基本信息 + 课程简要信息（id + name）
 *
 * 使用场景：
 * - 帖子详情中的节点引用（PostDetailDTO.node）
 * - 任何需要显示节点名称和所属课程的场景
 *
 * 字段说明：
 * - 包含节点 id 和 name
 * - 包含课程的 CourseBriefDTO（仅 id + name）
 * - 不包含 description、state、children 等管理字段
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
public class NodeWithCourseBriefDTO {

    /**
     * 节点ID
     */
    private Long id;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 课程简要信息
     * 说明：包含课程的 id 和 name，用于显示节点所属课程
     */
    private CourseBriefDTO course;
}
