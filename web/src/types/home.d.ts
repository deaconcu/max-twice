import type { PlatformStats } from './stats'
import type { Profession } from './profession'
import type { Course } from './course'
import type { ReviewSummary } from './memory'

/**
 * 首页相关类型定义
 */

/**
 * 用户学习统计
 */
export interface UserLearningStats {
  learningDays: number // 累计学习天数
  coursesInProgress: number // 正在学习的课程数
  careersInProgress: number // 正在学习的职业数
}

/**
 * 用户学习记录（带进度）
 * 泛型 T 表示具体的内容类型（课程或路线图）
 */
export interface UserLearning<T = unknown> {
  id: number
  userId: number
  objectId: number
  objectType: string
  progressPercent: number // 进度（万分位：0-10000）
  state: number // 状态：NOT_STARTED(0), IN_PROGRESS(1), COMPLETED(2)
  startedAt?: string
  completedAt?: string
  createdAt?: string
  updatedAt?: string
  object?: T // 关联的内容对象
}

/**
 * 课程带统计信息
 */
export interface CourseWithStats extends Course {
  learnerCount?: number
  subscriptionCount?: number
}

/**
 * 首页聚合数据
 */
export interface HomePage {
  platformStats: PlatformStats // 平台统计
  userStats: UserLearningStats // 用户学习统计
  learningCareers: UserLearning[] // 正在学习的职业路线
  learningCourses: UserLearning[] // 正在学习的课程
  reviewSummary: ReviewSummary // 复习概览
  recommendedCareers: Profession[] // 推荐职业
  recommendedCourses: CourseWithStats[] // 推荐课程
}
