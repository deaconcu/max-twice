# 系统模块列表

## 一、业务模块

### 1. learn-user (用户模块)
**核心表**: user, user_profile, verification
**主要功能**:
- 用户注册/登录
- 邮箱验证码
- 用户资料管理
- 角色权限（普通用户、版主、管理员）

### 2. learn-content (内容模块)
**核心表**: course, node, post, roadmap, profession
**主要功能**:
- 课程管理（创建、审核、发布）
- 节点管理（知识点章节）
- 帖子管理（文章型、目录型）
- 学习路线图
- 职业分类

### 3. learn-interaction (互动模块)
**核心表**: comment, upvote, follow, message
**主要功能**:
- 评论系统（支持回复、楼层）
- 点赞系统（twice 和 like 两种类型）
- 关注系统（用户关注用户）
- 私信系统

### 4. learn-memory (记忆模块)
**核心表**: memory_card_deck, memory_card, memory_card_version, user_card_srs, user_course_srs_setting
**主要功能**:
- 记忆卡片组管理
- 记忆卡片管理（支持版本历史）
- 间隔重复学习（SRS/Anki 算法）
- 记忆库管理（按课程组织卡片）
- 学习进度跟踪

### 5. learn-learning (学习模块)
**核心表**: user_course, user_roadmap, user_progress
**主要功能**:
- 用户课程学习记录
- 用户路线图学习记录
- 学习进度计算
- 完成状态跟踪

### 6. learn-analytics (分析模块)
**核心表**: content_stats, content_stats_yearly, user_stats, user_stats_yearly, operation_log
**主要功能**:
- 内容统计（浏览量、点赞数、评论数）
- 用户统计（学习天数、完成课程数）
- 年度数据统计
- 管理员操作日志

---

## 二、基础模块

### 7. learn-application (应用服务层)
**职责**: 跨域业务协调
**主要类**: CourseService, PostService, CommentService, MemoryCardService, UserRoadmapService 等
**功能**:
- 协调多个 DomainService
- DTO 转换
- 事件发布

### 8. learn-web (接口层)
**职责**: RESTful API 接口
**主要类**: 各种 Controller
**功能**:
- 接收 HTTP 请求
- 参数验证
- 权限校验（SaToken）
- 统一响应格式

### 9. learn-shared (共享模块)
**职责**: 通用基础设施
**包含**:
- 异常处理（StatusCode, BusinessException）
- 工具类（时间、字符串、JSON）
- 基础配置（SystemProperties）
- 抽象类（AbstractDataService - 缓存封装）
- 枚举类（ContentState, PostType, VoteType, UserRole）

### 10. learn-infrastructure (基础设施)
**职责**: 外部服务集成
**功能**:
- 邮件服务（验证码、通知）
- 图片上传（OSS/本地存储）
- 短信服务（预留）

### 11. learn-external (外部集成)
**职责**: 第三方服务
**功能**: 预留（支付、第三方登录等）

---

## 三、模块依赖关系

```
┌─────────────────────────────────────────┐
│            learn-web                     │
│         (Controller 层)                  │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│        learn-application                 │
│      (ApplicationService 层)             │
└─────────────────────────────────────────┘
                  ↓
┌──────────┬──────────┬──────────┬─────────┐
│  learn-  │  learn-  │  learn-  │ learn-  │
│  user    │  content │  inter-  │ memory  │
│          │          │  action  │         │
└──────────┴──────────┴──────────┴─────────┘
│  learn-  │  learn-  │
│  learning│ analytics│
└──────────┴──────────┘
(DomainService 层 + DataService 层 + Mapper 层)
                  ↓
┌─────────────────────────────────────────┐
│         learn-shared                     │
│      (通用工具和基础类)                  │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│      learn-infrastructure                │
│        (外部服务集成)                    │
└─────────────────────────────────────────┘
```

**依赖规则**:
- learn-web 可以依赖 learn-application
- learn-application 可以依赖所有领域模块
- 领域模块之间不能相互依赖（通过 learn-application 协调）
- 所有模块都可以依赖 learn-shared
- learn-infrastructure 提供基础设施服务

---

## 四、核心概念对照表

| 概念 | 英文 | 所属模块 | 说明 |
|------|------|----------|------|
| 用户 | User | learn-user | 系统账号 |
| 课程 | Course | learn-content | 学习课程 |
| 节点 | Node | learn-content | 课程的章节单元 |
| 帖子 | Post | learn-content | 课程内容（文章或目录） |
| 路线图 | Roadmap | learn-content | 课程学习路径 |
| 职业 | Profession | learn-content | 职业分类 |
| 评论 | Comment | learn-interaction | 内容评论 |
| 点赞 | Upvote | learn-interaction | 点赞（twice/like） |
| 关注 | Follow | learn-interaction | 用户关注 |
| 卡片组 | MemoryCardDeck | learn-memory | 一组记忆卡片 |
| 卡片 | MemoryCard | learn-memory | 单个记忆卡片 |
| SRS | UserCardSrs | learn-memory | 间隔重复学习状态 |
| 记忆库 | MemoryBank | learn-memory | 用户的学习卡片集合 |
| 学习记录 | UserCourse | learn-learning | 用户学习课程的进度 |
| 内容统计 | ContentStats | learn-analytics | 浏览/点赞/评论数 |

---

## 五、接口路由规划

### 用户相关
- `POST /api/v1/users/register` - 注册
- `POST /api/v1/users/login` - 登录
- `GET /api/v1/users/me` - 当前用户信息
- `PUT /api/v1/users/profile` - 更新资料

