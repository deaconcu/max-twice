-- 将 post.type 字段从 tinyint 改为 varchar，存储值从数字改为字符串
ALTER TABLE post MODIFY COLUMN type VARCHAR(20) NOT NULL DEFAULT 'article';

-- 迁移存量数据（原 1=index, 2=article）
UPDATE post SET type = 'index' WHERE type = '1';
UPDATE post SET type = 'article' WHERE type = '2';
