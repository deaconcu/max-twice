# Max-Twice 项目架构重构文档

## 📋 架构重构方案

### 整体设计原则
- **业务域优先**：按业务功能垂直拆分，而非技术分层
- **实用主义**：避免过度设计，Service层直接使用DO对象
- **边界转换**：只在API边界（Controller层）进行VO↔DO转换
- **事件驱动**：域间通过Spring Events实现解耦通信

### 项目整体结构

```
backend/
├── learn-web/              # 统一Web入口层
│   ├── controller/         # HTTP控制器（统一API入口）
│   ├── vo/                # View Objects（HTTP请求/响应对象）
│   │   ├── request/        # 请求VO
│   │   └── response/       # 响应VO
│   ├── config/            # Web配置（安全、CORS等）
│   ├── exception/         # 全局异常处理
│   └── interceptor/       # 拦截器
│
├── learn-application/      # 跨域应用编排层
│   ├── service/           # 跨域业务流程编排
│   │   ├── PageAppService.java     # 页面数据聚合
│   │   ├── WorkflowAppService.java # 复杂业务流程
│   │   └── IntegrationAppService.java # 系统集成
│   └── dto/               # 应用层传输对象（可选）
│
├── learn-shared/          # 共享基础设施
│   ├── domain/
│   │   ├── event/         # 领域事件定义
│   │   ├── enums/         # 通用枚举
│   │   └── exception/     # 异常定义
│   ├── common/
│   │   ├── constants/     # 常量定义
│   │   ├── utils/         # 工具类
│   │   └── validator/     # 通用校验
│   ├── infrastructure/
│   │   ├── config/        # 基础设施配置
│   │   ├── cache/         # 缓存配置
│   │   └── messaging/     # 事件总线配置
│   └── dataservice/       # 数据访问基础设施
│       ├── AbstractDataService.java   # 通用数据访问抽象（缓存+DB）
│       └── BaseDataService.java       # 数据访问接口定义
│
├── learn-content/         # 内容域（核心域）
│   ├── api/              # 域对外统一接口
│   │   ├── ContentQueryAPI.java
│   │   ├── ContentCommandAPI.java
│   │   ├── ContentQueryAPIImpl.java
│   │   └── ContentCommandAPIImpl.java
│   ├── shared/           # 域内共享服务（协调层）
│   │   ├── ContentDomainService.java
│   │   ├── ContentType.java        # 枚举
│   │   └── ContentValidator.java
│   ├── post/             # 文章子域（扁平结构）
│   │   ├── PostService.java
│   │   ├── PostDataService.java
│   │   ├── PostMapper.java
│   │   └── PostDO.java
│   ├── course/           # 课程子域（包含多个服务）
│   │   ├── CourseService.java
│   │   ├── CourseDataService.java
│   │   ├── CourseMapper.java
│   │   ├── CourseDO.java
│   │   ├── NodeService.java
│   │   ├── NodeDataService.java
│   │   ├── NodeMapper.java
│   │   └── NodeDO.java
│   ├── role/       # 职业子域（扁平结构）
│   │   ├── RoleService.java
│   │   ├── RoleDataService.java
│   │   ├── RoleMapper.java
│   │   └── RoleDO.java
│   └── roadmap/          # 路线图子域（扁平结构）
│       ├── RoadmapService.java
│       ├── RoadmapDataService.java
│       ├── RoadmapMapper.java
│       └── RoadmapDO.java
│
├── learn-interaction/     # 交互域
│   ├── api/              # 域对外统一接口
│   │   ├── InteractionQueryAPI.java
│   │   ├── InteractionCommandAPI.java
│   │   ├── InteractionQueryAPIImpl.java
│   │   └── InteractionCommandAPIImpl.java
│   ├── shared/           # 域内共享服务
│   │   ├── InteractionDomainService.java
│   │   └── InteractionType.java      # 枚举
│   ├── upvote/           # 点赞子域（扁平结构）
│   │   ├── UpvoteService.java
│   │   ├── UpvoteDataService.java
│   │   ├── UpvoteMapper.java
│   │   └── UpvoteDO.java
│   ├── comment/          # 评论子域（扁平结构）
│   │   ├── CommentService.java
│   │   ├── CommentDataService.java
│   │   ├── CommentMapper.java
│   │   └── CommentDO.java
│   ├── bookmark/         # 收藏子域（扁平结构）
│   │   ├── BookmarkService.java
│   │   ├── BookmarkDataService.java
│   │   ├── BookmarkMapper.java
│   │   └── BookmarkDO.java
│   ├── follow/           # 关注子域（扁平结构）
│   │   ├── FollowService.java
│   │   ├── FollowDataService.java
│   │   ├── FollowMapper.java
│   │   └── FollowDO.java
│   └── message/          # 私信子域（扁平结构）
│       ├── MessageService.java
│       ├── MessageDataService.java
│       ├── MessageMapper.java
│       └── MessageDO.java
│
├── learn-learning/        # 学习域
│   ├── api/              # 域对外统一接口
│   │   ├── LearningQueryAPI.java
│   │   ├── LearningCommandAPI.java
│   │   ├── LearningQueryAPIImpl.java
│   │   └── LearningCommandAPIImpl.java
│   ├── shared/           # 域内共享服务
│   │   ├── LearningDomainService.java
│   │   └── LearningStatus.java       # 枚举
│   ├── progress/         # 学习进度子域（扁平结构）
│   │   ├── ProgressService.java
│   │   ├── ProgressDataService.java
│   │   ├── ProgressMapper.java
│   │   └── UserProgressDO.java
│   ├── enrollment/       # 课程注册子域（扁平结构）
│   │   ├── EnrollmentService.java
│   │   ├── EnrollmentDataService.java
│   │   ├── EnrollmentMapper.java
│   │   └── UserCourseDO.java
│   └── achievement/      # 成就系统子域（扁平结构）
│       ├── AchievementService.java
│       ├── AchievementDataService.java
│       ├── AchievementMapper.java
│       ├── AchievementDO.java
│       └── UserAchievementDO.java
│
├── learn-memory/          # 记忆域
│   ├── api/              # 域对外统一接口
│   │   ├── MemoryQueryAPI.java
│   │   ├── MemoryCommandAPI.java
│   │   ├── MemoryQueryAPIImpl.java
│   │   └── MemoryCommandAPIImpl.java
│   ├── shared/           # 域内共享服务
│   │   ├── MemoryDomainService.java
│   │   └── ReviewStatus.java        # 枚举
│   ├── card/             # 记忆卡片子域（扁平结构）
│   │   ├── MemoryCardService.java
│   │   ├── MemoryCardDataService.java
│   │   ├── MemoryCardMapper.java
│   │   └── MemoryCardDO.java
│   ├── deck/             # 卡组管理子域（扁平结构）
│   │   ├── MemoryCardDeckService.java
│   │   ├── MemoryCardDeckDataService.java
│   │   ├── MemoryCardDeckMapper.java
│   │   └── MemoryCardDeckDO.java
│   ├── review/           # 复习算法子域（扁平结构）
│   │   ├── ReviewService.java
│   │   ├── ReviewDataService.java
│   │   ├── ReviewMapper.java
│   │   └── ReviewRecordDO.java
│   └── bank/             # 记忆银行子域（扁平结构）
│       ├── MemoryBankService.java
│       ├── MemoryBankDataService.java
│       ├── MemoryBankMapper.java
│       └── MemoryBankDO.java
│
├── learn-user/            # 用户域
│   ├── api/              # 域对外统一接口
│   │   ├── UserQueryAPI.java
│   │   ├── UserCommandAPI.java
│   │   ├── UserQueryAPIImpl.java
│   │   └── UserCommandAPIImpl.java
│   ├── shared/           # 域内共享服务
│   │   ├── UserDomainService.java
│   │   └── UserStatus.java          # 枚举
│   ├── profile/          # 用户资料子域（扁平结构）
│   │   ├── UserService.java
│   │   ├── UserDataService.java
│   │   ├── UserMapper.java
│   │   └── UserDO.java
│   ├── auth/             # 认证授权子域（扁平结构）
│   │   ├── AuthService.java
│   │   ├── UserTokenDataService.java
│   │   ├── UserTokenMapper.java
│   │   └── UserTokenDO.java
│   └── notification/     # 通知子域（扁平结构）
│       ├── NotificationService.java
│       ├── NotificationDataService.java
│       ├── NotificationMapper.java
│       └── NotificationDO.java
│
└── learn-analytics/       # 分析域
    ├── api/              # 域对外统一接口
    │   ├── AnalyticsQueryAPI.java
    │   ├── AnalyticsCommandAPI.java
    │   ├── AnalyticsQueryAPIImpl.java
    │   └── AnalyticsCommandAPIImpl.java
    ├── shared/           # 域内共享服务
    │   ├── AnalyticsDomainService.java
    │   └── StatsType.java           # 枚举
    ├── stats/            # 统计数据子域（包含多个服务，保留分类）
    │   ├── service/
    │   │   ├── UserStatsService.java
    │   │   ├── ContentStatsService.java
    │   │   └── RedisStatsService.java
    │   ├── dataservice/
    │   │   ├── UserStatsDataService.java
    │   │   └── ContentStatsDataService.java
    │   ├── mapper/
    │   │   ├── UserStatsMapper.java
    │   │   └── ContentStatsMapper.java
    │   └── dataobject/
    │       ├── UserStatsYearlyDO.java
    │       └── ContentStatsYearlyDO.java
    ├── sync/             # 数据同步子域（扁平结构）
    │   ├── DailyStatsService.java
    │   └── StatsScheduler.java
    ├── report/           # 报表分析子域（扁平结构）
    │   ├── ReportService.java
    │   ├── ReportDataService.java
    │   ├── ReportMapper.java
    │   └── ReportDO.java
    └── monitoring/       # 监控指标子域（扁平结构）
        ├── MonitoringService.java
        └── MetricsCollector.java
```

