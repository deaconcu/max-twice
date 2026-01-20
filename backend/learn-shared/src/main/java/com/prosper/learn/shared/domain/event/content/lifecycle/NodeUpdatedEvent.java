package com.prosper.learn.shared.domain.event.content.lifecycle;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 节点更新事件
 * 当节点内容被修改时触发
 */
@Data
@AllArgsConstructor
public class NodeUpdatedEvent {

    /** 节点ID */
    private Long id;

    /** 节点名称 */
    private String name;

    /** 节点描述 */
    private String description;
}
