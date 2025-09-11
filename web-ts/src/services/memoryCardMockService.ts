/**
 * 记忆卡片Mock数据服务
 * 用于前端开发和测试
 */

import type {
  MemoryCardDeck,
  MemoryCardView,
  UserCardSRSState,
  CreateDeckRequest,
  CreateCardRequest,
  ReviewCardRequest,
  GetDecksQuery,
  GetReviewQueueQuery,
  DeckDetail,
  ReviewSession,
  AddDeckToMemoryBankRequest,
  UpdateCourseSettingRequest,
  CourseMemoryBank,
  DeckUpdateDiff,
  CardDiff,
  UpdateDeckRequest,
  AcceptDeckChangesRequest
} from '@/types/memoryCard'
import { DeckState, CardState, ReviewResult, FrequencySetting, CourseStudyStatus, CardDiffType } from '@/types/memoryCard'

// Mock用户数据
const mockUsers = [
  { id: 1, name: 'AI助手', email: 'ai@example.com', avatar: null },
  { id: 2, name: '学习者小王', email: 'wang@example.com', avatar: null },
  { id: 3, name: '编程爱好者', email: 'coder@example.com', avatar: null },
  { id: 4, name: '数据科学家', email: 'ds@example.com', avatar: null }
]

// Mock卡片组数据
const mockDecks: MemoryCardDeck[] = [
  {
    id: 1,
    sourcePostId: 1,
    creator: mockUsers[0],
    title: 'Vue 3 核心概念',
    description: 'Vue 3的响应式系统、组合式API等核心概念的记忆卡片',
    state: DeckState.NORMAL,
    upvoteCount: 25,
    cardCount: 5,
    createdAt: '2024-01-01T10:00:00Z',
    updatedAt: '2024-01-01T10:00:00Z',
  },
  {
    id: 2,
    sourcePostId: 1,
    creator: mockUsers[1],
    title: 'TypeScript 类型系统',
    description: '深入理解TypeScript的类型系统和高级特性',
    state: DeckState.NORMAL,
    upvoteCount: 18,
    cardCount: 3,
    createdAt: '2024-01-02T14:30:00Z',
    updatedAt: '2024-01-02T14:30:00Z'
  },
  {
    id: 3,
    sourcePostId: 2,
    creator: mockUsers[2],
    title: 'React Hooks 进阶',
    description: 'React Hooks的使用技巧和最佳实践',
    state: DeckState.NORMAL,
    upvoteCount: 32,
    cardCount: 15,
    createdAt: '2024-01-03T09:15:00Z',
    updatedAt: '2024-01-03T09:15:00Z'
  }
]

