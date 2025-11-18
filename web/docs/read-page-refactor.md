# Read 页面重构 - 支持多种展示模式

## 概述

重构 `/read` 页面，使其支持三种展示模式：
1. **完整模式** - 显示左侧目录树 + 中间内容 + 右侧卡片（原有功能）
2. **节点模式** - 只显示节点帖子列表（无左侧目录）
3. **帖子模式** - 只显示单个帖子详情（无左侧目录）

## URL 路由设计

### 1. 完整模式（现有）
```
/read?courseId=123&path=1.2.3
```
- 显示完整的课程学习界面
- 包含左侧目录树、帖子列表、记忆卡片

### 2. 节点模式（新增）
```
/read?nodeId=456
```
- 只显示指定节点下的帖子列表
- 无左侧目录树
- 顶部显示：课程名 > 节点名（面包屑导航）

### 3. 帖子模式（新增）
```
/read?postId=789
```
- 只显示单个帖子的详细内容
- 无左侧目录树
- 包含评论区和记忆卡片
- 顶部显示：课程名 > 节点名（面包屑导航）

## 触发场景

### 节点模式
- 从消息通知点击节点评论 → `/read?nodeId=xxx`
- 从用户 Profile 查看用户在某节点的贡献 → `/read?nodeId=xxx`

### 帖子模式
- 从消息通知点击帖子评论 → `/read?postId=xxx`
- 从用户 Profile 查看用户的帖子 → `/read?postId=xxx`
- 从节点帖子列表点击某个帖子 → `/read?postId=xxx`

## 数据加载逻辑

### 完整模式（现有）
```typescript
// 加载课程完整数据（包含目录树、节点信息等）
const data = await courseApi.getCourseDetail(courseId, path)
```

### 节点模式（新增）
```typescript
// 加载节点基本信息 + 帖子列表
const nodeData = await nodeApi.getNodeDetail(nodeId)
// nodeData 包含：
// - node: 节点信息（id, name, courseId, courseName）
// - posts: 帖子列表
// - course: 课程基本信息（用于面包屑）
```

### 帖子模式（新增）
```typescript
// 加载帖子详情 + 相关数据
const postData = await postApi.getPostDetail(postId)
// postData 包含：
// - post: 帖子详情
// - node: 所属节点信息
// - course: 所属课程信息
// - comments: 评论列表
// - decks: 记忆卡片组
```

## 页面布局结构

### 完整模式
```
┌─────────────────────────────────────────────────┐
│          CourseHeader (完整课程头部)              │
├──────────┬──────────────────────┬────────────────┤
│          │                      │                │
│  左侧     │    中间内容区         │   右侧卡片区    │
│  目录树   │  (PostingList)      │  (MemoryCard)  │
│          │                      │                │
│ 240px    │     800px           │    320px       │
└──────────┴──────────────────────┴────────────────┘
```

### 节点模式 & 帖子模式
```
┌─────────────────────────────────────────────────┐
│      SimpleCourseHeader (简化面包屑)            │
│      课程名 > 节点名                             │
├──────────────────────────────┬──────────────────┤
│                              │                  │
│        中间内容区             │   右侧卡片区      │
│     (PostingList/Post)       │  (MemoryCard)    │
│                              │                  │
│          800px              │    320px         │
└──────────────────────────────┴──────────────────┘
```

## 组件设计

### 新增组件

#### SimpleCourseHeader.vue
简化版课程头部，用于节点模式和帖子模式

**Props:**
```typescript
interface Props {
  courseId: number
  courseName: string
  nodeId: number
  nodeName: string
}
```

**UI 设计:**
```vue
<div class="simple-course-header">
  <div class="breadcrumb">
    <a @click="gotoCourse">{{ courseName }}</a>
    <span class="separator">></span>
    <a @click="gotoNode">{{ nodeName }}</a>
  </div>
</div>
```

**样式要求:**
- 高度：60px（比完整版 CourseHeader 更矮）
- 背景：白色，底部淡边框
- 字体：14px，链接蓝色可点击

### 修改组件

#### ContentReadPage.vue

**新增计算属性:**
```typescript
const pageMode = computed(() => {
  if (route.query.nodeId) return 'node'
  if (route.query.postId) return 'post'
  return 'full'
})

const showSidebar = computed(() => pageMode.value === 'full')
```

**条件渲染:**
```vue
<!-- 左侧目录树 - 只在完整模式显示 -->
<div v-if="showSidebar" class="toc-sidebar">
  <!-- 现有目录树代码 -->
</div>

<!-- 顶部头部 - 根据模式切换 -->
<SimpleCourseHeader
  v-if="pageMode !== 'full'"
  :course-id="courseId"
  :course-name="courseName"
  :node-id="nodeId"
  :node-name="nodeName"
/>
<CourseHeader
  v-else
  <!-- 现有 props -->
/>
```

