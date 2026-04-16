package com.prosper.learn.learning.progress;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户节点完成记录
 * 每个完成的节点一条记录
 */
@Data
public class UserNodeCompletionDO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 节点ID
     */
    private Long nodeId;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;
}