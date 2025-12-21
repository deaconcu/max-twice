/**
 * API 模块统一导出
 * 所有 API 调用都从这里导入
 */

export { authApi } from './modules/auth'
export { userApi, followApi } from './modules/user'
export { courseApi, subscriptionApi } from './modules/course'
export { postApi } from './modules/post'
export { commentApi } from './modules/comment'
export { roadmapApi } from './modules/roadmap'
export { professionApi } from './modules/profession'
export { upvoteApi } from './modules/upvote'
export { messageApi } from './modules/message'
export { progressApi } from './modules/progress'
export { statsApi } from './modules/stats'
export { pageApi } from './modules/page'
export { systemApi } from './modules/system'
export * as memoryApi from './modules/memory'
export { adminApi } from './modules/admin'
export { imageApi } from './modules/image'

// 导出 apiClient 和 ApiError 供特殊场景使用
export { default as apiClient } from './client'
export { ApiError } from './client'
