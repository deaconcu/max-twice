/**
 * 记忆卡片 API
 */
import client from '../client'
import type { ApiResponse } from '@/types/api'
import type {
  CourseMemoryBank,
  MemoryCardView,
  MemoryCardDeck,
  GetReviewQueueParams,
  GetCardListParams,
  ReviewResult,
  ReviewStats,
  DeckUpdateDiff,
  CardContentDiff,
} from '@/types/memory'

/**
 * 获取记忆库课程列表
 */
export function getMemoryBankCourses(): Promise<ApiResponse<CourseMemoryBank[]>> {
  return client.get('/api/memory/courses')
}

/**
 * 根据节点ID获取卡片组列表
 */
export function getDecksByNode(
  nodeId: number,
  params?: {
    lastScore?: number
    lastId?: number
    limit?: number
  }
): Promise<ApiResponse<{ items: MemoryCardDeck[]; hasMore: boolean }>> {
  return client.get(`/v1/memory/decks/node/${nodeId}`, { params })
}

/**
 * 创建卡片组
 */
export function createDeck(data: {
  sourcePostId: number
  title: string
  description?: string
  cards: Array<{
    front: string
    back: string
  }>
}): Promise<ApiResponse<MemoryCardDeck>> {
  return client.post('/v1/memory/decks', data)
}

/**
 * 获取复习队列(到期的卡片)
 */
export function getReviewQueue(
  params: GetReviewQueueParams
): Promise<ApiResponse<MemoryCardView[]>> {
  return client.get('/api/memory/review-queue', { params })
}

/**
 * 获取卡片列表(所有卡片)
 */
export function getCardList(params: GetCardListParams): Promise<ApiResponse<MemoryCardView[]>> {
  return client.get('/api/memory/cards', { params })
}

/**
 * 提交卡片复习结果
 */
export function reviewCard(params: {
  cardId: number
  result: ReviewResult
  timeSpent: number
}): Promise<ApiResponse<void>> {
  return client.post('/api/memory/review', params)
}

/**
 * 获取复习统计
 */
export function getReviewStats(): Promise<ApiResponse<ReviewStats>> {
  return client.get('/api/memory/stats')
}

/**
 * 获取卡片组更新差异
 */
export function getDeckDiff(deckId: number): Promise<ApiResponse<DeckUpdateDiff>> {
  return client.get(`/api/memory/decks/${String(deckId)}/diff`)
}

/**
 * 获取卡片内容差异
 */
export function getCardDiff(cardId: number): Promise<ApiResponse<CardContentDiff>> {
  return client.get(`/api/memory/cards/${String(cardId)}/diff`)
}

/**
 * 接受卡片组更新
 */
export function acceptDeckChanges(deckId: number, cardIds: number[]): Promise<ApiResponse<void>> {
  return client.post(`/api/memory/decks/${String(deckId)}/accept`, { cardIds })
}

/**
 * 删除卡片
 */
export function deleteCards(cardIds: number[]): Promise<ApiResponse<void>> {
  return client.post('/api/memory/cards/delete', { cardIds })
}

/**
 * 重置卡片学习进度
 */
export function resetCardProgress(cardIds: number[]): Promise<ApiResponse<void>> {
  return client.post('/api/memory/cards/reset', { cardIds })
}

/**
 * 更新课程记忆设置
 */
export function updateCourseMemorySetting(params: {
  courseId: number
  status?: string
  frequencySetting?: string
}): Promise<ApiResponse<void>> {
  return client.put(`/api/memory/courses/${String(params.courseId)}/settings`, params)
}

/**
 * 移除课程记忆库
 */
export function removeCourseMemoryBank(courseId: number): Promise<ApiResponse<void>> {
  return client.delete(`/api/memory/courses/${String(courseId)}`)
}

/**
 * 获取帖子下的公共卡片组列表
 */
export function getPostPublicDecks(
  postId: number,
  params?: {
    sortBy?: string
    sortOrder?: string
    lastScore?: number
    lastId?: number
    limit?: number
  }
): Promise<
  ApiResponse<{
    items: MemoryCardDeck[]
    hasMore: boolean
    nextCursor?: {
      lastScore?: number
      lastId?: number
    }
  }>
