# 页面聚合接口文档 (PagesController)

## 基本信息

- **Controller**: `PagesController.java`
- **基础路径**: `/api/v1`
- **Rate Limit**: 80 requests/minute (per user)
- **服务类**: `PageService.java`

## 接口概述

PagesController 提供页面聚合数据接口，用于课程内容阅读页面。它将多个数据源（课程、节点、帖子、用户等）聚合到一个响应中，减少前端请求次数。

**接口列表**：
1. `GET /api/v1/pages/read` - 完整页面数据（用于 ContentReadPage）
2. `GET /api/v1/pages/node` - 节点帖子列表（优化版，用于 NodePostsPage）
3. `GET /api/v1/pages/post` - 帖子详情（优化版，用于 PostDetailPage）

---

## 1. 读取页面数据（完整版）

**接口路径**: `GET /api/v1/pages/read`

**是否需要登录**: 是 (`@SaCheckLogin`)

**功能说明**:
根据不同参数读取完整的页面聚合数据，用于课程内容阅读页面（ContentReadPage）。支持通过课程路径、节点ID、帖子ID或评论ID来定位内容。返回所有字段包括 TOC、目录节点信息、精选帖子、固定帖子和其他帖子。

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| courseId | Long | 否 | 课程ID，必须大于0 |
| path | String | 否 | 课程内容路径，格式：`/rootNodeId/childId/...` |
| nodeId | Long | 否 | 节点ID，必须大于0 |
| postId | Long | 否 | 帖子ID，必须大于0 |
| commentId | Long | 否 | 评论ID，必须大于0 |

**参数优先级**（从高到低）:
1. `commentId` - 通过评论定位到帖子和节点
2. `postId` - 通过帖子定位到节点
3. `nodeId` - 直接定位到节点
4. `courseId` + `path` - 通过路径定位到节点

**参数规则**:
- 必须至少提供一组参数（commentId、postId、nodeId 或 courseId）
- 如果都不提供，返回 400 错误
- `path` 参数只在使用 `courseId` 时有效

**返回类型**: `Map<String, Object>`

