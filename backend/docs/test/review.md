# 复习功能接口测试文档 (ReviewController)

## 测试文件

- **测试类**: `ReviewControllerTest.java`
- **位置**: `learn-web/src/test/java/com/prosper/learn/web/v1/controller/`
- **API文档**: `docs/api/review.md`
- **Controller**: `ReviewController.java`

## 测试框架配置

- 使用 `@SpringBootTest` 和 `@AutoConfigureMockMvc`
- 使用 `@Transactional` 实现测试后自动回滚
- 继承 `BaseControllerTest`

### 依赖注入

需要注入以下服务：
- `MockMvc` - 用于发送 HTTP 请求
- `WebApplicationContext` - Web 应用上下文
- `ReviewService` - 复习服务
- `DeckService` - 卡片组服务（创建测试卡片组）
- `CardService` - 卡片服务（创建测试卡片）
- `UserDomainService` - 用户领域服务（创建测试用户）
- `CourseService` - 课程服务（创建测试课程）

---

## 测试数据准备

### 必需的测试辅助方法

需要创建以下辅助方法来准备测试数据：

1. **创建测试用户**
   - 功能：创建并返回一个测试用户
   - 参数：邮箱地址、密码
   - 返回：UserDO 对象（包含自动生成的 ID）

2. **创建测试课程**
   - 功能：创建一个测试课程
   - 参数：课程名称、创建者ID
   - 返回：CourseDO 对象

3. **创建测试卡片组**
   - 功能：创建一个测试卡片组
   - 参数：标题、描述、课程ID、创建者ID
   - 返回：DeckDO 对象

4. **创建测试卡片**
   - 功能：创建一张测试卡片
   - 参数：正面内容、背面内容、卡片组ID、创建者ID
   - 返回：CardDO 对象

5. **订阅卡片组**
   - 功能：用户订阅指定卡片组，生成 SRS 学习状态
   - 参数：用户ID、卡片组ID
   - 返回：订阅记录

6. **修改卡片到期时间**
   - 功能：修改卡片的 reviewDueAt 时间，用于测试到期/未到期场景
   - 参数：用户ID、卡片ID、到期时间
   - 返回：无

---

## 测试用例设计

## 1. 获取复习队列 (GET /api/v1/memory/review/queue)

### 1.1 正常流程测试

#### 测试场景 1.1.1：获取到期的卡片（有数据）
**测试目标**：验证用户可以获取到期的复习卡片

**前置条件**：
- 创建测试用户 user1
- 创建测试课程 course1
- 创建卡片组 deck1（属于 course1）
- 创建 3 张卡片（card1, card2, card3）
- user1 订阅 deck1
- 修改 card1 和 card2 的到期时间为过去（已到期）
- 修改 card3 的到期时间为未来（未到期）
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/review/queue`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- 响应消息：`"操作成功"`
- data 字段：
  - 返回数组，包含 2 张卡片（card1, card2）
  - 不包含未到期的 card3
  - 每张卡片包含：
    - id, front, back
    - deck（卡片组信息）
    - creator（创建者信息）
    - srsState（SRS 学习状态）
    - hasDeckUpdate, hasCardUpdate

---

#### 测试场景 1.1.2：获取空队列（无到期卡片）
**测试目标**：验证没有到期卡片时返回空数组

**前置条件**：
- 创建测试用户 user1
- user1 订阅了卡片组，但所有卡片都未到期
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/review/queue`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- data 字段：空数组 `[]`

---

#### 测试场景 1.1.3：按课程筛选到期卡片
**测试目标**：验证可以按课程ID筛选到期的卡片

**前置条件**：
- 创建测试用户 user1
- 创建两个课程 course1, course2
- course1 有 2 张到期卡片
- course2 有 1 张到期卡片
- user1 订阅了两个课程的卡片组
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/review/queue?courseId=course1.id`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- data 字段：
  - 返回数组，包含 2 张卡片（都属于 course1）
  - 不包含 course2 的卡片

---

#### 测试场景 1.1.4：验证返回数量限制（20张）
**测试目标**：验证接口最多返回 20 张卡片

**前置条件**：
- 创建测试用户 user1
- 创建 25 张到期卡片
- user1 订阅了这些卡片
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/review/queue`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- data 字段：
  - 返回数组，包含 20 张卡片
  - 不超过 20 张

---

### 1.2 参数验证测试

