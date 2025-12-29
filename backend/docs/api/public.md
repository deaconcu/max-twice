# 公开接口 API 文档

## 概述

PublicController 提供无需登录即可访问的公开接口。

**基础路径**: `/api/v1/public`

**限流规则**:
- 类型: IP 限流
- 容量: 50 次/分钟
- 补充周期: 1 分钟

---

## 1. 获取课程分类

获取课程分类数据，支持 ETag 缓存优化。

### 请求

**端点**: `GET /api/v1/public/course-categories`

**请求头**:
- `If-None-Match` (可选): 客户端缓存的 ETag 值

**请求参数**: 无

### 响应

#### 成功响应 (200 OK)

**响应头**:
- `ETag`: 资源的 ETag 值，用于缓存验证

**响应体**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "mainCategories": [
      {
        "id": 1,
        "name": "编程语言",
        "description": "各类编程语言学习"
      },
      {
        "id": 2,
        "name": "框架技术",
        "description": "主流开发框架"
      }
    ],
    "categoryMapping": [
      {
        "mainCategoryId": 1,
        "subcategories": [
          {"id": 1, "name": "Java"},
          {"id": 2, "name": "Python"},
          {"id": 3, "name": "JavaScript"}
        ]
      },
      {
        "mainCategoryId": 2,
        "subcategories": [
          {"id": 1, "name": "Spring"},
          {"id": 2, "name": "Django"}
        ]
      }
    ]
  }
}
```

#### 未修改 (304 Not Modified)

客户端提供的 ETag 与服务器当前资源一致，资源未修改。

**响应头**:
- `ETag`: 资源的 ETag 值

**响应体**: 无

#### 错误响应 (500 Internal Server Error)

```json
{
  "code": 500,
  "message": "系统错误",
  "data": null
}
```

### 使用示例

```bash
# 首次请求
curl -X GET "http://localhost:8080/api/v1/public/course-categories"

# 带 ETag 的条件请求
curl -X GET "http://localhost:8080/api/v1/public/course-categories" \
  -H "If-None-Match: \"a1b2c3d4e5f6\""
```

---

## 2. 获取职业分类

获取职业分类数据，支持 ETag 缓存优化。

### 请求

**端点**: `GET /api/v1/public/profession-categories`

**请求头**:
- `If-None-Match` (可选): 客户端缓存的 ETag 值

**请求参数**: 无

### 响应

#### 成功响应 (200 OK)

**响应头**:
- `ETag`: 资源的 ETag 值，用于缓存验证

**响应体**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "mainCategories": [
      {
        "id": 1,
        "title": "软件开发",
        "description": "软件开发相关职业"
      },
      {
        "id": 2,
        "title": "数据分析",
        "description": "数据分析相关职业"
      }
    ],
    "categoryMapping": [
      {
        "mainCategoryId": 1,
        "subcategories": [
          {"id": 1, "name": "后端开发"},
          {"id": 2, "name": "前端开发"}
        ]
      },
      {
        "mainCategoryId": 2,
        "subcategories": [
          {"id": 1, "name": "数据科学家"},
          {"id": 2, "name": "数据工程师"}
        ]
      }
    ]
  }
}
```

#### 未修改 (304 Not Modified)

客户端提供的 ETag 与服务器当前资源一致，资源未修改。

**响应头**:
- `ETag`: 资源的 ETag 值

**响应体**: 无

#### 错误响应 (500 Internal Server Error)

```json
{
  "code": 500,
  "message": "系统错误",
  "data": null
}
```

### 使用示例

```bash
# 首次请求
curl -X GET "http://localhost:8080/api/v1/public/profession-categories"

# 带 ETag 的条件请求
curl -X GET "http://localhost:8080/api/v1/public/profession-categories" \
  -H "If-None-Match: \"f6e5d4c3b2a1\""
```

---

## 3. 查询只读模式状态

查询系统是否处于只读模式。

### 请求

**端点**: `GET /api/v1/public/readonly-mode`

**请求参数**: 无

### 响应

#### 成功响应 (200 OK)

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "enabled": false
  }
}
```

**字段说明**:
- `enabled` (boolean): 是否启用只读模式
  - `true`: 系统处于只读模式，写操作被禁用
  - `false`: 系统正常运行

#### 错误响应

```json
{
  "code": 500,
  "message": "查询失败",
  "data": null
}
```

### 使用示例

```bash
curl -X GET "http://localhost:8080/api/v1/public/readonly-mode"
```

---

## 4. 获取职业详情

获取指定职业的详细信息。

### 请求

**端点**: `GET /api/v1/public/professions/{id}`

**路径参数**:
- `id` (Long, 必填): 职业ID，必须大于 0

### 响应

#### 成功响应 (200 OK)

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "name": "Java 后端开发工程师",
    "description": "负责使用 Java 进行后端服务开发...",
    "mainCategoryId": 1,
    "subcategoryId": 1,
    "createdAt": "2024-01-01 10:00:00",
    "updatedAt": "2024-01-15 15:30:00"
  }
}
```

