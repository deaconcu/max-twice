# ProfileView 实现文档 (方案 A)

## 概述

本文档描述将 web-ts 中的 SelfView 页面功能迁移到 web-template-2 的 ProfileView（"我的" tab）的实现方案。

**实施方案：方案 A - 渐进式增强**
- 保持现有水平 Tab 导航布局
- 保留顶部用户信息卡片
- 组件化拆分各 Tab 功能
- 为每个 Tab 实现完整功能

## 目标

在现有 ProfileView 基础上，将用户个人中心的 9 个功能模块组件化并实现完整功能，采用 web-template-2 的简约设计风格，提供完整的用户信息管理、学习进度跟踪、订阅管理等功能。

## 功能模块

### 1. 个人信息 (UserInfoTab)
**功能描述：**
- 显示用户基本信息（用户名、头像、个人简介）
- 支持编辑用户名和个人简介
- 显示账户创建时间等元信息

**数据结构：**
```typescript
interface UserInfo {
  id: number
  name: string
  email: string
  avatar?: string
  biography?: string
  createdAt: string
  updatedAt: string
}
```

**UI 组件：**
- 用户头像（可点击上传）
- 可编辑文本框（用户名、简介）
- 保存/取消按钮
- 统计卡片（加入天数、活跃度等）

---

### 2. 学习进度 (LearningTab)
**功能描述：**
- 显示用户正在学习的路线图和课程
- 展示学习进度（完成节点数/总节点数）
- 支持切换查看路线图和课程两种视图
- 使用 Vue Flow 可视化路线图结构

**数据结构：**
```typescript
interface LearningProgress {
  totalProgress: number        // 总体进度百分比
  completedNodes: number       // 已完成节点数
  totalNodes: number          // 总节点数
  roadmaps: UserRoadmap[]     // 学习中的路线图
  courses: LearningCourse[]   // 学习中的课程
}

interface UserRoadmap {
  id: number
  name: string
  progress: number
  totalNodes: number
  completedNodes: number
  lastActivity: string
}

interface LearningCourse {
  id: number
  courseId: number
  title: string
  progress: number
  totalLessons: number
  completedLessons: number
  lastActivity: string
}
```

**UI 组件：**
- 进度总览卡片（环形进度图）
- Tab 切换器（路线图/课程）
- 路线图流程图（Vue Flow）
- 课程列表卡片（进度条 + 统计）
- 最近活动时间线

---

### 3. 统计数据 (StatsTab)
**功能描述：**
- 显示用户内容的统计数据（浏览量、点赞数、评论数等）
- 支持时间段选择（今天、昨天、7天、30天、全部）
- 趋势图表展示数据变化

**数据结构：**
```typescript
interface StatsData {
  totalViews: number          // 总浏览量
  totalTwice: number          // 总点赞数
  totalHelpful: number        // 总有用数
  totalComments: number       // 总评论数
  dailyStats?: DailyStats[]   // 每日统计数据
}

interface DailyStats {
  date: string
  views: number
  twice: number
  helpful: number
  comments: number
}
```

**UI 组件：**
- 时间段选择器（按钮组）
- 统计卡片组（4个指标）
- 趋势折线图（Chart.js 或简单实现）
- 数据对比（环比/同比增长）

---

### 4. 订阅管理 (SubscriptionTab)
**功能描述：**
- 显示用户订阅的课程列表
- 支持拖拽排序调整课程顺序
- 支持取消订阅（删除课程）
- 显示课程简介

**数据结构：**
```typescript
interface UserCourse {
  id: number
  courseId: number
  course: {
    id: number
    name: string
    description: string
    icon?: string
    iconColor?: string
  }
  order: number
}
```

**UI 组件：**
- 课程 Chip 列表（可拖拽）
- 删除按钮（悬停显示）
- 课程简介展示区
- 保存/恢复按钮

**技术依赖：**
- `vuedraggable` - 拖拽排序功能

---

