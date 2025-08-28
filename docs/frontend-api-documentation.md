# 学习平台前端API接口文档

## 1. 接口概述

本文档描述了学习平台前端与后端交互的所有API接口，包括用户管理、课程学习、内容管理、统计数据等核心功能。

### 1.1 基础信息
- **基础URL**: `/api`
- **数据格式**: JSON
- **字符编码**: UTF-8
- **认证方式**: Sa-Token (Header: `satoken`)

### 1.2 通用响应格式
```json
{
  "code": 200,           // 状态码：200成功，401未登录，400参数错误，500服务器错误
  "message": "success",  // 响应消息
  "data": {}            // 响应数据，具体结构见各接口说明
}
```

### 1.3 状态码说明
- `200`: 操作成功
- `400`: 请求参数错误
- `401`: 用户未登录或token无效
- `403`: 权限不足
- `404`: 资源不存在
- `500`: 服务器内部错误

---

## 2. 用户管理接口

### 2.1 用户注册
```http
POST /api/user/register
```

**请求参数:**
```json
{
  "userName": "string",    // 用户名，3-20字符，必填
  "email": "string",       // 邮箱地址，必填，格式验证
  "password": "string"     // 密码，6-32字符，必填
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "注册成功，请查收验证邮件",
  "data": null
}
```

**说明:**
- 注册成功后会发送验证邮件到用户邮箱
- 用户需要验证邮箱后才能正常登录

### 2.2 用户登录
```http
POST /api/user/login
```

**请求参数:**
```json
{
  "email": "string",       // 邮箱地址，必填
  "password": "string"     // 密码，必填
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "id": 1,
    "name": "张三",
    "email": "zhangsan@example.com",
    "avatar": "https://example.com/avatar.jpg",
    "subscriptions": [        // 用户订阅的课程列表
      {
        "id": 1,
        "name": "Java基础教程"
      }
    ]
  }
}
```

**说明:**
- 登录成功后会在响应头中返回satoken，前端需要保存
- 返回用户基本信息和订阅课程列表

### 2.3 获取当前用户信息
```http
GET /api/user/self
```

**请求头:**
```
Authorization: Bearer {satoken}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "张三",
    "email": "zhangsan@example.com",
    "biography": "热爱学习的程序员",
    "avatar": "https://example.com/avatar.jpg",
    "createTime": "2024-01-01T10:00:00",
    "emailValidated": true
  }
}
```

### 2.4 更新用户资料
```http
PUT /api/user/self
```

**请求参数:**
```json
{
  "name": "string",        // 用户昵称，可选
  "biography": "string"    // 个人简介，可选
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "更新成功",
  "data": null
}
```

### 2.5 用户搜索
```http
GET /api/user/search?name={用户名}
```

