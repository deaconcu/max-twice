package com.prosper.learn.application.dto.response.node;

import lombok.Data;

/**
 * 简单节点 DTO
 *
 * 用途：包含基础节点信息（id、name、description）
 *
 * 使用场景：
 * - 目录类型帖子展示引用的节点列表
 * - 需要 description 但不需要统计信息的场景
 *
 * @author Claude
 * @since 2025-03-03
 */
@Data
public class NodeSimpleDTO {

    /**
     * 节点ID
     */
    private Long id;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点描述
     */
    private String description;
}