### 5. 关注用户 (UserFollowingTab)
**功能描述：**
- 显示关注的用户列表
- 支持取消关注
- 显示用户基本信息和互动统计

**数据结构：**
```typescript
interface Following {
  id: number
  userId: number
  user: {
    id: number
    name: string
    avatar?: string
    biography?: string
  }
  followedAt: string
}
```

**UI 组件：**
- 用户卡片列表
- 用户头像 + 基本信息
- 关注/取消关注按钮
- 空状态提示

---

### 6. 我的内容 (UserContentsTab)
**功能描述：**
- 显示用户创建的内容列表（节点、课程等）
- 支持搜索和筛选
- 显示内容统计信息

**数据结构：**
```typescript
interface UserContent {
  id: number
  title: string
  type: 'node' | 'course' | 'roadmap'
  views: number
  likes: number
  comments: number
  createdAt: string
  status: 'published' | 'draft'
}
```

**UI 组件：**
- 内容类型筛选器
- 内容卡片列表
- 统计信息展示
- 编辑/删除操作

---

### 7. 我的文章 (UserPostsTab)
**功能描述：**
- 显示用户发布的文章列表
- 支持编辑和删除文章
- 显示文章阅读量和互动数据

**数据结构：**
```typescript
interface Post {
  id: number
  title: string
  content: string
  type: PostType
  views: number
  likes: number
  comments: number
  createdAt: string
  status: 'published' | 'draft'
}
```

**UI 组件：**
- 文章列表卡片
- 文章封面图片
- 统计信息
- 编辑/删除按钮

---

### 8. 记忆卡片组 (UserMemoryDecksTab)
**功能描述：**
- 显示用户创建的记忆卡片组
- 显示卡片组状态和学习进度
- 支持删除卡片组
- 快速进入复习模式

**数据结构：**
```typescript
interface MemoryCardDeck {
  id: number
  name: string
  description?: string
  cardCount: number
  reviewedCount: number
  state: DeckState
  createdAt: string
  lastReviewAt?: string
}

enum DeckState {
  ACTIVE = 'ACTIVE',
  ARCHIVED = 'ARCHIVED',
  DELETED = 'DELETED'
}
```

**UI 组件：**
- 卡片组网格布局
- 卡片组卡片（名称、进度、统计）
- 状态标签
- 操作按钮（复习、编辑、删除）

---

### 9. 我创建的路线图 (UserRoadmapsTab)
**功能描述：**
- 显示用户创建的学习路线图
- 显示路线图的使用统计
- 支持编辑和删除路线图

**数据结构：**
```typescript
interface UserRoadmap {
  id: number
  name: string
  description: string
  profession: {
    id: number
    name: string
  }
  nodes: RoadmapNode[]
  usageCount: number
  createdAt: string
  status: 'public' | 'private' | 'draft'
}
```

**UI 组件：**
- 路线图卡片列表
- 路线图缩略图（Vue Flow mini）
- 使用统计展示
- 操作按钮（查看、编辑、删除、发布）

---

## 现有结构分析

### 已有功能
- ✅ 页面布局框架（水平 Tab 导航）
- ✅ 用户信息卡片（头像、姓名、邮箱、统计数据）
- ✅ 9 个 Tab 页签框架
- ✅ 响应式设计基础
- ✅ 符合 web-template-2 设计风格

### 需要改进
- ❌ 所有 Tab 内容为空状态，需实现功能
- ❌ 所有逻辑在单文件（437 行），需组件化
- ❌ 缺少真实数据和交互逻辑
- ❌ 没有路由参数支持

## 技术架构

