-- 将 operation_level 从 tinyint 转换为 varchar(20)

-- 1. 添加临时列
ALTER TABLE `operation_log` ADD COLUMN `operation_level_temp` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL;

-- 2. 数据迁移：1->low, 2->medium, 3->high
UPDATE `operation_log` SET `operation_level_temp` = 
    CASE `operation_level`
        WHEN 1 THEN 'low'
        WHEN 2 THEN 'medium'
        WHEN 3 THEN 'high'
        ELSE 'medium'
    END;

-- 3. 删除原列
ALTER TABLE `operation_log` DROP COLUMN `operation_level`;

-- 4. 重命名临时列为原列名
ALTER TABLE `operation_log` RENAME COLUMN `operation_level_temp` TO `operation_level`;

-- 5. 修改列属性为 NOT NULL 和设置默认值
ALTER TABLE `operation_log` MODIFY COLUMN `operation_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'medium' COMMENT '操作级别（low=低, medium=中, high=高）';
