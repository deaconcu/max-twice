/**
 * 前端输入验证规则配置
 * 与后端 SystemProperties.Validation 保持一致
 */

// ========== 评论相关 ==========
export const COMMENT_VALIDATION = {
  CONTENT_MIN_LENGTH: 1,
  CONTENT_MAX_LENGTH: 500,
} as const

// ========== 用户相关 ==========
export const USER_VALIDATION = {
  USERNAME_MIN_LENGTH: 3,
  USERNAME_MAX_LENGTH: 20,
  PASSWORD_MIN_LENGTH: 8,
  PASSWORD_MAX_LENGTH: 20,
  BIOGRAPHY_MAX_LENGTH: 100,
  EMAIL_MAX_LENGTH: 254,
  PHONE_MAX_LENGTH: 20,
} as const

// ========== 课程相关 ==========
export const COURSE_VALIDATION = {
  NAME_MIN_LENGTH: 2,
  NAME_MAX_LENGTH: 40,
  DESCRIPTION_MIN_LENGTH: 20,
  DESCRIPTION_MAX_LENGTH: 1000,
} as const

// ========== 帖子相关 ==========
export const POST_VALIDATION = {
  CONTENT_MIN_LENGTH: 10,
  CONTENT_MAX_LENGTH: 20000,
} as const

// ========== 角色相关 ==========
export const ROLE_VALIDATION = {
  NAME_MIN_LENGTH: 2,
  NAME_MAX_LENGTH: 30,
  DESCRIPTION_MIN_LENGTH: 20,
  DESCRIPTION_MAX_LENGTH: 2000,
} as const

// ========== 记忆卡片相关 ==========
export const CARD_VALIDATION = {
  FRONT_MIN_LENGTH: 5,
  FRONT_MAX_LENGTH: 500,
  BACK_MIN_LENGTH: 1,
  BACK_MAX_LENGTH: 500,
} as const

export const DECK_VALIDATION = {
  DESCRIPTION_MAX_LENGTH: 200,
} as const

// ========== 消息相关 ==========
export const MESSAGE_VALIDATION = {
  CONTENT_MIN_LENGTH: 1,
  CONTENT_MAX_LENGTH: 1000,
} as const

// ========== 路线图相关 ==========
export const ROADMAP_VALIDATION = {
  CONTENT_MIN_LENGTH: 1,
  CONTENT_MAX_LENGTH: 5000,
  DESCRIPTION_MIN_LENGTH: 1,
  DESCRIPTION_MAX_LENGTH: 500,
} as const
