/**
 * 用户相关的类型定义
 * 统一合并所有后端 DTO 版本的字段
 */

// 统一的用户信息 (合并所有版本)
export interface User {
  id: number
  name: string
  email?: string               // 邮箱 (可选)
  password?: string            // 密码 (可选，一般不返回)
  phone?: string               // 手机号 (可选)
  emailValidated?: boolean     // 邮箱是否验证 (可选)
  biography?: string           // 个人简介 (可选)
  avatar?: string              // 头像 (可选)
  subscriptions?: number[]     // 订阅的课程ID列表 (可选)
  createdAt?: string           // 创建时间 (可选)
  updatedAt?: string           // 更新时间 (可选)
  followed?: number             // 关注人数
}

// 用户登录请求
export interface LoginRequest {
  email: string
  password: string
}

// 用户注册请求
export interface RegisterRequest {
  name: string
  email: string
  password: string
}

// 用户登录响应数据（只包含 id, name, subscriptions）
export interface LoginResponseData {
  id: number
  name: string
  subscriptions: number[]  // 订阅的课程ID列表
}

// 邮箱验证请求
export interface EmailValidationRequest {
  email: string
  code: string
}

// 更新用户信息请求
export interface UpdateUserRequest {
  name: string
  biography: string
}

// 用户关注关系
export interface UserFollow {
  followerId: number
  followeeId: number
  follower?: User        // 可选：某些查询可能不包含完整的用户信息
  followee?: User        // 可选：某些查询可能不包含完整的用户信息
  createdAt: string
}

// 每日统计数据（用于图表展示）
export interface DailyStatsDTO {
  date: string           // 格式: yyyy-MM-dd
  views: number
  twice: number
  helpful: number
  comments: number
}

// 用户统计信息
export interface UserStatsDTO {
  userId: number
  period: 'today' | '7days' | '15days' | '30days' | '1year' | 'all'  // 统计周期
  startDate: string      // 格式: yyyy-MM-dd
  endDate: string        // 格式: yyyy-MM-dd
  totalViews: number     // 总阅读量
  totalTwice: number     // max twice 总数
  totalHelpful: number   // 有帮助总数
  totalComments: number  // 评论总数
  dailyStats?: DailyStatsDTO[]  // 每日明细（用于图表展示）
}

// 空的统计数据
export const emptyUserStats = (): Partial<UserStatsDTO> => ({
  totalViews: 0,
  totalTwice: 0,
  totalHelpful: 0,
  totalComments: 0
})