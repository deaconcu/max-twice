import apiClient from '../client'
import type { Course, ApprovalResponse, CreateCourseRequest } from '@/types/course'
import type { UserCourse } from '@/types/userCourse'
import type { CursorPage } from '@/types/api'

/**
 * 课程管理相关 API
 */
export const courseApi = {
  /**
   * 获取课程详情
   */
  getCourse(id: number): Promise<Course> {
    return apiClient.get(`/courses/${String(id)}`)
  },

  /**
   * 搜索课程
   */
  searchCourses(name: string): Promise<Course[]> {
    return apiClient.get('/courses/search', {
      params: { name },
    })
  },

  /**
   * 根据状态获取课程列表（已废弃 - 普通用户不应查询任意状态）
   * @deprecated 请使用 getCoursesByCategory() 获取已发布课程
   */
  getCoursesByState(state: number, cursor?: string): Promise<CursorPage<Course>> {
    return apiClient.get('/courses', {
      params: { state, cursor },
    })
  },

  /**
   * 根据分类获取课程列表
   * @param mainCategory 主分类ID（可选）
   * @param subCategory 子分类ID（可选）
   * @param cursor 分页参数（可选）
   * 不传任何参数时，返回所有已发布课程
   */
  getCoursesByCategory(
    mainCategory?: number,
    subCategory?: number,
    cursor?: string
  ): Promise<CursorPage<Course>> {
    return apiClient.get('/courses', {
      params: { mainCategory, subCategory, cursor },
    })
  },

  /**
   * 获取热门课程
   */
  getHotCourses(): Promise<Course[]> {
    return apiClient.get('/courses/hot')
  },

  /**
   * 获取子课程列表
   */
  getSubCourses(parentId: number): Promise<CursorPage<Course>> {
    return apiClient.get('/courses', {
      params: { parentId },
    })
  },

  /**
   * 创建课程
   */
  createCourse(courseData: CreateCourseRequest): Promise<Course> {
    return apiClient.post('/courses', courseData)
  },

  /**
   * 更新课程
   */
  updateCourse(id: number, courseData: Partial<Course>): Promise<Course> {
    return apiClient.put(`/courses/${String(id)}`, courseData)
  },

  /**
   * 创建子课程
   */
  createSubcourse(parentId: number, name: string, description: string): Promise<Course> {
    return apiClient.post(`/courses/${String(parentId)}/subcourses`, {
      name,
      description,
    })
  },

  /**
   * 审核课程（管理员操作）
   */
  approveCourse(id: number, action: string, reason?: string): Promise<void> {
    return apiClient.post(`/admin/contents/course/${String(id)}/operate`, {
      action,
      reason,
    })
  },

  /**
   * 更新用户课程目录
   */
  updateUserCourseToc(courseId: number, indexArray: string): Promise<string> {
    return apiClient.put(`/users/current/courses/${String(courseId)}/toc`, {
      indexArray,
    })
  },
}

/**
 * 订阅相关 API
 */
export const subscriptionApi = {
  /**
   * 获取用户订阅列表
   */
  getUserSubscriptions(userId: number): Promise<UserCourse[]> {
    return apiClient.get(`/users/${String(userId)}/subscriptions`)
  },

  /**
   * 订阅课程
   * @returns 订阅后的状态 (true=已订阅)
   */
  subscribe(courseId: number): Promise<boolean> {
    return apiClient.post('/users/current/subscriptions', {
      courseId,
    })
  },

  /**
   * 取消订阅课程
   * @returns 取消订阅后的状态 (false=未订阅)
   */
  unsubscribe(courseId: number): Promise<boolean> {
    return apiClient.delete(`/users/current/subscriptions/${String(courseId)}`)
  },
}

export type { ApprovalResponse }