### 业务域划分

#### 核心域
1. **learn-content**：内容管理域
   - 职业(Role) → 路线图(Roadmap) → 课程(Course) → 节点(Node) → 文章(Post)
   - 内容的创建、编辑、发布、管理

2. **learn-learning**：学习行为域
   - 学习进度跟踪、课程注册、成就系统
   - 用户的学习轨迹和行为记录

3. **learn-memory**：记忆强化域
   - 记忆卡片、复习算法、记忆银行
   - 基于遗忘曲线的记忆强化系统

#### 支撑域
4. **learn-interaction**：交互域
   - 用户与内容交互：点赞、评论、收藏
   - 用户与用户交互：关注、私信、@提及

5. **learn-user**：用户域
   - 用户注册、认证、权限管理
   - 用户资料、偏好设置、消息通知

6. **learn-analytics**：分析域
   - 实时统计、历史数据、报表分析
   - 用户行为分析、内容质量分析

### 数据流转设计

#### 标准请求流程
```
HTTP Request (JSON)
    ↓
learn-web/controller: RequestVO → 参数提取
    ↓
learn-application/service: 跨域编排（如需要）
    ↓
各域/api: 接口调用
    ↓
各域/service: 业务处理（直接使用DO）
    ↓
各域/dataservice: 缓存 + 数据访问
    ↓
各域/mapper: MyBatis持久化
    ↓
Database
    ↓
learn-web/controller: DO/结果 → ResponseVO
    ↓
HTTP Response (JSON)
```

