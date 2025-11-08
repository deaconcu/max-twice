import apiClient from '../client'
import type { ApiResponse } from '@/types/api'
import type { Node, NodeProgressResponse } from '@/types/node'
import type { UserCourse } from '@/types/userCourse'
import type { UserRoadmap } from '@/types/userRoadmap'
import type { CourseCompletionResponse } from '@/types/course'

/**
 * 学习进度相关 API
 * 参考：web-ts/src/services/api/v1/apiServiceV1.ts (progressServiceV1)
 */
export const progressApi = {
  /**
   * 标记节点为完成
   */
  markNodeComplete(nodeId: number, courseId: number): Promise<ApiResponse<NodeProgressResponse>> {
    return apiClient.post(`/v1/progress/nodes/${String(nodeId)}/complete`, { courseId })
  },

  /**
   * 取消节点完成标记
   */
  unmarkNodeComplete(nodeId: number, courseId: number): Promise<ApiResponse<NodeProgressResponse>> {
    return apiClient.delete(`/v1/progress/nodes/${String(nodeId)}/complete`, {
      data: { courseId },
    })
  },

  /**
   * 获取节点状态
   */
  getNodeStatus(nodeId: number): Promise<ApiResponse<Node>> {
    return apiClient.get(`/v1/progress/nodes/${String(nodeId)}/status`)
  },

  /**
   * 开始学习课程
   */
  startCourse(courseId: number): Promise<ApiResponse<boolean>> {
    return apiClient.post(`/v1/progress/courses/${String(courseId)}/start`, { courseId })
  },

  /**
   * 获取课程进度
   */
  getCourseProgress(courseId: number): Promise<ApiResponse<UserCourse>> {
    return apiClient.get(`/v1/progress/courses/${String(courseId)}`)
  },

  /**
   * 获取所有课程进度
   */
  getAllCourseProgress(lastId?: number): Promise<ApiResponse<UserCourse[]>> {
    return apiClient.get('/v1/progress/courses', {
      params: { lastId },
    })
  },

  /**
   * 更新课程进度
   */
  updateCourseProgress(
    courseId: number,
    data: Partial<UserCourse>
  ): Promise<ApiResponse<UserCourse>> {
    return apiClient.put(`/v1/progress/courses/${String(courseId)}`, data)
  },

  /**
   * 删除课程进度
   */
  deleteCourseProgress(courseId: number): Promise<ApiResponse<string>> {
    return apiClient.delete(`/v1/progress/courses/${String(courseId)}`)
  },

  /**
   * 完成课程
   */
  completeCourse(courseId: number): Promise<ApiResponse<CourseCompletionResponse>> {
    return apiClient.post(`/v1/progress/courses/${String(courseId)}/complete`)
  },

  /**
   * 开始学习路线图
   */
  startRoadmap(roadmapId: number): Promise<ApiResponse<boolean>> {
    return apiClient.post(`/v1/progress/roadmaps/${String(roadmapId)}/start`)
  },

  /**
   * 获取路线图进度
   */
  getRoadmapProgress(roadmapId: number): Promise<ApiResponse<UserRoadmap>> {
    return apiClient.get(`/v1/progress/roadmaps/${String(roadmapId)}`)
  },

  /**
   * 获取用户的路线图列表
   */
  getUserRoadmaps(): Promise<ApiResponse<UserRoadmap[]>> {
    return apiClient.get('/v1/progress/roadmaps')
  },

  /**
   * 更新路线图进度
   */
  updateRoadmapProgress(
    roadmapId: number,
    progressPercent: number
  ): Promise<ApiResponse<UserRoadmap>> {
    return apiClient.put(`/v1/progress/roadmaps/${String(roadmapId)}`, { progressPercent })
  },

  /**
   * 删除路线图进度
   */
  deleteRoadmapProgress(roadmapId: number): Promise<ApiResponse<string>> {
    return apiClient.delete(`/v1/progress/roadmaps/${String(roadmapId)}`)
  },
}
