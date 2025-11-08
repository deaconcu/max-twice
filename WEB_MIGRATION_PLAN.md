# Web 项目迁移总体规划文档

## ⚠️ 重要提示

**🚨 代码质量原则 🚨**

在整个重构迁移过程中，必须遵守以下原则：

1. **发现问题必须指出**：如果发现旧项目中的代码实现与 Vue 3 / TypeScript / 前端最佳实践不一致，必须立即指出并改正
2. **不保留错误做法**：即使旧项目中存在某种实现方式，如果它不符合规范，也不要照搬迁移
3. **能模块化的要模块化**：任何可以拆分、可以复用的代码都要进行模块化，避免代码重复和臃肿的单文件
4. **主动审查代码**：在迁移每个模块时，主动检查：
   - 是否符合 Vue 3 Composition API 最佳实践
   - 是否有类型安全问题
   - 是否有性能问题
   - 是否有安全隐患
   - 是否有代码异味（重复代码、过长函数、不合理的依赖等）
   - 是否可以模块化拆分
5. **及时沟通**：发现不确定的实现方式时，立即提出疑问和建议
6. **持续优化**：不仅是迁移，更是重构和优化的过程

**目标：建立一个高质量、可维护、符合最佳实践的现代化前端项目**

---

## 📋 项目概述

### 背景
- **web-ts**: 功能完整的旧项目（166个源文件），TypeScript 配置宽松，积累了一些技术债
- **web-template-2**: 现代化模版项目（46个源文件），结构清晰但代码未模块化，不能直接使用
- **web（新项目）**: 从零开始，结合两个项目的优势，全面重构迁移

### 目标
- ✅ 建立严格的代码规范和类型检查
- ✅ 实现高质量、可维护的代码架构
- ✅ 迁移所有核心业务功能
- ✅ 优化性能和用户体验
- ⏱️ 预计时间：4-6周

---

## 🎯 技术栈选型

### 核心技术
- **Vue 3.5.24** - 最新稳定版，Composition API
- **TypeScript 5.9.3** - 严格模式，零 any
- **Vite 7.2.2** - 最新稳定版，快速构建工具
- **Vuetify 3.10.9** - UI 组件库最新版
- **Vue Router 4.6.3** - 路由管理最新版
- **Pinia 2.3.1** - 状态管理（2.x 最新稳定版，推荐使用）

### 功能依赖
- **@tiptap/vue-3 3.10.4** + 相关扩展包 - 富文本编辑器
  - @tiptap/starter-kit 3.10.4
  - @tiptap/extension-image 3.10.4
  - @tiptap/extension-link 3.10.4
  - @tiptap/extension-table 3.10.4
  - 其他 18 个扩展包
- **vue-i18n 11.1.12** - 国际化系统
- **axios 1.13.2** - HTTP 客户端
- **chart.js 4.5.1** - 数据可视化核心库
- **vue-chartjs 5.3.3** - Chart.js 的 Vue 3 封装
- **@vue-flow/core 1.47.0** - 流程图核心
- **@vue-flow/background 1.3.2** - 流程图背景
- **@vue-flow/controls 1.1.3** - 流程图控制
- **highlight.js 11.11.1** - 代码高亮
- **katex 0.16.25** - 数学公式渲染
- **marked 17.0.0** - Markdown 解析器
- **mermaid 11.12.1** - 图表生成
- **vuedraggable 2.24.3** - 拖拽排序功能
- **dagre 0.8.5** - 图布局算法
- **pinia-plugin-persistedstate 4.7.1** - Pinia 状态持久化
- **@mdi/font 7.4.47** - Material Design Icons

