/**
 * 帖子相关的类型定义
 * 统一合并所有后端 DTO 版本的字段
 */

import type { Node } from './node'
import type { User } from './user'
import { VoteType, PostType, ContentState } from './enums'

// 统一的帖子信息 (合并所有版本)
export interface Post {
  id: number
  content: string
  nodeId?: number              // 节点ID (可选)
  node?: Node                  // 节点信息 (可选)
  creatorId?: number           // 创建者ID (可选)
  creator?: User               // 创建者信息 (可选)
  type?: PostType              // 帖子类型 (可选)
  twice?: number               // "两遍就懂"票数 (可选)
  helpful?: number             // "有帮助"票数 (可选)
  commentCount?: number        // 评论数量 (可选)
  viewCount?: number           // 浏览量 (可选)
  state?: ContentState         // 帖子状态 (可选)
  score?: number               // 帖子得分 (可选)
  createdAt?: string           // 创建时间 (可选)
  updatedAt?: string           // 更新时间 (可选)
  voteType?: VoteType | null   // 当前用户投票状态 (可选)
  deckCount?: number           // 记忆卡片组数量 (可选)
}

// 创建帖子请求
export interface CreatePostRequest {
  content: string
  nodeId: number
  type: PostType              // 1=内容, 2=文章
}

// 更新帖子请求
export interface UpdatePostRequest {
  id: number                  // 帖子ID
  content?: string
}

// 获取帖子列表的查询参数
export interface GetPostsQuery {
  ids?: string               // 逗号分隔的帖子ID字符串
  nodeId?: number           // 节点ID
  lastScore?: number        // 分页参数：最后一个帖子的分数
  lastId?: number           // 分页参数：最后一个帖子ID
}