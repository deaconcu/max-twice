# 记忆卡片组管理接口文档 (MemoryCardDeckController)

## 基本信息

- **Controller**: `MemoryCardDeckController.java`
- **基础路径**: `/api/v1/memory`
- **Rate Limit**: 50 requests/minute (per user)
- **前端 API**: `web/src/api/modules/memory.ts`

## 概述

记忆卡片组功能允许用户为帖子创建记忆卡片,支持版本管理、状态审核、点赞等功能。系统采用 SRS (Spaced Repetition System) 算法帮助用户高效记忆。

**核心特性**:
- 支持创建、查询、更新、删除卡片组
- 版本控制：支持卡片组内容更新和差异查看
- 状态管理：pending/normal/rejected/banned
- 游标分页：支持高效的分页查询
- AI 生成：支持使用 AI 自动生成卡片组

---

## DTO 类型说明

### CourseBriefDTO (课程简要信息)
用途：课程引用信息

```json
{
  "id": 100,
  "title": "Java编程入门"
}
```

**字段说明**：
- `id` (Long): 课程ID
- `title` (String): 课程标题

**使用场景**：
- 嵌套在卡片组信息中
- 显示卡片组所属课程

---

### NodeBriefDTO (节点简要信息)
用途：节点引用信息

```json
{
  "id": 789,
  "name": "面向对象"
}
```

**字段说明**：
- `id` (Long): 节点ID
- `name` (String): 节点名称

**使用场景**：
- 嵌套在卡片组信息中
- 显示卡片组所属节点

---

### UserBriefDTO (用户简要信息)
用途：用户引用信息

```json
{
  "id": 999,
  "name": "张三",
  "avatar": "https://example.com/avatar.jpg"
}
```

**字段说明**：
- `id` (Long): 用户ID
- `name` (String): 用户名
- `avatar` (String): 头像URL

**使用场景**：
- 显示卡片组创建者信息
- 用户署名展示

---

### CardContentDTO (卡片基础内容)
用途：卡片的正反面内容

```json
{
  "id": 1001,
  "front": "什么是类？",
  "back": "类是对象的模板"
}
```

**字段说明**：
- `id` (Long): 卡片ID
- `front` (String): 卡片正面内容（问题）
- `back` (String): 卡片背面内容（答案）

**使用场景**：
- 作为 CardWithSrsDTO 的基类
- 创建卡片时的输入数据

---

### CardWithSrsDTO (卡片含SRS状态)
用途：包含学习进度的卡片信息

**继承关系**: CardWithSrsDTO extends CardContentDTO

```json
{
  "id": 1001,
  "front": "什么是类？",
  "back": "类是对象的模板，定义了对象的属性和方法",
  "version": 1,
  "interval": 0,
  "easeFactor": 2.5,
  "nextReviewAt": "2024-01-01 10:00:00",
  "lastReviewedAt": null
}
```

**完整字段说明**：

**从 CardContentDTO 继承的字段**：
- `id`, `front`, `back`

**CardWithSrsDTO 特有字段**：
- `version` (Integer): 卡片版本号，用于追踪内容变更
- `interval` (Integer): 复习间隔（天），SRS算法使用
- `easeFactor` (Double): 难度系数，范围通常为 1.3-2.5
- `nextReviewAt` (String): 下次复习时间，格式：`yyyy-MM-dd HH:mm:ss`
- `lastReviewedAt` (String): 上次复习时间，首次学习时为 null

**使用场景**：
- 卡片组详情中的卡片列表
- 记忆库中的学习卡片

---

### DeckSummaryDTO (卡片组摘要)
用途：卡片组基础信息

```json
{
  "id": 123,
  "postId": 456,
  "nodeId": 789,
  "courseId": 100,
  "title": "Java基础知识点",
  "description": "涵盖Java核心概念的记忆卡片",
  "state": 1,
  "upvoteCount": 25,
  "cardCount": 15,
  "createdAt": "2024-01-01 10:00:00",
  "updatedAt": "2024-01-02 15:30:00",
  "course": {
    "id": 100,
    "title": "Java编程入门"
  },
  "node": {
    "id": 789,
    "name": "面向对象"
  }
}
```

