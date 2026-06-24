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
  state?: string // 用户状态（'active' | 'banned'）
  role?: UserRole // 用户角色
  avatar?: string // 头像
  subscriptions?: SubscriptionInfo[] // 订阅的课程列表
  createdAt?: string // 创建时间
  updatedAt?: string // 更新时间
  followed?: number // 关注人数
  isFollowing?: boolean // 是否已关注
  timezone?: string // 用户时区（IANA格式，如 America/Los_Angeles）
  locale?: string // 用户偏好语言（'zh' / 'en'），登录后覆盖 localStorage
  hasPassword?: boolean // 是否已设置密码（邮箱验证码登录自动建号时为 false）
}

/**
 * 邮箱验证待处理会话
 * <p>
 * 注册 / 登录未验证邮箱场景下后端返回，前端用于：
 * - 存 pendingSessionToken 到 sessionStorage
 * - 跳转验证码输入页
 * - 按 resendAvailableIn 起重发倒计时
 */
export interface PendingSession {
  pendingSessionToken: string
  email: string
  expiresIn: number
  resendAvailableIn: number
}

/**
 * 设置密码 OTP 发送后的会话信息（用户已登录，仅需重发倒计时）
 */
export interface SetPasswordSession {
  resendAvailableIn: number
}

/**
 * 密码重置会话
 * <p>
 * 忘记密码流程：请求重置 → 收到 token → 输入验证码 → 设置新密码
 */
export interface PasswordResetSession {
  resetSessionToken: string
  email: string
  expiresIn: number
  resendAvailableIn: number
}

/**
 * 登录接口响应：已验证返回 user；未验证返回 pending
 */
export interface AuthLoginResponse {
  user?: User
  pending?: PendingSession
}

/**
 * 邮箱验证请求
 */
export interface EmailValidationRequest {
  pendingSessionToken: string
  code: string
}

/**
 * 更新用户信息请求
 */
export interface UpdateUserRequest {
  name: string
  biography: string
  timezone?: string
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
  viewCount: number
  twiceCount: number
  likeCount: number
  commentCount: number
}

/**
 * 用户当日统计数据
 * 用于今日统计接口，仅包含基础统计字段
 */
export interface UserDailyStatsDTO {
  userId: number
  viewCount: number // 总浏览量
  twiceCount: number // 总"两次能懂"点赞数
  likeCount: number // 总"有用"点赞数
  commentCount: number // 总评论数
}

/**
 * 用户统计数据（包含总计和每日明细）
 * 用于历史统计接口
 */
export interface UserStatsWithDailyDTO {
  userId: number
  viewCount: number // 总浏览量（累计）
  twiceCount: number // 总"两次能懂"点赞数（累计）
  likeCount: number // 总"有用"点赞数（累计）
  commentCount: number // 总评论数（累计）
  dailyStats: DailyStatsDTO[] // 每日明细列表
}

/**
 * 用户统计信息（完整版）
 * 用于全部时间统计
 */
export interface UserStatsDTO {
  userId: number
  viewCount: number // 总浏览量
  twiceCount: number // 总"两次能懂"点赞数
  likeCount: number // 总"有用"点赞数
  commentCount: number // 总评论数

  // 学习进度统计
  learningCourseCount: number // 正在学习的课程数
  completedCourseCount: number // 已完成的课程数
  inProgressRoleCount: number // 正在学习的角色数
  completedRoleCount: number // 已完成的角色数

  // 社交关系统计
  followingUserCount: number // 关注的用户数
  followingCourseCount: number // 关注的课程数
  followingRoleCount: number // 关注的角色数

  // 创作内容统计
  createdArticleCount: number // 创建的文章数
  createdIndexCount: number // 创建的目录数
  createdRoadmapCount: number // 创建的路线图数
  createdCardDeckCount: number // 创建的卡片组数

  // 连续天数统计
  learningStreakDays: number // 连续学习天数
  reviewStreakDays: number // 连续复习天数

  updatedAt: string
}