### 内容相关
- `GET /api/v1/courses` - 课程列表
- `POST /api/v1/courses` - 创建课程
- `GET /api/v1/courses/{id}` - 课程详情
- `GET /api/v1/nodes/{id}` - 节点详情
- `POST /api/v1/posts` - 发布帖子
- `GET /api/v1/posts` - 帖子列表
- `GET /api/v1/roadmaps` - 路线图列表

### 互动相关
- `POST /api/v1/comments` - 发表评论
- `GET /api/v1/comments` - 评论列表
- `POST /api/v1/upvotes` - 点赞/取消
- `GET /api/v1/upvotes/status` - 点赞状态
- `POST /api/v1/follows/{userId}` - 关注用户
- `DELETE /api/v1/follows/{userId}` - 取消关注

### 记忆相关
- `POST /api/v1/memory/decks` - 创建卡片组
- `POST /api/v1/memory/cards` - 创建卡片
- `PUT /api/v1/memory/cards/{id}` - 更新卡片
- `DELETE /api/v1/memory/cards/{id}` - 删除卡片
- `POST /api/v1/memory/decks/{id}/add` - 添加到记忆库
- `GET /api/v1/memory/review` - 获取待复习卡片
- `POST /api/v1/memory/review` - 提交学习结果

### 学习相关
- `POST /api/v1/progress/courses/{id}/start` - 开始学习课程
- `GET /api/v1/progress/my` - 我的学习进度
- `POST /api/v1/progress/roadmaps/{id}/start` - 开始学习路线图

### 管理相关
- `GET /api/v1/admin/contents` - 待审核内容列表
- `POST /api/v1/admin/contents/{id}/approve` - 审核通过
- `POST /api/v1/admin/contents/{id}/reject` - 审核拒绝
- `POST /api/v1/admin/contents/{id}/ban` - 封禁内容
- `GET /api/v1/admin/logs` - 操作日志

---

## 六、横切功能模块

### 1. 认证与权限控制 (SaToken + 自定义注解)
**位置**: learn-web/config + learn-web/annotation + learn-web/aspect
**核心组件**:
- `SaTokenConfig` - SaToken 配置
- `@RequireRole` - 自定义角色校验注解
- `PermissionAspect` - AOP 切面实现权限检查
- `StpUtil` - SaToken 令牌操作工具类

**功能**:
- Token 生成和验证（SaToken）
- 会话管理（默认7天有效期）
- 角色权限校验（自定义实现）
- 自动续期
- 踢人下线

**使用方式**:
```java
// 登录
StpUtil.login(userId);
String token = StpUtil.getTokenValue();

// 校验登录（SaToken 提供）
@SaCheckLogin
public void needLogin() { }

// 校验角色（自定义注解）
@RequireRole(UserRole.ADMIN)
public void onlyAdmin() { }

@RequireRole(UserRole.MODERATOR)
public void moderatorAndAbove() { }

// 获取当前用户
Long userId = StpUtil.getLoginIdAsLong();

// 登出
StpUtil.logout();
```

**权限层级**:
```
admin (100) > moderator (10) > user (1)

规则: 高角色可以执行低角色的所有操作
```

**实现原理**:
```
@RequireRole 注解 → PermissionAspect 切面拦截
  ↓
1. 从方法参数中查找 @CurrentUser 标注的 UserDO
   或从 StpUtil.getLoginIdAsLong() 获取 userId 查询用户
  ↓
2. 获取注解要求的角色（requiredRole）
  ↓
3. 检查 user.hasRole(requiredRole)
   - UserDO.hasRole() 内部逻辑：user.role >= requiredRole.code
  ↓
4. 权限不足抛出 NotPermissionException
  ↓
5. 全局异常处理器返回 403 错误
```

**类级和方法级注解**:
```java
@RequireRole(UserRole.ADMIN)  // 类级：整个 Controller 都需要 ADMIN
public class AdminUserController {

    @GetMapping("/users")
    @RequireRole(UserRole.MODERATOR)  // 方法级：此接口降级为 MODERATOR
    public ApiResponse<?> getUsers() { }

    @PostMapping("/ban")
    // 未标注方法级注解，继承类级注解，需要 ADMIN
    public ApiResponse<?> banUser() { }
}
```

**注意事项**:
- @RequireRole 支持类级和方法级，方法级优先级更高
- PermissionAspect 避免重复检查：方法有注解时跳过类级检查
- 必须先通过 @SaCheckLogin 登录校验，才能进行角色检查

---

### 2. 接口限流 (Bucket4j + Redis)
**位置**: learn-web/config/RateLimitConfig
**核心组件**:
- `RateLimitConfig` - 限流配置类
- `RateLimitInterceptor` - 限流拦截器
- `@RateLimit` - 限流注解
- Bucket4j 令牌桶算法
- Redisson 分布式实现

**限流策略**:
```java
// 基于用户ID限流
@RateLimit(capacity = 10, refillTokens = 10, refillDuration = 60)
// 含义: 容量10个令牌，每60秒补充10个

// 基于IP限流
@RateLimit(keyType = "IP", capacity = 20, refillDuration = 60)
```

**工作原理**:
```
1. 请求到达 → 拦截器检查是否有 @RateLimit 注解
2. 提取限流 key（userId 或 IP）
3. 从 Redis 获取该 key 的令牌桶
4. 尝试消费1个令牌:
   - 成功 → 放行请求
   - 失败 → 返回 429 Too Many Requests
5. 令牌桶按配置的速率自动补充
```