**字段说明**：
- `id` (Long): 卡片组ID
- `postId` (Long): 源帖子ID，卡片组来源
- `nodeId` (Long): 节点ID
- `courseId` (Long): 课程ID
- `title` (String): 卡片组标题（继承自帖子标题）
- `description` (String): 卡片组描述
- `state` (Integer): 状态
  - `0`: pending（待审核）
  - `1`: normal（正常）
  - `2`: rejected（已拒绝）
  - `3`: banned（已封禁）
- `upvoteCount` (Integer): 点赞数
- `cardCount` (Integer): 卡片数量
- `createdAt` (String): 创建时间
- `updatedAt` (String): 更新时间
- `course` (CourseBriefDTO): 课程信息
- `node` (NodeBriefDTO): 节点信息

**使用场景**：
- 作为其他 DeckDTO 的基类
- 不需要创建者信息的场景

---

### DeckWithCreatorDTO (卡片组+创建者)
用途：包含创建者信息的卡片组

**继承关系**: DeckWithCreatorDTO extends DeckSummaryDTO

```json
{
  "id": 123,
  "postId": 456,
  "nodeId": 789,
  "courseId": 100,
  "title": "Java基础知识点",
  "description": "涵盖Java核心概念的记忆卡片",
  "state": 1,
  "upvoteCount": 25,
  "cardCount": 15,
  "createdAt": "2024-01-01 10:00:00",
  "updatedAt": "2024-01-02 15:30:00",
  "creator": {
    "id": 999,
    "name": "张三",
    "avatar": "https://example.com/avatar.jpg"
  },
  "course": {
    "id": 100,
    "title": "Java编程入门"
  },
  "node": {
    "id": 789,
    "name": "面向对象"
  }
}
```

**完整字段说明**：

**从 DeckSummaryDTO 继承的字段**：
- 所有基础字段

**DeckWithCreatorDTO 特有字段**：
- `creator` (UserBriefDTO): 创建者信息

**使用场景**：
- 卡片组列表（需要显示作者）
- 审核列表

---

### DeckWithVoteDTO (卡片组+点赞状态)
用途：包含用户点赞状态的卡片组

**继承关系**: DeckWithVoteDTO extends DeckWithCreatorDTO

```json
{
  "id": 123,
  "postId": 456,
  "nodeId": 789,
  "courseId": 100,
  "title": "Java基础知识点",
  "description": "涵盖Java核心概念的记忆卡片",
  "state": 1,
  "upvoteCount": 25,
  "cardCount": 15,
  "createdAt": "2024-01-01 10:00:00",
  "updatedAt": "2024-01-02 15:30:00",
  "creator": {
    "id": 999,
    "name": "张三",
    "avatar": "https://example.com/avatar.jpg"
  },
  "course": {
    "id": 100,
    "title": "Java编程入门"
  },
  "node": {
    "id": 789,
    "name": "面向对象"
  },
  "hasUpvoted": false
}
```

**完整字段说明**：

**从 DeckWithCreatorDTO 继承的字段**：
- 所有字段

**DeckWithVoteDTO 特有字段**：
- `hasUpvoted` (Boolean): 当前用户是否已点赞（需要登录）

**使用场景**：
- 公共卡片组列表（显示点赞状态）
- 需要交互的卡片组展示

---

### DeckDetailDTO (卡片组详情)
用途：包含完整卡片列表的卡片组

**继承关系**: DeckDetailDTO extends DeckWithCreatorDTO

