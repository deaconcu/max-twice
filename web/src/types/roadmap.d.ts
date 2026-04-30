import type { User } from './user'
import type { Role } from './role'
import type { Node, Edge } from '@vue-flow/core'

/**
 * 路线图相关的类型定义
 */

/**
 * 路线图主体状态（revision 模型）
 *
 * - NEVER_PUBLISHED：从未发布过（仅草稿，或刚创建未审核）
 * - PUBLISHED：当前对外可见（current_revision_id 指向有效 revision）
 * - BANNED：被管理员封禁（仅 admin 与作者本人可见）
 */
export type RoadmapState = 'NEVER_PUBLISHED' | 'PUBLISHED' | 'BANNED'

/**
 * 路线图信息接口
 */
export interface Roadmap {
  id: number
  content?: string // 路线图内容
  roleId?: number // 角色ID
  role?: Role // 角色信息
  description?: string // 描述
  /** 主体状态（revision 模型）：NEVER_PUBLISHED / PUBLISHED / BANNED */
  state?: RoadmapState
  likeCount?: number // 点赞数
  commentCount?: number // 评论数量
  liked?: boolean // 是否已点赞
  pinned?: boolean // 是否置顶
  learning?: boolean // 是否正在学习
  bookmarked?: boolean // 是否已收藏
  creator?: User // 创建者信息
  updatedAt?: string // 更新时间
  createdAt?: string // 创建时间

  // 前端独有字段
  learnerCount?: number // 学习者数量
  nodeCount?: number // 节点数量
  nodes?: Node[] // Vue Flow 节点
  edges?: Edge[] // Vue Flow 边
}

/**
 * 路线图简要信息
 * 用于学习进度列表
 */
export interface RoadmapBrief {
  id: number
  roleName: string
  nodeCount: number
}
