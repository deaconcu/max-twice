-- ============================================================
-- 迁移脚本：Node 和 ToC 重构
-- 目的：支持 Roadmap 包含任意 Node，支持按 Node 阅读时显示目录
-- 日期：2026-01-26
-- ============================================================

-- ============================================================
-- 第一部分：表结构修改
-- ============================================================

-- 1. course_toc 表重命名为 node_toc
RENAME TABLE course_toc TO node_toc;

-- 2. 修改 node_toc 索引
ALTER TABLE node_toc
  DROP KEY idx_course_toc_hash;

ALTER TABLE node_toc
  ADD UNIQUE KEY idx_node_toc_hash (hash);

-- 3. user_course_toc 表重命名为 user_node_toc
RENAME TABLE user_course_toc TO user_node_toc;

-- 2. 修改字段名：course_id → node_id
ALTER TABLE user_node_toc
  CHANGE COLUMN course_id node_id BIGINT NOT NULL;

-- 3. 重建索引
ALTER TABLE user_node_toc
  DROP KEY idx_user_course_toc_unique;

ALTER TABLE user_node_toc
  ADD UNIQUE KEY idx_user_node_toc_unique (user_id, node_id);

-- 4. node 表增加 is_course_root 字段
ALTER TABLE node
  ADD COLUMN is_course_root TINYINT NOT NULL DEFAULT 0
  COMMENT '是否为课程根节点：0=普通节点, 1=课程根节点';

-- 5. 为 node 表添加索引（提升查询性能）
-- 注意：不能使用唯一索引，因为同一个 course_id 下有多个 is_course_root=0 的节点
-- 数据一致性由应用层保证：一个 Course 只能有一个 is_course_root=1 的节点
-- ALTER TABLE node
--  ADD INDEX idx_node_course_root (course_id, is_course_root);

-- ============================================================
-- 第二部分：数据迁移
-- ============================================================

-- 6. 设置现有 Course 的根节点 is_course_root=1
UPDATE node n
INNER JOIN course c ON n.id = c.root_node_id
SET n.is_course_root = 1
WHERE n.is_course_root = 0;

-- 验证：检查是否所有 Course 都有对应的根节点
-- 如果这个查询返回结果，说明有 Course 没有根节点，需要手动处理
SELECT c.id as course_id, c.name, c.root_node_id
FROM course c
LEFT JOIN node n ON c.root_node_id = n.id
WHERE n.id IS NULL OR n.is_course_root = 0;

-- ============================================================
-- 第三部分：Roadmap content 数据迁移
-- ============================================================

-- 注意：roadmap.content 是 JSON 格式，需要用应用层代码迁移
-- 这里只提供 SQL 查询，用于应用层迁移脚本

-- 查询所有需要迁移的 Roadmap
-- 应用层需要：
-- 1. 解析 content JSON
-- 2. 将节点列表中的 course_id 替换为对应的 root_node_id
-- 3. 重新生成 content_hash
-- 4. 更新 roadmap 记录

SELECT
    r.id,
    r.content,
    r.content_hash
FROM roadmap r
WHERE r.deleted_at IS NULL
ORDER BY r.id;

-- ============================================================
-- 第四部分：数据一致性检查
-- ============================================================

-- 检查 1：所有 is_course_root=1 的节点都应该有对应的 Course
SELECT n.id, n.name, n.course_id, n.is_course_root
FROM node n
LEFT JOIN course c ON c.root_node_id = n.id
WHERE n.is_course_root = 1 AND c.id IS NULL;

-- 检查 2：所有 Course 的 root_node_id 应该指向 is_course_root=1 的节点
SELECT c.id, c.name, c.root_node_id, n.is_course_root
FROM course c
INNER JOIN node n ON c.root_node_id = n.id
WHERE n.is_course_root != 1;

-- 检查 3：user_node_toc 表中的 node_id 都应该存在
SELECT ut.user_id, ut.node_id
FROM user_node_toc ut
LEFT JOIN node n ON ut.node_id = n.id
WHERE n.id IS NULL;

-- 检查 4：每个 Course 应该只有一个 is_course_root=1 的节点
SELECT course_id, COUNT(*) as root_count
FROM node
WHERE is_course_root = 1
GROUP BY course_id
HAVING COUNT(*) > 1;

-- ============================================================
-- 回滚脚本（如果需要回滚）
-- ============================================================

/*
-- 回滚步骤（谨慎使用！）

-- 1. 删除 node 表的 is_course_root 字段
ALTER TABLE node DROP COLUMN is_course_root;

-- 2. 删除索引
ALTER TABLE node DROP INDEX idx_node_course_root;

-- 3. user_node_toc 表改回 user_course_toc
ALTER TABLE user_node_toc
  CHANGE COLUMN node_id course_id BIGINT NOT NULL;

RENAME TABLE user_node_toc TO user_course_toc;

-- 4. 重建原索引
ALTER TABLE user_course_toc
  DROP KEY idx_user_node_toc_unique;

ALTER TABLE user_course_toc
  ADD UNIQUE KEY idx_user_course_toc_unique (user_id, course_id);

-- 注意：Roadmap content 的回滚需要应用层代码处理
*/
