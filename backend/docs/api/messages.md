# 消息管理接口文档

## 接口概览

| 接口 | 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|------|
| 按分类获取消息 | GET | `/api/v1/messages/category` | 是 | 按消息分类获取消息列表（互动/系统/私信），支持按类型过滤 |
| ~~获取消息列表~~ | ~~GET~~ | ~~/api/v1/messages~~ | ~~是~~ | ~~已注释，待私信功能开发时启用~~ |
| ~~发送系统消息~~ | ~~POST~~ | ~~/api/v1/messages/system~~ | ~~否~~ | ~~已注释，待管理后台开发时启用~~ |
| 邀请用户 | POST | `/api/v1/messages/invite` | 是 | 邀请用户查看节点内容 |

---

## 1. 按分类获取消息

### 接口信息
- **路径**: `GET /api/v1/messages/category`
- **认证**: 需要登录
- **限流**: 60次/分钟 (按用户)

### 请求参数

**Query Parameters**:
```
GET /api/v1/messages/category?category=1&lastId=0&type=2
```

| 参数 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| category | Integer | 是 | @NotNull, @Min(1), @Max(3) | 消息分类：1=互动消息, 2=系统消息, 3=私信 |
| lastId | Long | 否 | - | 最后一条消息ID，首次查询可不传 |
| type | Integer | 否 | - | 可选的消息类型过滤 |

### 请求头
```
token: your-auth-token
```

### 响应示例

#### 互动消息响应格式 (category=1)

**成功 (200) - 关注消息 (type=2)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 123,
      "type": 2,
      "createdAt": "2024-01-01 00:00:00",
      "follower": {
        "id": 789,
        "name": "李四",
        "avatar": "https://example.com/avatar2.jpg",
        "biography": "前端开发工程师"
      }
    }
  ]
}
```

**成功 (200) - 点赞消息 (type=3)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 125,
      "type": 3,
      "createdAt": "2024-01-01 00:02:00",
      "objectId": 456,
      "objectType": 1,
      "voteType": 1,
      "upvoter": {
        "id": 789,
        "name": "李四",
        "avatar": "https://example.com/avatar2.jpg",
        "biography": "前端开发工程师"
      },
      "node": {
        "id": 123,
        "name": "Java基础",
        "courseId": 100,
        "courseName": "Java编程入门"
      }
    }
  ]
}
```

**成功 (200) - 邀请消息 (type=4)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 126,
      "type": 4,
      "createdAt": "2024-01-01 00:03:00",
      "inviter": {
        "id": 789,
        "name": "李四",
        "avatar": "https://example.com/avatar2.jpg",
        "biography": "前端开发工程师"
      },
      "node": {
        "id": 123,
        "name": "Java基础",
        "courseId": 100,
        "courseName": "Java编程入门"
      }
    }
  ]
}
```

**成功 (200) - 节点评论消息 (type=5)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 127,
      "type": 5,
      "createdAt": "2024-01-01 00:04:00",
      "commentId": 456,
      "commenter": {
        "id": 789,
        "name": "李四",
        "avatar": "https://example.com/avatar2.jpg",
        "biography": "前端开发工程师"
      },
      "node": {
        "id": 123,
        "name": "Java基础",
        "courseId": 100,
        "courseName": "Java编程入门"
      }
    }
  ]
}
```

**成功 (200) - 帖子评论消息 (type=6)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 128,
      "type": 6,
      "createdAt": "2024-01-01 00:05:00",
      "commentId": 457,
      "commenter": {
        "id": 789,
        "name": "李四",
        "avatar": "https://example.com/avatar2.jpg",
        "biography": "前端开发工程师"
      },
      "node": {
        "id": 123,
        "name": "Java基础",
        "courseId": 100,
        "courseName": "Java编程入门"
      }
    }
  ]
}
```

**成功 (200) - 回复节点评论消息 (type=7)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 129,
      "type": 7,
      "createdAt": "2024-01-01 00:06:00",
      "commentId": 458,
      "commenter": {
        "id": 789,
        "name": "李四",
        "avatar": "https://example.com/avatar2.jpg",
        "biography": "前端开发工程师"
      },
      "node": {
        "id": 123,
        "name": "Java基础",
        "courseId": 100,
        "courseName": "Java编程入门"
      }
    }
  ]
}
```

