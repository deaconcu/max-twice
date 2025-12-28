# 学习进度管理接口文档

## 接口概览

### 节点进度管理
| 接口 | 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|------|
| 标记节点完成 | POST | `/api/v1/progress/nodes/{nodeId}/complete` | 是 | 标记某个节点为已完成 |
| 取消节点完成 | DELETE | `/api/v1/progress/nodes/{nodeId}/complete` | 是 | 取消节点的完成标记 |
| 检查节点状态 | GET | `/api/v1/progress/nodes/{nodeId}/status` | 是 | 查询节点的完成状态 |

### 课程进度管理
| 接口 | 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|------|
| 开始学习课程 | POST | `/api/v1/progress/courses/{courseId}/start` | 是 | 开始学习某门课程 |
| 取消学习课程 | DELETE | `/api/v1/progress/courses/{courseId}/start` | 是 | 取消学习某门课程 |
| 获取课程进度 | GET | `/api/v1/progress/courses/{courseId}` | 是 | 获取指定课程的学习进度 |
| 获取所有课程进度 | GET | `/api/v1/progress/courses` | 是 | 获取当前用户所有课程进度列表 |
| 更新课程进度 | PUT | `/api/v1/progress/courses/{courseId}` | 是 | 更新课程学习进度百分比 |
| 删除课程进度 | DELETE | `/api/v1/progress/courses/{courseId}` | 是 | 删除课程学习记录 |
| 标记课程完成 | POST | `/api/v1/progress/courses/{courseId}/complete` | 是 | 标记课程为已完成 |

### 路线图进度管理
| 接口 | 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|------|
| 开始学习路线图 | POST | `/api/v1/progress/roadmaps/{roadmapId}/start` | 是 | 开始学习某个路线图 |
| 取消学习路线图 | DELETE | `/api/v1/progress/roadmaps/{roadmapId}/start` | 是 | 取消学习某个路线图 |
| 获取路线图进度 | GET | `/api/v1/progress/roadmaps/{roadmapId}` | 是 | 获取指定路线图的学习进度 |
| 获取所有路线图进度 | GET | `/api/v1/progress/roadmaps` | 是 | 获取当前用户所有路线图进度 |
| 更新路线图进度 | PUT | `/api/v1/progress/roadmaps/{roadmapId}` | 是 | 更新路线图学习进度百分比 |
| 删除路线图进度 | DELETE | `/api/v1/progress/roadmaps/{roadmapId}` | 是 | 删除路线图学习记录 |

---

## 一、节点进度管理

### 1. 标记节点完成

#### 接口信息
- **路径**: `POST /api/v1/progress/nodes/{nodeId}/complete`
- **认证**: 需要登录
- **限流**: 60次/分钟 (按用户)

#### 请求参数

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| nodeId | Long | 是 | 节点ID，必须大于0 |

**Body (JSON)**:
```json
{
  "courseId": 123
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| courseId | Long | 是 | @NotNull, @Positive | 课程ID |

#### 请求头
```
token: your-auth-token
```

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "nodeId": 456,
    "completed": true
  },
  "timestamp": 1703001234567
}
```

**失败 - 节点已是完成状态 (1609)**:
```json
{
  "code": 1609,
  "message": "节点已是完成状态",
  "timestamp": 1703001234567
}
```

**失败 - 节点不存在 (1007)**:
```json
{
  "code": 1007,
  "message": "节点不存在",
  "timestamp": 1703001234567
}
```

**失败 - 课程不存在 (1007)**:
```json
{
  "code": 1007,
  "message": "课程不存在",
  "timestamp": 1703001234567
}
```

#### 业务说明
- 标记节点为已完成后会自动更新课程整体进度
- 如果该节点已标记完成，会返回错误码 1609
- 如需查询课程进度，请调用 `GET /api/v1/progress/courses/{courseId}` 接口

---

### 2. 取消节点完成

#### 接口信息
- **路径**: `DELETE /api/v1/progress/nodes/{nodeId}/complete`
- **认证**: 需要登录
- **限流**: 60次/分钟 (按用户)

#### 请求参数

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| nodeId | Long | 是 | 节点ID，必须大于0 |