### 开发工具
- **@vitejs/plugin-vue 6.0.1** - Vite 的 Vue 插件
- **vite-plugin-vuetify 2.1.2** - Vuetify 的 Vite 插件
- **vue-tsc 3.1.3** - Vue TypeScript 编译器
- **typescript 5.9.3** - TypeScript 编译器
- **eslint 9.39.1** - 代码检查工具
- **@typescript-eslint/parser 8.46.3** - TypeScript ESLint 解析器
- **prettier 3.6.2** - 代码格式化工具
- **husky 9.1.7** - Git hooks 管理
- **lint-staged 16.2.6** - 提交前代码检查
- **sass 1.93.3** - CSS 预处理器
- **@types/node 24.10.0** - Node.js 类型定义

---

## 📁 项目结构设计

> 基于 Vue 3 最佳实践和现代前端架构设计

```
web/
├── public/                      # 静态资源（不经过构建）
│   ├── favicon.ico
│   └── robots.txt
│
├── src/
│   ├── api/                     # API 请求层 ⚡️ 优化：去除 services 多余嵌套
│   │   ├── client.ts            # Axios 实例和拦截器配置
│   │   ├── modules/             # API 模块（按业务领域划分）
│   │   │   ├── auth.ts          # 认证相关 API
│   │   │   ├── user.ts          # 用户相关 API
│   │   │   ├── course.ts        # 课程相关 API
│   │   │   ├── post.ts          # 帖子相关 API
│   │   │   ├── comment.ts       # 评论相关 API
│   │   │   ├── message.ts       # 消息相关 API
│   │   │   └── admin.ts         # 管理相关 API
│   │   └── index.ts             # 统一导出
│   │
│   ├── assets/                  # 静态资源（会被构建处理）
│   │   ├── images/              # 图片资源
│   │   ├── fonts/               # 字体文件
│   │   └── icons/               # 图标文件
│   │
│   ├── components/              # 组件库 ⚡️ 优化：按职责分层
│   │   ├── base/                # 基础 UI 组件（高复用、无业务逻辑）
│   │   │   ├── BaseButton.vue
│   │   │   ├── BaseInput.vue
│   │   │   ├── BaseCard.vue
│   │   │   ├── BaseDialog.vue
│   │   │   └── ...
│   │   ├── layout/              # 布局组件
│   │   │   ├── AppHeader.vue
│   │   │   ├── AppSidebar.vue
│   │   │   ├── AppFooter.vue
│   │   │   └── DefaultLayout.vue
│   │   └── features/            # 业务功能组件（按领域划分）
│   │       ├── auth/            # 认证相关
│   │       │   ├── LoginForm.vue
│   │       │   ├── RegisterForm.vue
│   │       │   └── ...
│   │       ├── course/          # 课程相关
│   │       │   ├── CourseCard.vue
│   │       │   ├── CourseList.vue
│   │       │   └── ...
│   │       ├── learning/        # 学习模块
│   │       ├── memory/          # 记忆卡片
│   │       ├── message/         # 消息系统
│   │       ├── read/            # 阅读模块（富文本编辑器）
│   │       ├── roadmap/         # 路线图
│   │       ├── career/          # 职业中心
│   │       ├── admin/           # 管理后台
│   │       └── user/            # 用户相关
│   │
│   ├── composables/             # 组合式函数（Vue 3 Composition API）
│   │   ├── useAuth.ts           # 认证逻辑
│   │   ├── useUser.ts           # 用户信息
│   │   ├── useApi.ts            # API 请求封装
│   │   ├── useToast.ts          # 消息提示
│   │   ├── useDialog.ts         # 对话框管理
│   │   ├── useLoading.ts        # 加载状态
│   │   ├── usePagination.ts     # 分页逻辑
│   │   └── ...
│   │
│   ├── config/                  # 配置文件 ⚡️ 优化：plugins → config
│   │   ├── vuetify.ts           # Vuetify 主题配置
│   │   ├── i18n.ts              # 国际化配置
│   │   ├── pinia.ts             # Pinia 配置
│   │   └── app.ts               # 应用全局配置
│   │
│   ├── constants/               # 常量定义
│   │   ├── api.ts               # API 相关常量（URL、超时等）
│   │   ├── routes.ts            # 路由路径常量
│   │   ├── status.ts            # 状态常量
│   │   ├── permissions.ts       # 权限常量
│   │   └── ...
│   │
│   ├── directives/              # 自定义指令 ⚡️ 新增
│   │   ├── vPermission.ts       # 权限指令
│   │   ├── vLoading.ts          # 加载指令
│   │   ├── vLazyLoad.ts         # 懒加载指令
│   │   └── index.ts
│   │
│   ├── enums/                   # 枚举类型 ⚡️ 新增
│   │   ├── user.ts              # 用户相关枚举
│   │   ├── course.ts            # 课程相关枚举
│   │   └── ...
│   │
│   ├── locales/                 # 国际化文件
│   │   ├── en.json              # 英文翻译
│   │   ├── zh.json              # 中文翻译
│   │   └── index.ts             # 语言配置导出
│   │
│   ├── middleware/              # 中间件 ⚡️ 新增
│   │   ├── auth.ts              # 认证中间件
│   │   ├── permission.ts        # 权限检查
│   │   └── ...
│   │
│   ├── router/                  # 路由配置
│   │   ├── index.ts             # 路由主入口
│   │   ├── guards.ts            # 全局路由守卫
│   │   └── routes/              # 路由模块 ⚡️ 优化：独立目录
│   │       ├── base.ts          # 基础路由（首页、404等）
│   │       ├── auth.ts          # 认证路由
│   │       ├── course.ts        # 课程路由
│   │       ├── learning.ts      # 学习路由
│   │       ├── admin.ts         # 管理路由
│   │       └── ...
│   │
│   ├── stores/                  # Pinia 状态管理
│   │   ├── modules/             # Store 模块 ⚡️ 优化：模块化
│   │   │   ├── auth.ts          # 认证状态
│   │   │   ├── user.ts          # 用户状态
│   │   │   ├── course.ts        # 课程状态
│   │   │   ├── app.ts           # 应用全局状态
│   │   │   └── ...
│   │   └── index.ts             # Store 统一导出
│   │
│   ├── styles/                  # 全局样式 ⚡️ 优化：独立出 assets
│   │   ├── variables.scss       # SCSS 变量
│   │   ├── mixins.scss          # SCSS 混入
│   │   ├── reset.scss           # 样式重置
│   │   ├── global.scss          # 全局样式
│   │   └── index.ts             # 样式统一导入
│   │
│   ├── types/                   # TypeScript 类型定义
│   │   ├── api.d.ts             # API 接口类型
│   │   ├── models.d.ts          # 数据模型类型
│   │   ├── components.d.ts      # 组件类型
│   │   ├── env.d.ts             # 环境变量类型
│   │   ├── store.d.ts           # Store 类型
│   │   └── ...
│   │
│   ├── utils/                   # 工具函数
│   │   ├── format.ts            # 格式化函数（日期、货币等）
│   │   ├── validation.ts        # 表单验证函数
│   │   ├── storage.ts           # 本地存储封装
│   │   ├── crypto.ts            # 加密工具
│   │   ├── request.ts           # 请求工具
│   │   └── ...
│   │
│   ├── views/                   # 页面视图 ⚡️ 优化：按文件夹组织
│   │   ├── home/
│   │   │   └── HomePage.vue
│   │   ├── auth/
│   │   │   ├── LoginPage.vue
│   │   │   ├── RegisterPage.vue
│   │   │   └── ResetPasswordPage.vue
│   │   ├── course/
│   │   │   ├── CourseListPage.vue
│   │   │   ├── CourseDetailPage.vue
│   │   │   └── CourseLearningPage.vue
│   │   ├── learning/
│   │   │   └── LearningDashboard.vue
│   │   ├── profile/
│   │   │   └── ProfilePage.vue
│   │   ├── admin/
│   │   │   ├── DashboardPage.vue
│   │   │   ├── UserManagementPage.vue
│   │   │   └── ...
│   │   └── error/
│   │       ├── NotFoundPage.vue
│   │       └── ServerErrorPage.vue
│   │
│   ├── App.vue                  # 根组件
│   ├── main.ts                  # 应用入口
│   └── vite-env.d.ts            # Vite 环境类型声明
│
├── .env.development             # 开发环境变量 ⚡️ 新增
├── .env.production              # 生产环境变量 ⚡️ 新增
├── .env.local.example           # 本地环境变量示例
├── .eslintrc.cjs                # ESLint 配置
├── .eslintignore                # ESLint 忽略文件
├── .prettierrc                  # Prettier 配置
├── .prettierignore              # Prettier 忽略文件
├── .gitignore                   # Git 忽略文件
├── index.html                   # HTML 入口
├── package.json                 # 项目依赖
├── tsconfig.json                # TypeScript 配置
├── tsconfig.node.json           # Node 相关 TS 配置
├── vite.config.ts               # Vite 配置
└── README.md                    # 项目说明
```