**返回数据结构**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "toc": [...],
    "node": {...},
    "parentCourse": {...},
    "course": {...},
    "subCourseList": [...],
    "chosenPosting": {...},
    "fixedPostings": [...],
    "otherPostings": [...],
    "lastId": 12345,
    "tocNodeInfos": {...},
    "path": "/10/20/30",
    "users": [...],
    "learning": true,
    "post": {...},
    "nodeCompleted": true,
    "courseProgress": 45
  }
}
```

**返回字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `toc` | Array | 课程目录树（JSON结构） |
| `node` | NodeWithProgressDTO | 当前节点信息（含完成状态） |
| `parentCourse` | CourseWithProgressDTO | 父课程信息（含订阅状态和进度） |
| `course` | CourseWithProgressDTO | 当前课程信息（含订阅状态和进度） |
| `subCourseList` | List<CourseSummaryDTO> | 子课程列表 |
| `chosenPosting` | PostWithVoteDTO | 选中的目录型帖子（TOC 中选中显示的帖子） |
| `fixedPostings` | List<PostWithVoteDTO> | 固定帖子列表（TOC 中固定的帖子） |
| `otherPostings` | List<PostWithVoteDTO> | 其他普通帖子列表（含投票状态） |
| `lastId` | Long | 帖子分页游标ID |
| `tocNodeInfos` | Map | 目录节点信息映射 |
| `path` | String | 当前内容路径 |
| `users` | List<UserBriefDTO> | 涉及的用户信息列表 |
| `learning` | Boolean | 是否正在学习该课程 |
| `post` | PostWithVoteDTO | 指定的帖子（当通过 postId 查询时） |
| `nodeCompleted` | Boolean | 当前节点是否已完成 |
| `courseProgress` | Integer | 课程学习进度百分比（0-100） |

**使用场景1：通过课程路径读取**
```http
GET /api/v1/pages/read?courseId=608&path=/10/20/30
```

**使用场景2：通过节点ID读取**
```http
GET /api/v1/pages/read?nodeId=123
```

**使用场景3：通过帖子ID读取**
```http
GET /api/v1/pages/read?postId=456
```

**使用场景4：通过评论ID读取**
```http
GET /api/v1/pages/read?commentId=789
```

**特殊处理 - 评论跳转**:
当通过 `commentId` 查询且评论是回复评论（非顶级评论）时，返回特殊结构：
```json
{
  "code": 200,
  "data": {
    "commentId": 100,      // 顶级评论ID
    "subCommentId": 789    // 子评论ID
  }
}
```

**错误响应**:

| 错误码 | 说明 |
|--------|------|
| 400 | 参数验证失败（未提供有效参数或参数无效） |
| 401 | 未登录 |
| 404 | 课程/节点/帖子/评论不存在 |
| 403 | 课程未发布或节点未发布 |
| 429 | 请求频率超限（超过 80 次/分钟） |
| 500 | 服务器内部错误 |

**业务规则**:
1. 只能访问**已发布**状态的课程（state=1）
2. 只能访问**已发布**状态的节点（state=1）
3. 自动记录内容浏览事件（用于统计）
4. 自动加载用户学习状态和进度
5. 帖子列表包含用户投票状态

**性能优化**:
- 批量查询用户信息（避免 N+1 问题）
- 批量查询帖子投票状态
- TOC 数据使用缓存
- 学习进度数据使用 Redis 缓存

---

## 前端调用示例

### 方式1：通过课程路径读取
```typescript
// 假设前端 API 定义在 pageApi.ts 中
const { data: pageData } = useFetch({
  fetchFn: () => apiClient.get('/v1/pages/read', {
    params: {
      courseId: courseId.value,
      path: currentPath.value
    }
  }),
  immediate: true
})
```

### 方式2：通过节点ID读取
```typescript
const { data: pageData } = useFetch({
  fetchFn: () => apiClient.get('/v1/pages/read', {
    params: {
      nodeId: nodeId.value
    }
  }),
  immediate: true
})
```

### 方式3：通过帖子ID读取
```typescript
const { data: pageData } = useFetch({
  fetchFn: () => apiClient.get('/v1/pages/read', {
    params: {
      postId: postId.value
    }
  }),
  immediate: true
})
```

### 方式4：通过评论ID读取
```typescript
const { data: pageData } = useFetch({
  fetchFn: () => apiClient.get('/v1/pages/read', {
    params: {
      commentId: commentId.value
    }
  }),
  immediate: true
})
```

---

## 返回数据详细说明

### toc（目录树）
课程内容目录结构，JSON 数组格式。

**示例**:
```json
[
  {
    "id": 10,
    "name": "第一章",
    "children": [
      {
        "id": 11,
        "name": "1.1 简介"
      }
    ]
  }
]
```

### node（当前节点）
类型：`NodeWithProgressDTO`

```json
{
  "id": 20,
  "name": "节点名称",
  "description": "节点描述",
  "isCompleted": true
}
```

### parentCourse（父课程）
类型：`CourseWithProgressDTO`

```json
{
  "id": 608,
  "name": "高三政治",
  "description": "课程描述",
  "mainCategory": 10,
  "subCategory": 9,
  "subscribed": true,
  "progress": 45
}
```

### course（当前课程）
类型：`CourseWithProgressDTO`

与 `parentCourse` 结构相同。当课程无父课程时，`course` 和 `parentCourse` 是同一个对象。

### subCourseList（子课程列表）
类型：`List<CourseSummaryDTO>`

```json
[
  {
    "id": 1,
    "name": "子课程1",
    "description": "子课程描述",
    "mainCategory": 1,
    "subCategory": 2
  }
]
```

### chosenPosting（选中的目录型帖子）
类型：`PostWithVoteDTO`

当前节点在目录树（TOC）中选中显示的帖子。这是一个目录型帖子（type=目录型），作为该节点的主要内容展示在目录中。

```json
{
  "id": 1,
  "content": "目录型帖子内容...",
  "nodeId": 20,
  "creatorId": 1,
  "type": 1,
  "viewCount": 1000,
  "commentCount": 50,
  "creator": {
    "id": 1,
    "name": "作者",
    "avatar": "..."
  },
  "voteType": 1
}
```

### fixedPostings（固定帖子列表）
类型：`List<PostWithVoteDTO>`

当前节点在目录树（TOC）中固定显示的帖子列表。这些是固定在目录中的重要帖子。

### otherPostings（其他帖子列表）
类型：`List<PostWithVoteDTO>`

当前节点的其他普通帖子列表。

### lastId（分页游标）
类型：`Long`

用于加载更多帖子的游标ID。

### tocNodeInfos（目录节点信息）
类型：`Map<Long, NodeWithProgressDTO>`

目录中所有节点的完成状态映射，用于在目录树中显示进度。

**示例**:
```json
{
  "10": {
    "id": 10,
    "name": "第一章",
    "isCompleted": true
  },
  "11": {
    "id": 11,
    "name": "1.1 简介",
    "isCompleted": false
  }
}
```

### users（用户列表）
类型：`List<UserBriefDTO>`

页面中涉及的所有用户信息（帖子作者等），用于前端显示。

```json
[
  {
    "id": 83,
    "name": "user123",
    "avatar": "https://example.com/avatar.jpg"
  }
]
```

### learning（学习状态）
类型：`Boolean`

当前用户是否正在学习该课程。

### post（指定帖子）
类型：`PostWithVoteDTO`

当通过 `postId` 参数查询时返回，表示用户想要查看的特定帖子。

### nodeCompleted（节点完成状态）
类型：`Boolean`

当前节点是否已完成（用户学习进度）。

### courseProgress（课程进度）
类型：`Integer`

课程学习进度百分比，范围 0-100。

---

## 业务流程说明

### 1. 通过评论ID读取
```
commentId → 查找评论 → 判断是否是回复评论
  ├─ 是回复评论 → 返回 {commentId, subCommentId}（前端用于定位）
  └─ 是顶级评论 → 获取帖子 → 获取节点 → 返回完整页面数据
