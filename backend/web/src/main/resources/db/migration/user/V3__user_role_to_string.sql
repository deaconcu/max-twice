-- 将 user.role 字段从 int 改为 varchar，存储值从数字改为字符串
ALTER TABLE user MODIFY COLUMN role VARCHAR(20) NOT NULL DEFAULT 'user';

-- 迁移存量数据（原 0=user, 1=moderator, 2=admin, 3=super）
UPDATE user SET role = 'user' WHERE role = '0';
UPDATE user SET role = 'moderator' WHERE role = '1';
UPDATE user SET role = 'admin' WHERE role = '2';
UPDATE user SET role = 'super' WHERE role = '3';