**字段说明**:
- `id` (Long): 职业ID
- `name` (String): 职业名称
- `description` (String): 职业描述
- `mainCategoryId` (Integer): 主分类ID
- `subcategoryId` (Integer): 子分类ID
- `createdAt` (String): 创建时间
- `updatedAt` (String): 更新时间

#### 职业不存在 (404)

```json
{
  "code": 404,
  "message": "职业不存在",
  "data": null
}
```

#### 参数错误

**职业ID为空**:
```json
{
  "code": 400,
  "message": "职业ID不能为空",
  "data": null
}
```

**职业ID无效**:
```json
{
  "code": 400,
  "message": "职业ID必须大于0",
  "data": null
}
```

### 使用示例

```bash
# 获取职业详情
curl -X GET "http://localhost:8080/api/v1/public/professions/1"

# 职业不存在
curl -X GET "http://localhost:8080/api/v1/public/professions/99999"

# 参数错误
curl -X GET "http://localhost:8080/api/v1/public/professions/0"
curl -X GET "http://localhost:8080/api/v1/public/professions/-1"
```

---

## 5. 获取职业的路线图列表

获取指定职业的所有路线图，支持分页加载。

### 请求

**端点**: `GET /api/v1/public/professions/{professionId}/roadmaps`

**路径参数**:
- `professionId` (Long, 必填): 职业ID，必须大于 0

**查询参数**:
- `lastId` (Long, 可选): 上一页最后一条记录的ID，用于游标分页
  - 不传或传 `null`: 加载第一页
  - 传具体ID: 加载该ID之后的数据
- `pageSize` (Integer, 可选): 每页记录数，默认 20

### 响应

#### 成功响应 (200 OK)

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 101,
      "professionId": 1,
      "professionName": "Java 后端开发工程师",
      "description": "从零基础到资深 Java 后端工程师的完整学习路线",
      "authorId": 1001,
      "authorName": "张三",
      "difficulty": "intermediate",
      "estimatedHours": 500,
      "studentCount": 1234,
      "rating": 4.8,
      "createdAt": "2024-01-10 09:00:00"
    },
    {
      "id": 102,
      "professionId": 1,
      "professionName": "Java 后端开发工程师",
      "description": "企业级 Java 微服务架构实战路线",
      "authorId": 1002,
      "authorName": "李四",
      "difficulty": "advanced",
      "estimatedHours": 300,
      "studentCount": 856,
      "rating": 4.9,
      "createdAt": "2024-01-15 14:30:00"
    }
  ]
}
```

**字段说明**:
- `id` (Long): 路线图ID
- `professionId` (Long): 所属职业ID
- `professionName` (String): 职业名称
- `description` (String): 路线图描述
- `authorId` (Long): 作者用户ID
- `authorName` (String): 作者姓名
- `difficulty` (String): 难度级别
  - `beginner`: 初级
  - `intermediate`: 中级
  - `advanced`: 高级
- `estimatedHours` (Integer): 预计学习时长（小时）
- `studentCount` (Integer): 学习人数
- `rating` (Double): 评分（0-5）
- `createdAt` (String): 创建时间

#### 空列表 (200 OK)

职业存在但没有路线图，或已加载完所有数据：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": []
}
```

#### 参数错误

**职业ID为空**:
```json
{
  "code": 400,
  "message": "职业ID不能为空",
  "data": null
}
```

**职业ID无效**:
```json
{
  "code": 400,
  "message": "职业ID必须大于0",
  "data": null
}
```

### 分页说明

使用基于游标的分页（cursor-based pagination）:

1. **第一页**: 不传 `lastId` 参数，或传 `null`
2. **下一页**: 传入当前页最后一条记录的 `id` 作为 `lastId`
3. **判断是否还有数据**: 如果返回的数组长度小于 `pageSize`，说明已到最后一页

### 使用示例

```bash
# 获取第一页（默认20条）
curl -X GET "http://localhost:8080/api/v1/public/professions/1/roadmaps"

# 获取第一页（自定义每页10条）
curl -X GET "http://localhost:8080/api/v1/public/professions/1/roadmaps?pageSize=10"

# 获取下一页（lastId=102）
curl -X GET "http://localhost:8080/api/v1/public/professions/1/roadmaps?lastId=102&pageSize=10"

# 职业不存在或没有路线图
curl -X GET "http://localhost:8080/api/v1/public/professions/99999/roadmaps"
```

---

## 6. 读取课程页面数据

读取指定课程的页面内容，支持通过路径访问特定章节。

**注意**: 公开接口返回的数据不包含个性化信息（学习进度、订阅状态等均为默认值）。

### 请求

**端点**: `GET /api/v1/public/pages/read`

**查询参数**:
- `courseId` (Long, 必填): 课程ID，必须大于 0
- `path` (String, 可选): 页面路径
  - 不传: 读取课程根节点（目录首页）
  - 传路径: 读取指定章节，格式如 `1-xxx`、`1-2-yyy`

### 响应

#### 成功响应 (200 OK)

