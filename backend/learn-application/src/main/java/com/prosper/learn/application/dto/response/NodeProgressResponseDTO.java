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
}