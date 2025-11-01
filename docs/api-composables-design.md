# API Composables 设计文档

## 1. 概述

### 1.1 问题分析

当前项目中 API 调用存在大量重复代码，主要问题：

1. **重复的状态管理**：每个 API 调用都要管理 `loading`、`error` 状态
2. **重复的错误处理**：`try-catch`、`response.code` 判断到处重复
3. **重复的提示逻辑**：成功/失败提示代码分散在各处
4. **分页逻辑混乱**：`lastId`、`lastScore`、`hasMore` 等状态管理重复
5. **难以维护**：相同逻辑散落在多个组件中，修改困难

### 1.2 解决方案

创建 **3 个通用 Composables** 封装所有 API 调用模式：

**GET 请求（2种）：**
- `useFetch` - 获取数据（支持单个对象和列表，统一接口）
- `useInfiniteScroll` - 无限滚动分页列表（GET 列表 - cursor 分页）

**修改请求（1种）：**
- `useMutation` - 数据变更（统一处理 POST/PUT/DELETE）

**重要简化：**
1. `useList` 已合并到 `useFetch` - 通过泛型自动支持对象和列表
2. `useCreate`/`useUpdate`/`useDelete` 已合并到 `useMutation` - 统一的变更接口

### 1.3 设计原则

- ✅ **类型安全**：完整的 TypeScript 类型支持
- ✅ **灵活配置**：支持自定义转换、回调、错误处理
- ✅ **统一规范**：所有 API 调用遵循相同模式
- ✅ **易于测试**：逻辑集中，便于单元测试
- ✅ **渐进式重构**：可以逐步替换现有代码

---

## 2. API 响应格式规范

### 2.1 标准响应格式

```typescript
interface ApiResponse<T = any> {
  code: number        // 状态码：200 成功，401 未登录，其他为失败
  data: T            // 返回数据
  message?: string   // 错误信息
  msg?: string       // 错误信息（兼容字段）
}
```

### 2.2 状态码约定

| 状态码 | 含义 | 处理方式 |
|--------|------|----------|
| 200 | 成功 | 正常处理数据 |
| 401 | 未登录 | 提示"请先登录" |
| 其他 | 失败 | 显示 message/msg 或默认错误信息 |

---

## 3. Composables 详细设计

### GET 请求场景分类

| Composable | 适用场景 | 返回数据类型 | 是否分页 | 典型示例 |
|-----------|---------|------------|---------|---------|
| `useFetch` | 获取单个对象或列表 | 对象或数组 | 否 | 用户信息、课程详情、热门排行、订阅列表 |
| `useInfiniteScroll` | 滚动加载列表 | 数组 | cursor 分页 | 帖子流、评论列表、消息列表 |

**注意：** `useFetch` 统一支持单个对象和列表两种模式，通过泛型 `T` 自动识别。

---

## 3.1 useFetch - 获取数据（对象或列表）

### 功能说明
封装获取单个资源对象或列表数据的逻辑，支持两种模式：
1. **单个对象模式**：获取用户信息、课程详情等
2. **列表模式**：一次性加载热门课程、订阅列表等（不分页，或者 page-based 分页）

### 类型定义

```typescript
interface FetchOptions<T> {
  // 必填：获取数据的函数
  fetchFn: () => Promise<ApiResponse<T>>

  // 可选：数据转换函数
  transform?: (item: T extends Array<infer U> ? U : T) => T extends Array<infer U> ? U : T

  // 可选：是否立即加载（默认 false）
  immediate?: boolean

  // 可选：成功回调
  onSuccess?: (data: T) => void

  // 可选：错误回调
  onError?: (error: Error) => void

  // 可选：默认值
  defaultValue?: T

  // 可选：刷新时是否保留旧数据（默认 true，避免闪烁）
  keepDataOnRefresh?: boolean

  // 可选：防抖延迟（毫秒）
  debounce?: number

  // 可选：节流延迟（毫秒）
  throttle?: number
}

interface FetchReturn<T> {
  data: Ref<T | null>         // 数据（对象或数组）
  loading: Ref<boolean>       // 加载状态
  error: Ref<Error | null>    // 错误信息
  isReady: Ref<boolean>       // 数据是否已就绪
  isRefreshing: Ref<boolean>  // 是否正在刷新
  isEmpty: Ref<boolean>       // 是否为空（数组为空或对象为null）

  execute: () => Promise<void>           // 手动执行
  refresh: (silent?: boolean) => Promise<void>  // 刷新数据
  reset: () => void                      // 重置状态
}
```

**刷新行为说明：**

1. **`refresh()` - 标准刷新**
   - 显示 loading 状态
   - 重置 error 为 null
   - 默认保留旧数据直到新数据到达（避免闪烁）
   - 失败时保留旧数据

