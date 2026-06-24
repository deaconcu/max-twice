/**
 * 游标分页响应（v2 统一格式）
 */
export interface CursorPage<T> {
  items: T[]
  hasMore: boolean
  nextCursor?: string // base64 编码的游标，透传给下一次请求的 cursor 参数
}

/**
 * 创建资源的 202 Accepted 响应
 */
export interface CreateAcceptedResponse {
  id: number
}

/**
 * API 错误响应结构（HTTP 4xx/5xx 时后端返回的 body）
 * 形如：{ error: { code: "INVALID_PARAMETER", message: "...", details?: {} } }
 */
export interface ApiErrorBody {
  code: string // 字符串错误码，如 "INVALID_PARAMETER", "USER_NOT_FOUND"
  message: string
  details?: Record<string, unknown>
}

export interface ApiErrorResponse {
  error: ApiErrorBody
}

/**
 * HTTP 请求方法
 */
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
