# TypeScript 迁移计划

## 迁移目标
将现有的 JavaScript Vue 3 项目完整迁移到 TypeScript，提升代码质量和开发体验。

## 迁移策略
- **方式**：创建新的 TypeScript 项目，逐步迁移代码
- **原则**：从底层到上层，从简单到复杂
- **时间**：预计 2-3 周

## 迁移任务清单

### 第一阶段：项目初始化（第 1-2 天）

- [ ] **1. 创建 TypeScript 项目基础结构**
  - 使用 `npm create vite@latest web-ts -- --template vue-ts` 创建项目
  - 配置 tsconfig.json
  - 设置 ESLint 和 Prettier

- [ ] **2. 安装必要的依赖包**
  ```bash
  # 核心依赖
  npm install vuetify@3.7.6 axios@1.9.0 pinia@2.3.1 vue-router@4.5.0
  
  # UI 和工具库
  npm install @mdi/font @vue-flow/core dagre chart.js vue-chartjs
  
  # 编辑器相关
  npm install @tiptap/vue-3 @tiptap/starter-kit
  
  # 类型定义
  npm install -D @types/node
  ```

- [ ] **3. 复制静态资源和配置文件**
  - 复制 `public/` 目录
  - 复制 `src/assets/` 目录
  - 复制环境变量文件 (`.env`, `.env.development`, `.env.production`)
  - 迁移 `vite.config.ts` 配置

### 第二阶段：基础设施迁移（第 3-5 天）

- [ ] **4. 创建类型定义文件**
  - 创建 `src/types/` 目录
  - 定义全局类型 (`index.d.ts`)
  - API 响应类型 (`api.d.ts`)
  - 业务实体类型 (`models.d.ts`)
  
  ```typescript
  // src/types/models.d.ts 示例
  export interface User {
    id: number
    name: string
    email: string
    avatar?: string
  }
  
  export interface Post {
    id: number
    title: string
    content: string
    author: User
    createdAt: string
  }
  ```

- [ ] **5. 迁移常量和工具函数**
  - `src/constants/` - 添加 `as const` 断言
  - `src/utils/` - 添加参数和返回值类型
  - 示例：
  ```typescript
  // 原始 JS
  export function formatDate(date) {
    return new Date(date).toLocaleDateString()
  }
  
  // TypeScript
  export function formatDate(date: string | Date): string {
    return new Date(date).toLocaleDateString()
  }
  ```

- [ ] **6. 迁移 API 服务层**
  - 创建 API 请求和响应的类型定义
  - 重构 axios 实例配置
  - 为每个 API 方法添加类型
  ```typescript
  // src/services/api/userService.ts
  import type { User } from '@/types/models'
  
  export const userService = {
    async getUser(id: number): Promise<User> {
      const { data } = await api.get<User>(`/users/${id}`)
      return data
    },
    
    async updateUser(id: number, userData: Partial<User>): Promise<User> {
      const { data } = await api.put<User>(`/users/${id}`, userData)
      return data
    }
  }
  ```

### 第三阶段：状态和路由（第 6-7 天）

- [ ] **7. 迁移 Pinia 状态管理**
  - 定义 store 的状态类型
  - 迁移 actions 和 getters
  ```typescript
  // src/stores/user.ts
  interface UserState {
    currentUser: User | null
    isLoggedIn: boolean
  }
  
  export const useUserStore = defineStore('user', () => {
    const currentUser = ref<User | null>(null)
    const isLoggedIn = computed(() => currentUser.value !== null)
    
    function login(user: User) {
      currentUser.value = user
    }
    
    return { currentUser, isLoggedIn, login }
  })
  ```

- [ ] **8. 迁移路由配置**
  - 定义路由 meta 类型
  - 添加路由守卫类型
  ```typescript
  // src/router/index.ts
  import type { RouteRecordRaw } from 'vue-router'
  
  const routes: RouteRecordRaw[] = [
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/HomeView.vue'),
      meta: {
        requiresAuth: false,
        title: '首页'
      }
    }
  ]
  ```

### 第四阶段：组件迁移（第 8-12 天）

