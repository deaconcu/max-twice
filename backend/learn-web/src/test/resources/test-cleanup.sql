-- 测试数据清理脚本
-- 此文件会在测试结束后执行，清理 test-data.sql 插入的系统配置数据

-- ==================== 清理系统配置数据 ====================

-- 清空 system 表（删除所有测试配置数据）
TRUNCATE TABLE `system`;

-- ==================== 说明 ====================
-- 1. 使用 TRUNCATE 清空整个 system 表，比 DELETE 更快且重置自增ID
-- 2. 业务数据（user, course, node, post等）由 @Transactional 自动回滚，无需手动清理
-- 3. 如果 system 表有生产数据，请改用 DELETE WHERE key IN (...)

