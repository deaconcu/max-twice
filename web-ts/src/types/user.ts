/**
 * 用户相关的类型定义
 * 统一合并所有后端 DTO 版本的字段
 */

// 统一的用户信息 (合并所有版本)
export interface User {
  id: number                   // 所有版本都有
  name: string                 // 所有版本都有
  email?: string               // 邮箱 (可选) - UserDTO
  password?: string            // 密码 (可选，一般不返回) - UserDTO
  phone?: string               // 手机号 (可选) - UserDTO
  emailValidated?: boolean     // 邮箱是否验证 (可选) - UserDTO
  biography?: string           // 个人简介 (可选) - UserDTO, UserDTOV3
  avatar?: string              // 头像 (可选)
  subscriptions?: number[]     // 订阅的课程ID列表 (可选) - 用于兼容
  createdAt?: string           // 创建时间 (可选) - UserDTO
  updatedAt?: string           // 更新时间 (可选) - UserDTO
  followed?: number            // 关注人数 - UserDTOV3
  canFollow?: boolean          // 是否可以关注 (可选)
  isFollowing?: boolean        // 是否已关注 (可选)
}


// 用户登录响应数据（只包含 id, name, subscriptions）
export interface LoginResponseData {
  id: number
  name: string
  subscriptions: SubscriptionInfo[]  // 订阅的课程信息列表，与UserDTOV2一致
}

// 订阅信息（与后端SubscriptionDTO一致）
export interface SubscriptionInfo {
  id: number
  name: string
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