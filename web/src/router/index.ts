import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores'
import { useValidationConfigStore } from '@/stores/validationConfig'
import { routes } from './routes'

/**
 * 扩展路由元信息类型
 */
declare module 'vue-router' {
  interface RouteMeta {
    requireAuth?: boolean // 是否需要登录
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
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 定期检查验证配置更新（带节流，避免频繁请求）
  // 仅在跨页面导航时检查（不是首次加载）
  if (from.name) {
    const validationStore = useValidationConfigStore()
    validationStore.checkAndLoad().catch((error) => {
      console.error('[Router] 检查验证配置失败', error)
    })
  }

  // 检查是否需要登录
  if (to.meta.requireAuth && !authStore.isAuthenticated) {
    next({
      path: '/login',
      query: { redirect: to.fullPath },
    })
    return
  }

  next()
})

export default router
