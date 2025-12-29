# 关注管理接口文档 (FollowsController)

## 基本信息

- **Controller**: `FollowsController.java`
- **基础路径**: `/api/v1`
- **Rate Limit**: 60 requests/minute (per user)
- **前端 API**: `web/src/api/modules/user.ts` (followApi)

## 概述

关注功能允许用户关注其他用户，建立社交关系。系统采用事件驱动架构，通过发布领域事件来处理消息通知和统计更新。

**核心特性**:
- 支持关注和取消关注操作
- 幂等性设计：重复关注/取消关注不会报错
- 基于时间游标的分页查询
- 事件驱动：关注/取消关注操作会发布领域事件

---

## DTO 类型说明

### FolloweeDTO (关注者信息)
用途：返回关注列表中的用户信息

```json
{
  "id": 123,
  "name": "用户名",
  "biography": "个人简介",
  "createdAt": "2025-01-20 10:30:00"
}
```

**字段说明**：
- `id` (Long): 用户ID
- `name` (String): 用户名
- `biography` (String): 个人简介/签名
- `createdAt` (String): 关注时间
  - 格式：`yyyy-MM-dd HH:mm:ss`
  - 表示当前用户关注该用户的时间

**使用场景**：
- 获取关注列表接口的返回数据
- 显示用户的关注列表

---

## 接口列表

## 1. 关注用户

**接口路径**: `POST /api/v1/follows`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "followeeId": 123
}
```

**请求参数说明**:
- `followeeId` (Long, 必填): 被关注用户的ID
  - 验证规则：
    - 不能为空 (`@NotNull`)
    - 必须大于0 (`@Positive`)

**返回类型**: `Void` (无返回数据)

**业务逻辑说明**:

### 关注流程
1. **参数验证**
   - 验证 `followeeId` 不为空且大于0

2. **验证被关注者存在**
   - 查询被关注用户是否存在
   - 不存在则抛出 `NOT_FOUND` (1007) 异常

3. **检查是否已关注**
   - 查询关注关系是否已存在
   - 如果已关注，则返回成功（幂等操作）

4. **创建关注记录**
   - 插入关注记录到数据库
   - 记录关注时间

5. **发布领域事件**
   - 发布 `UserFollowedEvent` 事件
   - 事件订阅者会处理：
     - 发送关注通知（可选）
     - 更新关注统计数据
     - 更新推荐算法（可选）

**幂等性**:
- 重复关注同一用户不会报错
- 只有首次关注时会创建记录和发布事件
- 后续重复关注直接返回成功

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "timestamp": 1766850806720
}
```

**前端调用**:
```typescript
// API 调用
import { followApi } from '@/api/modules/user'

// 关注用户
await followApi.follow(123)

// 实际使用示例
const handleFollow = async (userId: number) => {
  try {
    await followApi.follow(userId)
    console.log('关注成功')
    // 更新UI状态
  } catch (error) {
    console.error('关注失败', error)
  }
}
```

**使用场景**:
- 用户个人主页的"关注"按钮
- 用户列表中的快捷关注按钮
- 推荐用户列表的关注操作

---

## 2. 取消关注

**接口路径**: `DELETE /api/v1/follows/{followeeId}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| followeeId | Long | 是 | 被关注用户的ID，必须大于0 |

**返回类型**: `Void` (无返回数据)

**业务逻辑说明**:

### 取消关注流程
1. **参数验证**
   - 验证 `followeeId` 不为空且大于0

2. **验证被关注者存在**
   - 查询被关注用户是否存在
   - 不存在则抛出 `NOT_FOUND` (1007) 异常

3. **检查关注关系**
   - 查询关注记录是否存在
   - 如果不存在，则返回成功（幂等操作）

4. **删除关注记录**
   - 从数据库删除关注记录

5. **发布领域事件**
   - 发布 `UserUnfollowedEvent` 事件
   - 事件订阅者会处理：
     - 更新关注统计数据
     - 更新推荐算法（可选）

**幂等性**:
- 重复取消关注同一用户不会报错
- 只有关注关系存在时才会删除记录和发布事件
- 后续重复取消关注直接返回成功

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "timestamp": 1766850806720
}
```

**前端调用**:
```typescript
// API 调用
import { followApi } from '@/api/modules/user'

// 取消关注
await followApi.unfollow(123)

// 实际使用示例
const handleUnfollow = async (userId: number) => {
  try {
    await followApi.unfollow(userId)
    console.log('取消关注成功')
    // 更新UI状态
  } catch (error) {
    console.error('取消关注失败', error)
  }
}
```

**使用场景**:
- 用户个人主页的"已关注"按钮
- 关注列表中的取消关注操作
- 批量管理关注时的取消操作

