/**
 * 评论相关的类型定义
 * 基于后端 CommentDTO.java, CommentDTOV1.java
 */

import { ObjectType, ContentState } from './enums'

// 统一的评论信息
export interface Comment {
  id: number
  content: string
  createdAt: string               // 创建时间 (必需，用于显示)
  upvoteCount: number             // 点赞数 (必需，用于显示)
  upvoted: boolean                // 点赞状态 (必需，用于显示)
  type?: ObjectType               // 评论类型 (可选)
  objectId?: number              // 被评论对象ID (可选)
  replyCount?: number            // 回复数量 (可选)
  replyToCommentId?: number      // 回复的评论ID (可选)
  creatorId?: number             // 评论者ID (可选)
  creatorName?: string           // 评论者用户名 (可选) - TODO: 后端需要添加此字段
  toUserId?: number              // 被回复者ID (可选)
  toUserName?: string            // 被回复者用户名 (可选)
  state?: ContentState           // 评论状态 (可选)
  children?: Comment[] | null    // 子评论列表 (可选，可以为null)
}

// 创建评论请求
export interface CreateCommentRequest {
  objectId: number
  type: ObjectType
  replyTo?: number
  toUser?: number
  content: string
}

// 获取评论请求参数 (第一种：根据对象获取评论)
export interface GetCommentsQuery {
  objectId: number
  type: ObjectType
  offsetId?: number
}

// 获取评论回复请求参数 (第二种：根据评论ID获取回复)
export interface GetCommentRepliesQuery {
  commentId: number
  offsetId?: number
}