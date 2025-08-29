# 完整的 API 结构设计

## API 基础路径
所有 API 以 `/api/v1` 为基础路径，支持版本控制。

## API 设计原则

### 设计策略说明
本API设计采用**混合策略**，结合了资源导向的RESTful接口和页面聚合接口：

- **简单资源操作**：使用标准RESTful接口（CRUD操作）
- **复杂页面数据**：使用专门的聚合接口（多表关联、数据一致性要求高）
- **实时更新数据**：使用独立接口按需刷新

### 页面聚合接口的必要性
像阅读页面这样的复杂场景，涉及：
- 课程、节点、帖子、用户等多表关联
- 动态路径计算和内容加载
- 用户状态（收藏、进度、点赞）的一致性
- 批量查询优化

这种场景**必须使用聚合接口**，分开调用会导致数据不一致、性能问题和复杂的前端逻辑。

## 完整的 API 结构

### 0. 页面聚合接口 (Pages) - 优先级最高
```
# 阅读页面聚合接口（基于现有 AggregateController.read）
GET    /api/v1/pages/read                      # 核心阅读页面数据聚合
       ?courseId=123&path=1-456&nodeId=789&postId=101&commentId=999

# 根据不同入口参数自动计算完整页面数据：
# - commentId: 根据评论定位到对应帖子/节点/课程
# - postId: 根据帖子定位到对应节点/课程  
# - nodeId: 根据节点定位到对应课程
# - courseId + path: 直接定位到课程路径

# 其他复杂页面聚合接口
GET    /api/v1/pages/course-detail/{id}        # 课程详情页聚合数据
GET    /api/v1/pages/user-profile/{id}         # 用户详情页聚合数据
GET    /api/v1/pages/home                      # 首页聚合数据
GET    /api/v1/pages/dashboard                 # 用户仪表板聚合数据
```

**阅读页面聚合接口返回数据结构：**
```json
{
  "code": 200,
  "data": {
    "node": {...},               // 当前节点信息
    "course": {...},             // 当前课程信息  
    "parentCourse": {...},       // 父课程信息
    "subCourseList": [...],      // 子课程列表
    "chosenPosting": {...},      // 选中的帖子
    "fixedPostings": [...],      // 固定帖子列表
    "otherPostings": [...],      // 其他帖子列表
    "toc": [...],               // 课程目录结构
    "tocNodeInfos": {...},       // 目录节点信息映射
    "path": "1-456-789",         // 当前路径
    "users": [...],             // 相关用户信息
    "learning": true,           // 是否正在学习
    "post": {...},              // 特定帖子（如果指定了postId）
    "lastId": 12345             // 最后一个帖子ID（分页用）
  }
}
```

### 1. 用户管理 (Users)
```
GET    /api/v1/users                      # 获取用户列表（管理员）
POST   /api/v1/users                      # 用户注册
GET    /api/v1/users/{id}                 # 获取用户信息
PUT    /api/v1/users/{id}                 # 修改用户信息
DELETE /api/v1/users/{id}                 # 删除用户（管理员）

GET    /api/v1/users/current              # 获取当前用户信息
PUT    /api/v1/users/current              # 修改当前用户信息
GET    /api/v1/users/search               # 搜索用户（按姓名）
       ?name=keyword&page=0&size=20

POST   /api/v1/auth/login                 # 用户登录
POST   /api/v1/auth/logout                # 用户登出
POST   /api/v1/auth/refresh               # 刷新令牌
POST   /api/v1/auth/validate-email        # 邮箱验证
POST   /api/v1/auth/forgot-password       # 忘记密码
POST   /api/v1/auth/reset-password        # 重置密码
```