2. **`refresh(true)` - 静默刷新**
   - `loading` 保持 false
   - `isRefreshing` 为 true（用于显示小型加载指示器）
   - 适用于下拉刷新、轮询等场景

3. **`reset()` - 完全重置**
   - 清空所有数据
   - 重置 loading、error
   - 回到初始状态

### 使用示例

#### 示例 1：获取单个对象

```typescript
// ✅ 获取用户信息
const { data: userInfo, loading, refresh } = useFetch({
  fetchFn: userServiceV1.getCurrentUser,
  immediate: true
})
```

#### 示例 2：获取列表

```typescript
// ✅ 加载热门课程列表
const { data: hotCourses, loading, isEmpty, refresh } = useFetch({
  fetchFn: courseServiceV1.getHotCourses,
  immediate: true
})

// isEmpty 会自动判断数组是否为空
watch(isEmpty, (empty) => {
  if (empty) {
    console.log('没有热门课程')
  }
})
```

### 刷新行为示例

```typescript
// 场景1：标准刷新（显示 loading）
const { data, loading, refresh } = useFetch({
  fetchFn: courseServiceV1.getHotCourses,
  immediate: true
})

await refresh()  // loading=true，保留旧数据直到新数据到达

// 场景2：静默刷新（不显示 loading，适合下拉刷新）
const { data, loading, isRefreshing, refresh } = useFetch({
  fetchFn: subscriptionServiceV1.getUserSubscriptions,
  immediate: true
})

await refresh(true)  // loading=false, isRefreshing=true

// 模板中使用
<v-progress-circular v-if="loading" />
<v-icon v-else-if="isRefreshing" class="rotating">mdi-refresh</v-icon>

// 场景3：不保留旧数据（立即清空，显示加载状态）
const { data, refresh } = useFetch({
  fetchFn: api.getList,
  keepDataOnRefresh: false  // 刷新时立即清空数据
})

await refresh()  // data 立即变为 null，然后显示新数据
```

### 更多使用场景

```typescript
// 场景1：加载订阅列表（支持下拉刷新）
const { data: subscriptions, loading, isRefreshing, refresh } = useFetch({
  fetchFn: () => subscriptionServiceV1.getUserSubscriptions(userId),
  immediate: true
})

// 场景2：加载系统配置（带数据转换）
const { data: categories } = useFetch({
  fetchFn: systemServiceV1.getCourseCategories,
  transform: (category) => ({
    ...category,
    displayName: `${category.icon} ${category.name}`
  })
})

// 场景3：轮询数据（静默刷新）
const { data: notifications, refresh } = useFetch({
  fetchFn: notificationServiceV1.getUnread,
  immediate: true
})

setInterval(() => refresh(true), 30000)  // 每 30 秒轮询

// 场景4：搜索结果（防抖）
const { data: searchResults, loading, execute } = useFetch({
  fetchFn: () => courseServiceV1.searchCourses(keyword.value),
  debounce: 300,
  keepDataOnRefresh: false
})

watch(keyword, () => { if (keyword.value) execute() })
```

---

## 3.2 useInfiniteScroll - 无限滚动分页

### 功能说明
封装基于 cursor 的无限滚动分页逻辑，支持任意分页参数组合（lastId、lastScore、timestamp、offset 等）。

**设计亮点：**
- ✅ 灵活的分页参数（`CursorParams` 支持任意字段）
- ✅ 用户自定义参数提取逻辑（`getNextParams`）
- ✅ 支持带筛选条件的分页（params 可包含 category、type 等）
- ✅ 兼容 cursor-based 和 offset-based 分页

### 类型定义

```typescript
// 灵活的游标类型，支持任意分页参数
type CursorParams = Record<string, any>

interface InfiniteScrollOptions<T> {
  // 必填：获取数据的函数
  // params 包含当前的分页参数（如 lastId, lastScore, timestamp 等）
  fetchFn: (params: CursorParams) => Promise<ApiResponse<T[]>>

  // 必填：提取下一页参数的函数
  // 从最后一项中提取下一次请求需要的参数
  getNextParams: (lastItem: T, currentParams: CursorParams) => CursorParams

  // 可选：初始分页参数（默认 {}）
  initialParams?: CursorParams

  // 可选：数据转换函数
  transform?: (item: T) => T

  // 可选：错误回调
  onError?: (error: Error) => void
}

interface InfiniteScrollReturn<T> {
  items: Ref<T[]>             // 列表数据
  loading: Ref<boolean>       // 加载状态
  error: Ref<Error | null>    // 错误信息
  hasMore: Ref<boolean>       // 是否还有更多数据
  params: Ref<CursorParams>   // 当前分页参数

  loadMore: (done?: LoadMoreCallback) => Promise<void>  // 加载更多
  reset: () => void                                      // 重置状态
  refresh: (done?: LoadMoreCallback) => Promise<void>   // 刷新列表
}
```

