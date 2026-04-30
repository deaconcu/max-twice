import apiClient from '../client'
import type {
  Course,
  ApprovalResponse,
  CreateCourseRequest,
  UpdateCourseRequest,
} from '@/types/course'
import type { UserCourse } from '@/types/userCourse'
import type { CursorPage, CreateAcceptedResponse } from '@/types/api'

/**
 * 课程重新提交 / 更新请求体（与 admin update 共用）
 */
export type CourseUpdateRequest = UpdateCourseRequest

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
   * 创建课程（申请，需审核）
   * - 主课程：parentCourseId 留空 / 0，必须传 mainCategory + subCategory
   * - 子课程：parentCourseId &gt; 0，分类继承自父课程
   */
  createCourse(courseData: CreateCourseRequest): Promise<CreateAcceptedResponse> {
    return apiClient.post('/courses', courseData)
  },

  /**
   * 更新课程（管理接口）
   */
  updateCourse(id: number, courseData: Partial<Course>): Promise<Course> {
    return apiClient.put(`/admin/contents/course/${String(id)}`, courseData)
  },

  /**
   * 创建子课程（兼容旧路径，内部委托到 POST /courses）
   */
  createSubcourse(
    parentId: number,
    name: string,
    description: string
  ): Promise<CreateAcceptedResponse> {
    return apiClient.post(`/courses/${String(parentId)}/subcourses`, {
      name,
      description,
    })
  },

  /**
   * 重新提交（被驳回 / 撤回后再申请）
   */
  resubmitCourse(id: number, data: CourseUpdateRequest): Promise<void> {
    return apiClient.post(`/courses/${String(id)}/resubmit`, data)
  },

  /**
   * 作者撤回审核中的版本
   */
  withdrawCourse(id: number): Promise<void> {
    return apiClient.post(`/courses/${String(id)}/withdraw`)
  },

  /**
   * 当前用户的课程申请列表（"我的课程"）
   * @param state 课程主体状态：NEVER_PUBLISHED | PUBLISHED（BANNED 由后端拦截）
   */
  getCurrentUserCourses(cursor?: string, state?: string): Promise<CursorPage<Course>> {
    const params: { cursor?: string; state?: string } = {}
    if (cursor !== undefined) params.cursor = cursor
    if (state !== undefined) params.state = state
    return apiClient.get('/users/me/courses', { params })
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
