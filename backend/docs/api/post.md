# 帖子管理接口文档 (PostsController)

## 基本信息

- **Controller**: `PostsController.java`
- **基础路径**: `/api/v1`
- **Rate Limit**: 30 requests/minute (per user)
- **前端 API**: `web/src/api/modules/post.ts`

## DTO 类型说明

### PostSummaryDTO (帖子摘要信息)
用途：基础帖子信息，不包含关联对象

```json
{
  "id": 1,
  "content": "帖子内容...",
  "nodeId": 10,
  "creatorId": 83,
  "type": 2,
  "twice": 5,
  "helpful": 10,
  "commentCount": 15,
  "viewCount": 100,
  "state": 1,
  "score": 95.5,
  "createdAt": "2025-01-18 10:00:00",
  "updatedAt": "2025-01-18 15:30:00"
}
```

**字段说明**：
- `id` (Long): 帖子ID
- `content` (String): 帖子内容（支持 Markdown 格式）
- `nodeId` (Long): 节点ID，帖子所属的课程节点
- `creatorId` (Long): 创建者ID
- `type` (Integer): 帖子类型
  - `0`: 普通帖子
  - `1`: 内容帖子（目录）
  - `2`: 文章帖子
- `twice` (Integer): 二次评论数
- `helpful` (Integer): 有用评论数
- `commentCount` (Integer): 总评论数
- `viewCount` (Integer): 浏览数
- `state` (Integer): 帖子状态
  - `0`: SUBMITTED (待审核)
  - `1`: PUBLISHED (已发布)
  - `2`: REJECTED (已拒绝)
- `score` (Double): 帖子得分，用于排序
- `createdAt` (String): 创建时间，格式：`yyyy-MM-dd HH:mm:ss`
- `updatedAt` (String): 更新时间，格式：`yyyy-MM-dd HH:mm:ss`

**使用场景**：
- 作为其他 PostDTO 的基类
- 不需要关联信息的场景
- 更新帖子后的返回值

---

### PostWithCreatorDTO (帖子+创建者信息)
用途：包含创建者信息的帖子

**继承关系**: PostWithCreatorDTO extends PostSummaryDTO

```json
{
  "id": 1,
  "content": "帖子内容...",
  "nodeId": 10,
  "creatorId": 83,
  "type": 2,
  "twice": 5,
  "helpful": 10,
  "commentCount": 15,
  "viewCount": 100,
  "state": 1,
  "score": 95.5,
  "createdAt": "2025-01-18 10:00:00",
  "updatedAt": "2025-01-18 15:30:00",
  "creator": {
    "id": 83,
    "name": "user123",
    "avatar": "https://example.com/avatar.jpg"
  }
}
```

**完整字段说明**：

**从 PostSummaryDTO 继承的字段**：
- `id`, `content`, `nodeId`, `creatorId`, `type`, `twice`, `helpful`, `commentCount`, `viewCount`, `state`, `score`, `createdAt`, `updatedAt`

**PostWithCreatorDTO 特有字段**：
- `creator` (UserBriefDTO): 创建者信息
  - `id`: 用户ID
  - `name`: 用户名
  - `avatar`: 头像URL

**使用场景**：
- 帖子列表（基础展示）
- 需要显示作者的场景

---

### PostDetailDTO (帖子详情)
用途：包含完整节点和课程信息的帖子

**继承关系**: PostDetailDTO extends PostWithCreatorDTO

```json
{
  "id": 1,
  "content": "帖子内容...",
  "nodeId": 10,
  "creatorId": 83,
  "type": 2,
  "twice": 5,
  "helpful": 10,
  "commentCount": 15,
  "viewCount": 100,
  "state": 1,
  "score": 95.5,
  "createdAt": "2025-01-18 10:00:00",
  "updatedAt": "2025-01-18 15:30:00",
  "creator": {
    "id": 83,
    "name": "user123",
    "avatar": "https://example.com/avatar.jpg"
  },
  "node": {
    "id": 10,
    "name": "节点名称",
    "course": {
      "id": 608,
      "name": "高三政治"
    }
  }
}
```

**完整字段说明**：