### 使用示例

#### 当前代码（重复 50+ 行）

```typescript
// ❌ 旧方式：每个列表都要写这些
const posts = ref([])
const loading = ref(false)
const lastPostingId = ref(0)
const lastScore = ref(0)

const loadMore = async ({ done }) => {
  try {
    const response = await postServiceV1.getPosts(
      undefined,
      props.currNodeId,
      lastScore.value,
      lastPostingId.value
    )

    if (response.code === 200) {
      response.data.forEach((posting) => {
        if (posting.voteType === 0) {
          posting.voteType = null
        }
      })
      props.data.otherPostings.push(...response.data)

      if (response.data.length > 0) {
        lastPostingId.value = response.data[response.data.length - 1].id
        lastScore.value = response.data[response.data.length - 1].score || 0
        done('ok')
      } else {
        done('empty')
      }
    }
  } catch {
    // todo
  }
}
```

#### 优化后（简洁灵活）

```typescript
// ✅ 新方式：支持任意分页参数
const { items: posts, loadMore, loading } = useInfiniteScroll({
  // fetchFn 接收分页参数对象
  fetchFn: (params) =>
    postServiceV1.getPosts(undefined, props.currNodeId, params.lastScore, params.lastId),

  // 定义如何从最后一项提取下一页参数
  getNextParams: (lastItem) => ({
    lastId: lastItem.id,
    lastScore: lastItem.score || 0
  }),

  // 初始参数
  initialParams: {
    lastId: 0,
    lastScore: 0
  },

  // 数据转换
  transform: (post) => ({
    ...post,
    voteType: post.voteType === 0 ? null : post.voteType
  })
})

// 通过 emit 通知父组件
watch(posts, (newPosts) => {
  emit('posts-updated', newPosts)
})
```

### 更多使用场景

```typescript
// 场景1：只用 lastId（最简单）
const { items: comments, loadMore } = useInfiniteScroll({
  fetchFn: (params) => commentServiceV1.getComments(postId, 'post', params.lastId),
  getNextParams: (lastItem) => ({ lastId: lastItem.id }),
  initialParams: { lastId: 0 }
})

// 场景2：lastId + timestamp（时间排序）
const { items: messages, loadMore } = useInfiniteScroll({
  fetchFn: (params) =>
    messageServiceV1.getSystemMessages(type, params.lastId, params.timestamp),
  getNextParams: (lastItem) => ({
    lastId: lastItem.id,
    timestamp: lastItem.createTime
  }),
  initialParams: {
    lastId: 0,
    timestamp: Date.now()
  }
})

// 场景3：复杂场景 - lastId + lastScore + 其他参数
const { items: professions, loadMore } = useInfiniteScroll({
  fetchFn: (params) =>
    professionServiceV1.getProfessionsByCategory(
      params.lastId,
      params.mainCategory,
      params.subCategory
    ),
  getNextParams: (lastItem, currentParams) => ({
    ...currentParams,  // 保留 mainCategory, subCategory
    lastId: lastItem.id
  }),
  initialParams: {
    lastId: 0,
    mainCategory: selectedMainCategory.value,
    subCategory: selectedSubCategory.value
  }
})

// 场景4：offset-based 分页（虽然不推荐，但也支持）
const { items: posts, loadMore } = useInfiniteScroll({
  fetchFn: (params) => postServiceV1.getPosts({ offset: params.offset, limit: 20 }),
  getNextParams: (lastItem, currentParams) => ({
    offset: currentParams.offset + 20
  }),
  initialParams: { offset: 0 }
})
```

---

## 3.3 useMutation - 数据变更

### 功能说明
封装所有数据变更操作的通用逻辑，统一处理 POST/PUT/DELETE 请求。

**设计理念：**
- 所有变更操作本质相同：发送请求 → 显示 loading → 处理结果 → 显示提示
- 通过配置项区分不同场景（如删除需要确认对话框）
- 减少 66% 代码重复，API 更简洁统一

### 类型定义

