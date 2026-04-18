# Max Twice - Web Frontend

基于 Vue 3 + TypeScript + Vuetify 3 的现代化前端项目

## 📋 项目简介

Max Twice 是一个学习管理平台的前端项目，采用最新的前端技术栈和最佳实践构建。

## 🚀 技术栈

### 核心框架
- **Vue 3.5.24** - 渐进式 JavaScript 框架，使用 Composition API
- **TypeScript 5.9.3** - JavaScript 的超集，提供静态类型检查
- **Vite 7.2.2** - 下一代前端构建工具

### UI 框架
- **Vuetify 3.10.9** - Vue 的 Material Design 组件框架
- **@mdi/font 7.4.47** - Material Design Icons

### 状态管理 & 路由
- **Vue Router 4.6.3** - Vue.js 官方路由
- **Pinia 2.3.1** - Vue 的状态管理库
- **pinia-plugin-persistedstate 3.2.3** - Pinia 状态持久化

### HTTP 客户端
- **Axios 1.13.2** - 基于 Promise 的 HTTP 客户端

### 开发工具
- **ESLint 9.39.1** - 代码质量检查
- **Prettier 3.6.2** - 代码格式化
- **TypeScript ESLint** - TypeScript 代码检查
- **Husky + lint-staged** - Git 提交前代码检查

## 📦 项目结构

```
web/
├── public/                      # 静态资源
├── src/
│   ├── api/                     # API 请求层
│   │   └── modules/             # API 模块化
│   ├── assets/                  # 静态资源（构建处理）
│   ├── components/              # 组件库
│   │   ├── base/                # 基础 UI 组件
│   │   ├── layout/              # 布局组件
│   │   └── features/            # 业务功能组件
│   ├── composables/             # 组合式函数
│   ├── config/                  # 配置文件
│   ├── router/                  # 路由配置
│   ├── stores/                  # Pinia 状态管理
│   ├── views/                   # 页面视图
│   └── ...
├── .env.development             # 开发环境变量
├── .env.production              # 生产环境变量
├── vite.config.ts               # Vite 配置
└── package.json                 # 项目依赖
```

## 🛠️ 开发命令

### 安装依赖
```bash
npm install
```

### 开发服务器
```bash
npm run dev
```
访问 http://localhost:5174

### 生产构建
```bash
npm run build
```

### 代码检查
```bash
# ESLint 检查并自动修复
npm run lint

# Prettier 格式化
npm run format

# TypeScript 类型检查
npm run type-check
```

## ⚙️ 环境变量

创建 `.env.local` 文件（不会被提交到 git）：

```env
# 应用配置
VITE_APP_TITLE=Max Twice
VITE_API_URL=http://localhost:9202
VITE_APP_PORT=5174
```

## 🎨 代码规范

### TypeScript 配置
- ✅ 启用所有严格类型检查
- ✅ 禁止未使用的局部变量和参数
- ✅ 索引访问类型安全（noUncheckedIndexedAccess）

### ESLint 规则
- Vue 3 推荐规则
- TypeScript 严格规则
- 禁止使用 `any` 类型
- 禁止使用 `console.log`（允许 warn 和 error）

## 🏗️ 架构设计

### 分层架构
- **表现层**: `views/` + `components/`
- **业务层**: `composables/` + `stores/`
- **数据层**: `api/` + `types/`
- **工具层**: `utils/` + `constants/`

### 组件分层
- **base/**: 纯 UI 组件，无业务逻辑，高度可复用
- **layout/**: 布局组件，定义页面结构
- **features/**: 业务组件，包含特定领域逻辑

## 📝 提交规范

使用约定式提交（Conventional Commits）：

```
feat: 新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式调整
refactor: 重构
perf: 性能优化
test: 测试相关
chore: 构建/工具相关
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: add some amazing feature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

### 开发流程
1. 确保通过所有代码检查：`npm run lint && npm run type-check`
2. 提交前会自动运行 lint-staged 检查
3. 保持代码整洁和可维护性

## 📄 许可证

[MIT](LICENSE)

---

**快速开始**
```bash
npm install
npm run dev
```

访问 http://localhost:5174 开始开发！
