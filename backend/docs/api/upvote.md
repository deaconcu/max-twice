# 点赞管理接口文档 (UpvotesController)

## 基本信息

- **Controller**: `UpvotesController.java`
- **基础路径**: `/api/v1`
- **Rate Limit**: 100 requests/minute (per user)
- **前端 API**: `web/src/api/modules/upvote.ts`

## 枚举类型说明

### ContentType (内容类型)
用途：标识可以被点赞的内容类型

| 值 | 名称 | 说明 |
|----|------|------|
| 1 | post | 帖子 |
| 2 | node | 节点（暂不支持点赞） |
| 3 | comment | 评论 |
| 4 | roadmap | 路线图 |
| 5 | memory_card_deck | 记忆卡片组 |
| 6 | memory_card | 记忆卡片（暂不支持点赞） |
| 7 | profession | 职业（暂不支持点赞） |
| 8 | course | 课程（暂不支持点赞） |

**当前支持点赞的内容类型**:
- ✅ post (1) - 帖子，支持 twice 和 like 两种点赞
- ✅ comment (3) - 评论，仅支持 like 点赞
- ✅ roadmap (4) - 路线图，仅支持 like 点赞
- ✅ memory_card_deck (5) - 记忆卡片组，仅支持 like 点赞

---

### VoteType (点赞类型)
用途：标识点赞的类型

| 值 | 名称 | 说明 | 使用场景 |
|----|------|------|----------|
| 1 | twice | Twice点赞 | 表示"内容只要看两次就能看懂""易于理解""清晰明了" |
| 2 | like | Like点赞 | 表示"有帮助""点赞""喜欢" |

**点赞类型与内容类型的关系**:
- **post**: 支持 twice (1) 和 like (2) 两种点赞
- **comment/roadmap/memory_card_deck**: 仅支持 like (2) 点赞

---

## DTO 类型说明

### UpvoteRequest (点赞请求)
用途：发起点赞/取消点赞操作

```json
{
  "objectId": 123,
  "objectType": 1,
  "type": 1
}
```

**字段说明**：
- `objectId` (Long, 必填): 被点赞对象的ID
  - 范围：必须大于0
  - 示例：帖子ID、评论ID、路线图ID等
- `objectType` (Integer, 必填): 内容类型
  - 取值：1-8（参考 ContentType 枚举）
  - 示例：1=帖子, 3=评论, 4=路线图, 5=记忆卡片组
- `type` (Integer, 必填): 点赞类型
  - 取值：1=twice, 2=like
  - 验证：必须在 1-2 范围内

**验证规则**：
- 所有字段都不能为空
- `objectId` 必须大于0
- `type` 必须在 1-2 范围内
- `objectType` 必须是有效的内容类型

---

### UpvoteStatusDTO (点赞状态响应)
用途：返回用户对特定内容的点赞状态

```json
{
  "twiceUpvoted": true,
  "likeUpvoted": false
}
```

**字段说明**：
- `twiceUpvoted` (Boolean): 是否已 twice 点赞
  - true: 已 twice 点赞
  - false: 未 twice 点赞
  - 仅对支持 twice 的内容类型（post）有意义
- `likeUpvoted` (Boolean): 是否已 like 点赞
  - true: 已 like 点赞
  - false: 未 like 点赞

**使用场景**：
- 显示点赞按钮的激活状态
- 判断用户是否已对内容点赞
- 切换点赞状态的前置检查

---

## 接口列表

## 1. 点赞/取消点赞

**接口路径**: `POST /api/v1/upvotes`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体** (`UpvoteRequest`):
```json
{
  "objectId": 123,
  "objectType": 1,
  "type": 1
}
```

**请求参数说明**:
- `objectId` (Long, 必填): 被点赞对象的ID，必须大于0
- `objectType` (Integer, 必填): 内容类型（1=post, 3=comment, 4=roadmap, 5=memory_card_deck）
- `type` (Integer, 必填): 点赞类型（1=twice, 2=like）

**返回类型**: `UpvoteStatusDTO`

