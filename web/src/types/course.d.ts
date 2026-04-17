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
  icon?: string // 课程图标（MDI图标名或图片URL）
  creator?: User // 创建者
  rootNodeId?: number // 根节点ID
  parentCourse?: Course // 父课程信息
  state?: ContentState // 课程状态
  mainCategory?: number // 主分类ID
  subCategory?: number // 子分类ID
  reason?: string // 拒绝原因
  createdAt?: string // 创建时间
  updatedAt?: string // 更新时间

  // 统计字段
  learnerCount?: number // 学习人数
  bookmarkCount?: number // 收藏人数
  bookmarked?: boolean // 是否已收藏
  progress?: number // 课程进度（万分位：0-10000）
  subCourseCount?: number // 子课程数量
}

/**
 * 创建课程请求
 */
export interface CreateCourseRequest {
  name: string
  description: string
  mainCategory: number | null
  subCategory: number | null
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
 * 课程进度响应
 * 用于开始/取消学习课程的响应
 */
export interface CourseProgressResponse {
  courseId: number
  learning: boolean
}

/**
 * 路线图进度响应
 * 用于开始/取消学习路线图的响应
 */
export interface RoadmapProgressResponse {
  roadmapId: number
  learning: boolean
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
  objectType: 'course' | 'role'
  action: 'approve' | 'reject' | 'delete'
}
