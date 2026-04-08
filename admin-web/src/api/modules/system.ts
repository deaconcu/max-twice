import apiClient from '../client'
import type { ApiResponse } from '@/types/api'

/**
 * 系统配置相关 API（公开接口）
 * 参考：web-ts/src/services/api/v1/apiServiceV1.ts (systemServiceV1)
 */
export const systemApi = {
  /**
   * 获取课程分类数据
   */
  getCourseCategories(): Promise<ApiResponse> {
    return apiClient.get('/v1/public/course-categories')
  },

  /**
   * 获取职业分类数据
   */
  getRoleCategories(): Promise<ApiResponse> {
    return apiClient.get('/v1/public/role-categories')
  },

  /**
   * 获取只读模式状态（公开接口，无需登录）
   */
  getReadonlyMode(): Promise<ApiResponse<{ enabled: boolean }>> {
    return apiClient.get('/v1/public/readonly-mode')
  },
}
