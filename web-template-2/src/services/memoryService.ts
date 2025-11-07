// 记忆服务 - Mock 数据实现
import type {
  MemoryCardView,
  CourseMemoryBank,
  ReviewStats,
  ReviewRequest,
  ReviewQueueRequest,
  CardListRequest,
  CardContentDiff
} from '@/types/memoryCard'
import {
  ReviewResult,
  FrequencySetting,
  CourseStudyStatus,
  DeckState
} from '@/types/memoryCard'

// API 响应类型
interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// Mock 数据 - 课程记忆库
const mockCourseMemoryBanks: CourseMemoryBank[] = [
  {
    course: {
      id: 1,
      name: 'Python编程入门',
      icon: 'mdi-language-python',
      iconColor: 'blue'
    },
    cardCount: 156,
    dueCardCount: 23,
    setting: {
      frequencySetting: FrequencySetting.NORMAL,
      status: CourseStudyStatus.STUDYING,
      dailyNewCards: 20,
      dailyReviewCards: 100
    }
  },
  {
    course: {
      id: 2,
      name: '数据结构与算法',
      icon: 'mdi-chart-tree',
      iconColor: 'green'
    },
    cardCount: 234,
    dueCardCount: 45,
    setting: {
      frequencySetting: FrequencySetting.HIGH,
      status: CourseStudyStatus.STUDYING,
      dailyNewCards: 30,
      dailyReviewCards: 150
    }
  },
  {
    course: {
      id: 3,
      name: '机器学习入门',
      icon: 'mdi-brain',
      iconColor: 'purple'
    },
    cardCount: 189,
    dueCardCount: 12,
    setting: {
      frequencySetting: FrequencySetting.NORMAL,
      status: CourseStudyStatus.STUDYING,
      dailyNewCards: 20,
      dailyReviewCards: 100
    }
  },
  {
    course: {
      id: 4,
      name: 'JavaScript核心技术',
      icon: 'mdi-language-javascript',
      iconColor: 'yellow'
    },
    cardCount: 145,
    dueCardCount: 8,
    setting: {
      frequencySetting: FrequencySetting.LOW,
      status: CourseStudyStatus.PAUSED,
      dailyNewCards: 10,
      dailyReviewCards: 50
    }
  },
  {
    course: {
      id: 5,
      name: '英语语法精讲',
      icon: 'mdi-book-alphabet',
      iconColor: 'red'
    },
    cardCount: 298,
    dueCardCount: 56,
    setting: {
      frequencySetting: FrequencySetting.HIGH,
      status: CourseStudyStatus.STUDYING,
      dailyNewCards: 30,
      dailyReviewCards: 150
    }
  }
]

// Mock 数据 - 记忆卡片
const generateMockCards = (courseId?: number, count: number = 50): MemoryCardView[] => {
  const cards: MemoryCardView[] = []
  const now = new Date()

  const courseName = courseId
    ? mockCourseMemoryBanks.find(b => b.course.id === courseId)?.course.name || '课程'
    : '课程'

  for (let i = 1; i <= count; i++) {
    const dueDate = new Date(now.getTime() - Math.random() * 7 * 24 * 60 * 60 * 1000)
    const repetitions = Math.floor(Math.random() * 5)

    cards.push({
      id: courseId ? courseId * 1000 + i : i,
      front: `${courseName} - 问题 ${i}: 这是一个关于${courseName}的问题`,
      back: `答案 ${i}: 这是对应的答案和解释`,
      deck: {
        id: courseId || 1,
        title: `${courseName} - 基础概念`,
        description: '核心知识点记忆卡片组',
        state: DeckState.ACTIVE,
        cardCount: count,
        sourceUrl: 'https://example.com/deck',
        lastUpdated: new Date().toISOString(),
        version: '1.0.0'
      },
      srsState: {
        repetitions: repetitions,
        intervalDays: repetitions > 0 ? repetitions * 2 : 0,
        easeFactor: 2.5,
        reviewDueAt: dueDate.toISOString(),
        lastReviewedAt: repetitions > 0 ? new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString() : undefined
      },
      hasDeckUpdate: Math.random() > 0.9,
      hasCardUpdate: Math.random() > 0.95,
      createdAt: new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000).toISOString(),
      updatedAt: new Date(now.getTime() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString()
    })
  }

  return cards
}

// Mock 数据 - 复习统计
const mockReviewStats: ReviewStats = {
  totalReviews: 1245,
  streakDays: 15,
  averageScore: 87,
  timeSpent: 2340
}

