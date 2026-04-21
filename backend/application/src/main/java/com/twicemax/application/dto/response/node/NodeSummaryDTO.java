package com.twicemax.application.dto.response.node;

import lombok.Data;

/**
 * 节点摘要 DTO
 *
 * 用途：基础节点信息
 */
@Data
public class NodeSummaryDTO {

    private Long id;

    private String name;

    private String description;

    private Integer commentCount;

    private Integer nodeReferenceCount;

    private Byte state;
}
