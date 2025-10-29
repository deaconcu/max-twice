import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { isAdmin, isModerator, isSuperAdmin } from '@/utils/permission'

// 核心页面 - 保持静态导入（首屏需要的）
import Login from '@/views/LoginView.vue'
import Main from '@/views/MainView.vue'

// 扩展路由元信息类型
declare module 'vue-router' {
  interface RouteMeta {
    requireAuth?: boolean // 是否需要登录
    requireAdmin?: boolean // 是否需要管理员权限
    requireModerator?: boolean // 是否需要审核员权限
    requireSuperAdmin?: boolean // 是否需要超级管理员权限
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/about',
    name: 'about',
    // route level code-splitting
    // this generates a separate chunk (About.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
    component: () => import('@/views/AboutView.vue')
  },
  {
    path: '/login',
    name: 'login',
    component: Login
  },
  {
    path: '/error',
    name: 'error',
    component: () => import('@/views/ErrorView.vue')
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
        meta: { requireAuth: true, requireModerator: true }
      },
      {
        path: 'course/list',
        name: 'courseList',
        component: () => import('@/views/CourseList.vue')
      },
      {
        path: 'ranking',
        name: 'hotRanking',
        component: () => import('@/views/HotRanking.vue')
      },
      {
        path: 'read',
        name: 'read',
        component: () => import('@/views/ReadView.vue')
      },
      {
        path: 'user/:username',
        name: 'user',
        component: () => import('@/views/UserView.vue')
      },
      {
        path: 'self',
        name: 'self',
        component: () => import('@/views/SelfView.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'message',
        name: 'message',
        component: () => import('@/views/Message.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'roadmap/:professionId?',
        name: 'roadmap',
        component: () => import('@/views/RoadmapFlow.vue')
      },
      {
        path: 'career',
        name: 'careerCenter',
        component: () => import('@/views/CareerCenter.vue')
      },
      {
        path: 'learning',
        name: 'learning',
        component: () => import('@/views/LearningView.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'memory/review',
        name: 'memoryReview',
        component: () => import('@/views/MemoryReviewView.vue'),
        meta: { requireAuth: true }
      }
    ]
  },
  {
    // 404 兜底路由 - 必须放在最后
    path: '/:pathMatch(.*)*',
    redirect: '/error'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const currentUser = userStore.currentUser

  // 检查是否需要登录
  if (to.meta.requireAuth && !currentUser) {
    next({
      path: '/error',
      query: { type: '401', message: '请先登录后再访问' }
    })
    return
  }

  // 检查是否需要超级管理员权限
  if (to.meta.requireSuperAdmin && !isSuperAdmin(currentUser)) {
    next({
      path: '/error',
      query: { type: '403', message: '需要超级管理员权限' }
    })
    return
  }

  // 检查是否需要管理员权限
  if (to.meta.requireAdmin && !isAdmin(currentUser)) {
    next({
      path: '/error',
      query: { type: '403', message: '需要管理员权限' }
    })
    return
  }

  // 检查是否需要审核员权限
  if (to.meta.requireModerator && !isModerator(currentUser)) {
    next({
      path: '/error',
      query: { type: '403', message: '需要审核员权限' }
    })
    return
  }

  next()
})

export default router