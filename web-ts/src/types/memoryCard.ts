/**
 * 记忆卡片相关的类型定义
 * 基于SRS设计文档V8.0的数据模型
 */

import type { User } from './user'
import type { Course } from './course'
import type { PageSortQuery, PageQuery, LimitQuery } from './common'

// 卡片组状态枚举
export const DeckState = {
  PENDING: 0,    // 审核中
  NORMAL: 1,     // 正常
  LOCKED: 2,     // 锁定，锁定后不可编辑，用来比如内容有争议，等待管理员处理
  PRIVATE: 3,    // 私有
  DELETED: 4     // 已删除
} as const

export type DeckState = typeof DeckState[keyof typeof DeckState]

// 卡片状态枚举
export const CardState = {
  NORMAL: 0,     // 正常
  DELETED: 1     // 已被创建者删除
} as const

export type CardState = typeof CardState[keyof typeof CardState]

// 复习结果枚举 (调整为4个级别)
export const ReviewResult = {
  FAILED: 0,     // 忘记了
  HARD: 1,       // 困难
  GOOD: 2,       // 良好  
  EASY: 3        // 简单
} as const

export type ReviewResult = typeof ReviewResult[keyof typeof ReviewResult]

// 课程学习状态枚举
export const CourseStudyStatus = {
  STUDYING: 1,   // 学习中
  PAUSED: 2,     // 已暂停   恢复的时候需要重新计算记忆卡的due_date，将其加上暂停的时间差
  ARCHIVED: 3    // 已归档   归档后不再出现在左侧栏，其它和暂停一致
} as const

export type CourseStudyStatus = typeof CourseStudyStatus[keyof typeof CourseStudyStatus]

// 复习频率设置枚举
export const FrequencySetting = {     // 就是在计算due_date的时候，乘以的一个系数，高频就是0.6，低频就是1.5
  HIGH: 0,       // 高频
  NORMAL: 1,     // 普通
  LOW: 2         // 低频
} as const

export type FrequencySetting = typeof FrequencySetting[keyof typeof FrequencySetting]

// 记忆卡片组
export interface MemoryCardDeck {
  id: number
  sourcePostId?: number              // 卡片组的来源帖子 (可选)，比如在卡片组list中，可以跳转到对应的post
  creator?: User                     // 创建者 (可选)
  title: string                      // 卡片组标题，一般叫xxx的记忆卡片组
  description?: string               // 卡片组描述 (可选)
  //version: number                    // delete 卡片组版本号, 每次内容更新时递增 todo 需要删除，前端不需要这个值展示, 后端会自动维护
  state: DeckState                   // 卡片组状态
  
  // 审计字段
  //auditorId?: number        // delete
  //auditor?: User            // delete
  //auditedAt?: string        // delete
  //updatedBy?: number        // delete
  updatedAt: string         // 最近更新时间
  createdAt: string         // 创建时间
  
  // 社区互动字段
  upvoteCount: number       // 点赞数
  cardCount: number         // 卡片数
  //score: number             // delete 综合得分（热度）
}

export interface MemoryCardView {
  // --- 核心字段 (必须) ---
  id: number;
  front: string;
  back: string;

  // --- 关联上下文 (强烈建议) ---
  deck?: MemoryCardDeck;  // 所属卡片组
  creator?: User;         // 创建者

  // --- 用户相关的学习状态 (在复习场景下必须) ---
  srsState?: UserCardSRSState;
}

// 记忆卡片主表 delete
/*
export interface MemoryCard {
  id: number
  deckId: number            // delete
  deck?: MemoryCardDeck     // 卡片所属的卡片组 (可选)
  creatorId: number         // delete
  creator?: User            // 创建者 (可选)
  currentVersionId: number
  state: CardState
  createdAt: string
  updatedAt: string
}
*/

// 记忆卡片版本表 delete
/*
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
*/

// 用户卡片全局记忆状态 (V8.0)
export interface UserCardSRSState {
  id: number
  //userId: number            // delete
  //cardId: number            // delete
  //deckVersion: number       // delete 学习时卡片组的版本快照
  //cardVersionId: number     // delete 用户学习时锁定的版本ID，实现内容快照
  
  // 核心SRS字段 (基于SM-2算法)
  reviewDueAt: string       // 预计算好的、全局唯一的下次复习时间
  lastReviewedAt?: string   // 上次复习时间
  intervalDays: number      // 当前复习间隔（天）
  //easeFactor: number        // delete 缓急因子
  repetitions: number       // 连续正确次数
  
  // 分析字段
  lapseCount: number        // 遗忘总次数
  
  //createdAt: string         // delete
  //updatedAt: string         // delete
}

// 用户课程复习策略
export interface UserCourseSRSSetting {
  id: number
  //userId: number             // delete
  //courseId: number           // delete
  
  // 课程级策略与状态
  frequencySetting: FrequencySetting  // 复习频率
  status: CourseStudyStatus           // 学习状态
  
