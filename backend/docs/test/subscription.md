# 订阅管理接口测试用例

> 参考测试编写规范：`docs/test/TEST_GUIDE.md`

---

## 测试准备

### 测试类基础配置

**测试类名**: `SubscriptionsControllerTest`

**继承**: `extends BaseControllerTest`

**注解**:
- `@Transactional` - 自动回滚，测试之间互不影响

**必需依赖**:
- MockMvc - HTTP 请求测试
- ObjectMapper - JSON 解析
- UserDomainService - 创建测试用户
- CourseDataService - 创建测试课程
- UserDataService - 查询用户订阅

### 测试辅助方法

需要创建以下辅助方法：

1. **createUser(String email)** - 创建测试用户
2. **createPublishedCourse(String name, Long creatorId)** - 创建已发布课程
3. **getUserSubscriptions(Long userId)** - 获取用户的订阅列表（从数据库）

### 说明

1. **@Transactional - 自动清理数据**:
   - 每个 `@Test` 方法执行完后，所有数据库操作都会自动回滚
   - 包括用户、课程、订阅关系等所有操作
   - 无需手动清理数据，测试之间互不影响

2. **动态ID**:
   - 所有测试数据使用数据库自动生成的ID
   - 避免测试之间的ID冲突

3. **依赖数据**:
   - 订阅课程前必须先创建用户和课程
   - 使用辅助方法确保依赖数据的正确创建

4. **Sa-Token 登录**:
   - 需要登录的接口使用 `StpUtil.login(userId)` 模拟登录
   - 请求时添加 header: `"token", StpUtil.getTokenValue()`
   - 测试结束后使用 `StpUtil.logout()` 清理登录状态

---

## 接口1: 获取用户订阅列表 (GET /api/v1/users/{userId}/subscriptions)

### 测试场景

#### 1.1 成功获取订阅列表 - 有订阅
- **准备**：创建用户A，创建3个课程，用户A订阅这3个课程
- **请求**：GET /users/{userA.id}/subscriptions
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` 是数组，长度为 3
  - 每个课程包含字段：id, name, description, mainCategory, subCategory
  - 每个课程包含统计字段：learnerCount, subscriptionCount (可能为0或null)
  - 每个课程包含用户字段：subscribed (true), progress (0或具体值)

#### 1.2 成功获取订阅列表 - 无订阅
- **准备**：创建用户A，未订阅任何课程
- **请求**：GET /users/{userA.id}/subscriptions
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` 是空数组

#### 1.3 字段验证 - userId 缺失
- **请求**：GET /users//subscriptions (路径中userId为空)
- **验证**：
  - 返回 200 状态码（项目统一返回200）
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 1.4 字段验证 - userId 无效（0）
- **请求**：GET /users/0/subscriptions
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 1.5 字段验证 - userId 无效（负数）
- **请求**：GET /users/-1/subscriptions
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 1.6 业务验证 - 用户不存在
- **请求**：GET /users/99999/subscriptions
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1116 (StatusCode.USER_NOT_FOUND)

#### 1.7 权限验证 - 不需要登录
- **请求**：GET /users/{userId}/subscriptions (不传token)
- **验证**：
  - 返回 200 状态码（此接口不需要登录）
  - `$.code` = 200
  - 可以正常获取订阅列表

#### 1.8 统计字段填充 - learnerCount
- **准备**：
  - 创建课程A
  - 3个用户订阅课程A（通过订阅接口，触发统计更新）
- **请求**：GET /users/{anyUserId}/subscriptions
- **验证**：
  - 课程A的 `learnerCount` 或 `subscriptionCount` 应该 > 0（根据统计服务实际数据）
  - 如果统计服务未初始化，可能为0，验证字段存在即可

#### 1.9 用户字段填充 - subscribed 始终为 true
- **准备**：用户A订阅课程B
- **请求**：GET /users/{userA.id}/subscriptions
- **验证**：
  - 返回的课程列表中，所有课程的 `subscribed` = true
  - 因为这是用户的订阅列表，所有课程必然已订阅

