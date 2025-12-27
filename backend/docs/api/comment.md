# 评论管理接口文档 (CommentsController)

## 基本信息

- **Controller**: `CommentsController.java`
- **基础路径**: `/api/v1`
- **Rate Limit**: 40 requests/minute (per user)
- **前端 API**: `web/src/api/modules/comment.ts`

## DTO 类型说明

### CommentSummaryDTO (评论摘要信息)
用途：基础评论信息，不包含用户名和点赞状态

```json
{
  "id": 1,
  "content": "这是一条评论内容...",
  "objectType": 0,
  "objectId": 123,
  "replyCount": 5,
  "replyToCommentId": null,
  "creatorId": 83,
  "toUserId": null,
  "upvoteCount": 10,
  "state": 1,
  "score": 95.5,
  "createdAt": "2025-01-18 10:00:00"
}
```

**字段说明**：
- `id` (Long): 评论ID
- `content` (String): 评论内容（原始文本或 Markdown）
- `objectType` (Integer): 评论对象类型
  - `0`: 帖子（Post）
  - `1`: 节点（Node）
  - `2`: 课程（Course）
  - `3`: 用户（User）
  - `4`: 路线图（Roadmap）
- `objectId` (Long): 评论对象ID（对应对象的ID）
- `replyCount` (Integer): 回复数量，该评论下的直接回复总数
- `replyToCommentId` (Long): 回复的评论ID
  - `null`: 顶级评论
  - 非空: 回复某条评论
- `creatorId` (Long): 创建者用户ID
- `toUserId` (Long): 被回复的用户ID
  - `null`: 顶级评论
  - 非空: 回复某条评论的作者ID
- `upvoteCount` (Integer): 点赞数
- `state` (Integer): 评论状态
  - `0`: PENDING (待审核)
  - `1`: APPROVED (已通过)
  - `2`: REJECTED (已拒绝)
  - `3`: BANNED (已封禁)
- `score` (Double): 评论分数，用于排序（基于点赞数、时间等因素）
- `createdAt` (String): 创建时间，格式：`yyyy-MM-dd HH:mm:ss`

**使用场景**：
- 管理端评论列表
- 不需要用户详细信息和点赞状态的场景

---

### CommentDetailDTO (评论详情)
用途：包含用户名和点赞状态的评论信息

**继承关系**: CommentDetailDTO extends CommentSummaryDTO

```json
{
  "id": 1,
  "content": "这是一条评论内容...",
  "objectType": 0,
  "objectId": 123,
  "replyCount": 5,
  "replyToCommentId": null,
  "creatorId": 83,
  "toUserId": null,
  "upvoteCount": 10,
  "state": 1,
  "score": 95.5,
  "createdAt": "2025-01-18 10:00:00",
  "creatorName": "user123",
  "toUserName": null,
  "upvoted": false
}
```

**完整字段说明**：

**从 CommentSummaryDTO 继承的字段**：
- `id`, `content`, `objectType`, `objectId`, `replyCount`, `replyToCommentId`, `creatorId`, `toUserId`, `upvoteCount`, `state`, `score`, `createdAt`

**CommentDetailDTO 特有字段**：
- `creatorName` (String): 创建者用户名
  - 动态填充：通过 creatorId 查询用户信息获得
- `toUserName` (String): 被回复用户名
  - 动态填充：通过 toUserId 查询用户信息获得
  - `null`: 顶级评论时为空
- `upvoted` (Boolean): 当前用户是否已点赞
  - 动态填充：根据当前登录用户ID和评论ID查询点赞关系
  - `true`: 已点赞
  - `false`: 未点赞
  - `null`: 未登录用户

**使用场景**：
- 用户端评论详情展示
- 获取某条评论的回复列表
- 创建评论后返回完整信息

---

### CommentWithRepliesDTO (带回复的评论)
用途：评论树结构展示（父评论 + 子评论列表）

**继承关系**: CommentWithRepliesDTO extends CommentDetailDTO

```json
{
  "id": 1,
  "content": "这是一条顶级评论...",
  "objectType": 0,
  "objectId": 123,
  "replyCount": 5,
  "replyToCommentId": null,
  "creatorId": 83,
  "toUserId": null,
  "upvoteCount": 10,
  "state": 1,
  "score": 95.5,
  "createdAt": "2025-01-18 10:00:00",
  "creatorName": "user123",
  "toUserName": null,
  "upvoted": false,
  "children": [
    {
      "id": 2,
      "content": "这是一条回复...",
      "objectType": 0,
      "objectId": 123,
      "replyCount": 0,
      "replyToCommentId": 1,
      "creatorId": 84,
      "toUserId": 83,
      "upvoteCount": 3,
      "state": 1,
      "score": 80.0,
      "createdAt": "2025-01-18 11:00:00",
      "creatorName": "user456",
      "toUserName": "user123",
      "upvoted": true
    }
  ]
}
```

