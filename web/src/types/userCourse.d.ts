import type { Course } from './course'
import type { UserProgressState } from '@/enums'

/**
 * 用户课程相关的类型定义
 * 参考：web-ts/src/types/userCourse.ts
 */

/**
 * 用户课程信息接口
 */
export interface UserCourse {
  id: number
  userId?: number // 用户ID
  course?: Course // 课程信息
  progressPercent?: number // 进度百分比
  state?: UserProgressState // 状态：NOT_STARTED(0), IN_PROGRESS(1), COMPLETED(2)
  startedAt?: string // 开始时间
  completedAt?: string // 完成时间
  createdAt?: string // 创建时间
  updatedAt?: string // 更新时间
}