#### 1.10 用户字段填充 - progress
- **准备**：
  - 用户A订阅课程B
  - 用户A学习课程B，完成部分节点（如果有学习进度功能）
- **请求**：GET /users/{userA.id}/subscriptions
- **验证**：
  - 课程B的 `progress` 字段存在
  - progress 值 >= 0 且 <= 100

#### 1.11 订阅顺序保持
- **准备**：用户A按顺序订阅课程1、课程2、课程3
- **请求**：GET /users/{userA.id}/subscriptions
- **验证**：
  - 返回的课程顺序与订阅顺序一致（如果后端实现了顺序保持）
  - 或按ID顺序返回（取决于实现）

---

## 接口2: 订阅课程 (POST /api/v1/users/current/subscriptions)

### 测试场景

#### 2.1 成功订阅课程
- **准备**：创建用户A，创建课程B（已发布）
- **登录**：模拟用户A登录
- **请求**：POST /users/current/subscriptions，body: `{"courseId": B.id}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` 不存在或为 null（返回void）
  - 数据库验证：用户A的 subscriptions 字段包含课程B的ID

#### 2.2 字段验证 - courseId 缺失
- **准备**：用户A登录
- **请求**：POST /users/current/subscriptions，body: `{}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 2.3 字段验证 - courseId 无效（0）
- **准备**：用户A登录
- **请求**：POST /users/current/subscriptions，body: `{"courseId": 0}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 2.4 字段验证 - courseId 无效（负数）
- **准备**：用户A登录
- **请求**：POST /users/current/subscriptions，body: `{"courseId": -1}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 2.5 业务验证 - 课程不存在
- **准备**：用户A登录
- **请求**：POST /users/current/subscriptions，body: `{"courseId": 99999}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1201 (StatusCode.COURSE_NOT_FOUND)

#### 2.6 业务验证 - 重复订阅（取决于配置）
- **准备**：
  - 用户A订阅课程B
  - 配置启用重复订阅检查
- **请求**：再次订阅课程B
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1113 (StatusCode.USER_COURSE_ALREADY_SUBSCRIBED)
  - 或成功（如果配置允许重复订阅）

#### 2.7 业务验证 - 重复订阅（配置关闭检查）
- **准备**：
  - 用户A订阅课程B
  - 配置关闭重复订阅检查
- **请求**：再次订阅课程B
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200（幂等操作，不报错）

#### 2.8 业务验证 - 订阅数量达到上限
- **准备**：
  - 创建用户A
  - 创建100个课程
  - 用户A订阅100个课程（达到上限）
  - 创建第101个课程
- **请求**：订阅第101个课程
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1117 (StatusCode.USER_SUBSCRIPTION_LIMIT_EXCEEDED)
  - 订阅列表仍为100个

#### 2.9 事件发布 - ContentBookmarkedEvent
- **准备**：用户A登录
- **请求**：订阅课程B
- **验证**：
  - 订阅成功
  - 验证 ContentBookmarkedEvent 事件已发布（通过事件监听器或统计数据变化验证）

#### 2.10 权限验证 - 未登录
- **请求**：POST /users/current/subscriptions，body: `{"courseId": 123}` (不传token)
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1101 (StatusCode.USER_NOT_LOGIN)

#### 2.11 并发订阅 - 多个用户订阅同一课程
- **准备**：创建3个用户，创建1个课程
- **操作**：3个用户并发订阅同一课程
- **验证**：
  - 所有用户都订阅成功
  - 每个用户的订阅列表都包含该课程

---

## 接口3: 取消订阅 (DELETE /api/v1/users/current/subscriptions/{courseId})

### 测试场景

#### 3.1 成功取消订阅
- **准备**：
  - 创建用户A，创建课程B
  - 用户A订阅课程B