```json
{
  "id": 123,
  "postId": 456,
  "title": "Java基础知识点",
  "description": "涵盖Java核心概念的记忆卡片",
  "state": 1,
  "upvoteCount": 25,
  "cardCount": 15,
  "creator": {
    "id": 999,
    "name": "张三",
    "avatar": "https://example.com/avatar.jpg"
  },
  "cards": [
    {
      "id": 1001,
      "front": "什么是类？",
      "back": "类是对象的模板",
      "version": 1,
      "interval": 0,
      "easeFactor": 2.5,
      "nextReviewAt": "2024-01-01 10:00:00",
      "lastReviewedAt": null
    }
  ]
}
```

**完整字段说明**：

**从 DeckWithCreatorDTO 继承的字段**：
- 所有字段

**DeckDetailDTO 特有字段**：
- `cards` (List<CardWithSrsDTO>): 卡片列表，包含完整的 SRS 学习状态

**使用场景**：
- 卡片组详情页
- 卡片组审核页面
- 学习页面

---

### KeysetPageResponse (游标分页响应)
用途：高效分页查询的响应结构

```json
{
  "items": [...],
  "lastScore": 25.0,
  "lastId": 123,
  "hasMore": true
}
```

**字段说明**：
- `items` (List<T>): 数据列表，泛型类型根据接口而定
- `lastScore` (Double): 最后一条记录的分数，用于按分数排序的分页
- `lastId` (Long): 最后一条记录的ID，用于分页游标
- `hasMore` (Boolean): 是否还有更多数据

**使用场景**：
- 所有支持分页的卡片组查询接口
- 替代传统的 offset 分页，性能更优

---

## 接口列表

## 1. 获取帖子的公共卡片组

**接口路径**: `GET /api/v1/memory/posts/{postId}/decks`

**是否需要登录**: 否

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| postId | Long | 是 | 帖子ID，必须大于0 |

**Query参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| sortBy | String | 否 | score | 排序方式：score=按分数降序，createdAt=按ID降序 |
| lastScore | Double | 否 | - | 游标分页：最后一条的分数（sortBy=score时使用） |
| lastId | Long | 否 | - | 游标分页：最后一条的ID |

**返回类型**: `KeysetPageResponse<DeckWithVoteDTO>`

**业务逻辑说明**:

### 查询流程
1. **参数验证**
   - 验证 `postId` 大于0

2. **状态过滤**
   - 只返回 `state=1` (normal) 的卡片组
   - 过滤掉待审核、已拒绝、已封禁的卡片组

3. **排序方式**
   - `sortBy=score`: 按分数降序（最高分在前）
   - `sortBy=createdAt`: 按ID降序（最新的在前）

4. **游标分页**
   - 固定每页返回20条
   - **按分数排序时**:
     - 首次查询不传 `lastScore` 和 `lastId`
     - 后续分页传递上次返回的 `lastScore` 和 `lastId`
   - **按时间排序时**:
     - 首次查询不传 `lastId`
     - 后续分页只传递上次返回的 `lastId`
     - 不需要传 `lastScore`

