/**
 * 记忆卡片API服务
 * 连接后端真实接口
 */

import apiClient from './api/v1/apiServiceV1'
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
   * 获取卡片组列表
   */
  static async getDecks(query: GetDecksQuery = {}): Promise<{
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

    return apiClient.get(`${API_V1_PREFIX}/memory/decks?${params.toString()}`)
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
}