// 模拟延迟
const delay = (ms: number = 300) => new Promise(resolve => setTimeout(resolve, ms))

export class MemoryService {
  // 获取记忆库课程列表
  static async getMemoryBankCourses(): Promise<ApiResponse<CourseMemoryBank[]>> {
    await delay()
    return {
      code: 200,
      message: 'success',
      data: mockCourseMemoryBanks
    }
  }

  // 获取复习队列（到期卡片）
  static async getReviewQueue(params: ReviewQueueRequest): Promise<ApiResponse<MemoryCardView[]>> {
    await delay()

    const allCards = generateMockCards(params.courseId, 100)
    // 筛选出到期的卡片
    const now = new Date()
    const dueCards = allCards.filter(card => {
      if (!card.srsState) return true
      return new Date(card.srsState.reviewDueAt) <= now
    })

    const limit = params.limit || 20
    return {
      code: 200,
      message: 'success',
      data: dueCards.slice(0, limit)
    }
  }

  // 获取卡片列表（全部卡片，支持分页）
  static async getCardList(params: CardListRequest): Promise<ApiResponse<MemoryCardView[]>> {
    await delay()

    const allCards = generateMockCards(params.courseId, 100)
    const limit = params.limit || 20
    const startIndex = params.lastId ? allCards.findIndex(c => c.id === params.lastId) + 1 : 0

    return {
      code: 200,
      message: 'success',
      data: allCards.slice(startIndex, startIndex + limit)
    }
  }

  // 获取复习统计
  static async getReviewStats(): Promise<ApiResponse<ReviewStats>> {
    await delay()
    return {
      code: 200,
      message: 'success',
      data: mockReviewStats
    }
  }

  // 提交复习结果
  static async reviewCard(request: ReviewRequest): Promise<ApiResponse<void>> {
    await delay()

    // 更新统计数据
    mockReviewStats.totalReviews++
    mockReviewStats.timeSpent += request.timeSpent

    // 根据复习结果更新平均分
    const resultScore = request.result * 33.33
    mockReviewStats.averageScore = Math.round(
      (mockReviewStats.averageScore * (mockReviewStats.totalReviews - 1) + resultScore) /
      mockReviewStats.totalReviews
    )

    console.log(`Card ${request.cardId} reviewed with result ${request.result}`)

    return {
      code: 200,
      message: 'success',
      data: undefined
    }
  }

  // 获取卡片内容差异
  static async getCardDiff(cardId: number): Promise<ApiResponse<CardContentDiff>> {
    await delay()

    return {
      code: 200,
      message: 'success',
      data: {
        cardId: cardId,
        deckId: 1,
        deckTitle: 'Python基础概念',
        oldFront: '什么是Python？',
        newFront: '什么是Python编程语言？',
        oldBack: 'Python是一种编程语言',
        newBack: 'Python是一种高级编程语言，以简洁和易读著称',
        lastUpdated: new Date().toISOString()
      }
    }
  }

  // 接受卡片组更新
  static async acceptDeckChanges(deckId: number, cardIds: number[]): Promise<ApiResponse<void>> {
    await delay()

    console.log(`Accepted changes for deck ${deckId}, cards: ${cardIds.join(', ')}`)

    return {
      code: 200,
      message: 'success',
      data: undefined
    }
  }

  // 删除卡片
  static async deleteCards(cardIds: number[]): Promise<ApiResponse<void>> {
    await delay()

    console.log(`Deleted cards: ${cardIds.join(', ')}`)

    return {
      code: 200,
      message: 'success',
      data: undefined
    }
  }

  // 重置卡片学习进度
  static async resetCards(cardIds: number[]): Promise<ApiResponse<void>> {
    await delay()

    console.log(`Reset progress for cards: ${cardIds.join(', ')}`)

    return {
      code: 200,
      message: 'success',
      data: undefined
    }
  }

  // 更新课程记忆设置
  static async updateCourseMemorySetting(
    courseId: number,
    setting: Partial<CourseMemoryBank['setting']>
  ): Promise<ApiResponse<void>> {
    await delay()

    const bank = mockCourseMemoryBanks.find(b => b.course.id === courseId)
    if (bank) {
      Object.assign(bank.setting, setting)
    }

    console.log(`Updated memory setting for course ${courseId}`, setting)

    return {
      code: 200,
      message: 'success',
      data: undefined
    }
  }

  // 移除课程
  static async removeCourse(courseId: number): Promise<ApiResponse<void>> {
    await delay()

    console.log(`Removed course ${courseId} from memory bank`)

    return {
      code: 200,
      message: 'success',
      data: undefined
    }
  }
}