**业务逻辑说明**:

### 点赞规则

1. **帖子点赞 (post, objectType=1)**:
   - 支持 twice (1) 和 like (2) 两种点赞
   - **首次点赞**: 直接创建点赞记录
   - **重复点赞相同类型**: 取消点赞（删除记录）
   - **切换点赞类型**: 更新点赞类型（twice ↔ like）

   示例场景：
   - 用户点 twice → 状态：{twiceUpvoted: true, likeUpvoted: false}
   - 用户再点 twice → 取消 twice → 状态：{twiceUpvoted: false, likeUpvoted: false}
   - 用户点 twice 后再点 like → 切换为 like → 状态：{twiceUpvoted: false, likeUpvoted: true}

2. **评论点赞 (comment, objectType=3)**:
   - 仅支持 like (2) 点赞
   - **首次点赞**: 创建 like 记录
   - **重复点赞**: 取消 like（删除记录）

3. **路线图点赞 (roadmap, objectType=4)**:
   - 仅支持 like (2) 点赞
   - **首次点赞**: 创建 like 记录
   - **重复点赞**: 取消 like（删除记录）

4. **记忆卡片组点赞 (memory_card_deck, objectType=5)**:
   - 仅支持 like (2) 点赞
   - **首次点赞**: 创建 like 记录
   - **重复点赞**: 取消 like（删除记录）

### 返回示例 1 - 帖子点赞成功（twice）:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "twiceUpvoted": true,
    "likeUpvoted": false
  }
}
```

### 返回示例 2 - 帖子点赞成功（like）:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "twiceUpvoted": false,
    "likeUpvoted": true
  }
}
```

### 返回示例 3 - 取消点赞:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "twiceUpvoted": false,
    "likeUpvoted": false
  }
}
```

### 返回示例 4 - 评论点赞（仅 like）:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "twiceUpvoted": false,
    "likeUpvoted": true
  }
}
```

**前端调用**:
```typescript
// API 调用
import { upvoteApi } from '@/api/modules/upvote'
import { ObjectType, VoteType } from '@/enums'

// 点赞帖子 - twice
await upvoteApi.upvote(123, ObjectType.POST, VoteType.TWICE)

// 点赞帖子 - like
await upvoteApi.upvote(123, ObjectType.POST, VoteType.LIKE)

// 点赞评论（仅 like）
await upvoteApi.upvote(456, ObjectType.COMMENT, VoteType.LIKE)

// 点赞路线图（仅 like）
await upvoteApi.upvote(789, ObjectType.ROADMAP, VoteType.LIKE)

// 点赞记忆卡片组（仅 like）
await upvoteApi.upvote(101, ObjectType.MEMORY_CARD_DECK, VoteType.LIKE)

// 实际使用 (SinglePost.vue:85-115)
const { execute: executeUpvote, loading: upvoteLoading } = useMutation(
  () => upvoteApi.upvote(props.post.id, ObjectType.POST, VoteType.TWICE),
  {
    onSuccess: (response) => {
      if (response.data) {
        // 更新本地点赞状态
        localTwiceUpvoted.value = response.data.twiceUpvoted
        localLikeUpvoted.value = response.data.likeUpvoted
      }
    }
  }
)

// 点赞按钮点击
const handleUpvote = async () => {
  await executeUpvote()
}
```

**使用场景**:
- `SinglePost.vue` - 帖子卡片的点赞按钮
- `PostDetailPage.vue` - 帖子详情页的点赞按钮
- `CommentCard.vue` - 评论卡片的点赞按钮
- `RoadmapCard.vue` - 路线图卡片的点赞按钮
- `DeckCard.vue` - 记忆卡片组的点赞按钮

---

## 2. 获取点赞状态

**接口路径**: `GET /api/v1/upvotes/status`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| objectId | Long | 是 | 被点赞对象的ID，必须大于0 |
| objectType | Integer | 是 | 内容类型（1=post, 3=comment, 4=roadmap, 5=memory_card_deck） |

**返回类型**: `UpvoteStatusDTO`