  //createdAt: string          // delete
  //updatedAt: string          // delete
}

// 用户卡片课程归属
/*
export interface UserCardInCourse {   // delete
  id: number
  userId: number
  cardId: number
  courseId: number
  createdAt: string
}
 */

// 完整的卡片信息（包含当前版本内容和上下文）
/*
export interface MemoryCardWithVersion extends MemoryCard {     // delete
  currentVersion: MemoryCardVersion
  userCard?: UserCardSRSState        // 用户的全局SRS状态
  courses?: Course[]                 // 卡片所属的课程列表
  primaryCourse?: Course             // 主要课程（最优策略）
}
*/

// 添加卡片组到用户记忆库请求
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
  cards: { front: string; back: string }[]
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

// 复习卡片请求 - 完成复习后提交
export interface ReviewCardRequest {
  cardId: number
  result: ReviewResult
  timeSpent?: number // 复习时间（秒）
}

// 获取卡片组列表的查询参数 - 使用Keyset分页
export interface GetDecksQuery {
  postId?: number
  creatorId?: number
  state?: DeckState
  sortBy?: 'score' | 'createdAt' | 'upvoteCount'
  sortOrder?: 'asc' | 'desc'
  // Keyset分页参数
  lastScore?: number    // 上一页最后一条记录的分数
  lastId?: number       // 上一页最后一条记录的ID
  limit?: number        // 每页大小，默认10
}

// 获取复习队列的查询参数 (V8.0)
export interface GetReviewQueueQuery extends LimitQuery {
  dueOnly?: boolean     // 只获取到期的卡片
  courseId?: number     // 筛选特定课程的卡片
}

// 记忆库管理查询参数  - 查询需要显示在左侧的课程列表
export interface GetMemoryBankQuery {
  //userId: number                          // delete 
  courseId?: number     // 筛选特定课程
  status?: CourseStudyStatus  // 筛选课程状态 - 学习中、已暂停、已归档 默认应该是前两个，也可以单独查询已归档的
}

// 课程记忆库信息 - 包含课程信息和记忆卡片统计
export interface CourseMemoryBank {
  course: Course
  setting: UserCourseSRSSetting
  cardCount: number
  dueCardCount: number
  newCardCount: number
  reviewCardCount: number
  learnedCardCount: number
}

// 卡片组统计信息 - 卡片组内的卡片统计
export interface DeckStats {
  totalCards: number
  newCards: number
  reviewCards: number
  learnedCards: number
}

// 卡片diff类型枚举
export const CardDiffType = {
  ADDED: 'added',     // 新增卡片
  MODIFIED: 'modified', // 修改卡片
  DELETED: 'deleted'   // 删除卡片
} as const

export type CardDiffType = typeof CardDiffType[keyof typeof CardDiffType]

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
  //oldVersion: number   // delete
  //newVersion: number   // delete
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

// 接受卡片组更新请求
export interface AcceptDeckChangesRequest {
  deckId: number
  cardIds: number[]    // 接受更新的卡片ID列表
  /*
  acceptedChanges: {
    //updateMeta?: boolean // 是否更新标题和描述         // delete 不需要这个值
    cardIds: number[]    // 接受更新的卡片ID列表
  }
    */
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
  //cards: MemoryCardWithVersion[]           // 类型修改为 MemoryCardView
  cards: MemoryCardView[]
  stats: DeckStats
}

/**
 * 工作流程示例：

  1. 用户点击“开始复习”，前端获取到一个包含50张卡片的复习队列。
  2. 前端在内存中创建一个 ReviewSession 对象：
  {
    "startTime": "2023-08-22T10:00:00Z",
    "totalCards": 50,
    "reviewedCards": 0,
    "correctAnswers": 0,
    "results": []
  }
  3. 用户复习了卡片A，点击了“良好(Good)”，用时8秒。
    - 前端 push 一个 ReviewCardResult 到 results 数组：{ cardId: 101, result: 2, timeSpent: 8 }。
    - 更新 reviewedCards 为 1，correctAnswers 为 1。
  4. 用户复习了卡片B，点击了“忘记了(Failed)”，用时15秒。
    - 前端 push 一个 ReviewCardResult 到 results 数组：{ cardId: 205, result: 0, timeSpent: 15 }。
    - 更新 reviewedCards 为 2，correctAnswers 保持 1。
  5. ...用户继续复习...
  6. 用户复习了20张后，点击了“结束学习”。
  7. 前端最终的 ReviewSession 对象可能是：
  {
    "startTime": "2023-08-22T10:00:00Z",
    "endTime": "2023-08-22T10:15:00Z",
    "totalCards": 50,
    "reviewedCards": 20,
    "correctAnswers": 15,
    "results": [ ...20个ReviewCardResult对象... ]
  }
  8. 前端可以将这个对象发送给后端保存，或者直接在前端用它来渲染一个“本次学习总结”的弹窗，告诉用户：“本次学习15分钟，复习了20张卡，正确率75%！继续加油！”
 */
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