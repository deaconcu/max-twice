// 状态常量定义 - 与后端Enums.java保持一致

/**
 * 课程状态枚举 (CourseState)
 */
export const COURSE_STATE = {
  SUBMITTED: 0, // 已提交
  APPROVED: 1, // 已批准
  REJECTED: 2, // 已拒绝
}

/**
 * 课程申请状态枚举 (CourseRequestState)
 */
export const COURSE_REQUEST_STATE = {
  SUBMITTED: 1, // 已提交
  PROVED: 2, // 已证明
  REJECT: 3, // 已拒绝
}

/**
 * 职业状态枚举 (ProfessionState)
 */
export const PROFESSION_STATE = {
  SUBMITTED: 0, // 已提交
  APPROVED: 1, // 已批准
  REJECTED: 2, // 已拒绝
}

/**
 * 帖子状态枚举 (PostState)
 */
export const POST_STATE = {
  SUBMITTED: 0, // 已提交
  APPROVED: 1, // 已批准
  DELETED: 2, // 已删除
}

/**
 * 评论状态枚举 (CommentState)
 */
export const COMMENT_STATE = {
  SUBMITTED: 0, // 已提交
  APPROVED: 1, // 已批准
  DELETED: 2, // 已删除
}

/**
 * 用户课程状态枚举 (UserCourseState)
 */
export const USER_COURSE_STATE = {
  NOT_STARTED: 0, // 未开始
  IN_PROGRESS: 1, // 进行中
  COMPLETED: 2, // 已完成
}

/**
 * 用户路线图状态枚举 (UserRoadmapState)
 */
export const USER_ROADMAP_STATE = {
  NOT_STARTED: 0, // 未开始
  IN_PROGRESS: 1, // 进行中
  COMPLETED: 2, // 已完成
}

/**
 * 帖子类型枚举 (PostType)
 */
export const POST_TYPE = {
  CONTENTS: 1, // 内容
  ARTICLE: 2, // 文章
}

/**
 * 投票类型枚举 (VoteType)
 */
export const VOTE_TYPE = {
  ONCE: 1, // 一次投票
  TWICE: 2, // 二次投票
  HELPFUL: 3, // 有帮助
}

/**
 * 消息类型枚举 (MessageType)
 */
export const MESSAGE_TYPE = {
  APPLY_COURSE: 1, // 申请课程
  FOLLOW: 2, // 关注
  UPVOTE: 3, // 点赞
  INVITE: 4, // 邀请
  NODE_COMMENT: 5, // 节点评论
  POST_COMMENT: 6, // 帖子评论
  REPLY_NODE_COMMENT: 7, // 回复节点评论
  REPLY_POSTING_COMMENT: 8, // 回复帖子评论
  REPLY_ROADMAP_COMMENT: 9, // 回复路线图评论
  ROADMAP_COMMENT: 10, // 路线图评论
  SYSTEM: 99, // 系统消息
  OTHER: 100, // 其他
}

/**
 * 对象类型枚举 (ObjectType)
 */
export const OBJECT_TYPE = {
  POST: 0, // 帖子
  NODE: 1, // 节点
  COMMENT: 2, // 评论
  ROADMAP: 3, // 路线图
}

/**
 * PostStats类型枚举 (PostStatsType)
 */
export const POST_STATS_TYPE = {
  POST: 0, // 帖子
  ROADMAP: 1, // 学习路线
}

/**
 * 布尔值枚举 (Bool)
 */
export const BOOL = {
  TRUE: 1, // 真
  FALSE: 0, // 假
}
