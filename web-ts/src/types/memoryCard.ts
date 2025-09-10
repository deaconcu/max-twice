/**
 * 记忆卡片相关的类型定义
 * 基于SRS设计文档V8.0的数据模型
 */

import type { User } from './user'
import type { Post } from './post'
import type { Course } from './course'

// 卡片组状态枚举
export enum DeckState {
  PENDING = 0,    // 审核中
  NORMAL = 1,     // 正常
  LOCKED = 2,     // 锁定
  PRIVATE = 3,    // 私有
  DELETED = 4     // 已删除
}

// 卡片状态枚举
export enum CardState {
  NORMAL = 0,     // 正常
  DELETED = 1     // 已被创建者删除
}

// 复习结果枚举 (调整为4个级别)
export enum ReviewResult {
  FAILED = 0,     // 忘记了
  HARD = 1,       // 困难
  GOOD = 2,       // 良好  
  EASY = 3        // 简单
}

// 课程学习状态枚举
export enum CourseStudyStatus {
  STUDYING = 1,   // 学习中
  PAUSED = 2,     // 已暂停
  ARCHIVED = 3    // 已归档
}

// 复习频率设置枚举
export enum FrequencySetting {
  HIGH = 0,       // 高频
  NORMAL = 1,     // 普通
  LOW = 2         // 低频
}

// 记忆卡片组
export interface MemoryCardDeck {
  id: number
  sourcePostId: number
  sourcePost?: Post
  creatorId: number
  creator?: User
  title: string
  description?: string
  version: number // 卡片组版本号
  state: DeckState
  
  // 审计字段
  auditorId?: number
  auditor?: User
  auditedAt?: string
  updatedBy?: number
  updatedAt: string
  
  // 社区互动字段
  upvoteCount: number
  cardCount: number
  score: number
  
  createdAt: string
  
  // 更新检测相关字段
  hasUpdate?: boolean // 是否有可用更新
  currentVersion?: number // 用户当前版本
  latestVersion?: number // 最新版本
}

// 记忆卡片主表
export interface MemoryCard {
  id: number
  deckId: number
  deck?: MemoryCardDeck
  creatorId: number
  creator?: User
  currentVersionId: number
  state: CardState
  createdAt: string
  updatedAt: string
}

// 记忆卡片版本表
export interface MemoryCardVersion {
  id: number
  cardId: number
  version: number
  creatorId: number
  creator?: User
  front: string      // 卡片正面（问题）
  back: string       // 卡片背面（答案）
  contentHash?: string
  isActive: boolean
  createdAt: string
}

// 用户卡片全局记忆状态 (V8.0)
export interface UserCardSRSState {
  id: number
  userId: number
  cardId: number
  deckVersion: number       // 学习时卡片组的版本快照
  cardVersionId: number     // 用户学习时锁定的版本ID，实现内容快照
  
  // 核心SRS字段 (基于SM-2算法)
  reviewDueAt: string       // 预计算好的、全局唯一的下次复习时间
  lastReviewedAt?: string   // 上次复习时间
  intervalDays: number      // 当前复习间隔（天）
  easeFactor: number        // 缓急因子
  repetitions: number       // 连续正确次数
  
  // 分析字段
  lapseCount: number        // 遗忘总次数
  
  createdAt: string
  updatedAt: string
}

// 用户课程复习策略
export interface UserCourseSRSSetting {
  id: number
  userId: number
  courseId: number
  
  // 课程级策略与状态
  frequencySetting: FrequencySetting  // 复习频率
  status: CourseStudyStatus           // 学习状态
  
  createdAt: string
  updatedAt: string
}

// 用户卡片课程归属
export interface UserCardInCourse {
  id: number
  userId: number
  cardId: number
  courseId: number
  createdAt: string
}

