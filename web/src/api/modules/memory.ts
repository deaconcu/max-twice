/**
 * 记忆卡片 API
 */
import client from '../client'
import type { ApiResponse } from '@/types/api'
import type {
  MemoryCardView,
  MemoryCardDeck,
  GetReviewQueueParams,
  GetCardListParams,
  ReviewResult,
  DeckUpdateDiff,
  CardContentDiff,
  ReviewSubmitResult,
  ReviewSummary,
} from '@/types/memory'

/**
 * 获取复习概览（包含记忆库课程列表和统计数据）
 */
export function getReviewSummary(): Promise<ApiResponse<ReviewSummary>> {
  return client.get('/v1/memory/memory-bank/courses')
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
  sourcePostId?: number
  nodeId?: number
  description?: string
  cards: {
    front: string
    back: string
  }[]
}): Promise<ApiResponse<MemoryCardDeck>> {
  return client.post('/v1/memory/decks', data)
}

/**
 * 获取复习队列(到期的卡片)
 */
export function getReviewQueue(
  params: GetReviewQueueParams
): Promise<ApiResponse<MemoryCardView[]>> {
  return client.get('/v1/memory/review/queue', { params })
}

/**
 * 获取卡片列表(所有卡片)
 */
export function getCardList(params: GetCardListParams): Promise<ApiResponse<MemoryCardView[]>> {
  return client.get('/v1/memory/review/cards', { params })
}

/**
 * 获取下一张待复习卡片
 */
export function getNextCard(params?: {
  courseId?: number
}): Promise<ApiResponse<ReviewSubmitResult>> {
  return client.get('/v1/memory/review/next', { params })
}

/**
 * 提交卡片复习结果
 * @returns 下一张卡片
 */
export function reviewCard(params: {
  cardId: number
  result: ReviewResult
  courseId?: number
  timeSpent?: number
}): Promise<ApiResponse<ReviewSubmitResult>> {
  return client.post('/v1/memory/review/submit', params)
}

/**
 * 获取卡片组更新差异
 */
export function getDeckDiff(deckId: number): Promise<ApiResponse<DeckUpdateDiff>> {
  return client.get(`/v1/memory/decks/${String(deckId)}/diff`)
}

/**
 * 获取卡片内容差异
 */
export function getCardDiff(cardId: number): Promise<ApiResponse<CardContentDiff>> {
  return client.get(`/v1/memory/cards/${String(cardId)}/diff`)
}

/**
 * 接受卡片组更新
 */
export function acceptDeckChanges(
  deckId: number,
  cardIds: number[],
  courseId?: number,
  removeOtherDeckCards?: boolean
): Promise<ApiResponse<void>> {
  return client.post(`/v1/memory/decks/${String(deckId)}/accept-changes`, {
    cardIds,
    courseId,
    removeOtherDeckCards,
  })
}

/**
 * 删除卡片
 */
export function deleteCards(cardIds: number[]): Promise<ApiResponse<void>> {
  return client.post('/v1/memory/cards/delete', { cardIds })
}

/**
 * 重置卡片学习进度
 */
export function resetCardProgress(cardIds: number[]): Promise<ApiResponse<void>> {
  return client.post('/v1/memory/cards/reset', { cardIds })
}

/**
 * 更新课程记忆设置
 */
export function updateCourseMemorySetting(params: {
  courseId: number
  status?: string
  frequencySetting?: string
  cardOrder?: number
}): Promise<ApiResponse<void>> {
  return client.put(`/v1/memory/memory-bank/courses/${String(params.courseId)}/settings`, params)
}

/**
 * 移除课程记忆库
 */
export function removeCourseMemoryBank(courseId: number): Promise<ApiResponse<void>> {
  return client.delete(`/v1/memory/memory-bank/courses/${String(courseId)}`)
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
    liked: boolean
    likeCount: number
  }>
> {
  return client.post('/v1/upvotes', {
    objectId: deckId,
    objectType: 5, // MEMORY_CARD_DECK
    type: 2, // LIKE
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
 * 移除卡片学习记录（从复习计划中全局移除）
 */
export function removeCardsFromStudy(cardIds: number[]): Promise<ApiResponse<void>> {
  return client.delete('/v1/memory/cards/study', { data: cardIds })
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
  data: { front: string; back: string }
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
 * 获取当前用户的所有卡片组
 */
export function getCurrentUserDecks(params?: {
  lastId?: number
  limit?: number
}): Promise<ApiResponse<{ items: MemoryCardDeck[]; hasMore: boolean }>> {
  return client.get('/v1/memory/users/me/memory-decks', { params })
}

/**
 * 获取指定用户的卡片组列表
 */
export function getUserDecks(
  userId: number,
  params?: {
    lastId?: number
    limit?: number
  }
): Promise<ApiResponse<{ items: MemoryCardDeck[]; hasMore: boolean }>> {
  return client.get(`/v1/memory/users/${userId}/memory-decks`, { params })
}

/**
 * 删除卡片组
 */
export function deleteDeck(deckId: number): Promise<ApiResponse<void>> {
  return client.delete(`/v1/memory/decks/${deckId}`)
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

/**
 * 移动节点到课程
 * 将用户在指定节点下学习的所有卡片移动到指定课程
 */
export function moveNodeToCourse(nodeId: number, courseId: number): Promise<ApiResponse<void>> {
  return client.post(`/v1/memory/nodes/${nodeId}/move-to-course`, null, {
    params: { courseId },
  })
}
