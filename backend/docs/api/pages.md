# 页面聚合接口文档 (PagesController)

## 基本信息

- **Controller**: `PagesController.java`
- **基础路径**: `/api/v1`
- **Rate Limit**: 80 requests/minute (per user)
- **前端 API**: `web/src/api/modules/course.ts` (readPage 方法)

## 返回数据结构说明

### PageData (页面聚合数据)
用途：课程内容阅读页面的完整聚合数据，包含目录、课程信息、节点信息、帖子列表等

```json
{
  "toc": [...],
  "node": {...},
  "parentCourse": {...},
  "course": {...},
  "subCourseList": [...],
  "chosenPosting": {...},
  "fixedPostings": [...],
  "otherPostings": [...],
  "lastId": 123,
  "tocNodeInfos": {...},
  "path": "1-10-20",
  "users": [...],
  "learning": true,
  "post": {...}
}
```

**字段说明**：

**目录和导航字段**：
- `toc` (Array): 课程目录树（TOC），嵌套的JSON数组结构
  - 格式：`[{"id": 1, "children": [...], "^": [1,2,3], "+": 5}]`
  - `id`: 节点ID
  - `children`: 子节点数组
  - `^`: 固定帖子ID列表（可选）
  - `+`: 精选帖子ID（可选）
- `tocNodeInfos` (Map<Long, NodeWithProgressDTO>): TOC中所有节点的详细信息
  - Key: 节点ID
  - Value: 节点摘要信息 + 完成状态
- `path` (String): 当前路径，格式：`1-10-20`（根节点-章节-小节）

**课程信息字段**：
- `course` (CourseWithProgressDTO): 当前课程信息
  - 包含基础信息、订阅状态、学习进度
- `parentCourse` (CourseWithProgressDTO): 父课程信息
  - 如果当前是子课程，返回父课程信息
  - 如果当前是主课程，返回自己
- `subCourseList` (List<CourseSummaryDTO>): 子课程列表
  - 当前课程的所有子课程（仅已发布状态）
- `learning` (Boolean): 当前用户是否正在学习该课程
  - true: 用户已开始学习
  - false: 用户未开始学习

**节点和内容字段**：
- `node` (NodeWithProgressDTO): 当前节点信息
  - 包含节点基础信息和完成状态
- `chosenPosting` (PostWithVoteDTO): 精选帖子
  - 在TOC中通过 `+` 标记的帖子
  - 显示在内容区域顶部
  - null: 无精选帖子
- `fixedPostings` (List<PostWithVoteDTO>): 固定帖子列表
  - 在TOC中通过 `^` 标记的帖子列表
  - 按固定顺序显示
  - 空数组: 无固定帖子
- `otherPostings` (List<PostWithVoteDTO>): 其他帖子列表
  - 当前节点下的其他帖子（非精选、非固定）
  - 按分数排序
  - 空数组: 无其他帖子
- `lastId` (Long): 最后一个帖子的ID
  - 用于分页加载更多帖子
  - -1: 无帖子

**特殊字段**：
- `post` (PostWithVoteDTO): 特定帖子（可选）
  - 仅当通过 postId 或 commentId 进入时返回
  - 用于高亮显示特定帖子
  - null: 无特定帖子
- `users` (List<UserBriefDTO>): 页面中所有用户的简要信息
  - 包含所有帖子作者的信息
  - 用于前端快速查找用户信息，避免重复请求

**使用场景**：
- `ContentReadPage.vue` - 课程内容阅读页面（主要使用场景）
- `PostDetailPage.vue` - 帖子详情页（通过 postId 跳转）
- `NodePostsPage.vue` - 节点帖子列表页（通过 nodeId 跳转）

**注意**：
- 所有个性化字段（learning, progress, subscribed, completed 等）需要登录
- 帖子列表包含用户的投票状态（voteType），需要登录
- TOC 中的节点完成状态需要登录