**成功 (200) - 回复帖子评论消息 (type=8)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 130,
      "type": 8,
      "createdAt": "2024-01-01 00:07:00",
      "commentId": 459,
      "commenter": {
        "id": 789,
        "name": "李四",
        "avatar": "https://example.com/avatar2.jpg",
        "biography": "前端开发工程师"
      },
      "node": {
        "id": 123,
        "name": "Java基础",
        "courseId": 100,
        "courseName": "Java编程入门"
      }
    }
  ]
}
```

**成功 (200) - 回复路线图评论消息 (type=9)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 131,
      "type": 9,
      "createdAt": "2024-01-01 00:08:00",
      "commentId": 460,
      "commenter": {
        "id": 789,
        "name": "李四",
        "avatar": "https://example.com/avatar2.jpg",
        "biography": "前端开发工程师"
      },
      "node": {
        "id": 123,
        "name": "Java基础",
        "courseId": 100,
        "courseName": "Java编程入门"
      }
    }
  ]
}
```

**成功 (200) - 路线图评论消息 (type=10)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 132,
      "type": 10,
      "createdAt": "2024-01-01 00:09:00",
      "commentId": 461,
      "commenter": {
        "id": 789,
        "name": "李四",
        "avatar": "https://example.com/avatar2.jpg",
        "biography": "前端开发工程师"
      },
      "node": {
        "id": 123,
        "name": "Java基础",
        "courseId": 100,
        "courseName": "Java编程入门"
      }
    }
  ]
}
```

#### 系统消息响应格式 (category=2)

**成功 (200) - 课程被拒绝 (type=11)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 201,
      "type": 11,
      "createdAt": "2024-01-01 10:00:00",
      "content": "{\"courseId\":100,\"courseName\":\"Java编程入门\",\"reason\":\"课程内容不符合平台规范\"}"
    }
  ]
}
```

**成功 (200) - 课程被封禁 (type=12)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 202,
      "type": 12,
      "createdAt": "2024-01-01 10:05:00",
      "content": "{\"courseId\":101,\"courseName\":\"Python入门\",\"reason\":\"包含违规内容\"}"
    }
  ]
}
```

**成功 (200) - 帖子被拒绝 (type=13)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 203,
      "type": 13,
      "createdAt": "2024-01-01 10:10:00",
      "content": "{\"postId\":456,\"postPreview\":\"这是一篇关于...\",\"nodeId\":123,\"nodeName\":\"Java基础\",\"courseName\":\"Java编程入门\",\"reason\":\"内容质量不达标\",\"linkUrl\":\"/self?tab=posts\"}"
    }
  ]
}
```

**成功 (200) - 帖子被封禁 (type=14)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 204,
      "type": 14,
      "createdAt": "2024-01-01 10:15:00",
      "content": "{\"postId\":457,\"postPreview\":\"这是一篇...\",\"nodeId\":124,\"nodeName\":\"Python基础\",\"courseName\":\"Python入门\",\"reason\":\"包含敏感信息\",\"linkUrl\":\"/self?tab=posts\"}"
    }
  ]
}
```

**成功 (200) - 评论被拒绝 (type=15)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 205,
      "type": 15,
      "createdAt": "2024-01-01 10:20:00",
      "content": "{\"commentId\":789,\"commentPreview\":\"我觉得...\",\"objectType\":\"post\",\"objectId\":456,\"objectTitle\":\"如何学习Java\",\"reason\":\"评论内容不当\"}"
    }
  ]
}
```

**成功 (200) - 评论被封禁 (type=16)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 206,
      "type": 16,
      "createdAt": "2024-01-01 10:25:00",
      "content": "{\"commentId\":790,\"commentPreview\":\"这个...\",\"objectType\":\"post\",\"objectId\":457,\"objectTitle\":\"Python学习路线\",\"reason\":\"包含广告信息\"}"
    }
  ]
}
```

**成功 (200) - 职业被拒绝 (type=17)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 207,
      "type": 17,
      "createdAt": "2024-01-01 10:30:00",
      "content": "{\"professionId\":10,\"professionName\":\"全栈开发工程师\",\"reason\":\"职业定义不清晰\"}"
    }
  ]
}
```