**Body (JSON)**:
```json
{
  "courseId": 123
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| courseId | Long | 是 | @NotNull, @Positive | 课程ID |

#### 请求头
```
token: your-auth-token
```

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "nodeId": 456,
    "completed": false
  },
  "timestamp": 1703001234567
}
```

**失败 - 节点已是未完成状态 (1610)**:
```json
{
  "code": 1610,
  "message": "节点已是未完成状态",
  "timestamp": 1703001234567
}
```

#### 业务说明
- 取消标记后会自动更新课程整体进度
- 如果节点本来就未完成，会返回错误码 1610
- 如需查询课程进度，请调用 `GET /api/v1/progress/courses/{courseId}` 接口

---

### 3. 检查节点状态

#### 接口信息
- **路径**: `GET /api/v1/progress/nodes/{nodeId}/status`
- **认证**: 需要登录
- **限流**: 60次/分钟 (按用户)

#### 请求参数

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| nodeId | Long | 是 | 节点ID，必须大于0 |

#### 请求头
```
token: your-auth-token
```

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "data": {
    "nodeId": 456,
    "completed": true
  },
  "timestamp": 1703001234567
}
```

#### 业务说明
- 返回节点的ID和完成状态
- `completed` 为 true 表示已完成，false 表示未完成

---

## 二、课程进度管理

### 4. 开始学习课程

#### 接口信息
- **路径**: `POST /api/v1/progress/courses/{courseId}/start`
- **认证**: 需要登录
- **限流**: 60次/分钟 (按用户)

#### 请求参数

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| courseId | Long | 是 | 课程ID，必须大于0 |

#### 请求头
```
token: your-auth-token
```

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "courseId": 123,
    "learning": true
  },
  "timestamp": 1703001234567
}
```

**失败 - 课程已开始学习 (1611)**:
```json
{
  "code": 1611,
  "message": "课程已开始学习",
  "timestamp": 1703001234567
}
```

**失败 - 课程不存在 (1007)**:
```json
{
  "code": 1007,
  "message": "课程不存在",
  "timestamp": 1703001234567
}
```

#### 业务说明
- 创建用户与课程的学习关系
- 初始进度为 0%
- 如果已经开始学习，会返回错误码 1611

---

### 5. 取消学习课程

#### 接口信息
- **路径**: `DELETE /api/v1/progress/courses/{courseId}/start`
- **认证**: 需要登录
- **限流**: 60次/分钟 (按用户)

#### 请求参数

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| courseId | Long | 是 | 课程ID，必须大于0 |

#### 请求头
```
token: your-auth-token
```

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "courseId": 123,
    "learning": false
  },
  "timestamp": 1703001234567
}
```

**失败 - 课程尚未开始学习 (1612)**:
```json
{
  "code": 1612,
  "message": "课程尚未开始学习",
  "timestamp": 1703001234567
}
```

#### 业务说明
- 删除用户与课程的学习关系
- 如果课程尚未开始学习，会返回错误码 1612

---

### 6. 获取课程进度

#### 接口信息
- **路径**: `GET /api/v1/progress/courses/{courseId}`
- **认证**: 需要登录
- **限流**: 60次/分钟 (按用户)

#### 请求参数

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| courseId | Long | 是 | 课程ID，必须大于0 |

#### 请求头
```
token: your-auth-token
```

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "userId": 100,
    "courseId": 123,
    "progressPercent": 75,
    "startedAt": "2024-01-01 00:00:00",
    "completedAt": null,
    "createdAt": "2024-01-01 00:00:00",
    "updatedAt": "2024-01-15 10:30:00",
    "course": {
      "id": 123,
      "name": "Java 从入门到精通"
    }
  },
  "timestamp": 1703001234567
}
```

**失败 - 学习记录不存在 (1602)**:
```json
{
  "code": 1602,
  "message": "课程学习记录不存在",
  "timestamp": 1703001234567
}
```

#### 业务说明
- 返回用户对该课程的详细学习进度
- 包含课程的基本信息(id 和 name)
- progressPercent 范围 0-100

---

### 6. 获取所有课程进度

#### 接口信息
- **路径**: `GET /api/v1/progress/courses`
- **认证**: 需要登录
- **限流**: 60次/分钟 (按用户)

#### 请求参数

**Query参数**:
| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| lastId | Long | 否 | 0 | 分页游标，上一页最后一条记录的ID |