```

### 2. 通过帖子ID读取
```
postId → 查找帖子 → 发布浏览事件 → 获取节点 → 返回完整页面数据
```

### 3. 通过节点ID读取
```
nodeId → 查找节点 → 验证节点已发布 → 返回完整页面数据
```

### 4. 通过课程路径读取
```
courseId + path → 验证路径格式 → 解析TOC → 根据路径定位节点 → 返回完整页面数据
```

---

## 数据聚合说明

该接口一次性返回课程阅读页面所需的所有数据：

1. **课程信息**：父课程、当前课程、子课程列表
2. **内容导航**：TOC 目录树、当前路径、节点完成状态
3. **帖子内容**：选中的目录型帖子、固定帖子、其他普通帖子
4. **用户数据**：作者信息、投票状态、学习进度
5. **交互状态**：是否学习、是否完成、课程进度

**优势**:
- ✅ 减少前端请求次数（1 次请求获取所有数据）
- ✅ 后端批量查询优化（避免 N+1 问题）
- ✅ 数据一致性好（同一时刻的快照）

---

## 错误码说明

| 错误码 | 场景 | 说明 |
|--------|------|------|
| 200 | 成功 | 成功获取页面数据 |
| 400 | 参数验证失败 | 未提供有效参数或参数格式错误 |
| 401 | 未登录 | 需要登录才能访问 |
| 403 | 权限不足 | 课程或节点未发布 |
| 404 | 资源不存在 | 课程/节点/帖子/评论不存在 |
| 429 | 请求频率超限 | 超过 80 次/分钟 |
| 500 | 服务器错误 | 内部处理异常 |

---

## 使用场景

### 场景1：课程内容阅读页
```
用户访问：/course/608/read?path=/10/20/30
前端调用：GET /api/v1/pages/read?courseId=608&path=/10/20/30
返回：完整页面数据（TOC、节点、帖子、进度等）
```

### 场景2：直接访问节点
```
用户访问：/node/123
前端调用：GET /api/v1/pages/read?nodeId=123
返回：完整页面数据
```

### 场景3：查看特定帖子
```
用户点击帖子链接
前端调用：GET /api/v1/pages/read?postId=456
返回：完整页面数据 + post 字段（高亮显示该帖子）
```

### 场景4：评论跳转
```
用户点击评论通知
前端调用：GET /api/v1/pages/read?commentId=789
返回：
  - 如果是顶级评论：完整页面数据
  - 如果是回复评论：{commentId, subCommentId}（用于定位）
