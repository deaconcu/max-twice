/**
 * 复习相关路由
 */
import type { RouteRecordRaw } from 'vue-router'

const reviewRoutes: RouteRecordRaw[] = [
  {
    path: '/review',
    name: 'Review',
    component: () => import('@/views/review/ReviewPage.vue'),
    meta: {
      title: 'review.title',
      requiresAuth: true,
    },
  },
]

export default reviewRoutes
