# 复习功能接口文档 (ReviewController)

## 基本信息

- **Controller**: `ReviewController.java`
- **基础路径**: `/api/v1/memory/review`
- **Rate Limit**: 50 requests/minute (per user)
- **认证要求**: 所有接口都需要登录

## DTO 类型说明

### UserCardSrsDTO (SRS 学习状态)
用途：记录用户对单张卡片的学习状态，基于 SuperMemo-2 算法实现

```json
{
  "id": 5001,
  "type": 2,
  "currentStep": 0,
  "interval": 7,
  "reviewDueAt": "2024-01-20 10:00:00",
  "lastReviewedAt": "2024-01-13 10:00:00",
  "repetitions": 3,
  "lapseCount": 1
}
```

**字段说明**：
- `id` (Long): SRS 状态记录ID
- `type` (Byte): 卡片状态
  - `0`: NEW（新卡片，从未学习过）
  - `1`: LEARNING（学习中，首次学习阶段）
  - `2`: REVIEW（复习阶段，已掌握的卡片）
  - `3`: RELEARNING（重新学习，因遗忘而重学）
- `currentStep` (Byte): 当前学习步骤索引
  - 仅在 `type=1 (LEARNING)` 或 `type=3 (RELEARNING)` 时有意义
  - 表示当前处于第几个学习步骤（从 0 开始）
  - 例如学习步骤配置为 `[1, 10, 60]` 分钟：
    - `currentStep=0`: 1 分钟后复习
    - `currentStep=1`: 10 分钟后复习
    - `currentStep=2`: 60 分钟后复习
- `interval` (Integer): 复习间隔
  - **单位取决于 type**：
    - `type=1 (LEARNING)` 或 `type=3 (RELEARNING)`: 单位为**分钟**
    - `type=2 (REVIEW)`: 单位为**天**
  - 表示距离下次复习的时间间隔
  - 由 SRS 算法根据用户的复习表现自动计算
- `reviewDueAt` (String): 下次复习时间，格式：`yyyy-MM-dd HH:mm:ss`
- `lastReviewedAt` (String): 上次复习时间，格式：`yyyy-MM-dd HH:mm:ss`
- `repetitions` (Integer): 成功复习次数
- `lapseCount` (Integer): 遗忘次数（答错次数）

**使用场景**：
- 所有卡片相关接口的学习状态字段
- 展示卡片学习进度
- 计算下次复习时间

**SRS 状态转换图**：
```
NEW (0)
  ↓ (开始学习)
LEARNING (1)
  ↓ (完成学习步骤)
REVIEW (2) ←→ RELEARNING (3)
  ↑ (答对)    ↓ (答错)
```

---

### UserBriefDTO (用户简要信息)
用途：基础用户信息（ID、名称、头像）

```json
{
  "id": 1001,
  "name": "张三",
  "avatar": "https://example.com/avatar.jpg"
}
```

**字段说明**：
- `id` (Long): 用户ID
- `name` (String): 用户名
- `avatar` (String): 头像URL

**使用场景**：
- CardWithSrsDTO 中的 creator 字段
- 显示卡片创建者信息

---

### DeckSummaryDTO (卡片组摘要)
用途：基础卡片组信息

```json
{
  "id": 10,
  "postId": 50,
  "nodeId": 100,
  "courseId": 200,
  "title": "Java 核心概念",
  "description": "Java 基础知识卡片组",
  "state": 1,
  "updatedAt": "2024-01-15 10:00:00",
  "createdAt": "2024-01-01 10:00:00",
  "upvoteCount": 25,
  "cardCount": 30,
  "course": {
    "id": 200,
    "name": "Java 编程入门"
  },
  "node": {
    "id": 100,
    "name": "第一章：基础语法"
  }
}
```

**字段说明**：
- `id` (Long): 卡片组ID
- `postId` (Long): 关联的帖子ID
- `nodeId` (Long): 关联的节点ID
- `courseId` (Long): 所属课程ID
- `title` (String): 卡片组标题
- `description` (String): 卡片组描述
- `state` (Integer): 卡片组状态
  - `0`: 草稿
  - `1`: 已发布
