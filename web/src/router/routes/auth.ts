import type { RouteRecordRaw } from 'vue-router'

/**
 * 认证路由
 * 包含：登录、注册、邮箱验证等（统一使用 AuthPage 组件）
 */
export const authRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/AuthPage.vue'),
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/AuthPage.vue'),
  },
  {
    path: '/verify-email',
    name: 'verify-email',
    component: () => import('@/views/auth/AuthPage.vue'),
  },
  // TODO: 添加更多认证相关路由
  // {
  //   path: '/reset-password',
  //   name: 'reset-password',
  //   component: () => import('@/views/auth/AuthPage.vue'),
  // },
]
