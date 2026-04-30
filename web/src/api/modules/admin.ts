import apiClient from '../client'
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
export type ContentType =
  | 'post'
  | 'roadmap'
  | 'memory_card_deck'
  | 'comment'
  | 'course'
  | 'role'
  | 'node'

/**
 * 管理后台 API
 */
export const adminApi = {
  // ========== 统一内容管理接口 ==========

  /**
   * 获取指定类型和状态的内容（统一接口）
   */
  getContentsByState(
    contentType: ContentType,
    state?: number,
    cursor?: string
  ): Promise<unknown[]> {
    const params: Record<string, unknown> = {}
    if (state !== undefined && state !== null) params.state = state
    if (cursor !== undefined && cursor !== null) params.cursor = cursor
    return apiClient.get(`/admin/contents/${contentType}`, { params })
  },

  /**
   * 内容操作（统一接口）
   */
  operateContent(contentType: ContentType, id: number, request: OperateRequest): Promise<void> {
    return apiClient.post(`/admin/contents/${contentType}/${id}/operate`, request)
  },

  // ========== 评论管理 ==========

  /**
   * 根据筛选条件获取评论
   */
  getCommentsByFilter(
    objectType?: number,
    objectId?: number,
    creatorId?: number,
    cursor?: string,
    state?: number
  ): Promise<Comment[]> {
    const params: Record<string, unknown> = {}
    if (objectType !== undefined) params.objectType = objectType
    if (objectId !== undefined) params.objectId = objectId
    if (creatorId !== undefined) params.creatorId = creatorId
    if (cursor !== undefined) params.cursor = cursor
    if (state !== undefined) params.state = state
    return apiClient.get('/admin/contents/comment/filter', { params })
  },

  // ========== 文章管理 ==========

  /**
   * 根据筛选条件获取文章
   */
  getPostsByFilter(
    nodeId?: number,
    creatorId?: number,
    cursor?: string,
    state?: number
  ): Promise<Post[]> {
    const params: Record<string, unknown> = {}
    if (nodeId !== undefined) params.nodeId = nodeId
    if (creatorId !== undefined) params.creatorId = creatorId
    if (cursor !== undefined) params.cursor = cursor
    if (state !== undefined) params.state = state
    return apiClient.get('/admin/contents/post/filter', { params })
  },

  // ========== 记忆卡片管理 ==========

  /**
   * 根据筛选条件获取卡片组
   */
  getDecksByFilter(
    postId?: number,
    creatorId?: number,
    state?: number,
    cursor?: string
  ): Promise<unknown[]> {
    const params: Record<string, unknown> = {}
    if (postId !== undefined) params.postId = postId
    if (creatorId !== undefined) params.creatorId = creatorId
    if (state !== undefined) params.state = state
    if (cursor !== undefined) params.cursor = cursor
    return apiClient.get('/admin/contents/memory_card_deck/filter', { params })
  },

  // ========== 课程管理 ==========

  /**
   * 根据筛选条件获取课程
   */
  getCoursesByFilter(state?: number, cursor?: string): Promise<unknown[]> {
    const params: Record<string, unknown> = {}
    if (state !== undefined) params.state = state
    if (cursor !== undefined) params.cursor = cursor
    return apiClient.get('/admin/contents/course/filter', { params })
  },

  /**
   * 获取课程详情
   */
  getCourseDetail(id: number): Promise<unknown> {
    return apiClient.get(`/admin/contents/course/${id}`)
  },

  /**
   * 获取子课程列表
   */
  getSubcourses(parentId: number, state?: number): Promise<unknown[]> {
    const params: Record<string, unknown> = {}
    if (state !== undefined) params.state = state
    return apiClient.get(`/admin/contents/course/${parentId}/subcourses`, { params })
  },

  /**
   * 更新课程信息
   */
  updateCourse(id: number, request: Record<string, unknown>): Promise<void> {
    return apiClient.put(`/admin/contents/course/${id}`, request)
  },

  // ========== 角色管理 ==========

  /**
   * 根据筛选条件获取角色
   */
  getRolesByFilter(state?: number, cursor?: string): Promise<unknown[]> {
    const params: Record<string, unknown> = {}
    if (state !== undefined) params.state = state
    if (cursor !== undefined) params.cursor = cursor
    return apiClient.get('/admin/contents/role', { params })
  },

  /**
   * 更新角色信息
   */
  updateRole(id: number, request: Record<string, unknown>): Promise<void> {
    return apiClient.put(`/admin/contents/role/${id}`, request)
  },

  // ========== 路线图管理 ==========

  /**
   * 根据筛选条件获取路线图
   */
  getRoadmapsByFilter(
    state?: number,
    roleId?: number,
    creatorId?: number,
    cursor?: string
  ): Promise<unknown[]> {
    const params: Record<string, unknown> = {}
    if (state !== undefined) params.state = state
    if (roleId !== undefined) params.roleId = roleId
    if (creatorId !== undefined) params.creatorId = creatorId
    if (cursor !== undefined) params.cursor = cursor
    return apiClient.get('/admin/contents/roadmap/filter', { params })
  },

  /**
   * 更新路线图描述
   */
  updateRoadmap(id: number, description: string): Promise<void> {
    return apiClient.put(`/admin/contents/roadmap/${id}`, { description })
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
    cursor?: string
  ): Promise<unknown[]> {
    const params: Record<string, unknown> = {}
    if (state !== undefined) params.state = state
    if (nodeId !== undefined) params.nodeId = nodeId
    if (courseId !== undefined) params.courseId = courseId
    if (creatorId !== undefined) params.creatorId = creatorId
    if (cursor !== undefined) params.cursor = cursor
    return apiClient.get('/admin/contents/node/filter', { params })
  },

  /**
   * 更新节点状态
   */
  updateNodeState(id: number, state: number, reason?: string): Promise<void> {
    const params: Record<string, unknown> = { state }
    if (reason) params.reason = reason
    return apiClient.put(`/admin/contents/node/${id}/state`, null, { params })
  },

  // ========== 用户管理 ==========

  /**
   * 获取用户列表
   */
  getUsers(offsetId?: number | null): Promise<unknown[]> {
    const params: Record<string, unknown> = {}
    if (offsetId !== null && offsetId !== undefined) {
      params.offsetId = offsetId
    }
    return apiClient.get('/admin/users', { params })
  },

  /**
   * 按ID获取用户详情
   */
  getUserById(userId: number): Promise<unknown> {
    return apiClient.get(`/admin/users/${userId}`)
  },

  /**
   * 搜索用户（包括被屏蔽用户）
   */
  adminSearchUser(name: string): Promise<unknown[]> {
    return apiClient.get('/admin/users/search', {
      params: { name },
    })
  },

  /**
   * 更新用户状态（封禁/解封）
   */
  updateUserState(userId: number, ban: boolean): Promise<unknown> {
    return apiClient.put(`/admin/users/${userId}/state`, null, {
      params: { ban },
    })
  },

  /**
   * 更新用户角色
   */
  updateUserRole(userId: number, role: string): Promise<unknown> {
    return apiClient.post(`/admin/users/${userId}/role`, { role })
  },

  // ========== 系统操作 ==========

  /**
   * 重建缓存
   */
  rebuildCache(): Promise<unknown> {
    return apiClient.post('/admin/system/rebuild-cache')
  },

  /**
   * 清空缓存
   */
  clearCache(): Promise<unknown> {
    return apiClient.post('/admin/system/clear-cache')
  },

  /**
   * 重新计算分数
   */
  recalculateScores(): Promise<unknown> {
    return apiClient.post('/admin/system/recalculate-scores')
  },

  /**
   * 获取系统统计信息
   */
  getSystemStats(): Promise<unknown> {
    return apiClient.get('/admin/system/stats')
  },

  // ========== 操作日志 ==========

  /**
   * 获取操作日志
   */
  getOperationLogs(query: OperationLogQueryRequest): Promise<OperationLogPageResponse> {
    const params: Record<string, unknown> = {}
    if (query.operatorId !== undefined && query.operatorId !== null) {
      params.operatorId = query.operatorId
    }
    if (query.module) params.module = query.module
    if (query.operationType) params.operationType = query.operationType
    if (query.targetType) params.targetType = query.targetType
    if (query.operationLevel !== undefined && query.operationLevel !== null) {
      params.operationLevel = query.operationLevel
    }
    if (query.startTime) params.startTime = query.startTime
    if (query.endTime) params.endTime = query.endTime
    if (query.lastId !== undefined && query.lastId !== null) params.lastId = query.lastId
    if (query.limit !== undefined && query.limit !== null) params.limit = query.limit
    return apiClient.get('/admin/operation-logs', { params })
  },

  /**
   * 查询单条操作日志详情
   */
  getOperationLogById(id: number): Promise<OperationLogDTO> {
    return apiClient.get(`/admin/operation-logs/${id}`)
  },

  // ========== 系统配置 ==========

  /**
   * 获取系统配置
   */
  getSystemConfig(part?: string): Promise<unknown> {
    if (part) {
      return apiClient.get('/admin/system', { params: { part } })
    }
    return apiClient.get('/admin/system')
  },

  /**
   * 根据key获取单个配置
   */
  getConfigByKey(key: string): Promise<string> {
    return apiClient.get(`/admin/system/${key}`)
  },

  /**
   * 更新配置（key-value模式）
   */
  updateConfigByKey(key: string, value: unknown): Promise<string> {
    return apiClient.post('/admin/system', { value }, { params: { key } })
  },

  /**
   * 删除配置
   */
  deleteConfigByKey(key: string): Promise<string> {
    return apiClient.delete('/admin/system', { params: { key } })
  },

  // ========== AutoAuthor 管理 ==========

  /**
   * 扫描节点并加入队列
   */
  scanAutoAuthorNodes(): Promise<void> {
    return apiClient.post('/admin/auto-author/scan')
  },

  /**
   * 将节点加入到 AutoAuthor 队列
   */
  enqueueAutoAuthorNode(nodeId: number): Promise<void> {
    return apiClient.post(`/admin/auto-author/enqueue/${nodeId}`)
  },

  /**
   * 重置 opencode 会话
   */
  resetAutoAuthorSession(): Promise<void> {
    return apiClient.post('/admin/auto-author/session/reset')
  },

  /**
   * 清空 AutoAuthor 队列
   */
  clearAutoAuthorQueue(): Promise<string> {
    return apiClient.delete('/admin/auto-author/queue')
  },

  /**
   * 重新计算节点引用数统计
   */
  recalculateNodeReferences(): Promise<{ processedPosts: number; updatedNodes: number }> {
    return apiClient.post('/admin/contents/nodes/recalculate-references')
  },

  // ========== 搜索索引同步 ==========

  /**
   * 全量同步所有搜索索引
   */
  syncAllSearchIndexes(): Promise<string> {
    return apiClient.post('/admin/search/sync-all')
  },

  /**
   * 同步课程索引
   */
  syncCourseIndexes(): Promise<number> {
    return apiClient.post('/admin/search/sync-courses')
  },

  /**
   * 同步节点索引
   */
  syncNodeIndexes(): Promise<number> {
    return apiClient.post('/admin/search/sync-nodes')
  },

  /**
   * 同步用户索引
   */
  syncUserIndexes(): Promise<number> {
    return apiClient.post('/admin/search/sync-users')
  },

  /**
   * 同步角色索引
   */
  syncRoleIndexes(): Promise<number> {
    return apiClient.post('/admin/search/sync-roles')
  },
}
