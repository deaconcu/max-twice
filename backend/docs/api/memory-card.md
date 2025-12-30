# 记忆卡片接口 API 文档

> Controller: `MemoryCardController`
> 基础路径: `/api/v1/memory`
> 限流: 50次/分钟 (按用户)

---

## 接口列表

1. [创建卡片](#1-创建卡片)
2. [更新卡片](#2-更新卡片)
3. [获取节点下的卡片列表](#3-获取节点下的卡片列表)
4. [获取卡片内容差异](#4-获取卡片内容差异)
5. [删除卡片](#5-删除卡片)

---

## DTO 类型说明

### CardWithSrsDTO（卡片完整信息）

```json
{
  "id": 12345,
  "front": "什么是闭包？",
  "back": "闭包是函数和其周围词法环境的组合...",
  "deck": {
    "id": 100,
    "title": "JavaScript 基础",
    "description": "JS 核心概念",
    "nodeId": 500,
    "courseId": 10,
    "cardCount": 25
  },
  "creator": {
    "id": 1,
    "name": "张三",
    "avatar": "/avatars/zhangsan.jpg"
  },
  "srsState": {
    "id": 9001,
    "type": 0,
    "currentStep": 0,
    "interval": 0,
    "reviewDueAt": "2024-12-30T10:00:00",
    "lastReviewedAt": null,
    "repetitions": 0,
    "lapseCount": 0
  },
  "hasDeckUpdate": false,
  "hasCardUpdate": false
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 卡片ID |
| front | String | 卡片正面（问题） |
| back | String | 卡片背面（答案） |
| deck | DeckSummaryDTO | 所属卡片组信息 |
| creator | UserBriefDTO | 创建者信息 |
| srsState | UserCardSrsDTO | SRS 学习状态 |
| hasDeckUpdate | Boolean | 卡片组有更新 |
| hasCardUpdate | Boolean | 用户个人修改了卡片 |

### DeckSummaryDTO（卡片组摘要）

```json
{
  "id": 100,
  "postId": 200,
  "nodeId": 500,
  "courseId": 10,
  "title": "JavaScript 基础",
  "description": "JS 核心概念",
  "state": 1,
  "updatedAt": "2024-12-30T10:00:00",
  "createdAt": "2024-12-01T10:00:00",
  "upvoteCount": 15,
  "cardCount": 25
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 卡片组ID |
| postId | Long | 关联帖子ID |
| nodeId | Long | 关联节点ID |
| courseId | Long | 关联课程ID |
| title | String | 卡片组标题 |
| description | String | 卡片组描述 |
| state | Integer | 状态：0=草稿，1=已发布，2=已归档 |
| updatedAt | String | 更新时间 |
| createdAt | String | 创建时间 |
| upvoteCount | Integer | 点赞数 |
| cardCount | Integer | 卡片数量 |

### UserBriefDTO（用户简要信息）

```json
{
  "id": 1,
  "name": "张三",
  "avatar": "/avatars/zhangsan.jpg"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户ID |
| name | String | 用户名 |
| avatar | String | 头像URL |

### UserCardSrsDTO（SRS 学习状态）

```json
{
  "id": 9001,
  "type": 2,
  "currentStep": 0,
  "interval": 7,
  "reviewDueAt": "2025-01-06T10:00:00",
  "lastReviewedAt": "2024-12-30T10:00:00",
  "repetitions": 3,
  "lapseCount": 0
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | SRS 状态ID |
| type | Integer | 卡片状态：0=NEW, 1=LEARNING, 2=REVIEW, 3=RELEARNING |
| currentStep | Integer | 当前学习步骤（仅 type=1或3时有效） |
| interval | Integer | 复习间隔（type=1或3时单位为分钟，type=2时单位为天） |
| reviewDueAt | String | 下次复习到期时间 |
| lastReviewedAt | String | 上次复习时间 |
| repetitions | Integer | 成功复习次数 |
| lapseCount | Integer | 遗忘次数 |

#### SRS 卡片状态说明

| type | 名称 | 说明 | interval单位 |
|------|------|------|--------------|
| 0 | NEW | 新卡片，从未学习 | - |
| 1 | LEARNING | 初次学习阶段 | 分钟 |
| 2 | REVIEW | 已掌握，长期复习 | 天 |
| 3 | RELEARNING | 遗忘后重新学习 | 分钟 |

**状态流转**:
```
NEW (0) → LEARNING (1) → REVIEW (2)
                          ↓ 回答错误
                        RELEARNING (3) → REVIEW (2)
```

### CardDiffDTO（卡片内容差异）

```json
{
  "cardId": 12345,
  "hasDiff": true,
  "deckVersion": {
    "front": "什么是闭包？",
    "back": "闭包是函数和其词法环境的组合。"
  },
  "userVersion": {
    "front": "什么是闭包？（我的笔记）",
    "back": "闭包是函数和其词法环境的组合。\n\n补充：可以访问外部变量。"
  },
  "diff": {
    "front": {
      "added": "（我的笔记）",
      "removed": ""
    },
    "back": {
      "added": "\n\n补充：可以访问外部变量。",
      "removed": ""
    }
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| cardId | Long | 卡片ID |
| hasDiff | Boolean | 是否有差异 |
| deckVersion | CardContentDTO | 卡片组原始版本 |
| userVersion | CardContentDTO | 用户修改版本 |
| diff | Object | 差异详情（front 和 back 的增删内容） |

### CreateCardRequest（创建卡片请求）

```json
{
  "deckId": 100,
  "front": "什么是闭包？",
  "back": "闭包是函数和其周围词法环境的组合..."
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deckId | Long | 是 | 卡片组ID |
| front | String | 是 | 卡片正面（问题），最大长度由配置接口获取 |
| back | String | 是 | 卡片背面（答案），最大长度由配置接口获取 |

### UpdateCardRequest（更新卡片请求）

```json
{
  "front": "什么是闭包？（更新后）",
  "back": "闭包是函数和其周围词法环境的组合...（更新后）"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| front | String | 是 | 卡片正面，最大长度2000字符 |
| back | String | 是 | 卡片背面，最大长度2000字符 |

---

## 1. 创建卡片

**接口路径**: `POST /api/v1/memory/cards`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体** (`CreateCardRequest`):
```json
{
  "deckId": 100,
  "front": "什么是闭包？",
  "back": "闭包是函数和其周围词法环境的组合..."
}
```

**请求参数说明**:
- `deckId` (Long, 必填): 卡片组ID
  - 验证规则: 不能为空
- `front` (String, 必填): 卡片正面（问题）
  - 验证规则: 不能为空，长度由配置接口动态获取
- `back` (String, 必填): 卡片背面（答案）
  - 验证规则: 不能为空，长度由配置接口动态获取

**返回类型**: `Void`

**返回示例**:
```json
{
  "code": 200,
  "message": "创建成功",
  "timestamp": 1735459200000
}
```

**前端调用**:
```typescript
// API 调用
memoryCardApi.createCard(createCardRequest)

// 实际使用 (DeckDetailDialog.vue:1316-1325)
const { execute: createCardMutation, loading: creatingCard } = useMutation(
  (data: { deckId: number; front: string; back: string }) =>
    memoryApi.createCard(data),
  {
    successMessage: '创建成功',
    onSuccess: () => {
      showEditDialog.value = false
      refreshDeckDetail()  // 刷新整个卡片列表
    }
  }
)
```

**使用场景**:
- `DeckDetailDialog.vue` - 卡片组详情对话框，创建新卡片

**业务说明**:
- 在指定卡片组中创建新卡片
- 自动创建初始 SRS 学习状态（type=0 NEW）
- 用户必须有卡片组的访问权限
- 长度验证规则从配置接口获取

---

## 2. 更新卡片

**接口路径**: `PUT /api/v1/memory/cards/{cardId}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| cardId | Long | 是 | 卡片ID，必须大于0 |

**请求体** (`UpdateCardRequest`):
```json
{
  "front": "什么是闭包？（更新后）",
  "back": "闭包是函数和其周围词法环境的组合...（更新后）"
}
```

**请求参数说明**:
- `front` (String, 必填): 卡片正面
  - 验证规则: 不能为空，最大长度2000字符
- `back` (String, 必填): 卡片背面
  - 验证规则: 不能为空，最大长度2000字符

**返回类型**: `Void`

**返回示例**:
```json
{
  "code": 200,
  "message": "更新成功",
  "timestamp": 1735459200000
}
```

**前端调用**:
```typescript
// API 调用
memoryCardApi.updateCard(cardId, updateCardRequest)

// 实际使用 (DeckDetailDialog.vue:1293-1303)
const { execute: updateCardMutation, loading: updatingCard } = useMutation(
  ({ cardId, data }) => memoryApi.updateCard(cardId, data),
  {
    successMessage: '更新成功',
    onSuccess: () => {
      showEditDialog.value = false
      refreshDeckDetail()  // 刷新整个卡片列表
    }
  }
)
```

**使用场景**:
- `DeckDetailDialog.vue` - 卡片组详情对话框，编辑卡片

**业务说明**:
- 只有卡片创建者可以更新
- 更新后 `hasCardUpdate` 标记为 `true`
- 不影响 SRS 学习进度
- 如果卡片组也有更新，需要用户决定是否同步

---

## 3. 获取节点下的卡片列表

**接口路径**: `GET /api/v1/memory/cards/node/{nodeId}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| nodeId | Long | 是 | 节点ID，必须大于0 |

**返回类型**: `List<CardWithSrsDTO>`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 12345,
      "front": "什么是闭包？",
      "back": "闭包是函数和其周围词法环境的组合...",
      "deck": {
        "id": 100,
        "title": "JavaScript 基础",
        "nodeId": 500,
        "cardCount": 25
      },
      "creator": {
        "id": 1,
        "name": "张三",
        "avatar": "/avatars/zhangsan.jpg"
      },
      "srsState": {
        "id": 9001,
        "type": 2,
        "interval": 7,
        "reviewDueAt": "2025-01-06T10:00:00",
        "repetitions": 3,
        "lapseCount": 0
      },
      "hasDeckUpdate": false,
      "hasCardUpdate": false
    }
  ],
  "timestamp": 1735459200000
}
```

**前端调用**:
```typescript
// API 调用
memoryCardApi.getCardsByNode(nodeId)

// 实际使用 (DeckDetailDialog.vue:1142-1156)
const response = await memoryApi.getUserCardsByNode(nodeId)
if (response?.data) {
  studyCards.value = response.data  // 保存用户学习的卡片列表
}
```

**使用场景**:
- `DeckDetailDialog.vue` - 加载节点下所有学习卡片，用于差异对比

**业务说明**:
- 查询当前用户在指定节点下的所有卡片
- 包含所有卡片组的卡片
- 按创建时间倒序排列
- 包含完整的 SRS 学习状态

---

## 4. 获取卡片内容差异

**接口路径**: `GET /api/v1/memory/cards/{cardId}/diff`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| cardId | Long | 是 | 卡片ID，必须大于0 |

**返回类型**: `CardDiffDTO`

**返回示例（有差异）**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "cardId": 12345,
    "hasDiff": true,
    "deckVersion": {
      "front": "什么是闭包？",
      "back": "闭包是函数和其词法环境的组合。"
    },
    "userVersion": {
      "front": "什么是闭包？（我的笔记）",
      "back": "闭包是函数和其词法环境的组合。\n\n补充：可以访问外部变量。"
    },
    "diff": {
      "front": {
        "added": "（我的笔记）",
        "removed": ""
      },
      "back": {
        "added": "\n\n补充：可以访问外部变量。",
        "removed": ""
      }
    }
  },
  "timestamp": 1735459200000
}
```

**返回示例（无差异）**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "cardId": 12345,
    "hasDiff": false,
    "message": "卡片内容与卡片组一致"
  },
  "timestamp": 1735459200000
}
```

**前端调用**:
```typescript
// API 调用
memoryCardApi.getCardDiff(cardId)
```

**使用场景**:
- `DeckDetailDialog.vue` - 差异对比 Tab，前端通过比较 deckDetail.cards 和 studyCards 计算差异

**业务说明**:
- 比较用户版本与卡片组原始版本的差异
- 用于用户决定同步或保留个人修改
- 只有卡片所有者可以查看

---

## 5. 删除卡片

**接口路径**: `DELETE /api/v1/memory/cards/{cardId}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| cardId | Long | 是 | 卡片ID，必须大于0 |

**返回类型**: `Void`

**返回示例**:
```json
{
  "code": 200,
  "message": "删除成功",
  "timestamp": 1735459200000
}
```

**前端调用**:
```typescript
// API 调用
memoryCardApi.deleteCard(cardId)

// 实际使用 (DeckDetailDialog.vue:1282-1291)
const { execute: deleteCardMutation } = useMutation(
  (cardId: number) => memoryApi.deleteCard(cardId),
  {
    successMessage: '删除成功',
    onSuccess: () => {
      refreshDeckDetail()  // 刷新整个卡片列表
    }
  }
)
```

**使用场景**:
- `DeckDetailDialog.vue` - 卡片组详情对话框，删除卡片

**业务说明**:
- 软删除卡片（设置 deleted_at 字段）
- 同时删除用户的 SRS 学习状态
- 只有卡片创建者可以删除
- 删除后不可恢复

---

## 错误码

| 错误码 | 说明 | 常见场景 |
|--------|------|----------|
| 200 | 成功 | 操作成功 |
| 1002 | 参数异常 | 缺少必填字段、长度超限、ID无效 |
| 1006 | 权限不足 | 不是创建者、没有访问权限 |
| 1101 | 用户未登录 | 未提供token或token无效 |
| 2201 | 卡片组不存在 | 创建卡片时卡片组不存在 |
| 2202 | 卡片不存在 | 更新、查看、删除不存在的卡片 |
| 2301 | 访问过于频繁 | 超过限流阈值 |

---

## 使用说明

### 卡片更新机制

#### hasDeckUpdate（卡片组有更新）
- **含义**：卡片组创建者更新了原始卡片
- **触发**：卡片组中的卡片被修改
- **用户选项**：
  - 同步卡片组版本（覆盖个人修改）
  - 保留个人版本
  - 查看差异后手动合并

#### hasCardUpdate（用户个人修改）
- **含义**：用户修改了自己的卡片副本
- **触发**：用户调用更新接口修改卡片
- **用户选项**：
  - 保留个人修改
  - 同步回卡片组版本
  - 查看差异决定

#### 冲突处理
当两个标记都为 `true` 时：
1. 调用获取差异接口查看详细变化
2. 提供三个选项：使用卡片组版本、保留个人版本、手动合并
3. 用户选择后更新卡片或标记为已确认

### SRS 学习系统

#### 工作原理
SRS（Spaced Repetition System）间隔重复系统根据记忆曲线自动计算复习时间：

1. **新卡片 (NEW)**：
   - 首次学习，interval = 0
   - 立即可以开始学习

2. **学习中 (LEARNING)**：
   - 初次学习阶段，使用短间隔（分钟级）
   - 多个学习步骤（如：1分钟、10分钟、1天）
   - 完成所有步骤后进入 REVIEW

3. **复习 (REVIEW)**：
   - 已掌握的卡片，使用长间隔（天级）
   - 间隔随成功复习次数增长（如：1天、3天、7天、15天...）
   - 回答错误会进入 RELEARNING

4. **重新学习 (RELEARNING)**：
   - 遗忘的卡片，重新学习
   - 使用较短间隔
   - 完成后回到 REVIEW

#### 复习间隔计算
- **学习阶段**：固定步骤（如：1分钟、10分钟、1天）
- **复习阶段**：根据公式计算，通常为 `上次间隔 × 难度系数`
- **难度调整**：根据用户回答质量动态调整

### 前端集成建议

#### 1. 卡片列表展示
```
[节点学习页]
├─ 卡片统计卡片
│  ├─ 新卡片: 5 张
│  ├─ 学习中: 3 张
│  ├─ 复习: 12 张
│  └─ 待复习: 8 张 [开始复习按钮]
│
└─ 卡片列表
   ├─ 卡片1 [NEW] [编辑] [删除]
   ├─ 卡片2 [REVIEW] [有更新提示] [编辑] [删除]
   └─ 卡片3 [LEARNING] [编辑] [删除]
```

#### 2. 卡片状态显示
使用不同颜色标识卡片状态：
- **NEW**：灰色（未学习）
- **LEARNING**：蓝色（学习中）
- **REVIEW**：绿色（已掌握）
- **RELEARNING**：橙色（重新学习）

#### 3. 复习提醒
根据 `reviewDueAt` 计算待复习卡片：
- 已到期：红色标记，优先复习
- 即将到期（1小时内）：橙色标记
- 未到期：显示剩余时间

#### 4. 更新提示
当卡片有更新标记时：
- 显示徽章："卡片组有更新" / "你修改了此卡片"
- 点击徽章可查看差异
- 提供快捷同步按钮

---

## 相关接口

- `/api/v1/memory/decks` - 卡片组管理
- `/api/v1/memory/review` - 复习接口
- `/api/v1/memory/bank` - 记忆库管理
- `/api/v1/config/validation` - 获取验证规则（card-front, card-back 的长度限制）

---

## 版本信息

- **API 版本**: v1
- **文档版本**: 1.0
- **最后更新**: 2024-12-30
