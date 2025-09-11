# 记忆卡片功能 API 接口文档

## 概述

基于 SRS (Spaced Repetition System) 设计规范 V8.0 的记忆卡片功能 API 接口定义。

## 基础信息

- **基础路径**: `/api/v1/memory`
- **响应格式**: JSON
- **认证方式**: JWT Token

## 状态码定义

### 卡片组状态 (DeckState)
- `0`: 审核中 (PENDING)
- `1`: 正常 (NORMAL)
- `2`: 锁定 (LOCKED)
- `3`: 私有 (PRIVATE)
- `4`: 已删除 (DELETED)

### 卡片状态 (CardState)
- `0`: 正常 (NORMAL)
- `1`: 已删除 (DELETED)

### 复习结果 (ReviewResult)
- `0`: 忘记了 (FAILED)
- `1`: 困难 (HARD)
- `2`: 良好 (GOOD)
- `3`: 简单 (EASY)

### 课程学习状态 (CourseStudyStatus)
- `1`: 学习中 (STUDYING)
- `2`: 已暂停 (PAUSED)
- `3`: 已归档 (ARCHIVED)

### 复习频率设置 (FrequencySetting)
- `0`: 高频 (HIGH)
- `1`: 普通 (NORMAL)
- `2`: 低频 (LOW)

---

## 1. 卡片组管理

### 1.1 获取卡片组列表

**GET** `/decks`

获取指定条件的卡片组列表，使用Keyset分页提供更好的性能。

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| postId | Long | 否 | 文章ID，筛选该文章下的卡片组 |
| creatorId | Long | 否 | 创建者ID |
| state | Integer | 否 | 卡片组状态 |
| sortBy | String | 否 | 排序字段: score/createdAt/upvoteCount，默认score |
| sortOrder | String | 否 | 排序方向: asc/desc，默认desc |
| lastScore | Double | 否 | Keyset分页：上一页最后一条记录的分数 |
| lastId | Long | 否 | Keyset分页：上一页最后一条记录的ID |
| limit | Integer | 否 | 每页大小，默认10，最大50 |

**Keyset分页说明**:
- 首次请求不需要传递lastScore和lastId
- 后续请求传递上一页最后一条记录的score和id
- 相比传统offset分页，Keyset分页性能更好，避免数据重复或遗漏

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "sourcePostId": 123,
        "creator": {
          "id": 456,
          "username": "user1",
          "avatar": "avatar_url"
        },
        "title": "Vue.js 基础概念记忆卡片组",
        "description": "包含 Vue.js 的基础概念和语法",
        "state": 1,
        "updatedAt": "2023-12-01T10:00:00Z",
        "createdAt": "2023-11-30T10:00:00Z",
        "upvoteCount": 25,
        "cardCount": 15
      }
    ],
    "hasMore": true,
    "nextCursor": {
      "lastScore": 85.5,
      "lastId": 1
    }
  }
}
```

### 1.2 获取卡片组详情

**GET** `/decks/{deckId}`

获取指定卡片组的详细信息，包含卡片列表。

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deckId | Long | 是 | 卡片组ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "sourcePostId": 123,
    "creator": {
      "id": 456,
      "username": "user1"
    },
    "title": "Vue.js 基础概念记忆卡片组",
    "description": "包含 Vue.js 的基础概念和语法",
    "state": 1,
    "updatedAt": "2023-12-01T10:00:00Z",
    "createdAt": "2023-11-30T10:00:00Z",
    "upvoteCount": 25,
    "cardCount": 15,
    "cards": [
      {
        "id": 1,
        "front": "什么是 Vue.js?",
        "back": "Vue.js 是一个渐进式的 JavaScript 框架",
        "deck": {
          "id": 1,
          "title": "Vue.js 基础概念记忆卡片组"
        },
        "creator": {
          "id": 456,
          "username": "user1"
        },
        "srsState": {
          "id": 1,
          "reviewDueAt": "2023-12-02T10:00:00Z",
          "lastReviewedAt": "2023-12-01T10:00:00Z",
          "intervalDays": 1,
          "repetitions": 2,
          "lapseCount": 0
        }
      }
    ],
    "stats": {
      "totalCards": 15,
      "newCards": 5,
      "reviewCards": 8,
      "learnedCards": 2
    }
  }
}
```

### 1.3 创建卡片组

**POST** `/decks`

创建新的卡片组。

**请求体**:
```json
{
  "sourcePostId": 123,
  "title": "新卡片组标题",
  "description": "卡片组描述",
  "cards": [
    {
      "front": "问题1",
      "back": "答案1"
    },
    {
      "front": "问题2",
      "back": "答案2"
    }
  ]
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 1,
    "title": "新卡片组标题",
    "cardCount": 2
  }
}
```

### 1.4 更新卡片组

**PUT** `/decks/{deckId}`

更新卡片组信息。

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deckId | Long | 是 | 卡片组ID |

**请求体**:
```json
{
  "title": "更新后的标题",
  "description": "更新后的描述"
}
```

---

## 2. 卡片管理

### 2.1 创建卡片

**POST** `/cards`

在指定卡片组中创建新卡片。

**请求体**:
```json
{
  "deckId": 1,
  "front": "新问题",
  "back": "新答案"
}
```

### 2.2 更新卡片

**PUT** `/cards/{cardId}`

更新指定卡片内容。

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| cardId | Long | 是 | 卡片ID |

**请求体**:
```json
{
  "front": "更新后的问题",
  "back": "更新后的答案"
}
```

