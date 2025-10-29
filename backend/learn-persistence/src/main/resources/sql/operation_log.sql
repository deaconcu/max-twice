-- 操作日志表
CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(100) NOT NULL COMMENT '操作人名称（冗余字段，避免用户改名后无法追溯）',
  `operator_role` TINYINT NOT NULL COMMENT '操作人角色（0=普通用户, 1=审核员, 2=管理员, 3=超级管理员）',

  `module` VARCHAR(50) NOT NULL COMMENT '模块名称（用户管理、内容管理、系统配置等）',
  `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型（封禁用户、删除帖子、审核通过等）',
  `operation_level` TINYINT NOT NULL DEFAULT 2 COMMENT '操作级别（1=低, 2=中, 3=高）',

  `target_type` VARCHAR(50) NOT NULL COMMENT '目标类型（User, Post, Course, Comment, SystemConfig等）',
  `target_id` BIGINT NOT NULL COMMENT '目标ID（SystemConfig类型时为0）',
  `target_name` VARCHAR(255) COMMENT '目标名称（冗余字段，便于查看）',

  `reason` VARCHAR(500) COMMENT '操作原因（如拒绝理由、屏蔽原因、封禁原因）',
  `extra_data` JSON COMMENT '额外数据（如修改前后的值、详细参数等）',

  `ip_address` VARCHAR(45) COMMENT '操作IP地址',

  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',

  PRIMARY KEY (`id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_module_type` (`module`, `operation_type`),
  KEY `idx_operation_level` (`operation_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
