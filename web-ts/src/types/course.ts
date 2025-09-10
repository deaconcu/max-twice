/**
 * 课程相关的类型定义
 * 统一合并所有后端 DTO 版本的字段
 */

import { CourseState } from './enums'
import type { User } from './user'

// 统一的课程信息 (合并所有版本)
export interface Course {
  id: number
  name: string
  description?: string          // 课程描述 (可选)
  creator?: User             // 创建者ID (可选)
  rootNodeId?: number            // 根节点ID (可选)
  parentCourseId?: number            // 父课程ID (可选，子课程才有)
  parentCourse?: Course              // 父课程信息 (可选，简化版本)
  state?: CourseState          // 课程状态 (可选)
  mainCategory?: number        // 主分类ID (可选)
  subCategory?: number         // 子分类ID (可选)
  rejectedReason?: string      // 拒绝原因 (可选)
  createdAt?: string           // 创建时间 (可选)
  updatedAt?: string           // 更新时间 (可选)
  
  // CourseDTOV4 独有的统计字段
  learnerCount?: number        // 学习人数 (可选)
  subscriptionCount?: number   // 收藏人数 (可选)
  subscribed?: boolean         // 是否已收藏 (可选)
  progress?: number            // 课程进度百分比 0-100 (可选)
}

// 创建课程请求
export interface CreateCourseRequest {
  name: string
  description: string
  mainCategory: number
  subCategory: number
}

// 更新课程请求
export interface UpdateCourseRequest {
  name?: string
  description?: string
  mainCategory?: number
  subCategory?: number
}

// 创建子课程请求
export interface CreateSubcourseRequest {
  parentId: number          // 父课程ID
  name: string
  description: string
}

// 课程审核请求
export interface ApproveCourseRequest {
  id: number                 // 课程ID
  action: string             // 审核动作
  rejectedReason?: string    // 拒绝原因
}

// 课程子分类
export interface SubCourseCategory {
  id: number
  name: string
  list?: any[]  // 可选的课程列表
}

// 课程分类
export interface CourseCategory {
  id?: number
  name: string
  list: SubCourseCategory[]
}