---

## 3. 记忆库管理

### 3.1 添加卡片组到记忆库

**POST** `/memory-bank/decks`

将卡片组添加到用户的记忆库中。

**请求体**:
```json
{
  "deckId": 1,
  "courseId": 123
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "添加成功",
  "data": {
    "addedCardCount": 15
  }
}
```

### 3.2 获取记忆库课程列表

**GET** `/memory-bank/courses`

获取用户记忆库中的课程列表及统计信息。

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | Integer | 否 | 课程状态筛选 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "course": {
        "id": 123,
        "name": "Vue.js 全栈开发",
        "description": "从零学习 Vue.js"
      },
      "setting": {
        "id": 1,
        "frequencySetting": 1,
        "status": 1
      },
      "cardCount": 50,
      "dueCardCount": 12,
      "newCardCount": 8,
      "reviewCardCount": 25,
      "learnedCardCount": 15
    }
  ]
}
```

### 3.3 更新课程复习策略

**PUT** `/memory-bank/courses/{courseId}/settings`

更新指定课程的复习策略。

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| courseId | Long | 是 | 课程ID |

**请求体**:
```json
{
  "frequencySetting": 0,
  "status": 1
}
```

### 3.4 移除卡片组

**DELETE** `/memory-bank/decks/{deckId}`

从记忆库中移除指定卡片组。

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deckId | Long | 是 | 卡片组ID |

---

## 4. 复习功能

### 4.1 获取复习队列

**GET** `/review/queue`

获取当前用户的复习队列。

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| dueOnly | Boolean | 否 | 只获取到期卡片，默认true |
| courseId | Long | 否 | 筛选特定课程 |
| limit | Integer | 否 | 返回数量限制，默认50 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "front": "什么是 Vue.js?",
      "back": "Vue.js 是一个渐进式的 JavaScript 框架",
      "deck": {
        "id": 1,
        "title": "Vue.js 基础概念"
      },
      "creator": {
        "id": 456,
        "username": "user1"
      },
      "srsState": {
        "id": 1,
        "reviewDueAt": "2023-12-01T10:00:00Z",
        "intervalDays": 1,
        "repetitions": 2,
        "lapseCount": 0
      }
    }
  ]
}
```

### 4.2 提交复习结果

**POST** `/review/submit`

提交单张卡片的复习结果。

**请求体**:
```json
{
  "cardId": 1,
  "result": 2,
  "timeSpent": 15
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "复习结果已提交",
  "data": {
    "nextReviewAt": "2023-12-03T10:00:00Z",
    "intervalDays": 2
  }
}
```

### 4.3 批量提交复习结果

**POST** `/review/batch-submit`

批量提交复习结果。

**请求体**:
```json
{
  "startTime": "2023-12-01T10:00:00Z",
  "endTime": "2023-12-01T10:30:00Z",
  "totalCards": 10,
  "reviewedCards": 8,
  "correctAnswers": 6,
  "results": [
    {
      "cardId": 1,
      "result": 2,
      "timeSpent": 15
    },
    {
      "cardId": 2,
      "result": 1,
      "timeSpent": 25
    }
  ]
}
```

### 4.4 获取复习统计

**GET** `/review/stats`

获取用户的复习统计信息。

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| period | String | 否 | 统计周期: day/week/month，默认week |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalReviews": 150,
    "streakDays": 7,
    "averageScore": 85.5,
    "timeSpent": 120
  }
}
```

---

## 5. 卡片组版本管理

### 5.1 获取卡片组更新对比

**GET** `/decks/{deckId}/diff`

获取卡片组的更新对比信息。

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deckId | Long | 是 | 卡片组ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "deckId": 1,
    "title": {
      "old": "旧标题",
      "newValue": "新标题"
    },
    "description": {
      "old": "旧描述",
      "newValue": "新描述"
    },
    "cardDiffs": [
      {
        "cardId": 1,
        "type": "modified",
        "oldVersion": {
          "front": "旧问题",
          "back": "旧答案"
        },
        "newVersion": {
          "front": "新问题",
          "back": "新答案"
        }
      },
      {
        "type": "added",
        "newVersion": {
          "front": "新增问题",
          "back": "新增答案"
        }
      }
    ],
    "summary": {
      "addedCount": 1,
      "modifiedCount": 1,
      "deletedCount": 0
    }
  }
}
```

### 5.2 接受卡片组更新

**POST** `/decks/{deckId}/accept-changes`

接受卡片组的更新内容。

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deckId | Long | 是 | 卡片组ID |

**请求体**:
```json
{
  "cardIds": [1, 2, 3]
}
```

---

## 6. 错误码定义

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权访问 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如重复添加） |
| 500 | 服务器内部错误 |

## 7. 通用响应格式

所有接口都遵循统一的响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2023-12-01T10:00:00Z"
}
```

## 8. 认证说明

所有接口都需要在请求头中携带 JWT Token：

```
Authorization: Bearer {token}
```

## 9. 限流规则

- 普通接口：每分钟 100 次请求
- 复习提交接口：每分钟 300 次请求
- 获取队列接口：每分钟 60 次请求

## 10. 分页说明

支持分页的接口统一使用以下参数：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | Integer | 1 | 页码 |
| size | Integer | 10 | 每页大小，最大100 |

分页响应格式：

```json
{
  "items": [],
  "totalCount": 100,
  "hasMore": true,
  "page": 1,
  "size": 10
}
```