**典型配置**:
```
登录接口: 5次/分钟 (防暴力破解)
发帖接口: 10次/分钟 (防刷帖)
评论接口: 20次/分钟
查询接口: 60次/分钟
```

**缓存**:
- 令牌桶状态存储在 Redis
- 缓存键: `rateLimitBuckets:{userId}` 或 `rateLimitBuckets:IP:{ip}`
- TTL: 10分钟（防止 Redis 内存泄漏）

---

### 3. 缓存设计 (Redis + Spring Cache)
**位置**: learn-shared/dataservice/AbstractDataService
**核心组件**:
- `AbstractDataService` - 抽象数据服务（封装缓存逻辑）
- `CacheConfig` - Redis 缓存配置
- Spring Cache 注解（@Cacheable, @CacheEvict）

**缓存架构**:
```
┌─────────────────────────────────────────┐
│         应用层                           │
│    CourseService.getCourse(123)         │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│    AbstractDataService                   │
│    1. 检查缓存 courses:123               │
│    2. 缓存命中 → 返回                    │
│    3. 缓存未命中 → 查询数据库            │
│    4. 写入缓存（TTL 15分钟）             │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│         Redis                            │
│  Key: courses:123                        │
│  Value: {id:123, name:"Java课程", ...}   │
│  TTL: 900秒                              │
└─────────────────────────────────────────┘
```

**缓存空间和 TTL 配置**:
```
users: 30分钟
  - 用户信息变化不频繁
  - 避免频繁查库

courses: 15分钟
  - 课程信息中等变化频率

nodes: 15分钟
  - 节点信息相对稳定

posts: 5分钟
  - 帖子内容可能被编辑
  - 需要较快看到更新

comments: 5分钟
  - 评论更新频繁

roadmaps: 20分钟
  - 路线图相对稳定

professions: 60分钟
  - 职业分类几乎不变

platformStats: 30分钟
  - 统计数据允许一定延迟
```

**缓存更新策略**:

**1. Cache-Aside（旁路缓存）**
```java
// 读操作
public CourseDO getById(Long id) {
    // 先查缓存
    CourseDO course = cache.get("courses:" + id);
    if (course != null) {
        return course;
    }

    // 缓存未命中，查数据库
    course = courseMapper.getById(id);

    // 写入缓存
    if (course != null) {
        cache.put("courses:" + id, course, Duration.ofMinutes(15));
    }

    return course;
}

// 写操作
public void update(CourseDO course) {
    // 先更新数据库
    courseMapper.update(course);

    // 删除缓存（不是更新缓存）
    cache.evict("courses:" + course.getId());
}
```

**2. Write-Through（写穿）**
```java
// 某些关键数据使用
public void updateAndCache(CourseDO course) {
    // 同时更新数据库和缓存
    courseMapper.update(course);
    cache.put("courses:" + course.getId(), course, ttl);
}
```

**缓存 Key 设计**:
```
单个实体: {cacheName}:{id}
  例: users:123, courses:456

列表: {cacheName}:list:{条件哈希}
  例: courses:list:category_1_state_1

关系: {cacheName}:{主体}:{客体}
  例: upvotes:user_123:post_456

计数: {cacheName}:count:{id}
  例: posts:count:views:789
```

**批量查询优化**:
```java
public List<CourseDO> getByIds(List<Long> ids) {
    List<CourseDO> result = new ArrayList<>();
    List<Long> missingIds = new ArrayList<>();

    // 1. 批量从缓存获取
    for (Long id : ids) {
        CourseDO course = cache.get("courses:" + id);
        if (course != null) {
            result.add(course);
        } else {
            missingIds.add(id);
        }
    }

    // 2. 批量查询未命中的
    if (!missingIds.isEmpty()) {
        List<CourseDO> fromDb = courseMapper.getByIds(missingIds);
        result.addAll(fromDb);

        // 3. 批量写入缓存
        for (CourseDO course : fromDb) {
            cache.put("courses:" + course.getId(), course, ttl);
        }
    }

    return result;
}
```

**缓存穿透防护**:
```java
// 场景: 恶意查询不存在的 ID

// 方案1: 缓存空值（短TTL）
public CourseDO getById(Long id) {
    CourseDO course = cache.get("courses:" + id);
    if (course == EMPTY_PLACEHOLDER) {
        return null;  // 缓存的空值
    }
    if (course != null) {
        return course;
    }

    course = courseMapper.getById(id);
    if (course == null) {
        // 缓存空值，TTL 1分钟
        cache.put("courses:" + id, EMPTY_PLACEHOLDER, Duration.ofMinutes(1));
    } else {
        cache.put("courses:" + id, course, Duration.ofMinutes(15));
    }

    return course;
}

// 方案2: 布隆过滤器（未实现）
```

**缓存雪崩防护**:
```java
// 问题: 大量 key 同时过期导致数据库压力

// 方案: TTL 加随机偏移
Duration ttl = Duration.ofMinutes(15)
    .plusSeconds(ThreadLocalRandom.current().nextInt(60));
```

**缓存一致性**:
```
强一致性:
  - 更新数据库后立即删除缓存
  - 下次读取时重新加载

最终一致性:
  - 依赖 TTL 自然过期
  - 适用于统计数据等对实时性要求不高的场景

事件驱动:
  - 数据更新时发布事件
  - 监听器清除相关缓存
```

**特殊缓存场景**:

**1. 热点数据**:
```java
// 点赞数、浏览量等高频更新数据
// 不直接更新数据库，先累计在 Redis

// 增加浏览量（仅更新缓存）
redis.incr("post:views:" + postId);

// 定时任务（每5分钟）批量回写数据库
@Scheduled(fixedRate = 300000)
public void syncViewsToDB() {
    // 获取所有 post:views:* 的值
    // 批量 UPDATE post SET views = views + ? WHERE id = ?
    // 删除 Redis 中的临时计数
}
```

**2. 分布式锁**:
```java
// 防止缓存击穿（热点 key 过期瞬间大量请求）
public CourseDO getById(Long id) {
    CourseDO course = cache.get("courses:" + id);
    if (course != null) {
        return course;
    }

    // 获取分布式锁
    RLock lock = redisson.getLock("lock:course:" + id);
    try {
        lock.lock(5, TimeUnit.SECONDS);

        // 双重检查
        course = cache.get("courses:" + id);
        if (course != null) {
            return course;
        }

        // 查询数据库
        course = courseMapper.getById(id);
        cache.put("courses:" + id, course, ttl);

        return course;
    } finally {
        lock.unlock();
    }
}
```

---

### 4. 全局异常处理
**位置**: learn-web/exception/GlobalExceptionHandler
**核心组件**:
- `@RestControllerAdvice` - 全局异常处理器
- `BusinessException` - 业务异常基类
- `StatusCode` - 错误码枚举

**异常处理流程**:
```
Controller 抛出异常
    ↓
GlobalExceptionHandler 捕获
    ↓
转换为统一响应格式
    ↓
返回给前端
```

**统一响应格式**:
```json
{
  "code": 1201,
  "message": "课程不存在",
  "data": null
}
```

**错误码体系**:
```
1xxx: 用户相关
  1001 - 未登录
  1116 - 用户不存在
  1103 - 密码错误
  1117 - 用户已被封禁

12xx: 课程相关
  1201 - 课程不存在
  1202 - 课程已存在

13xx: 节点相关
  1302 - 节点不存在

14xx: 评论相关
  1401 - 评论不存在

22xx: 记忆卡片相关
  2201 - 卡片组不存在
  2202 - 卡片不存在
  2203 - 卡片版本不存在
```

---

### 5. 事件驱动架构
**位置**: learn-shared/domain/event
**核心组件**:
- `ApplicationEventPublisher` - Spring 事件发布器
- 各种领域事件类（UserFollowedEvent, ContentCreatedEvent 等）
- `@EventListener` - 事件监听器

**事件流程**:
```
业务操作完成
    ↓
发布领域事件
    ↓
事件监听器异步处理
    ↓
执行副作用操作（消息通知、统计更新等）
```

**典型事件**:
```java
// 用户关注事件
public class UserFollowedEvent {
    private Long followerId;  // 关注者
    private Long followeeId;  // 被关注者
}

// 监听器
@EventListener
public void onUserFollowed(UserFollowedEvent event) {
    // 1. 创建系统消息通知被关注用户
    messageService.createSystemMessage(
        event.getFolloweeId(),
        "用户 X 关注了你"
    );

    // 2. 更新用户统计
    userStatsService.incrementFollowerCount(event.getFolloweeId());
    userStatsService.incrementFollowingCount(event.getFollowerId());
}
```

**优点**:
- 解耦业务逻辑
- 异步处理不阻塞主流程
- 易于扩展新功能

---

### 6. 定时任务
**位置**: 各模块的 scheduled 包
**核心组件**:
- `@Scheduled` - Spring 定时任务注解
- 各种定时任务类

**典型任务**:
```java
// 1. 热度分数计算（每小时）
@Scheduled(cron = "0 0 * * * ?")
public void calculatePostScore() {
    // 根据点赞数、评论数、时间衰减计算分数
    // UPDATE post SET score = 计算结果
}

// 2. 统计数据聚合（每天凌晨）
@Scheduled(cron = "0 0 0 * * ?")
public void aggregateDailyStats() {
    // 汇总昨天的数据到统计表
}

// 3. 过期验证码清理（每小时）
@Scheduled(fixedRate = 3600000)
public void cleanExpiredVerificationCodes() {
    // DELETE FROM verification
    // WHERE created_at < NOW() - INTERVAL 1 DAY
}

// 4. 缓存预热（每天凌晨3点）
@Scheduled(cron = "0 0 3 * * ?")
public void warmupCache() {
    // 预加载热门课程、用户等到缓存
}
```

---

### 7. 数据校验
**位置**: learn-web/controller + DTO
**核心组件**:
- Bean Validation 注解（@NotNull, @Size, @Email 等）
- `@Valid` - 触发校验
- 自定义校验器

**使用方式**:
```java
// DTO
public class CreateCourseRequest {
    @NotBlank(message = "课程名称不能为空")
    @Size(min = 1, max = 200, message = "课程名称长度1-200")
    private String name;

    @Size(max = 5000, message = "描述不超过5000字")
    private String description;

    @NotNull(message = "分类不能为空")
    private Integer mainCategory;
}

// Controller
@PostMapping("/courses")
public ApiResponse<Long> createCourse(@Valid @RequestBody CreateCourseRequest request) {
    // 校验失败会自动抛出 MethodArgumentNotValidException
    // 被全局异常处理器捕获并返回 400 错误
}
```

---

### 8. 日志记录
**位置**: 各模块
**核心组件**:
- Slf4j + Logback
- `@Slf4j` - Lombok 日志注解

**日志级别**:
```
ERROR: 系统错误、异常
WARN:  警告信息、异常情况
INFO:  关键操作日志
DEBUG: 调试信息（生产环境关闭）
```