5. **点赞状态**
   - 如果用户已登录，填充 `hasUpvoted` 字段
   - 未登录时 `hasUpvoted` 为 false

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "timestamp": 1703001234567,
  "data": {
    "items": [
      {
        "id": 123,
        "postId": 456,
        "nodeId": 789,
        "courseId": 100,
        "title": "Java基础知识点",
        "description": "涵盖Java核心概念的记忆卡片",
        "state": 1,
        "upvoteCount": 25,
        "cardCount": 15,
        "createdAt": "2024-01-01 10:00:00",
        "updatedAt": "2024-01-02 15:30:00",
        "creator": {
          "id": 999,
          "name": "张三",
          "avatar": "https://example.com/avatar.jpg"
        },
        "course": {
          "id": 100,
          "title": "Java编程入门"
        },
        "node": {
          "id": 789,
          "name": "面向对象"
        },
        "hasUpvoted": false
      }
    ],
    "lastScore": 25.0,
    "lastId": 123,
    "hasMore": true
  }
}
```

**前端使用**:
- **文件**: `web/src/components/features/read/MemoryCardSidebar.vue`
- **场景**: 用户在帖子侧边栏查看公共卡片组列表

---

## 2. 获取帖子创建者的卡片组

**接口路径**: `GET /api/v1/memory/posts/{postId}/creator-deck`

**是否需要登录**: 是

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| postId | Long | 是 | 帖子ID |

**Query参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| lastId | Long | 否 | 游标分页：最后一条的ID |

**返回类型**: `KeysetPageResponse<DeckWithVoteDTO>`

**业务逻辑说明**:
- 获取帖子原作者创建的卡片组
- 固定每页返回20条
- 固定按ID降序（最新的在前）
- 如果当前用户是帖子作者，返回所有状态；否则只返回 normal 状态

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "items": [
      {
        "id": 124,
        "postId": 456,
        "nodeId": 789,
        "courseId": 100,
        "title": "作者原创卡片组",
        "description": "官方卡片组",
        "state": 1,
        "upvoteCount": 50,
        "cardCount": 20,
        "createdAt": "2024-01-01 10:00:00",
        "updatedAt": "2024-01-02 15:30:00",
        "creator": {
          "id": 888,
          "name": "帖子作者",
          "avatar": "https://example.com/avatar.jpg"
        },
        "course": {
          "id": 100,
          "title": "Java编程入门"
        },
        "node": {
          "id": 789,
          "name": "面向对象"
        },
        "hasUpvoted": true
      }
    ],
    "lastScore": 50.0,
    "lastId": 124,
    "hasMore": false
  }
}
```

**前端使用**:
- **文件**: `web/src/components/features/read/MemoryCardSidebar.vue`
- **场景**: 显示帖子作者创建的官方卡片组

---

## 3. 获取我的帖子卡片组

**接口路径**: `GET /api/v1/memory/posts/{postId}/my-deck`

**是否需要登录**: 是

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| postId | Long | 是 | 帖子ID |

**Query参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| sortBy | String | 否 | createdAt | 排序方式：score=按分数降序，createdAt=按ID降序 |
| lastScore | Double | 否 | - | 游标分页：最后一条的分数（sortBy=score时使用） |
| lastId | Long | 否 | - | 游标分页：最后一条的ID |

**返回类型**: `KeysetPageResponse<DeckWithVoteDTO>`

**业务逻辑说明**:
- 获取当前登录用户在该帖子下创建的卡片组
- 固定每页返回20条
- 包含所有状态（pending/normal/rejected/banned）
- 用户可以查看自己被拒绝的卡片组
- 默认按时间排序（最新的在前）

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "items": [
      {
        "id": 125,
        "postId": 456,
        "nodeId": 789,
        "courseId": 100,
        "title": "我的学习笔记",
        "description": "个人卡片组",
        "state": 0,
        "upvoteCount": 5,
        "cardCount": 10,
        "createdAt": "2024-01-01 10:00:00",
        "updatedAt": "2024-01-02 15:30:00",
        "creator": {
          "id": 777,
          "name": "当前用户",
          "avatar": "https://example.com/me.jpg"
        },
        "course": {
          "id": 100,
          "title": "Java编程入门"
        },
        "node": {
          "id": 789,
          "name": "面向对象"
        },
        "hasUpvoted": false
      }
    ],
    "lastId": 125,
    "hasMore": false
  }
}
```

**前端使用**:
- **文件**: `web/src/components/features/read/MemoryCardSidebar.vue`
- **场景**: 显示当前用户创建的卡片组

---

## 4. 获取当前用户所有卡片组

**接口路径**: `GET /api/v1/memory/users/me/memory-decks`

**是否需要登录**: 是

**Query参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| lastId | Long | 否 | 游标分页参数 |

**返回类型**: `KeysetPageResponse<DeckWithCreatorDTO>`

**业务逻辑说明**:
- 获取当前用户创建的所有卡片组
- 包含所有状态
- 按ID倒序（最新的在前）
- 固定每页返回20条

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "items": [
      {
        "id": 130,
        "postId": 500,
        "nodeId": 800,
        "courseId": 110,
        "title": "Python核心概念",
        "description": "Python学习要点",
        "state": 1,
        "upvoteCount": 15,
        "cardCount": 12,
        "createdAt": "2024-01-05 10:00:00",
        "updatedAt": "2024-01-06 11:00:00",
        "creator": {
          "id": 777,
          "name": "我",
          "avatar": "https://example.com/me.jpg"
        },
        "course": {
          "id": 110,
          "title": "Python入门"
        },
        "node": {
          "id": 800,
          "name": "基础语法"
        }
      }
    ],
    "lastId": 130,
    "hasMore": true
  }
}
```