### 2. 课程管理 (Courses)
```
GET    /api/v1/courses                    # 获取课程列表
       ?category=1&subcategory=2&state=approved&page=0&size=20&sort=hot
POST   /api/v1/courses                    # 创建课程
GET    /api/v1/courses/{id}               # 获取课程详情
PUT    /api/v1/courses/{id}               # 修改课程
DELETE /api/v1/courses/{id}               # 删除课程

GET    /api/v1/courses/hot                # 热门课程排行
GET    /api/v1/courses/ranking            # 课程完整排行榜
GET    /api/v1/courses/categories         # 获取课程分类

# 课程审核（管理员）
POST   /api/v1/courses/{id}/approve       # 批准课程
POST   /api/v1/courses/{id}/reject        # 拒绝课程
       body: {"reason": "拒绝原因"}
POST   /api/v1/courses/{id}/block         # 屏蔽课程

# 子课程
GET    /api/v1/courses/{id}/subcourses    # 获取子课程列表
POST   /api/v1/courses/{id}/subcourses    # 创建子课程

# 课程统计
GET    /api/v1/courses/{id}/stats         # 课程统计信息
```

### 3. 课程节点 (Nodes)
```
GET    /api/v1/courses/{courseId}/nodes   # 获取课程节点列表
POST   /api/v1/courses/{courseId}/nodes   # 创建课程节点
GET    /api/v1/nodes/{id}                 # 获取节点详情
PUT    /api/v1/nodes/{id}                 # 修改节点
DELETE /api/v1/nodes/{id}                 # 删除节点

GET    /api/v1/nodes/{id}/comments        # 获取节点评论
POST   /api/v1/nodes/{id}/comments        # 添加节点评论
```

### 4. 课程目录 (TOC - Table of Contents)
```
GET    /api/v1/courses/{id}/toc           # 获取课程目录
PUT    /api/v1/courses/{id}/toc           # 更新课程目录

# 用户自定义目录
GET    /api/v1/users/{userId}/courses/{courseId}/toc    # 用户课程目录
PUT    /api/v1/users/{userId}/courses/{courseId}/toc    # 更新用户课程目录
```

### 5. 订阅管理 (Subscriptions)
```
GET    /api/v1/users/{userId}/subscriptions              # 获取用户订阅列表
       ?page=0&size=20
POST   /api/v1/users/{userId}/subscriptions              # 添加订阅
       body: {"courseId": 123}
PUT    /api/v1/users/{userId}/subscriptions              # 批量更新订阅
       body: {"courseIds": [1,2,3]}
DELETE /api/v1/users/{userId}/subscriptions/{courseId}   # 取消订阅

GET    /api/v1/users/current/subscriptions               # 当前用户订阅列表
POST   /api/v1/users/current/subscriptions               # 当前用户添加订阅
DELETE /api/v1/users/current/subscriptions/{courseId}    # 当前用户取消订阅
```

### 6. 关注功能 (Follows)
```
GET    /api/v1/users/{userId}/followers   # 获取用户粉丝列表
       ?page=0&size=20
GET    /api/v1/users/{userId}/followees   # 获取用户关注列表
       ?page=0&size=20

POST   /api/v1/follows                    # 关注用户
       body: {"followeeId": 123}
DELETE /api/v1/follows/{followeeId}       # 取消关注

GET    /api/v1/users/current/followers    # 当前用户粉丝
GET    /api/v1/users/current/followees    # 当前用户关注
POST   /api/v1/users/current/follows      # 当前用户关注他人
DELETE /api/v1/users/current/follows/{followeeId}  # 当前用户取消关注
```

### 7. 学习进度 (Progress)
```
GET    /api/v1/users/{userId}/progress                    # 用户总体学习进度
GET    /api/v1/users/{userId}/courses/{courseId}/progress # 用户课程学习进度
GET    /api/v1/users/{userId}/progress/stats              # 用户学习统计

POST   /api/v1/nodes/{nodeId}/complete                    # 标记节点完成
       body: {"courseId": 123}
DELETE /api/v1/nodes/{nodeId}/complete                    # 取消节点完成
       body: {"courseId": 123}
GET    /api/v1/nodes/{nodeId}/completion-status           # 检查节点完成状态

POST   /api/v1/courses/{courseId}/complete                # 标记课程完成
GET    /api/v1/courses/{courseId}/completion-status       # 检查课程完成状态

# 当前用户学习进度
GET    /api/v1/users/current/progress                     # 当前用户总体进度
GET    /api/v1/users/current/courses/{courseId}/progress  # 当前用户课程进度
POST   /api/v1/users/current/nodes/{nodeId}/complete      # 当前用户标记节点完成
DELETE /api/v1/users/current/nodes/{nodeId}/complete      # 当前用户取消节点完成
```

