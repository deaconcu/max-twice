# Web 项目迁移检查清单

> 本清单用于跟踪迁移进度，每完成一项请标记为 [x]

## ⚠️ 重要提示

**🚨 代码质量原则 🚨**

在执行每一项任务时，必须遵守以下原则：

1. **发现问题必须指出**：如果发现旧项目中的代码实现与最佳实践不一致，必须立即指出并改正
2. **不保留错误做法**：不要照搬旧代码中的错误实现
3. **能模块化的要模块化**：任何可以拆分、复用的代码都要模块化，避免代码重复和臃肿
4. **主动审查代码**：检查类型安全、性能、安全隐患、代码异味、模块化可能性
5. **及时沟通**：发现不确定的实现方式时，立即提出疑问
6. **持续优化**：这是重构和优化的过程，不仅仅是搬运代码

---

## 📋 阶段一：项目初始化和基础架构

### 1.1 项目创建
- [x] 使用 `npm create vite@latest web -- --template vue-ts` 创建项目
- [x] 进入项目目录 `cd web`
- [x] 安装依赖 `npm install`
- [x] 测试运行 `npm run dev`
- [x] 验证项目可以正常启动

### 1.2 开发规范配置
- [x] 安装 ESLint 和相关插件（使用 ESLint 9 扁平配置）
- [x] 安装 Prettier 和相关插件
- [x] 创建 `eslint.config.js` 配置文件（ESLint 9 新格式）
- [x] 创建 `.prettierrc` 配置文件
- [x] 创建 `.prettierignore` 文件
- [x] 配置 `package.json` 添加 lint、format 和 type-check 脚本
- [x] 测试 ESLint: `npm run lint` ✅ 通过
- [x] 测试 Prettier: `npm run format` ✅ 通过

### 1.3 Git Hooks 配置
- [x] 安装 husky: `npm install -D husky`
- [x] 安装 lint-staged: `npm install -D lint-staged`
- [x] 配置 lint-staged 规则（在 package.json 中）
- [ ] 初始化 husky（等待 git 提交时配置）
- [ ] 添加 pre-commit hook
- [ ] 测试提交前检查

### 1.4 TypeScript 配置
- [x] 查看 `tsconfig.app.json`（Vite 使用项目引用）
- [x] ✅ `strict: true` 已默认启用
- [x] ✅ `noUnusedLocals: true` 已默认启用
- [x] ✅ `noUnusedParameters: true` 已默认启用
- [x] ✅ `noFallthroughCasesInSwitch: true` 已默认启用
- [x] ✅ 添加 `noUncheckedIndexedAccess: true` 额外安全保障
- [x] 配置路径别名 `"@/*": ["./src/*"]`
- [x] 测试类型检查: `npm run type-check` ✅ 通过

### 1.5 核心依赖安装
- [x] 安装 Vue Router 4.6.3
- [x] 安装 Pinia 2.3.1（2.x 稳定版）
- [x] 安装 pinia-plugin-persistedstate 3.2.3（兼容 Pinia 2.x）
- [x] 安装 Vuetify 3.10.9
- [x] 安装 Vuetify Vite 插件 2.1.2
- [x] 安装 @mdi/font 7.4.47
- [x] 安装 Axios 1.13.2
- [x] 安装 Sass 1.93.3
- [x] 验证所有依赖安装成功

### 1.6 项目目录结构 ⚡️ 按最佳实践组织
- [x] 创建 `src/api/` 目录
- [x] 创建 `src/api/modules/` 目录
- [x] 创建 `src/assets/images/` 目录
- [x] 创建 `src/assets/fonts/` 目录
- [x] 创建 `src/assets/icons/` 目录
- [x] 创建 `src/components/base/` 目录
- [x] 创建 `src/components/layout/` 目录
- [x] 创建 `src/components/features/` 目录
- [x] 创建 `src/views/` 目录
- [x] 创建 `src/router/` 目录
- [x] 创建 `src/router/routes/` 目录
- [x] 创建 `src/stores/` 目录
- [x] 创建 `src/stores/modules/` 目录
- [x] 创建 `src/composables/` 目录
- [x] 创建 `src/config/` 目录（替代 plugins）
- [x] 创建 `src/types/` 目录
- [x] 创建 `src/utils/` 目录
- [x] 创建 `src/constants/` 目录
- [x] 创建 `src/locales/` 目录
- [x] 创建 `src/styles/` 目录（独立于 assets）
- [x] 创建 `src/directives/` 目录（新增）
- [x] 创建 `src/middleware/` 目录（新增）
- [x] 创建 `src/enums/` 目录（新增）

