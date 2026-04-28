import apiClient from '../client'
import type { CursorPage } from '@/types/api'

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
export type ContentType = 'role' | 'roadmap' | 'course' | 'post' | 'memory_card_deck'

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
  toggle(contentType: ContentType, contentId: number): Promise<boolean> {
    return apiClient.post(`/bookmarks/${contentType}/${contentId}`)
  },

  /**
   * 获取用户收藏列表（分页）
   * @param contentType 内容类型
   * @param cursor 分页游标
   * @param limit 每页数量
   */
  getBookmarks(contentType: ContentType, cursor?: string, limit = 20): Promise<CursorPage<Bookmark>> {
    return apiClient.get(`/bookmarks/${contentType}/list`, {
      params: { cursor, limit },
    })
  },
}
