/**
 * API Composables 公共类型定义
 */

/**
 * 标准 API 响应格式
 */
export interface ApiResponse<T = any> {
  code: number // 状态码：200 成功，401 未登录，其他为失败
  data: T // 返回数据
  message?: string // 错误信息
}

/**
 * 灵活的游标参数类型，支持任意分页字段
 * 例如: { lastId: 0, lastScore: 0, timestamp: Date.now() }
 */
export type CursorParams = Record<string, any>

/**
 * 无限滚动加载更多回调类型
 */
export type LoadMoreCallback = (status: 'ok' | 'empty') => void
