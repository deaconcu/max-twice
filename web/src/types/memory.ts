/**
 * 记忆卡片相关类型定义
 */

/**
 * 复习结果枚举
 */
export enum ReviewResult {
  AGAIN = 1, // 重来
  HARD = 2, // 困难
  GOOD = 3, // 良好
  EASY = 4, // 简单
}

/**
 * 复习频率设置
 */
export enum FrequencySetting {
  HIGH = 1, // 高频
  NORMAL = 2, // 普通
  LOW = 3, // 低频
}

/**
 * 课程学习状态
 */
export enum CourseStudyStatus {
  STUDYING = 1, // 学习中
  PAUSED = 2, // 已暂停
  ARCHIVED = 3, // 已归档
}

/**
 * 卡片组状态（与后端 ContentState 对应）
 */
export const DeckState = {
  SUBMITTED: 1, // 已提交/审核中
  PUBLISHED: 2, // 已发布/正常
  REJECTED: 3, // 已拒绝
  BANNED: 4, // 已封禁/屏蔽
} as const

export type DeckState = (typeof DeckState)[keyof typeof DeckState]

/**
 * 卡片状态类型
 */
export enum CardType {
  NEW = 0, // 新卡片
  LEARNING = 1, // 学习中
  REVIEW = 2, // 复习
  RELEARNING = 3, // 重新学习
}

/**
 * SRS 状态
 */
export interface UserCardSRSState {
  type?: CardType // 卡片状态
  currentStep?: number // 当前学习/重学步骤索引
  interval?: number // REVIEW阶段的复习间隔（天）
  reappearAt?: number // LEARNING/RELEARNING的下次再现计数
  repetitions: number // 重复次数
  intervalDays: number // 间隔天数（兼容旧字段）
  easeFactor: number // 简易因子
  reviewDueAt: string // 下次复习时间
  lastReviewAt?: string // 上次复习时间
  lapseCount?: number // 遗忘次数
  course?: {
    // 所属课程
    id: number
    name: string
    icon?: string
  }
}

/**
 * 记忆卡片视图
 */
export interface MemoryCardView {
  id: number
  front: string // 问题
  back: string // 答案
  state?: DeckState // 卡片状态（与 DeckState 共用枚举）
  deck?: MemoryCardDeck // 所属卡片组信息
  srsState?: UserCardSRSState // SRS 状态
  hasDeckUpdate?: boolean // 卡片组是否有更新
  hasCardUpdate?: boolean // 卡片内容是否有更新
  bookmarked?: boolean // 是否已收藏
}

/**
 * 记忆卡片组
 */
export interface MemoryCardDeck {
  id: number
  description?: string // 描述
  postId?: number // 来源帖子ID
  nodeId?: number // 所属节点ID
  courseId?: number // 所属课程ID
  nodeName?: string // 节点名称
  courseName?: string // 课程名称
  cardCount?: number // 卡片数量
  state?: DeckState // 状态：1=审核中, 2=已通过, 3=已拒绝, 4=已屏蔽
  likeCount?: number // 点赞数
  hasLiked?: boolean // 当前用户是否已点赞
  bookmarked?: boolean // 是否已收藏
  creatorId?: number // 创建者ID
  creator?: {
    id: number
    name: string
    avatar?: string
  } // 创建者信息
  course?: {
    id: number
    name: string
  } // 课程信息
  node?: {
    id: number
    name: string
  } // 节点信息
  firstCardQuestion?: string // 第一张卡片的问题
  createdAt?: string
  updatedAt?: string
}

/**
 * 卡片组统计信息
 */
export interface DeckStats {
  totalCardCount: number
  newCardCount: number
  reviewCardCount: number
  learnedCardCount: number
}

/**
 * 卡片组详情（包含卡片列表）
 */
export interface DeckDetail extends MemoryCardDeck {
  cards: MemoryCardView[]
  stats: DeckStats
}

/**
 * 卡片顺序枚举
 */
export enum CardOrder {
  REVIEW_FIRST = 0, // 先复习后新卡
  NEW_FIRST = 1, // 先新卡后复习
}

/**
 * 课程记忆库
 */
export interface CourseMemoryBank {
  course: {
    id: number
    name: string
    icon?: string
  }
  cardCount: number // 总卡片数
  dueCardCount: number // 到期卡片数
  setting: CourseMemorySetting
}

/**
 * 课程记忆设置
 */
export interface CourseMemorySetting {
  courseId: number
  state: CourseStudyStatus // 学习状态
  frequencySetting: FrequencySetting // 复习频率
  cardOrder: CardOrder // 卡片顺序
}

/**
 * 复习会话
 */
export interface ReviewSession {
  startTime: string
  endTime?: string
  totalCards: number
  reviewedCards: number
  correctAnswers: number
  results: ReviewCardResult[]
}

/**
 * 单张卡片复习结果
 */
export interface ReviewCardResult {
  cardId: number
  result: ReviewResult
  timeSpent: number // 秒
}

/**
 * 卡片组更新差异
 */
export interface DeckUpdateDiff {
  deckId: number
  hasMetaUpdate: boolean // 是否有元信息更新
  addedCards: MemoryCardView[] // 新增卡片
  removedCards: MemoryCardView[] // 删除的卡片
  updatedCards: MemoryCardView[] // 更新的卡片
}

/**
 * 卡片内容差异
 */
export interface CardContentDiff {
  cardId: number
  oldFront: string
  newFront: string
  oldBack: string
  newBack: string
}

/**
 * 获取复习队列参数
 */
export interface GetReviewQueueParams {
  courseId?: number
}

/**
 * 获取卡片列表参数
 */
export interface GetCardListParams {
  courseId?: number
  limit?: number
  lastId?: number
}

/**
 * 复习统计数据
 */
export interface ReviewStats {
  totalReviewCount: number // 总复习次数
  streakDays: number // 连续天数
  averageScore: number // 平均正确率
  timeSpent: number // 总学习时长(分钟)
}

/**
 * 复习提交结果
 */
export interface ReviewSubmitResult {
  nextCard: MemoryCardView | null // 下一张待复习卡片
}

/**
 * 复习概览
 */
export interface ReviewSummary {
  todayTotal: number // 今日待复习总数
  todayCompleted: number // 今日已复习数
  streakDays: number // 连续复习天数
  courses: CourseMemoryBank[] // 课程列表
}
