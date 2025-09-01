# API接口迁移映射文档

## 概述

本文档基于后端Controller中的映射注释，详细记录了从旧版API到新版v1 API的接口迁移关系，用于指导前端learnService.js的接口适配。

## 接口迁移映射详表

### 1. 用户认证和管理 (UsersController)

| 功能 | 旧接口 | 新接口 | 方法 | 参数变化 |
|------|--------|--------|------|----------|
| 用户登录 | `POST /login` | `POST /api/v1/auth/login` | POST | 无变化 |
| 用户注册 | `POST /user` | `POST /api/v1/auth/register` | POST | 无变化 |
| 邮箱验证 | `POST /user/validate` | `POST /api/v1/auth/validate-email` | POST | 无变化 |
| 获取当前用户 | `GET /self` | `GET /api/v1/users/current` | GET | 无变化 |
| 更新当前用户 | `POST /self` | `PUT /api/v1/users/current` | PUT | **方法变化**: POST→PUT |
| 获取用户信息 | `GET /user/{id}` | `GET /api/v1/users/{id}` | GET | 无变化 |
| 搜索用户 | `GET /user?name=xxx` | `GET /api/v1/users/search?name=xxx` | GET | 路径变化 |
| 获取用户文章 | `GET /user/article` | `GET /api/v1/users/{userId}/posts?type=article` | GET | **路径参数化** |
| 获取用户内容 | `GET /user/contents` | `GET /api/v1/users/{userId}/posts?type=content` | GET | **路径参数化** |

### 2. 关注功能 (FollowsController)

| 功能 | 旧接口 | 新接口 | 方法 | 参数变化 |
|------|--------|--------|------|----------|
| 关注用户 | `POST /user/follow` | `POST /api/v1/follows` | POST | 参数从form到query |
| 取消关注 | `DELETE /user/follow` | `DELETE /api/v1/follows/{followeeId}` | DELETE | **路径参数化** |
| 获取关注列表 | `GET /user/followee` | `GET /api/v1/users/{userId}/followees` | GET | **路径参数化** |

### 3. 订阅管理 (SubscriptionsController)

| 功能 | 旧接口 | 新接口 | 方法 | 参数变化 |
|------|--------|--------|------|----------|
| 获取订阅 | `GET /user/subscription` | `GET /api/v1/users/{userId}/subscriptions` | GET | **路径参数化** |
| 添加订阅 | `POST /user/subscription` | `POST /api/v1/users/current/subscriptions` | POST | 路径变化 |
| 更新订阅 | `PUT /user/subscription` | `PUT /api/v1/users/current/subscriptions` | PUT | 路径变化 |
| 取消订阅 | `DELETE /user/subscription` | `DELETE /api/v1/users/current/subscriptions/{courseId}` | DELETE | **路径参数化** |

### 4. 职业管理 (ProfessionsController)

| 功能 | 旧接口 | 新接口 | 方法 | 参数变化 |
|------|--------|--------|------|----------|
| 分页获取职业 | `GET /profession/list?page=1` | `GET /api/v1/professions?page=1&size=20` | GET | **新增size参数，page仍从1开始** |
| 获取已批准职业 | `GET /profession/list/approved` | `GET /api/v1/professions/approved?lastId=123` | GET | 分页方式变化 |
| 获取职业详情 | `GET /profession?id=123` | `GET /api/v1/professions/{id}` | GET | **路径参数化** |
| 创建职业 | `POST /profession` | `POST /api/v1/professions` | POST | 无变化 |
| 更新职业 | `PUT /profession` | `PUT /api/v1/professions/{id}` | PUT | **路径参数化** |
| 职业审核 | `POST /profession/operate` | `POST /api/v1/professions/{id}/approve` | POST | **路径参数化** |
| 删除职业 | `DELETE /profession` | `DELETE /api/v1/professions/{id}` | DELETE | **路径参数化** |
| 热门职业 | `GET /profession/hot` | `GET /api/v1/professions/hot?limit=10` | GET | 无变化 |

