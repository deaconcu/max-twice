/**
 * API Composables 公共类型定义
 */

// 从全局类型重导出，保持一致性
export type { ApiResponse } from '@/types/api'

/**
 * 灵活的游标参数类型，支持任意分页字段
 * 例如: { lastId: 0, lastScore: 0, timestamp: Date.now() }
 */
export type CursorParams = Record<string, any>

/**
 * 无限滚动加载更多回调类型
 */
export type LoadMoreCallback = (status: 'ok' | 'empty') => void