```typescript
interface MutationOptions<TPayload, TResult> {
  // 可选：成功提示信息
  successMessage?: string

  // 可选：失败提示信息
  errorMessage?: string

  // 可选：成功回调（接收结果和原始请求参数）
  onSuccess?: (result: TResult, payload: TPayload) => void | Promise<void>

  // 可选：失败回调
  onError?: (error: Error) => void

  // 可选：是否自动显示提示（默认 true）
  showToast?: boolean

  // 可选：是否需要确认对话框（默认 false）
  confirm?: boolean

  // 可选：确认提示信息
  confirmMessage?: string

  // 可选：防抖延迟（毫秒）
  debounce?: number

  // 可选：节流延迟（毫秒）
  throttle?: number

  // 可选：是否允许并发请求（默认 false）
  allowConcurrent?: boolean
}

interface MutationReturn<TPayload, TResult> {
  execute: (payload: TPayload) => Promise<TResult | null> // 执行变更
  loading: Ref<boolean> // 加载状态
  error: Ref<Error | null> // 错误信息
  data: Ref<TResult | null> // 返回数据
  reset: () => void // 重置状态
}
```

### 使用示例

#### 场景 1：创建资源（POST）

```typescript
// ✅ 创建帖子
const { execute: createPost, loading: posting } = useMutation(
  postService.createPost,
  {
    successMessage: '发布成功',
    onSuccess: (result) => {
      router.push(`/post/${result.id}`)
    }
  }
)

// 使用
await createPost({ title: '标题', content: '内容' })
```

#### 场景 2：更新资源（PUT）

```typescript
// ✅ 更新用户信息
const { execute: updateUser, loading } = useMutation(
  (data) => userService.updateCurrentUser(data.name, data.biography),
  {
    successMessage: '修改成功',
    onSuccess: loadUser
  }
)

// 使用
await updateUser({
  name: info.value.name,
  biography: info.value.biography
})
```

#### 场景 3：更新资源（需要 ID）

```typescript
// ✅ 更新课程（通过闭包传递 ID）
const courseId = ref(123)
const { execute: updateCourse } = useMutation(
  (data) => courseService.updateCourse(courseId.value, data),
  {
    successMessage: '课程已更新',
    onSuccess: (result, originalData) => {
      // result: 服务器返回的结果
      // originalData: 传入的数据
      console.log('Updated with:', originalData)
    }
  }
)

// 使用
await updateCourse({ name: '新课程名', description: '新描述' })
```

#### 场景 4：删除资源（DELETE，带确认）

```typescript
// ✅ 删除帖子（带确认对话框）
const { execute: deletePost, loading: deleting } = useMutation(
  postService.deletePost,
  {
    successMessage: '删除成功',
    confirm: true,
    confirmMessage: '确定要删除这篇帖子吗？',
    onSuccess: (result, postId) => {
      // postId 是传入的参数
      emit('deletePosting', postId)
    }
  }
)

// 使用
await deletePost(postingId)
```

#### 场景 5：取消订阅（无确认）

```typescript
// ✅ 取消订阅
const { execute: unsubscribe } = useMutation(
  subscriptionService.unsubscribe,
  {
    successMessage: '已取消订阅',
    onSuccess: loadSubscriptions
  }
)

// 使用
await unsubscribe(subscriptionId)
```

### 更多使用场景

```typescript
// 场景1：批量操作
const { execute: batchDelete } = useMutation(
  (ids: number[]) => postService.batchDelete(ids),
  {
    successMessage: `已删除 ${ids.length} 项`,
    confirm: true,
    confirmMessage: `确定要删除这 ${ids.length} 项吗？`
  }
)

// 场景2：点赞（节流防抖）
const { execute: upvote } = useMutation(
  (postId: number) => upvoteService.upvote(postId, 'post', 'up'),
  {
    throttle: 1000,  // 1秒内只能点一次
    successMessage: '点赞成功'
  }
)

// 场景3：自动保存（防抖）
const { execute: autoSave } = useMutation(
  (content: string) => draftService.saveDraft(content),
  {
    debounce: 2000,  // 2秒后自动保存
    showToast: false  // 不显示提示
  }
)

watch(editorContent, (newContent) => {
  autoSave(newContent)
})

// 场景4：表单提交（去重保护）
const { execute: submitForm, loading: submitting } = useMutation(
  formService.submit,
  {
    successMessage: '提交成功'
    // 默认 allowConcurrent=false，自动去重
  }
)

// 模板中自动禁用
<v-btn @click="submitForm(formData)" :loading="submitting">
  提交
</v-btn>
```

### 与原 API 对比

```typescript
// ❌ 旧方式：useCreate（30+ 行）
const submitting = ref(false)
const submitCareerApplication = async () => {
  try {
    submitting.value = true
    const response = await professionService.createProfession(data)
    if (response.code === 200) {
      showSnackbar('提交成功')
      closeDialog()
      loadList()
    } else if (response.code === 401) {
      showSnackbar('请先登录')
    } else {
      showSnackbar(response.message || '提交失败')
    }
  } catch (error) {
    showSnackbar('提交失败')
  } finally {
    submitting.value = false
  }
}

// ✅ 新方式：useMutation（5 行）
const { execute: submitCareerApplication, loading: submitting } = useMutation(
  professionService.createProfession,
  {
    successMessage: '提交成功',
    onSuccess: () => {
      closeDialog()
      loadList()
    }
  }
)
```