**成功 (200) - 职业被封禁 (type=18)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 208,
      "type": 18,
      "createdAt": "2024-01-01 10:35:00",
      "content": "{\"professionId\":11,\"professionName\":\"区块链专家\",\"reason\":\"包含误导性信息\"}"
    }
  ]
}
```

**成功 (200) - 路线图被拒绝 (type=19)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 209,
      "type": 19,
      "createdAt": "2024-01-01 10:40:00",
      "content": "{\"roadmapId\":50,\"professionId\":10,\"professionName\":\"全栈开发工程师\",\"reason\":\"路线图结构不完整\",\"linkUrl\":\"/self?tab=roadmaps\"}"
    }
  ]
}
```

**成功 (200) - 路线图被封禁 (type=20)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 210,
      "type": 20,
      "createdAt": "2024-01-01 10:45:00",
      "content": "{\"roadmapId\":51,\"professionId\":11,\"professionName\":\"AI工程师\",\"reason\":\"包含错误引导\",\"linkUrl\":\"/self?tab=roadmaps\"}"
    }
  ]
}
```

**成功 (200) - 记忆卡片组被拒绝 (type=21)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 211,
      "type": 21,
      "createdAt": "2024-01-01 10:50:00",
      "content": "{\"deckId\":300,\"deckTitle\":\"Java核心知识点\",\"postId\":456,\"postTitle\":\"Java学习笔记\",\"reason\":\"卡片内容质量不达标\",\"linkUrl\":\"/self?tab=memory-decks\"}"
    }
  ]
}
```

**成功 (200) - 记忆卡片组被封禁 (type=22)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 212,
      "type": 22,
      "createdAt": "2024-01-01 10:55:00",
      "content": "{\"deckId\":301,\"deckTitle\":\"Python速成\",\"postId\":457,\"postTitle\":\"Python快速入门\",\"reason\":\"包含抄袭内容\",\"linkUrl\":\"/self?tab=memory-decks\"}"
    }
  ]
}
```

**成功 (200) - 节点被拒绝 (type=23)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 213,
      "type": 23,
      "createdAt": "2024-01-01 11:00:00",
      "content": "{\"nodeId\":123,\"nodeName\":\"高级特性\",\"courseId\":100,\"courseName\":\"Java编程入门\",\"reason\":\"节点定义不清晰\"}"
    }
  ]
}
```

**成功 (200) - 节点被封禁 (type=24)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 214,
      "type": 24,
      "createdAt": "2024-01-01 11:05:00",
      "content": "{\"nodeId\":124,\"nodeName\":\"实战项目\",\"courseId\":101,\"courseName\":\"Python入门\",\"reason\":\"包含版权问题\"}"
    }
  ]
}
```

**成功 (200) - 课程审核通过 (type=25)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 215,
      "type": 25,
      "createdAt": "2024-01-01 11:10:00",
      "content": "{\"courseId\":102,\"courseName\":\"JavaScript全栈开发\",\"linkUrl\":\"/read?courseId=102\"}"
    }
  ]
}
```

