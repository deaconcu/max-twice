/**
 * 记忆卡片 API
 */
import client from '../client'
import type { ApiResponse } from '@/types/api'
import type {
  CourseMemoryBank,
  MemoryCardView,
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