### 5. 课程管理 (CoursesController)

| 功能 | 旧接口 | 新接口 | 方法 | 参数变化 |
|------|--------|--------|------|----------|
| 获取课程详情 | `GET /course/{id}` | `GET /api/v1/courses/{id}` | GET | 无变化 |
| 搜索课程 | `GET /course/search?name=xxx` | `GET /api/v1/courses/search?name=xxx` | GET | 无变化 |
| 按状态获取课程 | `GET /course/list?state=xxx&lastId=123` | `GET /api/v1/courses?state=xxx&lastId=123` | GET | 无变化 |
| 热门课程 | `GET /course/hot` | `GET /api/v1/courses/hot` | GET | 无变化 |
| 课程排行榜 | `GET /course/ranking` | `GET /api/v1/courses/ranking` | GET | 无变化 |
| 创建课程 | `POST /course` | `POST /api/v1/courses` | POST | 无变化 |
| 更新课程 | `PUT /course/{id}` | `PUT /api/v1/courses/{id}` | PUT | 无变化 |
| 创建子课程 | `POST /subcourse` | `POST /api/v1/courses/{parentId}/subcourses` | POST | **路径参数化** |
| 课程审核 | `POST /course/operate` | `POST /api/v1/courses/{id}/approve` | POST | **路径参数化** |

### 6. 帖子管理 (PostsController)

| 功能 | 旧接口 | 新接口 | 方法 | 参数变化 |
|------|--------|--------|------|----------|
| 批量获取帖子 | `GET /postings?ids=1,2,3` | `GET /api/v1/posts?ids=1,2,3` | GET | 无变化 |
| 创建帖子 | `POST /posting` | `POST /api/v1/posts` | POST | 无变化 |
| 更新帖子 | `PUT /posting` | `PUT /api/v1/posts/{id}` | PUT | **路径参数化** |
| 删除帖子 | `DELETE /posting` | `DELETE /api/v1/posts/{id}` | DELETE | **路径参数化** |
| 获取帖子详情 | `GET /posting/{id}` | `GET /api/v1/posts/{id}` | GET | 无变化 |
| 获取节点帖子 | `GET /node/{nodeId}/posting` | `GET /api/v1/nodes/{nodeId}/posts` | GET | 无变化 |
| 获取待审核帖子 | `GET /post/censor` | `GET /api/v1/admin/posts/pending` | GET | 路径变化 |
| 审核帖子 | `PUT /post` | `PUT /api/v1/admin/posts/{id}/approve` | PUT | **路径参数化** |

### 7. 评论管理 (CommentsController)

| 功能 | 旧接口 | 新接口 | 方法 | 参数变化 |
|------|--------|--------|------|----------|
| 创建评论 | `POST /comment` | `POST /api/v1/comments` | POST | 无变化 |
| 获取对象评论 | `GET /comment` | `GET /api/v1/comments?objectId=123&type=1&offsetId=0` | GET | 无变化 |
| 获取评论回复 | `GET /comment/{id}/reply` | `GET /api/v1/comments/{id}/replies?offsetId=0` | GET | 无变化 |
| 获取待审核评论 | `GET /comment/censor` | `GET /api/v1/admin/comments/pending` | GET | 路径变化 |
| 审核评论 | `PUT /comment` | `PUT /api/v1/admin/comments/{id}/approve` | PUT | **路径参数化** |

### 8. 路线图管理 (RoadmapsController)

| 功能 | 旧接口 | 新接口 | 方法 | 参数变化 |
|------|--------|--------|------|----------|
| 获取职业路线图 | `GET /roadmap/list/{professionId}` | `GET /api/v1/professions/{professionId}/roadmaps?lastId=123` | GET | 路径结构变化 |
| 更新路线图 | `PUT /roadmap/{id}` | `PUT /api/v1/roadmaps/{id}` | PUT | 无变化 |
| 路线图点赞 | `PUT /roadmap/{id}/upvote` | `PUT /api/v1/roadmaps/{id}/upvote` | PUT | 无变化 |
| 创建路线图 | `POST /roadmap` | `POST /api/v1/roadmaps` | POST | 无变化 |
| 获取路线图详情 | `GET /roadmap/{id}` | `GET /api/v1/roadmaps/{id}` | GET | 无变化 |
| 置顶路线图 | `POST /roadmap/pin` | `POST /api/v1/roadmaps/pin` | POST | 无变化 |

