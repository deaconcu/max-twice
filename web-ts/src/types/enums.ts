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

// 课程状态常量 (CourseState)
export const CourseState = {
  SUBMITTED: 0, // 已提交
  APPROVED: 1,  // 已批准
  REJECTED: 2   // 已拒绝
} as const

// 职业状态常量 (ProfessionState)
export const ProfessionState = {
  SUBMITTED: 0, // 已提交
  APPROVED: 1,  // 已批准
  REJECTED: 2   // 已拒绝
} as const

// 用户课程状态常量 (UserCourseState)
export const UserCourseState = {
  NOT_STARTED: 0, // 未开始
  IN_PROGRESS: 1, // 进行中
  COMPLETED: 2    // 已完成
} as const

// 用户路线图状态常量 (UserRoadmapState)
export const UserRoadmapState = {
  NOT_STARTED: 0, // 未开始
  IN_PROGRESS: 1, // 进行中
  COMPLETED: 2    // 已完成
} as const

// 帖子状态常量 (PostState)
export const PostState = {
  SUBMITTED: 0, // 已提交
  APPROVED: 1,  // 已批准
  DELETED: 2    // 已删除
} as const

// 评论状态常量 (CommentState)
export const CommentState = {
  SUBMITTED: 0, // 已提交
  APPROVED: 1,  // 已批准
  DELETED: 2    // 已删除
} as const

// 对象类型常量 (ObjectType)
export const ObjectType = {
  POST: 0,    // 帖子
  NODE: 1,    // 节点
  COMMENT: 2, // 评论
  ROADMAP: 3  // 路线图
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
export type CourseState = typeof CourseState[keyof typeof CourseState]
export type ProfessionState = typeof ProfessionState[keyof typeof ProfessionState]
export type UserCourseState = typeof UserCourseState[keyof typeof UserCourseState]
export type UserRoadmapState = typeof UserRoadmapState[keyof typeof UserRoadmapState]
export type PostState = typeof PostState[keyof typeof PostState]
export type CommentState = typeof CommentState[keyof typeof CommentState]
export type ObjectType = typeof ObjectType[keyof typeof ObjectType]
export type MessageType = typeof MessageType[keyof typeof MessageType]
export type Bool = typeof Bool[keyof typeof Bool]