### 🎯 结构设计原则

#### 1. **清晰的分层架构**
- **表现层**：`views/` + `components/`
- **业务层**：`composables/` + `stores/`
- **数据层**：`api/` + `types/`
- **工具层**：`utils/` + `constants/`

#### 2. **组件分层策略**
```
base/       → 纯 UI 组件，无业务逻辑，高度可复用
layout/     → 布局组件，定义页面结构
features/   → 业务组件，包含特定领域逻辑
```

#### 3. **关注点分离**
- **配置** (`config/`) 与 **业务逻辑** 分离
- **样式** (`styles/`) 与 **静态资源** (`assets/`) 分离
- **类型定义** (`types/`) 集中管理
- **枚举** (`enums/`) 独立维护

#### 4. **可扩展性**
- 模块化路由：`router/routes/`
- 模块化 Store：`stores/modules/`
- 模块化 API：`api/modules/`

#### 5. **代码组织规范**
- 每个文件夹都应该有 `index.ts` 统一导出
- 相关文件放在同一目录下（就近原则）
- 避免过深的嵌套（≤ 3 层）
- 文件命名采用 PascalCase（组件）或 camelCase（其他）

### ⚡️ 相比原设计的改进

| 改进项 | 优化方案 | 优势 |
|--------|---------|------|
| `services/api/` | → `api/` | 减少无意义嵌套 |
| `assets/styles/` | → `styles/` | 样式独立，职责清晰 |
| `plugins/` | → `config/` | 命名更准确 |
| `components/` 扁平化 | → `base/layout/features/` | 组件职责分层 |
| `views/` 单文件 | → 按文件夹组织 | 相关文件就近管理 |
| 新增 `directives/` | 自定义指令集中管理 | 功能完整性 |
| 新增 `middleware/` | 中间件逻辑独立 | 关注点分离 |
| 新增 `enums/` | 枚举类型集中管理 | 类型安全 |
| 路由/Store 模块化 | `routes/` 和 `modules/` | 可扩展性强 |
| 新增环境变量文件 | `.env.*` | 配置管理规范 |

