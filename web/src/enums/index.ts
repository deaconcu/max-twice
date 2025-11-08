/**
 * 枚举常量定义
 * 与后端 Enums.java 保持一致
 * 参考：web-ts/src/types/enums.ts
 */

/**
 * 投票类型常量
 */
export const VoteType = {
  NONE: 0, // 无投票
  NORMAL: 1, // 普通投票
  TWICE: 2, // 二次投票
  HELPFUL: 3, // 有帮助
} as const

export type VoteType = (typeof VoteType)[keyof typeof VoteType]

/**
 * 帖子类型常量
 */
export const PostType = {
  CONTENTS: 1, // 内容
  ARTICLE: 2, // 文章
} as const

export type PostType = (typeof PostType)[keyof typeof PostType]

/**
 * 用户进度状态常量
 * 统一用于 UserCourse 和 UserRoadmap
 */
export const UserProgressState = {
  NOT_STARTED: 0, // 未开始
  IN_PROGRESS: 1, // 进行中
  COMPLETED: 2, // 已完成
} as const

export type UserProgressState = (typeof UserProgressState)[keyof typeof UserProgressState]

/**
 * 内容状态常量
 * 统一用于 Post、Comment、Node、MemoryCardDeck、MemoryCard
 */
export const ContentState = {
  SUBMITTED: 1, // 待审核
  PUBLISHED: 2, // 已批准
  REJECTED: 3, // 审核不通过（可重新提交）
  BANNED: 4, // 违规封禁（一般不可逆）
} as const

export type ContentState = (typeof ContentState)[keyof typeof ContentState]

/**
 * 对象类型常量
 */
export const ObjectType = {
  POST: 1, // 帖子
  NODE: 2, // 节点
  COMMENT: 3, // 评论
  ROADMAP: 4, // 路线图
  MEMORY_CARD_DECK: 5, // 记忆卡片组
} as const

export type ObjectType = (typeof ObjectType)[keyof typeof ObjectType]

/**
 * 消息分类常量
 */
export const MessageCategory = {
  INTERACTION: 1, // 互动消息
  SYSTEM: 2, // 系统消息
  PRIVATE: 3, // 私信
} as const

export type MessageCategory = (typeof MessageCategory)[keyof typeof MessageCategory]

/**
 * 消息类型常量
 */
export const MessageType = {
  // 系统消息
  APPLY_COURSE: 1, // 申请课程

  // 互动消息
  FOLLOW: 2, // 关注
  UPVOTE: 3, // 点赞
  INVITE: 4, // 邀请
  NODE_COMMENT: 5, // 节点评论
  POST_COMMENT: 6, // 帖子评论
  REPLY_NODE_COMMENT: 7, // 回复节点评论
  REPLY_POSTING_COMMENT: 8, // 回复帖子评论
  REPLY_ROADMAP_COMMENT: 9, // 回复路线图评论
  ROADMAP_COMMENT: 10, // 路线图评论

  // 审核消息类型
  COURSE_REJECTED: 11, // 课程被拒绝
  COURSE_BANNED: 12, // 课程被封禁
  POST_REJECTED: 13, // 帖子被拒绝
  POST_BANNED: 14, // 帖子被封禁
  COMMENT_REJECTED: 15, // 评论被拒绝
  COMMENT_BANNED: 16, // 评论被封禁
  PROFESSION_REJECTED: 17, // 职业被拒绝
  PROFESSION_BANNED: 18, // 职业被封禁
  ROADMAP_REJECTED: 19, // 路线图被拒绝
  ROADMAP_BANNED: 20, // 路线图被封禁
  MEMORY_DECK_REJECTED: 21, // 卡片组被拒绝
  MEMORY_DECK_BANNED: 22, // 卡片组被封禁
  NODE_REJECTED: 23, // 节点被拒绝
  NODE_BANNED: 24, // 节点被封禁
  COURSE_APPROVED: 25, // 课程审核通过
  PROFESSION_APPROVED: 26, // 职业审核通过

  SYSTEM: 99, // 系统消息
  OTHER: 100, // 其他
} as const

export type MessageType = (typeof MessageType)[keyof typeof MessageType]

/**
 * 布尔值常量
 */
export const Bool = {
  TRUE: 1, // 真
  FALSE: 0, // 假
} as const

export type Bool = (typeof Bool)[keyof typeof Bool]

/**
 * 审核操作枚举
 */
export const ApprovalAction = {
  APPROVE: 'approve',
  REJECT: 'reject',
  BAN: 'ban',
} as const

export type ApprovalAction = (typeof ApprovalAction)[keyof typeof ApprovalAction]

/**
 * 用户角色枚举
 */
export const UserRole = {
  USER: 0,
  MODERATOR: 1,
  ADMIN: 2,
  SUPER_ADMIN: 3,
} as const

export type UserRole = (typeof UserRole)[keyof typeof UserRole]