```

---

## 性能优化说明

1. **批量查询**：
   - 批量查询用户信息
   - 批量查询帖子投票状态
   - 批量查询节点完成状态

2. **缓存策略**：
   - TOC 数据使用缓存
   - 学习进度使用 Redis 缓存
   - 减少数据库查询

3. **按需加载**：
   - 只加载当前页面需要的数据
   - 帖子列表支持分页（lastId 游标）

---

## 注意事项

1. **路径格式**：
   - 必须以 `/` 开头
   - 格式：`/rootNodeId/childId/grandchildId/...`
   - 如果路径无效，会使用默认路径（根节点）

2. **状态验证**：
   - 课程必须是已发布状态（state=1）
   - 节点必须是已发布状态（state=1）
   - 非已发布内容会返回 403 错误

3. **事件记录**：
   - 通过 `postId` 查询时会发布 `ContentViewedEvent`
   - 用于统计浏览量和用户行为分析

4. **个性化数据**（需要登录）：
   - 学习进度（learning、nodeCompleted、courseProgress）
   - 投票状态（voteType）
   - 订阅状态（subscribed）

---

## 测试用例建议

### 1. 基础功能测试
- ✅ 通过 courseId + path 读取成功
- ✅ 通过 nodeId 读取成功
- ✅ 通过 postId 读取成功
- ✅ 通过 commentId 读取成功

### 2. 参数验证测试
- ✅ 未提供任何参数返回 400
- ✅ ID 参数为 0 或负数返回 400
- ✅ 无效的 path 格式处理

### 3. 权限和状态测试
- ✅ 未登录返回 401
- ✅ 课程未发布返回 403
- ✅ 节点未发布返回 403

### 4. 资源不存在测试
- ✅ 课程不存在返回 404
- ✅ 节点不存在返回 404
- ✅ 帖子不存在返回 404
- ✅ 评论不存在返回 404

### 5. 数据完整性测试
- ✅ 返回数据包含所有必需字段
- ✅ TOC 数据格式正确
- ✅ 学习进度数据正确
- ✅ 投票状态正确

### 6. 特殊场景测试
- ✅ 回复评论的跳转逻辑
- ✅ 无子课程时 subCourseList 为空
- ✅ 无帖子时返回空列表
- ✅ 未学习课程时个性化字段为默认值

---

*此文档用于指导前后端开发和接口对接，确保页面聚合数据的完整性和一致性*

---

## 2. 读取节点帖子列表（优化版）

**接口路径**: `GET /api/v1/pages/node`

**是否需要登录**: 是 (`@SaCheckLogin`)

**功能说明**:
专为 NodePostsPage 优化的接口，仅返回节点帖子列表页需要的数据。不返回 TOC、目录节点信息、精选帖子、固定帖子，显著减少数据传输量和数据库查询。

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| nodeId | Long | 是 | 节点ID，必须大于0 |

**返回类型**: `Map<String, Object>`

**返回数据结构**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "node": {...},
    "parentCourse": {...},
    "course": {...},
    "subCourseList": [...],
    "otherPostings": [...],
    "users": [...],
    "learning": true
  }
}
```