**课程根节点（目录首页）**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "course": {
      "id": 123,
      "name": "Java 核心编程",
      "description": "深入学习 Java 核心技术",
      "authorId": 1001,
      "authorName": "张三",
      "createdAt": "2024-01-01 10:00:00"
    },
    "toc": [
      {
        "id": 1,
        "name": "第一章：Java 基础",
        "path": "1-abc",
        "children": [
          {
            "id": 2,
            "name": "1.1 环境搭建",
            "path": "1-2-xyz"
          },
          {
            "id": 3,
            "name": "1.2 基本语法",
            "path": "1-3-def"
          }
        ]
      },
      {
        "id": 4,
        "name": "第二章：面向对象",
        "path": "4-ghi",
        "children": []
      }
    ],
    "subscribed": false,
    "progress": 0
  }
}
```

**具体章节页面**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "course": {
      "id": 123,
      "name": "Java 核心编程",
      "description": "深入学习 Java 核心技术",
      "authorId": 1001,
      "authorName": "张三",
      "createdAt": "2024-01-01 10:00:00"
    },
    "currentNode": {
      "id": 2,
      "name": "1.1 环境搭建",
      "path": "1-2-xyz",
      "content": "本节介绍如何搭建 Java 开发环境...",
      "orderIndex": 1
    },
    "toc": [...],
    "navigation": {
      "prev": null,
      "next": {
        "id": 3,
        "name": "1.2 基本语法",
        "path": "1-3-def"
      }
    },
    "subscribed": false,
    "progress": 0,
    "completed": false
  }
}
```

**字段说明**:

**course** (课程信息):
- `id` (Long): 课程ID
- `name` (String): 课程名称
- `description` (String): 课程描述
- `authorId` (Long): 作者ID
- `authorName` (String): 作者姓名
- `createdAt` (String): 创建时间

**currentNode** (当前节点信息，仅在访问具体章节时返回):
- `id` (Long): 节点ID
- `name` (String): 节点名称
- `path` (String): 节点路径
- `content` (String): 节点内容（Markdown格式）
- `orderIndex` (Integer): 排序序号

**toc** (目录树):
- `id` (Long): 节点ID
- `name` (String): 节点名称
- `path` (String): 节点路径
- `children` (Array): 子节点列表（递归结构）

**navigation** (导航信息，仅在访问具体章节时返回):
- `prev` (Object | null): 上一节点
- `next` (Object | null): 下一节点

**个性化信息**（公开接口返回默认值）:
- `subscribed` (Boolean): 是否订阅，固定为 `false`
- `progress` (Integer): 学习进度，固定为 `0`
- `completed` (Boolean): 是否完成，固定为 `false`（仅章节页面）

#### 参数错误

**课程ID为空**:
```json
{
  "code": 400,
  "message": "课程ID不能为空",
  "data": null
}
```

**课程ID无效**:
```json
{
  "code": 400,
  "message": "课程ID必须大于0",
  "data": null
}
```

#### 业务错误

**课程不存在**:
```json
{
  "code": 404,
  "message": "课程不存在",
  "data": null
}
```

**章节不存在**:
```json
{
  "code": 404,
  "message": "章节不存在",
  "data": null
}
```

### 使用示例

```bash
# 读取课程根节点（目录首页）
curl -X GET "http://localhost:8080/api/v1/public/pages/read?courseId=123"

# 读取指定章节
curl -X GET "http://localhost:8080/api/v1/public/pages/read?courseId=123&path=1-2-xyz"

# 参数错误
curl -X GET "http://localhost:8080/api/v1/public/pages/read?courseId=0"
curl -X GET "http://localhost:8080/api/v1/public/pages/read"

# 课程不存在
curl -X GET "http://localhost:8080/api/v1/public/pages/read?courseId=99999"

# 章节不存在
curl -X GET "http://localhost:8080/api/v1/public/pages/read?courseId=123&path=invalid-path"
```

---

## 通用说明

### 限流处理

所有公开接口共享 IP 限流配置：
- 每个 IP 地址每分钟最多 50 次请求
- 超出限流后返回 429 Too Many Requests

```json
{
  "code": 429,
  "message": "请求过于频繁，请稍后再试",
  "data": null
}
```

### ETag 缓存机制

课程分类和职业分类接口支持 ETag 缓存：

1. **首次请求**: 服务器返回数据和 ETag 响应头
2. **后续请求**: 客户端携带 `If-None-Match` 请求头，值为上次的 ETag
3. **数据未变化**: 服务器返回 304 Not Modified，客户端使用缓存
4. **数据已变化**: 服务器返回 200 OK 和新数据，更新 ETag

### 错误码说明

| 错误码 | 说明 |
|-------|------|
| 200 | 操作成功 |
| 304 | 资源未修改（ETag 匹配） |
| 400 | 参数错误 |
| 404 | 资源不存在 |
| 429 | 请求频率超限 |
| 500 | 系统错误 |

### 数据格式

- 时间格式: `yyyy-MM-dd HH:mm:ss`
- 字符编码: UTF-8
- 响应格式: JSON

---

## 版本历史

- **v1.0** (2024-01-01): 初始版本
  - 课程分类接口
  - 职业分类接口
  - 只读模式查询接口
  - 职业详情接口
  - 职业路线图列表接口
  - 课程页面读取接口
