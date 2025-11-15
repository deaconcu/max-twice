import type { RouteRecordRaw } from 'vue-router'

/**
 * 管理后台路由
 */
export const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/admin/AdminView.vue'),
    meta: {
      requiresAuth: true,
      roles: ['admin', 'super_admin'], // 需要管理员权限
      title: '管理后台'
    }
  }
]