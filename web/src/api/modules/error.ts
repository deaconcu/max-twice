import apiClient from '../client'
import type { ApiResponse } from '@/types/api'

/**
 * 错误上报 API
 */
export const errorApi = {
  /**
   * 上报前端错误
   */
  report(data: {
    errorType: string
    message: string
    stackTrace?: string
    url: string
    userAgent: string
    extraData?: string
  }): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/errors/frontend', data)
  },
}