**返回字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `node` | NodeWithProgressDTO | 当前节点信息（含完成状态） |
| `parentCourse` | CourseWithProgressDTO | 父课程信息（含订阅状态和进度） |
| `course` | CourseWithProgressDTO | 当前课程信息（含订阅状态和进度） |
| `subCourseList` | List<CourseSummaryDTO> | 子课程列表 |
| `otherPostings` | List<PostWithVoteDTO> | 帖子列表（含投票状态） |
| `users` | List<UserBriefDTO> | 涉及的用户信息列表 |
| `learning` | Boolean | 是否正在学习该课程 |

**使用示例**:
```http
GET /api/v1/pages/node?nodeId=123
```

**性能优势**:
- ✅ 不查询 TOC 数据（节省数据库查询和 JSON 解析）
- ✅ 不查询 fixedPostings 和 chosenPosting（减少数据库查询）
- ✅ 减少约 40-60% 的数据传输量

**错误响应**:

| 错误码 | 说明 |
|--------|------|
| 400 | 参数验证失败（nodeId 为空或无效） |
| 401 | 未登录 |
| 404 | 节点不存在 |
| 403 | 节点未发布 |
| 429 | 请求频率超限（超过 80 次/分钟） |
| 500 | 服务器内部错误 |

**前端调用示例**:
```typescript
// 使用优化后的 API
const { data } = useFetch({
  fetchFn: () => pageApi.readNodePosts(nodeId.value),
  immediate: true
})
```

---

## 3. 读取帖子详情（优化版）

**接口路径**: `GET /api/v1/pages/post`

**是否需要登录**: 是 (`@SaCheckLogin`)

**功能说明**:
专为 PostDetailPage 优化的接口，仅返回帖子详情页需要的数据。不返回 TOC、目录节点信息、帖子列表（fixedPostings、otherPostings），显著减少数据传输量和数据库查询。支持通过 postId 或 commentId 定位。

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| postId | Long | 二选一 | 帖子ID，必须大于0 |
| commentId | Long | 二选一 | 评论ID，必须大于0 |

**参数规则**:
- 必须提供 `postId` 或 `commentId` 中的一个
- 如果同时提供，`commentId` 优先

**返回类型**: `Map<String, Object>`

**返回数据结构**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "node": {...},
    "parentCourse": {...},
    "course": {...},
    "subCourseList": [...],
    "post": {...},
    "users": [...],
    "learning": true
  }
}
```

**特殊响应（回复评论重定向）**:
当通过 `commentId` 查询且评论是回复评论（非顶级评论）时，返回重定向信息：
```json
{
  "code": 200,
  "data": {
    "commentId": 100,      // 顶级评论ID
    "subCommentId": 789    // 子评论ID（原始查询的 commentId）
  }
}
```

**返回字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `node` | NodeWithProgressDTO | 当前节点信息（含完成状态） |
| `parentCourse` | CourseWithProgressDTO | 父课程信息（含订阅状态和进度） |
| `course` | CourseWithProgressDTO | 当前课程信息（含订阅状态和进度） |
| `subCourseList` | List<CourseSummaryDTO> | 子课程列表 |
| `post` | PostWithVoteDTO | 帖子详情（含投票状态） |
| `users` | List<UserBriefDTO> | 涉及的用户信息列表 |
| `learning` | Boolean | 是否正在学习该课程 |
| `commentId` | Long | 顶级评论ID（仅回复评论重定向时返回） |
| `subCommentId` | Long | 子评论ID（仅回复评论重定向时返回） |

**使用示例1：通过帖子ID读取**:
```http
GET /api/v1/pages/post?postId=456
```

**使用示例2：通过评论ID读取**:
```http
GET /api/v1/pages/post?commentId=789
```

**性能优势**:
- ✅ 不查询 TOC 数据（节省数据库查询和 JSON 解析）
- ✅ 不查询 fixedPostings、chosenPosting、otherPostings（减少数据库查询）
- ✅ 减少约 50-70% 的数据传输量

**错误响应**:

| 错误码 | 说明 |
|--------|------|
| 400 | 参数验证失败（未提供 postId 或 commentId，或参数无效） |
| 401 | 未登录 |
| 404 | 帖子或评论不存在 |
| 403 | 节点未发布 |
| 429 | 请求频率超限（超过 80 次/分钟） |
| 500 | 服务器内部错误 |

**业务规则**:
1. 通过 `postId` 查询时自动发布 `ContentViewedEvent`（用于统计）
2. 通过 `commentId` 查询时：
   - 如果是回复评论：返回重定向信息 `{commentId, subCommentId}`
   - 如果是顶级评论：查找关联的帖子并返回完整数据
   - 如果评论关联的是节点（不是帖子）：返回节点信息，但不返回 post 字段
3. 只能访问已发布状态的节点

**前端调用示例**:
```typescript
// 通过帖子ID读取
const { data } = useFetch({
  fetchFn: () => pageApi.readPostDetail({ postId: postId.value }),
  immediate: true
})

