# 记忆库管理接口文档 (MemoryBankController)

## 基本信息

- **Controller**: `MemoryBankController.java`
- **基础路径**: `/api/v1/memory/memory-bank`
- **Rate Limit**: 50 requests/minute (per user)
- **前端 API**: `web/src/api/modules/memory.ts`

## 概述

记忆库功能允许用户将卡片组添加到个人学习计划，通过 SRS (Spaced Repetition System) 算法管理学习和复习。用户可以调整每个课程的学习频率和状态。

**核心特性**:
- 支持添加卡片组到记忆库
- 按课程组织卡片组
- 可调整每日新卡数量和学习状态
- 基于 SRS 算法的智能复习安排

---

## DTO 类型说明

### AddDeckToMemoryBankRequest (添加卡片组请求)

用途：添加卡片组到用户记忆库

```json
{
  "deckId": 123,
  "courseId": 456
}
```

**字段说明**:
- `deckId` (Long, 必填): 卡片组ID
  - 验证规则：`@NotNull`, `@Positive`（必须大于0）
- `courseId` (Long, 必填): 课程ID
  - 验证规则：`@NotNull`, `@Positive`（必须大于0）

**使用场景**:
- 用户在课程内容页添加卡片组到学习计划
- 从节点页面或帖子页面添加卡片组

---

### UpdateCourseSettingRequest (更新课程设置请求)

用途：调整课程的学习频率和状态

```json
{
  "courseId": 456,
  "frequencySetting": 20,
  "status": 1
}
```

**字段说明**:
- `courseId` (Long, 必填): 课程ID
  - 验证规则：`@NotNull`
- `frequencySetting` (Integer, 可选): 每天新学卡片数量
  - 建议范围：5-50
  - 默认：20
- `status` (Integer, 可选): 学习状态
  - `0` = 暂停学习
  - `1` = 学习中

**说明**:
- 可以只更新其中一个字段
- 也可以同时更新两个字段

**使用场景**:
- 复习页面调整课程学习设置

---

### CourseMemoryBankDTO (课程记忆库信息)

用途：记忆库课程列表，包含课程信息、SRS设置和学习统计

```json
{
  "course": {
    "id": 456,
    "name": "Java核心技术"
  },
  "setting": {
    "id": 789,
    "frequencySetting": 20,
    "state": 1
  },
  "cardCount": 150,
  "dueCardCount": 12,
  "newCardCount": 30,
  "reviewCardCount": 100,
  "learnedCardCount": 20
}
```

**字段说明**:
- `course` (CourseBriefDTO): 课程基本信息
  - `id` (Long): 课程ID
  - `name` (String): 课程名称
- `setting` (UserCourseSrsSettingDTO): 用户SRS设置
  - `id` (Long): 设置ID
  - `frequencySetting` (Integer): 每天新卡数量
  - `state` (Integer): 学习状态（0=暂停, 1=学习中）
- `cardCount` (Integer): 在学卡片总数
- `dueCardCount` (Integer): 今日到期卡片数
- `newCardCount` (Integer): 新卡片数（从未学习过）
- `reviewCardCount` (Integer): 复习中卡片数（已学但未掌握）
- `learnedCardCount` (Integer): 已掌握卡片数

**数量关系**:
```
cardCount = newCardCount + reviewCardCount + learnedCardCount
dueCardCount ⊆ cardCount (今日需要复习的子集)
```

**使用场景**:
- `ReviewPage.vue` - 复习页面显示所有记忆库课程

---

## 接口列表

## 1. 添加卡片组到记忆库

**接口路径**: `POST /api/v1/memory/memory-bank/decks`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**: `AddDeckToMemoryBankRequest`
- `deckId` (Long, 必填): 卡片组ID，必须大于0
- `courseId` (Long, 必填): 课程ID，必须大于0

**返回类型**: `Void`

**业务逻辑说明**:

### 添加流程
1. **参数验证**
   - 验证 deckId 和 courseId 有效性

2. **验证资源存在**
   - 验证卡片组是否存在
   - 验证课程是否存在

3. **检查重复添加**
   - 查询卡片组是否已在记忆库中
   - 如果已存在，返回错误

