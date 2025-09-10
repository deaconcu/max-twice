import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

// 核心页面 - 保持静态导入（首屏需要的）
import Login from '@/views/LoginView.vue'
import Main from '@/views/MainView.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/about',
    name: 'about',
    // route level code-splitting
    // this generates a separate chunk (About.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
    component: () => import('@/views/AboutView.vue'),
  },
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
        path: 'admin',
        name: 'admin',
        component: () => import('@/views/AdminView.vue'),
      },
      {
        path: 'course/list',
        name: 'courseList',
        component: () => import('@/views/CourseList.vue'),
      },
      {
        path: 'ranking',
        name: 'hotRanking',
        component: () => import('@/views/HotRanking.vue'),
      },
      {
        path: 'read',
        name: 'read',
        component: () => import('@/views/ReadView.vue'),
      },
      {
        path: 'user',
        name: 'user',
        component: () => import('@/views/UserView.vue'),
      },
      {
        path: 'self',
        name: 'self',
        component: () => import('@/views/SelfView.vue'),
      },
      {
        path: 'message',
        name: 'message',
        component: () => import('@/views/Message.vue'),
      },
      {
        path: 'roadmap/:professionId?',
        name: 'roadmap',
        component: () => import('@/views/RoadmapFlow.vue'),
      },
      {
        path: 'career',
        name: 'careerCenter',
        component: () => import('@/views/CareerCenter.vue'),
      },
      {
        path: 'learning',
        name: 'learning',
        component: () => import('@/views/LearningView.vue'),
      },
      {
        path: 'memory/review',
        name: 'memoryReview',
        component: () => import('@/views/MemoryReviewView.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router