/**
 * 记忆卡片API服务
 * 连接后端真实接口
 */

import apiClient from './api/v1/apiServiceV1'
import { ObjectType, VoteType } from '@/types/enums'
import type {
  MemoryCardView,
  ReviewCardRequest,
  GetReviewQueueQuery,
  ReviewStats,
  CourseMemoryBank,
  ReviewSession,
  MemoryCardDeck,
  DeckDetail,
  GetDecksQuery,
  CreateDeckRequest,
  UpdateDeckRequest,
  CreateCardRequest,
  UpdateCardRequest
} from '@/types/memoryCard'

const API_V1_PREFIX = '/api/v1'

export class MemoryService {
  /**
   * 获取复习队列 - 只获取到期的卡片，限制100个
   */
  static async getReviewQueue(query: { courseId?: number } = {}): Promise<{
    code: number
    data: MemoryCardView[]
    message?: string
  }> {
    const params = new URLSearchParams()
    
    if (query.courseId) {
      params.append('courseId', query.courseId.toString())
    }

    return apiClient.get(`${API_V1_PREFIX}/memory/review/queue?${params.toString()}`)
  }

  /**
   * 获取卡片列表 - 支持分页查询全部卡片
   */
  static async getCardList(query: GetReviewQueueQuery = {}): Promise<{
    code: number
    data: MemoryCardView[]
    message?: string
  }> {
    const params = new URLSearchParams()
    
    if (query.courseId) {
      params.append('courseId', query.courseId.toString())
    }
    if (query.limit) {
      params.append('limit', query.limit.toString())
    }
    if (query.lastId) {
      params.append('lastId', query.lastId.toString())
    }

    return apiClient.get(`${API_V1_PREFIX}/memory/review/cards?${params.toString()}`)
  }

  /**
   * 提交复习结果
   */
  static async reviewCard(request: ReviewCardRequest): Promise<{
    code: number
    data: any
    message?: string
  }> {
    return apiClient.post(`${API_V1_PREFIX}/memory/review/submit`, request)
  }

  /**
   * 批量提交复习结果
   */
  static async batchSubmitReview(session: ReviewSession): Promise<{
    code: number
    data: any
    message?: string
  }> {
    return apiClient.post(`${API_V1_PREFIX}/memory/review/batch-submit`, session)
  }

  /**
   * 获取复习统计
   */
  static async getReviewStats(period: string = 'WEEK'): Promise<{
    code: number
    data: ReviewStats
    message?: string
  }> {
    return apiClient.get(`${API_V1_PREFIX}/memory/review/stats?period=${period}`)
  }

  /**
   * 获取记忆库课程列表
   */
  static async getMemoryBankCourses(status?: number): Promise<{
    code: number
    data: CourseMemoryBank[]
    message?: string
  }> {
    const params = status ? `?status=${status}` : ''
    return apiClient.get(`${API_V1_PREFIX}/memory/memory-bank/courses${params}`)
  }

  /**
   * 更新课程复习策略
   */
  static async updateCourseSetting(courseId: number, setting: {
    frequencySetting?: number
    status?: number
  }): Promise<{
    code: number
    data: any
    message?: string
  }> {
    return apiClient.put(`${API_V1_PREFIX}/memory/memory-bank/courses/${courseId}/settings`, setting)
  }

  /**
   * 添加卡片组到记忆库
   */
  static async addDeckToMemoryBank(request: {
    deckId: number
    courseId: number
  }): Promise<{
    code: number
    data: any
    message?: string
  }> {
    return apiClient.post(`${API_V1_PREFIX}/memory/memory-bank/decks`, request)
  }

  /**
   * 移除卡片组
   */
  static async removeDeckFromCourse(courseId: number, deckId: number): Promise<{
    code: number
    data: any
    message?: string
  }> {
    return apiClient.delete(`${API_V1_PREFIX}/memory/memory-bank/courses/${courseId}/decks/${deckId}`)
  }

  // ========== Deck相关API ==========