---

## 🚀 迁移阶段规划

### 阶段一：项目初始化和基础架构（2-3天）

#### 目标
搭建项目骨架，建立开发规范

#### 任务清单
- [ ] 使用 Vite 创建 Vue 3 + TypeScript 项目
- [ ] 配置 ESLint + Prettier（严格模式）
- [ ] 配置 TypeScript（strict: true, noUnusedLocals, noUnusedParameters）
- [ ] 配置 Git hooks（husky + lint-staged）
- [ ] 安装核心依赖（Vue Router、Pinia、Vuetify、Axios）
- [ ] 创建项目目录结构
- [ ] 配置 Vite（API 代理、路径别名、环境变量）
- [ ] 配置 Vuetify 主题系统
- [ ] 编写项目 README

#### 参考资源
- web-template-2: 基础配置参考
- web-ts: 依赖列表参考

---

### 阶段二：核心基础设施（3-4天）

#### 目标
建立可复用的基础设施层

#### 任务清单

**API 服务层**
- [ ] 创建 Axios 实例配置（拦截器、错误处理）
- [ ] 实现请求/响应拦截器
- [ ] 实现统一错误处理机制
- [ ] 封装常用 HTTP 方法（GET、POST、PUT、DELETE）
- [ ] 实现 API 模块化（auth、user、course 等）