---

## 3. 获取关注列表

**接口路径**: `GET /api/v1/users/{userId}/followees`

**是否需要登录**: 否（公开接口）

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID，必须大于0 |

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| lastCreateTime | String | 否 | 当前时间 | 分页游标：上一页最后一条的关注时间 |

**分页说明**:
- 使用基于时间游标的分页
- 每页返回 10 条记录（固定）
- 按关注时间降序排列（最新关注的在前）
- 第一页请求不传 `lastCreateTime` 参数
- 后续页面传入上一页最后一条记录的 `createdAt` 值

**返回类型**: `List<FolloweeDTO>`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 456,
      "name": "张三",
      "biography": "全栈开发工程师",
      "createdAt": "2025-01-20 15:30:00"
    },
    {
      "id": 789,
      "name": "李四",
      "biography": "产品经理",
      "createdAt": "2025-01-20 14:20:00"
    },
    {
      "id": 321,
      "name": "王五",
      "biography": "UI设计师",
      "createdAt": "2025-01-20 10:15:00"
    }
  ],
  "timestamp": 1766850806720
}
```

**业务逻辑说明**:

### 查询流程
1. **参数验证**
   - 验证 `userId` 不为空且大于0

2. **验证用户存在**
   - 查询用户是否存在
   - 不存在则抛出 `NOT_FOUND` (1007) 异常

3. **解析时间游标**
   - 如果未提供 `lastCreateTime`，使用当前时间（第一页）
   - 如果提供了，解析为 `LocalDateTime` 对象
   - 格式：`yyyy-MM-dd HH:mm:ss`

4. **查询关注记录**
   - 查询该用户在指定时间之前的关注记录
   - 按关注时间降序排列
   - 限制返回 10 条

5. **批量查询用户信息**
   - 提取所有被关注者的ID
   - 批量查询用户信息（姓名、简介等）

6. **组装返回数据**
   - 将关注记录和用户信息合并
   - 转换为 `FolloweeDTO` 列表

**空列表场景**:
- 用户没有关注任何人
- 已经加载到最后一页

**前端调用**:
```typescript
// API 调用
import { followApi } from '@/api/modules/user'

// 获取第一页
const firstPage = await followApi.getFollowees(123)

// 获取下一页（传入上一页最后一条的 createdAt）
const lastItem = firstPage.data[firstPage.data.length - 1]
const nextPage = await followApi.getFollowees(123, lastItem.createdAt)

// 实际使用示例 - 无限滚动加载
const followees = ref<User[]>([])
const lastCreateTime = ref<string | undefined>(undefined)
const hasMore = ref(true)

