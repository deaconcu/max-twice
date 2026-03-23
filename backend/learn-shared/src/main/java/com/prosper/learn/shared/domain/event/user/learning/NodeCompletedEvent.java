package com.prosper.learn.shared.domain.event.user.learning;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 节点完成状态变更事件
 * 当用户完成或取消完成一个学习节点时触发
 */
@Data
@AllArgsConstructor
public class NodeCompletedEvent {

    /** 用户ID */
    private Long userId;

    /** 节点ID */
    private Long nodeId;

    /** true=完成，false=取消完成 */
    private boolean completed;

    /** 完成节点 */
    public static NodeCompletedEvent completed(Long userId, Long nodeId) {
        return new NodeCompletedEvent(userId, nodeId, true);
    }

    /** 取消完成节点 */
    public static NodeCompletedEvent uncompleted(Long userId, Long nodeId) {
        return new NodeCompletedEvent(userId, nodeId, false);
    }
}
