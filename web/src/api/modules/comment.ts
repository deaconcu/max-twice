import apiClient from '../client'
import type { ApiResponse } from '@/types/api'
import type { Comment } from '@/types/comment'
import type { ObjectType } from '@/enums'

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
   */
  getComments(
    objectId: number,
    objectType: ObjectType,
    offsetId = 0
  ): Promise<ApiResponse<Comment[]>> {
    return apiClient.get('/v1/comments', {
      params: { objectId, objectType, offsetId },
    })
  },

  /**
   * 获取评论的回复列表
   */
  getCommentReplies(id: number, offsetId = 0): Promise<ApiResponse<Comment[]>> {
    return apiClient.get(`/v1/comments/${String(id)}/replies`, {
      params: { offsetId },
    })
  },
}