  /**
   * 需求1: 获取帖子下的公共卡片组列表 - keyset分页，normal状态
   */
  static async getPostPublicDecks(postId: number, query: {
    sortBy?: string
    sortOrder?: string
    lastScore?: number
    lastId?: number
    limit?: number
  } = {}): Promise<{
    code: number
    data: {
      items: MemoryCardDeck[]
      hasMore: boolean
      nextCursor?: {
        lastScore?: number
        lastId?: number
      }
    }
    message?: string
  }> {
    const params = new URLSearchParams()

    if (query.sortBy) {
      params.append('sortBy', query.sortBy)
    }
    if (query.sortOrder) {
      params.append('sortOrder', query.sortOrder)
    }
    if (query.lastScore !== undefined) {
      params.append('lastScore', query.lastScore.toString())
    }
    if (query.lastId) {
      params.append('lastId', query.lastId.toString())
    }
    if (query.limit) {
      params.append('limit', query.limit.toString())
    }

    return apiClient.get(`${API_V1_PREFIX}/memory/posts/${postId}/decks?${params.toString()}`)
  }

  /**
   * 需求2: 获取帖子创建者提交的卡片组 - 最新创建，limit通常为1
   */
  static async getPostCreatorDeck(postId: number, query: {
    sortBy?: string
    sortOrder?: string
    lastScore?: number
    lastId?: number
    limit?: number
  } = {}): Promise<{
    code: number
    data: {
      items: MemoryCardDeck[]
      hasMore: boolean
      nextCursor?: {
        lastScore?: number
        lastId?: number
      }
    }
    message?: string
  }> {
    const params = new URLSearchParams()

    if (query.sortBy) {
      params.append('sortBy', query.sortBy)
    }
    if (query.sortOrder) {
      params.append('sortOrder', query.sortOrder)
    }
    if (query.lastScore !== undefined) {
      params.append('lastScore', query.lastScore.toString())
    }
    if (query.lastId) {
      params.append('lastId', query.lastId.toString())
    }
    if (query.limit) {
      params.append('limit', query.limit.toString())
    }

    return apiClient.get(`${API_V1_PREFIX}/memory/posts/${postId}/creator-deck?${params.toString()}`)
  }

  /**
   * 需求3: 获取用户自己在指定帖子下提交的卡片组 - 最新创建，limit通常为1
   */
  static async getMyPostDeck(postId: number, query: {
    sortBy?: string
    sortOrder?: string
    lastScore?: number
    lastId?: number
    limit?: number
  } = {}): Promise<{
    code: number
    data: {
      items: MemoryCardDeck[]
      hasMore: boolean
      nextCursor?: {
        lastScore?: number
        lastId?: number
      }
    }
    message?: string
  }> {
    const params = new URLSearchParams()

    if (query.sortBy) {
      params.append('sortBy', query.sortBy)
    }
    if (query.sortOrder) {
      params.append('sortOrder', query.sortOrder)
    }
    if (query.lastScore !== undefined) {
      params.append('lastScore', query.lastScore.toString())
    }
    if (query.lastId) {
      params.append('lastId', query.lastId.toString())
    }
    if (query.limit) {
      params.append('limit', query.limit.toString())
    }

    return apiClient.get(`${API_V1_PREFIX}/memory/posts/${postId}/my-deck?${params.toString()}`)
  }

  /**
   * 需求4: 获取用户自己提交的所有卡片组 - keyset分页，全部状态
   */
  static async getMyAllDecks(query: {
    sortBy?: string
    sortOrder?: string
    lastScore?: number
    lastId?: number
    limit?: number
  } = {}): Promise<{
    code: number
    data: {
      items: MemoryCardDeck[]
      hasMore: boolean
      nextCursor?: {
        lastScore?: number
        lastId?: number
      }
    }
    message?: string
  }> {
    const params = new URLSearchParams()

    if (query.sortBy) {
      params.append('sortBy', query.sortBy)
    }
    if (query.sortOrder) {
      params.append('sortOrder', query.sortOrder)
    }
    if (query.lastScore !== undefined) {
      params.append('lastScore', query.lastScore.toString())
    }
    if (query.lastId) {
      params.append('lastId', query.lastId.toString())
    }
    if (query.limit) {
      params.append('limit', query.limit.toString())
    }

    return apiClient.get(`${API_V1_PREFIX}/memory/decks/my?${params.toString()}`)
  }

  /**
   * 获取指定用户创建的所有卡片组 - 用于查看其他用户的卡片组
   */
  static async getUserDecks(userId: number, query: {
    sortBy?: string
    sortOrder?: string
    lastId?: number
    limit?: number
  } = {}): Promise<{
    code: number
    data: {
      items: MemoryCardDeck[]
      hasMore: boolean
      nextCursor?: {
        lastScore?: number
        lastId?: number
      }
    }
    message?: string
  }> {
    // 使用 getDecksForReview 接口，只传 creatorId
    return this.getDecksForReview({
      creatorId: userId,
      sortBy: query.sortBy || 'createdAt',
      sortOrder: query.sortOrder || 'desc',
      lastId: query.lastId,
      limit: query.limit || 10,
      state: 2 // 只显示已发布状态的卡片组 (PUBLISHED = 2)
    }) as any // 返回类型稍有不同，但兼容
  }