---

## 4. 统一错误处理

### 4.0 请求去重和防抖

为了防止重复请求和提升用户体验，所有 Composables 都内置请求去重保护。

#### 4.0.1 自动去重保护

**默认行为：**
- 当 `loading = true` 时，自动忽略新的请求
- 避免用户快速点击导致的重复调用

```typescript
// useCreate/useUpdate/useDelete 内部实现
async function execute(data: TData) {
  // ✅ 自动去重：如果正在请求中，直接返回
  if (loading.value) {
    console.warn('Request already in progress, ignoring duplicate call')
    return null
  }

  loading.value = true
  try {
    // ... 执行请求
  } finally {
    loading.value = false
  }
}
```

#### 4.0.2 可配置的防抖/节流

对于需要更精细控制的场景，可以配置防抖或节流：

```typescript
interface CreateOptions<TData, TResult> {
  // ... 其他选项

  // 可选：防抖延迟（毫秒）
  debounce?: number

  // 可选：节流延迟（毫秒）
  throttle?: number

  // 可选：是否允许并发请求（默认 false）
  allowConcurrent?: boolean
}
```

**使用示例：**

```typescript
// 场景1：搜索输入防抖（避免每次输入都请求）
const { execute: searchCourses } = useList({
  fetchFn: () => courseServiceV1.searchCourses(searchText.value),
  debounce: 300  // 300ms 防抖
})

// 用户输入时自动防抖
watch(searchText, () => {
  searchCourses()  // 自动防抖，300ms 内多次调用只执行最后一次
})

// 场景2：点赞按钮节流（防止快速点击）
const { execute: upvote } = useCreate(
  () => upvoteServiceV1.upvote(postId.value, 'post', 'up'),
  {
    throttle: 1000,  // 1秒内只能点一次
    successMessage: '点赞成功'
  }
)

// 场景3：创建按钮去重（默认行为）
const { execute: createPost, loading } = useCreate(
  postServiceV1.createPost,
  {
    successMessage: '发布成功'
  }
)

// ✅ 按钮自动禁用
<v-btn @click="createPost(data)" :loading="loading">
  发布
</v-btn>

// 场景4：允许并发请求（特殊场景）
const { execute: uploadFile } = useCreate(
  uploadServiceV1.uploadFile,
  {
    allowConcurrent: true,  // 允许同时上传多个文件
    successMessage: '上传成功'
  }
)

// 场景5：表单提交双重保护
const { execute: submitForm, loading: submitting } = useCreate(
  formServiceV1.submit,
  {
    successMessage: '提交成功',
    // 不需要配置，默认就有去重保护
  }
)

// 模板中双重保护
<v-btn
  @click="submitForm(formData)"
  :loading="submitting"
  :disabled="submitting || !formValid"
>
  提交
</v-btn>
```

#### 4.0.3 防抖/节流实现

```typescript
// 工具函数
function debounce<T extends (...args: any[]) => any>(
  fn: T,
  delay: number
): (...args: Parameters<T>) => void {
  let timeoutId: NodeJS.Timeout | null = null

  return function (...args: Parameters<T>) {
    if (timeoutId) {
      clearTimeout(timeoutId)
    }
    timeoutId = setTimeout(() => {
      fn(...args)
    }, delay)
  }
}

function throttle<T extends (...args: any[]) => any>(
  fn: T,
  delay: number
): (...args: Parameters<T>) => void {
  let lastCall = 0

  return function (...args: Parameters<T>) {
    const now = Date.now()
    if (now - lastCall >= delay) {
      lastCall = now
      fn(...args)
    }
  }
}

// 在 composable 中应用
export function useCreate<TData, TResult>(
  apiFn: (data: TData) => Promise<ApiResponse<TResult>>,
  options: CreateOptions<TData, TResult> = {}
): CreateReturn<TData, TResult> {
  const loading = ref(false)
  const error = ref<Error | null>(null)
  const data = ref<TResult | null>(null)

  // 核心执行函数
  const executeCore = async (data: TData): Promise<TResult | null> => {
    // 去重保护
    if (!options.allowConcurrent && loading.value) {
      console.warn('Request already in progress')
      return null
    }

    loading.value = true
    error.value = null

    try {
      const result = await handleApiCall(
        () => apiFn(data),
        {
          successMessage: options.successMessage,
          errorMessage: options.errorMessage,
          onSuccess: options.onSuccess,
          onError: options.onError,
          showToast: options.showToast
        }
      )

      data.value = result
      return result
    } finally {
      loading.value = false
    }
  }

  // 根据配置应用防抖/节流
  let execute = executeCore

  if (options.debounce) {
    execute = debounce(executeCore, options.debounce)
  } else if (options.throttle) {
    execute = throttle(executeCore, options.throttle)
  }

  const reset = () => {
    loading.value = false
    error.value = null
    data.value = null
  }

  return {
    execute,
    loading,
    error,
    data,
    reset
  }
}
```

