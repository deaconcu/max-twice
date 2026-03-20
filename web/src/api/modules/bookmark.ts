import apiClient from '../client'
import type { ApiResponse } from '@/types/api'

/**
 * 收藏记录 DTO（带关联对象）
 */
export interface Bookmark {
  id: number
  userId: number
  objectId: number
  objectType: number
  parentId?: number
  createdAt: string
  /** 关联对象（根据 objectType 决定具体类型） */
  object: unknown
}

/**
 * 内容类型
 */
export type ContentType = 'profession' | 'roadmap' | 'course' | 'post' | 'memory_card'

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
   * @param lastId 上一页最后一条记录的ID（首页不传）
   * @param limit 每页数量
   */
  getBookmarks(
    contentType: ContentType,
    lastId?: number,
    limit: number = 20
  ): Promise<ApiResponse<Bookmark[]>> {
    return apiClient.get(`/v1/bookmarks/${contentType}/list`, {
      params: { lastId, limit },
    })
  },
}