### 目录结构
```
web-template-2/
├── src/
│   ├── views/
│   │   └── ProfileView.vue          # 主页面（保持现有布局）
│   ├── components/
│   │   └── profile/                 # Profile 相关组件（新建）
│   │       ├── UserInfoTab.vue      # 个人信息
│   │       ├── LearningTab.vue      # 学习进度（正在学习）
│   │       ├── StatsTab.vue         # 统计数据
│   │       ├── SubscriptionTab.vue  # 订阅管理（关注的课程）
│   │       ├── FollowingTab.vue     # 关注的人
│   │       ├── CatalogsTab.vue      # 创建的目录
│   │       ├── ArticlesTab.vue      # 创建的文章
│   │       ├── MemoryDecksTab.vue   # 我的卡片组
│   │       └── RoadmapsTab.vue      # 创建的路线图
│   └── types/
│       └── profile.ts               # Profile 相关类型定义（新建）
```

### 主页面布局（保持现有结构）

```vue
<template>
  <div class="profile-page">
    <AppHeader />
    <LeftSidebar />

    <div class="main-content">
      <!-- 页面标题 -->
      <div class="mb-6">
        <div class="d-flex align-center">
          <v-avatar color="grey-lighten-3" size="64" rounded="lg" class="mr-3">
            <v-icon size="32" color="#666666">mdi-account</v-icon>
          </v-avatar>
          <div>
            <h1 class="text-h4 font-weight-bold text-grey-darken-4">我的</h1>
            <p class="text-body-2 text-grey-darken-2 mt-1">管理您的个人信息和学习数据</p>
          </div>
        </div>
      </div>

      <!-- 用户信息卡片（保留） -->
      <v-card border rounded="lg" class="mb-6">
        <v-card-text class="pa-6">
          <!-- 用户头像和基本信息 -->
          <div class="d-flex align-center mb-6">
            <v-avatar size="80" color="primary" class="mr-4">
              <v-icon icon="mdi-account" size="40" color="white"></v-icon>
            </v-avatar>
            <div class="flex-grow-1">
              <h2 class="text-h5 font-weight-bold mb-1">{{ userInfo.name }}</h2>
              <p class="text-body-2 text-grey-darken-2 mb-1">{{ userInfo.email }}</p>
              <p class="text-caption text-grey">加入于 {{ userInfo.joinDate }}</p>
            </div>
            <v-btn color="primary" variant="outlined" rounded="lg">
              <v-icon icon="mdi-pencil" size="18" class="mr-2"></v-icon>
              编辑资料
            </v-btn>
          </div>

          <!-- 统计信息（保留） -->
          <v-divider class="mb-4"></v-divider>
          <div class="d-flex justify-space-around flex-wrap" style="gap: 16px;">
            <!-- 统计卡片 -->
          </div>
        </v-card-text>
      </v-card>

      <!-- 水平 Tab 导航（保留） -->
      <v-tabs v-model="activeTab" color="primary" class="mb-6">
        <v-tab value="info">
          <v-icon icon="mdi-account-circle" size="18" class="mr-2"></v-icon>
          个人信息
        </v-tab>
        <!-- 其他 Tab... -->
      </v-tabs>

      <!-- Tab 内容（使用独立组件） -->
      <v-window v-model="activeTab">
        <v-window-item value="info">
          <UserInfoTab :user-info="userInfo" @update="handleUpdateUserInfo" />
        </v-window-item>
        <v-window-item value="studying">
          <LearningTab />
        </v-window-item>
        <!-- 其他 Tab 组件... -->
      </v-window>
    </div>
  </div>
</template>
```

### 路由配置

```typescript
{
  path: '/profile',
  name: 'profile',
  component: ProfileView,
  // 支持通过 query 参数切换 tab（需添加）
  // 例如: /profile?tab=studying
}
```

**需要添加的功能：**
```typescript
// ProfileView.vue
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 监听路由变化，支持 query 参数
watch(() => route.query.tab, (newTab) => {
  if (newTab && typeof newTab === 'string') {
    activeTab.value = newTab
  }
})

// Tab 切换时更新 URL
const handleTabChange = (tab: string) => {
  router.push({ query: { tab } })
}
```

### 样式规范

