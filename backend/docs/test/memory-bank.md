# 记忆库管理接口测试文档 (MemoryBankController)

## 测试文件

- **测试类**: `MemoryBankControllerTest.java`
- **位置**: `learn-web/src/test/java/com/prosper/learn/web/v1/controller/`
- **API文档**: `docs/api/memory-bank.md`
- **Controller**: `MemoryBankController.java`

## 测试框架配置

- 使用 `@SpringBootTest` 和 `@AutoConfigureMockMvc`
- 使用 `@Transactional` 实现测试后自动回滚
- 继承 `BaseControllerTest`

### 依赖注入

需要注入以下服务：
- `MockMvc` - 用于发送 HTTP 请求
- `WebApplicationContext` - Web 应用上下文
- `MemoryBankService` - 记忆库业务服务
- `UserDomainService` - 用户领域服务（用于创建测试用户）
- `UserDataService` - 用户数据服务
- `CourseDataService` - 课程数据服务
- `DeckDataService` - 卡片组数据服务
- `ObjectMapper` - JSON 解析

---

## 测试数据准备

### 必需的测试辅助方法

需要创建以下辅助方法来准备测试数据：

1. **创建测试用户**
   - 功能：创建并返回一个测试用户
   - 参数：邮箱地址
   - 返回：UserDO 对象（包含自动生成的 ID）

2. **创建已发布课程**
   - 功能：创建一个已发布状态的课程
   - 参数：课程名称、创建者ID
   - 返回：CourseDO 对象

3. **创建卡片组**
   - 功能：创建一个卡片组
   - 参数：节点ID、创建者ID
   - 返回：DeckDO 对象

4. **创建记忆库记录**
   - 功能：为用户创建记忆库课程记录
   - 参数：用户ID、课程ID、卡片组ID
   - 返回：记忆库记录

5. **验证记忆库记录存在**
   - 功能：验证数据库中是否存在指定的记忆库记录
   - 参数：用户ID、课程ID、卡片组ID
   - 返回：boolean

---

## 测试用例设计

## 1. 添加卡片组到记忆库接口 (POST /api/v1/memory/memory-bank/decks)

### 1.1 正常流程测试

#### 测试场景 1.1.1：首次添加卡片组成功
**测试目标**：验证用户可以成功将卡片组添加到记忆库

**前置条件**：
- 创建测试用户 user1
- 创建已发布课程 course1，创建者为 user1
- 创建卡片组 deck1，属于 course1
- user1 已登录
- deck1 尚未添加到 user1 的记忆库

**测试步骤**：
1. user1 登录系统
2. 发送 POST 请求到 `/api/v1/memory/memory-bank/decks`
3. 请求体：`{"deckId": deck1.id, "courseId": course1.id}`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- 响应消息：`"操作成功"`
- 无 data 字段
- 数据库验证：
  - 存在记忆库记录（user_id=user1.id, course_id=course1.id, deck_id=deck1.id）
  - 自动创建了 UserCourseSrsSetting 记录（frequency_setting=20, state=1）
  - 为 deck1 中所有卡片创建了 UserCardLearningRecord 记录（status=0 新卡片）

---

#### 测试场景 1.1.2：重复添加相同卡片组
**测试目标**：验证重复添加同一卡片组时的处理

**前置条件**：
- 创建测试用户 user1
- 创建已发布课程 course1
- 创建卡片组 deck1
- user1 已将 deck1 添加到记忆库
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 再次发送 POST 请求到 `/api/v1/memory/memory-bank/decks`
3. 请求体：`{"deckId": deck1.id, "courseId": course1.id}`

**预期结果**：
- HTTP 状态码：200
- 响应码：对应错误码（如 1002 或业务错误码）
- 响应消息包含："卡片组已在记忆库中" 或类似提示
- 数据库验证：
  - 记忆库记录数量没有增加
  - 原有记录未被修改

---

#### 测试场景 1.1.3：向已有课程添加新卡片组
**测试目标**：验证可以向同一课程添加多个不同的卡片组