### 1.7 环境变量配置 ⚡️ 新增
- [x] 创建 `.env.development` 文件
- [x] 创建 `.env.production` 文件
- [x] 创建 `.env.local.example` 文件
- [x] 配置开发环境 API 地址
- [x] 配置生产环境 API 地址
- [x] 添加 `.env.local` 到 `.gitignore` ✅ 已默认忽略
- [x] 创建环境变量类型定义 `src/types/env.d.ts`

### 1.8 Vite 配置
- [x] 配置路径别名 `@` 指向 `src`
- [x] 配置开发服务器端口 5174
- [x] 配置 API 代理到后端 `http://localhost:8090`
- [x] 配置 Vuetify 插件 `autoImport: true`
- [x] 配置生产环境构建优化（Terser + 代码分割）
- [x] 测试配置是否生效 ✅ 开发服务器成功启动

### 1.9 Vuetify 主题配置
- [x] 创建 `src/config/vuetify.ts` ⚡️ 路径更新
- [x] 配置主题颜色（primary, secondary, accent 等）
- [x] 配置暗色/亮色主题
- [x] 配置图标集（mdi）
- [x] 在 `main.ts` 中注册 Vuetify
- [x] 创建 `src/config/pinia.ts` 配置 Pinia
- [x] 创建 `src/router/index.ts` 配置 Router
- [x] 创建测试页面 `src/views/home/HomePage.vue`
- [x] 测试 Vuetify 组件可以正常使用 ✅ 开发服务器运行正常

### 1.10 基础文档
- [x] 更新项目 README.md
- [x] 添加项目简介
- [x] 添加技术栈说明
- [x] 添加开发命令说明
- [x] 添加项目结构说明
- [x] 添加代码规范和贡献指南

**阶段一完成标志**: ✅ 项目可以正常启动（http://localhost:5174），ESLint/Prettier/TypeCheck 全部通过，目录结构完整，文档齐全

---

## 📋 阶段二：核心基础设施

### 2.1 API 服务层 - Axios 配置 ⚡️ 路径更新
- [x] 创建 `src/api/client.ts` (不是 services/api)
- [x] 配置 Axios 实例（baseURL, timeout）
- [x] 实现请求拦截器（添加 token）
- [x] 实现响应拦截器（统一处理响应）
- [x] 实现错误处理拦截器
- [x] 实现 token 刷新逻辑（预留）
- [x] 测试 API 客户端

### 2.2 API 服务层 - 类型定义 ✅ 完成（零 unknown）
- [x] 创建 `src/types/api.d.ts` ⚡️ 使用 .d.ts 后缀
- [x] 定义 ApiResponse 泛型接口
- [x] 定义 ApiError 接口
- [x] 定义 PaginationParams 接口
- [x] 定义 PaginationResponse 接口
- [x] 导出所有 API 类型
- [x] 创建所有基础类型（user, course, node, post, comment, roadmap, profession, message, stats, upvote, page 等）
- [x] 枚举类型 `src/enums/index.ts`

### 2.3 API 服务层 - 模块化 API ⚡️ 路径更新 ✅ 完成（13个模块）
- [x] 创建 `src/api/modules/auth.ts` (认证 API)
- [x] 创建 `src/api/modules/user.ts` (用户 + 关注 API)
- [x] 创建 `src/api/modules/course.ts` (课程 + 订阅 API)
- [x] 创建 `src/api/modules/post.ts` (帖子 API)
- [x] 创建 `src/api/modules/comment.ts` (评论 API)
- [x] 创建 `src/api/modules/roadmap.ts` (路线图 API)
- [x] 创建 `src/api/modules/profession.ts` (职业 API)
- [x] 创建 `src/api/modules/upvote.ts` (点赞 API)
- [x] 创建 `src/api/modules/message.ts` (消息 API)
- [x] 创建 `src/api/modules/progress.ts` (学习进度 API)
- [x] 创建 `src/api/modules/stats.ts` (统计 API)
- [x] 创建 `src/api/modules/page.ts` (页面聚合 API)
- [x] 创建 `src/api/modules/system.ts` (系统配置 API)
- [x] 创建 `src/api/index.ts` 统一导出

### 2.4 国际化系统 - 安装配置 ⚡️ 路径更新
- [ ] 安装 vue-i18n: `npm install vue-i18n@9`
- [ ] 创建 `src/config/i18n.ts` (不是 plugins)
- [ ] 配置 i18n 实例
- [ ] 设置默认语言为中文
- [ ] 设置 fallback 语言为英文
- [ ] 在 `main.ts` 中注册 i18n

### 2.5 国际化系统 - 语言文件
- [ ] 从 web-ts 复制 `src/locales/zh.json`
- [ ] 从 web-ts 复制 `src/locales/en.json`
- [ ] 创建 `src/locales/index.ts` 导出语言包
- [ ] 审查和优化翻译文案
- [ ] 为新功能添加翻译 key

