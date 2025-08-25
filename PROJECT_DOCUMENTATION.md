# Max-Twice Project Documentation

## 项目概述
Max-Twice 是一个基于 Spring Boot (后端) 和 Vue.js (前端) 的在线学习平台。该项目采用微服务架构，支持课程管理、用户学习路径、评论系统、排名系统等功能。

## 项目架构

### 技术栈
- **后端**: Java 17, Spring Boot 3.3.3, Spring Cloud, MyBatis, MySQL, Redis
- **前端**: Vue.js 3.5.13, Vite, Vuetify, Pinia, Vue Router
- **数据库**: MySQL
- **缓存**: Redis
- **认证**: JWT, SA-Token
- **服务发现**: Eureka

### 项目结构

```
max-twice/
├── backend/                    # 后端微服务
│   ├── learn-api/             # API 网关服务
│   ├── learn-common/          # 公共工具类和实体
│   ├── learn-domain/          # 核心业务逻辑
│   ├── learn-dto/             # 数据传输对象
│   ├── learn-external/        # 外部服务 (Eureka)
│   ├── learn-front/          # 前端服务控制器
│   ├── learn-persistence/     # 数据持久层
│   ├── learn-user/           # 用户服务
│   └── docs/                 # 后端文档和SQL脚本
└── web/                      # 前端Vue应用
    ├── src/
    │   ├── components/       # Vue组件
    │   ├── views/           # 页面视图
    │   ├── stores/          # Pinia状态管理
    │   ├── services/        # API服务
    │   └── router/          # 路由配置
    └── public/              # 静态资源
```

## 核心模块说明

### 后端模块

#### learn-api (API网关)
- **用途**: 统一API入口，路由转发
- **主要控制器**: 
  - CourseController: 课程管理
  - UserController: 用户管理
  - CommentController: 评论系统
  - PlatformStatsController: 平台统计

#### learn-domain (核心业务)
- **用途**: 业务逻辑处理
- **主要服务**:
  - CourseService: 课程业务
  - UserCourseService: 用户课程关联
  - PlatformStatsService: 平台统计
  - ScoreCalculationService: 评分计算

#### learn-persistence (数据层)
- **用途**: 数据访问层
- **主要实体**: Course, User, Comment, Node, Roadmap
- **映射器**: MyBatis Mapper接口

#### learn-dto (数据传输)
- **用途**: API数据传输对象
- **版本化**: 支持多版本DTO (V1, V2, V3, V4)

### 前端模块

#### 核心组件
- **Header/NavigationBar**: 顶部导航
- **SideBar/RightSidebar**: 侧边栏
- **CourseProgressCard**: 课程进度卡片
- **Comment/Subcomment**: 评论系统
- **RoadmapCard**: 学习路径卡片

#### 主要页面
- **Course**: 课程详情页
- **Learning**: 学习页面
- **CareerCenter**: 职业中心
- **Admin**: 管理后台
- **User**: 用户个人页面

#### 状态管理 (Pinia)
- **user**: 用户状态管理
- **postings**: 帖子状态管理

## 功能特性

### 核心功能
1. **课程管理**: 课程创建、编辑、分类、进度跟踪
2. **学习路径**: 个性化学习路径规划
3. **评论系统**: 多级评论、点赞功能
4. **用户系统**: 注册、登录、个人资料管理
5. **排名系统**: 课程热度、用户积分排名
6. **统计分析**: 平台数据统计和分析

### 高级功能
1. **富文本编辑**: 基于TipTap的编辑器，支持数学公式
2. **拖拽功能**: 课程内容拖拽排序
3. **实时通知**: 消息系统
4. **数据可视化**: Three.js 3D效果
5. **响应式设计**: 移动端适配

## 开发指南

### 环境要求
- Java 17+
- Node.js 16+
- MySQL 8.0+
- Redis 6.0+

### 启动命令

#### 后端启动
```bash
cd backend
mvn clean install
# 启动各个微服务
```

#### 前端启动
```bash
cd web
npm install
npm run dev
```

### 测试命令
```bash
# 前端测试
cd web
npm run test:unit

# 后端测试
cd backend
mvn test
```

## 数据库设计

### 主要表结构
- **course**: 课程信息
- **user**: 用户信息
- **user_course**: 用户课程关联
- **comment**: 评论表
- **roadmap**: 学习路径
- **platform_stats**: 平台统计

### SQL 脚本位置
- `backend/docs/sql/`: 数据库更新脚本
- `web/`: 课程数据生成脚本

## API 接口

### 主要API端点
- `/api/courses`: 课程相关接口
- `/api/users`: 用户相关接口
- `/api/comments`: 评论相关接口
- `/api/stats`: 统计相关接口
- `/api/roadmaps`: 学习路径接口

## 部署说明

### 生产环境构建
```bash
# 前端构建
cd web
npm run build

# 后端打包
cd backend
mvn clean package
```

## 最近更新

根据git状态，最近有以下变更：
- 删除了 `PlatformStatsController.java` (可能已重构)

## 注意事项

1. **认证系统**: 使用JWT + SA-Token双重认证
2. **缓存策略**: Redis缓存用户会话和热点数据
3. **版本管理**: DTO支持多版本，保证API兼容性
4. **错误处理**: 全局异常处理器统一错误响应
5. **安全性**: 输入验证、SQL注入防护

## 开发建议

1. **代码规范**: 遵循Java和Vue.js最佳实践
2. **模块化**: 保持模块间低耦合
3. **测试覆盖**: 编写单元测试和集成测试
4. **文档更新**: 及时更新API文档和代码注释
5. **性能优化**: 关注数据库查询和前端渲染性能

---

*最后更新: 2025-08-24*
*文档版本: 1.0*