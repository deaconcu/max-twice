/**
 * 首页 API
 */
import apiClient from '../client'
import type { ApiResponse } from '@/types/api'
import type { HomePage } from '@/types/home'

export const homeApi = {
  /**
   * 获取首页聚合数据
   */
  getHomePageData(): Promise<ApiResponse<HomePage>> {
    return apiClient.get('/v1/home')
  },
}