// Mock卡片数据
const mockCards: MemoryCardView[] = [
  {
    id: 1,
    front: '什么是Vue 3的响应式系统？',
    back: 'Vue 3的响应式系统基于ES6的Proxy，能够拦截对象的读取、设置等操作，实现数据的自动追踪和视图更新。',
    deck: mockDecks[0],
    creator: mockUsers[0]
  },
  {
    id: 2,
    front: 'Composition API的主要优势是什么？',
    back: '1. 更好的逻辑复用 2. 更好的类型推导 3. 更小的生产包体积 4. 更灵活的代码组织',
    deck: mockDecks[0],
    creator: mockUsers[0]
  },
  {
    id: 3,
    front: '如何在Vue 3中使用ref和reactive？',
    back: 'ref用于基本类型数据的响应式包装，reactive用于对象的响应式包装。ref需要通过.value访问值，reactive可以直接访问属性。',
    deck: mockDecks[0],
    creator: mockUsers[0]
  },
  {
    id: 4,
    front: 'watchEffect和watch的区别是什么？',
    back: 'watchEffect会立即执行并自动追踪依赖，watch需要明确指定监听源且默认惰性执行。watchEffect更适合副作用操作，watch更适合条件性的响应。',
    deck: mockDecks[0],
    creator: mockUsers[0]
  },
  {
    id: 5,
    front: 'Vue 3的生命周期钩子有哪些变化？',
    back: '新增了setup()钩子，beforeDestroy改名为beforeUnmount，destroyed改名为unmounted。在setup中使用onMounted、onUpdated等组合式API。',
    deck: mockDecks[0],
    creator: mockUsers[0]
  },
  {
    id: 6,
    front: 'TypeScript的泛型如何工作？',
    back: '泛型允许在定义函数、类或接口时使用类型参数，在使用时指定具体类型。如function identity<T>(arg: T): T，可以保持类型安全同时提供代码复用。',
    deck: mockDecks[1],
    creator: mockUsers[1]
  },
  {
    id: 7,
    front: '什么是TypeScript的联合类型？',
    back: '联合类型使用|符号连接多个类型，表示值可以是其中任意一种类型。如string | number表示值可以是字符串或数字。',
    deck: mockDecks[1],
    creator: mockUsers[1]
  },
  {
    id: 8,
    front: 'TypeScript中interface和type的区别？',
    back: 'interface主要用于对象形状定义，支持声明合并和继承；type更灵活，支持联合类型、交叉类型等高级类型操作，但不支持声明合并。',
    deck: mockDecks[1],
    creator: mockUsers[1]
  }
]


// Mock用户复习计划 - 更新为新的数据结构
const mockUserCards: UserCardSRSState[] = [
  {
    id: 1,
    reviewDueAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(), // 2小时前到期
    lastReviewedAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
    intervalDays: 1,
    repetitions: 1,
    lapseCount: 0
  },
  {
    id: 2,
    reviewDueAt: new Date(Date.now() - 30 * 60 * 1000).toISOString(), // 30分钟前到期
    lastReviewedAt: new Date(Date.now() - 6 * 24 * 60 * 60 * 1000).toISOString(),
    intervalDays: 6,
    repetitions: 2,
    lapseCount: 1
  },
  {
    id: 3,
    reviewDueAt: new Date().toISOString(), // 现在到期
    lastReviewedAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString(),
    intervalDays: 3,
    repetitions: 3,
    lapseCount: 0
  },
  {
    id: 4,
    reviewDueAt: new Date(Date.now() - 4 * 60 * 60 * 1000).toISOString(), // 4小时前到期
    lastReviewedAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
    intervalDays: 1,
    repetitions: 1,
    lapseCount: 2
  },
  {
    id: 5,
    reviewDueAt: new Date(Date.now() - 1 * 60 * 60 * 1000).toISOString(), // 1小时前到期
    lastReviewedAt: new Date(Date.now() - 15 * 24 * 60 * 60 * 1000).toISOString(),
    intervalDays: 15,
    repetitions: 4,
    lapseCount: 0
  },
  {
    id: 6,
    reviewDueAt: new Date(Date.now() - 10 * 60 * 1000).toISOString(), // 10分钟前到期
    intervalDays: 1,
    repetitions: 0,
    lapseCount: 0
  },
  {
    id: 7,
    reviewDueAt: new Date(Date.now() - 5 * 60 * 1000).toISOString(), // 5分钟前到期
    lastReviewedAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
    intervalDays: 2,
    repetitions: 1,
    lapseCount: 0
  },
  {
    id: 8,
    reviewDueAt: new Date(Date.now() - 20 * 60 * 1000).toISOString(), // 20分钟前到期
    lastReviewedAt: new Date(Date.now() - 4 * 24 * 60 * 60 * 1000).toISOString(),
    intervalDays: 4,
    repetitions: 2,
    lapseCount: 0
  }
]

// 工具函数：模拟API延迟
const delay = (ms: number = 500): Promise<void> => 
  new Promise(resolve => setTimeout(resolve, ms))

// 工具函数：生成随机ID
const generateId = (): number => Date.now() + Math.floor(Math.random() * 1000)

