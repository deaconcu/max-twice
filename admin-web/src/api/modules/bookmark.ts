import apiClient from '../client'
import type { ApiResponse } from '@/types/api'

/**
 * 收藏相关类型
 */
export interface Bookmark {
  id: number
  userId: number
  objectId: number
  objectType: number
  parentId?: number
  createdAt: string
}

/**
 * 内容类型
 */
export type ContentType = 'role' | 'roadmap' | 'course' | 'post' | 'memory_card'

/**
 * 收藏相关 API
 */
export const bookmarkApi = {
  /**
   * 切换收藏状态
   * @param contentType 内容类型
   * @param contentId 内容ID
   * @returns true=已收藏, false=已取消收藏
   */
  toggle(contentType: ContentType, contentId: number): Promise<ApiResponse<boolean>> {
    return apiClient.post(`/v1/bookmarks/${contentType}/${contentId}`)
  },

  /**
   * 获取用户收藏列表（分页）
   * @param contentType 内容类型
   * @param lastId 上一页最后一条记录的ID（首页传0）
   * @param limit 每页数量
   */
  getBookmarks(
    contentType: ContentType,
    lastId: number = 0,
    limit: number = 20
  ): Promise<ApiResponse<Bookmark[]>> {
    return apiClient.get(`/v1/bookmarks/${contentType}/list`, {
      params: { lastId, limit },
    })
  },
}
