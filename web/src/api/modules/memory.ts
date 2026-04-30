/**
 * 记忆卡片 API
 */
import client from '../client'
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
import type { CursorPage } from '@/types/api'

/**
 * 获取复习概览（包含记忆库课程列表和统计数据）
 * @param state 课程状态：1=学习中，2=冻结，3=隐藏
 */
export function getReviewSummary(state?: number): Promise<ReviewSummary> {
  const params = state ? { state } : {}
  return client.get('/memory/memory-bank/courses', { params })
}

/**
 * 根据节点ID获取卡片组列表
 */
export function getDecksByNode(
  nodeId: number,
  params?: {
    cursor?: string
    limit?: number
  }
): Promise<CursorPage<MemoryCardDeck>> {
  return client.get(`/memory/decks/node/${nodeId}`, { params })
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
}): Promise<MemoryCardDeck> {
  return client.post('/memory/decks', data)
}

/**
 * 获取复习队列(到期的卡片)
 */
export function getReviewQueue(params: GetReviewQueueParams): Promise<MemoryCardView[]> {
  return client.get('/memory/review/queue', { params })
}

/**
 * 获取卡片列表(所有卡片)
 */
export function getCardList(params: GetCardListParams): Promise<CursorPage<MemoryCardView>> {
  return client.get('/memory/review/cards', { params })
}

/**
 * 获取下一张待复习卡片
 */
export function getNextCard(params?: { courseId?: number }): Promise<ReviewSubmitResult> {
  return client.get('/memory/review/next', { params })
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
}): Promise<ReviewSubmitResult> {
  return client.post('/memory/review/submit', params)
}

/**
 * 获取卡片组更新差异
 */
export function getDeckDiff(deckId: number): Promise<DeckUpdateDiff> {
  return client.get(`/memory/decks/${String(deckId)}/diff`)
}

/**
 * 获取卡片内容差异
 */
export function getCardDiff(cardId: number): Promise<CardContentDiff> {
  return client.get(`/memory/cards/${String(cardId)}/diff`)
}

/**
 * 接受卡片组更新
 */
export function acceptDeckChanges(
  deckId: number,
  cardIds: number[],
  courseId?: number,
  removeOtherDeckCards?: boolean
): Promise<void> {
  return client.post(`/memory/decks/${String(deckId)}/accept-changes`, {
    cardIds,
    courseId,
    removeOtherDeckCards,
  })
}

/**
 * 删除卡片（从复习计划中移除）
 */
export function deleteCards(cardIds: number[]): Promise<void> {
  return client.delete('/memory/cards/study', { data: cardIds })
}

/**
 * 重置卡片学习进度
 */
export function resetCardProgress(cardIds: number[]): Promise<void> {
  return client.post('/memory/cards/reset', { cardIds })
}

/**
 * 更新课程记忆设置
 */
export function updateCourseMemorySetting(params: {
  courseId: number
  status?: number
  frequencySetting?: number
  cardOrder?: number
  dailyNewLimit?: number
  dailyReviewLimit?: number
}): Promise<void> {
  return client.put(`/memory/memory-bank/courses/${String(params.courseId)}/settings`, params)
}

/**
 * 获取帖子下的公共卡片组列表
 */
export function getPostPublicDecks(
  postId: number,
  params?: {
    sortBy?: string
    sortOrder?: string
    cursor?: string
    limit?: number
  }
): Promise<{
  items: MemoryCardDeck[]
  hasMore: boolean
  nextCursor?: string
}> {
  return client.get(`/memory/posts/${postId}/decks`, { params })
}

/**
 * 获取帖子创建者提交的卡片组
 */
export function getPostCreatorDeck(
  postId: number,
  params?: {
    sortBy?: string
    sortOrder?: string
    cursor?: string
    limit?: number
  }
): Promise<{
  items: MemoryCardDeck[]
  hasMore: boolean
  nextCursor?: string
}> {
  return client.get(`/memory/posts/${postId}/creator-deck`, { params })
}

/**
 * 获取用户自己在指定帖子下提交的卡片组
 */
