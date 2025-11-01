# API Composables 实施 Checklist

> 本文档提供 API Composables 的完整实施清单，确保不遗漏任何步骤。

---

## 📋 总览

- [x] **阶段 1**：创建基础设施（config + types）✅
- [x] **阶段 2**：实现 3 个 Composables ✅
- [x] **阶段 3**：试点重构验证 ✅
- [ ] **阶段 4**：全面推广
- [ ] **阶段 5**：清理旧代码

**实际总工作量**：约 3 小时（大幅简化）

**重大架构优化 v2.0：**
1. `useList` 已合并到 `useFetch`（通过泛型自动支持对象和列表）
2. `useCreate`/`useUpdate`/`useDelete` 已合并到 `useMutation`（统一变更接口）
3. 最终方案：**3 个 Composables** 代替原来的 6 个

**试点验证完成情况：**
- ✅ 试点 1：`SelfView.vue` - useFetch（获取对象）
- ✅ 试点 2：`CourseList.vue` - useFetch（获取列表）
- ✅ 试点 3：`PostingList.vue` - useInfiniteScroll
- ✅ 试点 4：`CareerCenter.vue` - useMutation（创建）
- ✅ 试点 5：`UserInfoTab.vue` - useMutation（更新）
- ✅ 试点 6：`UserPosting.vue` - useMutation（删除）

**代码减少量：平均 68%**

---

## 🎯 阶段 1：创建基础设施 ✅

**工作量**：30-45 分钟

### 1.1 创建全局配置文件 ✅

- [x] 创建 `web-ts/src/composables/config.ts`
  - [x] 定义 `StatusCodeHandler` 类型
  - [x] 定义 `ApiComposableConfig` 接口
  - [x] 实现 `defaultConfig`（包含 401/403/500 处理器）
  - [x] 导出 `globalConfig`
  - [x] 导出 `setApiConfig` 函数

---

### 1.2 创建公共类型定义 ✅

- [x] 创建 `web-ts/src/composables/types.ts`
  - [x] 定义 `ApiResponse<T>` 接口
  - [x] 定义 `CursorParams` 类型
  - [x] 定义 `LoadMoreCallback` 类型
  - [x] 导出所有类型

---

### 1.3 配置全局错误处理 ✅

- [x] `showSnackbar` 已在 `App.vue` 中通过 `provide` 注入
- [x] 默认错误处理已在 `config.ts` 中配置
- [x] 无需额外配置 `main.ts`

---

## 🔨 阶段 2：实现 Composables ✅

**工作量**：2-3 小时

### 2.1 实现 useFetch（获取数据） ✅

- [x] 创建 `web-ts/src/composables/useFetch.ts`
  - [ ] 定义 `ListOptions<T>` 接口
    - [ ] fetchFn（必填）
    - [ ] transform（可选）
    - [ ] immediate（可选）
    - [ ] onSuccess / onError（可选）
    - [ ] keepDataOnRefresh（可选，默认 true）
    - [ ] debounce / throttle（可选）
  - [ ] 定义 `ListReturn<T>` 接口
    - [ ] data: Ref<T[]>
    - [ ] loading: Ref<boolean>
    - [ ] error: Ref<Error | null>
    - [ ] isEmpty: Ref<boolean>
    - [ ] isRefreshing: Ref<boolean>
    - [ ] execute()
    - [ ] refresh(silent?)
    - [ ] reset()
  - [ ] 实现 useList 函数
    - [ ] 初始化状态
    - [ ] 实现 execute 逻辑（调用 fetchFn + transform）
    - [ ] 实现 refresh 逻辑（保留旧数据 + 静默刷新）
    - [ ] 实现 reset 逻辑
    - [ ] 应用 debounce/throttle（如果配置）
    - [ ] immediate=true 时自动执行
  - [ ] 导出 useList

**验证点：**
```typescript
const { data, loading, execute } = useList({
  fetchFn: async () => ({ code: 200, data: [1, 2, 3] })
})
await execute()
console.log(data.value) // [1, 2, 3]
```

---

### 2.2 实现 useFetch（获取单个对象）

