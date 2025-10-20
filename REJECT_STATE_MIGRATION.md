# ContentState 状态迁移方案

## 背景

将 ContentState 枚举从 0/1/2 改为 1/2/3/4，并新增 REJECTED 状态，区分"审核不通过"和"违规封禁"。

## 状态变更

### 旧状态
```java
SUBMITTED(0)  // 待审核
APPROVED(1)   // 已批准
BANNED(2)     // 封禁（既表示审核不通过，又表示违规封禁）
```

### 新状态
```java
SUBMITTED(1)  // 待审核
APPROVED(2)   // 已批准
REJECTED(3)   // 审核不通过（可重新提交）
BANNED(4)     // 违规封禁（一般不可逆）
```

## 需要修改的内容

### 1. 枚举定义
- [x] `learn-common/src/main/java/com/prosper/learn/common/Enums.java`
  - ContentState 枚举已修改

### 2. 数据库迁移（SQL）

**决策：现有 BANNED(2) 数据迁移到 REJECTED(3)，保留 ban 功能用于未来的违规封禁。**

需要创建迁移脚本，将现有数据从旧状态值迁移到新状态值：

```sql
-- 迁移所有使用 ContentState 的表
-- 迁移策略：0→1, 1→2, 2→3（现有 BANNED 改为 REJECTED）
-- 注意：必须从后往前迁移，避免冲突

-- course 表
UPDATE course SET state = 3 WHERE state = 2;  -- BANNED(2) → REJECTED(3)
UPDATE course SET state = 2 WHERE state = 1;  -- APPROVED(1) → APPROVED(2)
UPDATE course SET state = 1 WHERE state = 0;  -- SUBMITTED(0) → SUBMITTED(1)

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

**说明：**
- 现有的 `state=2` (BANNED) 数据全部改为 `state=3` (REJECTED)
- 这意味着所有之前被"封禁"的内容现在变成"审核不通过"，用户可以修改后重新提交
- `state=4` (BANNED) 暂时不会有数据，保留给未来真正的违规封禁使用

### 3. Mapper 层修改

需要修改所有硬编码状态值的 SQL：

#### CourseMapper.java
- [x] `listRootByCategory`: 使用 APPROVED_VALUE
- [x] `approve`: 使用 APPROVED_VALUE
- [x] `reject`: 使用 REJECTED_VALUE
- [x] `ban`: 添加新方法，使用 BANNED_VALUE
- [x] `countActiveCourses`: 使用 APPROVED_VALUE

#### 其他 Mapper（全部已完成）
- [x] PostMapper - 使用 APPROVED_VALUE
- [x] CommentMapper - 使用 APPROVED_VALUE
- [x] NodeMapper - （如需要）
- [x] RoadmapMapper - 使用 SUBMITTED_VALUE, APPROVED_VALUE
- [x] ProfessionMapper - 使用 APPROVED_VALUE
- [x] MemoryCardDeckMapper - 使用 SUBMITTED_VALUE
- [x] MemoryCardMapper - 使用 SUBMITTED_VALUE
- [x] UserCardSrsMapper - 使用 APPROVED_VALUE
- [x] UserCourseMapper - 使用 APPROVED_VALUE, IN_PROGRESS_VALUE
- [x] UserRoadmapMapper - 使用 IN_PROGRESS_VALUE
- [x] UserCardInCourseMapper - 使用 APPROVED_VALUE

**关键改进：添加静态常量**
- 在 Enums.java 中添加了 `SUBMITTED_VALUE`, `APPROVED_VALUE`, `REJECTED_VALUE`, `BANNED_VALUE`
- 原因：MyBatis 注解需要编译时常量，不能使用 `.value()` 方法调用

### 4. Service 层修改

#### 需要区分 reject 和 ban 的 Service

**CourseService**
- [x] 已有 `reject(id, reason)` 方法
- [x] 添加 `ban(id, reason)` 方法（用于违规封禁）
- [x] 修改 `validateCommonStateForRejection` 方法，检查 REJECTED 和 BANNED
- [x] 添加 `validateStateForBan` 方法
- [x] CourseDataService 添加 `ban` 方法
- [x] CourseMapper 添加 `ban` 方法

**ProfessionService**
- [x] 修正 `reject` 方法，设置为 REJECTED 而不是 BANNED
- [x] 添加 `ban(id, reason)` 方法
- [x] 修改 `getById` 检查 REJECTED 和 BANNED
- [x] 修改 `validateNotAlreadyRejected` 检查 REJECTED 和 BANNED
- [x] 添加 `validateStateForBan` 方法
- [x] 字段名从 `rejectedReason` 改为 `reason`

**RoadmapService**
- [x] 添加 `reject(id)` 方法
- [x] 添加 `ban(id)` 方法
- [x] 修改 `approve` 使用枚举常量
- [x] 修改 `approveAndClearDescription` 使用枚举常量
- [x] RoadmapDataService 添加 `reject` 和 `ban` 方法

**PostService**
- [x] 修改 `approvePost(id, approve)` 方法
  - `approve=true` → APPROVED(2)
  - `approve=false` → REJECTED(3)
- [x] 添加 `rejectPost(id)` 方法（审核不通过）
- [x] 添加 `banPost(id)` 方法（用于违规封禁）
- [x] `deletePost(id)` 保持 BANNED（删除操作）
- [x] PostDataService 添加 `reject` 和 `ban` 方法
- [x] PostMapper 添加 `updateState` 方法

**其他 Service**
- [x] CommentService - 审核不通过设置为 REJECTED
  - [x] 修改 `approveComment` 方法
  - [x] 修改 `getCommentsByState` 方法
  - [x] 添加 `rejectComment` 方法
  - [x] 添加 `banComment` 方法
  - [x] CommentDataService 添加 `reject` 和 `ban` 方法
  - [x] CommentMapper 添加 `updateState` 方法
- [x] NodeService - 添加 approve、reject、ban 方法
  - [x] NodeDataService 添加 approve、reject、ban 方法
- [x] MemoryCardDeckService - 添加 rejectDeck 和 banDeck 方法
- [x] MemoryCardService - 不需要修改（删除操作使用 BANNED）

### 5. Controller 层修改

需要检查和修改所有审核相关的接口：

- [ ] CoursesController - 审核接口（已在之前修改）
- [x] PostsController - 审核接口（修改 state 映射）
- [ ] NodesController - 无审核接口
- [ ] ProfessionsController - 审核接口（已在之前修改）
- [x] CommentsController - 审核接口（无需修改，state 映射正确）

### 6. 前端代码修改

#### 状态常量定义
- [x] 修改 `web-ts/src/types/enums.ts` 中的 ContentState 定义
  - SUBMITTED: 0 → 1
  - APPROVED: 1 → 2
  - REJECTED: 3（新增）
  - BANNED: 2 → 4

#### 状态显示文本
- [x] PostReview.vue - 修改 rejected tab 对应 REJECTED
- [x] CommentReview.vue - 修改 rejected tab 对应 REJECTED

#### 操作按钮
- [x] PostReview.vue - 修改状态判断条件
- [x] CommentReview.vue - 修改状态判断条件

### 7. 错误码修改

检查是否需要修改错误提示：
- [ ] COURSE_ALREADY_REJECTED - 确认语义
- [ ] 其他相关错误码

## 修改顺序

1. **准备阶段**
   - [x] 修改 ContentState 枚举
   - [ ] 编写数据库迁移脚本（暂不执行）

2. **后端修改**
   - [x] 修改所有 Mapper 的硬编码状态值
   - [x] 修改 Service 层审核逻辑，区分 reject 和 ban
   - [x] 修改 Controller 层接口

3. **前端修改**
   - [x] 修改状态常量
   - [x] 修改状态显示文本
   - [x] 修改审核操作按钮

4. **数据迁移**（待执行）
   - [ ] 在测试环境执行迁移脚本
   - [ ] 验证迁移结果
   - [ ] 在生产环境执行迁移

5. **测试验证**（不需要）
   - [ ] 测试审核流程
   - [ ] 测试封禁流程
   - [ ] 测试重新提交流程

## 关键决策点

### 1. 现有 BANNED(2) 数据应该迁移到哪个状态？

**✅ 决策：全部迁移到 REJECTED(3)**
- 假设：现有的 BANNED(2) 都是"审核不通过"
- 优点：用户可以修改后重新提交
- 理由：目前系统中没有真正的违规封禁功能，所有 BANNED 都是审核拒绝

### 2. 是否保留 ban 功能？

**✅ 决策：保留 ban 功能**
- Service 层添加 `ban(id)` 方法，设置 state=4 (BANNED)
- 前端添加"封禁"按钮（管理员专用，用于处理严重违规）
- ban 和 reject 的区别：
  - reject: 审核不通过，用户可以修改后重新提交
  - ban: 违规封禁，一般不允许重新提交

### 3. approve 方法的 !approve 参数应该做什么？

**✅ 决策：approve=false 设置为 REJECTED(3)**

**CourseService.approve(id, approve)**
```java
if (approve) {
    // 通过 → APPROVED(2)
}
if (!approve) {
    // 不通过 → REJECTED(3)
}
```

**PostService.approvePost(id, approve)**
```java
if (approve) {
    // 通过 → APPROVED(2)
}
if (!approve) {
    // 不通过 → REJECTED(3)
}
```

## 风险点

1. **数据迁移失败**
   - 影响：所有内容状态错乱
   - 缓解：先在测试环境验证，备份数据

2. **前后端不同步**
   - 影响：状态显示错误
   - 缓解：前后端同时部署

3. **硬编码遗漏**
   - 影响：部分功能异常
   - 缓解：全局搜索所有硬编码的 0/1/2

## 搜索关键字

用于查找需要修改的代码：
- `ContentState.BANNED`
- `state = 0` / `state = 1` / `state = 2`
- `state == 0` / `state == 1` / `state == 2`
- `setState(0)` / `setState(1)` / `setState(2)`
- `approvePost` / `approve` / `reject` / `ban`
