import type { RouteRecordRaw } from 'vue-router'
import { baseRoutes } from './base'
import { authRoutes } from './auth'
import { courseRoutes } from './course'
import { roleRoutes } from './role'
import reviewRoutes from './review'

/**
 * 所有路由模块的统一导出
 */
export const routes: RouteRecordRaw[] = [
  ...baseRoutes,
  ...authRoutes,
  ...courseRoutes,
  ...roleRoutes,
  ...reviewRoutes,
  // TODO: 添加更多路由模块
  // ...learningRoutes,
]
