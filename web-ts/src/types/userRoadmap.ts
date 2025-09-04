/**
 * 用户路线图相关的类型定义
 * 基于后端 UserRoadmapDTO.java
 */

import type { Roadmap } from './roadmap'
import { UserRoadmapState } from './enums'

// 用户路线图信息
export interface UserRoadmap {
  id: number
  userId?: number              // 用户ID (可选)
  roadmap?: Roadmap            // 路线图信息 (可选)
  progressPercent?: number     // 进度百分比 (可选)
  state?: UserRoadmapState     // 状态：NOT_STARTED(0), IN_PROGRESS(1), COMPLETED(2) (可选)
  startedAt?: string           // 开始时间 (可选)
  completedAt?: string         // 完成时间 (可选)
  createdAt?: string           // 创建时间 (可选)
  updatedAt?: string           // 更新时间 (可选)
}