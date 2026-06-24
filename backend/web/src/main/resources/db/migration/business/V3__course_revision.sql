-- =============================================================================
-- course 表改造为 revision 模型
--
-- 变更内容：
-- 1. state: tinyint -> varchar(20)，存储 NewContentState 的字符串值
--    (NEVER_PUBLISHED / PUBLISHED / BANNED)
-- 2. 删除 reason 列（驳回原因移入 content_revision.reject_reason）
-- 3. 新增 current_revision_id / pending_revision_id 双指针
-- 4. 为已发布的 course 写入初始 content_revision 行，并回填 current_revision_id
-- 5. NewContentType 增加 COURSE
--
-- 已知 TODO（不在本次范围）：
--   rootNodeId 的可见性级联：当前 createCourse 在 course 状态为 SUBMITTED 时
--   就已经创建了一个 PUBLISHED 状态的 rootNode，这违反了"父级未发布则子节点不可见"
--   的不变量，留作下一个独立 PR 处理（让 rootNode 状态跟随 course 状态联动）。
-- =============================================================================

-- 0. 安全检查：列出现存 state 分布
-- SELECT state, COUNT(*) FROM course GROUP BY state;

-- 1. 先 drop 涉及 state 列的索引（避免 MODIFY 时隐式重建）
ALTER TABLE `course` DROP INDEX `idx_course_state_parent`;
ALTER TABLE `course` DROP INDEX `idx_course_category`;

-- 2. 修改 state 列类型为 VARCHAR(20)
ALTER TABLE `course`
    MODIFY COLUMN `state` VARCHAR(20) NOT NULL DEFAULT 'NEVER_PUBLISHED'
    COMMENT '主体状态: NEVER_PUBLISHED / PUBLISHED / BANNED (NewContentState)';

-- 3. 数据迁移：旧 ContentState 数值字符串 -> NewContentState 字符串
--    旧值映射: 0=DRAFT, 1=SUBMITTED, 2=PUBLISHED, 3=REJECTED, 4=BANNED
--    新值映射: PUBLISHED -> PUBLISHED, BANNED -> BANNED, 其他 -> NEVER_PUBLISHED
UPDATE `course` SET `state` = CASE
    WHEN `state` = '2' THEN 'PUBLISHED'
    WHEN `state` = '4' THEN 'BANNED'
    ELSE 'NEVER_PUBLISHED'
END;

-- 4. 删除 reason 列（驳回原因落到 content_revision.reject_reason）
ALTER TABLE `course` DROP COLUMN `reason`;

-- 5. 新增双指针列
ALTER TABLE `course`
    ADD COLUMN `current_revision_id` BIGINT DEFAULT NULL
        COMMENT '当前对外展示的 revision id（指向 content_revision.id）',
    ADD COLUMN `pending_revision_id` BIGINT DEFAULT NULL
        COMMENT '正在审核中的 revision id（同一时间至多 1 个）';

-- 6. 重建索引（保留与原始一致的列顺序）
ALTER TABLE `course` ADD INDEX `idx_course_state_parent`
    (`state`, `parent_course_id`, `id` DESC);
ALTER TABLE `course` ADD INDEX `idx_course_category`
    (`main_category`, `sub_category`, `state`, `parent_course_id`, `id` DESC);

-- 7. 为每个已 PUBLISHED 的 course 创建初始 content_revision 行
--    payload 包含 name / description / icon / mainCategory / subCategory / parentCourseId
--    注：course 表无 deleted_at 列
INSERT INTO `content_revision`
    (content_type, content_id, revision_no, status, payload, hash,
     reject_reason, author_id, reviewer_id, created_at, reviewed_at)
SELECT
    'course',
    c.id,
    1,
    'PUBLISHED',
    JSON_OBJECT(
        'name', c.name,
        'description', c.description,
        'icon', c.icon,
        'mainCategory', c.main_category,
        'subCategory', c.sub_category,
        'parentCourseId', c.parent_course_id
    ),
    SHA2(CONCAT_WS('|',
        IFNULL(c.name, ''),
        IFNULL(c.description, ''),
        IFNULL(c.icon, ''),
        IFNULL(c.main_category, 0),
        IFNULL(c.sub_category, 0),
        IFNULL(c.parent_course_id, 0)
    ), 256),
    NULL,
    c.creator_id,
    c.creator_id,
    c.created_at,
    c.updated_at
FROM `course` c
WHERE c.state = 'PUBLISHED';

-- 8. 回填 current_revision_id
UPDATE `course` c
JOIN `content_revision` cr
    ON cr.content_type = 'course'
   AND cr.content_id = c.id
   AND cr.revision_no = 1
SET c.current_revision_id = cr.id
WHERE c.state = 'PUBLISHED';