- `updatedAt` (String): 更新时间，格式：`yyyy-MM-dd HH:mm:ss`
- `createdAt` (String): 创建时间，格式：`yyyy-MM-dd HH:mm:ss`
- `upvoteCount` (Integer): 点赞数
- `cardCount` (Integer): 卡片数量
- `course` (CourseBriefDTO, 可选): 课程简要信息
  - `id`: 课程ID
  - `name`: 课程名称
- `node` (NodeBriefDTO, 可选): 节点简要信息
  - `id`: 节点ID
  - `name`: 节点名称

**使用场景**：
- CardWithSrsDTO 中的 deck 字段
- 显示卡片所属卡片组信息

---

### CardWithSrsDTO (包含 SRS 状态的卡片)
用途：完整的卡片数据结构，包含卡片内容、所属卡片组、创建者信息和 SRS 学习状态

**继承关系**: CardContentDTO → CardWithDeckDTO → CardWithCreatorDTO → CardWithSrsDTO

```json
{
  "id": 1,
  "front": "什么是 Java 虚拟机？",
  "back": "JVM (Java Virtual Machine) 是 Java 程序的运行环境，它负责将字节码翻译为机器码并执行。JVM 提供了内存管理、垃圾回收、安全性等功能。",
  "deck": {
    "id": 10,
    "title": "Java 核心概念",
    "description": "Java 基础知识卡片组",
    "courseId": 200
  },
  "creator": {
    "id": 1001,
    "name": "张三",
    "avatar": "https://example.com/avatar.jpg"
  },
  "srsState": {
    "id": 5001,
    "type": 2,
    "currentStep": 0,
    "interval": 7,
    "reviewDueAt": "2024-01-20 10:00:00",
    "lastReviewedAt": "2024-01-13 10:00:00",
    "repetitions": 3,
    "lapseCount": 1
  },
  "hasDeckUpdate": false,
  "hasCardUpdate": false
}
```

**完整字段说明**：

**从 CardContentDTO 继承的字段**：
- `id` (Long): 卡片ID
- `front` (String): 卡片正面内容（问题），支持 Markdown 格式
- `back` (String): 卡片背面内容（答案），支持 Markdown 格式

**从 CardWithDeckDTO 继承的字段**：
- `deck` (DeckSummaryDTO): 所属卡片组信息

**从 CardWithCreatorDTO 继承的字段**：
- `creator` (UserBriefDTO): 卡片创建者信息

**CardWithSrsDTO 特有字段**：
- `srsState` (UserCardSrsDTO): 用户的学习状态
  - 动态填充：根据当前登录用户ID和卡片ID查询 SRS 状态
  - 包含完整的学习进度信息
- `hasDeckUpdate` (Boolean): 卡片组是否有更新
  - `true`: 卡片组的标题或描述已修改
  - `false`: 卡片组未修改
  - 用于提醒用户卡片组内容已更新
- `hasCardUpdate` (Boolean): 卡片内容是否有更新
  - `true`: 卡片的正面或背面内容已修改
  - `false`: 卡片内容未修改
  - 用于提醒用户卡片内容已更新

**使用场景**：
- 复习队列接口返回值
- 卡片列表接口返回值
- 需要显示完整卡片信息和学习状态的场景

---

### ReviewStatsDTO (复习统计信息)
用途：用户在指定时间段内的复习统计数据

```json
{
  "totalReviews": 156,
  "streakDays": 7,
  "averageScore": 3.2,
  "timeSpent": 2340
}
```

**字段说明**：
- `totalReviews` (Integer): 复习总次数
  - 指定时间段内的复习次数总和
- `streakDays` (Integer): 连续学习天数
  - 从今天往前推，连续学习的天数
  - 中断一天则重置为 0
- `averageScore` (Double): 平均得分
  - 所有复习评分的平均值
  - 范围：1.0 - 4.0
