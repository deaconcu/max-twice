# API v1 接口重构 TODO 清单

## 目标
在保留现有 client 和 web 接口的基础上，创建新的 API v1 接口结构，提供统一、规范的 RESTful API。

## 总体任务分解

### 阶段1：基础结构搭建
- [ ] 创建 `com.prosper.learn.web.v1` 包结构
- [ ] 定义统一的响应格式 `ApiResponse<T>`
- [ ] 创建通用的异常处理器
- [ ] 配置统一的参数验证

### 阶段2：核心API接口实现

#### 2.1 页面聚合接口 (PagesController)
- [ ] 创建 `PagesController.java`
- [ ] 实现 `GET /api/v1/pages/read` (4个重载方法)
  - [ ] `readByPath(courseId, path)`
  - [ ] `readByNode(nodeId)`  
  - [ ] `readByPost(postId)`
  - [ ] `readByComment(commentId)`
- [ ] 实现 `GET /api/v1/pages/postings` - 获取帖子列表
- [ ] 实现 `POST /api/v1/pages/contents` - 内容操作
- [ ] 实现 `POST /api/v1/pages/upvote` - 点赞操作
- [ ] 实现 `POST /api/v1/pages/openai` - AI聊天
- [ ] 实现课程申请相关接口
  - [ ] `GET /api/v1/pages/message/new-course` - 获取申请列表
  - [ ] `POST /api/v1/pages/message/new-course` - 提交申请
- [ ] 实现消息相关接口
  - [ ] `GET /api/v1/pages/message` - 获取消息列表
  - [ ] `POST /api/v1/pages/message/system` - 发送系统消息
  - [ ] `PUT /api/v1/pages/message/system` - 修改课程申请

#### 2.2 用户管理接口 (UsersController)
- [ ] 创建 `UsersController.java`
- [ ] 实现用户基础功能
  - [ ] `GET /api/v1/users/current` - 获取当前用户
  - [ ] `PUT /api/v1/users/current` - 修改当前用户
  - [ ] `GET /api/v1/users/{id}` - 获取用户信息
  - [ ] `GET /api/v1/users/search` - 搜索用户
- [ ] 实现认证功能
  - [ ] `POST /api/v1/auth/register` - 用户注册
  - [ ] `POST /api/v1/auth/login` - 用户登录
  - [ ] `POST /api/v1/auth/validate-email` - 邮箱验证
- [ ] 实现用户内容接口
  - [ ] `GET /api/v1/users/{userId}/posts` - 用户帖子
  - [ ] `GET /api/v1/users/{userId}/contents` - 用户内容

#### 2.3 订阅管理接口 (SubscriptionsController)
- [ ] 创建 `SubscriptionsController.java`
- [ ] 实现订阅功能
  - [ ] `GET /api/v1/users/{userId}/subscriptions` - 获取订阅列表
  - [ ] `POST /api/v1/users/current/subscriptions` - 添加订阅
  - [ ] `PUT /api/v1/users/current/subscriptions` - 批量更新订阅
  - [ ] `DELETE /api/v1/users/current/subscriptions/{courseId}` - 取消订阅

#### 2.4 关注功能接口 (FollowsController)
- [ ] 创建 `FollowsController.java`
- [ ] 实现关注功能
  - [ ] `POST /api/v1/follows` - 关注用户
  - [ ] `DELETE /api/v1/follows/{followeeId}` - 取消关注
  - [ ] `GET /api/v1/users/{userId}/followees` - 获取关注列表

#### 2.5 学习进度接口 (ProgressController)
- [ ] 创建 `ProgressController.java`
- [ ] 实现节点进度功能
  - [ ] `POST /api/v1/progress/nodes/{nodeId}/complete` - 标记节点完成
  - [ ] `DELETE /api/v1/progress/nodes/{nodeId}/complete` - 取消节点完成
  - [ ] `GET /api/v1/progress/nodes/{nodeId}/status` - 检查节点状态
- [ ] 实现课程进度功能
  - [ ] `POST /api/v1/progress/courses/{courseId}/start` - 开始学习课程
  - [ ] `GET /api/v1/progress/courses/{courseId}` - 获取课程进度
  - [ ] `GET /api/v1/progress/courses` - 获取所有课程进度
  - [ ] `PUT /api/v1/progress/courses/{courseId}` - 更新课程进度
  - [ ] `DELETE /api/v1/progress/courses/{courseId}` - 删除课程进度
  - [ ] `POST /api/v1/progress/courses/{courseId}/complete` - 标记课程完成