**前端使用**:
- **文件**: `web/src/views/profile/ProfilePage.vue` → `MemoryDecksTab.vue`
- **场景**: 个人中心查看自己的所有卡片组

---

## 5. 获取指定用户卡片组

**接口路径**: `GET /api/v1/memory/users/{userId}/memory-decks`

**是否需要登录**: 是

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |

**Query参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| lastId | Long | 否 | 游标分页参数 |

**返回类型**: `KeysetPageResponse<DeckWithCreatorDTO>`

**业务逻辑说明**:
- 查看其他用户创建的卡片组
- 只返回 normal 状态的卡片组
- 按ID倒序
- 固定每页返回20条

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "items": [
      {
        "id": 135,
        "postId": 510,
        "nodeId": 820,
        "courseId": 120,
        "title": "JavaScript重点",
        "description": "JS核心知识",
        "state": 1,
        "upvoteCount": 30,
        "cardCount": 18,
        "createdAt": "2024-01-03 09:00:00",
        "updatedAt": "2024-01-04 10:00:00",
        "creator": {
          "id": 888,
          "name": "李四",
          "avatar": "https://example.com/user.jpg"
        },
        "course": {
          "id": 120,
          "title": "JavaScript全栈"
        },
        "node": {
          "id": 820,
          "name": "ES6特性"
        }
      }
    ],
    "lastId": 135,
    "hasMore": false
  }
}
```

**前端使用**:
- **文件**: `web/src/views/profile/ProfilePage.vue` → `MemoryDecksTab.vue`
- **场景**: 查看其他用户的卡片组列表

---

## 6. 获取卡片组详情

**接口路径**: `GET /api/v1/memory/decks/{deckId}`

**是否需要登录**: 是

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| deckId | Long | 是 | 卡片组ID |

**返回类型**: `DeckDetailDTO`

**业务逻辑说明**:
- 获取卡片组的完整信息
- 包含所有卡片及其SRS状态
- 任何登录用户都可以查看

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 123,
    "postId": 456,
    "title": "Java基础知识点",
    "description": "涵盖Java核心概念的记忆卡片",
    "state": 1,
    "upvoteCount": 25,
    "cardCount": 15,
    "creator": {
      "id": 999,
      "name": "张三",
      "avatar": "https://example.com/avatar.jpg"
    },
    "course": {
      "id": 100,
      "title": "Java编程入门"
    },
    "node": {
      "id": 789,
      "name": "面向对象"
    },
    "cards": [
      {
        "id": 1001,
        "front": "什么是类？",
        "back": "类是对象的模板",
        "version": 1,
        "interval": 0,
        "easeFactor": 2.5,
        "nextReviewAt": "2024-01-01 10:00:00",
        "lastReviewedAt": null
      }
    ]
  }
}
```

**前端使用**:
- **文件**: `web/src/components/features/read/DeckDetailDialog.vue`
- **场景**: 查看卡片组详情和所有卡片

---

## 7. 获取节点下的卡片组

**接口路径**: `GET /api/v1/memory/decks/node/{nodeId}`

**是否需要登录**: 是

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| nodeId | Long | 是 | 节点ID |

**Query参数**:
| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| lastScore | Double | - | 游标分页参数 |
| lastId | Long | - | 游标分页参数 |
| limit | Integer | 20 | 每页数量 |

