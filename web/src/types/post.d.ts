import type { Node } from './node'
import type { User } from './user'
import type { VoteType, PostType, ContentState } from '@/enums'

/**
 * 帖子相关的类型定义
 * 参考：web-ts/src/types/post.ts
 */

/**
 * 帖子信息接口（合并所有版本）
 */
export interface Post {
  id: number
  content: string
  nodeId?: number // 节点ID
  node?: Node // 节点信息
  creatorId?: number // 创建者ID
  creator?: User // 创建者信息
  type?: PostType // 帖子类型
  twiceCount?: number // "两遍就懂"票数
  likeCount?: number // "喜欢"票数
  commentCount?: number // 评论数量
  viewCount?: number // 浏览量
  state?: ContentState // 帖子状态
  score?: number // 帖子得分
  createdAt?: string // 创建时间
  updatedAt?: string // 更新时间
  voteType?: VoteType | null // 当前用户投票状态
  deckCount?: number // 记忆卡片组数量
}

/**
 * 创建帖子请求
 */
export interface CreatePostRequest {
  content: string
  nodeId: number
  type: PostType // 1=内容, 2=文章
}

/**
 * 更新帖子请求
 */
export interface UpdatePostRequest {
  id: number // 帖子ID
  content?: string
}

/**
 * 获取帖子列表的查询参数
 */
export interface GetPostsQuery {
  ids?: string // 逗号分隔的帖子ID字符串
  nodeId?: number // 节点ID
  lastScore?: number // 分页参数：最后一个帖子的分数
  lastId?: number // 分页参数：最后一个帖子ID
}
