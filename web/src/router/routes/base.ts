import type { RouteRecordRaw } from 'vue-router'

/**
 * 基础路由
 * 包含：首页、错误页、关于页等不需要认证的页面
 */
export const baseRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/home/HomePage.vue'),
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/profile/ProfilePage.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/error/:code',
    name: 'error',
    component: () => import('@/views/error/ErrorPage.vue'),
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    redirect: '/error/404',
  },
]
