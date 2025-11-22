package com.prosper.learn.dto.response.node;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 节点（含进度）DTO
 *
 * 用途：包含学习进度的节点信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NodeWithProgressDTO extends NodeSummaryDTO {

    private Boolean isCompleted;
}
