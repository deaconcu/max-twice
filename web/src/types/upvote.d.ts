import type { ObjectType, VoteType } from '@/enums'

/**
 * 点赞相关的类型定义
 * 参考：web-ts/src/types/response.ts (UpvoteStatusResponse)
 */

/**
 * 点赞状态响应
 */
export interface UpvoteStatusResponse {
  twiced: boolean // 是否已twice点赞
  liked: boolean // 是否已like点赞
  twiceCount: number // twice点赞总数
  likeCount: number // like点赞总数
}

/**
 * 点赞请求
 */
export interface UpvoteRequest {
  objectId: number
  objectType: ObjectType
  type: VoteType
}
