import type { Course } from './course'

/**
 * 节点相关的类型定义
 * 参考：web-ts/src/types/node.ts
 */

/**
 * 节点信息接口（合并所有版本）
 */
export interface Node {
  id: number
  name: string
  description?: string // 节点描述
  courseId?: number // 所属课程ID
  course?: Course // 所属课程信息
  children?: Node[] // 子节点列表
  creatorId?: number // 创建者ID
  commentCount?: number // 评论数量
  state?: number // 节点状态: 0=待审核, 1=正常, 2=已屏蔽
  createdAt?: string // 创建时间
  updatedAt?: string // 更新时间

  // 学习进度相关
  isCompleted?: boolean // 是否已完成
}

/**
 * 节点进度响应
 * 用于标记节点完成/取消完成的响应
 */
export interface NodeProgressResponse {
  nodeId: number
  completed: boolean
}
