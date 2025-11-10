import type { RouteRecordRaw } from 'vue-router'
import { baseRoutes } from './base'
import { authRoutes } from './auth'

/**
 * 所有路由模块的统一导出
 */
export const routes: RouteRecordRaw[] = [
  ...baseRoutes,
  ...authRoutes,
  // TODO: 添加更多路由模块
  // ...courseRoutes,
  // ...learningRoutes,
  // ...adminRoutes,
]
