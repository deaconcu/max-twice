-- 将 user.state 字段从 tinyint 改为 varchar，存储值从数字改为字符串
ALTER TABLE user MODIFY COLUMN state VARCHAR(20) NOT NULL DEFAULT 'active';

-- 迁移存量数据（原 1 = active, 2 = banned）
UPDATE user SET state = 'active' WHERE state = '1';
UPDATE user SET state = 'banned' WHERE state = '2';
