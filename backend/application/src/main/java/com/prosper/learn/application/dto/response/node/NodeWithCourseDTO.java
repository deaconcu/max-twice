package com.prosper.learn.application.dto.response.node;

import com.prosper.learn.application.dto.response.course.CourseSummaryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 节点详情（含课程）DTO
 *
 * 用途：包含完整课程对象的节点
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NodeWithCourseDTO extends NodeDetailDTO {

    private CourseSummaryDTO course;

    /**
     * 相似度分数（仅在搜索时返回）
     * 范围：0.0-1.0，1.0表示完全匹配
     */
    private Float similarityScore;
}
