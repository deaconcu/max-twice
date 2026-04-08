import { ref, computed } from 'vue'
import type { Ref } from 'vue'
import i18n from '@/i18n'

/**
 * 无限滚动响应数据格式
 */
export interface InfiniteScrollResponse<T> {
  code: number
  data: T[]
  message: string
  hasMore: boolean
  nextCursor?: any
}

/**
 * 无限滚动配置选项
 */
export interface InfiniteScrollOptions<T> {
  // 获取数据的函数
  fetchFn: (params: any) => Promise<InfiniteScrollResponse<T>>

  // 获取下一页参数的函数
  getNextParams: (lastItem: T, currentParams: any) => any

  // 初始参数
  initialParams: any

  // 是否立即加载（默认 false）
  immediate?: boolean

  // 错误回调
  onError?: (error: Error) => void
}

/**
 * 无限滚动返回值
 */
export interface InfiniteScrollReturn<T> {
  items: Ref<T[]>
  loading: Ref<boolean>
  hasMore: Ref<boolean>
  params: Ref
  loadMore: (options?: { done?: () => void }) => Promise<void>
  reset: () => void
}

/**
 * 无限滚动 Composable
 *
 * @param options 配置选项
 * @returns 无限滚动功能
 */
export function useInfiniteScroll<T>(options: InfiniteScrollOptions<T>): InfiniteScrollReturn<T> {
  const { fetchFn, getNextParams, initialParams, immediate = false, onError } = options

  // 状态
  const items = ref<T[]>([]) as Ref<T[]>
  const loading = ref(false)
  const hasMore = ref(true)
  const params = ref({ ...initialParams })

  // 加载更多数据
  const loadMore = async (options?: { done?: () => void }) => {
    if (loading.value || !hasMore.value) {
      options?.done?.()
      return
    }

    loading.value = true

    try {
      const response = await fetchFn(params.value)

      if (response.code === 200) {
        // 添加新数据
        items.value.push(...response.data)

        // 更新 hasMore 状态
        hasMore.value = response.hasMore

        // 更新下一页参数
        if (response.data.length > 0 && hasMore.value) {
          const lastItem = response.data[response.data.length - 1]
          params.value = getNextParams(lastItem, params.value)
        }
      } else {
        throw new Error(response.message || i18n.global.t('error.loadFailed'))
      }
    } catch (error) {
      console.error('Load more error:', error)
      hasMore.value = false

      if (onError) {
        onError(error instanceof Error ? error : new Error('Unknown error'))
      }
    } finally {
      loading.value = false
      options?.done?.()
    }
  }

  // 重置状态
  const reset = () => {
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
