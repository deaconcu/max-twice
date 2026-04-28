import apiClient from '../client'
import type { UserDailyStatsDTO, UserStatsWithDailyDTO, UserStatsDTO } from '@/types/user'
import type { PlatformStats, HeatmapData } from '@/types/stats'

/**
 * 统计相关 API
 */
export const statsApi = {
  /**
   * 记录浏览
   */
  recordView(articleId: number, userId?: number, ipAddress?: string): Promise<void> {
    return apiClient.post('/stats/views', {
      articleId,
      userId,
      ipAddress,
    })
  },

  /**
   * 获取用户今日统计
   */
  getUserTodayStats(userId: number): Promise<UserDailyStatsDTO> {
    return apiClient.get(`/stats/users/${String(userId)}/today`)
  },

  /**
   * 获取用户历史统计（包含总计和每日明细）
   */
  getUserHistoryStats(userId: number, days = 7): Promise<UserStatsWithDailyDTO> {
    return apiClient.get(`/stats/users/${String(userId)}/history`, {
      params: { days },
    })
  },

  /**
   * 获取用户全部统计
   */
  getUserAllTimeStats(userId: number): Promise<UserStatsDTO> {
    return apiClient.get(`/stats/users/${String(userId)}/all-time`)
  },

  /**
   * 手动同步统计
   */
  syncManual(): Promise<string> {
    return apiClient.post('/stats/sync/manual')
  },

  /**
   * 同步指定日期的统计
   */
  syncDate(date: string): Promise<string> {
    return apiClient.post('/stats/sync/date', { date })
  },

  /**
   * 获取平台统计
   */
  getPlatformStats(): Promise<PlatformStats> {
    return apiClient.get('/stats/platform')
  },

  /**
   * 获取用户热力图数据
   */
  getHeatmap(userId: number, months = 12): Promise<HeatmapData> {
    return apiClient.get(`/stats/users/${String(userId)}/heatmap`, {
      params: { months },
    })
  },
}
