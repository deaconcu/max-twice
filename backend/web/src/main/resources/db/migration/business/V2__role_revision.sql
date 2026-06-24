-- =============================================================================
-- role 表改造为 revision 模型
--
-- 变更内容：
-- 1. state: tinyint -> varchar(20)，存储 NewContentState 的字符串值
--    (NEVER_PUBLISHED / PUBLISHED / BANNED)
-- 2. 删除 reason 列（驳回原因移入 content_revision.reject_reason）
-- 3. 新增 current_revision_id / pending_revision_id 双指针
-- 4. 为已发布的 role 写入初始 content_revision 行，并回填 current_revision_id
-- 5. NewContentType 增加 ROLE
-- =============================================================================

-- 0. 安全检查：列出现存 state 分布
-- SELECT state, COUNT(*) FROM role GROUP BY state;

-- 1. 先 drop 涉及 state 列的索引（避免 MODIFY 时隐式重建）
ALTER TABLE `role` DROP INDEX `idx_role_state_id`;
ALTER TABLE `role` DROP INDEX `idx_role_categories`;

-- 2. 修改 state 列类型为 VARCHAR(20)
ALTER TABLE `role`
    MODIFY COLUMN `state` VARCHAR(20) NOT NULL DEFAULT 'NEVER_PUBLISHED'
    COMMENT '主体状态: NEVER_PUBLISHED / PUBLISHED / BANNED (NewContentState)';

-- 3. 数据迁移：旧 ContentState 数值字符串 -> NewContentState 字符串
--    旧值映射: 0=DRAFT, 1=SUBMITTED, 2=PUBLISHED, 3=REJECTED, 4=BANNED
--    新值映射: PUBLISHED -> PUBLISHED, BANNED -> BANNED, 其他 -> NEVER_PUBLISHED
UPDATE `role` SET `state` = CASE
    WHEN `state` = '2' THEN 'PUBLISHED'
    WHEN `state` = '4' THEN 'BANNED'
    ELSE 'NEVER_PUBLISHED'
END;

-- 4. 删除 reason 列（驳回原因落到 content_revision.reject_reason）
ALTER TABLE `role` DROP COLUMN `reason`;

-- 5. 新增双指针列
ALTER TABLE `role`
    ADD COLUMN `current_revision_id` BIGINT DEFAULT NULL
        COMMENT '当前对外展示的 revision id（指向 content_revision.id）',
    ADD COLUMN `pending_revision_id` BIGINT DEFAULT NULL
        COMMENT '正在审核中的 revision id（同一时间至多 1 个）';

-- 6. 重建索引
ALTER TABLE `role` ADD INDEX `idx_role_state_id` (`state`, `id` DESC);
ALTER TABLE `role` ADD INDEX `idx_role_categories`
    (`main_category`, `sub_category`, `state`, `id`);

-- 7. 为每个已 PUBLISHED 的 role 创建初始 content_revision 行
--    payload 包含 name / description / icon / skills / mainCategory / subCategory
INSERT INTO `content_revision`
    (content_type, content_id, revision_no, status, payload, hash,
     reject_reason, author_id, reviewer_id, created_at, reviewed_at)
SELECT
    'role',
    r.id,
    1,
    'PUBLISHED',
    JSON_OBJECT(
        'name', r.name,
        'description', r.description,
        'icon', r.icon,
        'skills', r.skills,
        'mainCategory', r.main_category,
        'subCategory', r.sub_category
    ),
    SHA2(CONCAT_WS('|',
        IFNULL(r.name, ''),
        IFNULL(r.description, ''),
        IFNULL(r.icon, ''),
        IFNULL(r.skills, ''),
        IFNULL(r.main_category, 0),
        IFNULL(r.sub_category, 0)
    ), 256),
    NULL,
    r.creator_id,
    r.creator_id,
    r.created_at,
    r.updated_at
FROM `role` r
WHERE r.state = 'PUBLISHED' AND r.deleted_at IS NULL;

-- 8. 回填 current_revision_id
UPDATE `role` r
JOIN `content_revision` cr
    ON cr.content_type = 'role'
   AND cr.content_id = r.id
   AND cr.revision_no = 1
SET r.current_revision_id = cr.id
WHERE r.state = 'PUBLISHED' AND r.deleted_at IS NULL;