**前置条件**：
- 创建测试用户 user1
- 创建已发布课程 course1
- 创建卡片组 deck1 和 deck2，都属于 course1
- user1 已将 deck1 添加到记忆库
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 POST 请求添加 deck2
3. 请求体：`{"deckId": deck2.id, "courseId": course1.id}`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- 数据库验证：
  - 存在两条记忆库记录（deck1 和 deck2）
  - 都关联到同一个 course1
  - 使用相同的 UserCourseSrsSetting 记录

---

### 1.2 参数验证测试

#### 测试场景 1.2.1：deckId 为 null
**前置条件**：user1 已登录

**测试步骤**：
- 发送请求体：`{"courseId": 1}`（不包含 deckId）

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："卡片组ID不能为空"

---

#### 测试场景 1.2.2：deckId 为 0
**前置条件**：user1 已登录

**测试步骤**：
- 发送请求体：`{"deckId": 0, "courseId": 1}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："卡片组ID必须大于0"

---

#### 测试场景 1.2.3：deckId 为负数
**前置条件**：user1 已登录

**测试步骤**：
- 发送请求体：`{"deckId": -1, "courseId": 1}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："卡片组ID必须大于0"

---

#### 测试场景 1.2.4：courseId 为 null
**前置条件**：user1 已登录

**测试步骤**：
- 发送请求体：`{"deckId": 1}`（不包含 courseId）

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："课程ID不能为空"

---

#### 测试场景 1.2.5：courseId 为 0
**前置条件**：user1 已登录

**测试步骤**：
- 发送请求体：`{"deckId": 1, "courseId": 0}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："课程ID必须大于0"

---

#### 测试场景 1.2.6：courseId 为负数
**前置条件**：user1 已登录

**测试步骤**：
- 发送请求体：`{"deckId": 1, "courseId": -1}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："课程ID必须大于0"

---

### 1.3 业务错误测试

#### 测试场景 1.3.1：卡片组不存在
**前置条件**：
- user1 已登录
- 创建课程 course1
- 不存在 ID 为 99999 的卡片组

**测试步骤**：
- 发送请求体：`{"deckId": 99999, "courseId": course1.id}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1007（NOT_FOUND）
- 响应消息："卡片组不存在"

---

#### 测试场景 1.3.2：课程不存在
**前置条件**：
- user1 已登录
- 创建卡片组 deck1
- 不存在 ID 为 99999 的课程

**测试步骤**：
- 发送请求体：`{"deckId": deck1.id, "courseId": 99999}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1007（NOT_FOUND）
- 响应消息："课程不存在"

---

#### 测试场景 1.3.3：卡片组与课程不匹配
**测试目标**：验证卡片组必须属于指定的课程

**前置条件**：
- user1 已登录
- 创建课程 course1 和 course2
- 创建卡片组 deck1，属于 course1

**测试步骤**：
- 发送请求体：`{"deckId": deck1.id, "courseId": course2.id}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息："卡片组不属于该课程"

---

#### 测试场景 1.3.4：节点下卡片数量超出限制
**测试目标**：验证系统限制节点下最多200张卡片

**前置条件**：
- user1 已登录
- 创建课程 course1 和节点 node1
- 创建卡片组 deck1（包含150张卡片），属于 node1
- 创建卡片组 deck2（包含60张卡片），属于 node1
- user1 已将 deck1 添加到记忆库

**测试步骤**：
1. user1 先添加 deck1 到记忆库（150张卡片）
2. 尝试添加 deck2 到记忆库（60张卡片）
3. 发送请求体：`{"deckId": deck2.id, "courseId": course1.id}`

**预期结果**：
- HTTP 状态码：200
- 响应码：2210（NODE_CARD_LIMIT_EXCEEDED）
- 响应消息："节点下的卡片数量已达到上限" 或包含具体数量说明（如："该节点已有150张卡片，添加60张新卡片将超过200张的限制"）
- 数据库验证：
  - deck2 未被添加到记忆库
  - node1 下仍然只有150张卡片

**业务说明**：
- 此限制是按节点（node）维度的，同一节点下的所有卡片组加起来不能超过200张
- 配置项：`app.srs.max-cards-per-node=200`
- 不同节点之间的卡片数量限制是独立的

---

#### 测试场景 1.3.5：未登录尝试添加
**前置条件**：
- 创建课程 course1 和卡片组 deck1
- 未登录

**测试步骤**：
- 不带 token 发送 POST 请求
- 请求体：`{"deckId": deck1.id, "courseId": course1.id}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1101（USER_NOT_LOGIN）
- 响应消息："用户未登录"

