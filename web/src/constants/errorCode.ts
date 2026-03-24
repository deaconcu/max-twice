/**
 * API 错误码常量定义
 * 与后端 StatusCode 保持一致
 */

/**
 * HTTP 状态码
 */
export const HTTP_STATUS = {
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  INTERNAL_SERVER_ERROR: 500,
} as const

/**
 * 业务错误码
 */
export const BUSINESS_ERROR = {
  /** 节点状态异常 - 节点不是发布状态，暂时无法访问或提交内容 */
  NODE_STATE_INVALID: 1309,
  /** 邮箱未验证 */
  USER_EMAIL_NOT_VALIDATED: 1104,
  /** 需要验证码 */
  CAPTCHA_REQUIRED: 2604,
} as const

/**
 * 所有错误码类型
 */
export type ErrorCode = (typeof HTTP_STATUS)[keyof typeof HTTP_STATUS] | (typeof BUSINESS_ERROR)[keyof typeof BUSINESS_ERROR]
