import apiClient from '../client'
import type { UpvoteStatusResponse } from '@/types/upvote'
import type { ObjectType, VoteType } from '@/enums'

/**
 * 点赞相关 API
 */
export const upvoteApi = {
  /**
   * 点赞/取消点赞
   */
  upvote(objectId: number, objectType: ObjectType, type: VoteType): Promise<UpvoteStatusResponse> {
    return apiClient.post('/upvotes', {
      objectId,
      objectType,
      type,
    })
  },

  /**
   * 获取点赞状态
   */
  getUpvoteStatus(objectId: number, objectType: ObjectType): Promise<UpvoteStatusResponse> {
    return apiClient.get('/upvotes/status', {
      params: { objectId, objectType },
    })
  },
}