---

## 2. 获取记忆库课程列表接口 (GET /api/v1/memory/memory-bank/courses)

### 2.1 正常流程测试

#### 测试场景 2.1.1：获取空记忆库
**测试目标**：验证用户没有任何记忆库课程时返回空列表

**前置条件**：
- 创建测试用户 user1
- user1 没有添加任何卡片组到记忆库
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/memory-bank/courses`
3. 不传 status 参数

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- data 字段为空数组：`[]`

---

#### 测试场景 2.1.2：获取有数据的记忆库
**测试目标**：验证正确返回记忆库课程列表及统计数据

**前置条件**：
- 创建测试用户 user1
- 创建课程 course1 和 course2
- 创建卡片组 deck1（属于 course1，包含 10 张卡片）
- 创建卡片组 deck2（属于 course2，包含 5 张卡片）
- user1 已将 deck1 和 deck2 添加到记忆库
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/memory-bank/courses`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- data 数组包含 2 个元素
- 验证每个 CourseMemoryBankDTO 包含：
  - `course`：课程基本信息（id, name）
  - `setting`：SRS 设置（id, frequencySetting=20, state=1）
  - `cardCount`：总卡片数（course1=10, course2=5）
  - `dueCardCount`：今日到期卡片数
  - `newCardCount`：新卡片数（未学习的）
  - `reviewCardCount`：复习中卡片数
  - `learnedCardCount`：已掌握卡片数

---

#### 测试场景 2.1.3：按状态筛选 - 只返回学习中的课程
**测试目标**：验证 status 参数过滤功能

