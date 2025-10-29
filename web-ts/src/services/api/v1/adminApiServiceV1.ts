import axios from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types/api'
import type { User } from '@/types/user'
import type { Course } from '@/types/course'
import type { Post } from '@/types/post'
import type { Comment } from '@/types/comment'
import type { Profession } from '@/types/profession'
import type { Roadmap } from '@/types/roadmap'
import type { Node } from '@/types/node'
import type { ApprovalResponse } from '@/types/response'

// 设置 axios 默认配置
axios.defaults.withCredentials = true

const adminApiClient = axios.create({
  baseURL: 'http://localhost:9202/',
  timeout: 60000,
})

// 请求拦截器
adminApiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
adminApiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('Admin API Error:', error)
    return Promise.reject(error)
  }
)

// API v1 统一服务模块，使用 JSON 格式
const API_V1_PREFIX = '/api/v1'

// 管理员用户管理服务
export const adminUserServiceV1 = {
  getUsers(offsetId: number | null = null): Promise<ApiResponse<User[]>> {
    const params = offsetId !== null ? { offsetId } : {}
    return adminApiClient.get(`${API_V1_PREFIX}/admin/users`, { params })
  },

  // 管理员：按ID获取用户详情
  getUserById(userId: number): Promise<ApiResponse<User>> {
    return adminApiClient.get(`${API_V1_PREFIX}/admin/users/${userId}`)
  },

  // 管理员：搜索用户（包括被屏蔽用户）
  adminSearchUser(name: string): Promise<ApiResponse<User[]>> {
    return adminApiClient.get(`${API_V1_PREFIX}/admin/users/search`, {
      params: { name },
    })
  },

  updateUserState(userId: number, ban: boolean): Promise<ApiResponse<User>> {
    return adminApiClient.put(`${API_V1_PREFIX}/admin/users/${userId}/state`, null, {
      params: { ban },
    })
  },
}

// 管理员职业管理服务
export const adminProfessionServiceV1 = {
  // 按状态获取职业（管理端使用）
  getAdminProfessions(state?: number, lastId?: number | null): Promise<ApiResponse<Profession[]>> {
    const params: Record<string, any> = { state }
    if (lastId !== null && lastId !== undefined) {
      params.lastId = lastId
    }
    return adminApiClient.get(`${API_V1_PREFIX}/admin/professions`, { params })
  },
}

// 管理员课程管理服务
export const adminCourseServiceV1 = {
  getAdminCourses(state: number, lastId?: number | null, mainCategory?: number, subCategory?: number): Promise<ApiResponse<Course[]>> {
    const params: Record<string, any> = {}
    if (state !== null && state !== undefined) {
      params.state = state
    }
    if (lastId !== null && lastId !== undefined) {
      params.lastId = lastId
    }
    if (mainCategory !== undefined && mainCategory !== null) {
      params.mainCategory = mainCategory
    }
    if (subCategory !== undefined && subCategory !== null) {
      params.subCategory = subCategory
    }
    return adminApiClient.get(`${API_V1_PREFIX}/admin/courses`, { params })
  },

  // 管理员：按ID获取课程详情（所有状态）
  getAdminCourse(id: number): Promise<ApiResponse<Course>> {
    return adminApiClient.get(`${API_V1_PREFIX}/admin/courses/${id}`)
  },

  // 管理员：获取子课程列表
  getAdminSubcourses(parentId: number, state?: number): Promise<ApiResponse<Course[]>> {
    const params: Record<string, any> = {}
    if (state !== undefined && state !== null) {
      params.state = state
    }
    return adminApiClient.get(`${API_V1_PREFIX}/admin/courses/${parentId}/subcourses`, { params })
  },
}

// 管理员帖子管理服务
export const adminPostServiceV1 = {
  getPendingPosts(): Promise<ApiResponse<Post[]>> {
    return adminApiClient.get(`${API_V1_PREFIX}/admin/posts/pending`)
  },

  getPostsByState(state: string, lastId?: number, limit?: number): Promise<ApiResponse<Post[]>> {
    return adminApiClient.get(`${API_V1_PREFIX}/admin/posts`, {
      params: { state, lastId, limit },
    })
  },

  getPostsByFilter(nodeId?: number, creatorId?: number, lastId?: number, state?: number): Promise<ApiResponse<Post[]>> {
    return adminApiClient.get(`${API_V1_PREFIX}/admin/posts/filter`, {
      params: { nodeId, creatorId, lastId, state },
    })
  },

  approvePost(id: number, action: string, reason?: string): Promise<ApiResponse<any>> {
    return adminApiClient.post(`${API_V1_PREFIX}/admin/posts/${id}/approve`, {
      action,
      reason: reason || '',
    })
  },
}

// 管理员评论管理服务
export const adminCommentServiceV1 = {
  getCommentsByState(state: string, offsetId = 0): Promise<ApiResponse<Comment[]>> {
    return adminApiClient.get(`${API_V1_PREFIX}/admin/comments/${state}`, {
      params: { offsetId },
    })
  },

  getCommentsByFilter(objectType?: number, objectId?: number, creatorId?: number, lastId?: number, state?: number): Promise<ApiResponse<Comment[]>> {
    const params: Record<string, any> = {}
    if (objectType !== undefined && objectType !== null) params.objectType = objectType
    if (objectId !== undefined && objectId !== null) params.objectId = objectId
    if (creatorId !== undefined && creatorId !== null) params.creatorId = creatorId
    if (lastId !== undefined && lastId !== null) params.lastId = lastId
    if (state !== undefined && state !== null) params.state = state
    return adminApiClient.get(`${API_V1_PREFIX}/admin/comments/filter`, { params })
  },

  approveComment(id: number, action: string, reason?: string): Promise<ApiResponse<any>> {
    return adminApiClient.post(`${API_V1_PREFIX}/admin/comments/${id}/approve`, {
      action,
      reason: reason || '',
    })
  },
}

