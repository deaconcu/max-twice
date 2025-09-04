import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

// 核心页面 - 保持静态导入（首屏需要的）
import Login from '@/views/LoginView.vue'
import Main from '@/views/MainView.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: Login,
  },
  {
    path: '/',
    name: 'main',
    component: Main,
    children: [
      {
        path: 'course/list',
        name: 'courseList',
        component: () => import('@/views/CourseList.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router