**前置条件**：
- 创建测试用户 user1
- 创建课程 course1 和 course2
- user1 已将 course1 和 course2 添加到记忆库
- course1 的学习状态为 1（学习中）
- course2 的学习状态为 0（暂停）
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/memory-bank/courses?status=1`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- data 数组只包含 1 个元素（course1）
- course1 的 setting.state = 1

---

#### 测试场景 2.1.4：按状态筛选 - 只返回暂停的课程
**测试目标**：验证 status=0 过滤功能

**前置条件**：
- 创建测试用户 user1
- 创建课程 course1 和 course2
- user1 已将 course1 和 course2 添加到记忆库
- course1 的学习状态为 1（学习中）
- course2 的学习状态为 0（暂停）
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 GET 请求到 `/api/v1/memory/memory-bank/courses?status=0`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- data 数组只包含 1 个元素（course2）
- course2 的 setting.state = 0

---

### 2.2 参数验证测试

#### 测试场景 2.2.1：status 为负数
**前置条件**：user1 已登录

**测试步骤**：
- 发送 GET 请求到 `/api/v1/memory/memory-bank/courses?status=-1`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："状态不能小于0"

---

#### 测试场景 2.2.2：status 为非法值
**前置条件**：user1 已登录

**测试步骤**：
- 发送 GET 请求到 `/api/v1/memory/memory-bank/courses?status=99`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- data 字段为空数组（没有状态为 99 的记录）

---

### 2.3 业务错误测试

#### 测试场景 2.3.1：未登录尝试获取
**前置条件**：未登录

**测试步骤**：
- 不带 token 发送 GET 请求到 `/api/v1/memory/memory-bank/courses`

**预期结果**：
- HTTP 状态码：200
- 响应码：1101（USER_NOT_LOGIN）
- 响应消息："用户未登录"

---

### 2.4 数据准确性测试

#### 测试场景 2.4.1：卡片统计数量准确性
**测试目标**：验证返回的卡片统计数据准确

**前置条件**：
- 创建测试用户 user1
- 创建课程 course1 和卡片组 deck1（包含 20 张卡片）
- user1 已将 deck1 添加到记忆库
- user1 已学习其中 5 张卡片（已到复习阶段）
- user1 已掌握其中 3 张卡片（学习次数≥5，间隔≥30天）
- user1 已登录

**测试步骤**：
- 发送 GET 请求到 `/api/v1/memory/memory-bank/courses`

**预期结果**：
- course1 的统计数据：
  - `cardCount` = 20（总数）
  - `newCardCount` = 12（20 - 5 - 3）
  - `reviewCardCount` = 5
  - `learnedCardCount` = 3
  - 验证：cardCount = newCardCount + reviewCardCount + learnedCardCount

---

## 3. 更新课程复习策略接口 (PUT /api/v1/memory/memory-bank/courses/{courseId}/settings)

### 3.1 正常流程测试

#### 测试场景 3.1.1：更新每日新卡数量
**测试目标**：验证可以成功更新 frequencySetting

**前置条件**：
- 创建测试用户 user1
- 创建课程 course1 和卡片组 deck1
- user1 已将 deck1 添加到记忆库
- 当前 frequencySetting = 20（默认值）
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 PUT 请求到 `/api/v1/memory/memory-bank/courses/{course1.id}/settings`
3. 请求体：`{"courseId": course1.id, "frequencySetting": 30}`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- 响应消息：`"操作成功"`
- 数据库验证：
  - UserCourseSrsSetting 的 frequency_setting 更新为 30
  - state 保持不变（仍为 1）

---

#### 测试场景 3.1.2：更新学习状态为暂停
**测试目标**：验证可以成功暂停课程学习

**前置条件**：
- 创建测试用户 user1
- 创建课程 course1 和卡片组 deck1
- user1 已将 deck1 添加到记忆库
- 当前 state = 1（学习中）
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 PUT 请求到 `/api/v1/memory/memory-bank/courses/{course1.id}/settings`
3. 请求体：`{"courseId": course1.id, "status": 0}`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- 数据库验证：
  - UserCourseSrsSetting 的 state 更新为 0
  - frequency_setting 保持不变

---

#### 测试场景 3.1.3：同时更新频率和状态
**测试目标**：验证可以同时更新两个字段

**前置条件**：
- 创建测试用户 user1
- 创建课程 course1 和卡片组 deck1
- user1 已将 deck1 添加到记忆库
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 PUT 请求到 `/api/v1/memory/memory-bank/courses/{course1.id}/settings`
3. 请求体：`{"courseId": course1.id, "frequencySetting": 25, "status": 1}`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- 数据库验证：
  - UserCourseSrsSetting 的 frequency_setting 更新为 25
  - UserCourseSrsSetting 的 state 更新为 1

---

#### 测试场景 3.1.4：恢复已暂停的课程
**测试目标**：验证可以恢复暂停的课程学习

**前置条件**：
- 创建测试用户 user1
- 创建课程 course1 和卡片组 deck1
- user1 已将 deck1 添加到记忆库
- 当前 state = 0（暂停）
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 PUT 请求到 `/api/v1/memory/memory-bank/courses/{course1.id}/settings`
3. 请求体：`{"courseId": course1.id, "status": 1}`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- 数据库验证：
  - UserCourseSrsSetting 的 state 更新为 1

---

### 3.2 参数验证测试

#### 测试场景 3.2.1：路径 courseId 为 0
**前置条件**：user1 已登录

**测试步骤**：
- 发送 PUT 请求到 `/api/v1/memory/memory-bank/courses/0/settings`
- 请求体：`{"courseId": 0, "frequencySetting": 20}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："课程ID必须大于0"

---

#### 测试场景 3.2.2：路径 courseId 为负数
**前置条件**：user1 已登录

**测试步骤**：
- 发送 PUT 请求到 `/api/v1/memory/memory-bank/courses/-1/settings`
- 请求体：`{"courseId": -1, "frequencySetting": 20}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："课程ID必须大于0"

---

#### 测试场景 3.2.3：请求体 courseId 为 null
**前置条件**：
- user1 已登录
- 创建课程 course1

**测试步骤**：
- 发送 PUT 请求到 `/api/v1/memory/memory-bank/courses/{course1.id}/settings`
- 请求体：`{"frequencySetting": 20}`（不包含 courseId）

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："课程ID不能为空"

---

#### 测试场景 3.2.4：frequencySetting 为负数
**前置条件**：
- user1 已登录
- 创建课程 course1

**测试步骤**：
- 发送 PUT 请求到 `/api/v1/memory/memory-bank/courses/{course1.id}/settings`
- 请求体：`{"courseId": course1.id, "frequencySetting": -1}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："每日新卡数量不能为负数" 或类似提示

---

