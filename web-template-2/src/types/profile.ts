// ==================== 用户信息相关类型 ====================

export interface UserInfo {
  id: number
  name: string
  email: string
  avatar?: string
  biography?: string
  joinDate: string
  createdAt?: string
  updatedAt?: string
}

// ==================== 学习进度相关类型 ====================

export interface LearningProgress {
  totalProgress: number       // 总体进度百分比
  completedNodes: number      // 已完成节点数
  totalNodes: number          // 总节点数
  roadmaps: UserRoadmap[]     // 学习中的路线图
  courses: LearningCourse[]   // 学习中的课程
}

export interface UserRoadmap {
  id: number
  name: string
  description?: string
  progress: number
  totalNodes: number
  completedNodes: number
  lastActivity: string
  profession?: {
    id: number
    name: string
  }
  status?: 'public' | 'private' | 'draft'
  createdAt?: string
}

export interface LearningCourse {
  id: number
  courseId: number
  title: string
  description?: string
  progress: number
  totalLessons: number
  completedLessons: number
  lastActivity: string
  category?: string
  difficulty?: string
  estimatedTime?: string
}

// ==================== 统计数据相关类型 ====================

export interface StatsData {
  totalViews: number          // 总浏览量
  totalLikes: number          // 总点赞数
  totalComments: number       // 总评论数
  totalShares: number         // 总分享数
  dailyStats?: DailyStats[]   // 每日统计数据
  weeklyStats?: WeeklyStats   // 周统计
  monthlyStats?: MonthlyStats // 月统计
}

export interface DailyStats {
  date: string
  views: number
  likes: number
  comments: number
  shares: number
}

export interface WeeklyStats {
  totalViews: number
  totalLikes: number
  viewsGrowth: number  // 增长百分比
  likesGrowth: number
}

export interface MonthlyStats {
  totalViews: number
  totalLikes: number
  viewsGrowth: number
  likesGrowth: number
}

// 统计时间段类型
export type StatsPeriod = 'today' | 'yesterday' | '7days' | '30days' | 'all'

// ==================== 订阅相关类型 ====================

export interface UserCourse {
  id: number
  courseId: number
  course: {
    id: number
    name: string
    description: string
    icon?: string
    iconColor?: string
    learnerCount?: number
    category?: string
  }
  order: number
  subscribedAt?: string
}

// ==================== 关注相关类型 ====================

export interface Following {
  id: number
  userId: number
  user: {
    id: number
    name: string
    avatar?: string
    biography?: string
    followersCount?: number
    postsCount?: number
  }
  followedAt: string
}

// ==================== 内容相关类型 ====================

export interface UserContent {
  id: number
  title: string
  type: 'node' | 'course' | 'roadmap' | 'catalog'
  description?: string
  views: number
  likes: number
  comments: number
  createdAt: string
  updatedAt?: string
  status: 'published' | 'draft' | 'archived'
}

// ==================== 文章相关类型 ====================

export enum PostType {
  ARTICLE = 'ARTICLE',
  DISCUSSION = 'DISCUSSION',
  QUESTION = 'QUESTION'
}

export interface Post {
  id: number
  title: string
  content: string
  excerpt?: string
  type: PostType
  coverImage?: string
  views: number
  likes: number
  comments: number
  createdAt: string
  updatedAt?: string
  status: 'published' | 'draft'
  tags?: string[]
}

// ==================== 记忆卡片组相关类型 ====================

export enum DeckState {
  ACTIVE = 'ACTIVE',
  ARCHIVED = 'ARCHIVED',
  DELETED = 'DELETED'
}

export interface MemoryCardDeck {
  id: number
  name: string
  description?: string
  cardCount: number
  reviewedCount: number
  dueCount?: number          // 待复习卡片数
  newCount?: number          // 新卡片数
  state: DeckState
  createdAt: string
  lastReviewAt?: string
  nextReviewAt?: string
  color?: string
  icon?: string
}

// ==================== 路线图相关类型 ====================

export interface RoadmapNode {
  id: string
  label: string
  type?: string
  position?: { x: number; y: number }
  data?: any
}

export interface RoadmapEdge {
  id: string
  source: string
  target: string
  label?: string
}

export interface CreatedRoadmap {
  id: number
  name: string
  description: string
  profession: {
    id: number
    name: string
  }
  nodes: RoadmapNode[]
  edges?: RoadmapEdge[]
  usageCount: number       // 使用人数
  starCount?: number       // 收藏数
  createdAt: string
  updatedAt?: string
  status: 'public' | 'private' | 'draft'
  thumbnail?: string       // 缩略图
}

// ==================== 目录相关类型 ====================

export interface Catalog {
  id: number
  name: string
  description?: string
  itemCount: number        // 目录中的项目数
  views: number
  likes: number
  isPublic: boolean
  createdAt: string
  updatedAt?: string
  coverImage?: string
  items?: CatalogItem[]
}

export interface CatalogItem {
  id: number
  type: 'course' | 'node' | 'article' | 'roadmap'
  itemId: number
  title: string
  order: number
}

// ==================== 统计卡片数据类型 ====================

export interface StatsCardData {
  label: string
  value: number
  icon: string
  color: string
  growth?: number          // 增长百分比
  trend?: 'up' | 'down' | 'stable'
}

// ==================== API 响应类型 ====================

export interface ApiResponse<T> {
  code: number
  data: T
  message?: string
}

export interface PaginatedResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  hasMore: boolean
}