#### 请求示例
```
GET /api/v1/progress/courses?lastId=50
```

#### 请求头
```
token: your-auth-token
```

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 51,
      "userId": 100,
      "courseId": 123,
      "progressPercent": 75,
      "state": 1,
      "startedAt": "2024-01-01 00:00:00",
      "completedAt": null,
      "createdAt": "2024-01-01 00:00:00",
      "updatedAt": "2024-01-15 10:30:00",
      "course": {
        "id": 123,
        "name": "Java 从入门到精通"
      }
    },
    {
      "id": 52,
      "userId": 100,
      "courseId": 124,
      "progressPercent": 30,
      "state": 1,
      "startedAt": "2024-01-10 00:00:00",
      "completedAt": null,
      "createdAt": "2024-01-10 00:00:00",
      "updatedAt": "2024-01-12 15:20:00",
      "course": {
        "id": 124,
        "name": "Python 数据分析"
      }
    }
  ],
  "timestamp": 1703001234567
}
```

#### 业务说明
- 返回当前用户所有正在学习的课程
- 支持基于ID的游标分页
- 按 ID 倒序排列（最新的在前）
- 每页返回数量由系统配置决定

---

### 7. 更新课程进度

#### 接口信息
- **路径**: `PUT /api/v1/progress/courses/{courseId}`
- **认证**: 需要登录
- **限流**: 60次/分钟 (按用户)

#### 请求参数

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| courseId | Long | 是 | 课程ID，必须大于0 |

**Body (JSON)**:
```json
{
  "progressPercent": 85
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| progressPercent | Integer | 是 | @NotNull, @Min(0), @Max(100) | 进度百分比，0-100 |

#### 请求头
```
token: your-auth-token
```

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "userId": 100,
    "courseId": 123,
    "progressPercent": 85,
    "state": 1,
    "startedAt": "2024-01-01 00:00:00",
    "completedAt": null,
    "createdAt": "2024-01-01 00:00:00",
    "updatedAt": "2024-01-16 09:15:00",
    "course": {
      "id": 123,
      "name": "Java 从入门到精通"
    }
  },
  "timestamp": 1703001234567
}
```

**失败 - 课程不存在 (1007)**:
```json
{
  "code": 1007,
  "message": "课程不存在",
  "timestamp": 1703001234567
}
```

**失败 - 学习记录不存在 (1602)**:
```json
{
  "code": 1602,
  "message": "课程学习记录不存在",
  "timestamp": 1703001234567
}
```

**失败 - 进度值超出范围 (1603)**:
```json
{
  "code": 1603,
  "message": "进度百分比必须在0-100之间",
  "timestamp": 1703001234567
}
```

#### 业务说明
- 手动更新课程学习进度
- 会自动更新 updatedAt 时间
- 进度必须在 0-100 之间
- 也可以通过完成节点来自动更新进度

---

### 8. 删除课程进度

#### 接口信息
- **路径**: `DELETE /api/v1/progress/courses/{courseId}`
- **认证**: 需要登录
- **限流**: 60次/分钟 (按用户)

#### 请求参数

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| courseId | Long | 是 | 课程ID，必须大于0 |

#### 请求头
```
token: your-auth-token
```

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "courseId": 123,
    "learning": false
  },
  "timestamp": 1703001234567
}
```

**失败 - 学习记录不存在 (1602)**:
```json
{
  "code": 1602,
  "message": "课程学习记录不存在",
  "timestamp": 1703001234567
}
```

#### 业务说明
- 删除用户与课程的学习关系
- 同时会删除该课程下所有节点的完成记录
- 删除后如需继续学习，需要重新开始

**注意**:
- 此接口与 `DELETE /api/v1/progress/courses/{courseId}/start` 的区别:
  - `/start`: 只取消学习记录
  - `/{courseId}`: 深度删除,包括所有节点完成记录

---

### 9. 标记课程完成

#### 接口信息
- **路径**: `POST /api/v1/progress/courses/{courseId}/complete`
- **认证**: 需要登录
- **限流**: 60次/分钟 (按用户)

#### 请求参数

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| courseId | Long | 是 | 课程ID，必须大于0 |

