import apiClient from '../client'

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
  }): Promise<void> {
    return apiClient.post('/errors/frontend', data)
  },
}
