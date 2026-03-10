import apiClient from '../client'
import type { ApiResponse, KeysetPageResponse } from '@/types/api'
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
  getContentsByState(contentType: ContentType, state?: number, lastId?: number): Promise<ApiResponse<KeysetPageResponse<any>>> {
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
  ): Promise<ApiResponse<KeysetPageResponse<Comment>>> {
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
  ): Promise<ApiResponse<KeysetPageResponse<Post>>> {
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
  ): Promise<ApiResponse<KeysetPageResponse<any>>> {
    const params: Record<string, unknown> = {}
    if (postId !== undefined) params.postId = postId
    if (creatorId !== undefined) params.creatorId = creatorId
    if (state !== undefined) params.state = state
    if (lastId !== undefined) params.lastId = lastId
    return apiClient.get('/v1/admin/contents/memory_card_deck/filter', { params })
  },

  // ========== 课程管理 ==========

  /**
   * 获取课程详情
   */
  getCourseDetail(id: number): Promise<ApiResponse<any>> {
    return apiClient.get(`/v1/admin/contents/course/${id}`)
  },

  /**
   * 获取节点详情
   */
  getNodeDetail(id: number): Promise<ApiResponse<any>> {
    return apiClient.get(`/v1/admin/contents/node/${id}`)
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

  /**
   * 按名称搜索课程（管理后台）
   */
  searchCoursesByName(name: string, lastId?: number): Promise<ApiResponse<KeysetPageResponse<any>>> {
    const params: Record<string, unknown> = { name }
    if (lastId !== undefined) params.lastId = lastId
    return apiClient.get('/v1/admin/contents/course/search', { params })
  },

  // ========== 职业管理 ==========

  /**
   * 根据筛选条件获取职业
   */
  getProfessionsByFilter(state?: number, lastId?: number): Promise<ApiResponse<KeysetPageResponse<any>>> {
    const params: Record<string, unknown> = {}
    if (state !== undefined) params.state = state
    if (lastId !== undefined) params.lastId = lastId
    return apiClient.get('/v1/admin/contents/profession', { params })
  },

  /**
   * 按名称搜索职业（管理后台）
   */
  searchProfessionsByName(name: string, lastId?: number): Promise<ApiResponse<KeysetPageResponse<any>>> {
    const params: Record<string, unknown> = { name }
    if (lastId !== undefined) params.lastId = lastId
    return apiClient.get('/v1/admin/contents/profession/search', { params })
  },

  /**
   * 获取职业详情（管理后台）
   */
  getProfessionById(id: number): Promise<ApiResponse<any>> {
    return apiClient.get(`/v1/admin/contents/profession/${id}`)
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
    roadmapId?: number,
    professionId?: number,
    creatorId?: number,
    lastId?: number
  ): Promise<ApiResponse<KeysetPageResponse<any>>> {
    const params: Record<string, unknown> = {}
    if (roadmapId !== undefined) params.roadmapId = roadmapId
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
  ): Promise<ApiResponse<KeysetPageResponse<any>>> {
    const params: Record<string, unknown> = {}
    if (state !== undefined) params.state = state
    if (nodeId !== undefined) params.nodeId = nodeId
    if (courseId !== undefined) params.courseId = courseId
    if (creatorId !== undefined) params.creatorId = creatorId
    if (lastId !== undefined) params.lastId = lastId
    return apiClient.get('/v1/admin/contents/node/filter', { params })
  },

  // ========== 用户管理 ==========

  /**
   * 获取用户列表
   */
  getUsers(offsetId?: number | null): Promise<ApiResponse<KeysetPageResponse<any>>> {
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
    if (query.targetId !== undefined && query.targetId !== null) {
      params.targetId = query.targetId
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

  // ========== Robot 内容生成管理 ==========

  /**
   * 扫描节点并加入队列
   */
  scanAutoAuthorNodes(): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/admin/robot/scan')
  },

  /**
   * 将节点加入到 Robot 队列
   */
  enqueueAutoAuthorNode(
    id: number,
    idType: 'course' | 'node',
    contentType: 'auto' | 'index' | 'article',
    recursive: boolean,
    deleteExisting: boolean
  ): Promise<ApiResponse<void>> {
    return apiClient.post(`/v1/admin/robot/enqueue/${id}`, null, {
      params: { idType, contentType, recursive, deleteExisting },
    })
  },

  /**
   * 重置 opencode 会话
   */
  resetAutoAuthorSession(): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/admin/robot/session/reset')
  },

  /**
   * 压缩 opencode 会话上下文
   */
  summarizeAutoAuthorSession(): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/admin/robot/session/summarize')
  },

  /**
   * 清空 Robot 队列
   */
  clearAutoAuthorQueue(): Promise<ApiResponse<string>> {
    return apiClient.delete('/v1/admin/robot/queue')
  },

  /**
   * 获取队列统计信息
   */
  getRobotQueueStats(): Promise<
    ApiResponse<{
      pendingCount: number
      todayCompletedCount: number
      lastExecuteTime: string | null
      status: string
    }>
  > {
    return apiClient.get('/v1/admin/robot/queue/stats')
  },

  /**
   * 暂停队列
   */
  pauseRobotQueue(): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/admin/robot/queue/pause')
  },

  /**
   * 恢复队列
   */
  resumeRobotQueue(): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/admin/robot/queue/resume')
  },

  /**
   * 获取 Robot 配置
   */
  getRobotConfig(): Promise<
    ApiResponse<{
      aiService: string
      model: string
    }>
  > {
    return apiClient.get('/v1/admin/robot/config')
  },

  /**
   * 更新 Robot 配置
   */
  updateRobotConfig(config: { aiService?: string; model?: string }): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/admin/robot/config', config)
  },

  // ========== Robot 路径生成 ==========

  /**
   * 生成学习路径
   */
  generateRoadmap(professionId: number): Promise<
    ApiResponse<{
      taskId: string
      status: string
    }>
  > {
    return apiClient.post(`/v1/admin/robot/roadmap/generate/${professionId}`)
  },

  /**
   * 获取路径生成任务状态
   */
  getRoadmapTask(taskId: string): Promise<
    ApiResponse<{
      taskId: string
      professionId?: number
      userId?: number
      status: string
      result?: string
      error?: string
      createdAt?: string
      completedAt?: string
    }>
  > {
    return apiClient.get(`/v1/admin/robot/roadmap/task/${taskId}`)
  },

  /**
   * 获取路径生成历史记录
   */
  getRoadmapHistory(): Promise<
    ApiResponse<
      Array<{
        taskId: string
        professionId?: number
        userId?: number
        status: string
        result?: string
        error?: string
        createdAt?: string
        completedAt?: string
      }>
    >
  > {
    return apiClient.get('/v1/admin/robot/roadmap/history')
  },

  /**
   * 保存路径草稿
   */
  saveRoadmapDraft(professionId: number, draftContent: string): Promise<ApiResponse<string>> {
    return apiClient.post('/v1/admin/robot/roadmap/draft', draftContent, {
      params: { professionId },
      headers: { 'Content-Type': 'application/json' },
    })
  },

  /**
   * 获取路径草稿
   */
  getRoadmapDraft(draftId: string): Promise<ApiResponse<string>> {
    return apiClient.get(`/v1/admin/robot/roadmap/draft/${draftId}`)
  },

  /**
   * 删除路径草稿
   */
  deleteRoadmapDraft(draftId: string): Promise<ApiResponse<void>> {
    return apiClient.delete(`/v1/admin/robot/roadmap/draft/${draftId}`)
  },

  /**
   * 获取草稿列表
   */
  getRoadmapDrafts(): Promise<
    ApiResponse<
      Array<{
        draftId: string
        professionId?: number
        userId?: number
        createdAt?: string
      }>
    >
  > {
    return apiClient.get('/v1/admin/robot/roadmap/drafts')
  },

  /**
   * 创建路线图
   */
  createRoadmap(data: {
    professionId: number
    content: string
    description: string
    state: number
  }): Promise<ApiResponse<number>> {
    return apiClient.post('/v1/roadmaps', data)
  },

  /**
   * 创建课程并自动审核通过
   */
  createCourse(data: {
    name: string
    description: string
    mainCategory: number
    subCategory: number
  }): Promise<ApiResponse<number>> {
    return apiClient.post('/v1/admin/contents/course/create', data)
  },

  /**
   * 创建节点并自动审核通过
   */
  createNode(data: {
    name: string
    description: string
    courseId: number
  }): Promise<ApiResponse<number>> {
    return apiClient.post('/v1/admin/contents/node/create', data)
  },

  /**
   * 重新计算节点引用数统计
   */
  recalculateNodeReferences(): Promise<ApiResponse<{ processedPosts: number; updatedNodes: number }>> {
    return apiClient.post('/v1/admin/contents/nodes/recalculate-references')
  },

  // ========== 搜索索引同步 ==========

  /**
   * 全量同步所有搜索索引
   */
  syncAllSearchIndexes(): Promise<ApiResponse<string>> {
    return apiClient.post('/v1/admin/search/sync-all')
  },

  /**
   * 同步课程索引
   */
  syncCourseIndexes(): Promise<ApiResponse<number>> {
    return apiClient.post('/v1/admin/search/sync-courses')
  },

  /**
   * 同步节点索引
   */
  syncNodeIndexes(): Promise<ApiResponse<number>> {
    return apiClient.post('/v1/admin/search/sync-nodes')
  },

  /**
   * 同步用户索引
   */
  syncUserIndexes(): Promise<ApiResponse<number>> {
    return apiClient.post('/v1/admin/search/sync-users')
  },

  /**
   * 同步职业索引
   */
  syncProfessionIndexes(): Promise<ApiResponse<number>> {
    return apiClient.post('/v1/admin/search/sync-professions')
  },
}
