# API 接口路径映射文档

## 现有Client接口 → API v1接口路径映射

### 1. AggregateClient → 按功能拆分

#### 页面聚合功能 → PagesApi
| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `GET /read?courseId=123&path=xxx` | `GET /api/v1/pages/read?courseId=123&path=xxx` | 根据课程和路径读取页面数据 |
| `GET /read?nodeId=123` | `GET /api/v1/pages/read?nodeId=123` | 根据节点ID读取页面数据 |
| `GET /read?postId=123` | `GET /api/v1/pages/read?postId=123` | 根据帖子ID读取页面数据 |
| `GET /read?commentId=123` | `GET /api/v1/pages/read?commentId=123` | 根据评论ID读取页面数据 |

#### 帖子功能 → PostsApi
| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `GET /postings?ids=1,2,3` | `GET /api/v1/posts?ids=1,2,3` | 批量获取帖子 |
| `GET /postings?nodeId=123&lastScore=0.5&lastId=100` | `GET /api/v1/posts?nodeId=123&lastScore=0.5&lastId=100` | 分页获取节点帖子 |

#### 点赞功能 → UpvotesApi
| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `POST /upvote` | `POST /api/v1/upvotes` | 点赞操作 |

#### 内容管理 → ContentsApi
| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `POST /contents` | `POST /api/v1/contents` | 内容操作（选择、固定等） |

#### AI功能 → AiApi
| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `POST /openai` | `POST /api/v1/ai/chat` | AI聊天功能 |

#### 消息功能 → MessagesApi
| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `GET /message/new-course` | `GET /api/v1/messages/course-applications` | 获取课程申请列表 |
| `POST /message/new-course` | `POST /api/v1/messages/course-applications` | 申请课程 |
| `GET /message` | `GET /api/v1/messages` | 获取消息列表 |
| `POST /message/system` | `POST /api/v1/messages/system` | 发送系统消息 |
| `PUT /message/system` | `PUT /api/v1/messages/course-applications/{id}` | 修改课程申请 |

---

### 2. UserClient → UsersApi

#### 用户基础功能
| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `GET /self` | `GET /api/v1/users/current` | 获取当前用户信息 |
| `POST /self` | `PUT /api/v1/users/current` | 修改当前用户信息 |
| `GET /user/{id}` | `GET /api/v1/users/{id}` | 获取用户信息 |
| `GET /user?name=xxx` | `GET /api/v1/users/search?name=xxx` | 搜索用户 |

#### 认证功能
| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `POST /user` | `POST /api/v1/auth/register` | 用户注册 |
| `POST /login` | `POST /api/v1/auth/login` | 用户登录 |
| `POST /user/validate` | `POST /api/v1/auth/validate-email` | 邮箱验证 |

#### 用户内容
| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `GET /user/article` | `GET /api/v1/users/{userId}/posts?type=article` | 获取用户文章 |
| `GET /user/contents` | `GET /api/v1/users/{userId}/posts?type=content` | 获取用户内容 |

#### 订阅功能 → SubscriptionsApi
| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `GET /user/subscription` | `GET /api/v1/users/{userId}/subscriptions` | 获取用户订阅 |
| `POST /user/subscription` | `POST /api/v1/users/current/subscriptions` | 添加订阅 |
| `PUT /user/subscription` | `PUT /api/v1/users/current/subscriptions` | 批量更新订阅 |
| `DELETE /user/subscription` | `DELETE /api/v1/users/current/subscriptions/{courseId}` | 取消订阅 |

#### 关注功能 → FollowsApi
| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `POST /user/follow` | `POST /api/v1/follows` | 关注用户 |
| `DELETE /user/follow` | `DELETE /api/v1/follows/{followeeId}` | 取消关注 |
| `GET /user/followee` | `GET /api/v1/users/{userId}/followees` | 获取关注列表 |

#### 学习进度 → ProgressApi
| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `POST /user/complete/{nodeId}` | `POST /api/v1/progress/nodes/{nodeId}/complete` | 标记节点完成 |
| `DELETE /user/complete/{nodeId}` | `DELETE /api/v1/progress/nodes/{nodeId}/complete` | 取消节点完成 |
| `GET /user/complete/{nodeId}` | `GET /api/v1/progress/nodes/{nodeId}/status` | 检查节点完成状态 |
| `POST /user/complete/course/{courseId}` | `POST /api/v1/progress/courses/{courseId}/complete` | 标记课程完成 |

---

### 3. CourseClient → CoursesApi

| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `GET /course/{id}` | `GET /api/v1/courses/{id}` | 获取课程详情 |
| `GET /course/search?name=xxx` | `GET /api/v1/courses/search?name=xxx` | 搜索课程 |
| `GET /course/list?state=xxx&lastId=123` | `GET /api/v1/courses?state=xxx&lastId=123` | 按状态获取课程列表 |
| `GET /course/list?mainCategory=1&subCategory=2` | `GET /api/v1/courses?mainCategory=1&subCategory=2` | 按分类获取课程 |
| `GET /course/list/approved?parentId=123` | `GET /api/v1/courses?parentId=123&state=approved` | 获取子课程 |
| `GET /course/list?parentId=123` | `GET /api/v1/courses?parentId=123` | 获取所有子课程 |
| `GET /course/hot` | `GET /api/v1/courses/hot` | 热门课程 |
| `GET /course/ranking` | `GET /api/v1/courses/ranking` | 课程排行榜 |
| `POST /course/operate` | `POST /api/v1/admin/courses/{id}/approve` | 课程审核操作 |
| `POST /course` | `POST /api/v1/courses` | 创建课程 |
| `POST /subcourse` | `POST /api/v1/courses/{parentId}/subcourses` | 创建子课程 |
| `PUT /course/{id}` | `PUT /api/v1/admin/courses/{id}` | 修改课程 |

---

### 4. PostClient → PostsApi

| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `POST /posting` | `POST /api/v1/posts` | 创建帖子 |
| `PUT /posting` | `PUT /api/v1/posts/{id}` | 修改帖子 |
| `DELETE /posting` | `DELETE /api/v1/posts/{id}` | 删除帖子 |
| `GET /posting/{id}` | `GET /api/v1/posts/{id}` | 获取帖子详情 |
| `GET /node/{nodeId}/posting` | `GET /api/v1/nodes/{nodeId}/posts` | 获取节点帖子 |
| `GET /node/{nodeId}/postings` | `GET /api/v1/nodes/{nodeId}/posts?lastId=123` | 分页获取节点帖子 |
| `GET /post/censor` | `GET /api/v1/admin/posts/pending` | 获取待审核帖子 |
| `PUT /post` | `PUT /api/v1/admin/posts/{id}/approve` | 审核帖子 |

---

### 5. CommentClient → CommentsApi

| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `POST /comment` | `POST /api/v1/comments` | 创建评论 |
| `PUT /comment` | `PUT /api/v1/admin/comments/{id}/approve` | 审核评论 |
| `GET /comment` | `GET /api/v1/comments?objectId=123&type=1&offsetId=0` | 获取对象评论 |
| `GET /comment/{id}/reply` | `GET /api/v1/comments/{id}/replies?offsetId=0` | 获取评论回复 |
| `GET /comment/censor` | `GET /api/v1/admin/comments/pending` | 获取待审核评论 |

---

### 6. MessageClient → MessagesApi

| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `POST /message` (未使用) | `POST /api/v1/messages` | 发送消息 |
| `GET /message/{id}` (未使用) | `GET /api/v1/messages/{id}` | 获取消息详情 |
| `GET /message/system` | `GET /api/v1/messages/system` | 获取系统消息 |
| `GET /message/course-apply` | `GET /api/v1/messages/course-applications` | 获取课程申请消息 |
| `POST /message/invite` | `POST /api/v1/messages/invitations` | 发送邀请 |

---

### 7. UserCourseClient → ProgressApi

| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `POST /user/course` | `POST /api/v1/progress/courses/{courseId}/start` | 开始学习课程 |
| `GET /user/course` | `GET /api/v1/progress/courses/{courseId}` | 获取课程进度 |
| `GET /user/course/list` | `GET /api/v1/progress/courses?lastId=123` | 获取所有课程进度 |
| `PUT /user/course` | `PUT /api/v1/progress/courses/{courseId}` | 更新课程进度 |
| `DELETE /user/course` | `DELETE /api/v1/progress/courses/{courseId}` | 删除课程进度 |

---

### 8. RoadmapClient → RoadmapsApi

| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `GET /roadmap/list/{roleId}` | `GET /api/v1/roles/{roleId}/roadmaps?lastId=123` | 获取职业路线图 |
| `PUT /roadmap/{id}` | `PUT /api/v1/roadmaps/{id}` | 更新路线图 |
| `PUT /roadmap/{id}/upvote` | `PUT /api/v1/roadmaps/{id}/upvote` | 路线图点赞 |
| `POST /roadmap` | `POST /api/v1/roadmaps` | 创建路线图 |
| `GET /roadmap/{id}` | `GET /api/v1/roadmaps/{id}` | 获取路线图详情 |
| `POST /roadmap/pin` | `POST /api/v1/roadmaps/pin` | 置顶路线图 |

---

### 9. roleClient → rolesApi

| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `GET /role/list?page=1` | `GET /api/v1/roles?page=0&size=20` | 分页获取职业 |
| `GET /role/list?state=1&lastId=123` | `GET /api/v1/roles?state=1&lastId=123` | 按状态获取职业 |
| `GET /role/list?mainCategory=1&lastId=123` | `GET /api/v1/roles?mainCategory=1&lastId=123` | 按主分类获取 |
| `GET /role/list?mainCategory=1&subCategory=2&lastId=123` | `GET /api/v1/roles?mainCategory=1&subCategory=2&lastId=123` | 按分类获取 |
| `GET /role/list/approved` | `GET /api/v1/roles/approved?lastId=123` | 获取已批准职业 |
| `GET /role?id=123` | `GET /api/v1/roles/{id}` | 获取职业详情 |
| `POST /role` | `POST /api/v1/roles` | 创建职业 |
| `PUT /role` | `PUT /api/v1/roles/{id}` | 更新职业 |
| `POST /role/operate` | `POST /api/v1/roles/{id}/approve` | 职业审核操作 |
| `DELETE /role` | `DELETE /api/v1/roles/{id}` | 删除职业 |
| `GET /role/hot` | `GET /api/v1/roles/hot?limit=10` | 热门职业 |

---

### 10. StatsClient → StatsApi

| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `POST /api/stats/view` | `POST /api/v1/stats/views` | 记录访问 |
| `GET /api/stats/user/{userId}/today` | `GET /api/v1/stats/users/{userId}/today` | 用户今日统计 |
| `GET /api/stats/user/{userId}/yesterday` | `GET /api/v1/stats/users/{userId}/yesterday` | 用户昨日统计 |
| `GET /api/stats/user/{userId}/history` | `GET /api/v1/stats/users/{userId}/history?days=7` | 用户历史统计 |
| `GET /api/stats/user/{userId}/period` | `GET /api/v1/stats/users/{userId}/period?days=7` | 用户时间段统计 |
| `GET /api/stats/user/{userId}/all-time` | `GET /api/v1/stats/users/{userId}/all-time` | 用户全部时间统计 |
| `POST /api/stats/sync/manual` | `POST /api/v1/stats/sync/manual` | 手动同步 |
| `GET /api/stats/health` | `GET /api/v1/stats/health` | 健康状态 |
| `POST /api/stats/sync/date` | `POST /api/v1/stats/sync/date?date=xxx` | 同步指定日期 |

---

### 11. UpvoteClient → UpvotesApi

| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| (空接口) | `POST /api/v1/upvotes` | 点赞操作（从AggregateClient迁移） |
| (空接口) | `GET /api/v1/upvotes/status?objectId=123&contentType=1` | 获取点赞状态 |

---

### 12. MemoryCardDeckApi → 记忆卡片管理接口

| 现有接口 | 新接口路径 | 说明 |
|---------|-----------|-----|
| `POST /api/v1/memory/decks/{deckId}/approve` | `POST /api/v1/admin/memory/decks/{deckId}/approve` | 审核通过卡片组（迁移到admin） |
| `POST /api/v1/memory/decks/{deckId}/reject` | `POST /api/v1/admin/memory/decks/{deckId}/approve` | 拒绝卡片组（统一到approve接口，action=reject） |
| `POST /api/v1/memory/decks/{deckId}/ban` | `POST /api/v1/admin/memory/decks/{deckId}/approve` | 屏蔽卡片组（统一到approve接口，action=ban） |
| `POST /api/v1/memory/decks/{deckId}/restore` | `POST /api/v1/admin/memory/decks/{deckId}/approve` | 恢复卡片组（统一到approve接口，action=restore） |

---

## 总结

### 新增的API模块：
1. **PagesApi** - 页面聚合接口（4个read接口）
2. **UsersApi** - 用户管理接口
3. **CoursesApi** - 课程管理接口
4. **PostsApi** - 帖子管理接口
5. **CommentsApi** - 评论管理接口
6. **MessagesApi** - 消息管理接口
7. **ProgressApi** - 学习进度接口
8. **RoadmapsApi** - 路线图接口
9. **rolesApi** - 职业管理接口
10. **StatsApi** - 统计接口
11. **UpvotesApi** - 点赞接口
12. **ContentsApi** - 内容管理接口
13. **AiApi** - AI功能接口
14. **SubscriptionsApi** - 订阅管理接口
15. **FollowsApi** - 关注功能接口
16. **MemoryCardDeckApi** - 记忆卡片管理接口

### 主要改进：
- 统一使用 `/api/v1/` 前缀
- 按资源类型重新组织接口
- 使用RESTful风格的URL
- 保持所有现有功能不变