#### 测试场景 1.2.1：courseId 为 0
**前置条件**：用户已登录

**测试步骤**：
- 发送请求：`GET /api/v1/memory/review/queue?courseId=0`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："课程ID必须大于0"

---

#### 测试场景 1.2.2：courseId 为负数
**前置条件**：用户已登录

**测试步骤**：
- 发送请求：`GET /api/v1/memory/review/queue?courseId=-1`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："课程ID必须大于0"

---

### 1.3 权限验证测试

#### 测试场景 1.3.1：未登录访问
**测试目标**：验证未登录用户无法访问

**测试步骤**：
- 未登录状态
- 发送 GET 请求到 `/api/v1/memory/review/queue`

**预期结果**：
- HTTP 状态码：200
- 响应码：1001（USER_NOT_LOGIN）
- 响应消息：`"未登录"`

---

### 1.4 数据一致性测试

#### 测试场景 1.4.1：验证 SRS 状态字段完整性
**测试目标**：验证返回的卡片包含完整的 SRS 状态

**前置条件**：
- 创建测试数据，user1 有一张到期卡片
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/review/queue`
3. 检查返回的卡片数据

**预期结果**：
- srsState 字段存在且包含：
  - id（SRS 记录ID）
  - type（卡片状态：0-3）
  - currentStep（当前步骤）
  - interval（复习间隔）
  - reviewDueAt（下次复习时间）
  - lastReviewedAt（上次复习时间）
  - repetitions（成功复习次数）
  - lapseCount（遗忘次数）

---

#### 测试场景 1.4.2：验证卡片组和创建者信息
**测试目标**：验证返回的卡片包含关联的卡片组和创建者信息

**前置条件**：
- 创建测试数据，user1 有一张到期卡片
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/review/queue`
3. 检查返回的卡片数据

**预期结果**：
- deck 字段存在且包含：
  - id, title, description, courseId
- creator 字段存在且包含：
  - id, name, avatar

---

## 2. 获取卡片列表 (GET /api/v1/memory/review/cards)

### 2.1 正常流程测试

#### 测试场景 2.1.1：获取第一页卡片（默认20张）
**测试目标**：验证可以获取用户的所有卡片（包含未到期的）

**前置条件**：
- 创建测试用户 user1
- user1 订阅了卡片组，有 15 张卡片
- 其中 5 张已到期，10 张未到期
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/review/cards`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- data 字段：
  - 返回数组，包含 15 张卡片
  - 包含到期和未到期的卡片
  - 每张卡片包含完整信息

---

#### 测试场景 2.1.2：分页加载（使用 lastId）
**测试目标**：验证游标分页功能

**前置条件**：
- 创建测试用户 user1
- user1 有 25 张卡片
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/review/cards`，获取第一页
3. 提取最后一张卡片的 id
4. 发送 GET 请求到 `/api/v1/memory/review/cards?lastId={上一页最后的id}`

**预期结果**：
- 第一页：返回 20 张卡片
- 第二页：返回 5 张卡片
- 第二页的卡片 ID 都小于 lastId

---

#### 测试场景 2.1.3：按课程筛选卡片
**测试目标**：验证可以按课程ID筛选卡片

**前置条件**：
- 创建测试用户 user1
- 创建两个课程 course1, course2
- course1 有 8 张卡片
- course2 有 6 张卡片
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/review/cards?courseId=course1.id`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- data 字段：
  - 返回数组，包含 8 张卡片（都属于 course1）
  - 不包含 course2 的卡片

---

#### 测试场景 2.1.4：空列表（用户无卡片）
**测试目标**：验证用户无卡片时返回空数组

**前置条件**：
- 创建测试用户 user1
- user1 没有订阅任何卡片组
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/review/cards`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- data 字段：空数组 `[]`

---

### 2.2 参数验证测试

#### 测试场景 2.2.1：courseId 为 0
**前置条件**：用户已登录

**测试步骤**：
- 发送请求：`GET /api/v1/memory/review/cards?courseId=0`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："课程ID必须大于0"

---

#### 测试场景 2.2.2：courseId 为负数
**前置条件**：用户已登录

**测试步骤**：
- 发送请求：`GET /api/v1/memory/review/cards?courseId=-1`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："课程ID必须大于0"

---

#### 测试场景 2.2.3：lastId 为 0
**前置条件**：用户已登录