- `timeSpent` (Integer): 总耗时
  - 所有复习的耗时总和
  - 单位：秒

**使用场景**：
- 复习统计接口返回值
- 用户个人中心的学习数据展示
- 学习进度报表

---

## 接口列表

## 1. 获取复习队列

**接口路径**: `GET /api/v1/memory/review/queue`

**是否需要登录**: 是 (`@SaCheckLogin`)

**功能说明**: 获取当前用户需要复习的卡片列表（只返回已到期的卡片）

**查询参数**:
- `courseId` (Long, 可选): 课程ID，筛选特定课程的卡片
  - 不传: 返回所有课程的到期卡片
  - 传入课程ID: 只返回该课程的到期卡片
  - 验证规则: 必须大于 0

**固定行为**:
- 只返回到期的卡片（`reviewDueAt <= 当前时间`）
- 最多返回 20 张卡片
- 不支持分页

**返回数据** (`List<CardWithSrsDTO>`):

成功响应 (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "front": "什么是 Java 虚拟机？",
      "back": "JVM (Java Virtual Machine) 是 Java 程序的运行环境...",
      "deck": {
        "id": 10,
        "title": "Java 核心概念",
        "description": "Java 基础知识卡片组",
        "courseId": 100
      },
      "creator": {
        "id": 1001,
        "name": "张三",
        "avatar": "https://example.com/avatar.jpg"
      },
      "srsState": {
        "id": 5001,
        "type": 2,
        "currentStep": 0,
        "interval": 7,
        "reviewDueAt": "2024-01-20 10:00:00",
        "lastReviewedAt": "2024-01-13 10:00:00",
        "repetitions": 3,
        "lapseCount": 1
      },
      "hasDeckUpdate": false,
      "hasCardUpdate": false
    }
  ]
}
```

空队列 (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": []
}
```

**错误响应**:

参数错误 (400):
```json
{
  "code": 400,
  "message": "课程ID必须大于0",
  "data": null
}
```

未登录 (401):
```json
{
  "code": 401,
  "message": "未登录",
  "data": null
}
```

**前端使用场景**:
1. **复习页面** (`/review`)
   - 页面加载时调用此接口获取待复习卡片
   - 显示卡片数量和进度条
   - 逐个展示卡片进行复习

2. **首页/仪表板** (`/dashboard`)
   - 显示待复习卡片总数
   - 提供一键开始复习的入口

3. **课程详情页** (`/courses/:id`)
   - 展示该课程的待复习卡片数量
   - 提供快速复习入口

**实现示例**:
```typescript
// 获取复习队列
async function loadReviewQueue(courseId?: number) {
  const params = new URLSearchParams();
  if (courseId) {
    params.append('courseId', courseId.toString());
  }

  const response = await fetch(
    `/api/v1/memory/review/queue?${params}`,
    { headers: { 'token': userToken } }
  );
  const { data: cards } = await response.json();

  if (cards.length === 0) {
    showMessage('🎉 今天没有需要复习的卡片！');
    return;
  }

  // 初始化复习会话
  startReviewSession(cards);
}
```

---

## 2. 获取卡片列表

**接口路径**: `GET /api/v1/memory/review/cards`

**是否需要登录**: 是 (`@SaCheckLogin`)

**功能说明**: 获取用户的所有卡片，支持分页查询（包含未到期的卡片）

**查询参数**:
- `courseId` (Long, 可选): 课程ID，筛选特定课程的卡片
  - 验证规则: 必须大于 0
- `lastId` (Long, 可选): 上一页最后一条记录的ID，用于游标分页
  - 不传: 加载第一页
  - 传具体ID: 加载该ID之后的数据
  - 验证规则: 必须大于 0

**固定行为**:
- 每次返回 20 张卡片
- 包含未到期的卡片

**返回数据** (`List<CardWithSrsDTO>`):