#### 分层职责说明

**Controller 层**：
- HTTP 请求/响应处理
- VO ↔ DO 转换（唯一转换边界）
- 参数验证和错误处理

**Service 层**：
- 业务逻辑处理
- 事务管理 (@Transactional)
- 跨实体协调
- 领域事件发布

**DataService 层**：
- 数据访问封装
- **缓存管理**（Spring Cache + Redis）
- 批量查询优化（MGET、Pipeline）
- 缓存穿透保护、预热、降级

**Mapper 层**：
- MyBatis SQL 映射
- 数据库 CRUD 操作

#### 对象使用规范
- **DO对象**：Service层、DataService层直接使用，保持纯净（只有数据字段）
- **VO对象**：只在Controller层使用，负责HTTP请求响应
- **转换边界**：只在Controller层进行VO↔DO转换

#### 跨域通信
```java
// 事件发布
@Service
public class PostService {

    private final PostDataService postDataService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PostDO createPost(PostDO post) {
        // 通过 DataService 保存（自动处理缓存）
        postDataService.insert(post);

        // 发布事件
        eventPublisher.publishEvent(new PostCreatedEvent(
            post.getId(), post.getAuthorId(), post.getTitle()
        ));

        return post;
    }
}

// 事件监听
@Component
public class AnalyticsEventListener {

    @EventListener
    @Async
    public void onPostCreated(PostCreatedEvent event) {
        // 初始化内容统计
        analyticsService.initializeContentStats(event.getPostId());
    }
}
```

