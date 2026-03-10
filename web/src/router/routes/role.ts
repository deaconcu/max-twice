import type { RouteRecordRaw } from 'vue-router'

/**
 * 角色相关路由
 */
export const roleRoutes: RouteRecordRaw[] = [
  {
    path: '/role',
    name: 'RoleList',
    component: () => import('@/views/role/RoleListPage.vue'),
    meta: {
      requiresAuth: false,
    },
  },
  {
    path: '/role/:id',
    name: 'RoleDetail',
    component: () => import('@/views/role/RoleDetailPage.vue'),
    meta: {
      requiresAuth: false,
    },
  },
  {
    path: '/role/:id/roadmap/create',
    name: 'RoadmapCreate',
    component: () => import('@/views/role/RoadmapCreatePage.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/role/:id/roadmap/:roadmapId/edit',
    name: 'RoadmapEdit',
    component: () => import('@/views/role/RoadmapCreatePage.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/roadmap/:id',
    name: 'RoadmapDetail',
    component: () => import('@/views/role/RoadmapDetailPage.vue'),
    meta: {
      requiresAuth: false,
    },
  },
]
