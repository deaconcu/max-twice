import { globalConfig } from './config'
import type { ApiResponse } from './types'

type ShowSnackbarFn = (message: string, type: string) => void

// 全局 snackbar 函数引用
let globalShowSnackbar: ShowSnackbarFn | null = null

/**
 * 设置全局 snackbar 函数
 */
export function setGlobalSnackbar(fn: ShowSnackbarFn) {
  globalShowSnackbar = fn
}

/**
 * 防抖函数
 */
export function debounce<T extends (...args: Parameters<T>) => ReturnType<T>>(
  fn: T,
  delay: number
): (...args: Parameters<T>) => void {
  let timeoutId: ReturnType<typeof setTimeout> | null = null

  return function (...args: Parameters<T>) {
    if (timeoutId) {
      clearTimeout(timeoutId)
    }
    timeoutId = setTimeout(() => {
      fn(...args)
    }, delay)
  }
}

/**
 * 节流函数
 */
export function throttle<T extends (...args: Parameters<T>) => ReturnType<T>>(
  fn: T,
  delay: number
): (...args: Parameters<T>) => void {
  let lastCall = 0

  return function (...args: Parameters<T>) {
    const now = Date.now()
    if (now - lastCall >= delay) {
      lastCall = now
      fn(...args)
    }
  }
}

/**
 * 统一的 API 调用处理函数
 */
export async function handleApiCall<T>(
  apiFn: () => Promise<ApiResponse<T>>,
  options: {
    successMessage?: string
    errorMessage?: string
    onSuccess?: (data: T) => void | Promise<void>
    onError?: (error: Error) => void
    showToast?: boolean
  } = {}
): Promise<T | null> {
  const showSnackbar = globalShowSnackbar

  try {
    const response = await apiFn()

    if (response.code === 200) {
      // 成功处理
      if (options.showToast !== false && options.successMessage) {
        showSnackbar?.(options.successMessage, 'success')
      }
      if (options.onSuccess) {
        await options.onSuccess(response.data)
      }
      return response.data
    } else {
      // 检查是否有自定义状态码处理器
      const handler = globalConfig.statusHandlers[response.code]
      if (handler) {
        const handled = handler(response)
        if (handled) {
          // 如果处理器返回 true，表示已完全处理，不再继续
          return null
        }
      }

      // 默认错误处理
      const message = response.message ?? options.errorMessage ?? globalConfig.defaultErrorMessage

      if (globalConfig.showErrorToast && options.showToast !== false) {
        showSnackbar?.(message, 'error')
      }

      const error = new Error(message)
      options.onError?.(error)
      throw error
    }
  } catch (err) {
    // 网络错误或其他异常
    if (err instanceof Error && err.message) {
      // 已经是处理过的错误，直接抛出
      throw err
    }

    const message = options.errorMessage ?? globalConfig.defaultErrorMessage
    if (globalConfig.showErrorToast && options.showToast !== false) {
      showSnackbar?.(message, 'error')
    }

    const error = err instanceof Error ? err : new Error('Unknown error')
    options.onError?.(error)
    throw error
  }
}