// 通过评论ID读取
const { data } = useFetch({
  fetchFn: () => pageApi.readPostDetail({ commentId: commentId.value }),
  immediate: true,
  onDataReady: () => {
    // 处理回复评论重定向
    if (data.value.commentId && data.value.subCommentId) {
      // 重定向到顶级评论
      router.push({
        query: {
          commentId: data.value.commentId,
          subCommentId: data.value.subCommentId
        }
      })
    }
  }
})
```

---

## 接口性能对比

| 场景 | 旧接口 (`/pages/read`) | 新接口 | 数据量减少 |
|------|------------------------|--------|-----------|
| 节点帖子列表页 | 返回 TOC + 所有帖子类型 | `/pages/node` 仅返回 otherPostings | ~40-60% |
| 帖子详情页 | 返回 TOC + 所有帖子列表 | `/pages/post` 仅返回单个帖子 | ~50-70% |
| 课程阅读页 | 返回完整数据 | 保持使用 `/pages/read` | 0% |

**优化说明**:
- NodePostsPage 不需要显示目录，无需查询 TOC
- PostDetailPage 不需要显示帖子列表，无需查询 otherPostings
- ContentReadPage 需要完整数据，继续使用原接口

---

## API 迁移指南

### 前端迁移步骤

**步骤1：更新 API 定义** (已完成)
```typescript
// page.ts
export const pageApi = {
  // 新增优化接口
  readNodePosts(nodeId: number): Promise<ApiResponse<ReadResponse>>,
  readPostDetail(params: { postId?: number; commentId?: number }): Promise<ApiResponse<ReadResponse>>,

  // 保留原接口（标记为 deprecated）
  readByNode(...),  // @deprecated
  readByPost(...),  // @deprecated
  readByComment(...) // @deprecated
}
```

**步骤2：更新 NodePostsPage** (已完成)
```typescript
// 旧代码
fetchFn: () => pageApi.readByNode(nodeId)

// 新代码
fetchFn: () => pageApi.readNodePosts(nodeId)
```

**步骤3：更新 PostDetailPage** (已完成)
```typescript
// 旧代码
if (route.query.postId) {
  return pageApi.readByPost(Number(route.query.postId))
} else if (route.query.commentId) {
  return pageApi.readByComment(Number(route.query.commentId))
}

// 新代码
const params: { postId?: number; commentId?: number } = {}
if (route.query.postId) {
  params.postId = Number(route.query.postId)
} else if (route.query.commentId) {
  params.commentId = Number(route.query.commentId)
}
return pageApi.readPostDetail(params)
```

**步骤4：处理回复评论重定向** (TODO)
```typescript
onDataReady: () => {
  if (data.value.commentId && data.value.subCommentId) {
    // 重定向逻辑
    router.push({
      query: {
        commentId: data.value.commentId,
        subCommentId: data.value.subCommentId
      }
    })
  }
}
```

### 后向兼容性

- ✅ 旧接口 `/pages/read` 保持不变，继续支持
- ✅ 新接口使用独立路径，不影响现有功能
- ✅ 可以逐步迁移，无需一次性全部更改
- ✅ 返回数据结构兼容，前端无需大量修改

---
