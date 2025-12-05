package com.prosper.learn.application.dto.response;

import lombok.Builder;
import lombok.Data;

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
     * 是否是新完成的（标记完成时使用）
     */
    private Boolean isNewlyCompleted;
    
    /**
     * 是否被移除（取消完成时使用）
     */
    private Boolean wasRemoved;
    
    /**
     * 课程进度百分比（0-10000）
     */
    private Integer courseProgress;
    
    /**
     * 用户已完成的节点总数
     */
    private Long totalCompletedNodes;
}