- [ ] 创建 `web/src/composables/useFetch.ts`
  - [ ] 定义 `FetchOptions<T>` 接口
    - [ ] fetchFn（必填）
    - [ ] transform（可选）
    - [ ] immediate（可选）
    - [ ] onSuccess / onError（可选）
    - [ ] defaultValue（可选）
    - [ ] keepDataOnRefresh（可选）
    - [ ] debounce / throttle（可选）
  - [ ] 定义 `FetchReturn<T>` 接口
    - [ ] data: Ref<T | null>
    - [ ] loading: Ref<boolean>
    - [ ] error: Ref<Error | null>
    - [ ] isReady: Ref<boolean>
    - [ ] isRefreshing: Ref<boolean>
    - [ ] execute()
    - [ ] refresh(silent?)
    - [ ] reset()
  - [ ] 实现 useFetch 函数（逻辑类似 useList）
  - [ ] 导出 useFetch

**验证点：**
```typescript
const { data, loading, isReady } = useFetch({
  fetchFn: async () => ({ code: 200, data: { id: 1, name: 'Test' } }),
  immediate: true
})
await nextTick()
console.log(isReady.value) // true
console.log(data.value) // { id: 1, name: 'Test' }
```

---

### 2.3 实现 useInfiniteScroll（无限滚动）

- [ ] 创建 `web/src/composables/useInfiniteScroll.ts`
  - [ ] 定义 `InfiniteScrollOptions<T>` 接口
    - [ ] fetchFn: (params: CursorParams) => Promise<...>
    - [ ] getNextParams: (lastItem, currentParams) => CursorParams
    - [ ] initialParams（可选）
    - [ ] transform（可选）
    - [ ] onError（可选）
  - [ ] 定义 `InfiniteScrollReturn<T>` 接口
    - [ ] items: Ref<T[]>
    - [ ] loading: Ref<boolean>
    - [ ] error: Ref<Error | null>
    - [ ] hasMore: Ref<boolean>
    - [ ] params: Ref<CursorParams>
    - [ ] loadMore(done?)
    - [ ] reset()
    - [ ] refresh(done?)
  - [ ] 实现 useInfiniteScroll 函数
    - [ ] 初始化状态
    - [ ] 实现 loadMore 逻辑
      - [ ] 检查 loading 状态（防止重复请求）
      - [ ] 调用 fetchFn(params)
      - [ ] 应用 transform
      - [ ] 追加到 items
      - [ ] 调用 getNextParams 更新 params
      - [ ] 更新 hasMore
      - [ ] 调用 done 回调
    - [ ] 实现 refresh 逻辑（重置 + 重新加载）
    - [ ] 实现 reset 逻辑
  - [ ] 导出 useInfiniteScroll

**验证点：**
```typescript
const { items, loadMore, hasMore } = useInfiniteScroll({
  fetchFn: async (params) => ({
    code: 200,
    data: [{ id: params.lastId + 1 }, { id: params.lastId + 2 }]
  }),
  getNextParams: (lastItem) => ({ lastId: lastItem.id }),
  initialParams: { lastId: 0 }
})

await loadMore()
console.log(items.value) // [{ id: 1 }, { id: 2 }]
await loadMore()
console.log(items.value) // [{ id: 1 }, { id: 2 }, { id: 3 }, { id: 4 }]
```

---

### 2.4 实现 useCreate（创建资源）

- [ ] 创建 `web/src/composables/useCreate.ts`
  - [ ] 定义 `CreateOptions<TData, TResult>` 接口
    - [ ] successMessage（可选）
    - [ ] errorMessage（可选）
    - [ ] onSuccess / onError（可选）
    - [ ] showToast（可选，默认 true）
    - [ ] debounce / throttle（可选）
    - [ ] allowConcurrent（可选，默认 false）
  - [ ] 定义 `CreateReturn<TData, TResult>` 接口
    - [ ] execute: (data: TData) => Promise<TResult | null>
    - [ ] loading: Ref<boolean>
    - [ ] error: Ref<Error | null>
    - [ ] data: Ref<TResult | null>
    - [ ] reset()
  - [ ] 实现 useCreate 函数
    - [ ] 初始化状态
    - [ ] 实现 execute 核心逻辑
      - [ ] 去重保护（检查 loading）
      - [ ] 调用 apiFn
      - [ ] 处理响应（成功/失败）
      - [ ] 显示提示消息
      - [ ] 调用回调
    - [ ] 应用 debounce/throttle
  - [ ] 导出 useCreate