- **登录**：模拟用户A登录
- **请求**：DELETE /users/current/subscriptions/{B.id}
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` 不存在或为 null（返回void）
  - 数据库验证：用户A的 subscriptions 字段不包含课程B的ID

#### 3.2 字段验证 - courseId 无效（0）
- **准备**：用户A登录
- **请求**：DELETE /users/current/subscriptions/0
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 3.3 字段验证 - courseId 无效（负数）
- **准备**：用户A登录
- **请求**：DELETE /users/current/subscriptions/-1
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 3.4 业务验证 - 课程不存在
- **准备**：用户A登录
- **请求**：DELETE /users/current/subscriptions/99999
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1201 (StatusCode.COURSE_NOT_FOUND)

#### 3.5 业务验证 - 取消未订阅的课程
- **准备**：
  - 创建用户A，创建课程B
  - 用户A未订阅课程B
- **登录**：用户A登录
- **请求**：DELETE /users/current/subscriptions/{B.id}
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1114 (StatusCode.USER_COURSE_NOT_SUBSCRIBED)

#### 3.6 业务验证 - 重复取消订阅
- **准备**：
  - 用户A订阅课程B
  - 用户A取消订阅课程B
- **请求**：再次取消订阅课程B
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1114 (StatusCode.USER_COURSE_NOT_SUBSCRIBED)

#### 3.7 权限验证 - 未登录
- **请求**：DELETE /users/current/subscriptions/123 (不传token)
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1101 (StatusCode.USER_NOT_LOGIN)

#### 3.8 事件发布 - ContentUnbookmarkedEvent
- **准备**：用户A订阅课程B，然后登录
- **请求**：取消订阅课程B
- **验证**：
  - 取消订阅成功
  - 验证 ContentUnbookmarkedEvent 事件已发布（通过事件监听器或统计数据变化验证）

#### 3.9 订阅列表完整性 - 取消部分订阅
- **准备**：
  - 用户A订阅课程1、2、3
  - 用户A登录
- **请求**：取消订阅课程2
- **验证**：
  - 取消成功
  - 数据库验证：用户A的订阅列表包含课程1和3，不包含课程2
  - 订阅列表顺序保持（1, 3）

---

## 参数验证测试

### 4. 通用参数验证

#### 4.1 ID参数验证（各接口）
- **测试接口**：
  - GET /users/{userId}/subscriptions
  - DELETE /users/current/subscriptions/{courseId}
- **测试用例**：
  - id = 0 → 返回 200, code = 1002
  - id = -1 → 返回 200, code = 1002
  - id 非数字 → 返回 200, code = 1002 或 400（取决于框架验证）

#### 4.2 请求体参数验证
- **测试接口**：POST /users/current/subscriptions
- **测试用例**：
  - body 为空 → 返回 200, code = 1002
  - body 格式错误（非JSON）→ 返回 400
  - courseId 缺失 → 返回 200, code = 1002

---

## 边界测试

### 5. 边界场景

#### 5.1 空数据库查询
- **请求**：查询不存在用户的订阅
- **验证**：返回 code = 1116，不报错

#### 5.2 大量订阅
- **准备**：用户A订阅50个课程
- **请求**：GET /users/{userA.id}/subscriptions
- **验证**：
  - 返回所有50个课程
  - 响应时间合理（< 2秒）

#### 5.3 并发订阅同一课程
- **场景**：同一用户在多个请求中并发订阅同一课程
- **验证**：
  - 根据配置，可能成功1次或多次
  - 最终用户订阅列表中该课程只出现1次

#### 5.4 并发取消订阅
- **场景**：同一用户在多个请求中并发取消订阅同一课程
- **验证**：
  - 所有请求都返回成功（幂等）
  - 最终用户订阅列表中不包含该课程

---

## 业务场景测试

### 6. 订阅业务流程

#### 6.1 完整订阅-取消流程
- **操作**：
  1. 用户A订阅课程B → 成功
  2. 获取订阅列表 → 包含课程B
  3. 取消订阅课程B → 成功
  4. 获取订阅列表 → 不包含课程B
- **验证**：每步都成功，数据一致

#### 6.2 多课程订阅管理
- **操作**：
  1. 用户A订阅课程1, 2, 3 → 成功
  2. 获取订阅列表 → 包含课程1, 2, 3
  3. 取消订阅课程2 → 成功
  4. 获取订阅列表 → 包含课程1, 3
  5. 订阅课程4 → 成功
  6. 获取订阅列表 → 包含课程1, 3, 4
- **验证**：订阅列表始终正确

#### 6.3 跨用户隔离
- **操作**：
  1. 用户A订阅课程1
  2. 用户B订阅课程2
  3. 获取用户A的订阅 → 只包含课程1
  4. 获取用户B的订阅 → 只包含课程2
- **验证**：用户订阅互不影响

---

## 统计和事件测试

### 7. 统计字段验证

#### 7.1 learnerCount 统计
- **准备**：多个用户订阅同一课程
- **验证**：
  - 课程的 learnerCount 或 subscriptionCount 随订阅增加
  - 统计数据准确（依赖统计服务）

#### 7.2 progress 字段准确性
- **准备**：用户学习课程，完成部分内容
- **验证**：
  - 订阅列表中的 progress 反映实际学习进度
  - 进度值在 0-100 范围内

#### 7.3 subscribed 字段准确性
- **验证**：
  - 获取用户订阅列表，所有课程的 subscribed = true
  - 获取其他用户订阅列表，当前用户未订阅的课程 subscribed 不应存在或为null

---

## 测试执行顺序建议

1. 先执行 **基础功能测试**（1.1, 2.1, 3.1）- 确保核心功能正常
2. 再执行 **参数验证测试**（1.3-1.6, 2.2-2.5, 3.2-3.4）- 确保输入验证完善
3. 然后执行 **权限验证测试**（1.7, 2.8, 3.7）- 确保鉴权正确
4. 再执行 **业务逻辑测试**（2.6-2.7, 3.5-3.6, 6.1-6.3）- 确保业务规则正确
5. 最后执行 **边界和并发测试**（5.1-5.4）- 确保系统健壮性

---

## 注意事项

1. **登录要求**：
   - `getUserSubscriptions` 不需要登录（公开查看他人订阅）
   - `subscribe` 和 `unsubscribe` 都需要登录

2. **幂等性**：
   - `subscribe`: 重复订阅的处理取决于配置
   - `unsubscribe`: 取消不存在的订阅不报错（幂等操作）

3. **事件发布**：
   - 订阅/取消订阅会发布事件，触发统计更新
   - 事件类型：`ContentBookmarkedEvent`, `ContentUnbookmarkedEvent`
   - 测试时需要验证事件是否正确发布

4. **数据存储**：
   - 订阅数据存储在 user 表的 subscriptions 字段（逗号分隔字符串）
   - 顺序即用户的排序偏好
   - 测试时需要验证数据库字段格式正确

5. **返回值**：
   - `getUserSubscriptions`: 返回 `List<CourseSummaryWithStatsAndProgressDTO>`
   - `subscribe/unsubscribe`: 返回 `void`（无数据，只有成功/失败状态）

6. **统计字段填充**：
   - `learnerCount`, `subscriptionCount`: 从统计服务查询，可能为0
   - `subscribed`: 订阅列表中始终为 true
   - `progress`: 从学习进度服务查询，范围 0-100

7. **课程状态**：
   - 测试时使用已发布（PUBLISHED）状态的课程
   - 未发布的课程不应该被订阅

8. **跨域验证**：
   - 所有操作都会验证课程是否存在
   - 用户订阅列表会自动过滤已删除的课程ID

---

*此文档用于指导订阅管理接口的测试用例编写，确保测试覆盖全面且准确*