export class MemoryCardMockService {
  // 获取卡片组列表
  static async getDecks(query: GetDecksQuery = {}): Promise<{
    code: number
    data: MemoryCardDeck[]
    message?: string
  }> {
    await delay()
    
    let filteredDecks = [...mockDecks]
    
    if (query.postId) {
      filteredDecks = filteredDecks.filter(deck => deck.sourcePostId === query.postId)
    }
    
    if (query.creatorId) {
      filteredDecks = filteredDecks.filter(deck => deck.creator?.id === query.creatorId)
    }
    
    if (query.state !== undefined) {
      filteredDecks = filteredDecks.filter(deck => deck.state === query.state)
    }
    
    // 排序
    if (query.sortBy) {
      filteredDecks.sort((a, b) => {
        const order = query.sortOrder === 'asc' ? 1 : -1
        switch (query.sortBy) {
          case 'score':
            // 为了兼容性，使用 upvoteCount 作为 score 的替代
            return ((b.upvoteCount || 0) - (a.upvoteCount || 0)) * order
          case 'upvoteCount':
            return ((b.upvoteCount || 0) - (a.upvoteCount || 0)) * order
          case 'createdAt':
            return (new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()) * order
          default:
            return 0
        }
      })
    }
    
    // 分页
    const page = query.page || 1
    const size = query.size || 20
    const start = (page - 1) * size
    const end = start + size
    
    return {
      code: 200,
      data: filteredDecks.slice(start, end)
    }
  }