---

## 接口列表

## 1. 读取页面数据（通用接口）

**接口路径**: `GET /api/v1/pages/read`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| courseId | Long | 条件必填 | 课程ID，与path一起使用 |
| path | String | 条件必填 | 节点路径，格式：`1-10-20`（根节点-章节-小节） |
| nodeId | Long | 条件必填 | 节点ID，直接跳转到指定节点 |
| postId | Long | 条件必填 | 帖子ID，跳转到帖子所在节点并高亮帖子 |
| commentId | Long | 条件必填 | 评论ID，跳转到评论所在帖子/节点 |

**参数优先级**（从高到低）:
1. `commentId` - 通过评论定位内容
2. `postId` - 通过帖子定位内容
3. `nodeId` - 直接定位到节点
4. `courseId + path` - 通过课程和路径定位

**参数组合规则**:
1. **通过课程和路径定位**: 传 `courseId` 和 `path`
   - 示例：`/api/v1/pages/read?courseId=608&path=1-10-20`
   - 用于用户在TOC中点击节点导航

2. **通过节点ID定位**: 只传 `nodeId`
   - 示例：`/api/v1/pages/read?nodeId=123`
   - 用于从其他页面直接跳转到节点

3. **通过帖子ID定位**: 只传 `postId`
   - 示例：`/api/v1/pages/read?postId=456`
   - 用于从帖子列表、搜索结果等跳转到帖子详情
   - 返回数据中包含 `post` 字段，用于高亮显示该帖子

4. **通过评论ID定位**: 只传 `commentId`
   - 示例：`/api/v1/pages/read?commentId=789`
   - 用于从通知、评论列表跳转到评论所在位置
   - 如果评论是回复，返回：`{"commentId": 主评论ID, "subCommentId": 当前评论ID}`
   - 如果评论是主评论，跳转到评论所在帖子/节点

**返回类型**: `Map<String, Object>` (PageData)

**返回示例 1 - 通过课程和路径**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "toc": [
      {
        "id": 1,
        "children": [
          {
            "id": 10,
            "children": [
              {
                "id": 20,
                "+": 5,
                "^": [1, 2, 3]
              }
            ]
          }
        ]
      }
    ],
    "tocNodeInfos": {
      "1": {
        "id": 1,
        "name": "第一章",
        "completed": false
      },
      "10": {
        "id": 10,
        "name": "第一节",
        "completed": true
      },
      "20": {
        "id": 20,
        "name": "知识点1",
        "completed": false
      }
    },
    "node": {
      "id": 20,
      "name": "知识点1",
      "description": "这是第一个知识点",
      "courseId": 608,
      "completed": false
    },
    "parentCourse": {
      "id": 608,
      "name": "高三政治",
      "description": "高三政治相关的专业课程和实践内容",
      "mainCategory": 10,
      "subCategory": 9,
      "subscribed": true,
      "progress": 45
    },
    "course": {
      "id": 608,
      "name": "高三政治",
      "subscribed": true,
      "progress": 45
    },
    "subCourseList": [
      {
        "id": 609,
        "name": "经济学基础",
        "description": "经济学基础知识",
        "mainCategory": 10,
        "subCategory": 9
      }
    ],
    "chosenPosting": {
      "id": 5,
      "type": 1,
      "content": "这是精选帖子的内容...",
      "nodeId": 20,
      "creatorId": 83,
      "creator": {
        "id": 83,
        "username": "张三",
        "avatar": "https://example.com/avatar.jpg"
      },
      "voteType": 1
    },
    "fixedPostings": [
      {
        "id": 1,
        "type": 1,
        "content": "固定帖子1...",
        "creator": {...},
        "voteType": null
      },
      {
        "id": 2,
        "type": 1,
        "content": "固定帖子2...",
        "creator": {...}
      }
    ],
    "otherPostings": [
      {
        "id": 100,
        "type": 1,
        "content": "其他帖子内容...",
        "creator": {...},
        "voteType": 2
      }
    ],
    "lastId": 100,
    "path": "1-10-20",
    "users": [
      {
        "id": 83,
        "username": "张三",
        "avatar": "https://example.com/avatar.jpg"
      }
    ],
    "learning": true
  }
}
```

**返回示例 2 - 通过帖子ID（包含 post 字段）**:
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
    "lastId": 123,
    "tocNodeInfos": {...},
    "path": "1-10-20",
    "users": [...],
    "learning": true,
    "post": {
      "id": 456,
      "type": 1,
      "content": "这是要高亮显示的帖子...",
      "nodeId": 20,
      "creatorId": 83,
      "creator": {
        "id": 83,
        "username": "张三",
        "avatar": "https://example.com/avatar.jpg"
      },
      "voteType": 1
    }
  }
}
```