#### 4.0.4 对比总结

| 保护机制 | 适用场景 | 行为 | 默认 |
|---------|---------|------|------|
| **去重保护** | 按钮点击、表单提交 | `loading=true` 时忽略新请求 | ✅ 开启 |
| **防抖 (debounce)** | 搜索输入、自动保存 | 等待用户停止操作后执行 | ❌ 可选 |
| **节流 (throttle)** | 点赞、滚动加载 | 固定时间间隔内只执行一次 | ❌ 可选 |
| **允许并发** | 文件上传、批量操作 | 允许多个请求同时进行 | ❌ 可选 |

#### 4.0.5 实际使用建议

```typescript
// ✅ 推荐：普通按钮点击（默认去重保护 + UI 禁用）
const { execute, loading } = useCreate(api, { successMessage: '成功' })
<v-btn @click="execute(data)" :loading="loading">提交</v-btn>

// ✅ 推荐：搜索输入（防抖）
const { execute } = useList({ fetchFn: searchApi, debounce: 300 })
watch(keyword, execute)

// ✅ 推荐：点赞按钮（节流）
const { execute } = useCreate(upvoteApi, { throttle: 1000 })

// ⚠️ 谨慎：允许并发（确认业务需求）
const { execute } = useCreate(uploadApi, { allowConcurrent: true })

// ❌ 避免：同时设置防抖和节流
const { execute } = useCreate(api, {
  debounce: 300,   // ❌ 只能选一个
  throttle: 1000
})
```

---

### 4.1 全局配置

创建 `web/src/composables/config.ts` 来配置全局行为：

```typescript
import { inject } from 'vue'
import { useRouter } from 'vue-router'

// 状态码处理器类型
export type StatusCodeHandler = (response: ApiResponse<any>) => boolean | void

export interface ApiComposableConfig {
  // 状态码处理器映射
  statusHandlers: Record<number, StatusCodeHandler>

  // 默认错误消息
  defaultErrorMessage: string

  // 是否自动显示错误提示
  showErrorToast: boolean
}

// 默认配置
export const defaultConfig: ApiComposableConfig = {
  statusHandlers: {
    // 401 未登录
    401: (response) => {
      const router = useRouter()
      const showSnackbar = inject<Function>('showSnackbar')

      showSnackbar?.('请先登录', 'error')
      router.push('/login')

      return true  // 返回 true 表示已处理，不再执行后续逻辑
    },

    // 403 无权限
    403: (response) => {
      const showSnackbar = inject<Function>('showSnackbar')
      showSnackbar?.('无权限访问', 'error')
      return true
    },

    // 500 服务器错误
    500: (response) => {
      const showSnackbar = inject<Function>('showSnackbar')
      showSnackbar?.('服务器错误，请稍后重试', 'error')
      return true
    }
  },

  defaultErrorMessage: '操作失败，请重试',
  showErrorToast: true
}

// 全局配置实例（可以被用户覆盖）
export let globalConfig = { ...defaultConfig }

// 设置全局配置
export function setApiConfig(config: Partial<ApiComposableConfig>) {
  globalConfig = { ...globalConfig, ...config }
}

// 在 main.js 中可以自定义配置
// setApiConfig({
//   statusHandlers: {
//     401: (response) => {
//       // 自定义 401 处理
//       store.commit('LOGOUT')
//       router.push('/login')
//       return true
//     }
//   }
// })
```

### 4.2 错误处理流程

所有 Composables 统一的错误处理流程：

```typescript
import { globalConfig } from './config'

async function handleApiCall<T>(
  apiFn: () => Promise<ApiResponse<T>>,
  options: {
    successMessage?: string
    errorMessage?: string
    onSuccess?: (data: T) => void
    onError?: (error: Error) => void
    showToast?: boolean
  }
) {
  try {
    const response = await apiFn()

    if (response.code === 200) {
      // 成功处理
      if (options.showToast !== false && options.successMessage) {
        showSnackbar(options.successMessage, 'success')
      }
      options.onSuccess?.(response.data)
      return response.data
    } else {
      // 检查是否有自定义状态码处理器
      const handler = globalConfig.statusHandlers[response.code]
      if (handler) {
        const handled = handler(response)
        if (handled) {
          // 如果处理器返回 true，表示已完全处理，不再继续
          return null
        }
      }

      // 默认错误处理
      const message = response.message || response.msg ||
                      options.errorMessage ||
                      globalConfig.defaultErrorMessage

      if (globalConfig.showErrorToast) {
        showSnackbar(message, 'error')
      }

      const error = new Error(message)
      options.onError?.(error)
      throw error
    }
  } catch (err) {
    // 网络错误或其他异常
    if (err instanceof Error && err.message) {
      // 已经是处理过的错误，直接抛出
      throw err
    }

    const message = options.errorMessage || globalConfig.defaultErrorMessage
    if (globalConfig.showErrorToast) {
      showSnackbar(message, 'error')
    }

    const error = err instanceof Error ? err : new Error('Unknown error')
    options.onError?.(error)
    throw error
  }
}
```