**从 PostWithCreatorDTO 继承的字段**：
- 所有 PostSummaryDTO 字段
- `creator`: 创建者信息

**PostDetailDTO 特有字段**：
- `node` (NodeWithCourseBriefDTO): 节点信息
  - `id`: 节点ID
  - `name`: 节点名称
  - `course`: 课程简要信息（CourseBriefDTO）
    - `id`: 课程ID
    - `name`: 课程名称

**使用场景**：
- 帖子详情页
- 需要显示完整节点和课程信息的场景

---

### PostWithVoteDTO (帖子+投票状态)
用途：包含用户投票状态的帖子

**继承关系**: PostWithVoteDTO extends PostWithCreatorDTO

```json
{
  "id": 1,
  "content": "帖子内容...",
  "nodeId": 10,
  "creatorId": 83,
  "type": 2,
  "twice": 5,
  "helpful": 10,
  "commentCount": 15,
  "viewCount": 100,
  "state": 1,
  "score": 95.5,
  "createdAt": "2025-01-18 10:00:00",
  "updatedAt": "2025-01-18 15:30:00",
  "creator": {
    "id": 83,
    "name": "user123",
    "avatar": "https://example.com/avatar.jpg"
  },
  "voteType": 1
}
```

**完整字段说明**：

**从 PostWithCreatorDTO 继承的字段**：
- 所有 PostSummaryDTO 字段
- `creator`: 创建者信息

**PostWithVoteDTO 特有字段**：
- `voteType` (Integer): 投票类型（需要登录）
  - `0`: 未投票
  - `1`: Twice（双倍点赞）
  - `2`: Like（喜欢）

**使用场景**：
- 帖子列表（含用户投票状态）
- 不需要完整节点信息的轻量级场景
- 批量获取帖子接口（/posts）

---

### PostFullDTO (帖子完整信息)
用途：包含节点、创建者、投票状态的完整帖子信息

**继承关系**: PostFullDTO extends PostDetailDTO

```json
{
  "id": 1,
  "content": "帖子内容...",
  "nodeId": 10,
  "creatorId": 83,
  "type": 2,
  "twice": 5,
  "helpful": 10,
  "commentCount": 15,
  "viewCount": 100,
  "state": 1,
  "score": 95.5,
  "createdAt": "2025-01-18 10:00:00",
  "updatedAt": "2025-01-18 15:30:00",
  "creator": {
    "id": 83,
    "name": "user123",
    "avatar": "https://example.com/avatar.jpg"
  },
  "node": {
    "id": 10,
    "name": "节点名称",
    "course": {
      "id": 608,
      "name": "高三政治"
    }
  },
  "voteType": 1
}
```

**完整字段说明**：

**从 PostDetailDTO 继承的字段**：
- 所有 PostSummaryDTO 字段
- `creator`: 创建者信息
- `node`: 节点信息（包含课程信息）

**PostFullDTO 特有字段**：
- `voteType` (Integer): 投票类型（需要登录）
  - `0`: 未投票
  - `1`: Twice（双倍点赞）
  - `2`: Like（喜欢）

**使用场景**：
- 用户个人中心的帖子列表
- 需要所有关联信息和投票状态的场景

---

## 接口列表

## 1. 批量获取帖子

**接口路径**: `GET /api/v1/posts`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| ids | Long[] | 否 | - | 帖子ID列表，多个ID用逗号分隔 |
| nodeId | Long | 否 | - | 节点ID，必须大于0 |
| lastScore | Double | 否 | 0 | 分页游标：上一页最后一条的分数 |
| lastId | Long | 否 | 0 | 分页游标：上一页最后一条的ID |

**参数组合规则**:
1. **按ID批量查询**: 传 `ids` 参数
   - 返回 `List<PostWithVoteDTO>` 格式
   - 返回指定ID的帖子列表
2. **按节点分页查询**: 传 `nodeId` 参数（可配合分页参数）
   - 返回 `KeysetPageResponse<PostWithVoteDTO>` 格式
   - 返回该节点下的帖子列表，按分数排序

**返回类型**:
- 按 IDs 查询: `List<PostWithVoteDTO>`
- 按节点查询: `KeysetPageResponse<PostWithVoteDTO>`