成功响应 (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "front": "什么是 Java 虚拟机？",
      "back": "JVM (Java Virtual Machine)...",
      "deck": { ... },
      "creator": { ... },
      "srsState": {
        "id": 5001,
        "type": 2,
        "reviewDueAt": "2024-01-25 10:00:00",
        ...
      },
      "hasDeckUpdate": false,
      "hasCardUpdate": false
    }
  ]
}
```

**错误响应**:

参数错误 (400):
```json
{
  "code": 400,
  "message": "课程ID必须大于0",
  "data": null
}
```

```json
{
  "code": 400,
  "message": "最后ID必须大于0",
  "data": null
}
```

**分页说明**:

使用基于游标的分页（cursor-based pagination）:

1. **第一页**: 不传 `lastId` 参数
2. **下一页**: 传入当前页最后一条记录的 `id` 作为 `lastId`
3. **判断是否还有数据**: 如果返回的数组长度小于 20，说明已到最后一页

**前端使用场景**:
1. **卡片库页面** (`/cards` 或 `/my-cards`)
   - 浏览用户的所有卡片
   - 查看学习进度
   - 手动复习特定卡片
   - 支持按课程筛选
   - 支持无限滚动或加载更多

2. **卡片详情弹窗**
   - 显示卡片内容
   - 显示 SRS 学习状态
   - 提供复习按钮

**实现示例**:
```typescript
// 加载卡片列表（支持分页）
async function loadCardList(courseId?: number, lastId?: number) {
  const params = new URLSearchParams();
  if (courseId) params.append('courseId', courseId.toString());
  if (lastId) params.append('lastId', lastId.toString());

  const response = await fetch(
    `/api/v1/memory/review/cards?${params}`,
    { headers: { 'token': userToken } }
  );
  const { data: cards } = await response.json();

  renderCardList(cards);

  // 如果返回 20 张，说明可能还有更多
  if (cards.length === 20) {
    showLoadMoreButton(cards[cards.length - 1].id);
  }
}

// 格式化间隔显示
function formatInterval(srsState: UserCardSrsDTO): string {
  const { type, interval } = srsState;

  // LEARNING (1) 和 RELEARNING (3) 的单位是分钟
  if (type === 1 || type === 3) {
    if (interval < 60) {
      return `${interval} 分钟`;
    } else {
      return `${Math.floor(interval / 60)} 小时`;
    }
  }

  // REVIEW (2) 的单位是天
  if (type === 2) {
    if (interval === 1) {
      return '1 天';
    } else if (interval < 30) {
      return `${interval} 天`;
    } else if (interval < 365) {
      return `${Math.floor(interval / 30)} 个月`;
    } else {
      return `${Math.floor(interval / 365)} 年`;
    }
  }

  return '-';
}
```

---

## 3. 提交复习结果

**接口路径**: `POST /api/v1/memory/review/submit`

**是否需要登录**: 是 (`@SaCheckLogin`)

**功能说明**: 提交单张卡片的复习结果，系统会根据 SRS 算法更新卡片状态

**请求体** (`ReviewCardRequest`):
```json
{
  "cardId": 1,
  "result": 3,
  "timeSpent": 15
}
```

**字段说明**：
- `cardId` (Long, 必填): 卡片ID
- `result` (Integer, 必填): 复习结果评分
  - `1`: Again（完全不记得，需要重新学习）
  - `2`: Hard（困难，勉强想起来）
  - `3`: Good（良好，正常回忆）
  - `4`: Easy（简单，轻松回忆）
  - 验证规则: 必须在 1-4 之间
- `timeSpent` (Integer, 可选): 复习耗时
  - 单位：秒
  - 用于统计学习时长

**返回数据** (`Void`):

成功响应 (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

**错误响应**:

参数错误 (400):
```json
{
  "code": 400,
  "message": "卡片ID不能为空",
  "data": null
}
```

```json
{
  "code": 400,
  "message": "复习结果不能为空",
  "data": null
}
```

```json
{
  "code": 400,
  "message": "复习结果值不正确",
  "data": null
}
```

业务错误:

卡片不存在 (404):
```json
{
  "code": 404,
  "message": "卡片不存在",
  "data": null
}
```

卡片不属于当前用户 (403):
```json
{
  "code": 403,
  "message": "无权访问该卡片",
  "data": null
}
```

**SRS 算法说明**:

系统使用改进的 SM-2 间隔重复算法：

| 评分 | 名称 | 行为 |
|------|------|------|
| 1 | Again | 重置学习进度，进入重新学习阶段 |
| 2 | Hard | 适度增加间隔，降低难度系数 |
| 3 | Good | 正常增加间隔 |
| 4 | Easy | 大幅增加间隔，跳过部分学习步骤 |

**前端使用场景**:
1. **复习页面** (`/review`)
   - 用户点击评分按钮后调用此接口
   - 提交结果后移动到下一张卡片
   - 记录复习耗时

**实现示例**:
```typescript
// 复习流程
let answerStartTime: number;

