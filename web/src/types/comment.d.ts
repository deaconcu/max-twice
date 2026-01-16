import type { ObjectType, ContentState } from '@/enums'

/**
 * 评论相关的类型定义
 * 参考：web-ts/src/types/comment.ts
 */

/**
 * 评论信息接口
 */
export interface Comment {
  id: number
  content: string
  createdAt: string // 创建时间（必需）
  likeCount: number // 点赞数（必需）
  liked: boolean // 点赞状态（必需）
  type?: ObjectType // 评论类型
  objectId?: number // 被评论对象ID
  replyCount?: number // 回复数量
  replyToCommentId?: number // 回复的评论ID
  creatorId?: number // 评论者ID
  creatorName?: string // 评论者用户名
  toUserId?: number // 被回复者ID
  toUserName?: string // 被回复者用户名
  state?: ContentState // 评论状态
  children?: Comment[] | null // 子评论列表
}

/**
 * 创建评论请求
 */
export interface CreateCommentRequest {
  objectId: number
  objectType: ObjectType
  replyTo?: number
  toUser?: number
  content: string
}

/**
 * 获取评论请求参数（根据对象获取评论）
 */
export interface GetCommentsQuery {
  objectId: number
  objectType: ObjectType
  offsetId?: number
}

/**
 * 获取评论回复请求参数（根据评论ID获取回复）
 */
export interface GetCommentRepliesQuery {
  commentId: number
  offsetId?: number
}
