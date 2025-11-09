import { UserRole } from '@/enums'

/**
 * 用户相关的类型定义
 * 参考：web-ts/src/types/user.ts
 */

/**
 * 订阅信息（与后端 SubscriptionDTO 一致）
 */
export interface SubscriptionInfo {
  id: number
  name: string
}

/**
 * 用户信息接口（合并所有版本）
 */
export interface User {
  id: number // 用户ID
  name: string // 用户名
  email?: string // 邮箱（可选）
  password?: string // 密码（可选，一般不返回）
  phone?: string // 手机号（可选）
  emailValidated?: boolean // 邮箱是否验证
  biography?: string // 个人简介
  state?: number // 用户状态（1: 正常, 2: 已屏蔽）
  role?: UserRole // 用户角色
  avatar?: string // 头像
  subscriptions?: SubscriptionInfo[] // 订阅的课程列表
  createdAt?: string // 创建时间
  updatedAt?: string // 更新时间
  followed?: number // 关注人数
  isFollowing?: boolean // 是否已关注
}

/**
 * 用户登录响应数据
 */
export interface LoginResponseData {
  id: number
  name: string
  subscriptions: SubscriptionInfo[] // 订阅的课程信息列表
  role?: UserRole // 用户角色
}

/**
 * 邮箱验证请求
 */
export interface EmailValidationRequest {
  email: string
  code: string
}

/**
 * 更新用户信息请求
 */
export interface UpdateUserRequest {
  name: string
  biography: string
}

/**
 * 用户关注关系
 */
export interface UserFollow {
  followerId: number
  followeeId: number
  follower?: User // 可选：关注者信息
  followee?: User // 可选：被关注者信息
  createdAt: string
}

/**
 * 每日统计数据（用于图表展示）
 */
export interface DailyStatsDTO {
  date: string // 格式: yyyy-MM-dd
  views: number
  twice: number
  helpful: number
  comments: number
}

/**
 * 用户统计信息
 */
export interface UserStatsDTO {
  userId: number
  period: 'today' | '7days' | '15days' | '30days' | '1year' | 'all' // 统计周期
  startDate: string // 格式: yyyy-MM-dd
  endDate: string // 格式: yyyy-MM-dd
  totalViews: number // 总阅读量
  totalTwice: number // max twice 总数
  totalHelpful: number // 有帮助总数
  totalComments: number // 评论总数
  dailyStats?: DailyStatsDTO[] // 每日明细（用于图表展示）
}