// 显示答案
function showAnswer() {
  cardBackElement.innerHTML = renderMarkdown(currentCard.back);
  cardBackElement.style.display = 'block';
  showRatingButtons(); // 显示评分按钮 (Again, Hard, Good, Easy)
  answerStartTime = Date.now();
}

// 提交复习结果
async function submitReview(rating: number) {
  const timeSpent = Math.floor((Date.now() - answerStartTime) / 1000);

  await fetch('/api/v1/memory/review/submit', {
    method: 'POST',
    headers: {
      'token': userToken,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      cardId: currentCard.id,
      result: rating,
      timeSpent: timeSpent
    })
  });

  // 移动到下一张卡片
  reviewSession.completed++;
  reviewSession.currentIndex++;

  if (reviewSession.currentIndex < reviewSession.cards.length) {
    showCard(reviewSession.cards[reviewSession.currentIndex]);
  } else {
    showCompletionSummary();
  }
}
```

**UI 建议**:
- 评分按钮使用不同颜色区分（红色-Again, 橙色-Hard, 绿色-Good, 蓝色-Easy）
- 支持键盘快捷键（1-4 对应四个评分）
- 显示每个评分对应的下次复习时间预览

---

## 4. 获取复习统计

**接口路径**: `GET /api/v1/memory/review/stats`

**是否需要登录**: 是 (`@SaCheckLogin`)

**功能说明**: 获取用户在指定时间段内的复习统计数据

**查询参数**:
- `period` (String, 可选): 统计时间段
  - 默认值: `WEEK`
  - 可选值:
    - `DAY`: 今日统计
    - `WEEK`: 本周统计（默认）
    - `MONTH`: 本月统计
    - `YEAR`: 本年统计

**返回数据** (`ReviewStatsDTO`):

成功响应 (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalReviews": 156,
    "streakDays": 7,
    "averageScore": 3.2,
    "timeSpent": 2340
  }
}
```

无数据响应 (200 OK):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalReviews": 0,
    "streakDays": 0,
    "averageScore": 0.0,
    "timeSpent": 0
  }
}
```

**错误响应**:

参数错误 (400):
```json
{
  "code": 400,
  "message": "无效的时间段参数",
  "data": null
}
```

**前端使用场景**:
1. **学习统计页面** (`/stats`)
   - 展示用户的学习数据和进度
   - 显示今日/本周/本月/本年的统计数据
   - 绘制学习趋势图表

2. **用户个人中心**
   - 显示学习概览卡片
   - 展示连续学习天数
   - 激励用户持续学习

3. **首页/仪表板**
   - 今日学习摘要
   - 连续学习天数（带火焰图标）

**实现示例**:
```typescript
// 加载统计数据
async function loadStats() {
  const [dayStats, weekStats, monthStats] = await Promise.all([
    fetchStats('DAY'),
    fetchStats('WEEK'),
    fetchStats('MONTH')
  ]);

  renderStatsCards({
    today: dayStats,
    thisWeek: weekStats,
    thisMonth: monthStats
  });
}

async function fetchStats(period: string) {
  const response = await fetch(
    `/api/v1/memory/review/stats?period=${period}`,
    { headers: { 'token': userToken } }
  );
  return (await response.json()).data;
}