// 管理员路线图管理服务
export const adminRoadmapServiceV1 = {
  getAdminRoadmaps(state?: number, professionId?: number, creatorId?: number, lastId?: number | null): Promise<ApiResponse<Roadmap[]>> {
    const params: Record<string, any> = {}
    if (state !== undefined && state !== null) {
      params.state = state
    }
    if (professionId !== undefined && professionId !== null) {
      params.professionId = professionId
    }
    if (creatorId !== undefined && creatorId !== null) {
      params.creatorId = creatorId
    }
    if (lastId !== undefined && lastId !== null) {
      params.lastId = lastId
    }
    return adminApiClient.get(`${API_V1_PREFIX}/admin/roadmaps`, { params })
  },
}

// 系统配置服务
export const adminSystemServiceV1 = {
  getSystemConfig(part?: string): Promise<ApiResponse<any>> {
    if (part) {
      return adminApiClient.get(`${API_V1_PREFIX}/admin/system`, {
        params: { part },
      })
    }
    return adminApiClient.get(`${API_V1_PREFIX}/admin/system`)
  },

  // 根据key获取单个配置
  getConfigByKey(key: string): Promise<ApiResponse<string>> {
    return adminApiClient.get(`${API_V1_PREFIX}/admin/system/${key}`)
  },

  // 更新配置（key-value模式）
  updateConfigByKey(key: string, value: any): Promise<ApiResponse<string>> {
    return adminApiClient.post(
      `${API_V1_PREFIX}/admin/system`,
      {
        value,
      },
      {
        params: { key },
      }
    )
  },

  // 删除配置
  deleteConfigByKey(key: string): Promise<ApiResponse<string>> {
    return adminApiClient.delete(`${API_V1_PREFIX}/admin/system`, {
      params: { key },
    })
  },

  // 获取课程分类数据（调用公开接口）
  getCourseCategories(): Promise<ApiResponse<any>> {
    return adminApiClient.get(`${API_V1_PREFIX}/public/course-categories`)
  },

  // 获取职业分类数据（调用公开接口）
  getProfessionCategories(): Promise<ApiResponse<any>> {
    return adminApiClient.get(`${API_V1_PREFIX}/public/profession-categories`)
  },

  // 获取只读模式状态
  getReadonlyMode(): Promise<ApiResponse<{ enabled: boolean }>> {
    return adminApiClient.get(`${API_V1_PREFIX}/public/readonly-mode`)
  },

  // 设置只读模式（管理员）
  setReadonlyMode(enable: boolean): Promise<ApiResponse<string>> {
    return adminApiClient.post(`${API_V1_PREFIX}/admin/system/readonly-mode`, {
      enable,
    })
  },
}

// AutoAuthor 管理服务
export const adminAutoAuthorServiceV1 = {
  // 将节点加入到 AutoAuthor 队列
  enqueue(nodeId: number): Promise<ApiResponse<void>> {
    return adminApiClient.post(`${API_V1_PREFIX}/admin/auto-author/enqueue/${nodeId}`)
  },

  // 扫描节点并加入队列
  scan(): Promise<ApiResponse<number>> {
    return adminApiClient.post(`${API_V1_PREFIX}/admin/auto-author/scan`)
  },

  // 重置 opencode 会话
  resetSession(): Promise<ApiResponse<void>> {
    return adminApiClient.post(`${API_V1_PREFIX}/admin/auto-author/session/reset`)
  },

  // 清空队列
  clearQueue(): Promise<ApiResponse<void>> {
    return adminApiClient.delete(`${API_V1_PREFIX}/admin/auto-author/queue`)
  },
}

// 节点管理服务
export const adminNodeServiceV1 = {
  getAdminNodes(state?: number, nodeId?: number, courseId?: number, creatorId?: number, lastId?: number): Promise<ApiResponse<Node[]>> {
    const params: Record<string, any> = {}
    if (state !== undefined && state !== null) {
      params.state = state
    }
    if (nodeId !== undefined && nodeId !== null) {
      params.nodeId = nodeId
    }
    if (courseId !== undefined && courseId !== null) {
      params.courseId = courseId
    }
    if (creatorId !== undefined && creatorId !== null) {
      params.creatorId = creatorId
    }
    if (lastId !== undefined && lastId !== null) {
      params.lastId = lastId
    }
    return adminApiClient.get(`${API_V1_PREFIX}/admin/nodes`, { params })
  },

  updateNodeState(nodeId: number, state: number, reason?: string): Promise<ApiResponse<Node>> {
    return adminApiClient.put(`${API_V1_PREFIX}/admin/nodes/${nodeId}/state`, null, {
      params: {
        state,
        reason: reason || '',
      },
    })
  },
}

export default adminApiClient