**返回示例 1 - 已点赞（twice）**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "twiceUpvoted": true,
    "likeUpvoted": false
  }
}
```

**返回示例 2 - 已点赞（like）**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "twiceUpvoted": false,
    "likeUpvoted": true
  }
}
```

**返回示例 3 - 未点赞**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "twiceUpvoted": false,
    "likeUpvoted": false
  }
}
```

**前端调用**:
```typescript
// API 调用
import { upvoteApi } from '@/api/modules/upvote'
import { ObjectType } from '@/enums'

// 获取帖子点赞状态
const response = await upvoteApi.getUpvoteStatus(123, ObjectType.POST)

// 获取评论点赞状态
const response = await upvoteApi.getUpvoteStatus(456, ObjectType.COMMENT)

// 实际使用 (PostDetailPage.vue:120-135)
const { data: upvoteStatus, execute: loadUpvoteStatus } = useFetch({
  fetchFn: () => upvoteApi.getUpvoteStatus(postId.value, ObjectType.POST),
  immediate: true,
  defaultValue: {
    twiceUpvoted: false,
    likeUpvoted: false
  }
})

// 根据状态显示点赞按钮
const twiceButtonColor = computed(() =>
  upvoteStatus.value?.twiceUpvoted ? 'primary' : 'default'
)

const likeButtonColor = computed(() =>
  upvoteStatus.value?.likeUpvoted ? 'error' : 'default'
)
```

**使用场景**:
- 页面加载时初始化点赞按钮状态
- 刷新点赞状态
- 验证用户点赞状态（防止重复操作）

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数验证失败 |
| 401 | 未登录（需要登录才能点赞） |
| 403 | 无权限 |
| 404 | 资源不存在（被点赞的内容不存在） |
| 429 | 请求频率超限（100次/分钟） |
| 500 | 服务器内部错误 |

**常见参数验证错误**:
- `对象ID不能为空` - objectId 为 null
- `对象ID必须大于0` - objectId ≤ 0
- `对象类型不能为空` - objectType 为 null
- `对象类型必须大于0` - objectType ≤ 0
- `投票类型不能为空` - type 为 null
- `投票类型不正确` - type 不在 1-2 范围内

**业务错误**:
- `INVALID_PARAMETER` - 不支持的内容类型（如 node、course 等）

---

## 测试用例建议

### 1. 帖子点赞（post）
- ✅ 首次点 twice（创建记录）
- ✅ 重复点 twice（取消点赞）
- ✅ 首次点 like（创建记录）
- ✅ 重复点 like（取消点赞）
- ✅ 先点 twice 再点 like（切换类型）
- ✅ 先点 like 再点 twice（切换类型）
- ✅ 帖子不存在返回404
- ✅ 未登录返回401

### 2. 评论点赞（comment）
- ✅ 首次点 like（创建记录）
- ✅ 重复点 like（取消点赞）
- ✅ 评论不存在返回404
- ✅ 尝试 twice 点赞（应该如何处理？）

### 3. 路线图点赞（roadmap）
- ✅ 首次点 like（创建记录）
- ✅ 重复点 like（取消点赞）
- ✅ 路线图不存在返回404
- ✅ 路线图被删除后的点赞状态

### 4. 记忆卡片组点赞（memory_card_deck）
- ✅ 首次点 like（创建记录）
- ✅ 重复点 like（取消点赞）
- ✅ 卡片组不存在返回404

### 5. 获取点赞状态
- ✅ 获取已点赞的状态（twice）
- ✅ 获取已点赞的状态（like）
- ✅ 获取未点赞的状态
- ✅ 获取不存在内容的点赞状态（应返回未点赞）
- ✅ 未登录返回401
- ✅ 参数验证（objectId ≤ 0, objectType 无效）

### 6. 参数验证
- ✅ objectId 为 null 返回400
- ✅ objectId 为 0 返回400
- ✅ objectId 为负数返回400
- ✅ objectType 为 null 返回400
- ✅ objectType 为无效值返回400
- ✅ type 为 null 返回400
- ✅ type 超出范围（0, 4, 5等）返回400

### 7. 并发场景
- ✅ 快速连续点击（防抖处理）
- ✅ 同一用户在不同设备同时点赞
- ✅ 多个用户同时点赞同一内容

### 8. 权限场景
- ✅ 未登录用户点赞返回401
- ✅ 已登录用户正常点赞
- ✅ 被封禁用户能否点赞

---

## 业务流程说明

### 点赞操作流程

```
用户点击点赞按钮
  ↓