#### 2.6 课程管理接口 (CoursesController)
- [ ] 创建 `CoursesController.java`
- [ ] 实现课程基础功能
  - [ ] `GET /api/v1/courses/{id}` - 获取课程详情
  - [ ] `GET /api/v1/courses/search` - 搜索课程
  - [ ] `GET /api/v1/courses` - 获取课程列表（支持多种筛选）
  - [ ] `GET /api/v1/courses/hot` - 热门课程
  - [ ] `GET /api/v1/courses/ranking` - 课程排行榜
- [ ] 实现课程管理功能
  - [ ] `POST /api/v1/courses` - 创建课程
  - [ ] `PUT /api/v1/courses/{id}` - 修改课程
  - [ ] `POST /api/v1/courses/{parentId}/subcourses` - 创建子课程
  - [ ] `POST /api/v1/courses/{id}/approve` - 课程审核操作

#### 2.7 帖子管理接口 (PostsController)
- [ ] 创建 `PostsController.java`
- [ ] 实现帖子基础功能
  - [ ] `POST /api/v1/posts` - 创建帖子
  - [ ] `PUT /api/v1/posts/{id}` - 修改帖子
  - [ ] `DELETE /api/v1/posts/{id}` - 删除帖子
  - [ ] `GET /api/v1/posts/{id}` - 获取帖子详情
  - [ ] `GET /api/v1/nodes/{nodeId}/posts` - 获取节点帖子
  - [ ] `GET /api/v1/nodes/{nodeId}/posts/more` - 分页获取节点帖子
- [ ] 实现帖子审核功能
  - [ ] `GET /api/v1/admin/posts/pending` - 获取待审核帖子
  - [ ] `PUT /api/v1/admin/posts/{id}/approve` - 审核帖子

#### 2.8 评论管理接口 (CommentsController)
- [ ] 创建 `CommentsController.java`
- [ ] 实现评论功能
  - [ ] `POST /api/v1/comments` - 创建评论
  - [ ] `GET /api/v1/comments` - 获取评论列表
  - [ ] `GET /api/v1/comments/{id}/replies` - 获取评论回复
- [ ] 实现评论审核功能
  - [ ] `PUT /api/v1/admin/comments/{id}/approve` - 审核评论
  - [ ] `GET /api/v1/admin/comments/pending` - 获取待审核评论

#### 2.9 消息管理接口 (MessagesController)
- [ ] 创建 `MessagesController.java`
- [ ] 实现消息功能
  - [ ] `POST /api/v1/messages` - 发送消息
  - [ ] `GET /api/v1/messages/{id}` - 获取消息详情
  - [ ] `GET /api/v1/messages/system` - 获取系统消息
  - [ ] `GET /api/v1/messages/course-applications` - 获取课程申请消息
  - [ ] `POST /api/v1/messages/invitations` - 发送邀请

#### 2.10 路线图接口 (RoadmapsController)
- [ ] 创建 `RoadmapsController.java`
- [ ] 实现路线图功能
  - [ ] `GET /api/v1/roles/{roleId}/roadmaps` - 获取职业路线图
  - [ ] `PUT /api/v1/roadmaps/{id}` - 更新路线图
  - [ ] `PUT /api/v1/roadmaps/{id}/upvote` - 路线图点赞
  - [ ] `POST /api/v1/roadmaps` - 创建路线图
  - [ ] `GET /api/v1/roadmaps/{id}` - 获取路线图详情
  - [ ] `POST /api/v1/roadmaps/pin` - 置顶路线图

#### 2.11 职业管理接口 (RolesController)
- [ ] 创建 `RolesController.java`
- [ ] 实现职业基础功能
  - [ ] `GET /api/v1/roles` - 获取职业列表（支持多种筛选）
  - [ ] `GET /api/v1/roles/approved` - 获取已批准职业
  - [ ] `GET /api/v1/roles/{id}` - 获取职业详情
  - [ ] `GET /api/v1/roles/hot` - 热门职业
- [ ] 实现职业管理功能
  - [ ] `POST /api/v1/roles` - 创建职业
  - [ ] `PUT /api/v1/roles/{id}` - 更新职业
  - [ ] `DELETE /api/v1/roles/{id}` - 删除职业
  - [ ] `POST /api/v1/roles/{id}/approve` - 职业审核操作