- [ ] **9. 迁移公共组件**
  - 基础 UI 组件（Button, Card, Modal 等）
  - 定义 props 和 emits 类型
  ```vue
  <!-- src/components/common/UserCard.vue -->
  <script setup lang="ts">
  import type { User } from '@/types/models'
  
  interface Props {
    user: User
    showDetails?: boolean
  }
  
  interface Emits {
    (e: 'click', user: User): void
    (e: 'update', id: number): void
  }
  
  const props = withDefaults(defineProps<Props>(), {
    showDetails: false
  })
  
  const emit = defineEmits<Emits>()
  </script>
  ```

- [ ] **10. 迁移布局组件**
  - Header, Footer, Sidebar
  - Navigation 组件
  - Layout 容器组件

- [ ] **11. 迁移页面组件**
  - 按模块迁移（用户、文章、课程等）
  - 每个模块包含：
    - 列表页
    - 详情页
    - 编辑页

### 第五阶段：测试和优化（第 13-15 天）

- [ ] **12. 测试和调试**
  - 运行类型检查：`npm run typecheck`
  - 修复类型错误
  - 功能测试
  - 性能优化
  - 打包构建测试

## 迁移顺序详细说明

### 优先级高（必须先迁移）
1. **类型定义** - 其他所有代码都依赖这些类型
2. **工具函数** - 被多处调用，需要先定义好类型
3. **API 服务** - 为组件提供数据类型

### 优先级中（核心功能）
4. **Pinia Stores** - 状态管理
5. **路由配置** - 应用导航
6. **公共组件** - 被多处复用

### 优先级低（可以最后迁移）
7. **页面组件** - 依赖上述所有内容
8. **样式文件** - 不需要类型，直接复制

## 常见问题和解决方案

### 1. 第三方库没有类型定义
```bash
# 尝试安装 @types 包
npm install -D @types/library-name

# 如果没有，创建声明文件
// src/types/library-name.d.ts
declare module 'library-name' {
  export function someFunction(): void
}
```

### 2. any 类型过多
- 初期可以使用 any，后期逐步替换为具体类型
- 使用 `// @ts-ignore` 临时忽略错误
- 配置 tsconfig.json 的 `strict: false` 降低严格度

### 3. Vue 组件类型推导问题
```typescript
// 使用 defineComponent 提供更好的类型推导
import { defineComponent } from 'vue'

export default defineComponent({
  // 组件选项
})
```

### 4. import 路径问题
```typescript
// tsconfig.json 配置路径别名
{
  "compilerOptions": {
    "paths": {
      "@/*": ["src/*"]
    }
  }
}
```

## 完成标准

- [ ] 所有 .js 文件已转换为 .ts
- [ ] 所有 .vue 文件添加了 `lang="ts"`
- [ ] 运行 `npm run typecheck` 无错误
- [ ] 运行 `npm run build` 构建成功
- [ ] 所有功能测试通过
- [ ] 代码审查完成

## 时间估算

| 阶段 | 预计时间 | 实际时间 |
|------|---------|---------|
| 项目初始化 | 1-2 天 | - |
| 基础设施迁移 | 3-5 天 | - |
| 状态和路由 | 2 天 | - |
| 组件迁移 | 5 天 | - |
| 测试和优化 | 2-3 天 | - |
| **总计** | **13-15 天** | - |

## 注意事项

1. **保持原项目运行**：迁移期间不要修改原项目
2. **逐步迁移**：每完成一个模块就测试
3. **版本控制**：使用 Git 管理迁移进度
4. **团队协作**：如有团队成员，分模块并行迁移
5. **文档更新**：同步更新开发文档

## 迁移后的收益

- ✅ **类型安全**：编译时发现错误，减少运行时 bug
- ✅ **更好的 IDE 支持**：自动补全、重构、跳转
- ✅ **代码可维护性**：类型即文档，降低理解成本
- ✅ **团队协作**：统一的类型定义，减少沟通成本
- ✅ **重构信心**：类型检查保证重构安全性

---

*最后更新：2025-09-04*