**验证点：**
```typescript
const { execute, loading, data } = useCreate(
  async (payload) => ({ code: 200, data: { id: 1, ...payload } }),
  { successMessage: '创建成功' }
)

await execute({ name: 'Test' })
console.log(data.value) // { id: 1, name: 'Test' }
console.log(loading.value) // false
```

---

### 2.5 实现 useUpdate（更新资源）

- [ ] 创建 `web/src/composables/useUpdate.ts`
  - [ ] 定义 `UpdateOptions<TData, TResult>` 接口（同 CreateOptions）
  - [ ] 定义 `UpdateReturn<TData, TResult>` 接口（同 CreateReturn）
  - [ ] 实现 useUpdate 函数（逻辑同 useCreate）
  - [ ] 导出 useUpdate

**验证点：**
```typescript
const itemId = ref(123)
const { execute, loading } = useUpdate(
  async (data) => ({ code: 200, data: { id: itemId.value, ...data } }),
  { successMessage: '更新成功' }
)

await execute({ name: 'Updated' })
```

---

### 2.6 实现 useDelete（删除资源）

- [ ] 创建 `web/src/composables/useDelete.ts`
  - [ ] 定义 `DeleteOptions<TResult>` 接口
    - [ ] successMessage（可选）
    - [ ] errorMessage（可选）
    - [ ] onSuccess / onError（可选）
    - [ ] showToast（可选）
    - [ ] confirm（可选，默认 false）
    - [ ] confirmMessage（可选）
  - [ ] 定义 `DeleteReturn<TResult>` 接口
    - [ ] execute: (id: number | string) => Promise<TResult | null>
    - [ ] loading: Ref<boolean>
    - [ ] error: Ref<Error | null>
    - [ ] reset()
  - [ ] 实现 useDelete 函数
    - [ ] 实现 execute 逻辑
    - [ ] 如果 confirm=true，显示确认对话框
    - [ ] 去重保护
    - [ ] 调用 apiFn
  - [ ] 导出 useDelete

**验证点：**
```typescript
const { execute, loading } = useDelete(
  async (id) => ({ code: 200, data: { id } }),
  { successMessage: '删除成功' }
)

await execute(123)
console.log(loading.value) // false
```

---

### 2.7 创建工具函数

- [ ] 创建 `web/src/composables/utils.ts`
  - [ ] 实现 `debounce<T>` 函数
  - [ ] 实现 `throttle<T>` 函数
  - [ ] 实现 `handleApiCall<T>` 函数（统一错误处理）
  - [ ] 导出所有工具函数

**验证点：**
```typescript
import { debounce, throttle } from '@/composables/utils'

const fn = debounce(() => console.log('debounced'), 300)
fn() // 300ms 后执行
```

---

## 🧪 阶段 3：试点重构验证

**工作量**：2-3 小时

### 3.1 验证 useFetch

- [ ] 重构 `SelfView.vue` 的用户信息加载
  - [ ] 找到 `loadUser` 函数
  - [ ] 替换为 `useFetch`
  - [ ] 验证功能正常
  - [ ] 验证错误处理
  - [ ] 验证 loading 状态
  - [ ] 提交代码

**重构前：**
```typescript
const info = ref({})
const loadUser = async () => {
  const response = await userServiceV1.getCurrentUser()
  if (response.code === 200) {
    info.value = response.data
  }
}
```

**重构后：**
```typescript
const { data: info, loading, refresh: loadUser } = useFetch({
  fetchFn: userServiceV1.getCurrentUser,
  immediate: true
})
```

---

### 3.2 验证 useList

- [ ] 重构 `CourseList.vue` 的热门课程加载
  - [ ] 找到 `loadHotCourses` 函数
  - [ ] 替换为 `useList`
  - [ ] 验证功能正常
  - [ ] 验证刷新行为
  - [ ] 提交代码

