-- 创建每年点赞统计表
CREATE TABLE upvote_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(20) NOT NULL COMMENT '对象类型：POST, COMMENT等',
    object_id BIGINT NOT NULL COMMENT '对象ID',
    stats JSON NOT NULL COMMENT '点赞统计数据，格式：{"1-1":{"twice": 3, "helpful": 2}, ...}',
    stat_year INT NOT NULL COMMENT '统计年份',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_type_object_year (type, object_id, stat_year),
    INDEX idx_type_object (type, object_id),
    INDEX idx_stat_year (stat_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每年点赞统计表';

-- 为post表添加分数相关字段
ALTER TABLE post
ADD COLUMN score DOUBLE DEFAULT 0.0 COMMENT '计算出的排序分数',
ADD COLUMN score_calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分数计算时间',
ADD INDEX idx_score (score DESC, id DESC);

-- 为post表的score字段创建索引以优化排序查询
CREATE INDEX idx_post_node_score ON post (nodeId, score DESC, id DESC);

ALTER TABLE roadmap
    ADD COLUMN score DOUBLE DEFAULT 0.0 COMMENT '计算出的排序分数',
    ADD COLUMN score_calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分数计算时间',
    ADD INDEX idx_score (score DESC, id DESC);