**日志内容**:
```java
// 业务操作日志
log.info("User {} created course {}", userId, courseId);
log.info("Admin {} approved content {} of type {}", adminId, contentId, type);

// 错误日志
log.error("Failed to send email to {}", email, exception);
log.warn("Invalid login attempt for email: {}", email);

// 性能日志
log.info("Query executed in {}ms: {}", duration, sql);
```

---

## 七、技术栈总结

**后端框架**:
- Spring Boot 3.3.3
- MyBatis（注解方式）
- SaToken（认证授权）
- Redis + Redisson（缓存、分布式锁）
- Bucket4j（限流）
- Spring Cache（缓存抽象）

**数据存储**:
- MySQL 8.0
- Redis

**工具库**:
- Lombok（简化代码）
- Jackson（JSON 处理）
- BCrypt（密码加密）
- JavaMail（邮件发送）

**测试**:
- JUnit 5
- MockMvc
- Spring Test
- AssertJ

---

## 八、业务模块详细设计

### 1. 用户模块 (learn-user)

**数据表**:
- **user**: email(唯一登录凭证)、password(BCrypt加密，约60字符)、name(昵称)、role(1=用户/10=版主/100=管理员)、state(0=正常/1=封禁)、msg_read_time(消息已读时间)
- **verification**: email + code(6位验证码)、used(防重复使用)、created_at(5分钟有效期)

**核心服务**:
- **UserDomainService**:
  - createUser - 检查邮箱未注册，BCrypt加密密码，创建用户默认role=1
  - validateLogin - 查用户、BCrypt验证密码、检查email_validated、检查state，全通过才返回
  - updateProfile - 增量更新昵称/简介/头像
- **UserDataService**:
  - getByEmail - 按邮箱查询缓存15分钟(登录高频)
  - getById - 按ID查询缓存30分钟
  - update - 同时清除users和usersByEmail缓存
- **VerificationDomainService**:
  - sendVerificationCode - 60秒间隔限制，生成6位验证码，发邮件
  - verifyCode - 验证码5分钟有效、一次性使用

**关键流程**:
- **注册**: 用户输入邮箱+密码 → 验证参数 → BCrypt加密密码 → 创建用户(email_validated=false) → 生成验证码 → 发送邮件 → 用户验证邮箱后才能登录
- **登录**: 根据邮箱查用户(走缓存) → BCrypt验证密码 → 检查email_validated → 检查state → 生成token(Redis存储7天)
- **更新资料**: 从token解析userId → 验证参数 → 增量更新 → 清缓存

**设计要点**:
- BCrypt加密每次结果不同(自带salt)，防彩虹表攻击，hash中包含版本+轮数+salt+hash约60字符
- BCrypt验证通过matches()自动提取salt进行比对，无需单独存储salt
- role用数字表示层级，高角色≥低角色权限
- 验证码防刷: 接口限流(5次/分钟)，验证码5分钟过期，used标记防重复使用
- 三级缓存空间: users(按ID)、usersByEmail(按邮箱)、usersByName(按昵称)分别管理
- msg_read_time用单个时间戳判断未读消息，节省存储

### 2. 内容模块 (learn-content)

**数据表**:
- **course**: name、description、creator_id、root_node_id(根节点)、parent_course_id(支持层级)、main_category/sub_category(分类)、state(0=待审核/1=已发布/2=已拒绝/3=已封禁)、reason(拒绝/封禁原因)
- **node**: name、description、course_id、creator_id、state、reason，课程的章节单元
- **post**: node_id、creator_id、type(1=文章/2=目录)、content、state、score(热度分数)、score_calculated_at
- **roadmap**: content(JSON格式课程序列)、content_hash(去重)、profession_id、creator_id、state、score、node_count、deleted_at(软删除)
- **profession**: name、description、state，职业分类（前端工程师、数据科学家等）

**核心服务**:
- **CourseDomainService**:
  - createCourse - 事务中创建课程+根节点，根节点name="课程根目录"自动发布state=1，课程state=0待审核
  - updateCourse - 只能更新name/description，不能改分类
  - approve/reject/ban - 管理员审核，修改state和reason，记录操作日志
- **NodeDomainService**:
  - createNode - 验证课程已发布，检查同课程下节点名不重复，新节点直接发布state=1
  - getByCourseAndName - 查询课程下指定名称的节点，用于目录型帖子去重
- **PostDomainService**:
  - createArticlePost - 验证节点已发布，插入type=1的文章帖子，state=0待审核
  - createContentsPost - 解析JSON章节列表(至少2个)，每个章节检查节点是否存在：存在则复用ID，不存在则创建新节点，最后content存储为"nodeId1,nodeId2,nodeId3"格式
  - processIdToName - 查询时将content的ID列表转换为节点信息JSON数组[{id,name,description}]，仅type=2时执行
  - updatePost - 只允许更新type=1的文章内容，type=2目录不可修改
  - softDelete - 逻辑删除，实际是设置state=3(BANNED)
- **RoadmapDomainService**:
  - createRoadmap - content_hash用于去重，content存储JSON格式的课程ID数组和顺序
  - updateRoadmap - 更新content和description，重新计算content_hash
  - softDelete - 设置deleted_at时间戳，查询时过滤deleted_at IS NULL