### 9. 点赞功能 (UpvotesController)

| 功能 | 旧接口 | 新接口 | 方法 | 参数变化 |
|------|--------|--------|------|----------|
| 点赞操作 | `POST /upvote` | `POST /api/v1/upvotes` | POST | 无变化 |
| 获取点赞状态 | - | `GET /api/v1/upvotes/status?objectId=123&objectType=1` | GET | **新增接口** |

### 10. 消息管理 (MessagesController)

| 功能 | 旧接口 | 新接口 | 方法 | 参数变化 |
|------|--------|--------|------|----------|
| 申请课程 | `POST /message/new-course` | `POST /api/v1/messages/course-applications` | POST | 路径变化 |
| 获取课程申请列表 | `GET /message/new-course` | `GET /api/v1/messages/course-applications` | GET | 路径变化 |
| 获取消息列表 | `GET /message` | `GET /api/v1/messages` | GET | 无变化 |
| 发送系统消息 | `POST /message/system` | `POST /api/v1/messages/system` | POST | 无变化 |
| 修改课程申请 | `PUT /message/system` | `PUT /api/v1/messages/course-applications/{id}` | PUT | **路径参数化** |

### 11. 学习进度 (ProgressController)

| 功能 | 旧接口 | 新接口 | 方法 | 参数变化 |
|------|--------|--------|------|----------|
| 标记节点完成 | `POST /user/complete/{nodeId}` | `POST /api/v1/progress/nodes/{nodeId}/complete` | POST | 路径变化 |
| 取消节点完成 | `DELETE /user/complete/{nodeId}` | `DELETE /api/v1/progress/nodes/{nodeId}/complete` | DELETE | 路径变化 |
| 检查节点状态 | `GET /user/complete/{nodeId}` | `GET /api/v1/progress/nodes/{nodeId}/status` | GET | 路径变化 |
| 开始学习课程 | `POST /user/course` | `POST /api/v1/progress/courses/{courseId}/start` | POST | **路径参数化** |
| 获取课程进度 | `GET /user/course` | `GET /api/v1/progress/courses/{courseId}` | GET | **路径参数化** |
| 获取所有课程进度 | `GET /user/course/list` | `GET /api/v1/progress/courses?lastId=123` | GET | 路径变化 |
| 更新课程进度 | `PUT /user/course` | `PUT /api/v1/progress/courses/{courseId}` | PUT | **路径参数化** |
| 删除课程进度 | `DELETE /user/course` | `DELETE /api/v1/progress/courses/{courseId}` | DELETE | **路径参数化** |
| 标记课程完成 | `POST /user/complete/course/{courseId}` | `POST /api/v1/progress/courses/{courseId}/complete` | POST | 路径变化 |

### 12. 统计功能 (StatsController)

| 功能 | 旧接口 | 新接口 | 方法 | 参数变化 |
|------|--------|--------|------|----------|
| 记录访问 | `POST /api/stats/view` | `POST /api/v1/stats/views` | POST | 路径变化 |
| 用户今日统计 | `GET /api/stats/user/{userId}/today` | `GET /api/v1/stats/users/{userId}/today` | GET | 路径变化 |
| 用户昨日统计 | `GET /api/stats/user/{userId}/yesterday` | `GET /api/v1/stats/users/{userId}/yesterday` | GET | 路径变化 |
| 用户历史统计 | `GET /api/stats/user/{userId}/history` | `GET /api/v1/stats/users/{userId}/history?days=7` | GET | 路径变化 |
| 用户时间段统计 | `GET /api/stats/user/{userId}/period` | `GET /api/v1/stats/users/{userId}/period?days=7` | GET | 路径变化 |
| 用户全部时间统计 | `GET /api/stats/user/{userId}/all-time` | `GET /api/v1/stats/users/{userId}/all-time` | GET | 路径变化 |
| 手动同步 | `POST /api/stats/sync/manual` | `POST /api/v1/stats/sync/manual` | POST | 路径变化 |
| 健康状态 | `GET /api/stats/health` | `GET /api/v1/stats/health` | GET | 路径变化 |
| 同步指定日期 | `POST /api/stats/sync/date` | `POST /api/v1/stats/sync/date?date=xxx` | POST | 路径变化 |

