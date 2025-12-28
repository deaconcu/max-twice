# 订阅管理接口文档 (SubscriptionsController)

## 基本信息

- **Controller**: `SubscriptionsController.java`
- **基础路径**: `/api/v1`
- **Rate Limit**: 60 requests/minute (per user)
- **前端 API**: `web/src/api/modules/course.ts` (subscriptionApi)

## 接口列表

## 1. 获取用户订阅列表

**接口路径**: `GET /api/v1/users/{userId}/subscriptions`

**是否需要登录**: 否

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID，必须大于0 |

**返回类型**: `List<CourseSummaryDTO>`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "Java 编程入门",
      "description": "从零开始学习 Java 编程",
      "state": 2,
      "creatorId": 10,
      "creatorName": "张老师",
      "mainCategory": 1,
      "subCategory": 2,
      "createdAt": "2025-01-15 10:00:00"
    },
    {
      "id": 2,
      "name": "数据结构与算法",
      "description": "掌握常用数据结构与算法",
      "state": 2,
      "creatorId": 11,
      "creatorName": "李老师",
      "mainCategory": 1,
      "subCategory": 3,
      "createdAt": "2025-01-16 14:30:00"
    }
  ]
}
```

**业务逻辑**:
1. 验证用户是否存在
2. 获取用户的订阅课程ID列表（从 user 表的 subscriptions 字段）
3. 批量查询课程信息
4. 转换为 CourseSummaryDTO 返回

**前端调用**:
```typescript
// API 调用
subscriptionApi.getUserSubscriptions(userId)

// 实际使用
const { data: subscriptions } = await subscriptionApi.getUserSubscriptions(user.id)
// subscriptions 是 CourseSummaryDTO[]
```

**使用场景**:
- 用户主页展示订阅的课程列表
- 课程收藏列表页面

---

## 2. 订阅课程

**接口路径**: `POST /api/v1/users/current/subscriptions`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "courseId": 123
}
```

**请求参数说明**:
- `courseId` (Long, 必填): 课程ID，必须大于0

**返回类型**: `int[]` (订阅的课程ID数组)

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [1, 2, 123]
}
```

**业务逻辑**:
1. 验证课程是否存在
2. 检查是否重复订阅（可通过配置开关控制）
3. 将课程ID添加到用户的订阅列表
4. 发布 `ContentBookmarkedEvent` 事件（触发统计更新）
5. 返回更新后的完整订阅ID列表

**前端调用**:
```typescript
// API 调用
subscriptionApi.subscribe(courseId)

// 实际使用
await subscriptionApi.subscribe(course.id)
// 成功后刷新订阅列表
```

**使用场景**:
- 课程详情页点击"订阅"按钮
- 课程列表页订阅操作

**错误情况**:
- 课程不存在：返回 1201 (COURSE_NOT_FOUND)
- 课程已订阅：返回 1113 (USER_COURSE_ALREADY_SUBSCRIBED)，取决于配置
- 未登录：返回 1101 (USER_NOT_LOGIN)

---

## 3. 取消订阅

**接口路径**: `DELETE /api/v1/users/current/subscriptions/{courseId}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| courseId | Long | 是 | 课程ID，必须大于0 |

**返回类型**: `int[]` (更新后的订阅课程ID数组)

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [1, 2]
}
```

**业务逻辑**:
1. 验证课程是否存在
2. 从用户的订阅列表中移除该课程ID
3. 发布 `ContentUnbookmarkedEvent` 事件（触发统计更新）
4. 返回更新后的订阅ID列表

**前端调用**:
```typescript
// API 调用
subscriptionApi.unsubscribe(courseId)

// 实际使用
await subscriptionApi.unsubscribe(course.id)
// 成功后刷新订阅列表
```

**使用场景**:
- 课程详情页点击"取消订阅"按钮
- 用户订阅列表页面删除订阅

**错误情况**:
- 课程不存在：返回 1201 (COURSE_NOT_FOUND)
- 课程未订阅：操作成功，返回当前订阅列表（幂等操作）
- 未登录：返回 1101 (USER_NOT_LOGIN)

---

## DTO 类型说明

### SubscriptionInfo (订阅信息)

**用途**: 简化的课程信息，用于订阅列表展示

```typescript
interface SubscriptionInfo {
  id: number      // 课程ID
  name: string    // 课程名称
}
```

**说明**:
- 这是一个简化的课程信息，只包含最基本的 ID 和名称
- 主要用于快速展示订阅列表
- 如需完整课程信息，应使用 `CourseSummaryDTO`

**使用场景**:
- 用户登录响应中的订阅列表
- 用户个人信息中的订阅字段
- 订阅排序/管理界面

---

## 数据存储说明

### User 表的 subscriptions 字段

**字段类型**: `TEXT`

**存储格式**: 逗号分隔的课程ID字符串
```
"1,2,3,5,8"
```

**设计说明**:
1. **为什么不用关联表？**
   - 订阅数据访问频繁（每次登录都要查询）
   - 订阅数量有限（通常不超过100个）
   - 使用 JSON 字段可以减少 JOIN 查询，提升性能

2. **排序保留**:
   - 字符串顺序即用户的订阅排序
   - 用户可以通过 `updateSubscriptions` 接口自定义排序

3. **数据一致性**:
   - 更新订阅时会验证课程是否存在
   - 自动过滤掉无效的课程ID

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 1002 | 参数验证失败 |
| 1101 | 未登录（需要登录的接口） |
| 1112 | 订阅数据解析失败 |
| 1113 | 课程已订阅 |
| 1116 | 用户不存在 |
| 1201 | 课程不存在 |
| 429 | 请求频率超限（超过 60 次/分钟） |

---

## 测试用例建议

### 1. 获取用户订阅列表
- ✅ 获取有订阅的用户列表
- ✅ 获取无订阅的用户列表（返回空数组）
- ✅ 用户不存在返回 1116
- ✅ userId 无效（0、负数）返回 1002
- ✅ 不需要登录

### 2. 订阅课程
- ✅ 成功订阅课程
- ✅ 返回更新后的订阅ID数组
- ✅ 课程不存在返回 1201
- ✅ 重复订阅（取决于配置）
- ✅ courseId 缺失/无效返回 1002
- ✅ 未登录返回 1101

### 3. 取消订阅
- ✅ 成功取消订阅
- ✅ 返回更新后的订阅ID数组
- ✅ 课程不存在返回 1201
- ✅ 课程未订阅（幂等操作，返回成功）
- ✅ courseId 无效返回 1002
- ✅ 未登录返回 1101

---

## 注意事项

1. **登录要求**:
   - `getUserSubscriptions` 不需要登录（公开查看他人订阅）
   - 其他所有操作都需要登录

2. **幂等性**:
   - `subscribe`: 重复订阅的处理取决于配置（可能报错或忽略）
   - `unsubscribe`: 取消不存在的订阅不报错（幂等操作）

3. **事件发布**:
   - 订阅/取消订阅会发布事件，触发统计更新
   - 事件类型：`ContentBookmarkedEvent`, `ContentUnbookmarkedEvent`

4. **数据格式**:
   - 订阅数据存储在 user 表的 subscriptions 字段（逗号分隔字符串）
   - 顺序即用户的排序偏好

5. **返回值差异**:
   - `getUserSubscriptions`: 返回 `List<CourseSummaryDTO>`（完整课程信息）
   - `subscribe/unsubscribe`: 返回 `int[]`（课程ID数组）

6. **跨域验证**:
   - 所有操作都会验证课程是否存在

---

*此文档用于指导前后端开发和接口对接，确保数据结构和业务逻辑的一致性*