**关键流程**:
- **创建课程**: 验证参数 → 事务开启 → 插入course(state=0,root_node_id=0) → 创建根节点(state=1) → 更新course.root_node_id → 提交事务 → 返回courseId等待审核
- **创建文章帖子**: 验证节点存在且state=1 → 插入post(type=1,content=文章内容,state=0) → 等待审核
- **创建目录帖子**: 解析JSON [{章节名:描述}] → 遍历每个章节：查node是否存在(course_id+name) → 存在用旧ID，不存在创建新node → 收集所有nodeId → 拼接"id1,id2,id3" → 插入post(type=2,content=ID列表,state=0)
- **查询目录帖子**: 查post列表 → 过滤type=2 → 解析content分割为ID数组 → 批量查node信息 → 转换content为JSON [{id,name,description}] → 返回
- **审核通过**: 验证管理员权限 → 更新state=1,reason=null → 清缓存 → 记录operation_log → 返回
- **审核拒绝**: 验证管理员权限 → 更新state=2,reason=拒绝原因 → 清缓存 → 记录日志 → 发通知给创建者
- **封禁内容**: 验证管理员权限 → 更新state=3,reason=封禁原因 → 清缓存 → 记录日志 → 可能触发用户处罚(多次违规封号)

**设计要点**:
- 课程创建时自动生成根节点，根节点不显示在前端，用于挂载子节点
- 节点名在同一课程内不能重复，建立(course_id,name)联合索引
- 目录型帖子复用现有节点，避免重复创建，content存ID列表节省空间，查询时动态转换为JSON
- 软删除分两种：deleted_at(用户删除，可恢复) vs state=3(管理员封禁，有原因记录)
- 状态流转: PENDING(0) → PUBLISHED(1) 或 REJECTED(2) → BANNED(3)，不可逆向
- score字段存储热度分数，定时任务根据点赞数/评论数/时间衰减计算，用于排序
- content_hash用于路线图去重，避免重复创建相同内容的路线图

### 3. 互动模块 (learn-interaction)

**数据表**:
- **comment**: content、object_type(1=post/2=course/3=comment等)、object_id(被评论对象ID)、reply_to_comment_id(回复的评论ID,0表示顶级评论)、creator_id、to_user_id(被回复的用户)、state、reason、score(热度分数)
- **upvote**: object_id、object_type、user_id、type(1=twice超级赞/2=like普通赞)，联合唯一索引(user_id,object_id,object_type)
- **follow**: follower_id(关注者)、followee_id(被关注者)、created_at，联合唯一索引(follower_id,followee_id)
- **message**: from_user_id、to_user_id、content、type(1=系统消息/2=用户私信)、created_at

**核心服务**:
- **CommentDomainService**:
  - createComment - 验证object存在，插入评论state=0待审核，reply_to_comment_id>0时验证父评论存在
  - getByObject - 按object_id+object_type查询评论列表，按score降序，支持分页(lastScore+lastId)
  - getChildren - 根据父评论ID列表批量查询子评论，用于展示评论树
- **UpvoteDomainService**:
  - toggleUpvote - 查询是否已点赞：不存在则插入新记录，存在且类型相同则删除(取消)，存在但类型不同则更新type(切换)，返回最终状态
  - getUpvoteStatus - 查询用户对某对象的点赞状态，返回{twiceUpvoted, likeUpvoted}
  - validateObjectExists - 根据object_type调用对应DataService验证对象存在
- **FollowDomainService**:
  - follow - 检查不能关注自己，插入记录(IGNORE重复)，返回是否新增，新增时ApplicationService发布UserFollowedEvent
  - unfollow - 删除关注记录，返回是否删除，删除时发布UserUnfollowedEvent
  - getFollowees - 查询用户关注列表，按created_at降序分页(lastId)
  - isFollowing - 检查A是否关注B，用于前端显示关注状态

**关键流程**:
- **发表评论**: 验证对象存在(post/course等) → 如果是回复评论验证父评论存在 → 插入comment(state=0) → 等待审核
- **点赞twice**: 查询upvote记录(user+object) → 不存在插入type=1 → 存在且type=1删除(取消) → 存在且type=2更新为type=1(切换) → 返回{twiceUpvoted:true/false, likeUpvoted:false}
- **点赞like**: 逻辑同上，type=2
- **关注用户**: 验证被关注用户存在 → 检查不能自己关注自己 → 查询是否已关注：已关注返回false(幂等)，未关注则插入follow记录 → 返回true则发布UserFollowedEvent → 监听器创建系统消息+更新统计
- **取消关注**: 查询关注记录是否存在：不存在返回false(幂等)，存在则删除 → 返回true则发布UserUnfollowedEvent → 更新统计
- **查询评论树**: 查顶级评论(reply_to_comment_id=0) → 提取所有commentId → 批量查子评论(reply_to_comment_id IN ids) → 组装树形结构

**设计要点**:
- upvote用联合唯一索引保证一个用户对一个对象只有一条点赞记录，通过type字段区分twice/like
- comment的score通过定时任务计算，公式考虑点赞数、时间衰减，用于热评排序
- follow用双字段表示关系，查关注列表WHERE follower_id，查粉丝列表WHERE followee_id
- reply_to_comment_id=0表示顶级评论，>0表示回复某评论，支持无限层级但前端只展示2层
- 事件驱动：关注/取消关注发布事件，监听器异步处理消息通知和统计更新，解耦业务逻辑

### 4. 记忆模块 (learn-memory)

**数据表**:
- **memory_card_deck**: post_id、node_id、creator_id、title、description、version(卡片组版本)、state、card_count(卡片数量)、score
- **memory_card**: deck_id、creator_id、current_version_id(当前版本)、state、deleted_at(软删除)
- **memory_card_version**: card_id、version、creator_id、front(问题)、back(答案)、content_hash(去重)、is_active(是否当前版本)
- **user_card_srs**: user_id、card_id、node_id、deck_id、deck_version、card_version_id、type(0=NEW/1=LEARNING/2=REVIEW/3=RELEARNING)、current_step(学习步骤索引)、interval(复习间隔)、review_due_at(下次复习时间)、ease_factor(难度系数2.5)、repetitions(连续正确次数)、lapse_count(遗忘次数)、lapse_old_interval(遗忘前间隔)
- **user_course_srs_setting**: user_id、course_id、daily_new_cards(每日新卡片数)、daily_review_cards(每日复习数)

