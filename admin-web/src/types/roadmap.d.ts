import type { User } from './user'
import type { Role } from './role'
import type { Node, Edge } from '@vue-flow/core'

/**
 * 路线图相关的类型定义
 * 参考：web-ts/src/types/roadmap.ts
 */

/**
 * 路线图信息接口
 */
export interface Roadmap {
  id: number
  content?: string // 路线图内容
  roleId?: number
  role?: Role
  description?: string // 描述
  state?: number // 状态：0-待审核，1-已批准，2-已拒绝
  reason?: string // 拒绝/封禁原因
  creator?: User // 创建者信息
  updatedAt?: string // 更新时间
  createdAt?: string // 创建时间

  // 统计字段
  viewCount?: number // 浏览量
  likeCount?: number // 点赞数
  commentCount?: number // 评论数量
  bookmarkCount?: number // 收藏数
  learnerCount?: number // 学习人数
  completedUserCount?: number // 完成人数
  rejectCount?: number // 被拒次数
  score?: number // 排序分数

  // 用户相关字段
  liked?: boolean // 是否已点赞
  pinned?: boolean // 是否置顶
  learning?: boolean // 是否正在学习
  bookmarked?: boolean // 是否已收藏

  // 前端独有字段
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
