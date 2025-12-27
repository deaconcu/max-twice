import apiClient from '../client'
import type { ApiResponse, KeysetPageResponse } from '@/types/api'
import type { Course, ApprovalResponse } from '@/types/course'
import type { UserCourse } from '@/types/userCourse'
import type { SubscriptionInfo } from '@/types/user'

/**
 * 课程管理相关 API
 * 参考：web-ts/src/services/api/v1/apiServiceV1.ts (courseServiceV1)
 */
export const courseApi = {
  /**
   * 获取课程详情
   */
  getCourse(id: number): Promise<ApiResponse<Course>> {
    return apiClient.get(`/v1/courses/${String(id)}`)
  },

  /**
   * 搜索课程
   */
  searchCourses(name: string): Promise<ApiResponse<Course[]>> {
    return apiClient.get('/v1/courses/search', {
      params: { name },
    })
  },

  /**
   * 根据状态获取课程列表（已废弃 - 普通用户不应查询任意状态）
   * @deprecated 请使用 getCoursesByCategory() 获取已发布课程
   */
  getCoursesByState(state: number, lastId?: number): Promise<ApiResponse<KeysetPageResponse<Course>>> {
    return apiClient.get('/v1/courses', {
      params: { state, lastId },
    })
  },

  /**
   * 根据分类获取课程列表
   * @param mainCategory 主分类ID（可选）
   * @param subCategory 子分类ID（可选）
   * @param lastId 分页参数（可选）
   * 不传任何参数时，返回所有已发布课程
   */
  getCoursesByCategory(
    mainCategory?: number,
    subCategory?: number,
    lastId?: number
  ): Promise<ApiResponse<KeysetPageResponse<Course>>> {
    return apiClient.get('/v1/courses', {
      params: { mainCategory, subCategory, lastId },
    })
  },

  /**
   * 获取热门课程
   */
  getHotCourses(): Promise<ApiResponse<Course[]>> {
    return apiClient.get('/v1/courses/hot')
  },

  /**
   * 获取子课程列表
   * 修正：使用 parentId 查询参数而不是路径参数
   */
  getSubCourses(parentId: number): Promise<ApiResponse<KeysetPageResponse<Course>>> {
    return apiClient.get('/v1/courses', {
      params: { parentId },
    })
  },

  /**
   * 创建课程
   */
  createCourse(courseData: Partial<Course>): Promise<ApiResponse<Course>> {
    return apiClient.post('/v1/courses', courseData)
  },

  /**
   * 更新课程
   */
  updateCourse(id: number, courseData: Partial<Course>): Promise<ApiResponse<Course>> {
    return apiClient.put(`/v1/courses/${String(id)}`, courseData)
  },

  /**
   * 创建子课程
   */
  createSubcourse(
    parentId: number,
    name: string,
    description: string
  ): Promise<ApiResponse<Course>> {
    return apiClient.post(`/v1/courses/${String(parentId)}/subcourses`, {
      name,
      description,
    })
  },

  /**
   * 审核课程
   */
  approveCourse(
    id: number,
    action: string,
    reason?: string
  ): Promise<ApiResponse<void>> {
    return apiClient.post(`/v1/courses/${String(id)}/approve`, {
      action,
      reason,
    })
  },

  /**
   * 更新用户课程目录
   */
  updateUserCourseToc(courseId: number, indexArray: string): Promise<ApiResponse<string>> {
    return apiClient.put(`/v1/users/current/courses/${String(courseId)}/toc`, {
      indexArray,
    })
  },
}

/**
 * 订阅相关 API
 * 参考：web-ts/src/services/api/v1/apiServiceV1.ts (subscriptionServiceV1)
 */
export const subscriptionApi = {
  /**
   * 获取用户订阅列表
   */
  getUserSubscriptions(userId: number): Promise<ApiResponse<UserCourse[]>> {
    return apiClient.get(`/v1/users/${String(userId)}/subscriptions`)
  },

  /**
   * 订阅课程
   */
  subscribe(courseId: number): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/users/current/subscriptions', {
      courseId,
    })
  },

  /**
   * 更新订阅
   * @param subscriptionIds 订阅的课程 ID，逗号分隔（例如："1,2,3"）
   */
  updateSubscriptions(subscriptionIds: string): Promise<ApiResponse<SubscriptionInfo[]>> {
    return apiClient.put('/v1/users/current/subscriptions', {
      subscription: subscriptionIds,
    })
  },

  /**
   * 取消订阅课程
   */
  unsubscribe(courseId: number): Promise<ApiResponse<void>> {
    return apiClient.delete(`/v1/users/current/subscriptions/${String(courseId)}`)
  },
}