**返回示例 - 按 IDs 查询**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "content": "帖子内容...",
      "nodeId": 10,
      "creatorId": 83,
      "type": 2,
      "twice": 5,
      "helpful": 10,
      "commentCount": 15,
      "viewCount": 100,
      "state": 1,
      "score": 95.5,
      "createdAt": "2025-01-18 10:00:00",
      "updatedAt": "2025-01-18 15:30:00",
      "creator": {
        "id": 83,
        "name": "user123",
        "avatar": "https://example.com/avatar.jpg"
      },
      "voteType": 1
    }
  ]
}
```

**返回示例 - 按节点分页查询**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "items": [
      {
        "id": 1,
        "content": "帖子内容...",
        "nodeId": 10,
        "creatorId": 83,
        "type": 2,
        "twice": 5,
        "helpful": 10,
        "commentCount": 15,
        "viewCount": 100,
        "state": 1,
        "score": 95.5,
        "createdAt": "2025-01-18 10:00:00",
        "updatedAt": "2025-01-18 15:30:00",
        "creator": {
          "id": 83,
          "name": "user123",
          "avatar": "https://example.com/avatar.jpg"
        },
        "voteType": 1
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
- `items` (List): 帖子列表
- `hasMore` (Boolean): 是否有更多数据
- `nextCursor` (Object): 下一页游标信息
  - `lastScore` (Double): 最后一条记录的分数
  - `lastId` (Long): 最后一条记录的ID

**前端调用**:
```typescript
// 按 IDs 批量查询
const { data: posts } = useFetch<Post[]>({
  fetchFn: () => postApi.getPosts([1, 2, 3]),
  immediate: true,
  defaultValue: []
})

// 按节点分页查询 (NodePostsPage.vue:245)
const { data: response } = useFetch<KeysetPageResponse<Post>>({
  fetchFn: () => postApi.getNodePosts(nodeId.value, lastScore.value, lastId.value),
  immediate: true
})
const posts = response.items
const hasMore = response.hasMore
```

**使用场景**:
- `NodePostsPage.vue` - 节点帖子列表页（分页查询）
- 批量获取指定帖子（IDs 查询）

---

## 2. 创建帖子

**接口路径**: `POST /api/v1/posts`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体** (`CreatePostRequest`):
```json
{
  "content": "帖子内容...",
  "nodeId": 10,
  "type": 2
}
```

**请求参数说明**:
- `content` (String, 必填): 帖子内容，不能为空
  - 长度限制：通过配置项 `post-content` 控制
- `nodeId` (Long, 必填): 节点ID，不能为空
- `type` (Integer, 必填): 帖子类型
  - `0`: 普通帖子
  - `1`: 内容帖子（目录）
  - `2`: 文章帖子

**返回类型**: `Void`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功"
}
```

**前端调用**:
```typescript
// API 调用
postApi.createPost(postData)

// 实际使用 (AddArticleDialog.vue:85)
const { execute: executeCreatePost } = useMutation(
  (payload: Partial<Post>) => postApi.createPost(payload),
  {
    successMessage: '帖子创建成功',
    onSuccess: () => {
      // 刷新列表
      emit('refresh')
    }
  }
)

// 调用方式
await executeCreatePost({
  content: '帖子内容...',
  nodeId: 10,
  type: 2
})
```

**使用场景**:
- `AddArticleDialog.vue` - 创建文章对话框
- `ContentReadPage.vue` - 创建内容帖子

---

## 3. 更新帖子

**接口路径**: `PUT /api/v1/posts/{id}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 帖子ID，必须大于0 |

**请求体** (`UpdatePostRequest`):
```json
{
  "content": "更新后的帖子内容..."
}
```

**请求参数说明**:
- `content` (String, 必填): 帖子内容，不能为空
  - 长度限制：最大 10000 字符

**权限验证**:
- 只有帖子创建者可以更新自己的帖子
- 非创建者更新会返回 403 错误

**返回类型**: `PostSummaryDTO`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "content": "更新后的帖子内容...",
    "nodeId": 10,
    "creatorId": 83,
    "type": 2,
    "twice": 5,
    "helpful": 10,
    "commentCount": 15,
    "viewCount": 100,
    "state": 1,
    "score": 95.5,
    "createdAt": "2025-01-18 10:00:00",
    "updatedAt": "2025-01-18 16:00:00"
  }
}
```