export function getMyPostDeck(
  postId: number,
  params?: {
    sortBy?: string
    sortOrder?: string
    cursor?: string
    limit?: number
  }
): Promise<{
  items: MemoryCardDeck[]
  hasMore: boolean
  nextCursor?: string
}> {
  return client.get(`/memory/posts/${postId}/my-deck`, { params })
}

/**
 * 点赞/取消点赞卡片组
 */
export function upvoteDeck(deckId: number): Promise<{
  liked: boolean
  likeCount: number
}> {
  return client.post('/upvotes', {
    objectId: deckId,
    objectType: 5, // MEMORY_CARD_DECK
    type: 2, // LIKE
  })
}

/**
 * 获取卡片组详情
 */
export function getDeckDetail(deckId: number): Promise<MemoryCardDeck> {
  return client.get(`/memory/decks/${deckId}`)
}

/**
 * 获取用户在指定节点下学习的所有卡片
 */
export function getUserCardsByNode(nodeId: number): Promise<MemoryCardView[]> {
  return client.get(`/memory/cards/node/${nodeId}`)
}

/**
 * 添加卡片到学习
 */
export function addCardToStudy(cardId: number): Promise<MemoryCardView> {
  return client.post(`/memory/cards/${cardId}/study`)
}

/**
 * 移除卡片学习记录（从复习计划中全局移除）
 */
export function removeCardsFromStudy(cardIds: number[]): Promise<void> {
  return client.delete('/memory/cards/study', { data: cardIds })
}

/**
 * 删除卡片
 */
export function deleteCard(cardId: number): Promise<void> {
  return client.delete(`/memory/cards/${cardId}`)
}

/**
 * 更新卡片
 */
export function updateCard(
  cardId: number,
  data: { front: string; back: string }
): Promise<MemoryCardView> {
  return client.put(`/memory/cards/${cardId}`, data)
}

/**
 * 创建卡片
 */
export function createCard(data: {
  deckId: number
  front: string
  back: string
}): Promise<MemoryCardView> {
  return client.post('/memory/cards', data)
}

/**
 * 管理员：批准卡片组
 */
export function approveDeck(deckId: number): Promise<void> {
  return client.post(`/admin/memory/decks/${deckId}/approve`)
}

/**
 * 管理员：拒绝卡片组
 */
export function rejectDeck(deckId: number, reason?: string): Promise<void> {
  return client.post(`/admin/memory/decks/${deckId}/reject`, reason ? { reason } : undefined)
}

/**
 * 管理员：屏蔽卡片组
 */
export function banDeck(deckId: number, reason?: string): Promise<void> {
  return client.post(`/admin/memory/decks/${deckId}/ban`, reason ? { reason } : undefined)
}

/**
 * 管理员：恢复卡片组
 */
export function restoreDeck(deckId: number): Promise<void> {
  return client.post(`/admin/memory/decks/${deckId}/restore`)
}

/**
 * 获取当前用户的所有卡片组
 */
export function getCurrentUserDecks(params?: {
  cursor?: string
  limit?: number
  state?: number
}): Promise<CursorPage<MemoryCardDeck>> {
  return client.get('/memory/users/me/memory-decks', { params })
}

/**
 * 获取指定用户的卡片组列表
 */
export function getUserDecks(
  userId: number,
  params?: {
    cursor?: string
    limit?: number
  }
): Promise<CursorPage<MemoryCardDeck>> {
  return client.get(`/memory/users/${userId}/memory-decks`, { params })
}

/**
 * 删除卡片组
 */
export function deleteDeck(deckId: number): Promise<void> {
  return client.delete(`/memory/decks/${deckId}`)
}

/**
 * 添加卡片组到记忆库
 */
export function addDeckToMemoryBank(request: { deckId: number; courseId: number }): Promise<void> {
  return client.post('/memory/memory-bank/decks', request)
}

/**
 * 移动节点到课程
 * 将用户在指定节点下学习的所有卡片移动到指定课程
 */
export function moveNodeToCourse(nodeId: number, courseId: number): Promise<void> {
  return client.post(`/memory/nodes/${nodeId}/move-to-course`, null, {
    params: { courseId },
  })
}