### 4.3 自定义配置示例

#### 在 `main.js` 中全局配置

```typescript
import { setApiConfig } from '@/composables/config'
import { useAuthStore } from '@/stores/auth'

// 应用启动时配置
setApiConfig({
  statusHandlers: {
    // 自定义 401 处理
    401: (response) => {
      const authStore = useAuthStore()
      const router = useRouter()

      // 清除用户状态
      authStore.logout()

      // 保存当前路由，登录后跳回
      const currentRoute = router.currentRoute.value.fullPath
      router.push({
        path: '/login',
        query: { redirect: currentRoute }
      })

      return true  // 已处理
    },

    // 自定义 403 处理
    403: (response) => {
      showSnackbar('您没有权限执行此操作', 'error')
      router.push('/403')
      return true
    },

    // 自定义 429 限流处理
    429: (response) => {
      showSnackbar('请求过于频繁，请稍后再试', 'warning')
      return true
    }
  },

  defaultErrorMessage: '操作失败，请稍后重试',
  showErrorToast: true
})
```

#### 单独覆盖某个 Composable 的行为

```typescript
// 某些场景不想显示 401 提示（如轮询接口）
const { data, refresh } = useFetch({
  fetchFn: () => userServiceV1.getCurrentUser(),
  immediate: true,
  onError: (error) => {
    // 自定义错误处理，不显示全局提示
    if (error.message.includes('401')) {
      console.log('User not logged in, skip notification')
    }
  }
})
```

### 4.4 配置优先级

错误处理的优先级：

1. **最高优先级**：Composable 的 `onError` 回调
2. **中等优先级**：全局 `statusHandlers` 配置
3. **最低优先级**：默认错误处理逻辑

```typescript
// 示例：组件级别覆盖全局行为
const { execute: createPost } = useCreate(
  postServiceV1.createPost,
  {
    successMessage: '发布成功',
    onError: (error) => {
      // 这里的处理会覆盖全局的 401 处理
      if (error.message.includes('401')) {
        // 自定义处理：不跳转，只显示提示
        showSnackbar('请先登录后再发布', 'warning')
        openLoginDialog()  // 打开登录弹窗而不是跳转页面
      }
    }
  }
)
```

---

## 5. 实施计划

### 5.1 第一阶段：创建 Composables（优先级：高）✅

**工作量**：2-3 小时

1. ✅ 创建 `web-ts/src/composables/config.ts` - 全局配置和错误处理
2. ✅ 创建 `web-ts/src/composables/types.ts` - 公共类型定义
3. ✅ 创建 `web-ts/src/composables/utils.ts` - 工具函数
4. ✅ 创建 `web-ts/src/composables/useFetch.ts` - 获取数据
5. ✅ 创建 `web-ts/src/composables/useInfiniteScroll.ts` - 无限滚动
6. ✅ 创建 `web-ts/src/composables/useMutation.ts` - 数据变更（统一 POST/PUT/DELETE）

**重要简化：**
- `useList` 已合并到 `useFetch`（对象和列表统一）
- `useCreate`/`useUpdate`/`useDelete` 已合并到 `useMutation`（变更操作统一）

### 5.2 第二阶段：试点重构（优先级：高）

**工作量**：2-3 小时

选择 6 个典型场景进行重构验证：

**GET 请求：**
1. **获取用户信息** (`SelfView.vue`) - 验证 `useFetch`（单个对象）
2. **加载热门课程** (`CourseList.vue`) - 验证 `useFetch`（列表）
3. **帖子列表** (`PostList.vue`) - 验证 `useInfiniteScroll`

**修改请求：**
4. **创建职业** (`CareerCenter.vue`) - 验证 `useMutation`（创建）
5. **更新用户信息** (`UserInfoTab.vue`) - 验证 `useMutation`（更新）
6. **删除帖子** (`UserPosting.vue`) - 验证 `useMutation`（删除）

### 5.3 第三阶段：逐步推广（优先级：中）

**工作量**：按需进行

根据开发节奏，在以下场景逐步应用：