**前端调用**:
```typescript
// API 调用
postApi.updatePost(id, postData)

// 实际使用
const { execute: executeUpdatePost } = useMutation(
  (payload: { id: number; content: string }) =>
    postApi.updatePost(payload.id, { content: payload.content }),
  {
    successMessage: '帖子更新成功',
    onSuccess: (data) => {
      // 更新本地数据
      post.value = data
    }
  }
)

// 调用方式
await executeUpdatePost({
  id: postId.value,
  content: '更新后的内容...'
})
```

**使用场景**:
- 帖子编辑功能
- 内容修改

---

## 4. 删除帖子

**接口路径**: `DELETE /api/v1/posts/{id}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 帖子ID，必须大于0 |

**权限验证**:
- 只有帖子创建者可以删除自己的帖子
- 非创建者删除会返回 403 错误

**删除方式**: 软删除（修改状态，不删除数据）

**返回类型**: `Void`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功"
}
```

**前端调用**:
```typescript
// API 调用
postApi.deletePost(id)

// 实际使用
const { execute: executeDeletePost } = useMutation(
  (id: number) => postApi.deletePost(id),
  {
    successMessage: '帖子删除成功',
    onSuccess: () => {
      // 刷新列表
      loadPosts()
    }
  }
)

// 调用方式
await executeDeletePost(postId.value)
```

**使用场景**:
- 帖子列表删除操作
- 个人中心内容管理

---

## 5. 获取帖子详情

**接口路径**: `GET /api/v1/posts/{id}`

**是否需要登录**: 否

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 帖子ID，必须大于0 |

**返回类型**: `PostSummaryDTO`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "content": "帖子内容...",
    "nodeId": 10,
    "creatorId": 83,
    "type": 2,
    "twice": 5,
    "helpful": 10,
    "commentCount": 15,
    "viewCount": 100,
    "state": 1,
    "score": 95.5,
    "createdAt": "2025-01-18 10:00:00",
    "updatedAt": "2025-01-18 15:30:00"
  }
}
```

**前端调用**:
```typescript
// API 调用
postApi.getPost(id)

// 实际使用
const { data: post } = useFetch<Post>({
  fetchFn: () => postApi.getPost(postId.value),
  immediate: true
})
```

**使用场景**:
- 获取单个帖子的基础信息
- 不需要关联数据的场景

---

## 6. 获取用户帖子列表

**接口路径**: `GET /api/v1/users/{userId}/posts`

**是否需要登录**: 否

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID，必须大于0 |

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| lastId | Long | 否 | - | 分页游标，上一页最后一条的ID |
| type | Integer | 否 | 2 | 帖子类型：0=普通帖子，1=内容（目录），2=文章 |

**注意事项**:
- 只返回**已发布**状态的帖子（state=1）
- 用于公开展示用户的发布内容
- 不显示待审核、已拒绝等状态的帖子

**返回类型**: `KeysetPageResponse<PostFullDTO>`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "items": [
      {
        "id": 1,
        "content": "帖子内容...",
        "nodeId": 10,
        "creatorId": 83,
        "type": 2,
        "twice": 5,
        "helpful": 10,
        "commentCount": 15,
        "viewCount": 100,
        "state": 1,
        "score": 95.5,
        "createdAt": "2025-01-18 10:00:00",
        "updatedAt": "2025-01-18 15:30:00",
        "creator": {
          "id": 83,
          "name": "user123",
          "avatar": "https://example.com/avatar.jpg"
        },
        "node": {
          "id": 10,
          "name": "节点名称",
          "course": {
            "id": 608,
            "name": "高三政治"
          }
        },
        "voteType": 0
      }
    ],
    "hasMore": true,
    "nextCursor": {
      "lastId": 1
    }
  }
}
```