**核心服务**:
- **MemoryCardDomainService**:
  - createCard - 验证deck存在，创建card记录，创建首个version(version=1,is_active=1)，card.current_version_id指向此version，deck.card_count+1
  - updateCard - 创建新version(version+1)，设置新version.is_active=1，旧version.is_active=0，更新card.current_version_id，保留完整历史
  - deleteCard - 设置card.deleted_at=NOW()，deck.card_count-1，Mapper所有查询都加deleted_at IS NULL过滤
  - getDiff - 查询card的两个version，对比front/back差异返回diff结果
- **MemoryBankDomainService**:
  - addDeckToMemoryBank - 验证deck存在，查询deck下所有card(deleted_at IS NULL)，为每张卡片创建user_card_srs记录(type=0 NEW,interval=0,review_due_at=NOW())，用户可立即开始学习
- **ReviewDomainService**(核心SRS算法):
  - getReviewQueue - 查询user_card_srs WHERE review_due_at<=NOW()按due时间排序，返回待复习卡片
  - submitReview - 根据rating(1=again/2=hard/3=good/4=easy)和当前type调用不同处理器
  - handleNewCard - NEW状态：rating=1/2进入LEARNING，rating=3跳过第一步或毕业，rating=4立即毕业到REVIEW
  - handleLearningCard - LEARNING状态：rating=1回到步骤0，rating=2延长当前步骤，rating=3进入下一步或毕业，rating=4立即毕业，使用学习步骤[1,10]分钟
  - handleReviewCard - REVIEW状态：rating=1遗忘进入RELEARNING保存lapse_old_interval，rating=2间隔×1.2，rating=3间隔×ease_factor，rating=4间隔×ease_factor×1.3并调高ease_factor
  - handleRelearningCard - RELEARNING状态：逻辑同LEARNING，毕业时根据lapse_old_interval×0.7恢复间隔，清空lapse_old_interval

**关键流程**:
- **创建卡片**: 验证deck → 插入memory_card → 插入memory_card_version(version=1) → 更新card.current_version_id → deck.card_count++ → deck.version++
- **修改卡片**: 查当前version → 创建新version(version+1,front=新问题,back=新答案) → 旧version.is_active=0 → 新version.is_active=1 → 更新card.current_version_id → deck.version++
- **添加到记忆库**: 验证deck → 查deck下所有card → 批量插入user_card_srs(type=NEW,interval=0,review_due_at=NOW(),ease_factor=2.5) → 用户可以开始学习
- **学习流程**:
  1. 查询review_due_at<=NOW()的卡片按时间排序
  2. 展示卡片front给用户
  3. 用户思考后点击显示答案看到back
  4. 用户选择质量rating(1-4)
  5. 根据当前type和rating调用Anki算法计算新的interval和review_due_at
  6. type=LEARNING时interval单位是分钟，type=REVIEW时是天
  7. 更新user_card_srs记录，repetitions++或重置为0
  8. 返回下一张卡片或学习完成提示

**设计要点**:
- 卡片版本管理：每次修改创建新version保留历史，current_version_id指向最新，is_active标记当前版本
- 软删除：用户删除卡片设deleted_at，所有Mapper查询加AND deleted_at IS NULL，不影响已学习的SRS记录
- SRS算法基于Anki：4个状态(NEW/LEARNING/REVIEW/RELEARNING)，学习步骤[1,10]分钟，毕业间隔1天，ease_factor动态调整(1.3-2.5)，遗忘后保存old_interval用于恢复
- interval双单位：LEARNING/RELEARNING是分钟，REVIEW是天，review_due_at统一存timestamp便于排序
- deck.version和card_version_id用于检测卡片内容是否更新，前端可提示用户"卡片已更新"

### 5. 学习模块 (learn-learning)

**数据表**:
- **user_course**: user_id、course_id、progress_percent(进度百分比0-100)、state(0=未开始/1=进行中/2=已完成)、started_at、completed_at
- **user_roadmap**: user_id、roadmap_id、progress_percent、state、started_at、completed_at
- **user_progress**: user_id、object_type、object_id、progress_data(JSON存储详细进度)

**核心服务**:
- **UserCourseDomainService**:
  - startCourse - 检查课程存在且已发布，查询是否已有记录：有则抛异常USER_COURSE_ALREADY_STARTED，无则插入user_course(progress_percent=0,state=1,started_at=NOW())
  - updateProgress - 查询课程总节点数和用户完成节点数，计算percent=完成数/总数×100，percent>=95自动设state=2+completed_at，否则state=1
  - cancelCourse - DELETE user_course记录，清除学习记录
- **UserRoadmapDomainService**:
  - startRoadmap - 验证roadmap存在，类似逻辑检查并插入user_roadmap
  - updateProgress - 解析roadmap.content获取课程列表，查询每个课程的user_course.progress_percent，计算总体完成度=平均值，>=95%自动完成
  - cancelRoadmap - DELETE记录
- **UserProgressDomainService**:
  - recordProgress - 记录用户对某个对象的详细进度，progress_data存JSON如{"completed_sections":[1,3,5],"last_position":"00:12:34"}
  - getProgress - 查询进度JSON，前端可恢复用户上次学习位置