**测试步骤**：
- 发送请求：`GET /api/v1/memory/review/cards?lastId=0`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："最后ID必须大于0"

---

#### 测试场景 2.2.4：lastId 为负数
**前置条件**：用户已登录

**测试步骤**：
- 发送请求：`GET /api/v1/memory/review/cards?lastId=-1`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："最后ID必须大于0"

---

### 2.3 权限验证测试

#### 测试场景 2.3.1：未登录访问
**测试目标**：验证未登录用户无法访问

**测试步骤**：
- 未登录状态
- 发送 GET 请求到 `/api/v1/memory/review/cards`

**预期结果**：
- HTTP 状态码：200
- 响应码：1001（USER_NOT_LOGIN）
- 响应消息：`"未登录"`

---

## 3. 提交复习结果 (POST /api/v1/memory/review/submit)

### 3.1 正常流程测试

#### 测试场景 3.1.1：提交复习结果（Good - 评分3）
**测试目标**：验证可以成功提交复习结果，SRS 状态正确更新

**前置条件**：
- 创建测试用户 user1
- user1 有一张到期卡片 card1
- card1 的 SRS 状态为 REVIEW（type=2）
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 POST 请求到 `/api/v1/memory/review/submit`
3. 请求体：`{"cardId": card1.id, "result": 3, "timeSpent": 15}`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- 响应消息：`"操作成功"`
- 数据库验证：
  - SRS 状态已更新
  - interval 增加
  - reviewDueAt 推迟到未来
  - lastReviewedAt 更新为当前时间
  - repetitions 加 1

---

#### 测试场景 3.1.2：提交复习结果（Again - 评分1）
**测试目标**：验证评分为 Again 时，卡片进入重新学习状态

**前置条件**：
- 创建测试用户 user1
- user1 有一张卡片 card1（REVIEW 状态）
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 POST 请求到 `/api/v1/memory/review/submit`
3. 请求体：`{"cardId": card1.id, "result": 1, "timeSpent": 30}`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- 数据库验证：
  - type 变更为 RELEARNING（type=3）
  - interval 重置为学习步骤的第一个值
  - lapseCount 加 1

---

#### 测试场景 3.1.3：提交复习结果（Easy - 评分4）
**测试目标**：验证评分为 Easy 时，间隔大幅增加

**前置条件**：
- 创建测试用户 user1
- user1 有一张卡片 card1（REVIEW 状态）
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 POST 请求到 `/api/v1/memory/review/submit`
3. 请求体：`{"cardId": card1.id, "result": 4, "timeSpent": 10}`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- 数据库验证：
  - interval 大幅增加（比 Good 更大）
  - reviewDueAt 推迟更久

---

#### 测试场景 3.1.4：提交复习结果（不传 timeSpent）
**测试目标**：验证 timeSpent 为可选参数

**前置条件**：
- 创建测试用户 user1
- user1 有一张卡片 card1
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 POST 请求到 `/api/v1/memory/review/submit`
3. 请求体：`{"cardId": card1.id, "result": 3}`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- 响应消息：`"操作成功"`
- SRS 状态正常更新

---

### 3.2 参数验证测试

#### 测试场景 3.2.1：cardId 为 null
**前置条件**：用户已登录

**测试步骤**：
- 发送请求体：`{"result": 3}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："卡片ID不能为空"

---

#### 测试场景 3.2.2：result 为 null
**前置条件**：用户已登录

**测试步骤**：
- 发送请求体：`{"cardId": 1}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："复习结果不能为空"

---

#### 测试场景 3.2.3：result 为 0（小于1）
**前置条件**：用户已登录

**测试步骤**：
- 发送请求体：`{"cardId": 1, "result": 0}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："复习结果值不正确"

---

#### 测试场景 3.2.4：result 为 5（大于4）
**前置条件**：用户已登录

**测试步骤**：
- 发送请求体：`{"cardId": 1, "result": 5}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："复习结果值不正确"

---

### 3.3 业务错误测试

#### 测试场景 3.3.1：卡片不存在
**前置条件**：用户已登录

**测试步骤**：
- 发送请求体：`{"cardId": 99999, "result": 3}`

**预期结果**：
- HTTP 状态码：200
- 响应码：404（NOT_FOUND）
- 响应消息：`"卡片不存在"`

---

#### 测试场景 3.3.2：卡片不属于当前用户
**前置条件**：
- 创建两个测试用户 user1, user2
- user2 有一张卡片 card1
- user1 已登录（但 card1 不属于 user1）