4. **添加到记忆库**
   - 创建用户-课程-卡片组的关联关系
   - 为卡片组中的所有卡片创建学习记录
   - 初始化卡片状态为"新卡片"

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功"
}
```

**前端调用**:
```typescript
memoryApi.addDeckToMemoryBank({ deckId, courseId })
```

**使用场景**:
- `ContentReadPage.vue:614` - 课程内容页，点击卡片组的"添加到学习"按钮
- `NodePostsPage.vue` - 节点页面添加卡片组
- `PostDetailPage.vue` - 帖子详情页添加卡片组

**错误情况**:
- deckId或courseId为空/≤0: 1002 (INVALID_PARAMETER)
- 卡片组不存在: 对应错误码
- 课程不存在: 对应错误码
- 卡片组已在记忆库: 对应错误码
- 未登录: 1101 (USER_NOT_LOGIN)

---

## 2. 获取记忆库课程列表

**接口路径**: `GET /api/v1/memory/memory-bank/courses`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | Integer | 否 | 状态筛选（0-暂停，1-学习中），不传返回所有 |

**返回类型**: `List<CourseMemoryBankDTO>`

返回字段说明：
- `course` (CourseBriefDTO): 课程基本信息
  - `id` (Long): 课程ID
  - `name` (String): 课程名称
- `setting` (UserCourseSrsSettingDTO): 用户SRS设置
  - `id` (Long): 设置ID
  - `frequencySetting` (Integer): 每天新卡数量
  - `state` (Integer): 学习状态（0=暂停, 1=学习中）
- `cardCount` (Integer): 在学卡片总数
- `dueCardCount` (Integer): 今日到期卡片数
- `newCardCount` (Integer): 新卡片数（从未学习过）
- `reviewCardCount` (Integer): 复习中卡片数（已学但未掌握）
- `learnedCardCount` (Integer): 已掌握卡片数

**业务逻辑说明**:

### 查询流程
1. **查询用户记忆库**
   - 查询用户的所有记忆库课程
   - 如果传入status参数，过滤指定状态的课程

2. **统计卡片数据**
   - 批量查询每个课程的卡片统计
   - cardCount: 总卡片数
   - dueCardCount: 今日到期数
   - newCardCount, reviewCardCount, learnedCardCount

3. **返回结果**
   - 返回课程列表及完整统计信息

**返回示例**:
```json
{
  "code": 200,
  "data": [
    {
      "course": {
        "id": 456,
        "name": "Java核心技术"
      },
      "setting": {
        "id": 789,
        "frequencySetting": 20,
        "state": 1
      },
      "cardCount": 150,
      "dueCardCount": 12,
      "newCardCount": 30,
      "reviewCardCount": 100,
      "learnedCardCount": 20
    }
  ]
}
```

**前端调用**:
```typescript
memoryApi.getMemoryBankCourses()
```

**使用场景**:
- `ReviewPage.vue:860` - 复习页面加载课程列表，显示左侧面板

**前端使用详情**:
- 页面加载时立即调用 (immediate: true)
- 显示所有记忆库课程
- 显示每个课程的到期卡片数
- 用于课程选择和学习进度展示

---

## 3. 更新课程复习策略

**接口路径**: `PUT /api/v1/memory/memory-bank/courses/{courseId}/settings`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| courseId | Long | 是 | 课程ID，必须大于0 |

**请求体**: `UpdateCourseSettingRequest`
- `courseId` (Long, 必填): 课程ID
- `frequencySetting` (Integer, 可选): 每天新学卡片数量，建议范围5-50，默认20
- `status` (Integer, 可选): 学习状态，0=暂停学习，1=学习中

**返回类型**: `Void`

**业务逻辑说明**:

### 更新流程
1. **参数验证**
   - 验证 courseId 大于0

2. **验证权限**
   - 验证用户是否有该课程的记忆库

3. **更新设置**
   - 如果传入 frequencySetting，更新每日新卡数量
   - 如果传入 status，更新学习状态
   - 立即生效

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功"
}
```

**前端调用**:
```typescript
memoryApi.updateCourseMemorySetting({
  courseId,
  status,
  frequencySetting
})
```