**国际化系统**
- [ ] 安装 vue-i18n
- [ ] 从 web-ts 提取翻译文案
- [ ] 重构为类型安全的 i18n 架构
- [ ] 实现语言切换功能
- [ ] 实现路由级别的语言管理

**布局系统**
- [ ] 设计灵活的布局组件架构
- [ ] 实现 AppHeader 组件（导航、用户菜单、语言切换）
- [ ] 实现 AppSidebar 组件（侧边导航、折叠功能）
- [ ] 实现 AppFooter 组件
- [ ] 实现主题切换功能（亮色/暗色）
- [ ] 实现响应式布局（移动端适配）

**路由架构**
- [ ] 设计模块化路由结构
- [ ] 实现路由守卫（认证、权限）
- [ ] 实现路由懒加载
- [ ] 配置路由元信息（标题、权限）
- [ ] 实现面包屑导航

**状态管理**
- [ ] 设计 Pinia Store 模块结构
- [ ] 实现用户状态管理（user store）
- [ ] 实现认证状态管理（auth store）
- [ ] 配置状态持久化插件
- [ ] 实现状态类型定义

#### 参考资源
- web-ts: API 结构、i18n 文案、路由配置
- web-template-2: 布局设计思路

---

### 阶段三：认证和用户系统（3-4天）

#### 目标
实现完整的用户认证和个人中心功能

#### 任务清单

**认证模块**
- [ ] 实现登录功能（表单验证、API 调用）
- [ ] 实现注册功能（邮箱验证、密码强度）
- [ ] 实现登出功能
- [ ] 实现密码重置功能
- [ ] 实现 Token 管理（存储、刷新）
- [ ] 实现认证守卫（路由保护）
- [ ] 封装 useAuth 组合式函数

**用户资料页**
- [ ] 实现用户信息展示
- [ ] 实现用户信息编辑
- [ ] 实现头像上传功能
- [ ] 实现密码修改功能
- [ ] 实现用户统计数据展示
- [ ] 实现学习记录 Tab
- [ ] 实现订阅关注 Tab
- [ ] 实现我的内容 Tab（课程、文章、路线图）
- [ ] 实现我的帖子 Tab

#### 参考资源
- web-ts: `components/auth/`, `components/user/`, `stores/user.ts`
- web-template-2: `components/profile/`, `views/ProfileView.vue`

---

### 阶段四：核心业务功能（5-7天）

#### 目标
实现主要业务功能模块

#### 任务清单

**富文本编辑器**
- [ ] 安装 TipTap 生态系统（22个扩展包）
- [ ] 配置 TipTap 编辑器基础功能
- [ ] 实现工具栏组件（格式化、插入）
- [ ] 实现代码高亮（highlight.js）
- [ ] 实现数学公式（katex）
- [ ] 实现图表支持（mermaid）
- [ ] 实现图片上传
- [ ] 实现 Markdown 快捷键
- [ ] 封装为可复用的编辑器组件

**课程系统**
- [ ] 实现课程列表页（筛选、搜索、分页）
- [ ] 实现课程详情页（大纲、简介、评论）
- [ ] 实现课程学习页（内容展示、进度追踪）
- [ ] 实现课程创建/编辑功能
- [ ] 实现课程章节管理
- [ ] 实现课程内容管理（拖拽排序）
- [ ] 实现课程发布/下架功能
- [ ] 实现课程收藏/订阅功能
- [ ] 实现学习进度保存
- [ ] 实现课程完成证书

**阅读模块**
- [ ] 实现文章列表页
- [ ] 实现文章详情页
- [ ] 实现文章发布功能
- [ ] 实现评论系统（发表、回复、删除）
- [ ] 实现点赞/收藏功能
- [ ] 实现文章目录导航
- [ ] 实现相关推荐
- [ ] 实现阅读时长统计

