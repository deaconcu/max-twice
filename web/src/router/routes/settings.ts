import type { RouteRecordRaw } from 'vue-router'

/**
 * 账户设置相关路由（均需登录）。
 */
export const settingsRoutes: RouteRecordRaw[] = [
  {
    path: '/settings/password',
    name: 'SettingsPassword',
    component: () => import('@/views/settings/PasswordPage.vue'),
    meta: { requireAuth: true },
  },
]