**完整字段说明**：

**从 CommentDetailDTO 继承的字段**：
- 所有 CommentSummaryDTO 字段
- `creatorName`: 创建者用户名
- `toUserName`: 被回复用户名
- `upvoted`: 是否已点赞

**CommentWithRepliesDTO 特有字段**：
- `children` (List<CommentDetailDTO>): 子评论列表（直接回复）
  - 动态填充：查询 replyToCommentId = 当前评论ID 的评论列表
  - 空列表: 表示没有回复，而非 null
  - 子评论按创建时间或分数排序
  - 通常限制返回数量（如最多显示 3 条，超过则需要"查看更多"）

**数据结构说明**：
- 通常只支持**两层结构**（父评论 + 直接回复）
- children 中的评论类型为 `CommentDetailDTO`（不再嵌套）
- 如果子评论有更多回复，需要通过"获取评论回复"接口获取

**使用场景**：
- 获取帖子的评论列表（需要展示嵌套回复）
- 获取节点的评论列表（需要展示嵌套回复）
- 需要显示评论树状结构的场景

---

## 接口列表

## 1. 创建评论

**接口路径**: `POST /api/v1/comments`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体** (`CreateCommentRequest`):
```json
{
  "objectId": 123,
  "objectType": 0,
  "replyTo": 10,
  "toUser": 84,
  "content": "这是一条评论..."
}
```

**请求参数说明**:
- `objectId` (Long, 必填): 对象ID，必须大于0
- `objectType` (Integer, 必填): 对象类型，取值范围 0-4
  - `0`: 帖子（Post）
  - `1`: 节点（Node）
  - `2`: 课程（Course）
  - `3`: 用户（User）
  - `4`: 路线图（Roadmap）
- `replyTo` (Long, 可选): 回复的评论ID，必须大于0
  - 不传或为 null: 创建顶级评论
  - 传值: 回复某条评论
- `toUser` (Long, 可选): 回复子评论时，被回复的用户ID，必须大于0
  - 场景：评论只有两级，回复子评论时需要 @ 用户
- `content` (String, 必填): 评论内容，不能为空
  - 长度限制：通过配置项 `comment-content` 控制

**返回类型**: `CommentDetailDTO`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "content": "这是一条评论...",
    "objectType": 0,
    "objectId": 123,
    "replyCount": 0,
    "replyToCommentId": null,
    "creatorId": 83,
    "toUserId": null,
    "upvoteCount": 0,
    "state": 1,
    "score": 0.0,
    "createdAt": "2025-01-18 10:00:00",
    "creatorName": "user123",
    "toUserName": null,
    "upvoted": false
  }
}
```

**前端调用**:
```typescript
// API 调用
commentApi.createComment(objectId, objectType, replyTo?, toUser?, content)

// 实际使用 (CommentSection.vue:150)
const { execute: executeCreateComment } = useMutation(
  (payload: { objectId: number; objectType: ObjectType; replyTo?: number; toUser?: number; content: string }) =>
    commentApi.createComment(payload.objectId, payload.objectType, payload.replyTo, payload.toUser, payload.content),
  {
    successMessage: '评论发表成功',
    onSuccess: (data) => {
      // 刷新评论列表
      loadComments()
    }
  }
)

// 创建顶级评论
await executeCreateComment({
  objectId: postId,
  objectType: ObjectType.POST,
  content: '评论内容...'
})