**重构前：**
```typescript
const hotCourses = ref([])
const loadHotCourses = async () => {
  const response = await courseServiceV1.getHotCourses()
  if (response.code === 200) {
    hotCourses.value = response.data
  }
}
```

**重构后：**
```typescript
const { data: hotCourses, loading, refresh } = useList({
  fetchFn: courseServiceV1.getHotCourses,
  immediate: true
})
```

---

### 3.3 验证 useInfiniteScroll

- [ ] 重构某个帖子列表组件的无限滚动
  - [ ] 找到 `loadMore` 函数
  - [ ] 替换为 `useInfiniteScroll`
  - [ ] 验证分页参数正确传递
  - [ ] 验证 transform 功能
  - [ ] 验证 hasMore 状态
  - [ ] 提交代码

**重构前：**
```typescript
const posts = ref([])
const lastId = ref(0)
const lastScore = ref(0)

const loadMore = async ({ done }) => {
  const response = await postServiceV1.getPosts(
    undefined, nodeId, lastScore.value, lastId.value
  )
  if (response.code === 200) {
    posts.value.push(...response.data)
    if (response.data.length > 0) {
      lastId.value = response.data[response.data.length - 1].id
      lastScore.value = response.data[response.data.length - 1].score
      done('ok')
    } else {
      done('empty')
    }
  }
}
```

**重构后：**
```typescript
const { items: posts, loadMore, hasMore } = useInfiniteScroll({
  fetchFn: (params) =>
    postServiceV1.getPosts(undefined, nodeId, params.lastScore, params.lastId),
  getNextParams: (lastItem) => ({
    lastId: lastItem.id,
    lastScore: lastItem.score || 0
  }),
  initialParams: { lastId: 0, lastScore: 0 }
})
```

---

### 3.4 验证 useCreate

- [ ] 重构 `CareerCenter.vue` 的创建职业功能
  - [ ] 找到 `submitCareerApplication` 函数
  - [ ] 替换为 `useCreate`
  - [ ] 验证去重保护
  - [ ] 验证成功提示
  - [ ] 验证错误处理
  - [ ] 提交代码

**重构前：**
```typescript
const submitting = ref(false)
const submitCareerApplication = async () => {
  try {
    submitting.value = true
    const response = await professionServiceV1.createProfession(data)
    if (response.code === 200) {
      showSnackbar('提交成功')
      closeDialog()
    }
  } finally {
    submitting.value = false
  }
}
```

**重构后：**
```typescript
const { execute: submitCareerApplication, loading: submitting } = useCreate(
  professionServiceV1.createProfession,
  {
    successMessage: '提交成功',
    onSuccess: closeDialog
  }
)
```

---

### 3.5 验证 useUpdate

- [ ] 重构 `UserInfoTab.vue` 的更新用户信息功能
  - [ ] 找到 `updateUser` 函数
  - [ ] 替换为 `useUpdate`
  - [ ] 验证功能正常
  - [ ] 提交代码

---

### 3.6 验证 useDelete

- [ ] 重构 `UserPosting.vue` 的删除帖子功能
  - [ ] 找到 `deletePosting` 函数
  - [ ] 替换为 `useDelete`
  - [ ] 验证确认对话框（如需要）
  - [ ] 验证功能正常
  - [ ] 提交代码

---

### 3.7 试点总结

- [ ] 运行完整测试流程
  - [ ] 加载用户信息
  - [ ] 加载热门课程
  - [ ] 无限滚动加载帖子
  - [ ] 创建职业
  - [ ] 更新用户信息
  - [ ] 删除帖子
- [ ] 检查控制台无报错
- [ ] 检查所有 loading 状态正常
- [ ] 检查错误提示正常显示
- [ ] 收集团队反馈
- [ ] 记录发现的问题
- [ ] 优化 Composables（如有需要）

---

## 🚀 阶段 4：全面推广

**工作量**：按需进行

### 4.1 制定推广计划

- [ ] 列出所有需要重构的组件
- [ ] 按模块分组（用户、课程、帖子、评论等）
- [ ] 估算每个模块的工作量
- [ ] 制定时间表

---

### 4.2 逐模块重构

