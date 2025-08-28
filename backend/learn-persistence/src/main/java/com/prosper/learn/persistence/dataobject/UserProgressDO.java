package com.prosper.learn.persistence.dataobject;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户学习记录数据对象
 * 存储用户完成的所有节点ID，采用逗号分隔的字符串存储
 */
@Data
public class UserProgressDO {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 已完成的节点ID列表，逗号分隔
     * 例如: "123,456,789,1001"
     */
    private String nodeIds;
    
    /**
     * 已完成节点总数（冗余字段，便于统计和查询优化）
     */
    private Integer count;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}