#### 测试场景 3.2.5：status 为非法值
**前置条件**：
- user1 已登录
- 创建课程 course1

**测试步骤**：
- 发送 PUT 请求到 `/api/v1/memory/memory-bank/courses/{course1.id}/settings`
- 请求体：`{"courseId": course1.id, "status": 99}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："状态值必须为0或1" 或类似提示

---

#### 测试场景 3.2.6：请求体为空对象
**前置条件**：
- user1 已登录
- 创建课程 course1

**测试步骤**：
- 发送 PUT 请求到 `/api/v1/memory/memory-bank/courses/{course1.id}/settings`
- 请求体：`{}`（空对象）

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："至少需要更新一个字段" 或参数缺失提示

---

### 3.3 业务错误测试

#### 测试场景 3.3.1：课程不存在
**前置条件**：
- user1 已登录
- 不存在 ID 为 99999 的课程

**测试步骤**：
- 发送 PUT 请求到 `/api/v1/memory/memory-bank/courses/99999/settings`
- 请求体：`{"courseId": 99999, "frequencySetting": 20}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1007（NOT_FOUND）
- 响应消息："课程不存在"

---

#### 测试场景 3.3.2：用户没有该课程的记忆库
**前置条件**：
- user1 已登录
- 创建课程 course1
- user1 没有将 course1 添加到记忆库

**测试步骤**：
- 发送 PUT 请求到 `/api/v1/memory/memory-bank/courses/{course1.id}/settings`
- 请求体：`{"courseId": course1.id, "frequencySetting": 20}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1007（NOT_FOUND）
- 响应消息："该课程不在您的记忆库中"

---

#### 测试场景 3.3.3：未登录尝试更新
**前置条件**：
- 创建课程 course1
- 未登录

**测试步骤**：
- 不带 token 发送 PUT 请求到 `/api/v1/memory/memory-bank/courses/{course1.id}/settings`
- 请求体：`{"courseId": course1.id, "frequencySetting": 20}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1101（USER_NOT_LOGIN）
- 响应消息："用户未登录"

---

### 3.4 边界条件测试

#### 测试场景 3.4.1：frequencySetting 设置为 0
**测试目标**：验证每日新卡数量可以设置为 0（完全不学新卡）

**前置条件**：
- user1 已登录
- 创建课程 course1，已添加到记忆库

**测试步骤**：
- 发送 PUT 请求更新 frequencySetting 为 0

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- 数据库验证：frequency_setting = 0

**业务验证**：
- 用户将不再获得新卡片
- 只复习已有的卡片

---

#### 测试场景 3.4.2：frequencySetting 设置为极大值
**测试目标**：验证系统对过大值的处理

**前置条件**：
- user1 已登录
- 创建课程 course1，已添加到记忆库

**测试步骤**：
- 发送 PUT 请求更新 frequencySetting 为 1000

**预期结果**：
- 选项1：接受该值（响应码 200）
- 选项2：限制最大值（响应码 1002，提示："每日新卡数量不能超过 100" 或类似）

**建议**：明确业务规则并实现相应验证

---

## 4. 移除卡片组接口 (DELETE /api/v1/memory/memory-bank/courses/{courseId}/decks/{deckId})

### 4.1 正常流程测试

#### 测试场景 4.1.1：成功移除卡片组
**测试目标**：验证用户可以成功从记忆库移除卡片组

**前置条件**：
- 创建测试用户 user1
- 创建课程 course1 和卡片组 deck1
- user1 已将 deck1 添加到记忆库
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 DELETE 请求到 `/api/v1/memory/memory-bank/courses/{course1.id}/decks/{deck1.id}`

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- 响应消息：`"操作成功"`
- 数据库验证：
  - 不存在记忆库记录（user_id=user1.id, course_id=course1.id, deck_id=deck1.id）
  - 该卡片组下所有卡片的学习记录已删除
  - 如果 course1 下没有其他卡片组，UserCourseSrsSetting 记录也被删除

---

#### 测试场景 4.1.2：移除后课程仍有其他卡片组
**测试目标**：验证移除一个卡片组不影响同课程的其他卡片组