**测试步骤**：
- user1 尝试提交 user2 的卡片复习结果
- 发送请求体：`{"cardId": card1.id, "result": 3}`

**预期结果**：
- HTTP 状态码：200
- 响应码：403（FORBIDDEN）
- 响应消息：`"无权访问该卡片"`

---

### 3.4 权限验证测试

#### 测试场景 3.4.1：未登录提交
**测试目标**：验证未登录用户无法提交复习结果

**测试步骤**：
- 未登录状态
- 发送 POST 请求到 `/api/v1/memory/review/submit`
- 请求体：`{"cardId": 1, "result": 3}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1001（USER_NOT_LOGIN）
- 响应消息：`"未登录"`

---

## 4. 获取复习统计 (GET /api/v1/memory/review/stats)

### 4.1 正常流程测试

#### 测试场景 4.1.1：获取今日统计（period=DAY）
**测试目标**：验证可以获取今日的复习统计

**前置条件**：
- 创建测试用户 user1
- user1 今天完成了 5 次复习
- 平均评分 3.2
- 总耗时 300 秒
- 连续学习 7 天
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/review/stats?period=DAY`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- data 字段：
  - totalReviews: 5
  - averageScore: 3.2
  - timeSpent: 300
  - streakDays: 7

---

#### 测试场景 4.1.2：获取本周统计（period=WEEK，默认）
**测试目标**：验证可以获取本周的复习统计

**前置条件**：
- 创建测试用户 user1
- user1 本周完成了 20 次复习
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/review/stats`（不传 period，默认 WEEK）

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- data 字段：
  - totalReviews: 20
  - 包含本周的统计数据

---

#### 测试场景 4.1.3：获取本月统计（period=MONTH）
**测试目标**：验证可以获取本月的复习统计

**前置条件**：
- 创建测试用户 user1
- user1 本月完成了 80 次复习
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/review/stats?period=MONTH`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- data 字段：
  - totalReviews: 80
  - 包含本月的统计数据

---

#### 测试场景 4.1.4：获取本年统计（period=YEAR）
**测试目标**：验证可以获取本年的复习统计

**前置条件**：
- 创建测试用户 user1
- user1 今年完成了 500 次复习
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/review/stats?period=YEAR`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- data 字段：
  - totalReviews: 500
  - 包含今年的统计数据

---

#### 测试场景 4.1.5：无数据时返回零值
**测试目标**：验证用户无复习记录时返回零值

**前置条件**：
- 创建测试用户 user1
- user1 今天没有复习记录
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/review/stats?period=DAY`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- data 字段：
  - totalReviews: 0
  - streakDays: 0
  - averageScore: 0.0
  - timeSpent: 0

---

### 4.2 参数验证测试

#### 测试场景 4.2.1：period 参数无效
**前置条件**：用户已登录

**测试步骤**：
- 发送请求：`GET /api/v1/memory/review/stats?period=INVALID`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："无效的时间段参数"

---

### 4.3 权限验证测试

#### 测试场景 4.3.1：未登录访问
**测试目标**：验证未登录用户无法访问

**测试步骤**：
- 未登录状态
- 发送 GET 请求到 `/api/v1/memory/review/stats`

**预期结果**：
- HTTP 状态码：200
- 响应码：1001（USER_NOT_LOGIN）
- 响应消息：`"未登录"`

---

## 边界测试

### 5. 边界场景

#### 测试场景 5.1：复习队列达到上限（20张）
**测试目标**：验证复习队列最多返回 20 张卡片

**前置条件**：
- 创建测试用户 user1
- user1 有 30 张到期卡片
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/review/queue`

**预期结果**：
- 返回 20 张卡片，不超过上限

---

#### 测试场景 5.2：卡片列表分页边界
**测试目标**：验证分页在边界情况下的正确性

**前置条件**：
- 创建测试用户 user1
- user1 恰好有 20 张卡片
- user1 已登录

**测试步骤**：
1. 获取第一页（返回 20 张）
2. 使用最后一张卡片的 ID 获取第二页

**预期结果**：
- 第一页返回 20 张
- 第二页返回空数组（表示已到最后）

---

#### 测试场景 5.3：连续学习天数计算
**测试目标**：验证连续学习天数的正确计算

**前置条件**：
- 创建测试用户 user1
- user1 在过去 7 天每天都有复习记录
- 第 8 天没有复习（中断）
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/review/stats?period=DAY`