// 回复评论
await executeCreateComment({
  objectId: postId,
  objectType: ObjectType.POST,
  replyTo: commentId,
  toUser: userId,
  content: '回复内容...'
})
```

**使用场景**:
- `CommentSection.vue` - 评论组件
- 帖子、节点、课程等对象的评论功能

---

## 2. 获取对象评论列表

**接口路径**: `GET /api/v1/comments`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| objectId | Long | 是 | - | 对象ID，必须大于0 |
| objectType | Integer | 是 | - | 对象类型，必须大于0（0-4） |
| lastScore | Double | 否 | - | 分页游标：上一页最后一条的分数 |
| lastId | Long | 否 | - | 分页游标：上一页最后一条的ID |

**排序规则**:
- 按 `score` 降序、`id` 降序排序
- score 高的在前，score 相同时 id 大的在前（热门评论优先）

**返回类型**: `KeysetPageResponse<CommentWithRepliesDTO>`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "items": [
      {
        "id": 1,
        "content": "这是一条顶级评论...",
        "objectType": 0,
        "objectId": 123,
        "replyCount": 2,
        "replyToCommentId": null,
        "creatorId": 83,
        "toUserId": null,
        "upvoteCount": 10,
        "state": 1,
        "score": 95.5,
        "createdAt": "2025-01-18 10:00:00",
        "creatorName": "user123",
        "toUserName": null,
        "upvoted": false,
        "children": [
          {
            "id": 2,
            "content": "这是一条回复...",
            "objectType": 0,
            "objectId": 123,
            "replyCount": 0,
            "replyToCommentId": 1,
            "creatorId": 84,
            "toUserId": 83,
            "upvoteCount": 3,
            "state": 1,
            "score": 80.0,
            "createdAt": "2025-01-18 11:00:00",
            "creatorName": "user456",
            "toUserName": "user123",
            "upvoted": true
          }
        ]
      }
    ],
    "hasMore": true,
    "nextCursor": {
      "lastScore": 95.5,
      "lastId": 1
    }
  }
}
```

**KeysetPageResponse 字段说明**:
- `items` (List): 评论列表（带子评论）
- `hasMore` (Boolean): 是否有更多数据
- `nextCursor` (Object): 下一页游标信息（当 hasMore=false 时不出现）
  - `lastScore` (Double): 最后一条记录的分数
  - `lastId` (Long): 最后一条记录的ID

**前端调用**:
```typescript
// API 调用
commentApi.getComments(objectId, objectType, lastScore?, lastId?)

// 实际使用 (CommentSection.vue:80)
const { data: response, execute: loadComments } = useFetch<KeysetPageResponse<Comment>>({
  fetchFn: () => commentApi.getComments(objectId, objectType, lastScore.value, lastId.value),
  immediate: true
})
const comments = response.items
const hasMore = response.hasMore

// 加载更多
const loadMore = async () => {
  if (!hasMore.value) return

  const nextCursor = response.value?.nextCursor
  if (nextCursor) {
    lastScore.value = nextCursor.lastScore
    lastId.value = nextCursor.lastId
    await loadComments()
  }
}
```

**使用场景**:
- `CommentSection.vue` - 评论区组件
- 帖子详情页评论列表
- 节点评论列表
- 支持分页加载更多

---

## 3. 获取评论回复列表

**接口路径**: `GET /api/v1/comments/{id}/replies`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 评论ID，必须大于0 |

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| lastScore | Double | 否 | - | 分页游标：上一页最后一条的分数 |
| lastId | Long | 否 | - | 分页游标：上一页最后一条的ID |

**排序规则**:
- 按 `score` 降序、`id` 降序排序
- score 高的在前，score 相同时 id 大的在前（热门回复优先）

**返回类型**: `KeysetPageResponse<CommentDetailDTO>`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "items": [
      {
        "id": 2,
        "content": "这是一条回复...",
        "objectType": 0,
        "objectId": 123,
        "replyCount": 0,
        "replyToCommentId": 1,
        "creatorId": 84,
        "toUserId": 83,
        "upvoteCount": 3,
        "state": 1,
        "score": 80.0,
        "createdAt": "2025-01-18 11:00:00",
        "creatorName": "user456",
        "toUserName": "user123",
        "upvoted": true
      },
      {
        "id": 3,
        "content": "这是另一条回复...",
        "objectType": 0,
        "objectId": 123,
        "replyCount": 0,
        "replyToCommentId": 1,
        "creatorId": 85,
        "toUserId": 83,
        "upvoteCount": 1,
        "state": 1,
        "score": 70.0,
        "createdAt": "2025-01-18 12:00:00",
        "creatorName": "user789",
        "toUserName": "user123",
        "upvoted": false
      }
    ],
    "hasMore": false,
    "nextCursor": null
  }
}
```

**KeysetPageResponse 字段说明**:
- `items` (List): 回复列表
- `hasMore` (Boolean): 是否有更多数据
- `nextCursor` (Object): 下一页游标信息（当 hasMore=false 时为 null）
  - `lastScore` (Double): 最后一条记录的分数
  - `lastId` (Long): 最后一条记录的ID

**前端调用**:
```typescript
// API 调用
commentApi.getCommentReplies(id, lastScore?, lastId?)

// 实际使用 (CommentSection.vue:200)
const { data: response, execute: loadReplies } = useFetch<KeysetPageResponse<Comment>>({
  fetchFn: () => commentApi.getCommentReplies(commentId, lastScore.value, lastId.value),
  immediate: false
})
const replies = response.items
const hasMore = response.hasMore