**查询参数:**
- `name`: 用户名关键词，支持模糊搜索

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "张三",
      "avatar": "https://example.com/avatar.jpg"
    }
  ]
}
```

---

## 3. 课程学习接口

### 3.1 读取课程内容 (核心接口)
```http
GET /api/read/by-path?courseId={课程ID}&path={路径}
GET /api/read/by-node?nodeId={节点ID}
GET /api/read/by-post?postId={帖子ID}
GET /api/read/by-comment?commentId={评论ID}
```

**查询参数:**
- `courseId`: 课程ID
- `path`: 节点路径，格式如"1-2-3"
- `nodeId`: 直接跳转到指定节点
- `postId`: 直接跳转到指定帖子
- `commentId`: 直接跳转到指定评论

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "course": {                    // 当前课程信息
      "id": 1,
      "name": "Java基础教程",
      "description": "从零开始学习Java编程",
      "progress": 2500,            // 用户进度，单位为百分比*100
      "isCompleted": false
    },
    "parentCourse": {              // 父课程信息（如果是子课程）
      "id": 1,
      "name": "Java基础教程",
      "description": "完整的Java学习路径",
      "subscribed": true
    },
    "subCourseList": [             // 子课程列表
      {
        "id": 2,
        "name": "Java进阶",
        "description": "深入Java高级特性"
      }
    ],
    "node": {                      // 当前节点信息
      "id": 123,
      "name": "变量和数据类型",
      "isCompleted": false
    },
    "toc": [                       // 课程目录结构
      {
        "1": {                     // 节点ID
          "2": {                   // 子节点ID
            "3": {},               // 叶子节点
            "+": 456,              // 选中的帖子ID
            "^": [789, 790]        // 置顶帖子ID列表
          }
        }
      }
    ],
    "tocNodeInfos": {              // 节点详细信息
      "123": {
        "name": "变量和数据类型",
        "isCompleted": false
      }
    },
    "chosenPosting": {             // 选中的帖子
      "id": 456,
      "title": "Java变量详解",
      "content": "...",
      "creator": {
        "id": 1,
        "name": "张三"
      },
      "voteType": 1                // 用户投票状态：1赞成，-1反对，null未投票
    },
    "fixedPostings": [             // 置顶帖子列表
      {
        "id": 789,
        "title": "重要：学习指南",
        "content": "..."
      }
    ],
    "otherPostings": [             // 其他帖子列表
      {
        "id": 790,
        "title": "常见问题解答",
        "content": "..."
      }
    ],
    "path": "1-2-3",              // 当前路径
    "learning": true,              // 用户是否正在学习此课程
    "users": [                     // 相关用户信息
      {
        "id": 1,
        "name": "张三",
        "avatar": "https://example.com/avatar.jpg"
      }
    ]
  }
}
```

**说明:**
- 这是前端Read.vue页面的核心数据接口
- 根据不同的查询参数返回对应的课程内容
- 包含完整的课程结构、用户进度、帖子内容等信息

### 3.2 获取课程信息
```http
GET /api/course/{courseId}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "Java基础教程",
    "description": "从零开始学习Java编程",
    "mainCategory": 1,
    "subCategory": 2,
    "state": "APPROVED",
    "creator": "admin",
    "createTime": "2024-01-01T10:00:00"
  }
}
```

### 3.3 获取分类课程列表
```http
GET /api/course/category?mainCategory={主分类}&subCategory={子分类}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "Java基础教程",
      "description": "从零开始学习Java编程",
      "subscribed": false          // 当前用户是否已订阅
    }
  ]
}
```

### 3.4 订阅/取消订阅课程
```http
POST /api/user/subscribe/{courseId}
DELETE /api/user/unsubscribe/{courseId}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [1, 2, 3]              // 用户当前订阅的课程ID列表
}
```

### 3.5 开始/停止学习课程
```http
POST /api/user/start-course/{courseId}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": true                   // true表示开始学习，false表示停止学习
}
```

---

## 4. 学习进度接口

### 4.1 标记节点完成
```http
POST /api/user/mark-node-completed
```

**请求参数:**
```json
{
  "nodeId": 123,                 // 节点ID，必填
  "courseId": 1                  // 课程ID，必填
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "nodeId": 123,
    "completed": true,
    "isNewlyCompleted": true,     // 是否是新完成的
    "courseProgress": 25,         // 课程整体进度百分比
    "totalCompletedNodes": 15     // 用户总完成节点数
  }
}
```

### 4.2 取消节点完成标记
```http
POST /api/user/unmark-node-completed
```

**请求参数:**
```json
{
  "nodeId": 123,
  "courseId": 1
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "nodeId": 123,
    "completed": false,
    "wasRemoved": true,           // 是否成功移除
    "courseProgress": 20,
    "totalCompletedNodes": 14
  }
}
```

### 4.3 检查节点完成状态
```http
GET /api/user/is-node-completed?nodeId={节点ID}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "nodeId": 123,
    "completed": true
  }
}
```

### 4.4 标记课程完成
```http
POST /api/user/mark-course-completed
```