  /**
   * 需求5: 管理员按状态查询所有卡片组 - keyset分页
   */
  static async getAdminDecks(state: number, query: {
    sortBy?: string
    sortOrder?: string
    lastScore?: number
    lastId?: number
    limit?: number
  } = {}): Promise<{
    code: number
    data: {
      items: MemoryCardDeck[]
      hasMore: boolean
      nextCursor?: {
        lastScore?: number
        lastId?: number
      }
    }
    message?: string
  }> {
    const params = new URLSearchParams()

    params.append('state', state.toString())
    if (query.sortBy) {
      params.append('sortBy', query.sortBy)
    }
    if (query.sortOrder) {
      params.append('sortOrder', query.sortOrder)
    }
    if (query.lastScore !== undefined) {
      params.append('lastScore', query.lastScore.toString())
    }
    if (query.lastId) {
      params.append('lastId', query.lastId.toString())
    }
    if (query.limit) {
      params.append('limit', query.limit.toString())
    }

    return apiClient.get(`${API_V1_PREFIX}/memory/decks/admin?${params.toString()}`)
  }

  /**
   * 获取指定节点下的卡片组列表
   */
  static async getDecksByNode(nodeId: number, query: {
    lastScore?: number
    lastId?: number
    limit?: number
  } = {}): Promise<{
    code: number
    data: {
      items: MemoryCardDeck[]
      hasMore: boolean
      nextCursor?: {
        lastScore?: number
        lastId?: number
      }
    }
    message?: string
  }> {
    const params = new URLSearchParams()

    if (query.lastScore !== undefined) {
      params.append('lastScore', query.lastScore.toString())
    }
    if (query.lastId) {
      params.append('lastId', query.lastId.toString())
    }
    if (query.limit) {
      params.append('limit', query.limit.toString())
    }

    return apiClient.get(`${API_V1_PREFIX}/memory/decks/node/${nodeId}?${params.toString()}`)
  }

  /**
   * 获取卡片组审核列表 - 包含卡片详情
   */
  static async getDecksForReview(query: GetDecksQuery = {}): Promise<{
    code: number
    data: { 
      items: DeckDetail[]
      hasMore: boolean
      nextCursor?: {
        lastScore?: number
        lastId?: number
      }
    }
    message?: string
  }> {
    const params = new URLSearchParams()
    
    if (query.postId) {
      params.append('postId', query.postId.toString())
    }
    if (query.creatorId) {
      params.append('creatorId', query.creatorId.toString())
    }
    if (query.state !== undefined) {
      params.append('state', query.state.toString())
    }
    if (query.sortBy) {
      params.append('sortBy', query.sortBy)
    }
    if (query.sortOrder) {
      params.append('sortOrder', query.sortOrder)
    }
    if (query.lastScore !== undefined) {
      params.append('lastScore', query.lastScore.toString())
    }
    if (query.lastId) {
      params.append('lastId', query.lastId.toString())
    }
    if (query.limit) {
      params.append('limit', query.limit.toString())
    }

    return apiClient.get(`${API_V1_PREFIX}/memory/decks/review?${params.toString()}`)
  }

  /**
   * 获取卡片组详情
   */
  static async getDeckDetail(deckId: number): Promise<{
    code: number
    data: DeckDetail
    message?: string
  }> {
    return apiClient.get(`${API_V1_PREFIX}/memory/decks/${deckId}`)
  }

  /**
   * 创建卡片组
   */
  static async createDeck(request: CreateDeckRequest): Promise<{
    code: number
    data: MemoryCardDeck
    message?: string
  }> {
    return apiClient.post(`${API_V1_PREFIX}/memory/decks`, request)
  }

  /**
   * 更新卡片组
   */
  static async updateDeck(deckId: number, request: UpdateDeckRequest): Promise<{
    code: number
    data: MemoryCardDeck
    message?: string
  }> {
    return apiClient.put(`${API_V1_PREFIX}/memory/decks/${deckId}`, request)
  }

  // ========== Card相关API ==========

  /**
   * 创建卡片
   */
  static async createCard(request: CreateCardRequest): Promise<{
    code: number
    data: MemoryCardView
    message?: string
  }> {
    return apiClient.post(`${API_V1_PREFIX}/memory/cards`, request)
  }