**KeysetPageResponse 字段说明**:
- `items` (List): 帖子列表
- `hasMore` (Boolean): 是否有更多数据
- `nextCursor` (Object): 下一页游标信息（当 hasMore=false 时不出现）
  - `lastScore` (Double): 最后一条记录的分数（用户帖子列表不使用分数排序，不出现）
  - `lastId` (Long): 最后一条记录的ID

**前端调用**:
```typescript
// 获取用户文章
const { data: response } = useFetch<KeysetPageResponse<Post>>({
  fetchFn: () => postApi.getUserPosts(userId, lastId.value, 2),
  immediate: true
})
const articles = response.items
const hasMore = response.hasMore

// 获取用户目录
const { data: response } = useFetch<KeysetPageResponse<Post>>({
  fetchFn: () => postApi.getUserPosts(userId, lastId.value, 1),
  immediate: true
})
const catalogs = response.items
```

**使用场景**:
- `ArticlesTab.vue` - 用户主页文章列表
- `CatalogsTab.vue` - 用户主页目录列表
- 公开展示用户发布的内容

---

## 7. 获取当前用户所有状态的帖子

**接口路径**: `GET /api/v1/users/me/posts`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| lastId | Long | 否 | - | 分页游标，上一页最后一条的ID |
| type | Integer | 否 | 2 | 帖子类型：0=普通帖子，1=内容（目录），2=文章 |

**特点**:
- 返回**所有状态**的帖子（待审核、已发布、已拒绝、已屏蔽）
- 仅限查询当前登录用户自己的帖子
- 用于个人中心的内容管理

**返回类型**: `KeysetPageResponse<PostFullDTO>`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "items": [
      {
        "id": 1,
        "content": "帖子内容...",
        "nodeId": 10,
        "creatorId": 83,
        "type": 2,
        "twice": 5,
        "helpful": 10,
        "commentCount": 15,
        "viewCount": 100,
        "state": 0,
        "score": 95.5,
        "createdAt": "2025-01-18 10:00:00",
        "updatedAt": "2025-01-18 15:30:00",
        "creator": {
          "id": 83,
          "name": "user123",
          "avatar": "https://example.com/avatar.jpg"
        },
        "node": {
          "id": 10,
          "name": "节点名称",
          "course": {
            "id": 608,
            "name": "高三政治"
          }
        },
        "voteType": 0
      }
    ],
    "hasMore": true,
    "nextCursor": {
      "lastId": 2
    }
  }
}
```

**KeysetPageResponse 字段说明**:
- `items` (List): 帖子列表
- `hasMore` (Boolean): 是否有更多数据
- `nextCursor` (Object): 下一页游标信息（当 hasMore=false 时不出现）
  - `lastScore` (Double): 最后一条记录的分数（用户帖子列表不使用分数排序，不出现）
  - `lastId` (Long): 最后一条记录的ID

**前端调用**:
```typescript
// API 调用（需要自己实现）
apiClient.get('/v1/users/me/posts', {
  params: { lastId, type }
})

// 预期使用场景
const { data: myPosts } = useFetch({
  fetchFn: () => apiClient.get('/v1/users/me/posts', {
    params: { lastId: lastId.value, type: 2 }
  }),
  immediate: true
})
```

**使用场景**:
- 个人中心内容管理页面
- 需要查看所有状态帖子的场景
- 区分待审核、已拒绝等状态

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数验证失败 |
| 401 | 未登录（需要登录的接口） |
| 403 | 无权限（非帖子创建者尝试修改/删除） |
| 404 | 帖子不存在 |
| 429 | 请求频率超限（超过 30 次/分钟） |
| 500 | 服务器内部错误 |

---

## DTO 继承体系图

```
PostSummaryDTO (基础)
    ↓ extends
PostWithCreatorDTO (+ creator)
    ↓ extends                    ↓ extends
PostDetailDTO (+ node)      PostWithVoteDTO (+ voteType)
    ↓ extends