前端调用 POST /api/v1/upvotes
  ↓
Controller 参数验证
  ↓
根据 objectType 分发到不同的 Service 方法
  ├─ post → upvotePost(objectId, user, type)
  ├─ comment → upvoteComment(objectId, user)
  ├─ roadmap → upvoteRoadmap(objectId, user)
  └─ memory_card_deck → upvoteMemoryCardDeck(objectId, user)
  ↓
Service 执行点赞逻辑：
  ├─ 查询现有点赞记录
  ├─ 判断操作类型（创建/取消/切换）
  │   ├─ 无记录 → 创建新点赞记录
  │   ├─ 相同类型 → 取消点赞（删除记录）
  │   └─ 不同类型 → 切换点赞类型（更新记录）
  ├─ 更新内容统计（点赞数）
  ├─ 发布领域事件（TwiceUpvoted/LikeUpvoted/Cancelled 等）
  └─ 更新排行榜（如果需要）
  ↓
查询最新点赞状态
  ↓
返回 UpvoteStatusDTO 给前端
  ↓
前端更新 UI（按钮颜色、点赞数）
```

### 点赞类型切换逻辑（仅 post）

```
初始状态: {twiceUpvoted: false, likeUpvoted: false}

用户点 twice:
  → 创建 twice 记录
  → {twiceUpvoted: true, likeUpvoted: false}

用户再点 like:
  → 删除 twice 记录
  → 创建 like 记录
  → {twiceUpvoted: false, likeUpvoted: true}

用户再点 like:
  → 删除 like 记录
  → {twiceUpvoted: false, likeUpvoted: false}

用户再点 twice:
  → 创建 twice 记录
  → {twiceUpvoted: true, likeUpvoted: false}
```

---

## 前端集成指南

### 1. 使用 useMutation 处理点赞操作

```typescript
import { useMutation } from '@/composables/useMutation'
import { upvoteApi } from '@/api/modules/upvote'
import { ObjectType, VoteType } from '@/enums'

// 点赞操作
const { execute: executeUpvote, loading: upvoteLoading } = useMutation(
  (type: VoteType) => upvoteApi.upvote(props.post.id, ObjectType.POST, type),
  {
    onSuccess: (response) => {
      if (response.data) {
        // 更新本地点赞状态
        twiceUpvoted.value = response.data.twiceUpvoted
        likeUpvoted.value = response.data.likeUpvoted
      }
    },
    onError: (error) => {
      console.error('点赞失败', error)
    }
  }
)

// Twice 点赞按钮
const handleTwiceUpvote = () => {
  executeUpvote(VoteType.TWICE)
}

// Like 点赞按钮
const handleLikeUpvote = () => {
  executeUpvote(VoteType.LIKE)
}
```

### 2. 显示点赞按钮状态

```vue
<template>
  <div class="upvote-buttons">
    <!-- Twice 按钮 -->
    <v-btn
      :color="twiceUpvoted ? 'primary' : 'default'"
      :loading="upvoteLoading"
      @click="handleTwiceUpvote"
    >
      <v-icon>{{ twiceUpvoted ? 'mdi-share' : 'mdi-share-outline' }}</v-icon>
      <span>Twice</span>
    </v-btn>

    <!-- Like 按钮 -->
    <v-btn
      :color="likeUpvoted ? 'error' : 'default'"
      :loading="upvoteLoading"
      @click="handleLikeUpvote"
    >
      <v-icon>{{ likeUpvoted ? 'mdi-heart' : 'mdi-heart-outline' }}</v-icon>
      <span>Like</span>
    </v-btn>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