  /**
   * 更新卡片
   */
  static async updateCard(cardId: number, request: UpdateCardRequest): Promise<{
    code: number
    data: MemoryCardView
    message?: string
  }> {
    return apiClient.put(`${API_V1_PREFIX}/memory/cards/${cardId}`, request)
  }

  /**
   * 删除卡片
   */
  static async deleteCard(cardId: number): Promise<{
    code: number
    data: any
    message?: string
  }> {
    return apiClient.delete(`${API_V1_PREFIX}/memory/cards/${cardId}`)
  }

  /**
   * 添加卡片到学习计划
   */
  static async addCardToStudy(cardId: number): Promise<{
    code: number
    data: any
    message?: string
  }> {
    // TODO: 需要确认后端是否有此接口，可能需要通过添加卡片组到记忆库的方式实现
    return apiClient.post(`${API_V1_PREFIX}/memory/cards/${cardId}/study`)
  }

  // ========== 审核相关API ==========

  /**
   * 审核通过卡片组
   */
  static async approveDeck(deckId: number): Promise<{
    code: number
    data: any
    message?: string
  }> {
    return apiClient.post(`${API_V1_PREFIX}/memory/decks/${deckId}/approve`)
  }

  /**
   * 拒绝卡片组
   */
  static async rejectDeck(deckId: number, reason?: string): Promise<{
    code: number
    data: any
    message?: string
  }> {
    return apiClient.post(`${API_V1_PREFIX}/memory/decks/${deckId}/reject`, {
      reason: reason || ''
    })
  }

  /**
   * 屏蔽卡片组
   */
  static async banDeck(deckId: number, reason?: string): Promise<{
    code: number
    data: any
    message?: string
  }> {
    return apiClient.post(`${API_V1_PREFIX}/memory/decks/${deckId}/ban`, {
      reason: reason || ''
    })
  }

  /**
   * 恢复卡片组
   */
  static async restoreDeck(deckId: number): Promise<{
    code: number
    data: any
    message?: string
  }> {
    return apiClient.post(`${API_V1_PREFIX}/memory/decks/${deckId}/restore`)
  }

  /**
   * 获取卡片组更新差异
   */
  static async getDeckDiff(deckId: number, userCurrentVersion?: number): Promise<{
    code: number
    data: any
    message?: string
  }> {
    const params = userCurrentVersion ? `?userCurrentVersion=${userCurrentVersion}` : ''
    return apiClient.get(`${API_V1_PREFIX}/memory/decks/${deckId}/diff${params}`)
  }

  /**
   * 获取单个卡片的内容差异
   */
  static async getCardDiff(cardId: number): Promise<{
    code: number
    data: {
      cardId: number
      oldVersion: { front: string; back: string }
      newVersion: { front: string; back: string }
    }
    message?: string
  }> {
    return apiClient.get(`${API_V1_PREFIX}/memory/cards/${cardId}/diff`)
  }

  /**
   * 获取用户在指定节点下学习的所有卡片
   */
  static async getUserCardsByNode(nodeId: number): Promise<{
    code: number
    data: MemoryCardView[]
    message?: string
  }> {
    return apiClient.get(`${API_V1_PREFIX}/memory/cards/node/${nodeId}`)
  }

  /**
   * 接受卡片组更新
   */
  static async acceptDeckChanges(deckId: number, cardIds: number[]): Promise<{
    code: number
    data: any
    message?: string
  }> {
    return apiClient.post(`${API_V1_PREFIX}/memory/decks/${deckId}/accept-changes`, cardIds)
  }

  // ========== 点赞相关API ==========

  /**
   * 点赞/取消点赞卡片组
   */
  static async upvoteDeck(deckId: number): Promise<{
    code: number
    data: {
      hasUpvoted: boolean
      upvoteCount: number
    }
    message?: string
  }> {
    return apiClient.post(`${API_V1_PREFIX}/upvotes`, {
      objectId: deckId,
      objectType: ObjectType.MEMORY_CARD_DECK,
      type: VoteType.NORMAL
    })
  }

  /**
   * 获取卡片组点赞状态
   */
  static async getDeckUpvoteStatus(deckId: number): Promise<{
    code: number
    data: {
      hasUpvoted: boolean
      upvoteCount: number
    }
    message?: string
  }> {
    return apiClient.get(`${API_V1_PREFIX}/upvotes/status`, {
      params: {
        objectId: deckId,
        objectType: ObjectType.MEMORY_CARD_DECK
      }
    })
  }
}