**请求参数:**
```json
{
  "courseId": 1
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "courseId": 1,
    "completed": true,
    "message": "课程已标记为完成"
  }
}
```

---

## 5. 帖子和评论接口

### 5.1 获取帖子列表 (分页)
```http
GET /api/postings?nodeId={节点ID}&lastScore={最后分数}&lastId={最后ID}
```

**查询参数:**
- `nodeId`: 节点ID，可选
- `lastScore`: 上次加载的最后一个帖子分数，用于分页
- `lastId`: 上次加载的最后一个帖子ID，用于分页

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "title": "Java变量详解",
      "content": "详细介绍Java中的变量...",
      "creator": {
        "id": 1,
        "name": "张三",
        "avatar": "https://example.com/avatar.jpg"
      },
      "upvoteCount": 10,
      "commentCount": 5,
      "viewCount": 100,
      "voteType": null,            // 当前用户的投票状态
      "createTime": "2024-01-01T10:00:00"
    }
  ]
}
```

### 5.2 投票操作
```http
POST /api/upvote
```

**请求参数:**
```json
{
  "objectId": 1,                 // 对象ID（帖子或评论）
  "objectType": 1,               // 对象类型：1帖子，2评论
  "type": 1                      // 投票类型：1赞成，-1反对
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "upvoteCount": 11,
    "voteType": 1                // 用户当前投票状态
  }
}
```

### 5.3 获取评论列表
```http
GET /api/comment/by-object?objectId={对象ID}&type={类型}&offsetId={偏移ID}
```

**查询参数:**
- `objectId`: 对象ID（帖子ID、节点ID等）
- `type`: 对象类型：1帖子，2节点，3路线图
- `offsetId`: 分页偏移ID，第一次加载传0

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "content": "这个解释很清楚！",
      "fromUser": 1,
      "fromUserName": "张三",
      "fromUserAvatar": "https://example.com/avatar.jpg",
      "upvoted": 0,                // 当前用户是否点赞：0未点赞，1已点赞
      "upvoteCount": 5,
      "replyCount": 2,
      "createTime": "2024-01-01T10:00:00",
      "children": [                // 子评论
        {
          "id": 2,
          "content": "我也这么认为",
          "fromUser": 2,
          "replyTo": 1             // 回复的评论ID
        }
      ]
    }
  ]
}
```

### 5.4 发表评论
```http
POST /api/comment/create
```

**请求参数:**
```json
{
  "objectId": 1,                 // 对象ID，必填
  "type": 1,                     // 对象类型，必填
  "content": "string",           // 评论内容，必填
  "replyTo": 0                   // 回复的评论ID，可选，0表示主评论
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "评论发表成功",
  "data": {
    "id": 3,
    "content": "这个解释很清楚！",
    "fromUser": 1,
    "createTime": "2024-01-01T10:00:00"
  }
}
```

---

## 6. 用户路线图接口

### 6.1 开始学习路线图
```http
POST /api/user-roadmap/start
```

**请求参数:**
```json
{
  "roadmapId": 1                 // 路线图ID，必填
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": true                   // 是否成功开始
}
```

### 6.2 获取用户路线图进度
```http
GET /api/user-roadmap/get?roadmapId={路线图ID}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "roadmapId": 1,
    "userId": 1,
    "progressPercent": 30,        // 完成百分比
    "completedSteps": ["step1", "step2"],
    "currentStep": "step3",
    "startTime": "2024-01-01T10:00:00",
    "lastUpdateTime": "2024-01-02T15:30:00"
  }
}
```

### 6.3 获取用户所有路线图
```http
GET /api/user-roadmap/all
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "roadmapId": 1,
      "roadmapName": "Java全栈开发",
      "progressPercent": 30,
      "status": "IN_PROGRESS"      // 状态：NOT_STARTED, IN_PROGRESS, COMPLETED
    }
  ]
}
```

### 6.4 更新路线图进度
```http
PUT /api/user-roadmap/update
```