**成功 (200) - 职业审核通过 (type=26)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 216,
      "type": 26,
      "createdAt": "2024-01-01 11:15:00",
      "content": "{\"professionId\":12,\"professionName\":\"前端开发工程师\",\"linkUrl\":\"/roadmap/12\"}"
    }
  ]
}
```

### 消息分类

| category | 名称 | 说明 |
|----------|------|------|
| 1 | 互动消息 | 关注、点赞、评论、邀请等用户互动产生的消息 |
| 2 | 系统消息 | 审核结果、课程申请等系统通知 |
| 3 | 私信 | 用户之间的私信对话 |

### 互动消息类型 (category=1)

| type | 名称 | 说明 | 字段 |
|------|------|------|------|
| 2 | follow | 关注通知 | follower (关注者信息) |
| 3 | upvote | 点赞通知 | upvoter (点赞者), node, objectId, objectType, voteType |
| 4 | invite | 邀请通知 | inviter (邀请者), node |
| 5 | nodeComment | 节点评论通知 | commenter (评论者), node, commentId |
| 6 | postComment | 帖子评论通知 | commenter (评论者), node, commentId |
| 7 | replyNodeComment | 回复节点评论通知 | commenter (评论者), node, commentId |
| 8 | replyPostingComment | 回复帖子评论通知 | commenter (评论者), node, commentId |
| 9 | replyRoadmapComment | 回复路线图评论通知 | commenter (评论者), node, commentId |
| 10 | roadmapComment | 路线图评论通知 | commenter (评论者), node, commentId |

### 系统消息类型 (category=2)

| type | 名称 | 说明 | content 字段 (JSON) |
|------|------|------|---------------------|
| 1 | applyCourse | 课程申请 (已废弃) | title, summary, explanation, parentId |
| 11 | courseRejected | 课程被拒绝 | courseId, courseName, reason |
| 12 | courseBanned | 课程被封禁 | courseId, courseName, reason |
| 13 | postRejected | 帖子被拒绝 | postId, postPreview, nodeId, nodeName, courseName, reason, linkUrl |
| 14 | postBanned | 帖子被封禁 | postId, postPreview, nodeId, nodeName, courseName, reason, linkUrl |
| 15 | commentRejected | 评论被拒绝 | commentId, commentPreview, objectType, objectId, objectTitle, reason |
| 16 | commentBanned | 评论被封禁 | commentId, commentPreview, objectType, objectId, objectTitle, reason |
| 17 | professionRejected | 职业被拒绝 | professionId, professionName, reason |
| 18 | professionBanned | 职业被封禁 | professionId, professionName, reason |
| 19 | roadmapRejected | 路线图被拒绝 | roadmapId, professionId, professionName, reason, linkUrl |
| 20 | roadmapBanned | 路线图被封禁 | roadmapId, professionId, professionName, reason, linkUrl |
| 21 | memoryDeckRejected | 记忆卡片组被拒绝 | deckId, deckTitle, postId, postTitle, reason, linkUrl |
| 22 | memoryDeckBanned | 记忆卡片组被封禁 | deckId, deckTitle, postId, postTitle, reason, linkUrl |
| 23 | nodeRejected | 节点被拒绝 | nodeId, nodeName, courseId, courseName, reason |
| 24 | nodeBanned | 节点被封禁 | nodeId, nodeName, courseId, courseName, reason |
| 25 | courseApproved | 课程审核通过 | courseId, courseName, linkUrl |
| 26 | professionApproved | 职业审核通过 | professionId, professionName, linkUrl |
| 99 | system | 其他系统消息 | 自定义内容 |

### 业务说明

1. **互动消息结构** (category=1):
   - 不同类型的消息返回不同的DTO结构，后端会根据type自动转换
   - **关注消息 (type=2)**:
     - DTO: `FollowMessageDTO`
     - 包含字段: `follower` (关注者用户信息)
   - **点赞消息 (type=3)**:
     - DTO: `UpvoteMessageDTO`
     - 包含字段: `upvoter` (点赞者), `node` (节点信息), `objectId` (被点赞对象ID), `objectType` (1=帖子, 2=评论), `voteType` (点赞类型)
   - **邀请消息 (type=4)**:
     - DTO: `InviteMessageDTO`
     - 包含字段: `inviter` (邀请者), `node` (节点信息)
   - **评论消息 (type=5-10)**:
     - DTO: `CommentMessageDTO`
     - 包含字段: `commenter` (评论者), `node` (节点信息), `commentId` (评论ID)
     - 类型包括: 节点评论、帖子评论、回复节点评论、回复帖子评论、回复路线图评论、路线图评论

2. **系统消息结构** (category=2):
   - 系统消息使用基础 `MessageDTO`，`content` 字段为 JSON 字符串
   - 前端需要根据 `type` 解析 `content` 字段
   - **审核拒绝消息 (type=11,13,15,17,19,21,23)**:
     - 包含 `reason` 字段说明拒绝原因
     - 部分包含 `linkUrl` 字段指向相关页面
   - **审核封禁消息 (type=12,14,16,18,20,22,24)**:
     - 包含 `reason` 字段说明封禁原因
     - 部分包含 `linkUrl` 字段指向相关页面
   - **审核通过消息 (type=25,26)**:
     - 包含 `linkUrl` 字段，可直接跳转到已通过的内容
   - **系统消息 senderId 为 0**，sender 字段为 null

3. **分页**:
   - 使用 `lastId` 进行游标分页
   - 每页默认返回20条
   - 首次查询可不传 `lastId`，或传 0
   - 按消息ID倒序排列（最新的在前）

4. **类型过滤**:
   - 如果传入 `type` 参数，只返回该类型的消息
   - 不传 `type` 则返回该分类下的所有消息
   - 例如查询所有系统消息: `?category=2`
   - 例如只查询课程审核通过消息: `?category=2&type=25`

---

<!-- 已注释接口 (2025/12/29 待私信功能开发时启用)
## 2. 获取消息列表

### 接口信息
- **路径**: `GET /api/v1/messages`
- **认证**: 需要登录
- **限流**: 60次/分钟 (按用户)

### 请求参数

**Query Parameters**:
```
GET /api/v1/messages?userId=456&type=1&lastId=0&conversation=1
```

| 参数 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| userId | Long | 是 | @NotNull, @Positive | 对方用户ID，必须大于0 |
| type | Integer | 是 | @NotNull, @Positive | 消息类型，必须大于0 |
| lastId | Long | 是 | @NotNull, @Min(0) | 最后一条消息ID，首次查询传0 |
| conversation | Integer | 是 | @NotNull, @Min(0) | 会话类型，0=单向, 1=双向对话 |

### 请求头
```
token: your-auth-token
```

### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1703001234567,
  "data": [
    {
      "id": 123,
      "senderId": 456,
      "sender": {
        "id": 456,
        "name": "张三",
        "avatar": "https://example.com/avatar.jpg",
        "biography": "Java开发工程师"
      },
      "receiverId": 789,
      "receiver": {
        "id": 789,
        "name": "李四",
        "avatar": "https://example.com/avatar2.jpg",
        "biography": "前端开发工程师"
      },
      "content": "你好，想请教一个问题",
      "type": 1,
      "createdAt": "2024-01-01 00:00:00"
    }
  ]
}
```

