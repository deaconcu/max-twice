/**
 * 用户路线图相关的类型定义
 * 基于后端 UserRoadmapDTO.java
 */

import type { Roadmap } from './roadmap'
import type { User } from './user'
import { UserProgressState } from './enums'
import type { Node, Edge } from '@vue-flow/core'

// 用户路线图信息
export interface UserRoadmap {
  id: number
  userId?: number              // 用户ID (可选)
  roadmap?: Roadmap            // 路线图信息 (可选)
  progressPercent?: number     // 进度百分比 (可选)
  state?: UserProgressState    // 状态：NOT_STARTED(0), IN_PROGRESS(1), COMPLETED(2) (可选)
  startedAt?: string           // 开始时间 (可选)
  completedAt?: string         // 完成时间 (可选)
  createdAt?: string           // 创建时间 (可选)
  updatedAt?: string           // 更新时间 (可选)
}


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
  [key: string]: any
}