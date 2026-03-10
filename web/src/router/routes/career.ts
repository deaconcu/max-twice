import type { RouteRecordRaw } from 'vue-router'

/**
 * 角色相关路由
 */
export const careerRoutes: RouteRecordRaw[] = [
  {
    path: '/role',
    name: 'CareerList',
    component: () => import('@/views/career/CareerListPage.vue'),
    meta: {
      requiresAuth: false,
    },
  },
  {
    path: '/role/:id',
    name: 'CareerDetail',
    component: () => import('@/views/career/CareerDetailPage.vue'),
    meta: {
      requiresAuth: false,
    },
  },
  {
    path: '/role/:id/roadmap/create',
    name: 'RoadmapCreate',
    component: () => import('@/views/career/RoadmapCreatePage.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/role/:id/roadmap/:roadmapId/edit',
    name: 'RoadmapEdit',
    component: () => import('@/views/career/RoadmapCreatePage.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/roadmap/:id',
    name: 'RoadmapDetail',
    component: () => import('@/views/career/RoadmapDetailPage.vue'),
    meta: {
      requiresAuth: false,
    },
  },
]
