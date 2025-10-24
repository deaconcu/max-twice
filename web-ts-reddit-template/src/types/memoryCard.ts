// 记忆卡片相关类型定义

// 复习结果枚举
export enum ReviewResult {
  FAILED = 0,
  HARD = 1,
  GOOD = 2,
  EASY = 3
}

// 频率设置枚举
export enum FrequencySetting {
  HIGH = 'HIGH',
  NORMAL = 'NORMAL',
  LOW = 'LOW'
}

// 课程学习状态枚举
export enum CourseStudyStatus {
  STUDYING = 'STUDYING',
  PAUSED = 'PAUSED',
  ARCHIVED = 'ARCHIVED'
}

// 卡片组状态枚举
export enum DeckState {
  ACTIVE = 'ACTIVE',
  ARCHIVED = 'ARCHIVED'
}

// 课程基础信息
export interface Course {
  id: number
  name: string
  description?: string
  icon?: string
  iconColor?: string
}

// 卡片组信息
export interface MemoryCardDeck {
  id: number
  title: string
  description?: string
  state: DeckState
  cardCount: number
  sourceUrl?: string
  lastUpdated?: string
  version?: string
}

// 用户卡片 SRS 状态
export interface UserCardSRSState {
  repetitions: number
  intervalDays: number
  easeFactor: number
  reviewDueAt: string
  lastReviewedAt?: string
}

// 记忆卡片视图
export interface MemoryCardView {
  id: number
  front: string
  back: string
  deck?: MemoryCardDeck
  srsState?: UserCardSRSState
  hasDeckUpdate?: boolean
  hasCardUpdate?: boolean
  createdAt?: string
  updatedAt?: string
}

// 复习会话
export interface ReviewSession {
  startTime: string
  endTime?: string
  totalCards: number
  reviewedCards: number
  correctAnswers: number
  results: ReviewCardResult[]
}

// 单张卡片复习结果
export interface ReviewCardResult {
  cardId: number
  result: ReviewResult
  timeSpent: number
}

// 课程记忆库
export interface CourseMemoryBank {
  course: Course
  cardCount: number
  dueCardCount: number
  setting: CourseMemorySetting
}

// 课程记忆设置
export interface CourseMemorySetting {
  frequencySetting: FrequencySetting
  status: CourseStudyStatus
  dailyNewCards: number
  dailyReviewCards: number
}

// 卡片组更新差异
export interface DeckUpdateDiff {
  deckId: number
  deckTitle: string
  hasMetaChanges: boolean
  metaChanges?: {
    oldTitle?: string
    newTitle?: string
    oldDescription?: string
    newDescription?: string
  }
  addedCards: CardDiff[]
  removedCards: CardDiff[]
  modifiedCards: CardDiff[]
}

// 卡片差异
export interface CardDiff {
  cardId: number
  oldFront?: string
  newFront?: string
  oldBack?: string
  newBack?: string
}

// 卡片内容差异
export interface CardContentDiff {
  cardId: number
  deckId: number
  deckTitle: string
  oldFront: string
  newFront: string
  oldBack: string
  newBack: string
  lastUpdated: string
}

// 复习统计
export interface ReviewStats {
  totalReviews: number
  streakDays: number
  averageScore: number
  timeSpent: number
}

// 复习请求参数
export interface ReviewRequest {
  cardId: number
  result: ReviewResult
  timeSpent: number
}

// 复习队列请求参数
export interface ReviewQueueRequest {
  courseId?: number
  limit?: number
}

// 卡片列表请求参数
export interface CardListRequest {
  courseId?: number
  limit?: number
  lastId?: number
}