### 业务说明

1. **会话模式**:
   - `conversation = 0`: 单向查询，只查询发送给当前用户的消息
   - `conversation = 1`: 双向对话，查询当前用户与指定用户的所有往来消息

2. **分页**:
   - 使用 `lastId` 进行游标分页
   - 每页默认返回20条
   - 按时间倒序排列（最新的在前）

-->

---

<!-- 已注释接口 (2025/12/29 待管理后台开发时启用)
## 2. 发送系统消息

### 接口信息
- **路径**: `POST /api/v1/messages/system`
- **认证**: 不需要（管理后台使用）
- **限流**: 60次/分钟 (按用户)

### 请求参数

**Body (JSON)**:
```json
{
  "type": 1,
  "userId": 456,
  "content": "您的课程申请已通过审核"
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| type | Integer | 是 | @NotNull | 消息类型 |
| userId | Long | 是 | @NotNull | 接收用户ID |
| content | String | 是 | @NotBlank, @ConfigurableSize | 消息内容，长度可配置 |

### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "发送成功",
  "timestamp": 1703001234567
}
```

### 业务说明

1. **使用场景**:
   - 管理后台发送系统通知
   - 批量通知用户
   - 目前该接口内部实现已被注释，需要进一步开发

2. **内容长度**:
   - 使用 `@ConfigurableSize(configKey = "message-content")` 配置长度限制
   - 具体长度由系统配置决定

-->

---

## 2. 邀请用户