### 8. 帖子管理 (Posts)
```
GET    /api/v1/posts                      # 获取帖子列表
       ?nodeId=123&type=article&page=0&size=20&sort=hot
POST   /api/v1/posts                      # 创建帖子
GET    /api/v1/posts/{id}                 # 获取帖子详情
PUT    /api/v1/posts/{id}                 # 修改帖子
DELETE /api/v1/posts/{id}                 # 删除帖子

GET    /api/v1/users/{userId}/posts       # 获取用户发布的帖子
       ?type=article&page=0&size=20
GET    /api/v1/nodes/{nodeId}/posts       # 获取节点下的帖子

# 帖子统计
GET    /api/v1/posts/{id}/stats           # 帖子统计信息
POST   /api/v1/posts/{id}/view            # 记录帖子浏览
```

### 9. 评论系统 (Comments)
```
GET    /api/v1/comments                   # 获取评论列表
       ?objectId=123&objectType=post&page=0&size=20
POST   /api/v1/comments                   # 创建评论
GET    /api/v1/comments/{id}              # 获取评论详情
PUT    /api/v1/comments/{id}              # 修改评论
DELETE /api/v1/comments/{id}              # 删除评论

GET    /api/v1/comments/{id}/replies      # 获取评论回复
POST   /api/v1/comments/{id}/replies      # 回复评论

GET    /api/v1/users/{userId}/comments    # 获取用户评论
GET    /api/v1/posts/{postId}/comments    # 获取帖子评论
GET    /api/v1/nodes/{nodeId}/comments    # 获取节点评论
```

### 10. 点赞系统 (Upvotes)
```
POST   /api/v1/upvotes                    # 点赞/取消点赞
       body: {"objectId": 123, "objectType": "post", "type": "once"}
GET    /api/v1/upvotes/status             # 获取点赞状态
       ?objectId=123&objectType=post

GET    /api/v1/users/{userId}/upvotes     # 获取用户点赞记录
GET    /api/v1/posts/{postId}/upvotes     # 获取帖子点赞统计
GET    /api/v1/comments/{commentId}/upvotes # 获取评论点赞统计
```

### 11. 消息系统 (Messages)
```
GET    /api/v1/messages                   # 获取消息列表
       ?type=follow&isRead=false&page=0&size=20
POST   /api/v1/messages                   # 发送消息
GET    /api/v1/messages/{id}              # 获取消息详情
PUT    /api/v1/messages/{id}/read         # 标记消息已读
DELETE /api/v1/messages/{id}              # 删除消息

GET    /api/v1/messages/unread-count      # 获取未读消息数量
POST   /api/v1/messages/mark-all-read     # 标记所有消息已读
```

### 12. 职业/专业 (Professions)
```
GET    /api/v1/professions                # 获取职业列表
       ?category=1&subcategory=2&page=0&size=20
POST   /api/v1/professions                # 创建职业
GET    /api/v1/professions/{id}           # 获取职业详情
PUT    /api/v1/professions/{id}           # 修改职业
DELETE /api/v1/professions/{id}           # 删除职业

# 职业审核
POST   /api/v1/professions/{id}/approve   # 批准职业
POST   /api/v1/professions/{id}/reject    # 拒绝职业
```

