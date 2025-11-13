import type { RouteRecordRaw } from 'vue-router'

/**
 * 职业相关路由
 */
export const careerRoutes: RouteRecordRaw[] = [
  {
    path: '/career',
    name: 'CareerList',
    component: () => import('@/views/career/CareerListPage.vue'),
    meta: {
      requiresAuth: false,
    },
  },
  {
    path: '/career/:id',
    name: 'CareerDetail',
    component: () => import('@/views/career/CareerDetailPage.vue'),
    meta: {
      requiresAuth: false,
    },
  },
  {
    path: '/career/:id/roadmap/create',
    name: 'RoadmapCreate',
    component: () => import('@/views/career/RoadmapCreatePage.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/roadmap/:careerId/detail/:id',
    name: 'RoadmapDetail',
    component: () => import('@/views/career/RoadmapDetailPage.vue'),
    meta: {
      requiresAuth: false,
    },
  },
]
