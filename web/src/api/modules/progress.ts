import apiClient from '../client'
import type { NodeProgressResponse } from '@/types/node'
import type { Roadmap } from '@/types/roadmap'
import type { UserCourse } from '@/types/userCourse'
import type { UserRoadmap } from '@/types/userRoadmap'
import type { UserLearning } from '@/types/home'
import type {
  CourseCompletionResponse,
  CourseProgressResponse,
  RoadmapProgressResponse,
} from '@/types/course'

/**
 * 学习进度相关 API
 */
export const progressApi = {
  /**
   * 标记节点为完成
   */
  markNodeComplete(nodeId: number, rootNodeId: number): Promise<NodeProgressResponse> {
    return apiClient.post(`/progress/nodes/${String(nodeId)}/complete`, { rootNodeId })
  },

  /**
   * 取消节点完成标记
   */
  unmarkNodeComplete(nodeId: number, rootNodeId: number): Promise<NodeProgressResponse> {
    return apiClient.delete(`/progress/nodes/${String(nodeId)}/complete`, {
      data: { rootNodeId },
    })
  },

  /**
   * 获取节点状态
   */
  getNodeStatus(nodeId: number): Promise<NodeProgressResponse> {
    return apiClient.get(`/progress/nodes/${String(nodeId)}/status`)
  },

  /**
   * 注册学习课程
   */
  startCourse(courseId: number): Promise<CourseProgressResponse> {
    return apiClient.post(`/progress/courses/${String(courseId)}/enrollment`)
  },

  /**
   * 取消注册学习课程
   */
  cancelCourse(courseId: number): Promise<CourseProgressResponse> {
    return apiClient.delete(`/progress/courses/${String(courseId)}/enrollment`)
  },

  /**
   * 获取课程进度
   */
  getCourseProgress(courseId: number): Promise<UserCourse> {
    return apiClient.get(`/progress/courses/${String(courseId)}`)
  },

  /**
   * 获取所有课程进度
   * @param state 状态过滤（'learning'=进行中, 'completed'=已完成，不传=全部）
   * @param cursor 分页游标
   */
  getAllCourseProgress(state?: 'learning' | 'completed', cursor?: string): Promise<UserLearning[]> {
    return apiClient.get('/progress/courses', {
      params: { state, cursor },
    })
  },

  /**
   * 更新课程进度
   */
  updateCourseProgress(courseId: number, data: Partial<UserCourse>): Promise<UserCourse> {
    return apiClient.put(`/progress/courses/${String(courseId)}`, data)
  },

  /**
   * 删除课程进度
   */
  deleteCourseProgress(courseId: number): Promise<CourseProgressResponse> {
    return apiClient.delete(`/progress/courses/${String(courseId)}`)
  },

  /**
   * 完成课程
   */
  completeCourse(courseId: number): Promise<CourseCompletionResponse> {
    return apiClient.post(`/progress/courses/${String(courseId)}/complete`)
  },

  /**
   * 注册学习路线图
   */
  startRoadmap(roadmapId: number): Promise<RoadmapProgressResponse> {
    return apiClient.post(`/progress/roadmaps/${String(roadmapId)}/enrollment`)
  },

  /**
   * 取消注册学习路线图
   */
  cancelRoadmap(roadmapId: number): Promise<RoadmapProgressResponse> {
    return apiClient.delete(`/progress/roadmaps/${String(roadmapId)}/enrollment`)
  },

  /**
   * 获取路线图进度
   */
  getRoadmapProgress(roadmapId: number): Promise<UserRoadmap> {
    return apiClient.get(`/progress/roadmaps/${String(roadmapId)}`)
  },

  /**
   * 获取用户的路线图列表
   * @param state 状态过滤（'learning'=进行中, 'completed'=已完成，不传=全部）
   * @param cursor 分页游标
   */
  getUserRoadmaps(state?: 'learning' | 'completed', cursor?: string): Promise<UserLearning[]> {
    return apiClient.get('/progress/roadmaps', {
      params: { state, cursor },
    })
  },

  /**
   * 更新路线图进度
   */
  updateRoadmapProgress(roadmapId: number, progressPercent: number): Promise<UserRoadmap> {
    return apiClient.put(`/progress/roadmaps/${String(roadmapId)}`, { progressPercent })
  },

  /**
   * 获取用户正在学习的角色路线图（最多20条）
   */
  getLearningRoadmapsByRole(roleId: number): Promise<Roadmap[]> {
    return apiClient.get(`/progress/roles/${String(roleId)}/roadmaps/learning`)
  },
}
