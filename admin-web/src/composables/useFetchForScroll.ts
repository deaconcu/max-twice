import { ref } from 'vue'
import type { Ref } from 'vue'
import type { ApiResponse, KeysetPageResponse } from '@/types/api'

/**
 * 无限滚动配置选项（适用于 KeysetPageResponse 格式）
 */
export interface FetchForScrollOptions<T, P = Record<string, unknown>> {
  // 获取数据的函数，返回标准的 ApiResponse<KeysetPageResponse<T>> 格式
  fetchFn: (params: P) => Promise<ApiResponse<KeysetPageResponse<T>>>

  // 初始参数
  initialParams: P

  // 是否立即加载（默认 true）
  immediate?: boolean

  // 错误回调
  onError?: (error: Error) => void
}

/**
 * 无限滚动返回值
 */
export interface FetchForScrollReturn<T, P = Record<string, unknown>> {
  items: Ref<T[]>
  loading: Ref<boolean>
  hasMore: Ref<boolean>
  params: Ref<P>
  loadMore: () => Promise<void>
  reset: () => void
}

/**
 * 适用于 KeysetPageResponse 格式的无限滚动 Composable
 *
 * 自动处理 ApiResponse<KeysetPageResponse<T>> 格式转换，
 * 并基于返回数据的最后一项 id 作为下一页的 lastId 参数
 *
 * @param options 配置选项
 * @returns 无限滚动功能
 */
export function useFetchForScroll<T extends { id: number }, P extends { lastId?: number | null } = { lastId?: number | null }>(
  options: FetchForScrollOptions<T, P>
): FetchForScrollReturn<T, P> {
  const { fetchFn, initialParams, immediate = true, onError } = options

  // 状态
  const items = ref<T[]>([]) as Ref<T[]>
  const loading = ref(false)
  const hasMore = ref(true)
  const params = ref<P>({ ...initialParams }) as Ref<P>

  // 加载更多数据
  const loadMore = async (): Promise<void> => {
    if (loading.value || !hasMore.value) {
      return
    }

    loading.value = true

    try {
      const response = await fetchFn(params.value)

      if (response.code === 200 && response.data) {
        const pageData = response.data
        const newItems = pageData.items || []

        // 添加新数据
        items.value.push(...newItems)

        // 更新 hasMore 状态
        hasMore.value = pageData.hasMore ?? false

        // 更新下一页参数（使用最后一项的 id 作为 lastId）
        if (newItems.length > 0 && hasMore.value) {
          const lastItem = newItems[newItems.length - 1]
          params.value = { ...params.value, lastId: lastItem.id }
        }
      } else {
        throw new Error(response.message || '加载失败')
      }
    } catch (error) {
      console.error('Load more error:', error)
      hasMore.value = false

      if (onError) {
        onError(error instanceof Error ? error : new Error('Unknown error'))
      }
    } finally {
      loading.value = false
    }
  }

  // 重置状态
  const reset = (): void => {
    items.value = []
    loading.value = false
    hasMore.value = true
    params.value = { ...initialParams }
  }

  // 如果设置了 immediate，自动加载首屏数据
  if (immediate) {
    void loadMore()
  }

  return {
    items,
    loading,
    hasMore,
    params,
    loadMore,
    reset,
  }
}
