import { ref, computed } from 'vue'
import type { Ref } from 'vue'
import { handleApiCall } from './utils'
import type { ApiResponse, CursorParams, LoadMoreCallback } from './types'

/**
 * useInfiniteScroll 配置选项
 */
export interface InfiniteScrollOptions<T> {
  // 必填：获取数据的函数（接收分页参数）
  fetchFn: (params: CursorParams) => Promise<ApiResponse<T[]>>

  // 必填：从最后一项提取下一页参数的函数
  getNextParams: (lastItem: T, currentParams: CursorParams) => CursorParams

  // 可选：初始分页参数（默认 {}）
  initialParams?: CursorParams

  // 可选：数据转换函数
  transform?: (item: T) => T

  // 可选：错误回调
  onError?: (error: Error) => void
}

/**
 * useInfiniteScroll 返回值
 */
export interface InfiniteScrollReturn<T> {
  items: Ref<T[]> // 列表数据
  loading: Ref<boolean> // 加载状态
  error: Ref<Error | null> // 错误信息
  hasMore: Ref<boolean> // 是否还有更多数据
  params: Ref<CursorParams> // 当前分页参数

  loadMore: (done?: LoadMoreCallback) => Promise<void> // 加载更多
  reset: () => void // 重置状态
  refresh: (done?: LoadMoreCallback) => Promise<void> // 刷新列表
}

/**
 * 无限滚动分页 Composable
 * 支持任意分页参数组合（lastId、lastScore、timestamp、offset 等）
 *
 * @example
 * // 使用 lastId + lastScore
 * const { items: posts, loadMore, hasMore } = useInfiniteScroll({
 *   fetchFn: (params) => postService.getPosts(params.lastScore, params.lastId),
 *   getNextParams: (lastItem) => ({
 *     lastId: lastItem.id,
 *     lastScore: lastItem.score || 0
 *   }),
 *   initialParams: { lastId: 0, lastScore: 0 }
 * })
 *
 * @example
 * // 只使用 lastId
 * const { items: comments, loadMore } = useInfiniteScroll({
 *   fetchFn: (params) => commentService.getComments(postId, params.lastId),
 *   getNextParams: (lastItem) => ({ lastId: lastItem.id }),
 *   initialParams: { lastId: 0 }
 * })
 */
export function useInfiniteScroll<T>(
  options: InfiniteScrollOptions<T>
): InfiniteScrollReturn<T> {
  const { fetchFn, getNextParams, initialParams = {}, transform, onError } = options

  // 状态
  const items = ref<T[]>([]) as Ref<T[]>
  const loading = ref(false)
  const error = ref<Error | null>(null)
  const hasMore = ref(true)
  const params = ref<CursorParams>({ ...initialParams }) as Ref<CursorParams>

  // 加载更多
  const loadMore = async (done?: LoadMoreCallback) => {
    // 防止重复请求
    if (loading.value || !hasMore.value) {
      done?.('empty')
      return
    }

    loading.value = true
    error.value = null

    try {
      const result = await handleApiCall(() => fetchFn(params.value), {
        onError,
        showToast: false // 列表加载通常不需要成功提示
      })

      if (result && result.length > 0) {
        // 应用数据转换
        const newItems = transform ? result.map(transform) : result

        // 追加到列表
        items.value.push(...newItems)

        // 更新分页参数
        const lastItem = newItems[newItems.length - 1]
        params.value = getNextParams(lastItem, params.value)

        // 通知加载完成
        done?.('ok')
      } else {
        // 没有更多数据
        hasMore.value = false
        done?.('empty')
      }
    } catch (err) {
      error.value = err instanceof Error ? err : new Error('Unknown error')
      hasMore.value = false
      done?.('empty')
    } finally {
      loading.value = false
    }
  }

  // 刷新列表
  const refresh = async (done?: LoadMoreCallback) => {
    // 重置状态
    items.value = []
    params.value = { ...initialParams }
    hasMore.value = true
    error.value = null

    // 加载第一页
    await loadMore(done)
  }

  // 重置状态
  const reset = () => {
    items.value = []
    loading.value = false
    error.value = null
    hasMore.value = true
    params.value = { ...initialParams }
  }

  return {
    items,
    loading,
    error,
    hasMore,
    params,
    loadMore,
    reset,
    refresh
  }
}