**返回示例 3 - 通过评论ID（如果评论是回复）**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "commentId": 100,
    "subCommentId": 789
  }
}
```

**前端调用**:
```typescript
// API 调用方式1: 通过课程和路径
courseApi.readPage({ courseId: 608, path: '1-10-20' })

// API 调用方式2: 通过节点ID
courseApi.readPage({ nodeId: 123 })

// API 调用方式3: 通过帖子ID
courseApi.readPage({ postId: 456 })

// API 调用方式4: 通过评论ID
courseApi.readPage({ commentId: 789 })

// 实际使用 (ContentReadPage.vue:185-220)
const loadPageData = async () => {
  try {
    let params: any = {}

    if (route.query.commentId) {
      params.commentId = Number(route.query.commentId)
    } else if (route.query.postId) {
      params.postId = Number(route.query.postId)
    } else if (route.query.nodeId) {
      params.nodeId = Number(route.query.nodeId)
    } else {
      params.courseId = courseId.value
      params.path = currentPath.value
    }

    const response = await courseApi.readPage(params)
    if (response.data) {
      pageData.value = response.data
      // 处理返回数据...
    }
  } catch (error) {
    console.error('加载页面数据失败', error)
  }
}
```

**使用场景**:
- `ContentReadPage.vue` - 课程内容阅读页面
  - 通过 TOC 导航（courseId + path）
  - 通过节点ID直接跳转（nodeId）
  - 通过帖子ID查看帖子（postId）
  - 通过评论ID查看评论（commentId）
- `PostDetailPage.vue` - 帖子详情页（使用 postId）
- `NodePostsPage.vue` - 节点帖子列表（使用 nodeId）

**注意事项**:
1. **参数验证**:
   - 所有ID参数必须大于0
   - 至少需要提供一组有效参数（commentId / postId / nodeId / courseId+path）
   - 如果所有参数都为空，返回 400 错误

2. **权限控制**:
   - 必须登录才能访问
   - 只能查看已发布状态的课程和节点
   - 如果课程/节点被删除或封禁，返回相应错误

3. **内容浏览事件**:
   - 当通过 postId 或 commentId 访问时，会发布 ContentViewedEvent
   - 用于统计帖子浏览量

4. **路径处理**:
   - 路径格式必须为 `数字-数字-数字` 格式（如：`1-10-20`）
   - 如果路径无效，系统会自动修复为默认路径（根节点）
   - 默认路径：`1-{rootNodeId}`

5. **帖子列表排序**:
   - chosenPosting: TOC中标记的精选帖子
   - fixedPostings: TOC中标记的固定帖子列表（保持标记顺序）
   - otherPostings: 节点下的其他帖子（按分数排序，最多返回配置数量）

6. **性能优化**:
   - users 字段包含所有帖子作者的信息，前端可以缓存避免重复请求
   - tocNodeInfos 使用 Map 结构，前端可以快速查找节点信息
   - 帖子列表限制数量，避免一次返回过多数据

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数验证失败（如：所有参数都为空，ID小于等于0） |
| 401 | 未登录 |
| 403 | 无权限 |
| 404 | 资源不存在（课程、节点、帖子、评论不存在） |
| 429 | 请求频率超限 |
| 500 | 服务器内部错误 |

**业务错误码**:
- `COURSE_IS_NOT_PUBLISHED` - 课程未发布
- `NODE_STATE_INVALID` - 节点状态无效（未发布）
- `JSON_PROCESSING_ERROR` - JSON解析失败

---

## 测试用例建议

### 1. 通过课程和路径读取
- ✅ 正常路径（如：`1-10-20`）
- ✅ 默认路径（如：`1-10`）
- ✅ 无效路径自动修复
- ✅ 课程不存在返回404
- ✅ 课程未发布返回业务错误

### 2. 通过节点ID读取
- ✅ 正常节点ID
- ✅ 节点不存在返回404
- ✅ 节点未发布返回业务错误
- ✅ 节点ID为0或负数返回400

### 3. 通过帖子ID读取
- ✅ 正常帖子ID
- ✅ 返回数据包含 post 字段
- ✅ 发布内容浏览事件
- ✅ 帖子不存在返回404
- ✅ 帖子ID为0或负数返回400

### 4. 通过评论ID读取
- ✅ 主评论（跳转到帖子/节点）
- ✅ 回复评论（返回 commentId 和 subCommentId）
- ✅ 评论对象为帖子
- ✅ 评论对象为节点
- ✅ 评论不存在返回404

### 5. 权限和状态
- ✅ 未登录返回401
- ✅ 课程未发布不可访问
- ✅ 节点未发布不可访问
- ✅ 查看已删除课程返回404

### 6. 数据完整性
- ✅ TOC结构完整
- ✅ tocNodeInfos 包含所有节点信息
- ✅ 节点完成状态正确
- ✅ 课程订阅状态正确
- ✅ 课程学习进度正确
- ✅ 帖子投票状态正确（登录后）
- ✅ 用户信息完整

### 7. 特殊场景
- ✅ 无精选帖子（chosenPosting = null）
- ✅ 无固定帖子（fixedPostings = []）
- ✅ 无其他帖子（otherPostings = []）
- ✅ 无子课程（subCourseList = []）
- ✅ 父课程为自己（当前是主课程）
- ✅ 未开始学习（learning = false）

---

## 数据流程说明

### 读取页面数据的处理流程

```
请求参数
  ↓
