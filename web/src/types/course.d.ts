import type { ContentState } from '@/enums'
import type { User } from './user'

/**
 * 课程相关的类型定义
 * 参考：web-ts/src/types/course.ts
 */

/**
 * 课程信息接口（合并所有版本）
 */
export interface Course {
  id: number
  name: string
  description?: string // 课程描述
  creator?: User // 创建者
  rootNodeId?: number // 根节点ID
  parentCourseId?: number // 父课程ID（子课程才有）
  parentCourse?: Course // 父课程信息
  state?: ContentState // 课程状态
  mainCategory?: number // 主分类ID
  subCategory?: number // 子分类ID
  reason?: string // 拒绝原因
  createdAt?: string // 创建时间
  updatedAt?: string // 更新时间

  // 统计字段
  learnerCount?: number // 学习人数
  subscriptionCount?: number // 收藏人数
  subscribed?: boolean // 是否已收藏
  progress?: number // 课程进度百分比 0-100
}

/**
 * 创建课程请求
 */
export interface CreateCourseRequest {
  name: string
  description: string
  mainCategory: number
  subCategory: number
}

/**
 * 更新课程请求
 */
export interface UpdateCourseRequest {
  name?: string
  description?: string
  mainCategory?: number
  subCategory?: number
}

/**
 * 创建子课程请求
 */
export interface CreateSubcourseRequest {
  parentId: number // 父课程ID
  name: string
  description: string
}

/**
 * 课程审核请求
 */
export interface ApproveCourseRequest {
  id: number // 课程ID
  action: string // 审核动作
  reason?: string // 拒绝原因
}

/**
 * 课程子分类
 */
export interface SubCourseCategory {
  id: number
  name: string
  list?: Course[] // 课程列表
}

/**
 * 课程分类
 */
export interface CourseCategory {
  id?: number
  name: string
  list: SubCourseCategory[]
}

/**
 * 课程完成响应
 * 用于完成课程后的响应
 */
export interface CourseCompletionResponse {
  courseId: number
  completed: boolean
  message: string
}

/**
 * 审批响应
 * 用于课程审核操作的响应
 * @deprecated 已废弃，所有审核接口已改为返回 ApiResponse<void>
 */
export interface ApprovalResponse {
  success: boolean
  message: string
  objectId: number
  objectType: 'course' | 'profession'
  action: 'approve' | 'reject' | 'delete'
}
