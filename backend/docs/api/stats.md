# 统计接口 API 文档

> Controller: `StatsController`
> 基础路径: `/api/v1`
> 限流: 100次/分钟 (按用户)

---

## 接口列表

### 内容访问统计
1. [记录访问](#1-记录访问)

### 用户统计数据
2. [用户今日统计](#2-用户今日统计)
3. [用户昨日统计](#3-用户昨日统计)
4. [用户历史统计](#4-用户历史统计)
5. [用户时间段统计](#5-用户时间段统计)
6. [用户全部时间统计](#6-用户全部时间统计)

### 平台统计
7. [获取平台统计数据](#7-获取平台统计数据)

### 管理功能
8. [手动同步统计](#8-手动同步统计)
9. [同步指定日期](#9-同步指定日期)
10. [健康状态检查](#10-健康状态检查)

---

## 接口详情

### 1. 记录访问

#### 接口信息
- **路径**: `POST /api/v1/stats/views`
- **认证**: 不需要
- **限流**: 100次/分钟 (按用户)

#### 请求参数

**Body (JSON)**:
```json
{
  "articleId": 123,
  "userId": 456,
  "ipAddress": "192.168.1.1"
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| articleId | Long | 是 | @NotNull | 文章ID |
| userId | Long | 是 | @NotNull | 用户ID |
| ipAddress | String | 否 | @Size(max=45) | IP地址，用于防刷 |

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1703001234567
}
```

#### 业务说明
- 记录文章浏览行为
- 数据存储在 Redis 中
- 用于统计文章浏览量

---

### 2. 用户今日统计

#### 接口信息
- **路径**: `GET /api/v1/stats/users/{userId}/today`
- **认证**: 不需要
- **限流**: 100次/分钟 (按用户)

#### 请求参数

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID，必须大于0 |

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 123,
    "views": 50,
    "twices": 10,
    "likes": 5,
    "comments": 3,
    "learningCourses": 2,
    "completedCourses": 1,
    "inProgressProfessions": 1,
    "completedProfessions": 0,
    "followingUsers": 5,
    "followingCourses": 8,
    "followingProfessions": 3,
    "createdArticles": 2,
    "createdIndexs": 0,
    "createdRoadmaps": 1,
    "createdCardDecks": 0,
    "totalLearningItems": 4,
    "totalCreatedItems": 3,
    "lastUpdated": "2024-01-20 10:30:00"
  },
  "timestamp": 1703001234567
}
```

#### 字段说明

**累计统计**:
- `views`: 总浏览量
- `twices`: 总"两次能懂"点赞数
- `likes`: 总"有用"点赞数
- `comments`: 总评论数

**学习进度**:
- `learningCourses`: 正在学习的课程数
- `completedCourses`: 已完成的课程数
- `inProgressProfessions`: 正在学习的职业路径数
- `completedProfessions`: 已完成的职业路径数

**社交关系**:
- `followingUsers`: 关注的用户数
- `followingCourses`: 收藏的课程数
- `followingProfessions`: 关注的职业路径数

**创作内容**:
- `createdArticles`: 创建的文章数
- `createdIndexs`: 创建的索引数
- `createdRoadmaps`: 创建的路线图数
- `createdCardDecks`: 创建的卡片组数

**汇总信息**:
- `totalLearningItems`: 学习项总数
- `totalCreatedItems`: 创作项总数
- `lastUpdated`: 最后更新时间

#### 业务说明
- 返回用户今天的统计数据
- 数据来源于 Redis 和数据库

---

### 3. 用户昨日统计

#### 接口信息
- **路径**: `GET /api/v1/stats/users/{userId}/yesterday`
- **认证**: 不需要
- **限流**: 100次/分钟 (按用户)

#### 请求参数

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID，必须大于0 |

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 123,
    "views": 45,
    "twices": 8,
    "likes": 4,
    "comments": 2,
    "learningCourses": 2,
    "completedCourses": 0,
    "inProgressProfessions": 1,
    "completedProfessions": 0,
    "followingUsers": 5,
    "followingCourses": 8,
    "followingProfessions": 3,
    "createdArticles": 1,
    "createdIndexs": 0,
    "createdRoadmaps": 0,
    "createdCardDecks": 0,
    "totalLearningItems": 3,
    "totalCreatedItems": 1,
    "lastUpdated": "2024-01-19 23:59:59"
  },
  "timestamp": 1703001234567
}
```

#### 业务说明
- 返回用户昨天的统计数据
- 数据来源于持久化的每日统计表

---

### 4. 用户历史统计

#### 接口信息
- **路径**: `GET /api/v1/stats/users/{userId}/history`
- **认证**: 不需要
- **限流**: 100次/分钟 (按用户)

#### 请求参数

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID，必须大于0 |

**Query 参数**:
| 参数 | 类型 | 必填 | 默认值 | 校验规则 | 说明 |
|------|------|------|---------|----------|------|
| days | int | 否 | 7 | @Positive | 统计天数，必须大于0 |

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 123,
    "views": 350,
    "twices": 70,
    "likes": 35,
    "comments": 21,
    "learningCourses": 2,
    "completedCourses": 3,
    "inProgressProfessions": 1,
    "completedProfessions": 1,
    "followingUsers": 5,
    "followingCourses": 8,
    "followingProfessions": 3,
    "createdArticles": 5,
    "createdIndexs": 0,
    "createdRoadmaps": 2,
    "createdCardDecks": 0,
    "totalLearningItems": 7,
    "totalCreatedItems": 7,
    "lastUpdated": "2024-01-20 10:30:00"
  },
  "timestamp": 1703001234567
}
```

#### 业务说明
- 返回用户过去N天的累计统计数据
- 默认查询最近7天
- 数据来源于每日统计表的聚合

---

### 5. 用户时间段统计

#### 接口信息
- **路径**: `GET /api/v1/stats/users/{userId}/period`
- **认证**: 不需要
- **限流**: 100次/分钟 (按用户)

#### 请求参数

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID，必须大于0 |

**Query 参数**:
| 参数 | 类型 | 必填 | 默认值 | 校验规则 | 说明 |
|------|------|------|---------|----------|------|
| days | int | 否 | 7 | @Positive | 统计天数，必须大于0 |

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 123,
    "views": 350,
    "twices": 70,
    "likes": 35,
    "comments": 21,
    "learningCourses": 2,
    "completedCourses": 3,
    "inProgressProfessions": 1,
    "completedProfessions": 1,
    "followingUsers": 5,
    "followingCourses": 8,
    "followingProfessions": 3,
    "createdArticles": 5,
    "createdIndexs": 0,
    "createdRoadmaps": 2,
    "createdCardDecks": 0,
    "totalLearningItems": 7,
    "totalCreatedItems": 7,
    "lastUpdated": "2024-01-20 10:30:00"
  },
  "timestamp": 1703001234567
}
```

#### 业务说明
- 返回用户指定时间段的统计数据（包含每日明细）
- 默认查询最近7天
- 与 history 接口的区别：包含每日明细数据

---

### 6. 用户全部时间统计

#### 接口信息
- **路径**: `GET /api/v1/stats/users/{userId}/all-time`
- **认证**: 不需要
- **限流**: 100次/分钟 (按用户)

#### 请求参数

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID，必须大于0 |

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 123,
    "views": 1250,
    "twices": 250,
    "likes": 125,
    "comments": 80,
    "learningCourses": 2,
    "completedCourses": 15,
    "inProgressProfessions": 1,
    "completedProfessions": 3,
    "followingUsers": 5,
    "followingCourses": 8,
    "followingProfessions": 3,
    "createdArticles": 20,
    "createdIndexs": 5,
    "createdRoadmaps": 8,
    "createdCardDecks": 2,
    "totalLearningItems": 21,
    "totalCreatedItems": 35,
    "lastUpdated": "2024-01-20 10:30:00"
  },
  "timestamp": 1703001234567
}
```

#### 业务说明
- 返回用户所有时间的累计统计数据
- 数据来源于所有每日统计记录的聚合

---

### 7. 获取平台统计数据

#### 接口信息
- **路径**: `GET /api/v1/stats/platform`
- **认证**: 不需要
- **限流**: 100次/分钟 (按用户)

#### 请求参数
无

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "courseCount": 1250,
    "careerPathCount": 85,
    "roadmapCount": 320,
    "knowledgeNodeCount": 5600,
    "articleCount": 2800,
    "lastUpdated": "2024-01-20 10:30:00"
  },
  "timestamp": 1703001234567
}
```

#### 字段说明
- `courseCount`: 平台课程总数
- `careerPathCount`: 职业路径总数
- `roadmapCount`: 学习路线图总数
- `knowledgeNodeCount`: 知识节点总数
- `articleCount`: 文章总数
- `lastUpdated`: 统计数据最后更新时间

#### 业务说明
- 返回平台整体的内容统计数据
- 用于展示平台规模
- 数据实时查询数据库

---

### 8. 手动同步统计

#### 接口信息
- **路径**: `POST /api/v1/stats/sync/manual`
- **认证**: 需要管理员权限
- **限流**: 100次/分钟 (按用户)

#### 请求参数
无

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "同步成功",
  "timestamp": 1703001234567
}
```

#### 业务说明
- 手动触发统计数据同步
- 同步昨日的统计数据到数据库
- 通常用于定时任务失败后的补救操作

---

### 9. 同步指定日期

#### 接口信息
- **路径**: `POST /api/v1/stats/sync/date`
- **认证**: 需要管理员权限
- **限流**: 100次/分钟 (按用户)

#### 请求参数

**Body (JSON)**:
```json
{
  "date": "2024-01-15"
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| date | String | 是 | @NotBlank | 日期，格式: yyyy-MM-dd |

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "指定日期统计数据同步成功",
  "timestamp": 1703001234567
}
```

**失败 - 日期格式错误**:
```json
{
  "code": 1004,
  "message": "无效的日期",
  "timestamp": 1703001234567
}
```

#### 业务说明
- 手动同步指定日期的统计数据
- 用于补录历史数据或修复数据不一致
- 日期格式必须为 yyyy-MM-dd

---

### 10. 健康状态检查

#### 接口信息
- **路径**: `GET /api/v1/stats/health`
- **认证**: 不需要
- **限流**: 100次/分钟 (按用户)

#### 请求参数
无

#### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "OK",
  "timestamp": 1703001234567
}
```

#### 业务说明
- 检查统计系统的健康状态
- 返回系统运行状态信息
- 用于监控和告警

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 1002 | 参数异常 |
| 1004 | 无效的日期 |
| 1101 | 用户未登录 |

---

## 使用示例

### 记录文章访问
```bash
curl -X POST http://localhost:8080/api/v1/stats/views \
  -H "Content-Type: application/json" \
  -d '{
    "articleId": 123,
    "userId": 456,
    "ipAddress": "192.168.1.1"
  }'
```

### 查询用户今日统计
```bash
curl http://localhost:8080/api/v1/stats/users/123/today
```

### 查询用户最近30天统计
```bash
curl http://localhost:8080/api/v1/stats/users/123/history?days=30
```

### 查询平台统计
```bash
curl http://localhost:8080/api/v1/stats/platform
```

---

## 注意事项

1. **数据来源**:
   - 今日统计：Redis + 数据库实时查询
   - 历史统计：每日统计表聚合数据
   - 平台统计：数据库实时统计

2. **数据更新**:
   - 每日统计数据在每天凌晨自动同步
   - 可通过手动同步接口补录数据

3. **性能考虑**:
   - 历史统计查询已优化，避免大量数据扫描
   - 平台统计数据建议配置缓存

4. **权限说明**:
   - 大部分接口无需认证（公开统计）
   - 手动同步接口需要管理员权限

---