**前置条件**：
- 创建测试用户 user1
- 创建课程 course1，卡片组 deck1 和 deck2
- user1 已将 deck1 和 deck2 都添加到记忆库
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 DELETE 请求移除 deck1

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- 数据库验证：
  - deck1 的记忆库记录和学习记录已删除
  - deck2 的记忆库记录和学习记录保持不变
  - UserCourseSrsSetting 记录仍然存在

---

#### 测试场景 4.1.3：移除最后一个卡片组
**测试目标**：验证移除课程最后一个卡片组后的清理

**前置条件**：
- 创建测试用户 user1
- 创建课程 course1 和卡片组 deck1
- user1 已将 deck1 添加到记忆库（course1 下唯一的卡片组）
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 DELETE 请求移除 deck1

**预期结果**：
- HTTP 状态码：200
- 响应码：200
- 数据库验证：
  - deck1 的记忆库记录和学习记录已删除
  - course1 的 UserCourseSrsSetting 记录也被删除
  - course1 不再出现在用户的记忆库课程列表中

---

#### 测试场景 4.1.4：重复移除（幂等性测试）
**测试目标**：验证重复移除不会报错

**前置条件**：
- 创建测试用户 user1
- 创建课程 course1 和卡片组 deck1
- user1 没有将 deck1 添加到记忆库
- user1 已登录

**测试步骤**：
1. user1 登录系统
2. 发送 DELETE 请求到 `/api/v1/memory/memory-bank/courses/{course1.id}/decks/{deck1.id}`

**预期结果**：
- HTTP 状态码：200
- 响应码：200（或 1007）
- 响应消息：`"操作成功"` 或 `"卡片组不在记忆库中"`

---

### 4.2 参数验证测试

#### 测试场景 4.2.1：courseId 为 0
**前置条件**：user1 已登录

**测试步骤**：
- 发送 DELETE 请求到 `/api/v1/memory/memory-bank/courses/0/decks/1`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："课程ID必须大于0"

---

#### 测试场景 4.2.2：courseId 为负数
**前置条件**：user1 已登录

**测试步骤**：
- 发送 DELETE 请求到 `/api/v1/memory/memory-bank/courses/-1/decks/1`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："课程ID必须大于0"

---

#### 测试场景 4.2.3：deckId 为 0
**前置条件**：user1 已登录

**测试步骤**：
- 发送 DELETE 请求到 `/api/v1/memory/memory-bank/courses/1/decks/0`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："卡片组ID必须大于0"

---

#### 测试场景 4.2.4：deckId 为负数
**前置条件**：user1 已登录

**测试步骤**：
- 发送 DELETE 请求到 `/api/v1/memory/memory-bank/courses/1/decks/-1`

**预期结果**：
- HTTP 状态码：200
- 响应码：1002（INVALID_PARAMETER）
- 响应消息包含："卡片组ID必须大于0"

---

### 4.3 业务错误测试

#### 测试场景 4.3.1：课程不存在
**前置条件**：
- user1 已登录
- 不存在 ID 为 99999 的课程

**测试步骤**：
- 发送 DELETE 请求到 `/api/v1/memory/memory-bank/courses/99999/decks/1`

**预期结果**：
- HTTP 状态码：200
- 响应码：1007（NOT_FOUND）
- 响应消息："课程不存在"

---

#### 测试场景 4.3.2：卡片组不存在
**前置条件**：
- user1 已登录
- 创建课程 course1
- 不存在 ID 为 99999 的卡片组

**测试步骤**：
- 发送 DELETE 请求到 `/api/v1/memory/memory-bank/courses/{course1.id}/decks/99999`

**预期结果**：
- HTTP 状态码：200
- 响应码：1007（NOT_FOUND）
- 响应消息："卡片组不存在"

---

#### 测试场景 4.3.3：未登录尝试移除
**前置条件**：
- 创建课程 course1 和卡片组 deck1
- 未登录

