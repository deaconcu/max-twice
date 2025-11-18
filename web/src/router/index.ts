import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore, useAuthStore } from '@/stores'
import { isSuperAdmin, isAdmin, isModerator } from '@/utils/permission'
import { routes } from './routes'

/**
 * 扩展路由元信息类型
 */
declare module 'vue-router' {
  interface RouteMeta {
    requireAuth?: boolean // 是否需要登录
    requireAdmin?: boolean // 是否需要管理员权限
    requireModerator?: boolean // 是否需要审核员权限
    requireSuperAdmin?: boolean // 是否需要超级管理员权限
  }
}

/**
 * 创建路由实例
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    // 如果有保存的位置（浏览器前进/后退），恢复到该位置
    if (savedPosition) {
      return savedPosition
    }
    // 如果有hash锚点，滚动到对应元素
    if (to.hash) {
      return { el: to.hash, behavior: 'smooth' }
    }
    // 否则滚动到顶部
    return { top: 0 }
  },
})

/**
 * 全局前置守卫 - 权限检查
 */
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const authStore = useAuthStore()
  const currentUser = userStore.currentUser

  // 检查是否需要登录
  if (to.meta.requireAuth && !authStore.isAuthenticated) {
    next({
      path: '/login',
      query: { redirect: to.fullPath },
    })
    return
  }

  // 检查是否需要超级管理员权限
  if (to.meta.requireSuperAdmin && !isSuperAdmin(currentUser)) {
    next('/error/403')
    return
  }

  // 检查是否需要管理员权限
  if (to.meta.requireAdmin && !isAdmin(currentUser)) {
    next('/error/403')
    return
  }

  // 检查是否需要审核员权限
  if (to.meta.requireModerator && !isModerator(currentUser)) {
    next('/error/403')
    return
  }

  next()
})

export default router