// 展开回复列表
const expandReplies = async (commentId: number) => {
  await loadReplies()
}

// 加载更多回复
const loadMoreReplies = async () => {
  if (!hasMore.value) return

  const nextCursor = response.value?.nextCursor
  if (nextCursor) {
    lastScore.value = nextCursor.lastScore
    lastId.value = nextCursor.lastId
    await loadReplies()
  }
}
```

**使用场景**:
- `CommentSection.vue` - 展开"查看更多回复"
- 当父评论的子评论数量超过初始显示数量时使用
- 支持分页加载更多回复

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数验证失败 |
| 401 | 未登录（需要登录的接口） |
| 403 | 无权限 |
| 404 | 资源不存在（评论不存在） |
| 429 | 请求频率超限（超过 40 次/分钟） |
| 500 | 服务器内部错误 |

---

## DTO 继承体系图

```
CommentSummaryDTO (基础)
    ↓ extends
CommentDetailDTO (+ creatorName, toUserName, upvoted)
    ↓ extends
CommentWithRepliesDTO (+ children)
```

**设计说明**：
- **CommentSummaryDTO**: 最基础的评论信息，无用户名和点赞状态
- **CommentDetailDTO**: 添加用户名和点赞状态，用于用户端展示
- **CommentWithRepliesDTO**: 添加子评论列表，用于树状结构展示

---

## 评论结构说明

### 两层评论结构

系统采用**两层评论结构**：
1. **顶级评论**：直接评论对象（帖子、节点等）
2. **回复评论**：回复顶级评论或其他回复

**示例**：
```
帖子 123
├─ 评论1（顶级评论，replyToCommentId=null）
│  ├─ 评论2（回复评论1，replyToCommentId=1, toUserId=user1）
│  └─ 评论3（回复评论1，replyToCommentId=1, toUserId=user1）
└─ 评论4（顶级评论，replyToCommentId=null）
   ├─ 评论5（回复评论4，replyToCommentId=4, toUserId=user4）
   └─ 评论6（回复评论5，replyToCommentId=4, toUserId=user5）← 注意：replyToCommentId 仍指向顶级评论
```

**关键点**：
- `replyToCommentId` 始终指向**顶级评论**
- `toUserId` 指向**被回复的用户**（可能是顶级评论作者或其他回复者）
- 子评论不再嵌套，只有两层

---

## 测试用例建议

### 1. 创建评论
- ✅ 未登录返回 401
- ✅ 必填字段缺失返回 400
- ✅ 成功创建顶级评论
- ✅ 成功创建回复评论
- ✅ objectType 不在 0-4 范围返回 400
- ✅ 内容长度超限返回 400
- ✅ 返回 CommentDetailDTO（包含用户名和点赞状态）

### 2. 获取对象评论列表
- ✅ 未登录返回 401
- ✅ objectId 或 objectType 缺失返回 400
- ✅ 成功获取评论列表
- ✅ 返回 KeysetPageResponse<CommentWithRepliesDTO>
- ✅ 子评论正确嵌套在父评论下
- ✅ 分页功能正常（lastScore + lastId）
- ✅ 按 score 降序、id 降序排序

### 3. 获取评论回复列表
- ✅ 未登录返回 401
- ✅ 评论ID不存在返回 404
- ✅ 成功获取回复列表
- ✅ 返回 KeysetPageResponse<CommentDetailDTO>
- ✅ 分页功能正常
- ✅ 按 score 降序、id 降序排序

---

## 注意事项

1. **登录要求**：
   - 所有评论接口都需要登录
   - 未登录用户无法查看和创建评论

2. **分页策略**：
   - 使用 Keyset 分页（lastScore + lastId），性能优于传统 offset
   - 适合大数据量场景

3. **评论层级**：
   - 只支持两层评论结构（顶级评论 + 回复）
   - 回复子评论时使用 @ 用户的方式

4. **排序规则**：
   - 评论按分数（score）和ID排序，热门评论优先
   - 分数基于点赞数、时间等因素计算

5. **DTO 选择**：
   - 列表场景使用 `CommentWithRepliesDTO`（含子评论）
   - 回复列表使用 `CommentDetailDTO`（不含子评论）
   - 创建评论返回 `CommentDetailDTO`

6. **子评论数量限制**：
   - `CommentWithRepliesDTO.children` 通常只返回部分子评论（如 3 条）
   - 超过的子评论需通过"获取评论回复"接口加载

---

*此文档用于指导前后端开发和接口对接，确保数据结构和业务逻辑的一致性*