### 13. 学习路线 (Roadmaps)
```
GET    /api/v1/roadmaps                   # 获取路线列表
       ?professionId=123&page=0&size=20&sort=hot
POST   /api/v1/roadmaps                   # 创建路线
GET    /api/v1/roadmaps/{id}              # 获取路线详情
PUT    /api/v1/roadmaps/{id}              # 修改路线
DELETE /api/v1/roadmaps/{id}              # 删除路线

GET    /api/v1/professions/{id}/roadmaps  # 获取职业路线
GET    /api/v1/users/{userId}/roadmaps    # 获取用户学习路线

# 用户路线进度
GET    /api/v1/users/{userId}/roadmaps/{roadmapId}/progress  # 路线进度
POST   /api/v1/users/{userId}/roadmaps/{roadmapId}/start     # 开始学习路线
POST   /api/v1/users/{userId}/roadmaps/{roadmapId}/complete  # 完成学习路线
```

### 14. 统计信息 (Statistics)
```
# 平台统计
GET    /api/v1/stats/platform             # 平台总体统计
GET    /api/v1/stats/daily                # 每日统计
GET    /api/v1/stats/trends               # 趋势统计

# 用户统计
GET    /api/v1/users/{userId}/stats       # 用户统计信息
GET    /api/v1/users/{userId}/stats/{year} # 用户年度统计

# 内容统计
GET    /api/v1/posts/{postId}/stats       # 帖子统计
GET    /api/v1/courses/{courseId}/stats   # 课程统计
GET    /api/v1/comments/{commentId}/stats # 评论统计
```

### 15. 系统管理 (System/Admin)
```
# 系统配置
GET    /api/v1/admin/config               # 获取系统配置
PUT    /api/v1/admin/config               # 更新系统配置

# 内容审核
GET    /api/v1/admin/pending-courses      # 待审核课程
GET    /api/v1/admin/pending-professions  # 待审核职业
GET    /api/v1/admin/reported-content     # 被举报内容

# 用户管理
GET    /api/v1/admin/users                # 用户管理列表
POST   /api/v1/admin/users/{id}/ban       # 封禁用户
POST   /api/v1/admin/users/{id}/unban     # 解封用户

# 系统监控
GET    /api/v1/admin/system-health        # 系统健康状态
GET    /api/v1/admin/performance-metrics  # 性能指标
```

### 16. 文件上传 (Files)
```
POST   /api/v1/files/upload               # 文件上传
GET    /api/v1/files/{id}                 # 获取文件信息
DELETE /api/v1/files/{id}                 # 删除文件

POST   /api/v1/files/images/upload        # 图片上传
POST   /api/v1/files/documents/upload     # 文档上传
```

### 17. 搜索功能 (Search)
```
GET    /api/v1/search                     # 全局搜索
       ?q=keyword&type=course&page=0&size=20
GET    /api/v1/search/courses             # 搜索课程
GET    /api/v1/search/users               # 搜索用户
GET    /api/v1/search/posts               # 搜索帖子
GET    /api/v1/search/suggestions         # 搜索建议
```

### 18. 聚合操作接口 (Aggregates)
```
# 基于现有 AggregateController 的其他功能
POST   /api/v1/aggregates/postings        # 获取帖子列表（支持批量ID或节点筛选）
       body: {"ids": [1,2,3]} 或 {"nodeId": 123, "lastScore": 0.5, "lastId": 100}

POST   /api/v1/aggregates/upvote          # 点赞操作
       body: {"objectId": 123, "objectType": 1, "type": 2}

POST   /api/v1/aggregates/contents        # 内容操作（选择、固定等）
       body: {"path": "1-456", "courseId": 123, "postingId": 789, "action": 1}

POST   /api/v1/aggregates/chat            # AI聊天助手
       body: {"prompt": "问题内容", "model": "gpt-3.5-turbo"}

POST   /api/v1/aggregates/apply-course    # 申请课程
       body: {"title": "课程名", "summary": "摘要", "explanation": "说明", "parentId": 0}

GET    /api/v1/aggregates/apply-courses   # 获取课程申请列表（管理员）
       ?page=1&length=20

POST   /api/v1/aggregates/system-message  # 发送系统消息
       body: {"type": 1, "userId": 123, "content": "消息内容"}

PUT    /api/v1/aggregates/course-apply/{id} # 修改课程申请状态
       body: {"reply": "回复内容"}
```

