import type { RouteRecordRaw } from 'vue-router'

/**
 * 认证路由
 * 包含：登录、注册、邮箱验证、忘记密码（统一使用 AuthPage 组件）
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
  {
    path: '/forgot-password',
    name: 'forgot-password',
    component: () => import('@/views/auth/AuthPage.vue'),
  },
  {
    path: '/reset-password/verify',
    name: 'reset-password-verify',
    component: () => import('@/views/auth/AuthPage.vue'),
  },
  {
    path: '/reset-password/confirm',
    name: 'reset-password-confirm',
    component: () => import('@/views/auth/AuthPage.vue'),
  },
]