### 2.6 国际化系统 - 语言切换
- [ ] 创建 `src/composables/useI18n.ts`
- [ ] 实现语言切换函数
- [ ] 实现语言持久化（localStorage）
- [ ] 实现路由语言管理
- [ ] 测试语言切换功能

### 2.7 布局系统 - Header 组件
- [ ] 创建 `src/components/layout/AppHeader.vue`
- [ ] 实现导航菜单
- [ ] 实现用户下拉菜单
- [ ] 实现语言切换按钮
- [ ] 实现主题切换按钮
- [ ] 实现搜索框（可选）
- [ ] 实现响应式设计（移动端汉堡菜单）
- [ ] 测试所有功能

### 2.8 布局系统 - Sidebar 组件
- [ ] 创建 `src/components/layout/AppSidebar.vue`
- [ ] 实现侧边导航菜单
- [ ] 实现菜单折叠/展开功能
- [ ] 实现二级菜单
- [ ] 实现当前路由高亮
- [ ] 实现权限控制（根据用户角色显示菜单）
- [ ] 测试所有功能

### 2.9 布局系统 - Footer 组件
- [ ] 创建 `src/components/layout/AppFooter.vue`
- [ ] 实现页脚信息显示
- [ ] 实现友情链接
- [ ] 实现版权信息
- [ ] 实现响应式设计

### 2.10 布局系统 - 主布局
- [ ] 创建 `src/components/layout/DefaultLayout.vue`
- [ ] 组合 Header + Sidebar + Footer
- [ ] 实现布局切换（全屏/带侧边栏）
- [ ] 实现主题切换逻辑
- [ ] 测试布局响应式

### 2.11 路由架构 - 基础配置
- [ ] 创建 `src/router/index.ts`
- [ ] 配置路由模式（history）
- [ ] 配置路由基础路径
- [ ] 创建基础路由（Home, Login, 404）
- [ ] 测试路由跳转

### 2.12 路由架构 - 路由守卫
- [ ] 创建 `src/router/guards.ts`
- [ ] 实现认证守卫（requiresAuth）
- [ ] 实现权限守卫（requiresRole）
- [ ] 实现页面标题守卫
- [ ] 实现进度条（nprogress，可选）
- [ ] 测试守卫功能

### 2.13 路由架构 - 模块化路由 ⚡️ 路径更新
- [ ] 创建 `src/router/routes/base.ts` (基础路由)
- [ ] 创建 `src/router/routes/auth.ts` (认证路由)
- [ ] 创建 `src/router/routes/course.ts` (课程路由)
- [ ] 创建 `src/router/routes/user.ts` (用户路由)
- [ ] 创建 `src/router/routes/admin.ts` (管理路由)
- [ ] 在主路由中导入所有模块
- [ ] 配置路由懒加载

### 2.14 状态管理 - 用户 Store ⚡️ 路径更新
- [ ] 创建 `src/stores/modules/user.ts` (使用模块化)
- [ ] 定义用户状态接口
- [ ] 实现用户信息 state
- [ ] 实现获取用户信息 action
- [ ] 实现更新用户信息 action
- [ ] 实现 getters（isLoggedIn, userRole 等）
- [ ] 配置状态持久化

### 2.15 状态管理 - 认证 Store ⚡️ 路径更新
- [ ] 创建 `src/stores/modules/auth.ts` (使用模块化)
- [ ] 定义认证状态接口
- [ ] 实现 token state
- [ ] 实现登录 action
- [ ] 实现登出 action
- [ ] 实现 token 刷新 action
- [ ] 配置状态持久化

### 2.16 Pinia 持久化配置 ⚡️ 路径更新
- [ ] 创建 `src/config/pinia.ts` (不是 plugins)
- [ ] 配置 persistedstate 插件
- [ ] 设置持久化存储（localStorage）
- [ ] 设置持久化 key 前缀
- [ ] 测试状态持久化

**阶段二完成标志**: ✅ API 服务层可用，i18n 工作正常，布局完整，路由和状态管理就绪

---

## 📋 阶段三：认证和用户系统

### 3.1 认证组合式函数
- [ ] 创建 `src/composables/useAuth.ts`
- [ ] 实现登录逻辑
- [ ] 实现登出逻辑
- [ ] 实现注册逻辑
- [ ] 实现密码重置逻辑
- [ ] 实现 token 管理
- [ ] 导出认证方法和状态