#### 参考资源
- web-ts: `components/course/`, `components/read/`, `views/CourseList.vue`, `views/ReadView.vue`
- web-template-2: `components/course/`, `views/ContentReadView.vue`

---

### 阶段五：增强功能模块（4-5天）

#### 目标
实现辅助功能和增强体验

#### 任务清单

**记忆卡片系统**
- [ ] 实现卡片列表管理
- [ ] 实现卡片创建/编辑
- [ ] 实现间隔重复算法（SM-2）
- [ ] 实现卡片复习功能
- [ ] 实现复习统计和可视化
- [ ] 实现卡片分类和标签

**学习跟踪和数据可视化**
- [ ] 安装 Chart.js + vue-chartjs
- [ ] 实现学习时长统计
- [ ] 实现学习进度图表
- [ ] 实现学习热力图
- [ ] 实现排行榜功能
- [ ] 实现成就系统
- [ ] 实现学习报告生成

**消息通知系统**
- [ ] 实现消息列表
- [ ] 实现系统消息
- [ ] 实现私信功能
- [ ] 实现课程申请通知
- [ ] 实现消息已读/未读状态
- [ ] 实现消息实时推送（可选 WebSocket）
- [ ] 实现消息提醒（桌面通知）

**职业中心**
- [ ] 实现职业列表页（分类、筛选）
- [ ] 实现职业详情页
- [ ] 实现职业路线图
- [ ] 实现职业技能树
- [ ] 实现职业推荐算法

**路线图系统**
- [ ] 安装 Vue Flow
- [ ] 实现路线图可视化展示
- [ ] 实现路线图创建/编辑
- [ ] 实现节点拖拽和连接
- [ ] 实现路线图分享功能
- [ ] 实现路线图学习进度

#### 参考资源
- web-ts: `components/memory/`, `components/message/`, `components/career/`, `components/roadmap/`, `components/ranking/`

---

### 阶段六：管理功能和优化（3-4天）

#### 目标
完善管理后台和性能优化

#### 任务清单

**管理后台**
- [ ] 实现管理员权限控制
- [ ] 实现用户管理（列表、封禁、权限）
- [ ] 实现课程审核管理
- [ ] 实现帖子审核管理
- [ ] 实现评论审核管理
- [ ] 实现分类管理
- [ ] 实现系统配置管理
- [ ] 实现数据统计面板
- [ ] 实现操作日志

**性能优化**
- [ ] 实现路由懒加载
- [ ] 实现组件懒加载
- [ ] 实现图片懒加载
- [ ] 代码分割优化
- [ ] 打包体积优化
- [ ] 实现 API 请求缓存
- [ ] 实现虚拟滚动（长列表）
- [ ] 实现预加载策略

**测试和修复**
- [ ] 编写单元测试（核心功能）
- [ ] 编写 E2E 测试（关键流程）
- [ ] 浏览器兼容性测试
- [ ] 移动端适配测试
- [ ] 性能测试和优化
- [ ] 无障碍访问（A11y）测试
- [ ] Bug 修复和代码优化

#### 参考资源
- web-ts: `components/admin/`, `views/AdminView.vue`

---

## 📐 代码质量标准

### TypeScript 规范
- ✅ 严格模式（strict: true）
- ✅ 禁止 any 类型（除非必要且有注释说明）
- ✅ 所有函数必须有类型注解
- ✅ 所有接口/类型必须导出并复用
- ✅ 使用 enum 管理常量集合

### 组件设计原则
- ✅ 单一职责原则（每个组件只做一件事）
- ✅ 高内聚低耦合
- ✅ Props 必须有类型定义和默认值
- ✅ Emit 事件必须有类型定义
- ✅ 组件名使用 PascalCase
- ✅ 文件名与组件名一致

### Composables 规范
- ✅ 函数命名以 use 开头
- ✅ 返回响应式数据和方法
- ✅ 可测试、可复用
- ✅ 避免副作用

