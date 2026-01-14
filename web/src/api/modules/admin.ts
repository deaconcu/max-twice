import apiClient from '../client'
import type { ApiResponse } from '@/types/api'
import type { Comment } from '@/types/comment'
import type { Post } from '@/types/post'
import type {
  OperationLogDTO,
  OperationLogQueryRequest,
  OperationLogPageResponse,
} from '@/types/operationLog'

/**
 * 审批操作请求
 */
export interface OperateRequest {
  action: 'approve' | 'reject' | 'remove' | 'ban' | 'restore' | 'delete'
  reason?: string
}

/**
 * 内容类型
 */
export type ContentType = 'post' | 'roadmap' | 'memory_card_deck' | 'comment' | 'course' | 'profession' | 'node'

/**
 * 管理后台 API
 */
export const adminApi = {
  // ========== 统一内容管理接口 ==========

  /**
   * 获取指定类型和状态的内容（统一接口）
   * @param contentType 内容类型
   * @param state 状态值（数字）
   * @param lastId 分页游标
   */
  getContentsByState(contentType: ContentType, state?: number, lastId?: number): Promise<ApiResponse<any[]>> {
    const params: Record<string, unknown> = {}
    if (state !== undefined && state !== null) params.state = state
    if (lastId !== undefined && lastId !== null) params.lastId = lastId
    return apiClient.get(`/v1/admin/contents/${contentType}`, { params })
  },

  /**
   * 内容操作（统一接口）
   * @param contentType 内容类型
   * @param id 内容ID
   * @param request 操作请求
   */
  operateContent(contentType: ContentType, id: number, request: OperateRequest): Promise<ApiResponse<void>> {
    return apiClient.post(`/v1/admin/contents/${contentType}/${id}/operate`, request)
  },

  // ========== 评论管理 ==========

  /**
   * 根据筛选条件获取评论
   */
  getCommentsByFilter(
    objectType?: number,
    objectId?: number,
    creatorId?: number,
    lastId?: number,
    state?: number
  ): Promise<ApiResponse<Comment[]>> {
    const params: Record<string, unknown> = {}
    if (objectType !== undefined) params.objectType = objectType
    if (objectId !== undefined) params.objectId = objectId
    if (creatorId !== undefined) params.creatorId = creatorId
    if (lastId !== undefined) params.lastId = lastId
    if (state !== undefined) params.state = state
    return apiClient.get('/v1/admin/contents/comment/filter', { params })
  },

  // ========== 文章管理 ==========

  /**
   * 根据筛选条件获取文章
   */
  getPostsByFilter(
    nodeId?: number,
    creatorId?: number,
    lastId?: number,
    state?: number
  ): Promise<ApiResponse<Post[]>> {
    const params: Record<string, unknown> = {}
    if (nodeId !== undefined) params.nodeId = nodeId
    if (creatorId !== undefined) params.creatorId = creatorId
    if (lastId !== undefined) params.lastId = lastId
    if (state !== undefined) params.state = state
    return apiClient.get('/v1/admin/contents/post/filter', { params })
  },

  // ========== 记忆卡片管理 ==========

  /**
   * 根据筛选条件获取卡片组
   */
  getDecksByFilter(
    postId?: number,
    creatorId?: number,
    state?: number,
    lastId?: number
  ): Promise<ApiResponse<any[]>> {
    const params: Record<string, unknown> = {}
    if (postId !== undefined) params.postId = postId
    if (creatorId !== undefined) params.creatorId = creatorId
    if (state !== undefined) params.state = state
    if (lastId !== undefined) params.lastId = lastId
    return apiClient.get('/v1/admin/contents/memory_card_deck/filter', { params })
  },

  // ========== 课程管理 ==========

  /**
   * 根据筛选条件获取课程
   */
  getCoursesByFilter(state?: number, lastId?: number): Promise<ApiResponse<any[]>> {
    const params: Record<string, unknown> = {}
    if (state !== undefined) params.state = state
    if (lastId !== undefined) params.lastId = lastId
    return apiClient.get('/v1/admin/contents/course/filter', { params })
  },

  /**
   * 获取课程详情
   */
  getCourseDetail(id: number): Promise<ApiResponse<any>> {
    return apiClient.get(`/v1/admin/contents/course/${id}`)
  },

  /**
   * 获取子课程列表
   */
  getSubcourses(parentId: number, state?: number): Promise<ApiResponse<any[]>> {
    const params: Record<string, unknown> = {}
    if (state !== undefined) params.state = state
    return apiClient.get(`/v1/admin/contents/course/${parentId}/subcourses`, { params })
  },

  /**
   * 更新课程信息
   */
  updateCourse(id: number, request: any): Promise<ApiResponse<void>> {
    return apiClient.put(`/v1/admin/contents/course/${id}`, request)
  },

  // ========== 职业管理 ==========

  /**
   * 根据筛选条件获取职业
   */
  getProfessionsByFilter(state?: number, lastId?: number): Promise<ApiResponse<any[]>> {
    const params: Record<string, unknown> = {}
    if (state !== undefined) params.state = state
    if (lastId !== undefined) params.lastId = lastId
    return apiClient.get('/v1/admin/contents/profession', { params })
  },

  /**
   * 更新职业信息
   */
  updateProfession(id: number, request: any): Promise<ApiResponse<void>> {
    return apiClient.put(`/v1/admin/contents/profession/${id}`, request)
  },

  // ========== 路线图管理 ==========

  /**
   * 根据筛选条件获取路线图
   */
  getRoadmapsByFilter(
    state?: number,
    professionId?: number,
    creatorId?: number,
    lastId?: number
  ): Promise<ApiResponse<any[]>> {
    const params: Record<string, unknown> = {}
    if (state !== undefined) params.state = state
    if (professionId !== undefined) params.professionId = professionId
    if (creatorId !== undefined) params.creatorId = creatorId
    if (lastId !== undefined) params.lastId = lastId
    return apiClient.get('/v1/admin/contents/roadmap/filter', { params })
  },

  /**
   * 更新路线图描述
   */
  updateRoadmap(id: number, description: string): Promise<ApiResponse<void>> {
    return apiClient.put(`/v1/admin/contents/roadmap/${id}`, { description })
  },

  // ========== 节点管理 ==========

  /**
   * 根据筛选条件获取节点
   */
  getNodesByFilter(
    state?: number,
    nodeId?: number,
    courseId?: number,
    creatorId?: number,
    lastId?: number
  ): Promise<ApiResponse<any[]>> {
    const params: Record<string, unknown> = {}
    if (state !== undefined) params.state = state
    if (nodeId !== undefined) params.nodeId = nodeId
    if (courseId !== undefined) params.courseId = courseId
    if (creatorId !== undefined) params.creatorId = creatorId
    if (lastId !== undefined) params.lastId = lastId
    return apiClient.get('/v1/admin/contents/node/filter', { params })
  },

  /**
   * 更新节点状态
   */
  updateNodeState(id: number, state: number, reason?: string): Promise<ApiResponse<void>> {
    const params: Record<string, unknown> = { state }
    if (reason) params.reason = reason
    return apiClient.put(`/v1/admin/contents/node/${id}/state`, null, { params })
  },

  // ========== 用户管理 ==========

  /**
   * 获取用户列表
   */
  getUsers(offsetId?: number | null): Promise<ApiResponse<any[]>> {
    const params: Record<string, unknown> = {}
    if (offsetId !== null && offsetId !== undefined) {
      params.offsetId = offsetId
    }
    return apiClient.get('/v1/admin/users', { params })
  },

  /**
   * 按ID获取用户详情
   */
  getUserById(userId: number): Promise<ApiResponse<any>> {
    return apiClient.get(`/v1/admin/users/${userId}`)
  },

  /**
   * 搜索用户（包括被屏蔽用户）
   */
  adminSearchUser(name: string): Promise<ApiResponse<any[]>> {
    return apiClient.get('/v1/admin/users/search', {
      params: { name },
    })
  },

  /**
   * 更新用户状态（封禁/解封）
   */
  updateUserState(userId: number, ban: boolean): Promise<ApiResponse<any>> {
    return apiClient.put(`/v1/admin/users/${userId}/state`, null, {
      params: { ban },
    })
  },

  /**
   * 更新用户角色
   */
  updateUserRole(userId: number, role: number): Promise<ApiResponse<any>> {
    return apiClient.post(`/v1/admin/users/${userId}/role`, { role })
  },

  // ========== 系统操作 ==========

  /**
   * 重建缓存
   */
  rebuildCache(): Promise<ApiResponse<any>> {
    return apiClient.post('/v1/admin/system/rebuild-cache')
  },

  /**
   * 清空缓存
   */
  clearCache(): Promise<ApiResponse<any>> {
    return apiClient.post('/v1/admin/system/clear-cache')
  },

  /**
   * 重新计算分数
   */
  recalculateScores(): Promise<ApiResponse<any>> {
    return apiClient.post('/v1/admin/system/recalculate-scores')
  },

  /**
   * 获取系统统计信息
   */
  getSystemStats(): Promise<ApiResponse<any>> {
    return apiClient.get('/v1/admin/system/stats')
  },

  // ========== 操作日志 ==========

  /**
   * 获取操作日志
   */
  getOperationLogs(
    query: OperationLogQueryRequest
  ): Promise<ApiResponse<OperationLogPageResponse>> {
    const params: Record<string, unknown> = {}
    if (query.operatorId !== undefined && query.operatorId !== null) {
      params.operatorId = query.operatorId
    }
    if (query.module) {
      params.module = query.module
    }
    if (query.operationType) {
      params.operationType = query.operationType
    }
    if (query.targetType) {
      params.targetType = query.targetType
    }
    if (query.operationLevel !== undefined && query.operationLevel !== null) {
      params.operationLevel = query.operationLevel
    }
    if (query.startTime) {
      params.startTime = query.startTime
    }
    if (query.endTime) {
      params.endTime = query.endTime
    }
    if (query.lastId !== undefined && query.lastId !== null) {
      params.lastId = query.lastId
    }
    if (query.limit !== undefined && query.limit !== null) {
      params.limit = query.limit
    }
    return apiClient.get('/v1/admin/operation-logs', { params })
  },

  /**
   * 查询单条操作日志详情
   */
  getOperationLogById(id: number): Promise<ApiResponse<OperationLogDTO>> {
    return apiClient.get(`/v1/admin/operation-logs/${id}`)
  },

  // ========== 系统配置 ==========

  /**
   * 获取系统配置
   */
  getSystemConfig(part?: string): Promise<ApiResponse<any>> {
    if (part) {
      return apiClient.get('/v1/admin/system', { params: { part } })
    }
    return apiClient.get('/v1/admin/system')
  },

  /**
   * 根据key获取单个配置
   */
  getConfigByKey(key: string): Promise<ApiResponse<string>> {
    return apiClient.get(`/v1/admin/system/${key}`)
  },

  /**
   * 更新配置（key-value模式）
   */
  updateConfigByKey(key: string, value: any): Promise<ApiResponse<string>> {
    return apiClient.post('/v1/admin/system', { value }, { params: { key } })
  },

  /**
   * 删除配置
   */
  deleteConfigByKey(key: string): Promise<ApiResponse<string>> {
    return apiClient.delete('/v1/admin/system', { params: { key } })
  },

  // ========== AutoAuthor 管理 ==========

  /**
   * 扫描节点并加入队列
   */
  scanAutoAuthorNodes(): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/admin/auto-author/scan')
  },

  /**
   * 将节点加入到 AutoAuthor 队列
   */
  enqueueAutoAuthorNode(nodeId: number): Promise<ApiResponse<void>> {
    return apiClient.post(`/v1/admin/auto-author/enqueue/${nodeId}`)
  },

  /**
   * 重置 opencode 会话
   */
  resetAutoAuthorSession(): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/admin/auto-author/session/reset')
  },

  /**
   * 清空 AutoAuthor 队列
   */
  clearAutoAuthorQueue(): Promise<ApiResponse<string>> {
    return apiClient.delete('/v1/admin/auto-author/queue')
  },
}