**返回类型**: `KeysetPageResponse<DeckWithCreatorDTO>`

**业务逻辑说明**:
- 获取指定节点下的所有卡片组
- 只返回 normal 状态
- 按评分或创建时间排序

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "items": [
      {
        "id": 150,
        "postId": 600,
        "nodeId": 789,
        "courseId": 100,
        "title": "面向对象核心概念",
        "description": "OOP精髓",
        "state": 1,
        "upvoteCount": 40,
        "cardCount": 20,
        "createdAt": "2024-01-02 14:00:00",
        "updatedAt": "2024-01-03 16:00:00",
        "creator": {
          "id": 888,
          "name": "专家",
          "avatar": "https://example.com/expert.jpg"
        },
        "course": {
          "id": 100,
          "title": "Java编程入门"
        },
        "node": {
          "id": 789,
          "name": "面向对象"
        }
      }
    ],
    "lastScore": 40.0,
    "lastId": 150,
    "hasMore": true
  }
}
```

**前端使用**:
- **文件**: `web/src/components/features/read/MemoryCardList.vue`
- **场景**: 加载节点下的所有卡片组

---

## 8. 创建卡片组

**接口路径**: `POST /api/v1/memory/decks`

**是否需要登录**: 是

**请求体**:
```json
{
  "sourcePostId": 456,
  "description": "这是一组Java基础知识卡片",
  "cards": [
    {
      "front": "什么是类？",
      "back": "类是对象的模板"
    },
    {
      "front": "什么是继承？",
      "back": "继承是面向对象的重要特性"
    }
  ]
}
```

**请求参数说明**:
- `sourcePostId` (Long, 必填): 源帖子ID
- `description` (String, 可选): 卡片组描述
- `cards` (List, 必填): 卡片列表，至少1张
  - `front` (String, 必填): 卡片正面
  - `back` (String, 必填): 卡片背面

**返回类型**: `Void`

**业务逻辑说明**:

### 创建流程
1. **参数验证**
   - 验证 sourcePostId 有效
   - 验证至少有1张卡片
   - 验证 front/back 不为空

2. **自动继承信息**
   - 从源帖子获取 nodeId, courseId, title
   - description 为用户输入，可选

3. **初始状态**
   - 新建卡片组 state=0 (pending)
   - 需要管理员审核

4. **卡片初始化**
   - 每张卡片的 version=1
   - SRS 参数使用默认值

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "timestamp": 1703001234567
}
```

**前端使用**:
- **文件**: `web/src/components/features/read/CreateDeckDialog.vue`
- **场景**: 创建新的记忆卡片组

---

## 9. 更新卡片组元信息

**接口路径**: `PUT /api/v1/memory/decks/{deckId}`

**是否需要登录**: 是

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| deckId | Long | 是 | 卡片组ID |

**请求体**:
```json
{
  "description": "更新后的描述"
}
```

**请求参数说明**:
- `description` (String, 可选): 新描述，最大长度1000字符

**返回类型**: `Void`

**业务逻辑说明**:
- 只能更新描述，不允许更新标题
- 只有创建者可以更新

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "timestamp": 1703001234567
}
```

**前端使用**:
- **状态**: 未在前端使用
- **说明**: 前端未定义该接口

---

## 10. 获取卡片组更新差异

**接口路径**: `GET /api/v1/memory/decks/{deckId}/diff`

**是否需要登录**: 是

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| deckId | Long | 是 | 卡片组ID |

**Query参数**:
| 参数名 | 类型 | 说明 |
|--------|------|------|
| userCurrentVersion | Integer | 用户当前持有的版本号 |

**返回类型**: `DeckDiffDTO`

**业务逻辑说明**:
- 对比用户版本与最新版本
- 返回新增、修改、删除的卡片
- 用户可选择性接受更新

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "hasUpdates": true,
    "currentVersion": 3,
    "userVersion": 1,
    "addedCards": [
      {
        "id": 1005,
        "front": "什么是多态？",
        "back": "多态是指同一操作作用于不同对象可以有不同的解释",
        "version": 2
      }
    ],
    "modifiedCards": [
      {
        "id": 1002,
        "front": "什么是封装？",
        "back": "封装是将数据和操作数据的方法绑定在一起（更新后）",
        "version": 3
      }
    ],
    "deletedCardIds": [1003]
  }
}
```

