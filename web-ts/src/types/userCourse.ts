/**
 * 用户课程相关的类型定义
 * 基于后端 UserCourseDTO.java
 */

import type { Course } from './course'
import { UserCourseState } from './enums'

// 用户课程信息
export interface UserCourse {
  id: number
  userId?: number              // 用户ID (可选)
  course?: Course              // 课程信息 (可选)
  progressPercent?: number     // 进度百分比 (可选)
  state?: UserCourseState      // 状态：NOT_STARTED(0), IN_PROGRESS(1), COMPLETED(2) (可选)
  startedAt?: string           // 开始时间 (可选)
  completedAt?: string         // 完成时间 (可选)
  createdAt?: string           // 创建时间 (可选)
  updatedAt?: string           // 更新时间 (可选)
}