> {
  return client.get(`/v1/memory/posts/${postId}/decks`, { params })
}

/**
 * 获取帖子创建者提交的卡片组
 */
export function getPostCreatorDeck(
  postId: number,
  params?: {
    sortBy?: string
    sortOrder?: string
    lastScore?: number
    lastId?: number
    limit?: number
  }
): Promise<
  ApiResponse<{
    items: MemoryCardDeck[]
    hasMore: boolean
    nextCursor?: {
      lastScore?: number
      lastId?: number
    }
  }>
> {
  return client.get(`/v1/memory/posts/${postId}/creator-deck`, { params })
}

/**
 * 获取用户自己在指定帖子下提交的卡片组
 */
export function getMyPostDeck(
  postId: number,
  params?: {
    sortBy?: string
    sortOrder?: string
    lastScore?: number
    lastId?: number
    limit?: number
  }
): Promise<
  ApiResponse<{
    items: MemoryCardDeck[]
    hasMore: boolean
    nextCursor?: {
      lastScore?: number
      lastId?: number
    }
  }>
> {
  return client.get(`/v1/memory/posts/${postId}/my-deck`, { params })
}

/**
 * 点赞/取消点赞卡片组
 */
export function upvoteDeck(deckId: number): Promise<
  ApiResponse<{
    upvoted: boolean
    upvotes: number
  }>
> {
  return client.post('/v1/upvotes', {
    objectId: deckId,
    objectType: 'MEMORY_CARD_DECK',
    type: 'NORMAL',
  })
}

/**
 * 获取卡片组详情
 */
export function getDeckDetail(deckId: number): Promise<ApiResponse<any>> {
  return client.get(`/v1/memory/decks/${deckId}`)
}

/**
 * 获取用户在指定节点下学习的所有卡片
 */
export function getUserCardsByNode(nodeId: number): Promise<ApiResponse<any[]>> {
  return client.get(`/v1/memory/cards/node/${nodeId}`)
}

/**
 * 添加卡片到学习
 */
export function addCardToStudy(cardId: number): Promise<ApiResponse<any>> {
  return client.post(`/v1/memory/cards/${cardId}/study`)
}

/**
 * 删除卡片
 */
export function deleteCard(cardId: number): Promise<ApiResponse<void>> {
  return client.delete(`/v1/memory/cards/${cardId}`)
}

/**
 * 更新卡片
 */
export function updateCard(
  cardId: number,
  data: { id: number; front: string; back: string }
): Promise<ApiResponse<any>> {
  return client.put(`/v1/memory/cards/${cardId}`, data)
}

/**
 * 创建卡片
 */
export function createCard(data: {
  deckId: number
  front: string
  back: string
}): Promise<ApiResponse<any>> {
  return client.post('/v1/memory/cards', data)
}

/**
 * 管理员：批准卡片组
 */
export function approveDeck(deckId: number): Promise<ApiResponse<void>> {
  return client.post(`/v1/admin/memory/decks/${deckId}/approve`)
}

/**
 * 管理员：拒绝卡片组
 */
export function rejectDeck(deckId: number, reason?: string): Promise<ApiResponse<void>> {
  return client.post(`/v1/admin/memory/decks/${deckId}/reject`, reason ? { reason } : undefined)
}

/**
 * 管理员：屏蔽卡片组
 */
export function banDeck(deckId: number, reason?: string): Promise<ApiResponse<void>> {
  return client.post(`/v1/admin/memory/decks/${deckId}/ban`, reason ? { reason } : undefined)
}

/**
 * 管理员：恢复卡片组
 */
export function restoreDeck(deckId: number): Promise<ApiResponse<void>> {
  return client.post(`/v1/admin/memory/decks/${deckId}/restore`)
}

/**
 * 添加卡片组到记忆库
 */
export function addDeckToMemoryBank(request: {
  deckId: number
  courseId: number
}): Promise<ApiResponse<any>> {
  return client.post('/v1/memory/memory-bank/decks', request)
}
