import apiClient from '../client'
import type { ApiResponse } from '@/types/api'
import type { Comment } from '@/types/comment'
import type { ObjectType } from '@/enums'

/**
 * 分页响应
 */
export interface KeysetPageResponse<T> {
  items: T[]
  hasMore: boolean
  nextCursor?: {
    lastScore: number
    lastId: number
  }
}

/**
 * 评论上下文响应
 */
export interface CommentContextResponse {
  items?: Comment[]
  subItems?: Comment[]
  targetCommentId: number
  parentCommentId?: number
  hasMoreBefore: boolean
  hasMoreAfter: boolean
  firstScore?: number
  firstId?: number
  lastScore?: number
  lastId?: number
}

/**
 * 评论管理相关 API
 * 参考：web-ts/src/services/api/v1/apiServiceV1.ts (commentServiceV1)
 */
export const commentApi = {
  /**
   * 创建评论
   */
  createComment(
    objectId: number,
    objectType: ObjectType,
    replyTo?: number | null,
    toUser?: number | null,
    content?: string
  ): Promise<ApiResponse<Comment>> {
    const body: Record<string, unknown> = {
      objectId,
      objectType,
      content,
    }
    if (replyTo !== null && replyTo !== undefined) {
      body.replyTo = replyTo
    }
    if (toUser !== null && toUser !== undefined) {
      body.toUser = toUser
    }
    return apiClient.post('/v1/comments', body)
  },

  /**
   * 获取评论列表
   * @param objectId 对象ID
   * @param objectType 对象类型
   * @param lastScore 上一页最后一条记录的分数，首页不传
   * @param lastId 上一页最后一条记录的ID，首页不传
   */
  getComments(
    objectId: number,
    objectType: ObjectType,
    lastScore?: number,
    lastId?: number
  ): Promise<ApiResponse<KeysetPageResponse<Comment>>> {
    const params: Record<string, unknown> = { objectId, objectType }
    if (lastScore !== undefined && lastId !== undefined) {
      params.lastScore = lastScore
      params.lastId = lastId
    }
    return apiClient.get('/v1/comments', { params })
  },

  /**
   * 获取评论的回复列表
   * @param id 评论ID
   * @param lastScore 上一页最后一条记录的分数，首页不传
   * @param lastId 上一页最后一条记录的ID，首页不传
   */
  getCommentReplies(
    id: number,
    lastScore?: number,
    lastId?: number
  ): Promise<ApiResponse<KeysetPageResponse<Comment>>> {
    const params: Record<string, unknown> = {}
    if (lastScore !== undefined && lastId !== undefined) {
      params.lastScore = lastScore
      params.lastId = lastId
    }
    return apiClient.get(`/v1/comments/${String(id)}/replies`, { params })
  },

  /**
   * 获取评论上下文
   * 根据评论ID获取该评论及其前后评论，用于从外部链接跳转到特定评论
   * @param id 评论ID
   */
  getCommentContext(id: number): Promise<ApiResponse<CommentContextResponse>> {
    return apiClient.get(`/v1/comments/${String(id)}/context`)
  },
}
