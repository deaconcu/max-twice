-- ====================================================================================================
-- Anki 算法改造 - 数据库迁移脚本
-- 版本: 1.0
-- 日期: 2025-10-30
-- 说明: 为 user_card_srs 表添加 Anki 算法所需的新字段
-- ====================================================================================================

-- 1. 新增 type 字段 (卡片状态)
-- 0=NEW(新卡片), 1=LEARNING(学习中), 2=REVIEW(复习), 3=RELEARNING(重新学习)
ALTER TABLE user_card_srs
ADD COLUMN type TINYINT NOT NULL DEFAULT 0
COMMENT '卡片状态: 0=NEW, 1=LEARNING, 2=REVIEW, 3=RELEARNING';

-- 2. 新增 current_step 字段 (学习/重学步骤索引)
ALTER TABLE user_card_srs
ADD COLUMN current_step TINYINT NOT NULL DEFAULT 0
COMMENT '学习/重学步骤索引，仅在 type=1/3 时有意义';

-- 3. 重命名 interval_days 字段为 interval
-- 注意: interval 的单位由 type 决定 (type=1/3时为分钟, type=2时为天)
ALTER TABLE user_card_srs
CHANGE COLUMN interval_days `interval` INT NOT NULL
COMMENT '复习间隔: type=1/3时单位为分钟, type=2时单位为天';

-- 4. 新增 lapse_old_interval 字段 (遗忘前的间隔)
-- 用于保存遗忘前的间隔(天)，在 RELEARNING 重新毕业时计算恢复间隔
ALTER TABLE user_card_srs
ADD COLUMN lapse_old_interval SMALLINT NULL DEFAULT NULL
COMMENT '遗忘前的间隔(天), 仅在 type=3(RELEARNING) 时使用';

-- 5. 创建复习队列查询优化索引
-- 用于加速 getReviewQueue 查询 (按 type 和 review_due_at 排序)
CREATE INDEX idx_user_review_queue
ON user_card_srs(user_id, type, review_due_at);

-- 6. 创建新卡片查询优化索引
-- 用于加速新卡片查询 (按 id 排序)
CREATE INDEX idx_user_new_cards
ON user_card_srs(user_id, type, id);

-- ====================================================================================================
-- 数据初始化 (如果表中已有数据)
-- ====================================================================================================

-- 注意: 如果当前系统未上线或表中无数据，可以跳过此步骤
-- 如果有现有数据，需要将所有现有卡片初始化为 REVIEW 状态

-- 示例: 将所有 interval > 0 的卡片设置为 REVIEW 状态
-- UPDATE user_card_srs
-- SET type = 2, current_step = 0
-- WHERE `interval` > 0;

-- 示例: 将所有 interval = 0 的卡片设置为 NEW 状态
-- UPDATE user_card_srs
-- SET type = 0, current_step = 0
-- WHERE `interval` = 0;

-- ====================================================================================================
-- 验证脚本
-- ====================================================================================================

-- 验证表结构
-- DESCRIBE user_card_srs;

-- 验证索引
-- SHOW INDEX FROM user_card_srs;

-- 统计各状态卡片数量
-- SELECT type, COUNT(*) as count FROM user_card_srs GROUP BY type;
