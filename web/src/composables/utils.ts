import { ApiError } from '@/api/client'
import { getGlobalSnackbar, getGlobalRouter } from './config'
import i18n from '@/i18n'

function t(key: string): string {
  return i18n.global.t(key)
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
    if (timeoutId) clearTimeout(timeoutId)
    timeoutId = setTimeout(() => fn(...args), delay)
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
 * 将 ApiError 转换为用户可读的错误信息
 */
export function getErrorMessage(error: unknown, fallback?: string): string {
  if (error instanceof ApiError) {
    return error.message
  }
  if (error instanceof Error) {
    return error.message
  }
  return fallback ?? t('error.retryLater')
}

/**
 * 统一的 API 调用处理函数（v2）
 *
 * apiFn 直接返回 Promise<T>（无 ApiResponse 包装）。
 * 错误由 axios 拦截器转为 ApiError 抛出，这里统一处理：
 * - 401 → 跳转登录页
 * - 其他 → 显示 snackbar 错误提示
 */
export async function handleApiCall<T>(
  apiFn: () => Promise<T>,
  options: {
    successMessage?: string
    errorMessage?: string
    onSuccess?: (data: T) => void | Promise<void>
    onError?: (error: Error) => void
    showToast?: boolean
  } = {}
): Promise<T | null> {
  const showSnackbar = getGlobalSnackbar()
  const router = getGlobalRouter()

  try {
    const result = await apiFn()

    if (options.showToast !== false && options.successMessage) {
      showSnackbar?.(options.successMessage, 'success')
    }

    if (options.onSuccess) {
      await options.onSuccess(result)
    }

    return result
  } catch (err) {
    const error = err instanceof Error ? err : new Error(t('error.retryLater'))

    // 401 → 跳转登录
    if (err instanceof ApiError && err.httpStatus === 401) {
      showSnackbar?.(t('error.pleaseLogin'), 'error')
      router?.push('/login')
      options.onError?.(error)
      return null
    }

    const message = options.errorMessage ?? getErrorMessage(err, t('error.retryLater'))

    if (options.showToast !== false) {
      showSnackbar?.(message, 'error')
    }

    options.onError?.(error)
    throw error
  }
}