#### 请求头
```
token: your-auth-token
```

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "courseId": 123,
    "userId": 100,
    "completed": true,
    "completedAt": "2024-01-16 12:00:00",
    "progressPercent": 100
  },
  "timestamp": 1703001234567
}
```

#### 业务说明
- 标记整个课程为已完成
- 进度自动设置为 100%
- 会记录完成时间
- 可能触发成就或证书生成（具体看业务配置）

---

## 三、路线图进度管理

### 10. 开始学习路线图

#### 接口信息
- **路径**: `POST /api/v1/progress/roadmaps/{roadmapId}/start`
- **认证**: 需要登录
- **限流**: 60次/分钟 (按用户)

#### 请求参数

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| roadmapId | Long | 是 | 路线图ID，必须大于0 |

#### 请求头
```
token: your-auth-token
```

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "roadmapId": 200,
    "learning": true
  },
  "timestamp": 1703001234567
}
```

**失败 - 路线图已开始学习 (1613)**:
```json
{
  "code": 1613,
  "message": "路线图已开始学习",
  "timestamp": 1703001234567
}
```

**失败 - 路线图不存在 (1501)**:
```json
{
  "code": 1501,
  "message": "路线图不存在",
  "timestamp": 1703001234567
}
```

#### 业务说明
- 创建用户与路线图的学习关系
- 初始进度为 0%
- 如果已经开始学习，会返回错误码 1613

---

### 11. 取消学习路线图

#### 接口信息
- **路径**: `DELETE /api/v1/progress/roadmaps/{roadmapId}/start`
- **认证**: 需要登录
- **限流**: 60次/分钟 (按用户)

#### 请求参数

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| roadmapId | Long | 是 | 路线图ID，必须大于0 |

#### 请求头
```
token: your-auth-token
```

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "roadmapId": 200,
    "learning": false
  },
  "timestamp": 1703001234567
}
```

**失败 - 路线图尚未开始学习 (1614)**:
```json
{
  "code": 1614,
  "message": "路线图尚未开始学习",
  "timestamp": 1703001234567
}
```

#### 业务说明
- 删除用户与路线图的学习关系
- 如果路线图尚未开始学习，会返回错误码 1614

---

### 12. 获取路线图进度

#### 接口信息
- **路径**: `GET /api/v1/progress/roadmaps/{roadmapId}`
- **认证**: 需要登录
- **限流**: 60次/分钟 (按用户)

#### 请求参数

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| roadmapId | Long | 是 | 路线图ID，必须大于0 |

#### 请求头
```
token: your-auth-token
```

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "userId": 100,
    "roadmapId": 200,
    "progressPercent": 60,
    "state": 1,
    "startedAt": "2024-01-01 00:00:00",
    "completedAt": null,
    "createdAt": "2024-01-01 00:00:00",
    "updatedAt": "2024-01-15 14:20:00",
    "roadmap": {
      "id": 200,
      "description": "后端开发完整学习路线",
      "creatorId": 50,
      "state": 2,
      "content": "{...}"
    }
  },
  "timestamp": 1703001234567
}
```

**失败 - 学习记录不存在 (1601)**:
```json
{
  "code": 1601,
  "message": "学习记录不存在",
  "timestamp": 1703001234567
}
```

#### 业务说明
- 返回用户对该路线图的详细学习进度
- 包含路线图的基本信息
- progressPercent 范围 0-100

---

### 12. 获取所有路线图进度

#### 接口信息
- **路径**: `GET /api/v1/progress/roadmaps`
- **认证**: 需要登录
- **限流**: 60次/分钟 (按用户)

#### 请求参数
无

