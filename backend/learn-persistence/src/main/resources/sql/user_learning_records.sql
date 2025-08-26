-- 用户学习记录表
-- 存储用户完成的所有节点ID，用逗号分隔
CREATE TABLE user_learning_records (
    user_id INT PRIMARY KEY COMMENT '用户ID',
    completed_node_ids TEXT COMMENT '已完成的节点ID列表，逗号分隔',
    completed_count SMALLINT DEFAULT 0 COMMENT '已完成节点总数（冗余字段，便于统计）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_updated_at (updated_at),
    INDEX idx_completed_count (completed_count)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COMMENT='用户学习记录表';