### 接口信息
- **路径**: `POST /api/v1/messages/invite`
- **认证**: 需要登录
- **限流**: 60次/分钟 (按用户)

### 请求参数

**Body (JSON)**:
```json
{
  "userId": 456,
  "nodeId": 123
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| userId | Long | 是 | @NotNull | 被邀请用户ID，必须大于0 |
| nodeId | Long | 是 | @NotNull | 节点ID，必须大于0 |

### 请求头
```
token: your-auth-token
```

### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "邀请成功",
  "timestamp": 1703001234567
}
```

**失败 - 参数无效 (1002)**:
```json
{
  "code": 1002,
  "message": "用户ID无效",
  "timestamp": 1703001234567
}
```

**失败 - 用户不存在 (1116)**:
```json
{
  "code": 1116,
  "message": "用户不存在",
  "timestamp": 1703001234567
}
```

**失败 - 用户未登录 (1101)**:
```json
{
  "code": 1101,
  "message": "用户未登录",
  "timestamp": 1703001234567
}
```

### 业务说明

1. **邀请机制**:
   - 邀请指定用户查看某个节点的内容
   - 被邀请用户会收到邀请消息通知
   - 消息中包含节点信息和邀请者信息

2. **验证**:
   - 验证被邀请用户是否存在
   - 验证节点ID是否有效
   - 邀请者必须登录

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 1002 | 参数异常 |
| 1101 | 用户未登录 |
| 1116 | 用户不存在 |
| 2301 | 访问过于频繁，请稍后再试 |

---

## 限流说明

### 全局限流
- **容量**: 60次
- **补充速率**: 每分钟补充60次
- **限流维度**: 按用户ID

### 限流错误响应
```json
{
  "code": 2301,
  "message": "访问过于频繁，请稍后再试",
  "timestamp": 1703001234567
}
```

---

## 业务流程说明

### 场景1: 接收互动通知
1. 用户A关注了用户B
2. 系统创建关注消息（type=2，category=1）发送给用户B
3. 用户B登录后查看互动消息列表
4. 看到用户A的关注通知

### 场景2: 邀请用户
1. 用户A在节点123下看到精彩内容
2. 用户A邀请用户B查看（使用POST /messages/invite）
3. 系统创建邀请消息（type=4，category=1）发送给用户B
4. 用户B收到通知，点击跳转到节点123

### 场景3: 审核通知
1. 用户发布课程/帖子/评论等内容
2. 管理员审核内容，决定通过/拒绝/封禁
3. 系统自动创建对应的审核消息（type=11-24）
4. 用户收到审核结果通知，查看原因和建议

---

## 数据结构说明

### MessageDTO (基础消息)
```typescript
interface MessageDTO {
  id: number                // 消息ID
  senderId: number          // 发送者ID，0表示系统
  sender: UserDTO | null    // 发送者信息
  receiverId: number        // 接收者ID
  receiver: UserDTO         // 接收者信息
  content: string           // 消息内容（可能是JSON字符串）
  type: number              // 消息类型
  createdAt: string         // 创建时间
}
```

### CommentMessageDTO (评论消息)
```typescript
interface CommentMessageDTO extends MessageDTO {
  commentId: number         // 评论ID
  commenter: UserDTO        // 评论者信息
  node: NodeSummaryDTO      // 节点信息
}
```

### UpvoteMessageDTO (点赞消息)
```typescript
interface UpvoteMessageDTO extends MessageDTO {
  objectId: number          // 被点赞对象ID
  objectType: number        // 对象类型：1=帖子, 2=评论
  voteType: number          // 点赞类型：1=twice, 2=like
  upvoter: UserDTO          // 点赞者信息
  node: NodeSummaryDTO      // 节点信息
}
```

### FollowMessageDTO (关注消息)
```typescript
interface FollowMessageDTO extends MessageDTO {
  follower: UserDTO         // 关注者信息
}
```

### InviteMessageDTO (邀请消息)
```typescript
interface InviteMessageDTO extends MessageDTO {
  inviter: UserDTO          // 邀请者信息
  node: NodeSummaryDTO      // 节点信息
}
```