#### 请求头
```
token: your-auth-token
```

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "userId": 100,
      "roadmapId": 200,
      "progressPercent": 60,
      "state": 1,
      "startedAt": "2024-01-01 00:00:00",
      "completedAt": null,
      "createdAt": "2024-01-01 00:00:00",
      "updatedAt": "2024-01-15 14:20:00",
      "roadmap": {
        "id": 200,
        "professionName": "后端开发",
        "nodeCount": 12
      }
    },
    {
      "id": 2,
      "userId": 100,
      "roadmapId": 201,
      "progressPercent": 25,
      "state": 1,
      "startedAt": "2024-01-10 00:00:00",
      "completedAt": null,
      "createdAt": "2024-01-10 00:00:00",
      "updatedAt": "2024-01-12 09:30:00",
      "roadmap": {
        "id": 201,
        "professionName": "前端开发",
        "nodeCount": 10
      }
    }
  ],
  "timestamp": 1703001234567
}
```

#### 业务说明
- 返回当前用户所有正在学习的路线图
- 按开始时间倒序排列
- 无分页限制（通常路线图数量不会太多）

---

### 13. 更新路线图进度

#### 接口信息
- **路径**: `PUT /api/v1/progress/roadmaps/{roadmapId}`
- **认证**: 需要登录
- **限流**: 60次/分钟 (按用户)

#### 请求参数

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| roadmapId | Long | 是 | 路线图ID，必须大于0 |

**Body (JSON)**:
```json
{
  "progressPercent": 75
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| progressPercent | Integer | 是 | @NotNull, @Min(0), @Max(100) | 进度百分比，0-100 |

#### 请求头
```
token: your-auth-token
```

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "userId": 100,
    "roadmapId": 200,
    "progressPercent": 75,
    "state": 1,
    "startedAt": "2024-01-01 00:00:00",
    "completedAt": null,
    "createdAt": "2024-01-01 00:00:00",
    "updatedAt": "2024-01-16 10:45:00"
  },
  "timestamp": 1703001234567
}
```

**失败 - 进度值超出范围 (1603)**:
```json
{
  "code": 1603,
  "message": "进度百分比必须在0-100之间",
  "timestamp": 1703001234567
}
```

#### 业务说明
- 手动更新路线图学习进度
- 会自动更新 updatedAt 时间
- progressPercent 范围 0-100
- 进度必须在 0-100 之间

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 1002 | 参数异常 |
| 1007 | 资源不存在 |
| 1101 | 用户未登录 |
| 1501 | 路线图不存在 |
| 1601 | 学习记录不存在（路线图） |
| 1602 | 课程学习记录不存在 |
| 1603 | 进度百分比必须在0-100之间 |
| 1609 | 节点已是完成状态 |
| 1610 | 节点已是未完成状态 |
| 1611 | 课程已开始学习 |
| 1612 | 课程尚未开始学习 |
| 1613 | 路线图已开始学习 |
| 1614 | 路线图尚未开始学习 |
| 2301 | 访问过于频繁，请稍后再试 |

---

## 认证说明

所有接口都需要登录认证，请在请求头中携带有效的 token：

```
token: your-auth-token
```

如果未登录或 token 无效，会返回错误码 1101。

详细认证流程请参考[用户管理接口文档](user.md)。

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

### 学习课程的完整流程

1. **开始学习**
   ```
   POST /api/v1/progress/courses/{courseId}/start
   ```

2. **标记节点完成**（多次）
   ```
   POST /api/v1/progress/nodes/{nodeId}/complete
   Body: {"courseId": 123}
   ```
   - 每标记一个节点完成，课程进度会自动更新

3. **手动更新进度**（可选）
   ```
   PUT /api/v1/progress/courses/{courseId}
   Body: {"progressPercent": 50}
   ```

4. **标记课程完成**
   ```
   POST /api/v1/progress/courses/{courseId}/complete
   ```
   - 进度自动设置为 100%

5. **查看进度**（随时）
   ```
   GET /api/v1/progress/courses/{courseId}
   GET /api/v1/progress/courses  # 查看所有
   ```

### 学习路线图的完整流程

1. **开始学习**
   ```
   POST /api/v1/progress/roadmaps/{roadmapId}/start
   ```

2. **更新进度**
   ```
   PUT /api/v1/progress/roadmaps/{roadmapId}
   Body: {"progressPercent": 60}
   ```

3. **查看进度**
   ```
   GET /api/v1/progress/roadmaps/{roadmapId}
   GET /api/v1/progress/roadmaps  # 查看所有
   ```

---

## 注意事项

1. **自动进度计算**: 标记节点完成时，系统会自动计算课程整体进度
2. **进度范围**: 进度百分比必须在 0-100 之间
3. **时间记录**: 系统会自动记录开始时间、最后学习时间、完成时间
4. **关联删除**: 删除课程进度时，相关的节点完成记录也会被删除
5. **重复操作**: 重复标记完成或取消完成会返回错误
6. **并发安全**: 系统保证进度更新的并发安全性
7. **分页查询**: 课程进度列表支持游标分页，路线图列表返回全部（通常数量较少）