**前端使用**:
- **文件**: API定义存在于 `web/src/api/modules/memory.ts`
- **状态**: 暂未在组件中使用
- **说明**: 待版本更新功能实现

---

## 11. 接受卡片组更新

**接口路径**: `POST /api/v1/memory/decks/{deckId}/accept-changes`

**是否需要登录**: 是

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| deckId | Long | 是 | 卡片组ID |

**请求体**:
```json
[1005, 1002]
```

**请求参数说明**:
- 卡片ID数组，表示要接受的卡片更新

**返回类型**: `Void`

**业务逻辑说明**:
- 用户选择接受哪些卡片的更新
- 接受后版本号更新
- SRS 状态相应调整

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "timestamp": 1703001234567
}
```

**前端使用**:
- **文件**: `web/src/components/features/read/DeckDetailDialog.vue`
- **场景**: 接受卡片组的更新变化

---

## 12. 替换所有卡片

**接口路径**: `PUT /api/v1/memory/decks/{deckId}/cards`

**是否需要登录**: 是

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| deckId | Long | 是 | 卡片组ID |

**请求体**:
```json
{
  "sourcePostId": 456,
  "description": "完全重写的卡片组",
  "cards": [
    {
      "front": "新问题1",
      "back": "新答案1"
    },
    {
      "front": "新问题2",
      "back": "新答案2"
    }
  ]
}
```

**返回类型**: `Void`

**业务逻辑说明**:
- 删除所有旧卡片，创建新卡片
- 版本号+1
- 只有创建者可以操作
- 已添加到记忆库的用户会收到更新通知

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "timestamp": 1703001234567
}
```

**前端使用**:
- **状态**: 未在前端使用
- **说明**: 前端未定义该接口

---

## 13. AI生成卡片组

**接口路径**: `POST /api/v1/memory/decks/{postId}/ai-generate`

**是否需要登录**: 是

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| postId | Long | 是 | 帖子ID |

**返回类型**: `Void`

**业务逻辑说明**:
- AI根据帖子内容自动生成卡片组
- 异步处理，接口立即返回
- 生成的卡片组初始状态为 pending

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "timestamp": 1703001234567
}
```

**前端使用**:
- **状态**: 未在前端使用
- **说明**: 前端未定义该接口

---

## 14. 删除卡片组

**接口路径**: `DELETE /api/v1/memory/decks/{id}`

**是否需要登录**: 是

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 卡片组ID |

**返回类型**: `Void`

**业务逻辑说明**:
- 软删除，不会真正删除数据
- 只有创建者或管理员可以删除
- 已添加到记忆库的用户仍可继续复习

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "timestamp": 1703001234567
}
```

**前端使用**:
- **文件**: `web/src/views/profile/ProfilePage.vue` → `MemoryDecksTab.vue`
- **场景**: 删除自己创建的卡片组

---

## 卡片组状态说明

| state | 名称 | 说明 |
|-------|------|------|
| 0 | pending | 待审核，用户刚创建 |
| 1 | normal | 正常，已通过审核 |
| 2 | rejected | 已拒绝，未通过审核 |
| 3 | banned | 已封禁，包含违规内容 |

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 1002 | 参数异常 |
| 1006 | 权限不足 |
| 1101 | 用户未登录 |
| 2201 | 卡片组不存在 |
| 2206 | 卡片组已存在于记忆库中 |
| 2301 | 访问过于频繁，请稍后再试 |
| 9005 | AI服务请求失败 |

---

## 业务流程说明

