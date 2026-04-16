package com.prosper.learn.application.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 节点进度响应DTO
 */
@Data
@Builder
public class NodeProgressResponseDTO {

    /**
     * 节点ID
     */
    private Long nodeId;

    /**
     * 是否已完成
     */
    private Boolean completed;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 课程进度（万分位：0-10000）
     */
    private Integer courseProgressPercent;

    /**
     * 可完成的节点ID列表
     * 这些节点的递归完成度达到100%但节点本身未完成，可以提示用户标记完成
     */
    private List<Long> completableNodeIds;
}
