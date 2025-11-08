import type { Roadmap } from './roadmap'
import type { User } from './user'
import type { UserProgressState } from '@/enums'
import type { Node, Edge } from '@vue-flow/core'

/**
 * 用户路线图相关的类型定义
 * 参考：web-ts/src/types/userRoadmap.ts
 */

/**
 * 用户路线图信息接口
 */
export interface UserRoadmap {
  id: number
  userId?: number // 用户ID
  roadmap?: Roadmap // 路线图信息
  progressPercent?: number // 进度百分比
  state?: UserProgressState // 状态：NOT_STARTED(0), IN_PROGRESS(1), COMPLETED(2)
  startedAt?: string // 开始时间
  completedAt?: string // 完成时间
  createdAt?: string // 创建时间
  updatedAt?: string // 更新时间
}

/**
 * 处理后的用户路线图信息（用于前端展示）
 */
export interface ProcessedUserRoadmap {
  id: number
  title: string
  description?: string
  creator?: User
  createdAt: string
  addedDate: string
  vote: number
  upvoted: boolean
  progress: number
  completedNodes: number
  totalNodes: number
  lastActivity: string
  state: UserProgressState
  startedAt?: string
  completedAt?: string
  tags: string[]
  profession?: { name: string }
  nodes: Node[]
  edges: Edge[]
  content: string
  [key: string]: unknown // 索引签名
}
