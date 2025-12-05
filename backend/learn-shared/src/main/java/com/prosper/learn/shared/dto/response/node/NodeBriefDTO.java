package com.prosper.learn.shared.dto.response.node;

import lombok.Data;

/**
 * 节点简要 DTO
 *
 * 用途：极简节点信息，仅包含 ID 和名称
 *
 * 使用场景：
 * - 作为节点引用（嵌套在其他 DTO 中）
 * - 任何只需要显示节点名称的场景
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
public class NodeBriefDTO {

    /**
     * 节点ID
     * 说明：节点的唯一标识
     */
    private Long id;

    /**
     * 节点名称
     * 说明：节点的显示名称
     */
    private String name;
}