#### 2.12 统计接口 (StatsController)
- [ ] 创建 `StatsController.java`
- [ ] 实现统计功能
  - [ ] `POST /api/v1/stats/views` - 记录访问
  - [ ] `GET /api/v1/stats/users/{userId}/today` - 用户今日统计
  - [ ] `GET /api/v1/stats/users/{userId}/yesterday` - 用户昨日统计
  - [ ] `GET /api/v1/stats/users/{userId}/history` - 用户历史统计
  - [ ] `GET /api/v1/stats/users/{userId}/period` - 用户时间段统计
  - [ ] `GET /api/v1/stats/users/{userId}/all-time` - 用户全部时间统计
  - [ ] `POST /api/v1/stats/sync/manual` - 手动同步
  - [ ] `GET /api/v1/stats/health` - 健康状态
  - [ ] `POST /api/v1/stats/sync/date` - 同步指定日期

### 阶段3：公共组件实现

#### 3.1 响应格式统一
- [ ] 创建 `ApiResponse.java` 统一响应格式
- [ ] 创建 `PagedResponse.java` 分页响应格式
- [ ] 创建业务异常类 `BusinessException.java`
- [ ] 创建错误码枚举 `BusinessErrorCode.java`

#### 3.2 全局异常处理
- [ ] 创建 `GlobalExceptionHandler.java`
- [ ] 处理参数验证异常
- [ ] 处理业务异常
- [ ] 处理系统异常
- [ ] 统一错误响应格式

#### 3.3 参数验证
- [ ] 创建请求 DTO 类
- [ ] 添加参数验证注解
- [ ] 配置验证器

#### 3.4 权限控制
- [ ] 实现基于注解的权限控制
- [ ] 定义角色和权限
- [ ] 添加方法级权限检查

### 阶段4：测试和文档

#### 4.1 单元测试
- [ ] 为每个 Controller 编写单元测试
- [ ] 测试正常流程
- [ ] 测试异常情况
- [ ] 测试参数验证

#### 4.2 集成测试
- [ ] 测试完整的请求流程
- [ ] 测试权限控制
- [ ] 测试数据一致性

#### 4.3 API 文档
- [ ] 配置 Swagger/OpenAPI
- [ ] 添加接口注释和说明
- [ ] 生成在线API文档

### 阶段5：性能优化

#### 5.1 缓存优化
- [ ] 识别可缓存的接口
- [ ] 实现Redis缓存
- [ ] 配置缓存策略

#### 5.2 查询优化
- [ ] 优化数据库查询
- [ ] 实现批量查询
- [ ] 减少N+1查询问题

#### 5.3 并发优化
- [ ] 实现异步调用
- [ ] 优化页面聚合接口的并行处理

## 优先级排序

### 高优先级（必须完成）
1. **PagesController** - 核心的页面聚合功能
2. **UsersController** - 用户基础功能
3. **CoursesController** - 课程核心功能
4. **PostsController** - 帖子功能
5. **统一响应格式和异常处理**

### 中优先级（重要功能）
6. ProgressController - 学习进度
7. CommentsController - 评论功能
8. SubscriptionsController - 订阅功能
9. FollowsController - 关注功能

### 低优先级（扩展功能）
10. RoadmapsController - 路线图
11. RolesController - 职业管理
12. MessagesController - 消息功能
13. StatsController - 统计功能

## 注意事项

### 开发原则
- 保持现有业务逻辑不变，只改变接口形式
- 统一使用 `/api/v1/` 前缀
- 遵循 RESTful 设计原则
- 保持向后兼容性
- 完善的错误处理和参数验证

### 测试要求
- 每个接口都要有对应的测试用例
- 确保新接口与旧接口功能一致
- 性能不能比旧接口差

### 文档要求
- 完整的API文档
- 接口变更记录
- 迁移指南

## 完成标准

- [ ] 所有新接口功能与现有Client接口一致
- [ ] 通过所有单元测试和集成测试
- [ ] API文档完整且准确
- [ ] 性能测试通过
- [ ] 代码审查通过

## 估时评估
- 阶段1：2-3天
- 阶段2：15-20天（按优先级逐步完成）
- 阶段3：3-5天
- 阶段4：5-7天
- 阶段5：3-5天

**总计：28-40天**（可以按优先级分批完成和上线）