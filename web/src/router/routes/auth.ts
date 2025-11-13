import type { RouteRecordRaw } from 'vue-router'

/**
 * 认证路由
 * 包含：登录、注册、密码重置等
 */
export const authRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginPage.vue'),
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterPage.vue'),
  },
  // TODO: 添加更多认证相关路由
  // {
  //   path: '/reset-password',
  //   name: 'reset-password',
  //   component: () => import('@/views/auth/ResetPasswordPage.vue'),
  // },
  // {
  //   path: '/verify-email',
  //   name: 'verify-email',
  //   component: () => import('@/views/auth/VerifyEmailPage.vue'),
  // },
]
