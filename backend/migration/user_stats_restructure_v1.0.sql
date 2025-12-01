-- 用户统计系统重构数据库迁移脚本
-- 版本：v1.0
-- 说明：将现有user_stats表重命名为user_stats_yearly，并创建新的user_stats表

-- ===========================================
-- 第一步：重命名现有表为历史表
-- ===========================================

-- 备份现有表结构（可选，生产环境建议执行）
-- CREATE TABLE user_stats_backup AS SELECT * FROM user_stats LIMIT 0;

-- 重命名现有表
RENAME TABLE user_stats TO user_stats_yearly;

-- 验证重命名是否成功
-- SHOW TABLES LIKE '%user_stats%';

-- ===========================================
-- 第二步：创建新的user_stats表
-- ===========================================

CREATE TABLE user_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,

    -- 日度增量统计
    daily_views INT DEFAULT 0 COMMENT '当日浏览量',
    daily_twice INT DEFAULT 0 COMMENT '当日两次能懂点赞数',
    daily_helpful INT DEFAULT 0 COMMENT '当日有帮助点赞数',
    daily_comments INT DEFAULT 0 COMMENT '当日评论数',

    -- 学习统计（累计快照）
    learning_courses_count INT DEFAULT 0 COMMENT '正在学习课程数',
    completed_courses_count INT DEFAULT 0 COMMENT '已完成课程数',
    in_progress_professions_count INT DEFAULT 0 COMMENT '正在进行职业数',
    completed_professions_count INT DEFAULT 0 COMMENT '已完成职业数',

    -- 社交统计（累计快照）
    following_users_count INT DEFAULT 0 COMMENT '关注的人数',
    following_courses_count INT DEFAULT 0 COMMENT '关注的课程数',
    following_professions_count INT DEFAULT 0 COMMENT '关注的职业数',

    -- 创作统计（累计快照）
    created_articles_count INT DEFAULT 0 COMMENT '创建的文章数',
    created_tocs_count INT DEFAULT 0 COMMENT '创建的目录数',
    created_roadmaps_count INT DEFAULT 0 COMMENT '创建的路线图数',
    created_card_decks_count INT DEFAULT 0 COMMENT '创建的卡片组数',

    -- 元数据
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 索引
    UNIQUE KEY uk_user_date (user_id, stat_date),
    INDEX idx_stat_date (stat_date),
    INDEX idx_updated_at (updated_at),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='用户当日统计表';

-- ===========================================
-- 第三步：验证表结构
-- ===========================================

-- 检查新表结构
-- DESCRIBE user_stats;

-- 检查历史表结构
-- DESCRIBE user_stats_yearly;

-- 检查索引
-- SHOW INDEX FROM user_stats;

-- ===========================================
-- 第四步：数据初始化（可选）
-- ===========================================

-- 如果需要为活跃用户初始化今日数据，可以执行以下SQL
-- 这里暂时注释，在应用启动时通过代码处理

/*
INSERT INTO user_stats (user_id, stat_date)
SELECT DISTINCT user_id, CURDATE()
FROM user_stats_yearly
WHERE stat_year = YEAR(CURDATE())
AND user_id IN (
    -- 这里可以添加筛选最近活跃用户的条件
    SELECT DISTINCT creator_id FROM posts WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
    UNION
    SELECT DISTINCT user_id FROM comments WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
)
ON DUPLICATE KEY UPDATE updated_at = NOW();
*/

-- ===========================================
-- 迁移完成验证
-- ===========================================

-- 验证表是否创建成功
SELECT
    'user_stats' as table_name,
    COUNT(*) as record_count,
    'New daily stats table' as description
FROM user_stats

UNION ALL

SELECT
    'user_stats_yearly' as table_name,
    COUNT(*) as record_count,
    'Historical yearly stats table' as description
FROM user_stats_yearly;

-- ===========================================
-- 回滚脚本（紧急情况使用）
-- ===========================================

/*
-- 如果需要回滚，执行以下SQL：

-- 删除新创建的表
DROP TABLE IF EXISTS user_stats;

-- 将yearly表重命名回原名
RENAME TABLE user_stats_yearly TO user_stats;

-- 验证回滚
SHOW TABLES LIKE '%user_stats%';
*/

-- ===========================================
-- 迁移日志
-- ===========================================

-- 创建迁移记录（可选）
/*
CREATE TABLE IF NOT EXISTS migration_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    migration_name VARCHAR(255) NOT NULL,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('SUCCESS', 'FAILED') DEFAULT 'SUCCESS',
    notes TEXT
);

INSERT INTO migration_log (migration_name, notes)
VALUES ('user_stats_restructure_v1.0', '重构用户统计表：user_stats -> user_stats_yearly，创建新的user_stats表');
*/