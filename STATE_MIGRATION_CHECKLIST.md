# ContentState 状态迁移检查清单

## 状态映射表

| 旧状态 | 新状态 | 说明 |
|--------|--------|------|
| SUBMITTED(0) | SUBMITTED(1) | 待审核 |
| APPROVED(1) | APPROVED(2) | 已批准 |
| BANNED(2) | REJECTED(3) | 审核不通过（现有数据） |
| - | BANNED(4) | 违规封禁（新功能，暂无数据） |

## Mapper 层需要修改的硬编码值

### CourseMapper.java ✅
- [ ] line 42: `state = 1` → `state = 2`
- [ ] line 56: `state = 1` → `state = 2`
- [ ] line 60: `state = 2` → `state = 3`
- [ ] line 68: `state = 1` → `state = 2`

### PostMapper.java
- [ ] line 37: `state = 1` → `state = 2`
- [ ] line 42: `state = 1` → `state = 2`
- [ ] line 84: `state = 1` → `state = 2`
- [ ] line 87: `state != 2` → `state != 3`
- [ ] line 91: excludeState 参数，调用处需要检查

### CommentMapper.java
- [ ] line 20: `state = 1` → `state = 2`
- [ ] line 24: `state = 1` → `state = 2`
- [ ] line 33: `state = 1` → `state = 2`
- [ ] line 38: `state = 1` → `state = 2`
- [ ] line 45: `state = 1` → `state = 2`
- [ ] line 49: `state = 1` → `state = 2`

### ProfessionMapper.java
- [ ] line 25: `state = 1` → `state = 2`
- [ ] line 28: `state = 1` → `state = 2`
- [ ] line 31: `state = 1` → `state = 2`
- [ ] line 70: `state = 1` → `state = 2`

### RoadmapMapper.java
- [ ] line 55: `state, 0` → `state, 1` (INSERT 默认值)
- [ ] line 75: `state = 1` → `state = 2`
- [ ] line 86: `state = 1` → `state = 2`

### MemoryCardMapper.java
- [ ] line 90: `state = 0` → `state = 1`（需要确认这是 SUBMITTED 还是别的）

## Service 层需要修改的审核逻辑

### CourseService.java
- [ ] validateCommonStateForRejection: 检查 BANNED → 改为检查 REJECTED
- [ ] 添加 ban(id, reason) 方法

### PostService.java
- [ ] approvePost: !approve 时设置 REJECTED(3) 而不是 BANNED(4)
- [ ] deletePost: 改为使用 deleted_at 软删除（需要先添加字段）
- [ ] 添加 banPost(id) 方法

### CommentService.java
- [ ] 检查审核逻辑，确保使用正确的状态值

### ProfessionService.java
- [ ] 检查审核逻辑

### RoadmapService.java
- [ ] 检查审核逻辑

### MemoryCardService.java
- [ ] 检查状态相关逻辑

### MemoryCardDeckService.java
- [ ] 检查状态相关逻辑

## Controller 层需要检查的文件

- [ ] CoursesController.java
- [ ] PostsController.java
- [ ] ProfessionsController.java
- [ ] RoadmapsController.java
- [ ] 其他 Controller

## 前端需要修改的文件

需要搜索前端代码中的：
- [ ] ContentState 常量定义
- [ ] 状态显示文本映射
- [ ] 审核操作按钮

## 数据库迁移脚本

创建文件：`migration_content_state.sql`

```sql
-- 迁移所有使用 ContentState 的表
-- 注意：必须从后往前迁移，避免冲突

-- course 表
UPDATE course SET state = 3 WHERE state = 2;
UPDATE course SET state = 2 WHERE state = 1;
UPDATE course SET state = 1 WHERE state = 0;

-- post 表
UPDATE post SET state = 3 WHERE state = 2;
UPDATE post SET state = 2 WHERE state = 1;
UPDATE post SET state = 1 WHERE state = 0;

-- comment 表
UPDATE comment SET state = 3 WHERE state = 2;
UPDATE comment SET state = 2 WHERE state = 1;
UPDATE comment SET state = 1 WHERE state = 0;

-- node 表
UPDATE node SET state = 3 WHERE state = 2;
UPDATE node SET state = 2 WHERE state = 1;
UPDATE node SET state = 1 WHERE state = 0;

-- roadmap 表
UPDATE roadmap SET state = 3 WHERE state = 2;
UPDATE roadmap SET state = 2 WHERE state = 1;
UPDATE roadmap SET state = 1 WHERE state = 0;

-- profession 表
UPDATE profession SET state = 3 WHERE state = 2;
UPDATE profession SET state = 2 WHERE state = 1;
UPDATE profession SET state = 1 WHERE state = 0;

-- memory_card_deck 表
UPDATE memory_card_deck SET state = 3 WHERE state = 2;
UPDATE memory_card_deck SET state = 2 WHERE state = 1;
UPDATE memory_card_deck SET state = 1 WHERE state = 0;

-- memory_card 表
UPDATE memory_card SET state = 3 WHERE state = 2;
UPDATE memory_card SET state = 2 WHERE state = 1;
UPDATE memory_card SET state = 1 WHERE state = 0;
```

## 执行顺序

1. ✅ 修改 ContentState 枚举
2. [ ] 修改所有 Mapper 硬编码值
3. [ ] 修改所有 Service 审核逻辑
4. [ ] 修改所有 Controller
5. [ ] 修改前端代码
6. [ ] 创建并执行数据库迁移脚本
7. [ ] 测试验证