### 13. 页面聚合 (PagesController)

| 功能 | 旧接口 | 新接口 | 方法 | 参数变化 |
|------|--------|--------|------|----------|
| 按课程路径读取 | `GET /read?courseId=123&path=xxx` | `GET /api/v1/pages/read?courseId=123&path=xxx` | GET | 路径变化 |
| 按节点读取 | `GET /read?nodeId=123` | `GET /api/v1/pages/read?nodeId=123` | GET | 路径变化 |
| 按帖子读取 | `GET /read?postId=123` | `GET /api/v1/pages/read?postId=123` | GET | 路径变化 |
| 按评论读取 | `GET /read?commentId=123` | `GET /api/v1/pages/read?commentId=123` | GET | 路径变化 |

### 14. 内容管理 (ContentsController)

| 功能 | 旧接口 | 新接口 | 方法 | 参数变化 |
|------|--------|--------|------|----------|
| 内容操作 | `POST /contents` | `POST /api/v1/contents` | POST | 无变化 |

### 15. AI功能 (AiController)

| 功能 | 旧接口 | 新接口 | 方法 | 参数变化 |
|------|--------|--------|------|----------|
| AI聊天 | `POST /openai` | `POST /api/v1/ai/chat` | POST | 路径变化 |

## 重要变更说明

### 1. 路径参数化变更
以下接口将query参数或body参数改为路径参数：
- 用户ID相关：`/user/{id}` → `/api/v1/users/{id}`
- 职业ID相关：`?id=123` → `/professions/{id}`
- 课程进度：从query参数改为路径参数
- 关注操作：followeeId改为路径参数

### 2. HTTP方法变更
- `POST /self` → `PUT /api/v1/users/current`（更新用户信息）

### 3. 分页参数说明
- **page参数仍从1开始**：虽然注释中写着`page=1 → page=0`，但实际上后端Service会自动处理`(page - 1) * pageSize`的转换
- **新增size参数**：可选参数，默认值为20
- **示例**：`GET /api/v1/professions?page=1&size=20`

### 4. 路径结构重组
- 关注功能：从`/user/follow`移至`/follows`
- 订阅功能：从`/user/subscription`移至`/users/{userId}/subscriptions`
- 进度功能：从`/user/complete`移至`/progress/nodes`
- 统计功能：路径层级调整，增加复数形式

### 5. 新增接口
- `GET /api/v1/upvotes/status` - 获取点赞状态

## 响应格式统一

所有新接口都使用统一的ApiResponse格式：
```json
{
  "code": 200,
  "message": "success",
  "data": {...}
}
```

## 前端适配建议

### 高优先级迁移
1. **用户认证** - 登录注册等核心功能
2. **课程和职业** - 主要业务功能  
3. **学习进度** - 用户体验核心

### 中优先级迁移
1. **帖子评论** - 社区功能
2. **关注订阅** - 用户互动
3. **路线图** - 学习规划

### 低优先级迁移
1. **统计功能** - 数据分析
2. **AI功能** - 辅助功能
3. **内容管理** - 运营功能

### 适配注意事项
1. **参数位置变化**：注意路径参数vs查询参数的变化
2. **分页逻辑**：page参数保持从1开始，新增size参数
3. **HTTP方法**：注意POST/PUT方法的变化
4. **响应解析**：适配新的ApiResponse包装格式
5. **错误处理**：统一的错误响应格式