参数优先级判断
  ↓
[commentId] → 查询评论 → 判断是否回复
  ├─ 是回复 → 返回 {commentId, subCommentId}
  └─ 不是回复 → 通过评论找到帖子/节点

[postId] → 查询帖子 → 发布浏览事件 → 通过帖子找到节点

[nodeId] → 直接查询节点

[courseId + path] → 通过课程和路径找到节点
  ↓
验证课程和节点状态（必须是已发布状态）
  ↓
并行查询多个数据源：
  ├─ 获取TOC（课程目录树）
  ├─ 获取节点完成状态
  ├─ 获取精选帖子（TOC中标记的+）
  ├─ 获取固定帖子列表（TOC中标记的^）
  ├─ 获取其他帖子（节点下的其他帖子）
  ├─ 获取用户信息（所有帖子作者）
  ├─ 获取课程信息（包含订阅状态和学习进度）
  ├─ 获取父课程信息
  ├─ 获取子课程列表
  └─ 获取用户学习状态
  ↓
聚合所有数据
  ↓
返回完整的页面数据
```

### TOC 结构说明

TOC（Table of Contents）是课程的目录树结构，使用嵌套的 JSON 数组表示：

```json
[
  {
    "id": 1,              // 节点ID（根节点）
    "children": [         // 子节点列表
      {
        "id": 10,         // 节点ID（章节）
        "children": [
          {
            "id": 20,     // 节点ID（小节）
            "+": 5,       // 精选帖子ID（可选）
            "^": [1,2,3]  // 固定帖子ID列表（可选）
          }
        ]
      }
    ]
  }
]
```

**字段含义**:
- `id`: 节点ID
- `children`: 子节点数组（可选）
- `+`: 精选帖子ID（可选，在内容区域顶部显示）
- `^`: 固定帖子ID数组（可选，按顺序显示在精选帖子之后）

### 路径格式说明

路径是从根节点到当前节点的ID序列，用 `-` 分隔：

- `1-10`: 根节点1 → 章节10
- `1-10-20`: 根节点1 → 章节10 → 小节20
- `1-10-20-30`: 根节点1 → 章节10 → 小节20 → 知识点30

**路径的作用**:
1. 定位当前节点在TOC中的位置
2. 在TOC中高亮显示当前节点及其祖先节点
3. 用于面包屑导航

---

## 前端集成指南

### 1. 使用 useFetch 加载页面数据

```typescript
import { useFetch } from '@/composables/useFetch'
import { courseApi } from '@/api/modules/course'