## API 接口设计

### 节点详情接口（后端需新增）
```
GET /api/v1/nodes/{nodeId}/detail

Response:
{
  "code": 200,
  "data": {
    "node": {
      "id": 456,
      "name": "Java 基础语法",
      "courseId": 123,
      "courseName": "Java 编程入门"
    },
    "posts": [
      {
        "id": 789,
        "title": "变量与数据类型详解",
        "author": {...},
        "upvotes": 42,
        "createdAt": "2025-01-15"
      }
    ]
  }
}
```

### 帖子详情接口（后端需新增或扩展）
```
GET /api/v1/posts/{postId}/detail

Response:
{
  "code": 200,
  "data": {
    "post": {
      "id": 789,
      "title": "变量与数据类型详解",
      "content": "...",
      "author": {...}
    },
    "node": {
      "id": 456,
      "name": "Java 基础语法",
      "courseId": 123,
      "courseName": "Java 编程入门"
    },
    "decks": [...],  // 记忆卡片组
  }
}
```

## 样式调整

### 布局宽度计算
```scss
// 完整模式
.read-content {
  display: flex;
  .toc-sidebar { width: 240px; }
  .center-content { width: 800px; }
  .memory-sidebar { width: 320px; }
}

// 节点/帖子模式
.read-content-simple {
  display: flex;
  justify-content: center;
  .center-content { width: 800px; }
  .memory-sidebar { width: 320px; }
}
```

### 响应式设计
```scss
// 小屏幕（< 1280px）
@media (max-width: 1280px) {
  .memory-sidebar { display: none; }
  .center-content { width: 100%; max-width: 800px; }
}

// 移动端（< 768px）
@media (max-width: 768px) {
  .center-content { width: 100%; padding: 16px; }
}
```

## 前端路由守卫

```typescript
// router/index.ts
{
  path: '/read',
  component: ContentReadPage,
  beforeEnter: (to, from, next) => {
    const { courseId, path, nodeId, postId } = to.query

    // 验证参数合法性
    if (nodeId && postId) {
      // 不能同时指定 nodeId 和 postId
      console.error('Invalid params: nodeId and postId cannot coexist')
      next('/404')
      return
    }

    if (!courseId && !nodeId && !postId) {
      // 至少需要一个参数
      console.error('Missing required params')
      next('/404')
      return
    }

    next()
  }
}
```

## 更新消息点击跳转

### NotificationMenu.vue
```typescript
const handleMessageClick = (message: Message) => {
  const data = JSON.parse(message.content)
  const type = message.type

  let url = ''

  // 评论相关 - 跳转到帖子详情
  if ([
    MessageType.POST_COMMENT,
    MessageType.REPLY_POSTING_COMMENT,
  ].includes(type)) {
    const postId = data.postId
    if (postId) {
      url = `/read?postId=${postId}`
    }
  }

  // 节点评论 - 跳转到节点帖子列表
  if (type === MessageType.NODE_COMMENT) {
    const nodeId = data.nodeId
    if (nodeId) {
      url = `/read?nodeId=${nodeId}`
    }
  }

  if (url) {
    router.push(url)
  }
}
```

## 实现优先级

### Phase 1: 核心功能
1. ✅ 创建 SimpleCourseHeader 组件
2. ✅ 修改 ContentReadPage 支持条件渲染
3. ✅ 添加节点模式数据加载逻辑
4. ✅ 添加帖子模式数据加载逻辑

### Phase 2: 集成优化
5. ✅ 更新路由配置和守卫
6. ✅ 更新 NotificationMenu 跳转逻辑
7. ✅ 调整样式和响应式布局

### Phase 3: 测试完善
8. ✅ 测试三种模式切换
9. ✅ 测试各种跳转场景
10. ✅ 移动端适配测试

## 兼容性考虑

### 向后兼容
- 原有的 `/read?courseId=xxx&path=xxx` 继续正常工作
- 不影响现有学习进度记录逻辑
- 现有书签和分享链接依然有效

### 数据一致性
- 三种模式共享相同的评论数据
- 记忆卡片状态保持同步
- 用户学习进度正确记录

## 注意事项

1. **SEO 优化**: 节点和帖子页面需要合适的 meta 标签
2. **性能优化**: 懒加载记忆卡片组件，避免首屏加载过慢
3. **错误处理**: 当 nodeId/postId 无效时，显示友好错误提示
4. **权限控制**: 检查用户是否有权限访问私有课程内容
5. **分享功能**: URL 应该是可分享的，他人访问也能看到内容

## 未来扩展

1. **搜索集成**: 支持 `/read?search=关键词` 搜索模式
2. **标签筛选**: 支持 `/read?tag=标签名` 标签筛选
3. **历史记录**: 记录用户浏览历史，支持快速返回
4. **相关推荐**: 在帖子详情页显示相关推荐内容