### API 服务规范
- ✅ 统一错误处理
- ✅ 请求/响应类型定义
- ✅ 加载状态管理
- ✅ 超时处理
- ✅ 重试机制（可选）

### 样式规范
- ✅ 使用 scoped 样式
- ✅ 使用 Vuetify 主题变量
- ✅ 避免内联样式
- ✅ 响应式设计（mobile-first）
- ✅ 使用 BEM 命名规范（可选）

### Git 提交规范
- ✅ 使用约定式提交（Conventional Commits）
- ✅ 提交信息清晰明确
- ✅ 每次提交只做一件事
- ✅ 提交前运行 lint 和 test

---

## 🔍 迁移注意事项

### 代码迁移原则
1. **不要直接复制代码**：理解业务逻辑后重新实现
2. **逐步重构**：每次迁移一个小模块，确保可运行
3. **保持类型安全**：所有代码必须通过 TypeScript 检查
4. **编写测试**：关键功能必须有测试覆盖
5. **性能优先**：注意性能影响，及时优化

### 常见陷阱
- ⚠️ TipTap 扩展配置复杂，注意版本兼容
- ⚠️ Vue I18n 路由集成需要特殊处理
- ⚠️ Pinia 持久化可能导致状态不一致
- ⚠️ Vuetify 版本升级有 breaking changes
- ⚠️ Vue Flow 性能问题，大量节点需要优化
- ⚠️ Chart.js 响应式需要手动处理

### 技术债务管理
- 📝 记录所有 TODO 和 FIXME
- 📝 记录已知问题和临时方案
- 📝 定期 code review
- 📝 持续重构和优化

---

## 📊 进度追踪

### 里程碑
- [ ] 阶段一：项目初始化（Day 1-3）
- [ ] 阶段二：基础设施（Day 4-7）
- [ ] 阶段三：认证系统（Day 8-11）
- [ ] 阶段四：核心功能（Day 12-18）
- [ ] 阶段五：增强功能（Day 19-23）
- [ ] 阶段六：管理优化（Day 24-27）
- [ ] 最终验收和上线准备（Day 28-30）

### 风险评估
| 风险项 | 级别 | 应对措施 |
|--------|------|----------|
| TipTap 集成复杂 | 高 | 预留充足时间，参考官方文档 |
| 类型定义不完整 | 中 | 渐进式补充，优先核心模块 |
| 性能问题 | 中 | 持续监控，及时优化 |
| 时间超期 | 中 | 按优先级调整功能范围 |
| 业务理解偏差 | 低 | 及时沟通确认需求 |

---

## 🎓 学习资源

### 官方文档
- [Vue 3](https://vuejs.org/)
- [TypeScript](https://www.typescriptlang.org/)
- [Vuetify 3](https://vuetifyjs.com/)
- [Pinia](https://pinia.vuejs.org/)
- [TipTap](https://tiptap.dev/)
- [Vue I18n](https://vue-i18n.intlify.dev/)

### 最佳实践
- [Vue 3 风格指南](https://vuejs.org/style-guide/)
- [TypeScript 最佳实践](https://www.typescriptlang.org/docs/handbook/declaration-files/do-s-and-don-ts.html)
- [Vue 3 性能优化](https://vuejs.org/guide/best-practices/performance.html)

---

## 📝 文档清单

### 必需文档
- [x] 迁移总体规划文档（本文档）
- [ ] 迁移检查清单（Checklist）
- [ ] API 文档
- [ ] 组件库文档
- [ ] 部署文档

### 可选文档
- [ ] 架构设计文档
- [ ] 数据库设计文档
- [ ] 测试用例文档
- [ ] 性能优化记录

---

## 🚀 开始迁移

1. 阅读本文档，理解整体规划
2. 查看迁移检查清单，了解具体任务
3. 按阶段逐步执行
4. 遇到问题及时记录和沟通
5. 定期 review 和调整计划

**祝迁移顺利！** 🎉