**请求参数:**
```json
{
  "roadmapId": 1,               // 路线图ID，必填
  "progressPercent": 50         // 进度百分比，必填，0-100
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "roadmapId": 1,
    "progressPercent": 50,
    "lastUpdateTime": "2024-01-02T16:00:00"
  }
}
```

### 6.5 删除路线图进度
```http
DELETE /api/user-roadmap/delete?roadmapId={路线图ID}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

## 7. 统计数据接口

### 7.1 获取平台统计数据
```http
GET /api/platform-stats
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalUsers": 10000,          // 总用户数
    "totalCourses": 500,          // 总课程数
    "totalPosts": 50000,          // 总帖子数
    "activeUsers": 1500,          // 活跃用户数
    "todayLearningTime": 120000,  // 今日学习时长（分钟）
    "popularCourses": [           // 热门课程
      {
        "id": 1,
        "name": "Java基础",
        "enrollCount": 1000
      }
    ]
  }
}
```

### 7.2 获取用户学习统计
```http
GET /api/user/learning-stats
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalLearningTime": 1200,    // 总学习时长（分钟）
    "completedCourses": 5,        // 完成课程数
    "completedNodes": 150,        // 完成节点数
    "currentStreak": 7,           // 连续学习天数
    "weeklyProgress": [           // 本周学习进度
      {
        "date": "2024-01-01",
        "minutes": 60
      }
    ]
  }
}
```

---

## 8. 系统配置接口

### 8.1 获取系统配置
```http
GET /api/system/config
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "siteName": "学习平台",
    "version": "1.0.0",
    "features": {
      "enableComments": true,
      "enableRoadmaps": true,
      "enableStats": true
    },
    "limits": {
      "maxUploadSize": 10485760,  // 最大上传文件大小（字节）
      "maxCommentLength": 1000    // 最大评论长度
    }
  }
}
```

### 8.2 获取配置的特定部分
```http
GET /api/system/config-part?part={配置部分}
```

**查询参数:**
- `part`: 配置部分名称，如"features"、"limits"等

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "enableComments": true,
    "enableRoadmaps": true,
    "enableStats": true
  }
}
```

---

## 9. 内容管理接口

### 9.1 配置课程内容
```http
POST /api/contents/update
```

**请求参数:**
```json
{
  "path": "1-2-3",             // 节点路径，必填
  "courseId": 1,               // 课程ID，必填
  "postingId": 456,            // 帖子ID，必填
  "action": 1                  // 操作类型：1选择，2取消选择，3置顶，4取消置顶
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

### 9.2 更新内容列表
```http
POST /api/contents/list
```

**请求参数:**
```json
{
  "courseId": 1,               // 课程ID，必填
  "list": "..."                // 内容列表JSON字符串，必填
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "更新成功",
  "data": null
}
```

---

## 10. AI聊天接口

### 10.1 与AI聊天
```http
POST /api/chat/gpt
```

**请求参数:**
```json
{
  "prompt": "string",          // 用户输入的问题，必填
  "model": "gpt-3.5-turbo"     // AI模型，可选，默认gpt-3.5-turbo
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": "AI回复的内容..."
}
```

**说明:**
- 用于学习过程中的AI辅助功能
- 支持多种AI模型选择

---

## 11. 子课程申请接口

### 11.1 申请创建子课程
```http
POST /api/apply-course
```

**请求参数:**
```json
{
  "title": "string",           // 课程标题，必填
  "summary": "string",         // 课程摘要，必填
  "explanation": "string",     // 详细说明，必填
  "parentId": 1                // 父课程ID，必填
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "申请提交成功，等待审核",
  "data": null
}
```

### 11.2 获取课程申请列表 (管理员)
```http
GET /api/apply-course/list?page={页码}&length={每页数量}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "messages": [
      {
        "id": 1,
        "title": "Java进阶课程",
        "summary": "深入学习Java高级特性",
        "explanation": "详细说明...",
        "applicantName": "张三",
        "status": "PENDING",       // 状态：PENDING待审核，APPROVED已通过，REJECTED已拒绝
        "createTime": "2024-01-01T10:00:00"
      }
    ],
    "pagination": {
      "total": 100,
      "pageSize": 10,
      "currentPage": 1,
      "totalPages": 10
    }
  }
}
```

---

## 12. 邮件验证接口

### 12.1 验证邮箱
```http
POST /api/user/validate-email
```

**请求参数:**
```json
{
  "email": "string",           // 邮箱地址，必填
  "code": "string"             // 验证码，必填
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "邮箱验证成功",
  "data": {
    "id": 1,
    "name": "张三",
    "email": "zhangsan@example.com",
    "emailValidated": true
  }
}
```

---

## 13. 错误处理

### 13.1 常见错误响应

#### 参数错误 (400)
```json
{
  "code": 400,
  "message": "参数不正确: 课程ID不能为空",
  "data": null
}
```

#### 未登录 (401)
```json
{
  "code": 401,
  "message": "请先登录",
  "data": null
}
```

#### 权限不足 (403)
```json
{
  "code": 403,
  "message": "权限不足",
  "data": null
}
```

#### 资源不存在 (404)
```json
{
  "code": 404,
  "message": "课程不存在",
  "data": null
}
```

#### 服务器错误 (500)
```json
{
  "code": 500,
  "message": "系统繁忙，请稍后重试",
  "data": null
}
```

---

## 14. 接口使用示例

### 14.1 完整的学习流程示例

```javascript
// 1. 用户登录
const loginResponse = await fetch('/api/user/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'password123'
  })
});

