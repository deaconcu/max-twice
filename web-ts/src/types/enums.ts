/**
 * 枚举常量定义
 * 与后端 Enums.java 保持一致
 */

// 投票类型常量 (VoteType)
export const VoteType = {
  NONE: 0,      // 无投票
  NORMAL: 1,    // 普通投票
  TWICE: 2,     // 二次投票  
  HELPFUL: 3    // 有帮助
} as const

// 帖子类型常量 (PostType)
export const PostType = {
  CONTENTS: 1,  // 内容
  ARTICLE: 2    // 文章
} as const

// Course 和 Profession 使用 ContentState

// 用户进度状态常量 (UserProgressState) - 统一用于 UserCourse 和 UserRoadmap
export const UserProgressState = {
  NOT_STARTED: 0, // 未开始
  IN_PROGRESS: 1, // 进行中
  COMPLETED: 2    // 已完成
} as const

// 内容状态常量 (ContentState) - 统一用于 Post、Comment、Node、MemoryCardDeck、MemoryCard
export const ContentState = {
  SUBMITTED: 0, // 已提交
  APPROVED: 1,  // 已批准
  BANNED: 2     // 已禁止
} as const

// 对象类型常量 (ObjectType)
export const ObjectType = {
  POST: 1,    // 帖子
  NODE: 2,    // 节点
  COMMENT: 3, // 评论
  ROADMAP: 4, // 路线图
  MEMORY_CARD_DECK: 5 // 记忆卡片组
} as const

// 消息类型常量 (MessageType)
export const MessageType = {
  APPLY_COURSE: 1,          // 申请课程
  FOLLOW: 2,                // 关注
  UPVOTE: 3,                // 点赞
  INVITE: 4,                // 邀请
  NODE_COMMENT: 5,          // 节点评论
  POST_COMMENT: 6,          // 帖子评论
  REPLY_NODE_COMMENT: 7,    // 回复节点评论
  REPLY_POSTING_COMMENT: 8, // 回复帖子评论
  REPLY_ROADMAP_COMMENT: 9, // 回复路线图评论
  ROADMAP_COMMENT: 10,      // 路线图评论
  SYSTEM: 99,               // 系统消息
  OTHER: 100                // 其他
} as const

// 布尔值常量 (Bool)
export const Bool = {
  TRUE: 1,  // 真
  FALSE: 0  // 假
} as const

// 类型导出
export type VoteType = typeof VoteType[keyof typeof VoteType]
export type PostType = typeof PostType[keyof typeof PostType]
export type UserProgressState = typeof UserProgressState[keyof typeof UserProgressState]
export type ContentState = typeof ContentState[keyof typeof ContentState]
export type ObjectType = typeof ObjectType[keyof typeof ObjectType]
export type MessageType = typeof MessageType[keyof typeof MessageType]
export type Bool = typeof Bool[keyof typeof Bool]