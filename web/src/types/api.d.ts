/**
 * API 响应基础接口
 */
export interface ApiResponse<T = unknown> {
  code: number // 状态码：200 成功，401 未授权等
  data?: T // 响应数据（可选，错误时可能不存在）
  message?: string // 错误信息（可选）
  timestamp?: number // 时间戳（可选）
  path?: string // 请求路径（可选）
}

/**
 * API 错误响应接口
 */
export interface ApiErrorResponse {
  code: number
  message: string
  timestamp?: number
  path?: string
  details?: Record<string, unknown>
}

/**
 * 分页参数接口
 */
export interface PaginationParams {
  page?: number // 页码（从 1 开始）
  size?: number // 每页数量
  lastId?: number // 最后一条记录的 ID（游标分页）
  lastScore?: number // 最后一条记录的分数（排序分页）
}

/**
 * 分页响应接口
 */
export interface PaginationResponse<T> {
  items: T[] // 数据列表
  total?: number // 总记录数
  page?: number // 当前页码
  size?: number // 每页数量
  hasMore?: boolean // 是否有更多数据
}

/**
 * HTTP 请求方法
 */
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'

/**
 * 请求配置接口
 */
export interface RequestConfig {
  url: string
  method: HttpMethod
  params?: Record<string, unknown>
  data?: Record<string, unknown>
  headers?: Record<string, string>
  timeout?: number
}
