-- 测试数据初始化脚本
-- 此文件会在测试开始前自动执行，插入测试所需的基础数据

-- ==================== 系统配置数据 ====================

-- 1. 课程分类配置
INSERT INTO `system` (`key`, `value`) VALUES (
  'courseCategories',
  '{
    "mainCategories": [
      {"id": 1, "name": "编程语言", "description": "各类编程语言学习"},
      {"id": 2, "name": "框架技术", "description": "主流开发框架"}
    ],
    "categoryMapping": [
      {
        "mainCategoryId": 1,
        "subcategories": [
          {"id": 1, "name": "Java"},
          {"id": 2, "name": "Python"},
          {"id": 3, "name": "JavaScript"}
        ]
      },
      {
        "mainCategoryId": 2,
        "subcategories": [
          {"id": 1, "name": "Spring"},
          {"id": 2, "name": "Django"}
        ]
      }
    ]
  }'
) ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);

-- 2. 职业分类配置
INSERT INTO `system` (`key`, `value`) VALUES (
  'professionCategories',
  '{
    "mainCategories": [
      {"id": 1, "title": "软件开发", "description": "软件开发相关职业"},
      {"id": 2, "title": "数据分析", "description": "数据分析相关职业"}
    ],
    "categoryMapping": [
      {
        "mainCategoryId": 1,
        "subcategories": [
          {"id": 1, "name": "后端开发"},
          {"id": 2, "name": "前端开发"}
        ]
      },
      {
        "mainCategoryId": 2,
        "subcategories": [
          {"id": 1, "name": "数据科学家"},
          {"id": 2, "name": "数据工程师"}
        ]
      }
    ]
  }'
) ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);

-- 3. 验证配置
INSERT INTO `system` (`key`, `value`) VALUES (
  'validationConfig',
  '{
    "course-name": {"min": 1, "max": 40},
    "course-description": {"min": 20, "max": 1000},
    "node-name": {"min": 1, "max": 40},
    "node-description": {"min": 1, "max": 200},
    "post-content": {"min": 1, "max": 10000},
    "comment-content": {"min": 1, "max": 500},
    "profession-name": {"min": 1, "max": 30},
    "profession-description": {"min": 1, "max": 2000},
    "roadmap-description": {"min": 1, "max": 500},
    "deck-title": {"min": 1, "max": 30},
    "deck-description": {"min": 1, "max": 1000},
    "card-front": {"min": 1, "max": 500},
    "card-back": {"min": 1, "max": 500}
  }'
) ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);

-- ==================== 说明 ====================
-- 1. 所有INSERT使用 ON DUPLICATE KEY UPDATE，避免重复插入
-- 2. 只插入系统配置数据，不插入业务数据
-- 3. 业务数据由各个测试用例自己创建
-- 4. @Transactional 会在每个测试后自动回滚业务数据
-- 5. 系统配置数据不会被回滚，在整个测试期间保持可用
