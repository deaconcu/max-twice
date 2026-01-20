package com.prosper.learn.shared.domain.event.content.lifecycle;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 节点创建事件
 * 当用户创建新节点时触发
 */
@Data
@AllArgsConstructor
public class NodeCreatedEvent {

    /** 节点ID */
    private Long id;

    /** 节点名称 */
    private String name;

    /** 节点描述 */
    private String description;
}