**测试步骤**：
- 不带 token 发送 DELETE 请求到 `/api/v1/memory/memory-bank/courses/{course1.id}/decks/{deck1.id}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1101（USER_NOT_LOGIN）
- 响应消息："用户未登录"

---

#### 测试场景 4.3.4：尝试移除其他用户的卡片组
**测试目标**：验证权限隔离

**前置条件**：
- 创建测试用户 user1 和 user2
- 创建课程 course1 和卡片组 deck1
- user2 已将 deck1 添加到记忆库
- user1 已登录（不是 deck1 的拥有者）

**测试步骤**：
1. user1 登录系统
2. 发送 DELETE 请求到 `/api/v1/memory/memory-bank/courses/{course1.id}/decks/{deck1.id}`

**预期结果**：
- HTTP 状态码：200
- 响应码：1007（NOT_FOUND）或 1103（PERMISSION_DENIED）
- 响应消息："卡片组不在您的记忆库中" 或 "无权操作"
- 数据库验证：
  - user2 的记忆库记录保持不变

---

## 5. 集成测试场景

### 测试场景 5.1：完整记忆库管理流程
**测试目标**：验证从添加到移除的完整流程

**前置条件**：
- 创建测试用户 user1
- 创建课程 course1，包含卡片组 deck1 和 deck2

**测试步骤**：
1. user1 添加 deck1 到记忆库
2. 验证记忆库课程列表包含 course1
3. 验证 course1 的卡片统计正确
4. user1 添加 deck2 到记忆库
5. 验证 course1 的卡片统计更新（包含两个卡片组的总数）
6. user1 更新 course1 的学习设置（frequencySetting=30）
7. 验证设置更新成功
8. user1 移除 deck1
9. 验证 course1 仍在记忆库中，但卡片数减少
10. user1 移除 deck2
11. 验证 course1 不再出现在记忆库课程列表中

**预期结果**：
- 每一步都成功
- 数据库状态与预期一致
- 卡片统计数据随操作实时更新

---

### 测试场景 5.2：多课程记忆库管理
**测试目标**：验证多个课程的独立管理

**前置条件**：
- 创建测试用户 user1
- 创建课程 course1, course2, course3
- 每个课程各有 1 个卡片组

**测试步骤**：
1. user1 添加 course1, course2, course3 的卡片组到记忆库
2. 验证记忆库课程列表包含 3 个课程
3. user1 设置 course1 为暂停状态
4. user1 设置 course2 的 frequencySetting 为 50
5. 验证每个课程的设置独立
6. 按状态筛选：获取学习中的课程（应返回 course2 和 course3）
7. user1 移除 course2 的卡片组
8. 验证只剩 course1 和 course3 在记忆库中

**预期结果**：
- 每个课程的设置和状态独立
- 筛选功能正确
- 移除操作不影响其他课程

---

### 测试场景 5.3：学习进度数据验证
**测试目标**：验证卡片统计数据随学习进度更新

**前置条件**：
- 创建测试用户 user1
- 创建课程 course1 和卡片组 deck1（包含 20 张卡片）
- user1 已将 deck1 添加到记忆库

**测试步骤**：
1. 初始状态：获取记忆库课程列表
   - 验证 newCardCount = 20, reviewCardCount = 0, learnedCardCount = 0
2. 模拟学习 5 张卡片（创建学习记录）
3. 再次获取记忆库课程列表
   - 验证 newCardCount = 15, reviewCardCount = 5, learnedCardCount = 0
4. 模拟其中 2 张卡片达到掌握标准
5. 再次获取记忆库课程列表
   - 验证 newCardCount = 15, reviewCardCount = 3, learnedCardCount = 2
6. 验证 cardCount 始终等于三者之和（20）

**预期结果**：
- 卡片统计随学习进度实时更新
- 数量关系正确：cardCount = newCardCount + reviewCardCount + learnedCardCount

---

## 6. 性能测试建议

### 测试场景 6.1：大量课程列表查询
**测试目标**：验证系统处理大量记忆库课程的性能

**前置条件**：
- 创建测试用户 user1
- 创建 50 个课程，每个课程包含 1-3 个卡片组
- user1 已将所有卡片组添加到记忆库

**测试步骤**：
1. 发送 GET 请求获取记忆库课程列表
2. 记录响应时间

**预期结果**：
- 响应时间在合理范围内（< 1秒）
- 正确返回所有 50 个课程及其统计数据
- 使用批量查询优化，避免 N+1 问题

**性能验证**：
- 开启 SQL 日志验证查询优化
- 数据库查询次数应该是固定的，不随课程数量线性增长

---

### 测试场景 6.2：大量卡片统计查询
**测试目标**：验证大量卡片的统计计算性能

**前置条件**：
- 创建测试用户 user1
- 创建课程 course1，包含 10 个卡片组
- 每个卡片组包含 100 张卡片（总计 1000 张）
- user1 已将所有卡片组添加到记忆库
- 已学习其中 500 张卡片

**测试步骤**：
1. 发送 GET 请求获取记忆库课程列表
2. 记录响应时间

**预期结果**：
- 响应时间在合理范围内（< 2秒）
- 正确统计 1000 张卡片的学习状态
- 使用聚合查询或缓存优化

---

## 7. 边界条件测试清单

- [ ] 用户记忆库为空
- [ ] 用户只有 1 个课程在记忆库中
- [ ] 课程只有 1 个卡片组
- [ ] 卡片组没有卡片（空卡片组）
- [ ] 课程有多个卡片组（5-10个）
- [ ] 课程有大量卡片组（50+）
- [ ] 卡片组有大量卡片（1000+）
- [ ] 所有卡片都已掌握（learnedCardCount = cardCount）
- [ ] 所有卡片都是新卡片（newCardCount = cardCount）
- [ ] frequencySetting 为 0
- [ ] frequencySetting 为极大值（1000+）
- [ ] 所有课程都处于暂停状态
- [ ] 重复添加同一卡片组
- [ ] 重复移除同一卡片组
- [ ] 添加后立即移除
- [ ] 移除后立即重新添加
- [ ] 并发添加相同卡片组
- [ ] 并发更新相同课程设置

---

## 8. 错误码完整性检查

确保以下所有错误码都被测试覆盖：

- [ ] 200 - OK（成功场景）
- [ ] 1002 - INVALID_PARAMETER（所有参数验证错误）
- [ ] 1007 - NOT_FOUND（课程、卡片组不存在）
- [ ] 1101 - USER_NOT_LOGIN（未登录）
- [ ] 1103 - PERMISSION_DENIED（权限错误，可选）
- [ ] 2301 - RATE_LIMIT_EXCEEDED（频率限制，可选）

---

## 9. 测试执行指南

### 执行单个测试类
```bash
mvn test -Dtest=MemoryBankControllerTest
```

### 执行特定测试方法
```bash
mvn test -Dtest=MemoryBankControllerTest#testAddDeckToMemoryBank_Success
```

### 查看测试覆盖率
```bash
mvn clean test jacoco:report
```

### 测试数据清理
- 使用 `@Transactional` 注解确保测试后自动回滚
- 每个测试方法使用唯一的邮箱地址避免冲突
- 测试结束后清理 Sa-Token 登录状态

---

## 10. 注意事项

1. **SRS 算法测试**
   - 需要理解 SRS 算法的卡片状态分类规则
   - 新卡片：学习次数 = 0
   - 复习中：学习次数 > 0，间隔 < 30天
   - 已掌握：学习次数 ≥ 5次，间隔 ≥ 30天

2. **卡片统计准确性**
   - 验证 cardCount = newCardCount + reviewCardCount + learnedCardCount
   - dueCardCount 应该是 cardCount 的子集
   - 统计数据应随学习进度实时更新

3. **课程与卡片组关系**
   - 一个课程可以有多个卡片组
   - 移除最后一个卡片组时，课程的 SRS 设置记录也应删除
   - 同一课程的多个卡片组共享 SRS 设置

4. **幂等性测试重要性**
   - 添加和移除操作都需要测试重复操作的幂等性
   - 验证重复操作不会导致数据不一致或错误

5. **状态筛选功能**
   - status=0：只返回暂停的课程
   - status=1：只返回学习中的课程
   - 不传 status：返回所有课程

6. **数据一致性**
   - 记忆库记录、学习记录、SRS 设置记录之间的一致性
   - 删除操作的级联处理
   - 并发操作的数据一致性

7. **性能优化**
   - 批量查询优化很重要
   - 应该验证 N+1 查询问题已解决
   - 卡片统计计算可能需要缓存或聚合查询优化

8. **Rate Limit 测试**
   - Controller 配置了 50 requests/minute 的限制
   - 可选：测试超出频率限制的情况

---

*此测试文档用于指导测试人员和开发人员编写全面的测试用例，确保记忆库管理功能的正确性和稳定性*