## 接口选择指南

### 何时使用页面聚合接口
以下情况**必须使用页面聚合接口**：
- 数据间有强烈的一致性要求
- 涉及复杂的多表关联查询
- 需要动态计算和逻辑处理
- 批量关联查询能显著提升性能
- 前端逻辑会变得过于复杂

**示例场景：**
- 阅读页面：课程+节点+帖子+用户状态的完整数据
- 课程详情：课程信息+子课程+学习进度+相关推荐
- 用户主页：用户信息+学习统计+最近动态

### 何时使用资源导向接口
以下情况**使用标准RESTful接口**：
- 简单的CRUD操作
- 单一资源的操作
- 实时数据更新
- 缓存策略不同的数据

**示例场景：**
- 用户个人信息修改
- 发布新帖子/评论
- 点赞状态更新
- 单独获取课程列表

### 混合使用策略
```javascript
// 页面初次加载：使用聚合接口
const pageData = await getReadPageData({courseId, path, nodeId});
setPageState(pageData);

// 用户交互更新：使用资源接口
const handleUpvote = async (postId) => {
  const result = await upvotePost(postId, voteType);
  updatePostInState(result);
};

// 实时数据刷新：使用资源接口
const handleLoadMorePosts = async () => {
  const morePosts = await getPostsByNode(nodeId, lastScore, lastId);
  appendPostsToState(morePosts);
};
```

## 统一的请求/响应格式

### 请求格式
```json
// 分页参数（所有列表接口）
{
  "page": 0,        // 页码，从0开始
  "size": 20,       // 每页大小，默认20，最大100
  "sort": "createdAt:desc"  // 排序，格式: 字段:方向
}

// 过滤参数示例
{
  "filters": {
    "category": 1,
    "state": "approved",
    "dateRange": {
      "start": "2024-01-01",
      "end": "2024-12-31"
    }
  }
}
```

### 响应格式
```json
// 成功响应
{
  "code": 200,
  "message": "操作成功",
  "data": {...},
  "timestamp": 1640995200000,
  "path": "/api/v1/courses"
}

// 分页响应
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false
  },
  "timestamp": 1640995200000,
  "path": "/api/v1/courses"
}

// 错误响应
{
  "code": 400,
  "message": "参数错误",
  "error": "用户名不能为空",
  "timestamp": 1640995200000,
  "path": "/api/v1/users"
}
```

## HTTP 状态码规范

- `200 OK` - 请求成功
- `201 Created` - 创建成功
- `204 No Content` - 成功但无返回内容
- `400 Bad Request` - 请求参数错误
- `401 Unauthorized` - 未授权
- `403 Forbidden` - 权限不足
- `404 Not Found` - 资源不存在
- `409 Conflict` - 资源冲突
- `422 Unprocessable Entity` - 业务逻辑错误
- `500 Internal Server Error` - 服务器内部错误

## 权限控制

### 角色定义
- `GUEST` - 游客（未登录）
- `USER` - 普通用户
- `CREATOR` - 内容创建者
- `MODERATOR` - 版主
- `ADMIN` - 管理员

### 权限注解示例
```java
@PreAuthorize("hasRole('USER')")                    // 需要登录
@PreAuthorize("hasRole('ADMIN')")                   // 需要管理员权限
@PreAuthorize("@userService.isOwner(#userId)")      // 资源所有者
@PreAuthorize("@courseService.canEdit(#courseId)")  // 自定义权限检查
```

这个完整的 API 结构设计遵循 RESTful 规范，提供了清晰的资源组织方式和统一的接口风格。