**设计原则：**
- 遵循 web-template-2 的极简风格
- 白色背景 + 浅灰边框 (#E5E5E5)
- 统一圆角 (8px-14px)
- 扁平化设计，无阴影或低阴影
- 主色调：蓝色 (#1867C0)

**关键样式：**
```css
.profile-page {
  background-color: #FFFFFF;
  min-height: 100vh;
}

.main-content {
  margin-left: max(160px, calc((100vw - 1550px) / 2));
  padding: 80px 40px 40px 40px;
  max-width: 1550px;
}

.sticky-nav {
  position: sticky;
  top: 75px;
  max-height: calc(100vh - 95px);
  overflow-y: auto;
}

.sticky-help {
  position: sticky;
  top: 75px;
  max-height: calc(100vh - 95px);
  overflow-y: auto;
}

/* Tab 激活状态 */
.v-list-item--active {
  background-color: #EFF6FF !important;
  color: #2563EB !important;
}
```

---

## 实现步骤（方案 A）

### Phase 1: 基础架构（优先级：高）
- [x] 分析现有 ProfileView 结构
- [x] 确定实施方案
- [ ] 创建 components/profile 目录
- [ ] 创建 types/profile.ts 类型定义文件
- [ ] 添加路由 query 参数支持

### Phase 2: 核心功能组件化（优先级：高）
- [ ] 实现 UserInfoTab 组件（带编辑保存功能）
- [ ] 实现 LearningTab 组件（学习进度展示）
- [ ] 实现 StatsTab 组件（统计数据和图表）
- [ ] 实现 SubscriptionTab 组件（订阅管理，含拖拽）
- [ ] 更新 ProfileView 集成以上组件

### Phase 3: 扩展功能（优先级：中）
- [ ] 实现 MemoryDecksTab 组件（记忆卡片组）
- [ ] 实现 RoadmapsTab 组件（我创建的路线图）
- [ ] 实现 FollowingTab 组件（关注的人）

### Phase 4: 次要功能（优先级：低）
- [ ] 实现 CatalogsTab 组件（创建的目录）
- [ ] 实现 ArticlesTab 组件（创建的文章）

### Phase 5: 优化和测试（优先级：中）
- [ ] 添加加载状态和错误处理
- [ ] 优化响应式布局
- [ ] 性能优化（组件懒加载）
- [ ] 整体测试和调整

---

## 依赖管理

### 需要安装的依赖
```bash
# 拖拽排序功能（订阅管理必需）
npm install vuedraggable@next

# 图表功能（统计数据可选）
# 可使用简单实现或安装图表库
# npm install chart.js vue-chartjs
```

### 现有依赖（已安装）
- Vue 3
- Vuetify 3
- Vue Router 4
- Pinia
- @vue-flow/core - 流程图
- TypeScript

---

## Mock 数据策略

在连接真实 API 之前，使用 Mock 数据进行开发和测试：

**Mock 数据位置：**
```
src/
├── mocks/
│   ├── profileData.ts       # Profile 相关 Mock 数据
│   ├── learningData.ts      # 学习进度 Mock 数据
│   └── statsData.ts         # 统计数据 Mock 数据
```

**Mock 数据示例：**
```typescript
// mocks/profileData.ts
export const mockUserInfo = {
  id: 1,
  name: '张三',
  email: 'zhangsan@example.com',
  biography: '热爱学习的程序员',
  createdAt: '2024-01-01T00:00:00Z',
  avatar: 'https://example.com/avatar.jpg'
}

export const mockSubscriptions = [
  {
    id: 1,
    courseId: 101,
    course: {
      id: 101,
      name: 'Python 编程入门',
      description: '从零开始学习 Python',
      icon: 'mdi-language-python',
      iconColor: 'blue'
    },
    order: 1
  },
  // ... more courses
]
```

---

## API 集成准备

### API 端点规划

```typescript
// services/profileService.ts
export const profileService = {
  // 个人信息
  getCurrentUser: () => GET('/api/v1/users/me'),
  updateCurrentUser: (data) => PUT('/api/v1/users/me', data),

  // 学习进度
  getLearningProgress: () => GET('/api/v1/users/me/learning'),

  // 统计数据
  getStats: (period) => GET(`/api/v1/users/me/stats?period=${period}`),

  // 订阅管理
  getSubscriptions: () => GET('/api/v1/users/me/subscriptions'),
  updateSubscriptions: (ids) => PUT('/api/v1/users/me/subscriptions', { ids }),

  // 记忆卡片组
  getMemoryDecks: () => GET('/api/v1/users/me/memory-decks'),
  deleteMemoryDeck: (id) => DELETE(`/api/v1/memory-decks/${id}`),

  // 路线图
  getUserRoadmaps: () => GET('/api/v1/users/me/roadmaps'),

  // 关注
  getFollowings: () => GET('/api/v1/users/me/followings'),
  unfollow: (userId) => DELETE(`/api/v1/users/me/followings/${userId}`)
}
```

---

## 性能优化

### 1. 组件懒加载
```typescript
const UserInfoTab = defineAsyncComponent(() =>
  import('@/components/profile/UserInfoTab.vue')
)
```

### 2. 虚拟滚动
对于长列表（订阅、关注、内容列表），使用虚拟滚动：
```vue
<v-virtual-scroll :items="items" :item-height="80">
  <template #default="{ item }">
    <item-card :item="item" />
  </template>
</v-virtual-scroll>
```

### 3. 数据缓存
使用 Pinia 缓存用户数据，避免重复请求：
```typescript
// stores/profile.ts
export const useProfileStore = defineStore('profile', {
  state: () => ({
    userInfo: null,
    subscriptions: [],
    stats: null,
    // ...
  }),
  actions: {
    async fetchUserInfo() {
      if (this.userInfo) return this.userInfo
      this.userInfo = await profileService.getCurrentUser()
      return this.userInfo
    }
  }
})
```

---

## 响应式设计

### 断点定义
- **Desktop (lg+)**: >= 1264px - 三栏布局（Tab 导航 + 内容 + 帮助）
- **Tablet (md)**: 960px - 1263px - 两栏布局（内容 + 帮助）
- **Mobile (sm-)**: < 960px - 单栏布局（全宽内容）

### 移动端适配
```css
@media (max-width: 960px) {
  .main-content {
    margin-left: 0;
    padding: 80px 20px 40px 20px;
  }

  .sticky-nav,
  .sticky-help {
    position: relative;
    top: 0;
  }

  /* Tab 导航改为横向滚动 */
  .v-list {
    display: flex;
    overflow-x: auto;
  }
}
```

---

## 测试计划

### 单元测试
- [ ] ProfileView 组件渲染测试
- [ ] Tab 切换逻辑测试
- [ ] 各 Tab 组件独立测试
- [ ] 数据加载和更新测试

### E2E 测试
- [ ] 用户信息编辑流程
- [ ] 订阅拖拽排序流程
- [ ] 路由参数切换流程

---

## 注意事项

1. **数据隔离**: 确保只显示当前登录用户的数据
2. **权限控制**: 编辑操作需要验证用户权限
3. **错误处理**: 友好的错误提示和重试机制
4. **加载状态**: 所有异步操作显示 loading 状态
5. **空状态**: 各列表提供友好的空状态提示
6. **国际化**: 预留 i18n 支持，使用 t() 函数

---

## 后续优化方向

1. **实时更新**: WebSocket 实时同步学习进度
2. **数据导出**: 支持导出学习报告
3. **分享功能**: 分享学习成就到社交媒体
4. **个性化**: 自定义主题和布局
5. **通知中心**: 集成系统通知功能

---

## 参考资料

- [Vue 3 文档](https://vuejs.org/)
- [Vuetify 3 文档](https://vuetifyjs.com/)
- [Vue Flow 文档](https://vueflow.dev/)
- [web-template-2 设计规范](./CLAUDE.md)

---

**文档版本**: v1.0
**创建日期**: 2025-11-07
**最后更新**: 2025-11-07
**维护者**: Development Team