### 3.2 登录页面
- [ ] 创建 `src/views/auth/LoginView.vue`
- [ ] 实现登录表单（邮箱/用户名 + 密码）
- [ ] 实现表单验证
- [ ] 实现记住我功能
- [ ] 实现错误提示
- [ ] 实现加载状态
- [ ] 添加跳转到注册页链接
- [ ] 测试登录流程

### 3.3 注册页面
- [ ] 创建 `src/views/auth/RegisterView.vue`
- [ ] 实现注册表单
- [ ] 实现密码强度检查
- [ ] 实现邮箱验证
- [ ] 实现验证码功能（可选）
- [ ] 实现用户协议勾选
- [ ] 测试注册流程

### 3.4 密码重置功能
- [ ] 创建 `src/views/auth/ResetPasswordView.vue`
- [ ] 实现邮箱验证
- [ ] 实现验证码发送
- [ ] 实现新密码设置
- [ ] 测试密码重置流程

### 3.5 个人资料页 - 基础结构 ⚡️ 路径更新
- [ ] 创建 `src/views/profile/ProfilePage.vue` (按文件夹组织)
- [ ] 实现 Tab 导航结构
- [ ] 实现用户信息头部卡片
- [ ] 实现响应式布局

### 3.6 个人资料页 - 用户信息 Tab ⚡️ 路径更新
- [ ] 创建 `src/components/features/user/UserInfoTab.vue` (在 features 下)
- [ ] 参考 web-template-2 的实现
- [ ] 实现用户信息展示
- [ ] 实现用户信息编辑表单
- [ ] 实现头像上传功能
- [ ] 实现保存功能
- [ ] 测试编辑功能

### 3.7 个人资料页 - 统计数据 Tab ⚡️ 路径更新
- [ ] 创建 `src/components/features/user/StatsTab.vue`
- [ ] 参考 web-template-2 的实现
- [ ] 实现学习时长统计
- [ ] 实现学习进度统计
- [ ] 实现成就展示
- [ ] 实现数据可视化（简单图表）

### 3.8 个人资料页 - 学习记录 Tab
- [ ] 创建 `src/components/profile/LearningTab.vue`
- [ ] 参考 web-template-2 的实现
- [ ] 实现学习中的课程列表
- [ ] 实现已完成的课程列表
- [ ] 实现学习进度显示
- [ ] 实现继续学习功能

### 3.9 个人资料页 - 订阅关注 Tab
- [ ] 创建 `src/components/profile/SubscriptionTab.vue`
- [ ] 参考 web-template-2 的实现
- [ ] 实现关注的用户列表
- [ ] 实现关注的话题列表
- [ ] 实现取消关注功能

### 3.10 个人资料页 - 其他 Tabs
- [ ] 参考 web-template-2 创建其他必要的 Tab 组件
- [ ] 实现我的课程 Tab
- [ ] 实现我的文章 Tab
- [ ] 实现我的路线图 Tab
- [ ] 测试所有 Tab 功能

**阶段三完成标志**: ✅ 用户可以登录注册，个人资料页功能完整

---

## 📋 阶段四：核心业务功能

待完成... （内容较多，需要分批添加）

---

## 🎯 当前进度
- 阶段一：✅ 10/10 任务组完成（100%）🎉
- 阶段二：✅ 3/16 任务组完成（约19%）
  - ✅ 2.1 API 服务层 - Axios 配置（7/7）
  - ✅ 2.2 API 服务层 - 类型定义（8/6 超额完成 - 零 unknown）
  - ✅ 2.3 API 服务层 - 模块化 API（14/7 超额完成 - 13个模块）
  - ⏳ 2.4 国际化系统 - 安装配置（0/6）
  - ⏳ 2.5 国际化系统 - 语言文件（0/5）
  - ⏳ 2.6-2.16 其他任务待完成
- 阶段三：0/10 任务完成
- 总进度：约 40%

**重要提示**: 所有路径都已更新为最佳实践结构，请严格按照新路径执行！

**阶段二当前成果**：
- ✅ 完整的 API 服务层（13个模块，单一职责）
- ✅ 零 `unknown` 的类型系统（14个类型文件 + 枚举）
- ✅ TypeScript 类型检查通过
- ⚠️ ESLint 有 56 个错误待修复（主要是模板字符串 number 类型和错误处理）

**下一步**: 修复 ESLint 错误，然后继续国际化系统
- ✅ TypeScript/ESLint/Prettier 全部通过
- ✅ Vuetify 3 正确配置（自动按需引入，无需手动导入样式）
- ✅ 环境变量管理 + 类型定义
- ✅ Vite 完整配置（代理、构建优化、代码分割）
- ✅ Router + Pinia 集成完成
- ✅ 完整的项目文档

**下一步**: 开始阶段二 - 核心基础设施开发

---

*此文档会持续更新，随着迁移进展逐步完善*