// 本地点赞状态
const twiceUpvoted = ref(false)
const likeUpvoted = ref(false)
</script>
```

### 3. 页面加载时获取点赞状态

```typescript
import { useFetch } from '@/composables/useFetch'

// 获取点赞状态
const { data: upvoteStatus, loading: statusLoading } = useFetch({
  fetchFn: () => upvoteApi.getUpvoteStatus(postId.value, ObjectType.POST),
  immediate: true,
  defaultValue: {
    twiceUpvoted: false,
    likeUpvoted: false
  }
})

// 监听状态变化，更新本地状态
watch(upvoteStatus, (newStatus) => {
  if (newStatus) {
    twiceUpvoted.value = newStatus.twiceUpvoted
    likeUpvoted.value = newStatus.likeUpvoted
  }
}, { immediate: true })
```

### 4. 防抖处理（避免重复点击）

```typescript
import { ref } from 'vue'

const upvoteLoading = ref(false)

const handleUpvote = async (type: VoteType) => {
  if (upvoteLoading.value) return // 防止重复点击

  upvoteLoading.value = true
  try {
    const response = await upvoteApi.upvote(
      props.post.id,
      ObjectType.POST,
      type
    )
    if (response.data) {
      twiceUpvoted.value = response.data.twiceUpvoted
      likeUpvoted.value = response.data.likeUpvoted
    }
  } catch (error) {
    console.error('点赞失败', error)
  } finally {
    upvoteLoading.value = false
  }
}
```

### 5. 评论点赞（仅 like）

```vue
<template>
  <v-btn
    :color="likeUpvoted ? 'error' : 'default'"
    :loading="upvoteLoading"
    @click="handleUpvote"
    size="small"
  >
    <v-icon size="small">
      {{ likeUpvoted ? 'mdi-heart' : 'mdi-heart-outline' }}
    </v-icon>
    <span class="ml-1">{{ likeCount }}</span>
  </v-btn>
</template>

<script setup lang="ts">
const handleUpvote = async () => {
  await executeUpvote()
  // 点赞成功后，更新点赞数
  if (likeUpvoted.value) {
    likeCount.value++
  } else {
    likeCount.value--
  }
}
</script>
```

---

## 性能优化建议

### 1. 后端优化
- ✅ 使用事务保证点赞操作的原子性
- ✅ 使用 Redis 缓存点赞状态，减少数据库查询
- ✅ 使用异步事件处理点赞后的统计更新
- ✅ 批量查询点赞状态（如果需要）

### 2. 前端优化
- ✅ 使用乐观更新，先更新 UI 再等待服务器响应
- ✅ 防抖处理，避免快速连续点击
- ✅ 缓存点赞状态，避免重复请求
- ✅ 使用 loading 状态禁用按钮，防止重复提交

### 3. 缓存策略
- ✅ 点赞状态可以短期缓存（5-10秒）
- ✅ 点赞数可以使用 stale-while-revalidate 策略
- ✅ 使用 WebSocket 实时推送点赞数变化（可选）

---

## 领域事件说明

点赞操作会发布以下领域事件，用于后续处理（统计更新、通知等）：

### 点赞事件
- `TwiceUpvotedEvent` - Twice 点赞
- `LikeUpvotedEvent` - Like 点赞
- `UpvoteTypeSwitchedEvent` - 点赞类型切换

### 取消点赞事件
- `TwiceUpvoteCancelledEvent` - 取消 Twice 点赞
- `LikeUpvoteCancelledEvent` - 取消 Like 点赞

**事件订阅者**:
- 统计服务 - 更新内容的点赞数
- 排行榜服务 - 更新热门排行
- 通知服务 - 发送点赞通知（可选）
- 积分服务 - 奖励积分（可选）

---

## 相关文档

- [帖子管理接口文档](./post.md)
- [评论管理接口文档](./comment.md)
- [路线图管理接口文档](./roadmap.md)
- [记忆卡片管理接口文档](./memory-card.md)
- [统计服务文档](./stats.md)
