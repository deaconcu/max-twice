-- 将 upvote.type 字段从 tinyint 改为 varchar，存储值从数字改为字符串
ALTER TABLE upvote MODIFY COLUMN type VARCHAR(20) NOT NULL DEFAULT 'like';

-- 迁移存量数据（原 1=twice, 2=like）
UPDATE upvote SET type = 'twice' WHERE type = '1';
UPDATE upvote SET type = 'like' WHERE type = '2';
