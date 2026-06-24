import apiClient from '../client'
import type { Comment } from '@/types/comment'
import type { ObjectType } from '@/enums'
import type { CursorPage } from '@/types/api'

/**
 * 评论基本信息响应
 */
export interface CommentBasicResponse {
  id: number
  objectType: number
  objectId: number
  replyToCommentId?: number
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
  firstCursor?: string
  lastCursor?: string
}

/**
 * 评论管理相关 API
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
  ): Promise<Comment> {
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
    return apiClient.post('/comments', body)
  },

  /**
   * 获取评论列表
   * @param objectId 对象ID
   * @param objectType 对象类型
   * @param cursor 分页游标
   */
  getComments(
    objectId: number,
    objectType: ObjectType,
    cursor?: string
  ): Promise<CursorPage<Comment>> {
    const params: Record<string, unknown> = { objectId, objectType }
    if (cursor !== undefined) params.cursor = cursor
    return apiClient.get('/comments', { params })
  },

  /**
   * 获取评论的回复列表
   * @param id 评论ID
   * @param cursor 分页游标
   */
  getCommentReplies(id: number, cursor?: string): Promise<CursorPage<Comment>> {
    const params: Record<string, unknown> = {}
    if (cursor !== undefined) params.cursor = cursor
    return apiClient.get(`/comments/${String(id)}/replies`, { params })
  },

  /**
   * 获取评论上下文
   * 根据评论ID获取该评论及其前后评论，用于从外部链接跳转到特定评论
   * @param id 评论ID
   */
  getCommentContext(id: number): Promise<CommentContextResponse> {
    return apiClient.get(`/comments/${String(id)}/context`)
  },

  /**
   * 获取评论基本信息
   * @param id 评论ID
   */
  getCommentBasic(id: number): Promise<CommentBasicResponse> {
    return apiClient.get(`/comments/${String(id)}`)
  },
}