PostFullDTO (+ voteType)
```

**设计说明**:
- **PostSummaryDTO**: 最基础的帖子信息，无关联对象
- **PostWithCreatorDTO**: 添加创建者信息，用于需要显示作者的场景
- **PostDetailDTO**: 添加节点信息，用于需要显示完整上下文的场景
- **PostWithVoteDTO**: 添加投票状态，用于需要显示用户互动的场景
- **PostFullDTO**: 完整信息，包含节点、创建者、投票状态

---

## 测试用例建议

### 1. 批量获取帖子
- ✅ 未登录返回 401
- ✅ 按 ID 列表查询
- ✅ 按节点ID查询
- ✅ 分页查询（lastScore + lastId）
- ✅ 返回包含投票状态

### 2. 创建帖子
- ✅ 未登录返回 401
- ✅ 必填字段缺失返回 400
- ✅ 成功创建帖子
- ✅ 内容长度超限返回 400

### 3. 更新帖子
- ✅ 未登录返回 401
- ✅ 帖子不存在返回 404
- ✅ 非创建者更新返回 403
- ✅ 创建者成功更新
- ✅ 返回更新后的 PostSummaryDTO

### 4. 删除帖子
- ✅ 未登录返回 401
- ✅ 帖子不存在返回 404
- ✅ 非创建者删除返回 403
- ✅ 创建者成功删除（软删除）

### 5. 获取帖子详情
- ✅ 帖子存在返回 PostSummaryDTO
- ✅ 帖子不存在返回 404
- ✅ 不需要登录

### 6. 获取节点帖子列表
- ✅ 节点存在返回帖子列表
- ✅ 节点不存在返回空列表
- ✅ 不需要登录

### 7. 获取用户帖子列表
- ✅ 只返回已发布状态的帖子
- ✅ 按类型筛选（文章/目录）
- ✅ 分页功能（lastId）
- ✅ 返回 PostFullDTO（含节点和投票状态）

### 8. 获取当前用户所有状态帖子
- ✅ 未登录返回 401
- ✅ 返回所有状态的帖子
- ✅ 分页功能正常
- ✅ 返回 KeysetPageResponse 格式

---

## 前端 API 实现说明

**注意**：获取用户帖子的接口在 `web/src/api/modules/user.ts` 中实现，而不是 `post.ts`。

**已实现的接口**：

```typescript
// web/src/api/modules/user.ts

/**
 * 获取用户帖子（公开，仅已发布）
 */
getUserPosts(userId: number, lastId?: number, type = 2): Promise<ApiResponse<Post[]>> {
  return apiClient.get(`/v1/users/${userId}/posts`, {
    params: { lastId, type }
  })
}

/**
 * 获取当前用户所有状态的帖子（用于个人中心）
 */
getCurrentUserAllPosts(
  lastId?: number,
  type?: number
): Promise<ApiResponse<{
  items: Post[]
  hasMore: boolean
  nextCursor?: {
    lastScore?: number
    lastId?: number
  }
}>> {
  const params: { lastId?: number; type?: number } = {}
  if (lastId !== undefined && lastId !== null) {
    params.lastId = lastId
  }
  if (type !== undefined && type !== null) {
    params.type = type
  }
  return apiClient.get('/v1/users/me/posts', { params })
}
```

**使用场景**：
- `ArticlesTab.vue` (ProfilePage) - 调用 `userApi.getUserPosts(userId, lastId, 2)`
- `CatalogsTab.vue` (ProfilePage) - 调用 `userApi.getUserPosts(userId, lastId, 1)`
- 个人中心内容管理 - 调用 `userApi.getCurrentUserAllPosts(lastId, type)`

---

## 注意事项

1. **权限控制**：
   - 更新和删除操作只能由创建者执行
   - 批量获取帖子需要登录（包含投票状态）

2. **分页策略**：
   - 使用 Keyset 分页（lastScore + lastId），性能优于传统 offset
   - 适合大数据量场景

3. **状态过滤**：
   - 公开接口只返回已发布状态（state=1）
   - `/users/me/posts` 返回所有状态，用于个人内容管理

4. **DTO 选择**：
   - 根据场景选择合适的 DTO，避免过度查询
   - 列表场景优先使用 PostWithVoteDTO（轻量级）
   - 详情场景使用 PostFullDTO（完整信息）

5. **软删除**：
   - 删除操作是软删除，不会物理删除数据
   - 便于数据恢复和审计

---

*此文档用于指导前后端开发和接口对接，确保数据结构和业务逻辑的一致性*
