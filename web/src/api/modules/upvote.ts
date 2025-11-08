import apiClient from '../client'
import type { ApiResponse } from '@/types/api'
import type { UpvoteStatusResponse } from '@/types/upvote'
import type { ObjectType, VoteType } from '@/enums'

/**
 * 点赞相关 API
 * 参考：web-ts/src/services/api/v1/apiServiceV1.ts (upvoteServiceV1)
 */
export const upvoteApi = {
  /**
   * 点赞/取消点赞
   */
  upvote(
    objectId: number,
    objectType: ObjectType,
    type: VoteType
  ): Promise<ApiResponse<UpvoteStatusResponse>> {
    return apiClient.post('/v1/upvotes', {
      objectId,
      objectType,
      type,
    })
  },

  /**
   * 获取点赞状态
   */
  getUpvoteStatus(
    objectId: number,
    objectType: ObjectType
  ): Promise<ApiResponse<UpvoteStatusResponse>> {
    return apiClient.get('/v1/upvotes/status', {
      params: { objectId, objectType },
    })
  },
}
