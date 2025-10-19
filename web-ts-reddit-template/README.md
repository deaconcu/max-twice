# Reddit Style Template

Reddit 风格的 Vue 3 + Vuetify 模版项目

## 特性

- ✅ Flat Design（扁平化设计，无阴影）
- ✅ 全屏响应式布局
- ✅ Vue 3 + TypeScript + Composition API
- ✅ Vuetify 3 UI 框架
- ✅ Vue Router + Pinia
- ✅ Mock 数据

## 设计风格

- 简约的 Reddit 风格
- 使用边框和背景色区分层次
- 圆角按钮和输入框
- Reddit 橙色 (#FF4500) 作为主色调
- 蓝色 (#0079D3) 作为辅助色

## 安装和运行

```bash
# 安装依赖
cd web-ts-reddit-template
npm install

# 开发模式
npm run dev

# 构建
npm run build

# 预览构建结果
npm run preview
```

## 项目结构

```
web-ts-reddit-template/
├── src/
│   ├── components/      # 组件
│   ├── views/          # 视图页面
│   │   └── LoginView.vue
│   ├── router/         # 路由配置
│   ├── stores/         # Pinia 状态管理
│   ├── types/          # TypeScript 类型
│   ├── App.vue
│   └── main.ts
├── index.html
├── vite.config.ts
├── tsconfig.json
└── package.json
```

## 已实现页面

- [x] 登录页面 (LoginView)

## TODO

- [ ] 首页帖子列表
- [ ] 帖子详情页
- [ ] 评论系统
- [ ] 用户页面
- [ ] Header 和 Sidebar 组件
