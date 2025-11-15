import apiClient from '../client'
import type { ApiResponse } from '@/types/api'
import type { Comment } from '@/types/comment'
import type { Post } from '@/types/post'
import type {
  OperationLogDTO,
  OperationLogQueryRequest,
  OperationLogPageResponse
} from '@/types/operationLog'

/**
 * 审批操作请求
 */
export interface OperateRequest {
  action: 'approve' | 'reject' | 'ban'
  reason?: string
}

/**
 * 审批响应
 */
export interface ApprovalResponse {
  success: boolean
  message: string
  objectId: number
  objectType: string
  action: string
}

/**
 * 管理后台 API
 */
export const adminApi = {
  // ========== 评论管理 ==========

  /**
   * 获取指定状态的评论
   */
  getCommentsByState(state: string, lastId?: number): Promise<ApiResponse<Comment[]>> {
    const params: Record<string, unknown> = {}
    if (lastId !== undefined) {
      params.lastId = lastId
    }
    return apiClient.get(`/v1/admin/comments/${state}`, { params })
  },

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
    return apiClient.get('/v1/admin/comments/filter', { params })
  },

  /**
   * 审核评论
   */
  approveComment(id: number, request: OperateRequest): Promise<ApiResponse<ApprovalResponse>> {
    return apiClient.post(`/v1/admin/comments/${id}/approve`, request)
  },

  // ========== 文章管理 ==========

  /**
   * 获取指定状态的文章
   */
  getPostsByState(state: string, lastId?: number): Promise<ApiResponse<Post[]>> {
    const params: Record<string, unknown> = {}
    if (lastId !== undefined) {
      params.lastId = lastId
    }
    return apiClient.get(`/v1/admin/posts/${state}`, { params })
  },

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
    return apiClient.get('/v1/admin/posts/filter', { params })
  },

  /**
   * 审核文章
   */
  approvePost(id: number, request: OperateRequest): Promise<ApiResponse<ApprovalResponse>> {
    return apiClient.post(`/v1/admin/posts/${id}/approve`, request)
  },

  // ========== 记忆卡片管理 ==========

  /**
   * 获取指定状态的卡片组
   */
  getDecksByState(state: string, lastId?: number): Promise<ApiResponse<any[]>> {
    const params: Record<string, unknown> = {}
    if (lastId !== undefined) {
      params.lastId = lastId
    }
    return apiClient.get(`/v1/admin/decks/${state}`, { params })
  },

  /**
   * 根据筛选条件获取卡片组（用于查询功能）
   */
  getDecksByFilter(
    nodeId?: number,
    creatorId?: number,
    state?: number,
    lastId?: number
  ): Promise<ApiResponse<any[]>> {
    const params: Record<string, unknown> = {}
    if (nodeId !== undefined) params.postId = nodeId // 对应后端的postId字段
    if (creatorId !== undefined) params.creatorId = creatorId
    if (state !== undefined) params.state = state
    if (lastId !== undefined) params.lastId = lastId
    params.limit = 20
    params.sortBy = 'createdAt'
    params.sortOrder = 'desc'

    return apiClient.get('/v1/admin/memory/decks', { params })
  },

  /**
   * 审核卡片组
   */
  approveDeck(id: number, request: OperateRequest): Promise<ApiResponse<ApprovalResponse>> {
    return apiClient.post(`/v1/admin/decks/${id}/approve`, request)
  },

  // ========== 课程管理 ==========

  /**
   * 获取指定状态的课程
   */
  getCoursesByState(state: string, lastId?: number): Promise<ApiResponse<any[]>> {
    const params: Record<string, unknown> = {}
    if (lastId !== undefined) {
      params.lastId = lastId
    }
    return apiClient.get(`/v1/admin/courses/${state}`, { params })
  },

  /**
   * 审核课程
   */
  approveCourse(id: number, request: OperateRequest): Promise<ApiResponse<ApprovalResponse>> {
    return apiClient.post(`/v1/admin/courses/${id}/approve`, request)
  },

  // ========== 职业管理 ==========

  /**
   * 获取指定状态的职业
   */
  getProfessionsByState(state: string, lastId?: number): Promise<ApiResponse<any[]>> {
    const params: Record<string, unknown> = {}
    if (lastId !== undefined) {
      params.lastId = lastId
    }
    return apiClient.get(`/v1/admin/professions/${state}`, { params })
  },

  /**
   * 审核职业
   */
  approveProfession(id: number, request: OperateRequest): Promise<ApiResponse<ApprovalResponse>> {
    return apiClient.post(`/v1/admin/professions/${id}/approve`, request)
  },

  // ========== 路线图管理 ==========

  /**
   * 获取指定状态的路线图
   */
  getRoadmapsByState(state: string, lastId?: number): Promise<ApiResponse<any[]>> {
    const params: Record<string, unknown> = {}
    if (lastId !== undefined) {
      params.lastId = lastId
    }
    return apiClient.get(`/v1/admin/roadmaps/${state}`, { params })
  },

  /**
   * 审核路线图
   */
  approveRoadmap(id: number, request: OperateRequest): Promise<ApiResponse<ApprovalResponse>> {
    return apiClient.post(`/v1/admin/roadmaps/${id}/approve`, request)
  },

  // ========== 节点管理 ==========

  /**
   * 根据筛选条件获取节点
   */
  getNodesByFilter(
    nodeId?: number,
    courseId?: number,
    creatorId?: number,
    state?: number,
    lastId?: number
  ): Promise<ApiResponse<any[]>> {
    const params: Record<string, unknown> = {}
    if (nodeId !== undefined) params.nodeId = nodeId
    if (courseId !== undefined) params.courseId = courseId
    if (creatorId !== undefined) params.creatorId = creatorId
    if (state !== undefined) params.state = state
    if (lastId !== undefined) params.lastId = lastId
    return apiClient.get('/v1/admin/nodes/filter', { params })
  },

  /**
   * 审核节点
   */
  approveNode(id: number, request: OperateRequest): Promise<ApiResponse<ApprovalResponse>> {
    return apiClient.post(`/v1/admin/nodes/${id}/approve`, request)
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
      params: { name }
    })
  },

  /**
   * 更新用户状态（封禁/解封）
   */
  updateUserState(userId: number, ban: boolean): Promise<ApiResponse<any>> {
    return apiClient.put(`/v1/admin/users/${userId}/state`, null, {
      params: { ban }
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
  getOperationLogs(query: OperationLogQueryRequest): Promise<ApiResponse<OperationLogPageResponse>> {
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
    return apiClient.post(
      '/v1/admin/system',
      { value },
      { params: { key } }
    )
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
