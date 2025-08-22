-- 用户统计表
CREATE TABLE user_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL COMMENT '用户ID',
    stat_date DATE NOT NULL COMMENT '统计日期',
    total_views BIGINT DEFAULT 0 COMMENT '当日总阅读量',
    total_twice BIGINT DEFAULT 0 COMMENT '当日总twice点赞数',
    total_helpful BIGINT DEFAULT 0 COMMENT '当日总helpful点赞数',
    total_comments BIGINT DEFAULT 0 COMMENT '当日总评论数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date (user_id, stat_date),
    INDEX idx_user_id (user_id),
    INDEX idx_stat_date (stat_date),
    INDEX idx_user_date_range (user_id, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户统计表';

-- 为post表添加view_count字段（如果还没有的话）
-- ALTER TABLE post ADD COLUMN view_count INT DEFAULT 0 COMMENT '总阅读量';
-- CREATE INDEX idx_post_view_count ON post (view_count DESC);