---

## 使用示例

### 1. 获取互动消息

```bash
curl -X GET "http://localhost:9202/api/v1/messages/category?category=1&lastId=0" \
  -H "token: your-auth-token"
```

### 2. 邀请用户查看节点

```bash
curl -X POST http://localhost:9202/api/v1/messages/invite \
  -H "Content-Type: application/json" \
  -H "token: your-auth-token" \
  -d '{
    "userId": 456,
    "nodeId": 123
  }'
```

---

## 前端集成

### API 调用

```typescript
// 定义在 web/src/api/modules/message.ts
import { get, post } from '@/api'


// 按分类获取消息（支持按类型过滤）
export const getMessagesByCategory = (
  category: number,
  lastId?: number,
  type?: number
) => {
  return get<MessageDTO[]>('/api/v1/messages/category', {
    params: { category, lastId, type }
  })
}

// 邀请用户
export const inviteUser = (userId: number, nodeId: number) => {
  return post<void>('/api/v1/messages/invite', {
    userId,
    nodeId
  })
}
```

### 使用示例

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMessagesByCategory, inviteUser } from '@/api/modules/message'

const messages = ref<MessageDTO[]>([])
const lastId = ref<number>(0)
const loading = ref(false)

// 加载互动消息
const loadMessages = async () => {
  loading.value = true
  try {
    const data = await getMessagesByCategory(1, lastId.value)
    messages.value.push(...data)
    if (data.length > 0) {
      lastId.value = data[data.length - 1].id
    }
  } catch (error) {
    console.error('加载消息失败', error)
  } finally {
    loading.value = false
  }
}

// 邀请用户
const handleInvite = async (userId: number, nodeId: number) => {
  try {
    await inviteUser(userId, nodeId)
    // 显示成功提示
  } catch (error) {
    console.error('邀请失败', error)
  }
}

onMounted(() => {
  loadMessages()
})
</script>
```

---

## 注意事项

1. **消息内容格式**:
   - 系统消息的 content 字段通常是JSON字符串
   - 不同类型的消息有不同的JSON结构
   - 需要根据 type 解析 content

2. **系统消息发送者**:
   - 系统消息的 senderId 为 0
   - sender 字段为 null
   - 不要尝试获取发送者信息

3. **参数验证**:
   - 邀请用户时，使用 IllegalArgumentException 进行验证（代码中应改为 StatusCode）
   - userId 和 nodeId 都必须大于 0
   - 被邀请用户必须存在

4. **管理接口安全**:
   - 发送系统消息接口没有认证
   - 这些接口仅供管理后台使用
   - 生产环境应添加管理员权限验证

5. **消息类型扩展**:
   - 系统支持24种消息类型
   - 前10种是互动消息（category=1）
   - 后14种是审核消息（category=2）
   - 可根据业务需求继续扩展

6. **分页性能**:
   - 使用游标分页（lastId）提高查询效率
   - 避免使用 offset 分页
   - 每页固定返回20条

---

## 常见问题

### Q1: 如何区分不同类型的消息？
A: 根据 `type` 字段判断消息类型，然后将 content 解析为对应的结构。系统会根据类型返回不同的DTO（如CommentMessageDTO、UpvoteMessageDTO等）。

### Q2: 系统消息的 content 是什么格式？
A: content 通常是JSON字符串，包含具体的业务数据。例如课程申请消息包含 title、summary、explanation、parentId 等字段。

### Q3: 如何实现消息的实时推送？
A: 当前接口只支持轮询查询。如需实时推送，可以考虑集成 WebSocket 或 Server-Sent Events (SSE)。

### Q4: conversation 参数有什么作用？
A: `conversation=0` 只查询发给当前用户的消息，`conversation=1` 查询当前用户与指定用户的所有往来消息（双向对话）。

### Q5: 如何处理审核消息？
A: 审核消息（type=11-24）会在管理员审核内容后自动发送给用户。content 中包含审核结果、原因等信息，需要解析JSON获取详情。

---

*此文档用于指导前后端开发和接口对接，确保数据结构和业务逻辑的一致性*