**关键流程**:
- **开始学习课程**: 验证课程存在且state=1 → 查询是否已有user_course记录 → 已有则抛异常，无则插入 → 返回成功
- **更新课程进度**:
  1. 用户完成某个节点触发
  2. 查询course下节点总数(state=1)
  3. 查询user完成的节点数(可以从user_progress或业务逻辑计算)
  4. 计算percent = 完成数/总数 × 100
  5. 更新user_course.progress_percent
  6. percent>=95自动设state=2,completed_at=NOW()
- **开始学习路线图**: 验证roadmap存在 → 查询是否已有记录 → 无则插入user_roadmap → 路线图包含多个课程，需逐个完成
- **更新路线图进度**:
  1. 解析roadmap.content获取[course1_id,course2_id,...]
  2. 批量查询这些课程的user_course.progress_percent
  3. 计算平均进度或加权平均
  4. 更新user_roadmap.progress_percent
  5. >=95%自动完成

**设计要点**:
- user_course和user_roadmap只存储汇总进度，详细进度(哪些节点完成)存user_progress的JSON
- 进度计算可以由前端触发(用户点完成)或定时任务批量计算
- state三态：0未开始(预留，实际有记录就是开始了)、1进行中、2已完成
- 完成阈值95%而非100%，允许用户跳过部分内容也能完成课程
- startCourse不使用INSERT IGNORE，重复调用会抛异常提示已开始，避免静默失败

### 6. 分析模块 (learn-analytics)

**数据表**:
- **content_stats**: object_type、object_id、view_count(浏览数)、upvote_count(点赞数)、comment_count(评论数)、share_count(分享数)
- **content_stats_yearly**: 同上增加year字段，用于年度数据归档
- **user_stats**: user_id、study_days(学习天数)、streak_days(连续学习天数)、completed_courses(完成课程数)、created_posts(发布帖子数)、received_upvotes(获得点赞数)
- **user_stats_yearly**: 同上增加year字段
- **operation_log**: operator_id(操作人)、operation_type(APPROVE/REJECT/BAN等)、object_type、object_id、reason、created_at

**核心服务**:
- **ContentStatsDomainService**:
  - incrementViewCount - Redis INCR post:views:{id}，定时任务每5分钟批量回写MySQL，避免高频写DB
  - incrementUpvoteCount - 监听点赞事件，更新content_stats.upvote_count++
  - incrementCommentCount - 监听评论事件，更新comment_count++
  - getStats - 查询某内容的统计数据返回{views,upvotes,comments,shares}
- **UserStatsDomainService**:
  - recordStudyDay - 用户每日首次学习时调用，检查昨天是否学习：是则streak_days++，否则重置为1，study_days++
  - incrementCompletedCourses - 监听课程完成事件，completed_courses++
  - incrementCreatedPosts - 发布帖子时触发，created_posts++
  - incrementReceivedUpvotes - 内容被点赞时触发，received_upvotes++
- **OperationLogDomainService**:
  - logOperation - 管理员操作(审核/封禁)时记录，INSERT operation_log(operator_id,operation_type,object_type,object_id,reason)
  - getLogsByOperator - 查询某管理员的操作历史
  - getLogsByObject - 查询某内容的操作历史(谁审核的、理由是什么)
- **StatsAggregationService**(定时任务):
  - aggregateDailyStats - 每天凌晨执行，汇总昨天的数据到yearly表，清理content_stats中的临时计数
  - syncViewsFromRedis - 每5分钟执行，批量从Redis读取post:views:*的计数，UPDATE content_stats，DEL Redis键

**关键流程**:
- **增加浏览量**: 用户访问内容 → Redis INCR post:views:{id} → 定时任务批量回写DB
- **增加点赞数**: 用户点赞 → UpvoteDomainService发布UpvotedEvent → 监听器调用incrementUpvoteCount → 事务更新content_stats
- **记录学习天数**:
  1. 用户首次学习(当天第一次)触发
  2. 查user_stats获取last_study_date
  3. 如果last_study_date=昨天，streak_days++
  4. 如果间隔>1天，streak_days=1
  5. study_days++，last_study_date=今天
- **操作日志**: 管理员审核内容 → DomainService执行approve/reject/ban → 调用logOperation记录 → 插入operation_log(operator,type,object,reason,time) → 可追溯审核历史

**设计要点**:
- 浏览量等高频操作先写Redis再定时批量同步，避免MySQL写压力
- 统计数据允许一定延迟，使用最终一致性
- 通过事件监听器异步更新统计，不阻塞主业务流程
- yearly表用于数据归档和年度报告，按年分表减少单表数据量
- operation_log用于审计和问题追溯，永久保存不删除
- streak_days连续天数用于激励用户每日学习，中断重置增加粘性

---

## 九、总结

至此，所有业务模块详细设计已完成：
1. **用户模块** - 注册/登录/权限
2. **内容模块** - 课程/节点/帖子/路线图
3. **互动模块** - 评论/点赞/关注
4. **记忆模块** - 卡片/SRS算法
5. **学习模块** - 进度跟踪
6. **分析模块** - 统计/日志

加上横切功能模块：
- 认证与权限控制、接口限流、缓存设计、全局异常处理、事件驱动、定时任务、数据校验、日志记录

整个系统采用DDD分层架构，领域模块间通过ApplicationService协调，使用事件驱动解耦，Redis缓存提升性能，完整的审核流程保证内容质量，SRS算法实现科学的记忆复习。