// 渲染统计卡片
function renderStatsCards(stats) {
  // 今日统计卡片
  todayCard.innerHTML = `
    <h3>📅 今日学习</h3>
    <div class="stat-item">
      <span>复习次数</span>
      <strong>${stats.today.totalReviews}</strong>
    </div>
    <div class="stat-item">
      <span>学习时长</span>
      <strong>${formatTime(stats.today.timeSpent)}</strong>
    </div>
    <div class="stat-item">
      <span>平均得分</span>
      <strong>${stats.today.averageScore.toFixed(1)} / 4.0</strong>
    </div>
  `;

  // 连续学习天数（大卡片）
  streakCard.innerHTML = `
    <h2>🔥 连续学习</h2>
    <div class="streak-number">${stats.today.streakDays}</div>
    <p>天</p>
  `;
}
```

**交互提示**:
- 连续学习达到里程碑时显示庆祝动画（7天、30天、100天等）
- 平均得分低于 2.5 时显示鼓励信息
- 今日未复习时显示提醒

---

## 已废弃的接口

### 批量提交复习结果 (已废弃)

**接口路径**: `POST /api/v1/memory/review/batch-submit`

**废弃原因**:
- 此接口未被前端使用
- 后端实现存在严重 bug
- 前端已采用单次提交模式（`POST /submit`），用户体验更好

**状态**: 已注释，不可用

**重要**: 如需重新启用此接口，必须先修复 `ReviewService.batchSubmitReview()` 的实现。

---

## 通用说明

### 认证方式

所有接口都需要在请求头中携带 token：

```
token: your-auth-token
```

### 限流处理

所有接口共享用户限流配置：
- 每个用户每分钟最多 50 次请求
- 超出限流后返回 429 Too Many Requests

```json
{
  "code": 429,
  "message": "请求过于频繁，请稍后再试",
  "data": null
}
```

### 错误码说明

| 错误码 | 说明 |
|-------|------|
| 200 | 操作成功 |
| 400 | 参数错误 |
| 401 | 未登录 |
| 403 | 无权访问 |
| 404 | 资源不存在 |
| 429 | 请求频率超限 |
| 500 | 系统错误 |

### 数据格式

- 时间格式: `yyyy-MM-dd HH:mm:ss`
- 字符编码: UTF-8
- 响应格式: JSON

---

## 完整复习流程示例

### 1. 复习页面完整实现

```typescript
// 页面状态
interface ReviewSession {
  cards: CardWithSrsDTO[];
  currentIndex: number;
  completed: number;
  startTime: number;
}

let reviewSession: ReviewSession;
let currentCard: CardWithSrsDTO;
let answerStartTime: number;

// 1. 页面加载时获取复习队列
async function loadReviewQueue(courseId?: number) {
  const params = new URLSearchParams();
  if (courseId) {
    params.append('courseId', courseId.toString());
  }

  const response = await fetch(
    `/api/v1/memory/review/queue?${params}`,
    { headers: { 'token': userToken } }
  );
  const { data: cards } = await response.json();

  if (cards.length === 0) {
    showMessage('🎉 今天没有需要复习的卡片！');
    return;
  }

  // 初始化复习会话
  reviewSession = {
    cards: cards,
    currentIndex: 0,
    completed: 0,
    startTime: Date.now()
  };

  showCard(cards[0]);
}

// 2. 展示卡片
function showCard(card: CardWithSrsDTO) {
  currentCard = card;

  // 显示卡片正面
  cardFrontElement.innerHTML = renderMarkdown(card.front);
  cardBackElement.style.display = 'none';

  // 显示卡片组信息
  deckNameElement.textContent = card.deck.title;

  // 显示进度
  progressElement.textContent =
    `${reviewSession.currentIndex + 1} / ${reviewSession.cards.length}`;

  // 显示更新提示
  if (card.hasCardUpdate) {
    showUpdateBadge('卡片内容已更新');
  }
  if (card.hasDeckUpdate) {
    showUpdateBadge('卡片组信息已更新');
  }

  // 显示 SRS 状态信息（可选）
  showSrsInfo(card.srsState);

  // 显示"显示答案"按钮
  showAnswerButton.style.display = 'block';
  ratingButtons.style.display = 'none';
}

