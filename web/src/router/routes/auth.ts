import type { RouteRecordRaw } from 'vue-router'

/**
 * 认证路由
 * 纯邮箱验证码登录：/login 输邮箱 → /verify-email 输验证码
 */
export const authRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/AuthPage.vue'),
  },
  {
    path: '/verify-email',
    name: 'verify-email',
    component: () => import('@/views/auth/AuthPage.vue'),
  },
]