  // 创建卡片组
  static async createDeck(request: CreateDeckRequest): Promise<{
    code: number
    data: MemoryCardDeck
    message?: string
  }> {
    await delay()
    
    const newDeck: MemoryCardDeck = {
      id: generateId(),
      sourcePostId: request.sourcePostId,
      creator: mockUsers[0],
      title: request.title,
      description: request.description,
      state: DeckState.PENDING, // 新创建的卡片组需要审核
      upvoteCount: 0,
      cardCount: 0,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
    
    mockDecks.push(newDeck)
    
    return {
      code: 200,
      data: newDeck
    }
  }

  // 创建卡片
  static async createCard(request: CreateCardRequest): Promise<{
    code: number
    data: MemoryCardView
    message?: string
  }> {
    await delay()
    
    const cardId = generateId()
    
    // 创建卡片
    const newCard: MemoryCardView = {
      id: cardId,
      front: request.front,
      back: request.back,
      deck: mockDecks.find(d => d.id === request.deckId),
      creator: mockUsers[0]
    }
    
    mockCards.push(newCard)
    
    // 更新卡片组的卡片数量
    const deck = mockDecks.find(d => d.id === request.deckId)
    if (deck) {
      deck.cardCount++
      deck.updatedAt = new Date().toISOString()
    }
    
    return {
      code: 200,
      data: newCard
    }
  }

  // 获取卡片组详情
  static async getDeckDetail(deckId: number): Promise<{
    code: number
    data: DeckDetail
    message?: string
  }> {
    await delay()
    
    const deck = mockDecks.find(d => d.id === deckId)
    if (!deck) {
      return {
        code: 404,
        data: null as any,
        message: '卡片组不存在'
      }
    }
    
    const deckCards = mockCards.filter(c => c.deck?.id === deckId)
    const cardsWithSRSState: MemoryCardView[] = deckCards.map(card => {
      const userCard = mockUserCards.find(uc => uc.id === card.id)
      
      return {
        ...card,
        srsState: userCard
      }
    })
    
    const deckDetail: DeckDetail = {
      ...deck,
      cards: cardsWithSRSState,
      stats: {
        totalCards: cardsWithSRSState.length,
        newCards: cardsWithSRSState.filter(c => !c.srsState).length,
        reviewCards: cardsWithSRSState.filter(c => c.srsState && c.srsState.repetitions > 0).length,
        learnedCards: cardsWithSRSState.filter(c => c.srsState && c.srsState.repetitions >= 3).length
      }
    }
    
    return {
      code: 200,
      data: deckDetail
    }
  }

  // 添加卡片到学习计划
  static async addCardToStudy(cardId: number): Promise<{
    code: number
    data: UserCardSRSState
    message?: string
  }> {
    await delay()
    
    const existingUserCard = mockUserCards.find(uc => uc.id === cardId)
    if (existingUserCard) {
      return {
        code: 400,
        data: null as any,
        message: '该卡片已在学习计划中'
      }
    }
    
    const card = mockCards.find(c => c.id === cardId)
    if (!card) {
      return {
        code: 404,
        data: null as any,
        message: '卡片不存在'
      }
    }
    
    const newUserCard: UserCardSRSState = {
      id: generateId(),
      reviewDueAt: new Date().toISOString(), // 立即可复习
      intervalDays: 0,
      repetitions: 0,
      lapseCount: 0
    }
    
    mockUserCards.push(newUserCard)
    
    return {
      code: 200,
      data: newUserCard
    }
  }

  // 获取复习队列
  static async getReviewQueue(query: GetReviewQueueQuery = {}): Promise<{
    code: number
    data: MemoryCardView[]
    message?: string
  }> {
    await delay()
    
    let reviewCards = [...mockUserCards]
    
    if (query.dueOnly) {
      const now = new Date()
      reviewCards = reviewCards.filter(uc => new Date(uc.reviewDueAt) <= now)
    }
    
    // 排序：到期时间越早的越优先
    reviewCards.sort((a, b) => 
      new Date(a.reviewDueAt).getTime() - new Date(b.reviewDueAt).getTime()
    )
    
    if (query.limit) {
      reviewCards = reviewCards.slice(0, query.limit)
    }
    
    // 组装完整卡片信息
    const cardsWithSRSState: MemoryCardView[] = reviewCards.map(userCard => {
      const card = mockCards.find(c => c.id === userCard.id)!
      
      return {
        ...card,
        srsState: userCard
      }
    })
    
    return {
      code: 200,
      data: cardsWithSRSState
    }
  }

  // 复习卡片
  static async reviewCard(request: ReviewCardRequest): Promise<{
    code: number
    data: UserCardSRSState
    message?: string
  }> {
    await delay()
    
    const userCard = mockUserCards.find(uc => uc.id === request.cardId)
    if (!userCard) {
      return {
        code: 404,
        data: null as any,
        message: '用户卡片不存在'
      }
    }
    
    // 简化的SM-2算法实现
    const updateSchedule = (userCard: UserCardSRSState, result: ReviewResult) => {
      const now = new Date()
      
      if (result >= ReviewResult.GOOD) {
        // 回答正确
        if (userCard.repetitions === 0) {
          userCard.intervalDays = 1
        } else if (userCard.repetitions === 1) {
          userCard.intervalDays = 6
        } else {
          userCard.intervalDays = Math.round(userCard.intervalDays * 2.5) // 简化的ease factor
        }
        
        userCard.repetitions++
      } else {
        // 回答错误
        userCard.repetitions = 0
        userCard.intervalDays = 1
        userCard.lapseCount++
      }
      
      // 设置下次复习时间
      const nextReviewDate = new Date(now)
      nextReviewDate.setDate(nextReviewDate.getDate() + userCard.intervalDays)
      userCard.reviewDueAt = nextReviewDate.toISOString()
      
      userCard.lastReviewedAt = now.toISOString()
    }
    
    updateSchedule(userCard, request.result)
    
    return {
      code: 200,
      data: userCard
    }
  }

  // 获取复习统计
  static async getReviewStats(): Promise<{
    code: number
    data: {
      totalReviews: number
      streakDays: number
      averageScore: number
      timeSpent: number
    }
    message?: string
  }> {
    await delay()
    
    return {
      code: 200,
      data: {
        totalReviews: 234, // 增加总复习次数
        streakDays: 12,    // 增加连续学习天数
        averageScore: 87.3, // 平均正确率
        timeSpent: 65 // 今日学习时间（分钟）
      }
    }
  }

  // 获取卡片组更新对比信息
  static async getDeckUpdateDiff(deckId: number): Promise<{
    code: number
    data: DeckUpdateDiff
    message?: string
  }> {
    await delay()
    
    // 模拟卡片组ID为1的更新对比数据
    if (deckId === 1) {
      const mockDiff: DeckUpdateDiff = {
        deckId: 1,
        title: {
          old: 'Vue 3 核心概念',
          new: 'Vue 3 核心概念与实践'
        },
        description: {
          old: 'Vue 3的响应式系统、组合式API等核心概念的记忆卡片',
          new: 'Vue 3的响应式系统、组合式API等核心概念的记忆卡片，包含最新的实践示例'
        },
        cardDiffs: [
          {
            cardId: 1,
            type: CardDiffType.MODIFIED,
            oldVersion: {
              front: '什么是Vue 3的响应式系统？',
              back: 'Vue 3的响应式系统基于ES6的Proxy，能够拦截对象的读取、设置等操作，实现数据的自动追踪和视图更新。'
            },
            newVersion: {
              front: '什么是Vue 3的响应式系统？',
              back: 'Vue 3的响应式系统基于ES6的Proxy，能够拦截对象的读取、设置等操作，实现数据的自动追踪和视图更新。相比Vue 2的Object.defineProperty，Proxy可以监听数组变化和对象属性的添加删除。'
            }
          },
          {
            cardId: 6,
            type: CardDiffType.ADDED,
            newVersion: {
              front: 'Vue 3中如何使用Teleport？',
              back: 'Teleport允许我们将子组件渲染到DOM中的任意位置。使用<Teleport to="#modal">可以将内容渲染到指定的DOM节点，常用于模态框、提示框等需要脱离当前组件层级的场景。'
            }
          },
          {
            cardId: 7,
            type: CardDiffType.ADDED,
            newVersion: {
              front: 'Suspense组件的作用是什么？',
              back: 'Suspense用于协调异步依赖，可以在等待异步组件或异步数据时显示loading状态。通过#default和#fallback插槽，提供优雅的异步内容加载体验。'
            }
          },
          {
            cardId: 9, // 给删除的卡片也分配ID
            type: CardDiffType.DELETED,
            oldVersion: {
              front: '旧的问题',
              back: '旧的答案，已经过时了'
            }
          }
        ],
        summary: {
          addedCount: 2,
          modifiedCount: 1,
          deletedCount: 1
        }
      }
      
      return {
        code: 200,
        data: mockDiff
      }
    }
    
    return {
      code: 404,
      data: null as any,
      message: '没有找到更新信息'
    }
  }

  // 应用卡片组更新
  static async updateDeck(request: AcceptDeckChangesRequest): Promise<{
    code: number
    data: boolean
    message?: string
  }> {
    await delay()
    
    try {
      console.log('应用卡片组更新:', request)
      
      const deck = mockDecks.find(d => d.id === request.deckId)
      if (!deck) {
        return {
          code: 404,
          data: false,
          message: '卡片组不存在'
        }
      }

      // 根据文档要求：用户选择更新后，系统会更新卡片的版本号和deck的版本号
      // 如果用户没有选择任何更新，只更新deck的版本号
      
      if (request.cardIds.length > 0) {
        // 用户选择了部分卡片更新
        console.log(`用户接受了 ${request.cardIds.length} 张卡片的更新`)
      } else {
        // 用户没有选择任何卡片更新
        console.log('用户拒绝了所有更新')
      }
      
      return {
        code: 200,
        data: true,
        message: '卡片组更新成功'
      }
    } catch (error) {
      console.error('Failed to update deck:', error)
      return {
        code: 500,
        data: false,
        message: '更新失败'
      }
    }
  }
}