// 3. 显示答案
function showAnswer() {
  cardBackElement.innerHTML = renderMarkdown(currentCard.back);
  cardBackElement.style.display = 'block';

  // 隐藏"显示答案"按钮，显示评分按钮
  showAnswerButton.style.display = 'none';
  ratingButtons.style.display = 'flex';

  // 记录开始时间（用于计算耗时）
  answerStartTime = Date.now();
}

// 4. 提交复习结果
async function submitReview(rating: number) {
  const timeSpent = Math.floor((Date.now() - answerStartTime) / 1000);

  await fetch('/api/v1/memory/review/submit', {
    method: 'POST',
    headers: {
      'token': userToken,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      cardId: currentCard.id,
      result: rating,
      timeSpent: timeSpent
    })
  });

  // 移动到下一张卡片
  reviewSession.completed++;
  reviewSession.currentIndex++;

  if (reviewSession.currentIndex < reviewSession.cards.length) {
    showCard(reviewSession.cards[reviewSession.currentIndex]);
  } else {
    showCompletionSummary();
  }
}

// 5. 显示完成总结
function showCompletionSummary() {
  const totalTime = Math.floor((Date.now() - reviewSession.startTime) / 1000);
  const minutes = Math.floor(totalTime / 60);
  const seconds = totalTime % 60;

  showMessage(`
    ✅ 复习完成！
    - 复习卡片: ${reviewSession.completed} 张
    - 用时: ${minutes} 分 ${seconds} 秒
  `);
}

// 6. 键盘快捷键
document.addEventListener('keydown', (e) => {
  // 空格键显示答案
  if (e.key === ' ' && showAnswerButton.style.display !== 'none') {
    e.preventDefault();
    showAnswer();
  }

  // 1-4 键对应评分
  if (['1', '2', '3', '4'].includes(e.key) && ratingButtons.style.display !== 'none') {
    submitReview(parseInt(e.key));
  }
});
```

### 2. UI 组件建议

```html
<div class="review-page">
  <!-- 进度条 -->
  <div class="progress-bar">
    <span id="progress">1 / 20</span>
    <div class="deck-name" id="deckName">Java 核心概念</div>
  </div>

  <!-- 卡片显示区 -->
  <div class="card-container">
    <div class="card-front" id="cardFront">
      <!-- 卡片正面内容 -->
    </div>
    <div class="card-back" id="cardBack" style="display: none;">
      <!-- 卡片背面内容 -->
    </div>
  </div>

  <!-- 更新提示 -->
  <div class="update-badges" id="updateBadges"></div>

  <!-- 显示答案按钮 -->
  <button id="showAnswerButton" class="btn-show-answer">
    显示答案 (空格)
  </button>

  <!-- 评分按钮组 -->
  <div id="ratingButtons" class="rating-buttons" style="display: none;">
    <button class="btn-again" onclick="submitReview(1)">
      <span>不记得</span>
      <kbd>1</kbd>
    </button>
    <button class="btn-hard" onclick="submitReview(2)">
      <span>困难</span>
      <kbd>2</kbd>
    </button>
    <button class="btn-good" onclick="submitReview(3)">
      <span>良好</span>
      <kbd>3</kbd>
    </button>
    <button class="btn-easy" onclick="submitReview(4)">
      <span>简单</span>
      <kbd>4</kbd>
    </button>
  </div>
</div>
```

---

## 版本历史

- **v1.0** (2024-01-01): 初始版本
  - 获取复习队列接口
  - 获取卡片列表接口
  - 提交复习结果接口
  - 获取复习统计接口
- **v1.1** (2024-06-01): 废弃批量提交接口
  - 注释批量提交接口（存在 bug，前端未使用）