- 评论列表
- 课程列表
- 路线图列表
- 职业列表
- 消息列表
- 管理后台各种列表

### 5.4 第四阶段：清理旧代码（优先级：低）

**工作量**：1-2 小时

- 移除不再使用的旧代码
- 统一代码风格
- 补充单元测试

---

## 6. 优势与收益

### 6.1 代码量对比

| 场景 | 当前代码行数 | 优化后行数 | 减少比例 |
|------|------------|-----------|---------|
| 获取单个对象 | ~20 行 | ~3 行 | 85% |
| 普通列表加载 | ~25 行 | ~3 行 | 88% |
| 无限滚动列表 | ~50 行 | ~5 行 | 90% |
| 创建资源 | ~30 行 | ~5 行 | 83% |
| 更新资源 | ~25 行 | ~3 行 | 88% |
| 删除资源 | ~20 行 | ~3 行 | 85% |

**预计减少代码量**：70-85%

### 6.2 质量提升

- ✅ **一致性**：所有 API 调用遵循相同模式
- ✅ **可维护性**：逻辑集中，修改一处影响全局
- ✅ **可测试性**：Composables 易于单元测试
- ✅ **类型安全**：完整的 TypeScript 支持
- ✅ **错误处理**：统一的错误处理和提示

### 6.3 开发体验

- ✅ 新功能开发更快（减少 70% 模板代码）
- ✅ Bug 修复更容易（逻辑集中）
- ✅ 代码审查更简单（关注业务逻辑而非重复代码）
- ✅ 新成员上手更快（统一的使用模式）

---

## 7. 注意事项

### 7.1 兼容性

- 不影响现有代码，可以渐进式重构
- 新旧代码可以共存
- 无需一次性全部替换

### 7.2 学习成本

- 团队成员需要熟悉 Composables 的使用方式
- 建议先阅读本文档和示例代码
- 可以先在小范围试点，积累经验后推广

### 7.3 特殊场景

某些特殊场景可能不适合使用这些 Composables：

- 需要高度定制的 API 调用逻辑
- 需要复杂的数据转换和状态管理
- 多个 API 调用需要编排的场景

这些场景仍然可以直接调用 Service 层，保持灵活性。

---

## 8. 后续优化方向

### 8.1 请求去重

防止短时间内重复发起相同请求：

```typescript
const { execute } = useCreate(api, {
  debounce: 300  // 300ms 内去重
})
```

### 8.2 乐观更新

立即更新 UI，失败时回滚：

```typescript
const { execute } = useUpdate(api, {
  optimistic: true,
  rollbackOnError: true
})
```

### 8.3 请求缓存

缓存 GET 请求结果：

```typescript
const { items } = useInfiniteScroll({
  fetchFn: api.getList,
  cache: true,
  cacheTime: 5 * 60 * 1000  // 5分钟
})
```

### 8.4 重试机制

失败自动重试：

```typescript
const { execute } = useCreate(api, {
  retry: 3,
  retryDelay: 1000
})
```

---

## 9. 参考资料

- [Vue 3 Composition API 官方文档](https://vuejs.org/guide/reusability/composables.html)
- [VueUse - Vue Composition API 工具库](https://vueuse.org/)
- [TanStack Query - 数据请求库最佳实践](https://tanstack.com/query/latest)

---

## 10. 总结

通过创建这 **3 个通用 Composables**，可以：

1. **大幅减少代码量**（70-85%）
2. **统一代码风格和错误处理**
3. **提升代码质量和可维护性**
4. **加速新功能开发**
5. **降低 Bug 率**
6. **覆盖所有 API 调用场景**（GET/POST/PUT/DELETE）

**最终方案（3 个 Composables）：**
- `useFetch` - 获取数据（统一支持单个对象和列表）
- `useInfiniteScroll` - 无限滚动（帖子流、评论列表等）
- `useMutation` - 数据变更（统一支持 POST/PUT/DELETE）

**重要简化：**
1. 原设计的 6 个 Composables 简化为 **3 个**
2. `useList` 合并到 `useFetch` - 通过泛型自动识别对象/列表
3. `useCreate`/`useUpdate`/`useDelete` 合并到 `useMutation` - 减少 66% 代码重复

**收益：**
- 代码文件从 6 个减少到 3 个（-50%）
- 学习成本更低（API 更简洁统一）
- 更符合现代数据请求库设计理念（React Query / TanStack Query）

建议优先实施第一和第二阶段，验证效果后再逐步推广。

---

**文档版本**：v2.0
**创建日期**：2025-10-31
**最后更新**：2025-10-31
**作者**：Claude Code

**更新日志：**
- v2.0: 简化架构，从 6 个 Composables 优化为 3 个
- v1.0: 初始设计（6 个 Composables）
