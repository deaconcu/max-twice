import type { ObjectType, VoteType } from '@/enums'

/**
 * 点赞相关的类型定义
 * 参考：web-ts/src/types/response.ts (UpvoteStatusResponse)
 */

/**
 * 点赞状态响应
 */
export interface UpvoteStatusResponse {
  objectId: number
  objectType: number
  upvotes: number
  upvoted: boolean
  twiceUpvotes?: number
  twiceUpvoted?: boolean
  helpfulUpvotes?: number
  helpfulUpvoted?: boolean
}

/**
 * 点赞请求
 */
export interface UpvoteRequest {
  objectId: number
  objectType: ObjectType
  type: VoteType
}