// 完整的卡片信息（包含当前版本内容和上下文）
export interface MemoryCardWithVersion extends MemoryCard {
  currentVersion: MemoryCardVersion
  userCard?: UserCardSRSState        // 用户的全局SRS状态
  courses?: Course[]                 // 卡片所属的课程列表
  primaryCourse?: Course             // 主要课程（最优策略）
}

// 添加卡片组到记忆库请求
export interface AddDeckToMemoryBankRequest {
  deckId: number
  courseId: number
}

// 更新课程复习策略请求
export interface UpdateCourseSettingRequest {
  courseId: number
  frequencySetting?: FrequencySetting
  status?: CourseStudyStatus
}

// 移除卡片组请求
export interface RemoveDeckFromCourseRequest {
  deckId: number
  courseId: number
}

// 创建卡片组请求
export interface CreateDeckRequest {
  sourcePostId: number
  title: string
  description?: string
}

// 更新卡片组请求
export interface UpdateDeckRequest {
  id: number
  title?: string
  description?: string
}

// 创建卡片请求
export interface CreateCardRequest {
  deckId: number
  front: string
  back: string
}

// 更新卡片请求
export interface UpdateCardRequest {
  id: number
  front: string
  back: string
}

// 复习卡片请求
export interface ReviewCardRequest {
  cardId: number
  result: ReviewResult
  timeSpent?: number // 复习时间（秒）
}

// 获取卡片组列表的查询参数
export interface GetDecksQuery {
  postId?: number
  creatorId?: number
  state?: DeckState
  sortBy?: 'score' | 'createdAt' | 'upvoteCount'
  sortOrder?: 'asc' | 'desc'
  page?: number
  size?: number
}

// 获取复习队列的查询参数 (V8.0)
export interface GetReviewQueueQuery {
  dueOnly?: boolean     // 只获取到期的卡片
  courseId?: number     // 筛选特定课程的卡片
  limit?: number
}

// 记忆库管理查询参数
export interface GetMemoryBankQuery {
  userId: number
  courseId?: number     // 筛选特定课程
  status?: CourseStudyStatus  // 筛选课程状态
}

// 课程记忆库信息
export interface CourseMemoryBank {
  course: Course
  setting: UserCourseSRSSetting
  cardCount: number
  dueCardCount: number
  newCardCount: number
  reviewCardCount: number
  learnedCardCount: number
}

// 卡片组统计信息
export interface DeckStats {
  totalCards: number
  newCards: number
  reviewCards: number
  learnedCards: number
}

// 卡片diff类型枚举
export enum CardDiffType {
  ADDED = 'added',     // 新增卡片
  MODIFIED = 'modified', // 修改卡片
  DELETED = 'deleted'   // 删除卡片
}

// 单个卡片的diff信息
export interface CardDiff {
  cardId?: number
  type: CardDiffType
  oldVersion?: {
    front: string
    back: string
  }
  newVersion?: {
    front: string
    back: string
  }
}

// 卡片组更新对比信息
export interface DeckUpdateDiff {
  deckId: number
  oldVersion: number
  newVersion: number
  title?: {
    old: string
    new: string
  }
  description?: {
    old: string
    new: string
  }
  cardDiffs: CardDiff[]
  summary: {
    addedCount: number
    modifiedCount: number
    deletedCount: number
  }
}

// 更新卡片组请求
export interface UpdateDeckRequest {
  deckId: number
  acceptedChanges: {
    updateMeta?: boolean // 是否更新标题和描述
    cardIds: number[]    // 接受更新的卡片ID列表
  }
}

// 复习统计信息
export interface ReviewStats {
  totalReviews: number
  streakDays: number
  averageScore: number
  timeSpent: number  // 分钟
}

// 卡片组详情（包含卡片列表）
export interface DeckDetail extends MemoryCardDeck {
  cards: MemoryCardWithVersion[]
  stats: DeckStats
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

// 单次复习结果
export interface ReviewCardResult {
  cardId: number
  result: ReviewResult
  timeSpent: number // 秒
}