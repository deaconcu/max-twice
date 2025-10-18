/**
 * 路线图相关的类型定义
 * 基于后端 RoadmapDTO.java, RoadmapDTOV2.java
 */

import type { User } from './user'
import type { Profession } from './profession'
import type { Node, Edge } from '@vue-flow/core'

// 统一的路线图信息
export interface Roadmap {
  id: number                  // Long -> number
  content?: string            // 路线图内容 (可选)
  professionId?: number       // 职业ID (可选) - Long -> number
  profession?: Profession     // 职业信息 (可选)
  description?: string        // 描述 (可选)
  state?: number              // 状态：0-待审核，1-已批准，2-已拒绝 (可选)
  vote?: number              // 投票数 (可选) - Integer -> number
  comment?: number           // 评论数 (可选) - Integer -> number
  upvoted?: boolean          // 是否已点赞 (可选)
  pinned?: boolean           // 是否置顶 (可选)
  learning?: boolean         // 是否正在学习 (可选)
  creator?: User             // 创建者信息 (可选) - UserDTOV4 -> User
  updatedAt?: string         // 更新时间 (可选) - LocalDateTime -> string
  createdAt?: string         // 创建时间 (可选) - LocalDateTime -> string

  // todo: 前端独有字段
  learners?: number          // 学习者数量 (可选)
  nodes?: Node[]             // Vue Flow 节点 (可选)
  edges?: Edge[]             // Vue Flow 边 (可选)
}