**使用场景**:
- `ReviewPage.vue:1106` - 复习页面调整课程设置，保存时调用

**前端使用详情**:
- 用户点击课程设置按钮
- 修改每日新卡数量或学习状态
- 点击保存触发此接口
- 成功后显示"更新成功"提示

**错误情况**:
- courseId为空或≤0: 1002 (INVALID_PARAMETER)
- 用户没有该课程: 对应错误码
- 未登录: 1101 (USER_NOT_LOGIN)

---

## 4. 移除卡片组

**接口路径**: `DELETE /api/v1/memory/memory-bank/courses/{courseId}/decks/{deckId}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| courseId | Long | 是 | 课程ID，必须大于0 |
| deckId | Long | 是 | 卡片组ID，必须大于0 |

**返回类型**: `Void`

**业务逻辑说明**:

### 移除流程
1. **参数验证**
   - 验证 courseId 和 deckId 有效性

2. **验证权限**
   - 验证用户是否拥有该卡片组

3. **移除卡片组**
   - 从记忆库删除卡片组
   - 删除该卡片组下所有卡片的学习记录
   - 更新课程统计数据

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功"
}
```

**前端调用**:
```typescript
// 前端暂未封装此接口
client.delete(`/v1/memory/memory-bank/courses/${courseId}/decks/${deckId}`)
```

**使用场景**:
- 记忆库管理页面（未实现）
- 卡片组管理功能

**注意**:
- ⚠️ 不可逆操作，会永久删除学习进度
- 建议使用"暂停课程"替代临时不学的情况

**错误情况**:
- courseId或deckId为空/≤0: 1002 (INVALID_PARAMETER)
- 用户没有该卡片组: 对应错误码
- 未登录: 1101 (USER_NOT_LOGIN)

---

## SRS 系统说明

### 复习频率设置 (frequencySetting)

**含义**: 每天学习多少张新卡片

**推荐值**:
- **5-10张/天**: 轻度学习（业余学习）
- **15-20张/天**: 标准学习（默认推荐）
- **30-50张/天**: 强化学习（考试冲刺）

**影响**: 值越大学习速度越快，但每日复习压力越大

---

### 学习状态 (state)

**0 (暂停)**:
- 不产生新的学习任务
- 已有到期卡片仍可复习
- dueCardCount 不再增加
- 适用于暂时没时间学习的课程

**1 (学习中)**:
- 每天按 frequencySetting 产生新卡片
- 到期卡片正常进入复习队列
- dueCardCount 每天更新

---

### 卡片状态分类

**新卡片 (newCardCount)**:
- 从未学习过的卡片
- 学习次数 = 0
- 等待首次学习

**复习中 (reviewCardCount)**:
- 已学习但未掌握的卡片
- 学习次数 > 0，间隔 < 30天
- 需要定期复习巩固

**已学会 (learnedCardCount)**:
- 已掌握的卡片
- 学习次数 ≥ 5次，间隔 ≥ 30天
- 定期复习防止遗忘

**到期卡片 (dueCardCount)** ⭐:
- 今天需要复习的卡片
- 包含：今日新卡片 + 到期复习卡片
- 计算：`min(frequencySetting, newCardCount) + 到期复习卡片数`

---

## 使用流程

```
步骤1: 浏览课程/节点 → 发现卡片组
   ↓
步骤2: 添加到记忆库
   POST /memory-bank/decks
   ↓
步骤3: 查看记忆库课程
   GET /memory-bank/courses
   ↓
步骤4: 调整学习设置（可选）
   PUT /courses/{courseId}/settings
   ↓
步骤5: 开始复习
   (使用 ReviewController)
```

---

## 相关接口

### ReviewController (复习功能)
- `GET /api/v1/memory/review/queue` - 获取复习队列
- `POST /api/v1/memory/review/submit` - 提交复习结果
- `GET /api/v1/memory/review/stats` - 获取复习统计

### DecksController (卡片组管理)
- `GET /api/v1/memory/decks/node/{nodeId}` - 获取节点下的卡片组
- `POST /api/v1/memory/decks` - 创建卡片组
- `GET /api/v1/memory/decks/{deckId}` - 获取卡片组详情

---

*此文档用于指导前后端开发和接口对接*