**预期结果**：
- streakDays: 7（从中断后重新计算）

---

## 参数验证汇总

### 6. 通用参数验证

| 接口 | 参数 | 无效值 | 预期错误码 |
|------|------|--------|-----------|
| 获取复习队列 | courseId | 0 | 1002 |
| 获取复习队列 | courseId | -1 | 1002 |
| 获取卡片列表 | courseId | 0 | 1002 |
| 获取卡片列表 | courseId | -1 | 1002 |
| 获取卡片列表 | lastId | 0 | 1002 |
| 获取卡片列表 | lastId | -1 | 1002 |
| 提交复习结果 | cardId | null | 1002 |
| 提交复习结果 | result | null | 1002 |
| 提交复习结果 | result | 0 | 1002 |
| 提交复习结果 | result | 5 | 1002 |
| 提交复习结果 | cardId | 99999 | 404 |
| 获取复习统计 | period | INVALID | 1002 |

---

## 权限测试

### 7. 权限验证

| 接口 | 未登录 | 预期错误码 |
|------|--------|-----------|
| GET /memory/review/queue | ❌ | 1001 |
| GET /memory/review/cards | ❌ | 1001 |
| POST /memory/review/submit | ❌ | 1001 |
| GET /memory/review/stats | ❌ | 1001 |

---

## SRS 算法测试

### 8. SRS 状态转换测试

#### 测试场景 8.1：NEW → LEARNING 转换
**测试目标**：验证新卡片首次复习后进入学习中状态

**前置条件**：
- 创建卡片，SRS 状态为 NEW（type=0）

**测试步骤**：
- 提交复习结果（result=3）

**预期结果**：
- type 变更为 LEARNING（type=1）
- currentStep = 0
- interval 设置为学习步骤的第一个值（分钟）

---

#### 测试场景 8.2：LEARNING → REVIEW 转换
**测试目标**：验证完成所有学习步骤后进入复习状态

**前置条件**：
- 创建卡片，SRS 状态为 LEARNING
- currentStep 为最后一个步骤

**测试步骤**：
- 提交复习结果（result=3）

**预期结果**：
- type 变更为 REVIEW（type=2）
- interval 单位变为天
- currentStep 重置

---

#### 测试场景 8.3：REVIEW → RELEARNING 转换
**测试目标**：验证答错后进入重新学习状态

**前置条件**：
- 创建卡片，SRS 状态为 REVIEW（type=2）

**测试步骤**：
- 提交复习结果（result=1，Again）

**预期结果**：
- type 变更为 RELEARNING（type=3）
- interval 重置为学习步骤的第一个值（分钟）
- lapseCount 加 1

---

#### 测试场景 8.4：RELEARNING → REVIEW 转换
**测试目标**：验证重新学习完成后回到复习状态

**前置条件**：
- 创建卡片，SRS 状态为 RELEARNING
- currentStep 为最后一个步骤

**测试步骤**：
- 提交复习结果（result=3）

**预期结果**：
- type 变更为 REVIEW（type=2）
- interval 单位变为天

---

### 9. 间隔计算测试

#### 测试场景 9.1：验证不同评分的间隔差异
**测试目标**：验证不同评分产生不同的间隔

**前置条件**：
- 创建 4 张相同状态的卡片

**测试步骤**：
- card1 提交 result=1（Again）
- card2 提交 result=2（Hard）
- card3 提交 result=3（Good）
- card4 提交 result=4（Easy）

**预期结果**：
- interval 顺序：card1 < card2 < card3 < card4
- card4 的间隔最大（Easy）

---

## 测试覆盖率要求

- **接口覆盖率**: 100%（所有公开接口）
- **分支覆盖率**: >= 80%（核心业务逻辑）
- **异常场景覆盖**:
  - ✅ 参数验证失败
  - ✅ 资源不存在
  - ✅ 权限不足
  - ✅ 卡片不属于用户
  - ✅ SRS 状态转换

---

## 运行测试

```bash
# 运行复习功能测试
mvn test -Dtest=ReviewControllerTest

# 运行所有测试
mvn test

# 生成测试报告
mvn test jacoco:report
```

---

## 测试数据清理

- 所有测试使用 `@Transactional` 注解
- 测试完成后自动回滚数据
- 无需手动清理测试数据