// 2. 获取课程内容
const courseResponse = await fetch('/api/read/by-path?courseId=1&path=1-2-3', {
  headers: { 'satoken': 'your-token-here' }
});

// 3. 标记节点完成
const completeResponse = await fetch('/api/user/mark-node-completed', {
  method: 'POST',
  headers: { 
    'Content-Type': 'application/json',
    'satoken': 'your-token-here'
  },
  body: JSON.stringify({
    nodeId: 123,
    courseId: 1
  })
});

// 4. 发表评论
const commentResponse = await fetch('/api/comment/create', {
  method: 'POST',
  headers: { 
    'Content-Type': 'application/json',
    'satoken': 'your-token-here'
  },
  body: JSON.stringify({
    objectId: 1,
    type: 1,
    content: '这个课程很有帮助！'
  })
});
```

### 14.2 前端拦截器配置示例

```javascript
// Axios拦截器配置
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('satoken');
  if (token) {
    config.headers['satoken'] = token;
  }
  return config;
});

axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      // 未登录，跳转到登录页
      router.push('/login');
    }
    return Promise.reject(error);
  }
);
```

---

## 15. 完整API接口列表

### 用户相关接口 (15个)
1. **POST** `/login` - 用户登录
2. **POST** `/user` - 用户注册  
3. **POST** `/user/validate` - 邮箱验证
4. **GET** `/self` - 获取当前用户信息
5. **POST** `/self` - 更新个人信息
6. **GET** `/user/subscription` - 获取用户订阅
7. **PUT** `/user/subscription` - 订阅课程
8. **DELETE** `/user/subscription` - 取消订阅课程
9. **GET** `/user/{id}` - 获取指定用户信息
10. **GET** `/user?name={name}` - 搜索用户
11. **POST** `/user/follow` - 关注用户
12. **DELETE** `/user/follow` - 取消关注
13. **GET** `/user/followee` - 获取关注列表
14. **GET** `/user/contents` - 获取用户内容
15. **GET** `/user/article` - 获取用户文章

### 系统配置接口 (3个)
16. **GET** `/system` - 获取系统配置
17. **POST** `/system` - 更新系统配置
18. **POST** `/openai` - OpenAI接口

### 课程相关接口 (9个)
19. **GET** `/course/list` - 获取课程列表（支持分页、分类、状态过滤）
20. **POST** `/course/operate` - 课程操作（审核）
21. **PUT** `/course/{id}` - 更新课程信息
22. **GET** `/course/{id}` - 获取课程详情
23. **POST** `/course` - 创建课程
24. **GET** `/course/list/approved` - 获取已通过审核的子课程
25. **GET** `/course/search` - 按名称搜索课程
26. **GET** `/course/hot` - 获取热门课程
27. **GET** `/course/ranking` - 获取课程排行榜

### 内容阅读接口 (1个)
28. **GET** `/read` - 通用阅读接口（支持courseId+path、nodeId、postId、commentId）

### 动态发布接口 (4个)
29. **GET** `/postings` - 获取动态列表
30. **POST** `/posting` - 发布动态
31. **PUT** `/posting` - 更新动态
32. **DELETE** `/posting` - 删除动态

### 内容审核接口 (2个)
33. **GET** `/post/censor` - 获取待审核文章列表
34. **PUT** `/post` - 审核文章（通过/拒绝）

### 内容管理接口 (3个)
35. **POST** `/contents` - 创建内容
36. **POST** `/toc` - 设置目录索引

### 评论相关接口 (4个)
38. **GET** `/comment` - 获取评论列表
39. **GET** `/comment/{id}/reply` - 获取评论回复
40. **POST** `/comment` - 发表评论
41. **PUT** `/comment` - 审核评论
42. **GET** `/comment/censor` - 获取待审核评论

### 点赞相关接口 (1个)
43. **POST** `/upvote` - 点赞/取消点赞

### 消息相关接口 (8个)
44. **POST** `/message/new-course` - 申请新课程
45. **GET** `/message/course-apply` - 获取用户的课程申请消息
46. **GET** `/message/system` - 获取系统消息
47. **GET** `/message/new-course` - 获取新课程申请消息
48. **GET** `/message` - 获取消息列表
49. **POST** `/message/system` - 发送系统消息
50. **PUT** `/message/system` - 回复申请消息
51. **POST** `/message/invite` - 邀请用户

### 子课程相关接口 (1个)
52. **POST** `/subcourse` - 创建子课程

### 学习路径接口 (4个)
53. **GET** `/roadmap/list/{professionId}` - 获取职业学习路径列表
54. **POST** `/roadmap` - 创建学习路径
55. **PUT** `/roadmap/{id}/upvote` - 点赞学习路径
56. **POST** `/roadmap/pin` - 置顶学习路径

### 用户学习相关接口 (4个)
57. **POST** `/user/roadmap` - 开始学习路径
58. **GET** `/user/roadmap/list` - 获取用户学习的路径列表
59. **POST** `/user/course` - 开始学习课程
60. **GET** `/user/course/list` - 获取用户学习的课程列表

### 职业相关接口 (7个)
61. **GET** `/profession/list/approved` - 获取已审核职业列表
62. **GET** `/profession/list` - 获取职业列表（支持分类过滤、状态过滤）
63. **POST** `/profession` - 提交职业申请
64. **POST** `/profession/operate` - 操作职业申请
65. **PUT** `/profession` - 更新职业信息
66. **DELETE** `/profession` - 删除职业申请
67. **GET** `/profession/hot` - 获取热门职业

### 平台统计接口 (10个)
68. **GET** `/platform/stats` - 获取平台统计数据
69. **GET** `/api/stats/user/{userId}/today` - 获取用户今日统计
70. **GET** `/api/stats/user/{userId}/yesterday` - 获取用户昨日统计
71. **GET** `/api/stats/user/{userId}/history` - 获取用户历史统计
72. **GET** `/api/stats/user/{userId}/period` - 获取用户时间段统计
73. **GET** `/api/stats/user/{userId}/all-time` - 获取用户全部时间统计
74. **POST** `/api/stats/view` - 记录文章访问
75. **GET** `/api/stats/health` - 获取系统健康状态
76. **POST** `/api/stats/sync/manual` - 手动同步统计数据
77. **POST** `/api/stats/sync/date` - 同步指定日期数据

### 学习进度接口 (4个)
78. **POST** `/user/complete/{nodeId}` - 标记节点完成
79. **DELETE** `/user/complete/{nodeId}` - 取消标记节点完成
80. **GET** `/user/complete/{nodeId}` - 检查节点是否完成
81. **POST** `/user/complete/course/{courseId}` - 标记课程完成

**总计: 81个API接口**

---

## 17. 后端已实现但前端未使用的接口列表

基于前端 `learnService.js` 的分析，以下是后端已实现但前端尚未使用的接口：

### 节点管理接口 (3个)
- `POST /api/node` - 创建节点
- `GET /api/node/{id}` - 获取节点详情
- `GET /api/node/ids-{ids}` - 批量获取节点

### 目录管理接口 (1个)
- `GET /api/toc/size` - 获取课程TOC大小

### 投票管理接口 (4个)
- `GET /api/upvotes` - 获取已投票列表
- `GET /api/upvote` - 检查是否已投票
- `POST /api/posting/{id}/vote` - 对帖子投票
- `DELETE /api/posting/{id}/vote` - 取消投票

### 路线图管理接口 (1个)
- `PUT /api/roadmap/{id}` - 更新路线图内容

### 用户课程管理接口 (2个)
- `GET /api/user/course` - 获取单个课程进度
- `PUT /api/user/course` - 更新课程进度

### 用户路线图管理接口 (2个)
- `GET /api/user/roadmap` - 获取单个路线图进度
- `PUT /api/user/roadmap` - 更新路线图进度

**总计：13个未使用的后端接口**

### 前端已使用的后端接口统计
经分析 `learnService.js`，前端已使用的后端接口共 **68个**，涵盖：

#### 用户管理 (15个)
- 登录、注册、个人信息管理
- 关注、订阅、搜索用户等

#### 课程管理 (14个)  
- 课程CRUD、分类查询、审核操作
- 热门课程、子课程管理等

#### 内容阅读 (4个)
- 按路径、节点、帖子、评论读取内容

#### 帖子管理 (6个)
- 帖子CRUD、审核等

#### 评论系统 (4个)
- 评论CRUD、审核等

#### 消息系统 (8个)
- 课程申请、系统消息、邀请等

#### 职业管理 (7个)
- 职业CRUD、分类查询、审核等

#### 路线图管理 (4个)
- 路线图CRUD、点赞、置顶

#### 用户学习进度 (4个)
- 课程和路线图学习记录、节点完成等

#### 统计数据 (7个)
- 平台统计、用户统计、访问记录等

#### 系统配置 (3个)
- 系统配置、AI聊天等

#### 内容管理 (2个)
- 内容发布、目录设置

### 接口使用率分析
- **后端总接口数**: 约81个
- **前端已使用**: 68个 (84%)
- **前端未使用**: 13个 (16%)

前端已实现了绝大部分后端功能，未使用的接口主要集中在：
1. **节点细粒度管理** - 目前前端通过其他方式管理节点
2. **高级投票功能** - 前端使用了简化的投票接口
3. **详细进度管理** - 前端暂未实现单个课程/路线图的详细进度查询和更新

这些未使用的接口为未来功能扩展预留了空间。

---

## 16. 版本变更记录

### v1.0.0 (2024-01-01)
- 初始版本，包含所有核心功能接口
- 支持用户管理、课程学习、进度跟踪
- 支持评论、投票、路线图功能

### v1.1.0 (2024-02-01)
- 新增AI聊天接口
- 优化缓存机制
- 新增国际化支持

---

**注意事项:**
1. 所有需要认证的接口都需要在请求头中携带satoken
2. 分页接口建议前端实现无限滚动加载
3. 文件上传接口有大小限制，请在前端做好校验
4. 对于敏感操作（如删除），建议前端做二次确认
5. 所有时间字段都使用ISO 8601格式
6. 建议前端实现接口重试机制，提高稳定性