### 依赖关系设计

#### Maven模块依赖
```
learn-web
  ↓ depends on
learn-application
  ↓ depends on
各域API接口 (learn-content-api, learn-interaction-api, ...)
  ↓ depends on
learn-shared

域内依赖：
api ← service ← dataservice ← mapper ← dataobject
  ↘                           ↙
              learn-shared
```

#### 关键原则
- **单向依赖**：上层依赖下层，下层不依赖上层
- **接口隔离**：域间只通过API接口交互
- **事件解耦**：跨域操作通过事件异步处理
- **缓存透明**：Service 层不感知缓存，DataService 层统一管理

### 实施计划

#### 第1周：基础设施搭建
- 创建所有Maven模块
- 配置模块依赖关系
- 建立事件总线基础设施
- 定义共享基础组件

#### 第2周：核心域迁移
- **learn-content**：迁移Post、Course、Node、Role等
- **learn-interaction**：迁移Upvote、Comment、Follow等
- 建立域间事件通信机制

#### 第3周：支撑域迁移与整合
- **learn-user**：迁移用户管理相关功能
- **learn-analytics**：迁移统计分析功能
- **learn-application**：创建跨域编排服务
- **learn-web**：重构Controller层

#### 第4周：测试与优化
- 集成测试验证
- 性能测试与优化
- 文档完善
- 代码Review

### 关键技术决策

#### 1. 简化对象模型
- Service层直接使用DO对象，减少转换开销
- 只在API边界（Controller）进行VO转换
- 避免过度的DTO分层

#### 2. 扁平化目录结构
- **子模块采用扁平结构**：文件少（3-5个）时直接放在子模块根目录
- **保留分类的场景**：当子模块包含多个同类型文件时（如stats子域有3个Service），保留service/dataservice/mapper/dataobject分类
- **shared目录简化**：去掉service/enums/validator子目录，直接放置文件
- **优势**：降低目录层级，减少导航成本，提升开发效率

#### 3. DataService 层设计
- **定位**：数据访问 + 缓存管理的统一封装层
- **职责**：
  - 封装 Mapper 调用，对上层屏蔽数据来源
  - 集成 Spring Cache + Redis 实现透明缓存
  - 批量查询优化（MGET、Pipeline）
  - 缓存穿透保护、预热、统计、降级
- **基础设施**：
  - `AbstractDataService`：提供通用缓存和数据访问能力
  - `BaseDataService`：定义标准数据访问接口
- **优势**：
  - Service 层专注业务逻辑，不感知缓存
  - 统一缓存策略，避免重复代码
  - 便于缓存监控和性能优化

#### 4. 事件驱动架构
- 使用Spring Events实现域间解耦
- 异步处理跨域操作，提升性能
- 便于未来微服务演进

#### 5. 业务域自治
- 每个域包含完整的业务逻辑和数据访问
- 域间不直接依赖，通过事件通信
- 支持团队按域并行开发

#### 6. 统一Web入口
- 保持单一API网关
- 统一认证、限流、监控
- 便于API版本管理

### 预期收益

#### 技术收益
- 模块化程度提升，代码维护性增强
- 业务逻辑边界清晰，职责分明
- 支持团队并行开发，提升开发效率
- 为微服务架构演进做好准备

#### 业务收益
- 新功能开发速度提升40%+
- Bug修复时间减少60%+
- 代码重用率提升50%+
- 系统稳定性和可扩展性显著改善