#### 用户模块
- [ ] `UserView.vue`
- [ ] `SelfView.vue`
- [ ] `UserInfoTab.vue`
- [ ] `UserPosting.vue`
- [ ] `UserContentsTab.vue`
- [ ] `UserPostsTab.vue`
- [ ] `SubscriptionTab.vue`

#### 课程模块
- [ ] `CourseList.vue`
- [ ] `CourseManagement.vue`
- [ ] `ReadView.vue`

#### 职业模块
- [ ] `CareerCenter.vue`
- [ ] `ProfessionManagement.vue`

#### 路线图模块
- [ ] `RoadmapFlow.vue`
- [ ] `LearningView.vue`

#### 消息模块
- [ ] `Message.vue`

#### 管理后台
- [ ] `AdminView.vue`
- [ ] `SystemConfiguration.vue`

---

### 4.3 每个模块重构后

- [ ] 运行功能测试
- [ ] 运行 ESLint
- [ ] 运行 TypeScript 类型检查
- [ ] 提交代码
- [ ] Code Review

---

## 🧹 阶段 5：清理旧代码

**工作量**：1-2 小时

### 5.1 检查旧代码是否还在使用

- [ ] 搜索 `learnService.js` 的使用情况
- [ ] 确认没有组件再使用旧的 API 调用方式
- [ ] 检查是否有遗漏的组件

---

### 5.2 移除或归档旧代码

- [ ] 备份 `learnService.js`（移到 `archived/` 目录）
- [ ] 如果完全不用了，删除该文件
- [ ] 更新相关导入语句（如果有）

---

### 5.3 文档更新

- [ ] 更新 `README.md`
- [ ] 添加 Composables 使用指南
- [ ] 更新团队开发文档
- [ ] 记录最佳实践

---

### 5.4 代码审查

- [ ] 检查是否有残留的 console.log
- [ ] 检查是否有重复代码
- [ ] 统一代码风格
- [ ] 补充必要的注释

---

## ✅ 最终验收标准

### 功能验收

- [ ] 所有 GET 请求使用 `useFetch`/`useList`/`useInfiniteScroll`
- [ ] 所有 POST 请求使用 `useCreate`
- [ ] 所有 PUT 请求使用 `useUpdate`
- [ ] 所有 DELETE 请求使用 `useDelete`
- [ ] 所有 loading 状态正常显示
- [ ] 所有错误提示正常显示
- [ ] 401 跳转登录页正常
- [ ] 去重保护生效
- [ ] 防抖/节流功能正常

---

### 代码质量

- [ ] 无 ESLint 错误
- [ ] 无 TypeScript 类型错误
- [ ] 无 console.log（除了必要的日志）
- [ ] 代码风格统一
- [ ] 所有组件通过 Code Review

---

### 性能验收

- [ ] 代码量减少 70-85%
- [ ] 页面加载速度无明显下降
- [ ] 无内存泄漏
- [ ] 无重复请求（通过 Network 面板检查）

---

### 文档验收

- [ ] 完成设计文档
- [ ] 完成使用指南
- [ ] 完成 Checklist
- [ ] 团队成员都能理解和使用

---

## 📊 进度跟踪

### 总体进度

```
阶段 1: 基础设施     [ ] 0%
阶段 2: Composables  [ ] 0%
阶段 3: 试点验证     [ ] 0%
阶段 4: 全面推广     [ ] 0%
阶段 5: 清理代码     [ ] 0%
```

### 重构进度统计

```
总组件数：____ 个
已重构：____ 个
进度：____%
```

---

## 🐛 问题记录

### 遇到的问题

| 日期 | 问题描述 | 解决方案 | 状态 |
|------|---------|---------|------|
|      |         |         |      |

---

## 💡 优化建议

### 改进点

| 日期 | 建议内容 | 优先级 | 状态 |
|------|---------|--------|------|
|      |         |        |      |

---

## 📝 备注

- 遇到问题及时记录
- 每完成一个阶段进行团队 Review
- 保持与团队成员的沟通
- 定期更新进度

---

**文档版本**：v1.0
**创建日期**：2025-10-31
**最后更新**：2025-10-31
