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
  isCourseRoot?: boolean // 是否为课程根节点（用于 Roadmap 区分课程和普通节点）
  createdAt?: string // 创建时间
  updatedAt?: string // 更新时间

  // 学习进度相关
  isCompleted?: boolean // 是否已完成
  progress?: number // 进度百分比 (0.0-100.0)
  canComplete?: boolean // 递归完成度100%但节点本身未完成，可以提示用户标记完成

  // 搜索/统计相关
  similarityScore?: number // 向量搜索相似度分数
  nodeReferenceCount?: number // 节点引用数量
}

/**
 * 节点进度响应
 * 用于标记节点完成/取消完成的响应
 */
export interface NodeProgressResponse {
  nodeId: number
  completed: boolean
  courseId?: number
  courseProgressPercent?: number
  completableNodeIds?: number[] // 可完成的节点ID列表
}
