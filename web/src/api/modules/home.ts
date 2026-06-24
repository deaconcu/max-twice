/**
 * 首页 API
 */
import apiClient from '../client'
import type { HomePage } from '@/types/home'

export const homeApi = {
  /**
   * 获取首页聚合数据
   */
  getHomePageData(): Promise<HomePage> {
    return apiClient.get('/home')
  },
}
