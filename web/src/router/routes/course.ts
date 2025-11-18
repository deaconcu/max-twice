import type { RouteRecordRaw } from 'vue-router'

/**
 * 课程路由
 * 包含：课程列表、课程详情、课程学习等
 */
export const courseRoutes: RouteRecordRaw[] = [
  {
    path: '/courses',
    name: 'courses',
    component: () => import('@/views/course/CourseListPage.vue'),
    meta: {
      title: '课程中心',
      requireAuth: true,
    },
  },
  {
    path: '/courses/:id',
    name: 'course-detail',
    component: () => import('@/views/course/CourseDetailPage.vue'),
    meta: {
      title: '课程详情',
      requireAuth: true,
    },
  },
  {
    path: '/read',
    name: 'content-read',
    component: () => import('@/views/course/ReadPageRouter.vue'),
    meta: {
      title: '内容阅读',
      requireAuth: true,
    },
  },
]