### 场景1: 用户创建卡片组
1. 用户在帖子下阅读内容
2. 创建记忆卡片组（POST /memory/decks）
3. 卡片组进入 pending 状态
4. 管理员审核后变为 normal 状态
5. 其他用户可以看到并添加到记忆库

### 场景2: 查看帖子的卡片组
1. 访问帖子详情页
2. 获取帖子的公共卡片组（GET /posts/{postId}/decks）
3. 获取作者的卡片组（GET /posts/{postId}/creator-deck）
4. 获取自己的卡片组（GET /posts/{postId}/my-deck）
5. 展示所有卡片组供用户选择

### 场景3: 卡片组版本更新
1. 创建者更新了卡片组内容（PUT /decks/{id}/cards）
2. 已添加到记忆库的用户收到更新通知
3. 用户查看更新差异（GET /decks/{id}/diff）
4. 用户选择接受部分或全部更新（POST /decks/{id}/accept-changes）
5. 用户记忆库中的卡片同步更新

---

## 注意事项

1. **游标分页**:
   - 使用 lastScore + lastId 进行游标分页
   - 比传统 offset 分页性能更好
   - 需要同时传递两个参数才能正确分页

2. **状态管理**:
   - 用户只能看到自己所有状态的卡片组
   - 其他用户只能看到 normal 状态的卡片组
   - 管理员可以查看所有状态

3. **版本控制**:
   - 卡片组内容更新会增加版本号
   - 用户可以选择性接受更新
   - 不影响已学习的卡片的SRS状态

4. **权限控制**:
   - 创建、更新、删除：仅创建者
   - 查看详情：登录用户
   - 审核：管理员

---

## 前端集成总结

### 已使用的接口 (11个)

| 接口 | 前端文件 | 使用场景 |
|------|---------|----------|
| GET /posts/{postId}/decks | `MemoryCardSidebar.vue` | 加载帖子的公共卡片组列表 |
| GET /posts/{postId}/creator-deck | `MemoryCardSidebar.vue` | 加载帖子作者创建的卡片组 |
| GET /posts/{postId}/my-deck | `MemoryCardSidebar.vue` | 加载当前用户创建的卡片组 |
| GET /decks/{deckId} | `DeckDetailDialog.vue` | 查看卡片组详情和所有卡片 |
| GET /decks/node/{nodeId} | `MemoryCardList.vue` | 加载指定节点下的所有卡片组 |
| POST /decks | `CreateDeckDialog.vue` | 创建新的记忆卡片组 |
| POST /decks/{deckId}/accept-changes | `DeckDetailDialog.vue` | 接受卡片组的更新变化 |
| GET /users/me/memory-decks | `MemoryDecksTab.vue` | 个人中心查看自己的所有卡片组 |
| GET /users/{userId}/memory-decks | `MemoryDecksTab.vue` | 查看其他用户的卡片组列表 |
| DELETE /decks/{id} | `MemoryDecksTab.vue` | 删除自己创建的卡片组 |
| GET /decks/{deckId}/diff | API定义存在 | 暂未使用（待版本更新功能实现） |

### 未使用的接口 (3个)

1. **PUT /decks/{deckId}** - 更新卡片组元信息
   - 用途: 修改标题和描述
   - 状态: 前端未定义该接口

2. **PUT /decks/{deckId}/cards** - 替换所有卡片
   - 用途: 整体重写卡片组内容
   - 状态: 前端未定义该接口

3. **POST /decks/{postId}/ai-generate** - AI生成卡片组
   - 用途: 使用AI自动生成记忆卡片
   - 状态: 前端未定义该接口

### 管理后台接口

卡片组审核功能已整合到统一的管理后台接口:
- **接口**: `GET /api/v1/admin/contents/memory_card_deck?state=xxx&lastId=xxx`
- **Controller**: `AdminContentsController`
- **说明**: 统一管理所有内容类型的审核,包括 post, roadmap, memory_card_deck, comment, course, profession, node

---

*此文档用于指导前后端开发和接口对接，确保数据结构和业务逻辑的一致性*
