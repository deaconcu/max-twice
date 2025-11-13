import { ref, computed, onMounted } from 'vue'
import type { Ref } from 'vue'
import { handleApiCall, debounce, throttle } from './utils'
import type { ApiResponse } from './types'

/**
 * useFetch 配置选项
 */
export interface FetchOptions<T> {
  // 必填：获取数据的函数
  fetchFn: () => Promise<ApiResponse<T>>

  // 可选：数据转换函数
  transform?: (item: T extends (infer U)[] ? U : T) => T extends (infer U)[] ? U : T

  // 可选：是否立即加载（默认 false）
  immediate?: boolean

  // 可选：成功回调
  onSuccess?: (data: T) => void

  // 可选：错误回调
  onError?: (error: Error) => void

  // 可选：默认值
  defaultValue?: T

  // 可选：刷新时是否保留旧数据（默认 true，避免闪烁）
  keepDataOnRefresh?: boolean

  // 可选：防抖延迟（毫秒）
  debounce?: number

  // 可选：节流延迟（毫秒）
  throttle?: number
}

/**
 * useFetch 返回值
 */
export interface FetchReturn<T> {
  data: Ref<T | null> // 数据
  loading: Ref<boolean> // 加载状态
  error: Ref<Error | null> // 错误信息
  isReady: Ref<boolean> // 数据是否已就绪
  isRefreshing: Ref<boolean> // 是否正在刷新
  isEmpty: Ref<boolean> // 是否为空（数组为空或对象为null）

  execute: () => Promise<void> // 手动执行
  refresh: (silent?: boolean) => Promise<void> // 刷新数据
  reset: () => void // 重置状态
}

/**
 * 通用数据获取 Composable
 *
 * 支持两种模式：
 * 1. 获取单个对象：fetchFn 返回 ApiResponse<object>
 * 2. 获取列表：fetchFn 返回 ApiResponse<array>
 *
 * @example
 * // 获取单个对象
 * const { data: user, loading } = useFetch({
 *   fetchFn: userService.getCurrentUser,
 *   immediate: true
 * })
 *
 * @example
 * // 获取列表
 * const { data: courses, loading, isEmpty } = useFetch({
 *   fetchFn: courseService.getHotCourses,
 *   immediate: true
 * })
 */
export function useFetch<T>(options: FetchOptions<T>): FetchReturn<T> {
  const {
    fetchFn,
    transform,
    immediate = false,
    onSuccess,
    onError,
    defaultValue = null,
    keepDataOnRefresh = true,
    debounce: debounceDelay,
    throttle: throttleDelay,
  } = options

  // 状态
  const data = ref<T | null>(defaultValue) as Ref<T | null>
  const loading = ref(false)
  const error = ref<Error | null>(null)
  const isRefreshing = ref(false)

  // 计算属性
  const isReady = computed(() => data.value !== null && !loading.value)

  // isEmpty: 如果是数组则判断长度，如果是对象则判断是否为 null
  const isEmpty = computed(() => {
    if (data.value === null) return true
    if (Array.isArray(data.value)) return data.value.length === 0
    return false
  })

  // 核心执行函数
  const executeCore = async () => {
    loading.value = true
    error.value = null

    try {
      const result = await handleApiCall(fetchFn, {
        onSuccess,
        onError,
        showToast: false, // 数据获取通常不需要成功提示
      })

      if (result !== null) {
        // 应用数据转换
        if (transform && Array.isArray(result)) {
          data.value = result.map(transform) as T
        } else if (transform && !Array.isArray(result)) {
          data.value = transform(result as T extends (infer U)[] ? U : T) as T
        } else {
          data.value = result
        }
      }
    } catch (err) {
      error.value = err instanceof Error ? err : new Error('Unknown error')
    } finally {
      loading.value = false
    }
  }

  // 刷新函数
  const refresh = async (silent = false) => {
    if (silent) {
      // 静默刷新：不显示 loading，使用 isRefreshing
      isRefreshing.value = true
      const oldData = keepDataOnRefresh ? data.value : null

      try {
        const result = await handleApiCall(fetchFn, {
          onSuccess,
          onError,
          showToast: false,
        })

        if (result !== null) {
          if (transform && Array.isArray(result)) {
            data.value = result.map(transform) as T
          } else if (transform && !Array.isArray(result)) {
            data.value = transform(result as T extends (infer U)[] ? U : T) as T
          } else {
            data.value = result
          }
        }
      } catch (err) {
        // 刷新失败，恢复旧数据
        if (keepDataOnRefresh) {
          data.value = oldData
        }
        error.value = err instanceof Error ? err : new Error('Unknown error')
      } finally {
        isRefreshing.value = false
      }
    } else {
      // 标准刷新：显示 loading
      const oldData = keepDataOnRefresh ? data.value : null

      loading.value = true
      error.value = null

      try {
        const result = await handleApiCall(fetchFn, {
          onSuccess,
          onError,
          showToast: false,
        })

        if (result !== null) {
          if (transform && Array.isArray(result)) {
            data.value = result.map(transform) as T
          } else if (transform && !Array.isArray(result)) {
            data.value = transform(result as T extends (infer U)[] ? U : T) as T
          } else {
            data.value = result
          }
        }
      } catch (err) {
        // 刷新失败，恢复旧数据
        if (keepDataOnRefresh) {
          data.value = oldData
        }
        error.value = err instanceof Error ? err : new Error('Unknown error')
      } finally {
        loading.value = false
      }
    }
  }

  // 重置状态
  const reset = () => {
    data.value = defaultValue
    loading.value = false
    error.value = null
    isRefreshing.value = false
  }

  // 应用防抖或节流
  let execute = executeCore

  if (debounceDelay) {
    const debouncedFn = debounce(executeCore, debounceDelay)
    execute = () => {
      debouncedFn()
      return Promise.resolve()
    }
  } else if (throttleDelay) {
    const throttledFn = throttle(executeCore, throttleDelay)
    execute = () => {
      throttledFn()
      return Promise.resolve()
    }
  }

  // 立即加载
  if (immediate) {
    onMounted(() => {
      execute()
    })
  }

  return {
    data,
    loading,
    error,
    isReady,
    isRefreshing,
    isEmpty,
    execute,
    refresh,
    reset,
  }
}
