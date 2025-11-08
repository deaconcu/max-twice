import apiClient from '../client'
import type { ApiResponse } from '@/types/api'
import type { DailyStatsDTO, UserStatsDTO } from '@/types/user'
import type { PlatformStats } from '@/types/stats'

/**
 * 统计相关 API
 * 参考：web-ts/src/services/api/v1/apiServiceV1.ts (statsServiceV1)
 */
export const statsApi = {
  /**
   * 记录浏览
   */
  recordView(articleId: number, userId?: number, ipAddress?: string): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/stats/views', {
      articleId,
      userId,
      ipAddress,
    })
  },

  /**
   * 获取用户今日统计
   */
  getUserTodayStats(userId: number): Promise<ApiResponse<DailyStatsDTO>> {
    return apiClient.get(`/v1/stats/users/${String(userId)}/today`)
  },

  /**
   * 获取用户昨日统计
   */
  getUserYesterdayStats(userId: number): Promise<ApiResponse<DailyStatsDTO>> {
    return apiClient.get(`/v1/stats/users/${String(userId)}/yesterday`)
  },

  /**
   * 获取用户历史统计
   */
  getUserHistoryStats(userId: number, days = 7): Promise<ApiResponse<DailyStatsDTO[]>> {
    return apiClient.get(`/v1/stats/users/${String(userId)}/history`, {
      params: { days },
    })
  },

  /**
   * 获取用户周期统计
   */
  getUserPeriodStats(userId: number, days = 7): Promise<ApiResponse<UserStatsDTO>> {
    return apiClient.get(`/v1/stats/users/${String(userId)}/period`, {
      params: { days },
    })
  },

  /**
   * 获取用户全部统计
   */
  getUserAllTimeStats(userId: number): Promise<ApiResponse<UserStatsDTO>> {
    return apiClient.get(`/v1/stats/users/${String(userId)}/all-time`)
  },

  /**
   * 手动同步统计
   */
  syncManual(): Promise<ApiResponse<string>> {
    return apiClient.post('/v1/stats/sync/manual')
  },

  /**
   * 健康检查
   */
  getHealth(): Promise<ApiResponse<string>> {
    return apiClient.get('/v1/stats/health')
  },

  /**
   * 同步指定日期的统计
   */
  syncDate(date: string): Promise<ApiResponse<string>> {
    return apiClient.post('/v1/stats/sync/date', { date })
  },

  /**
   * 获取平台统计
   */
  getPlatformStats(): Promise<ApiResponse<PlatformStats>> {
    return apiClient.get('/v1/stats/platform')
  },
}