const { data: pageData, loading, error, execute } = useFetch({
  fetchFn: () => courseApi.readPage({
    courseId: courseId.value,
    path: currentPath.value
  }),
  immediate: true
})
```

### 2. 处理不同参数类型

```typescript
const loadPageData = async () => {
  let params: any = {}

  // 按优先级构建参数
  if (route.query.commentId) {
    params.commentId = Number(route.query.commentId)
  } else if (route.query.postId) {
    params.postId = Number(route.query.postId)
  } else if (route.query.nodeId) {
    params.nodeId = Number(route.query.nodeId)
  } else {
    params.courseId = courseId.value
    params.path = currentPath.value
  }

  await execute()
}
```

### 3. 渲染TOC

```vue
<template>
  <v-list>
    <TocNode
      v-for="node in pageData.toc"
      :key="node.id"
      :node="node"
      :nodeInfos="pageData.tocNodeInfos"
      :currentPath="pageData.path"
    />
  </v-list>
</template>
```

### 4. 显示帖子列表

```vue
<template>
  <!-- 精选帖子 -->
  <PostCard
    v-if="pageData.chosenPosting"
    :post="pageData.chosenPosting"
    badge="精选"
  />

  <!-- 固定帖子 -->
  <PostCard
    v-for="post in pageData.fixedPostings"
    :key="post.id"
    :post="post"
    badge="置顶"
  />

  <!-- 其他帖子 -->
  <PostCard
    v-for="post in pageData.otherPostings"
    :key="post.id"
    :post="post"
  />
</template>
```

### 5. 使用用户信息缓存

```typescript
// 构建用户信息Map，避免重复查找
const userMap = computed(() => {
  const map = new Map()
  pageData.value?.users?.forEach(user => {
    map.set(user.id, user)
  })
  return map
})

// 快速获取用户信息
const getUser = (userId: number) => {
  return userMap.value.get(userId)
}
```

---

## 性能优化建议

### 1. 后端优化
- ✅ 使用批量查询减少数据库往返次数
- ✅ 返回所有用户信息，避免前端重复请求
- ✅ 使用 Map 结构提供快速节点查找
- ✅ 限制帖子列表数量，避免返回过多数据

### 2. 前端优化
- ✅ 缓存用户信息，避免重复查找
- ✅ 使用虚拟滚动加载长列表
- ✅ 路径变化时只重新加载必要数据
- ✅ 使用 computed 缓存计算结果

### 3. 缓存策略
- ✅ TOC 数据可以缓存（节点完成状态除外）
- ✅ 课程信息可以短期缓存
- ✅ 帖子列表需要实时获取（包含投票状态）

---

## 相关文档

- [课程管理接口文档](./course.md)
- [节点管理接口文档](./node.md)
- [帖子管理接口文档](./post.md)
- [评论管理接口文档](./comment.md)
