/**
 * 节点相关的类型定义
 * 统一合并所有后端 DTO 版本的字段
 */

import type { Course } from './course'

// 统一的节点信息 (合并所有版本)
export interface Node {
  id: number
  name: string
  description?: string      // 节点描述 (可选)
  courseId?: number         // 所属课程ID (可选)
  course?: Course           // 所属课程信息 (可选)
  children?: Node[]         // 子节点列表 (可选)
  creatorId?: number          // 创建者ID (可选)
  commentCount?: number     // 评论数量 (可选)
  state?: number            // 节点状态: 0=待审核, 1=正常, 2=已屏蔽 (可选)
  createdAt?: string        // 创建时间 (可选)
  updatedAt?: string        // 更新时间 (可选)

  // NodeDTOV2 独有字段
  isCompleted?: boolean     // 是否已完成 (可选)
}