const loadFollowees = async () => {
  try {
    const response = await followApi.getFollowees(userId, lastCreateTime.value)

    if (response.data.length > 0) {
      followees.value.push(...response.data)
      // 更新游标
      lastCreateTime.value = response.data[response.data.length - 1].createdAt
      hasMore.value = response.data.length === 10 // 如果返回少于10条，说明没有更多了
    } else {
      hasMore.value = false
    }
  } catch (error) {
    console.error('加载关注列表失败', error)
  }
}
```

**使用场景**:
- 用户个人主页的"关注"标签页
- 查看其他用户的关注列表
- 推荐系统分析用户兴趣

---

## 错误码说明

| 错误码 | 名称 | 说明 |
|--------|------|------|
| 200 | OK | 成功 |
| 1002 | INVALID_PARAMETER | 参数验证失败 |
| 1007 | NOT_FOUND | 用户不存在 |
| 1101 | USER_NOT_LOGIN | 用户未登录（关注/取消关注需要登录） |
| 2301 | RATE_LIMIT_EXCEEDED | 请求频率超限（60次/分钟） |

**常见参数验证错误 (1002)**:
- `被关注用户ID不能为空` - followeeId 为 null
- `被关注用户ID必须大于0` - followeeId ≤ 0
- `用户ID不能为空` - userId 为 null
- `用户ID必须大于0` - userId ≤ 0

**业务错误详细说明**:
- **1007 (NOT_FOUND)**: 被关注的用户不存在或已被删除
- **1101 (USER_NOT_LOGIN)**: 执行关注/取消关注操作时未登录

---

## 业务规则说明

### 1. 幂等性设计

**关注操作**:
- 首次关注：创建记录 + 发布事件
- 重复关注：直接返回成功，不创建记录，不发布事件

**取消关注操作**:
- 存在关注关系：删除记录 + 发布事件
- 不存在关注关系：直接返回成功，不删除记录，不发布事件

**好处**:
- 避免前端重复点击导致的错误
- 支持并发请求的场景
- 提升用户体验

### 2. 关注限制

**当前限制**:
- 无关注数量限制
- 可以关注任何存在的用户
- 可以关注自己（根据业务需求调整）

**可能的扩展**:
- 添加最大关注数限制
- 禁止关注自己
- 支持私密账号（需要批准）
- 黑名单/屏蔽功能

### 3. 分页设计

**使用时间游标分页的优点**:
- 避免传统偏移分页在数据变化时的数据重复/遗漏问题
- 性能稳定，不受数据总量影响
- 适合实时数据流

**缺点**:
- 无法跳转到指定页码
- 无法显示总页数

**适用场景**:
- 社交媒体的关注列表
- 无限滚动的数据加载
- 实时更新的数据流

---

## 领域事件说明

### UserFollowedEvent (用户关注事件)
当用户首次关注另一用户时发布

**事件数据**:
- `followerId` (Long): 关注者ID
- `followeeId` (Long): 被关注者ID

**事件订阅者**:
- 消息服务 - 发送关注通知给被关注者
- 统计服务 - 更新用户的关注数/粉丝数
- 推荐服务 - 更新推荐算法（可选）
- 活动服务 - 记录用户活动日志（可选）

### UserUnfollowedEvent (用户取消关注事件)
当用户取消关注另一用户时发布

**事件数据**:
- `followerId` (Long): 关注者ID
- `followeeId` (Long): 被关注者ID

**事件订阅者**:
- 统计服务 - 更新用户的关注数/粉丝数
- 推荐服务 - 更新推荐算法（可选）

---

## 性能优化建议

### 1. 后端优化
- ✅ 使用事务保证数据一致性
- ✅ 幂等性设计避免重复操作
- ✅ 批量查询用户信息，减少数据库查询
- ✅ 使用时间游标分页，性能稳定
- ⚠️ 建议：关注列表添加缓存（Redis）

### 2. 前端优化
- ✅ 使用防抖/节流避免重复点击
- ✅ 乐观更新：先更新UI，再等待服务器响应
- ✅ 无限滚动加载关注列表
- ⚠️ 建议：缓存已加载的关注列表数据

### 3. 数据库优化
- ✅ 关注表索引：`(follower_id, followee_id)` 联合索引
- ✅ 时间游标查询索引：`(follower_id, created_at)` 联合索引
- ⚠️ 建议：考虑关注数量过大时的分表策略

---

## 测试用例建议

### 1. 关注用户
- ✅ 未登录返回 1101
- ✅ followeeId 为空返回 1002
- ✅ followeeId 为 0 返回 1002
- ✅ followeeId 为负数返回 1002
- ✅ 被关注用户不存在返回 1007
- ✅ 首次关注成功
- ✅ 重复关注返回成功（幂等）
- ✅ 验证关注记录已创建
- ✅ 验证事件已发布

### 2. 取消关注
- ✅ 未登录返回 1101
- ✅ followeeId 为空返回 1002
- ✅ followeeId 为 0 返回 1002
- ✅ 被关注用户不存在返回 1007
- ✅ 取消关注成功
- ✅ 重复取消关注返回成功（幂等）
- ✅ 验证关注记录已删除
- ✅ 验证事件已发布

### 3. 获取关注列表
- ✅ userId 为空返回 1002
- ✅ userId 为 0 返回 1002
- ✅ 用户不存在返回 1007
- ✅ 成功获取第一页（不传 lastCreateTime）
- ✅ 成功获取下一页（传 lastCreateTime）
- ✅ 返回空列表（用户没有关注任何人）
- ✅ 验证分页正确（每页10条）
- ✅ 验证排序正确（按时间降序）
- ✅ 验证用户信息正确填充

### 4. 边界条件
- ✅ 关注自己（根据业务需求可能需要禁止）
- ✅ 时间格式错误
- ✅ 并发关注/取消关注

---

## 注意事项

1. **关注/取消关注需要登录**
   - POST 和 DELETE 接口需要 `@SaCheckLogin`
   - GET 接口不需要登录（公开数据）

2. **幂等性保证**
   - 重复操作不会报错
   - 前端可以安全地重试

3. **分页参数格式**
   - `lastCreateTime` 格式必须是 `yyyy-MM-dd HH:mm:ss`
   - 建议直接使用上一次返回的 `createdAt` 值

4. **事件驱动架构**
   - 关注操作是异步的
   - 通知和统计更新通过事件处理
   - 确保事件处理的幂等性

5. **性能考虑**
   - 关注列表固定每页10条
   - 批量查询用户信息优化性能
   - 考虑添加缓存层

---

## 相关文档

- [用户管理接口文档](./user.md)
- [消息通知接口文档](./message.md)

---

*此文档用于指导前后端开发和接口对接，确保关注功能的正确实现*
