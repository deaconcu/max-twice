/**
 * API Composables 公共类型定义
 */

export type { CursorPage, CreateAcceptedResponse } from '@/types/api'

/**
 * 无限滚动加载更多回调类型
 */
export type LoadMoreCallback = (status: 'ok' | 'empty') => void
