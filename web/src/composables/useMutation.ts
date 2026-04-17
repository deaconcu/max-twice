import { ref } from 'vue'
import type { Ref } from 'vue'
import { handleApiCall, debounce, throttle } from './utils'
import type { ApiResponse } from './types'
import i18n from '@/i18n'

/**
 * useMutation 配置选项
 */
export interface MutationOptions<TPayload, TResult> {
  // 可选：成功提示信息
  successMessage?: string

  // 可选：失败提示信息
  errorMessage?: string

  // 可选：成功回调（接收结果和原始请求参数）
  onSuccess?: (result: TResult, payload: TPayload) => void | Promise<void>

  // 可选：失败回调
  onError?: (error: Error) => void

  // 可选：是否自动显示提示（默认 true）
  showToast?: boolean

  // 可选：是否需要确认对话框（默认 false）
  confirm?: boolean

  // 可选：确认提示信息
  confirmMessage?: string

  // 可选：防抖延迟（毫秒）
  debounce?: number

  // 可选：节流延迟（毫秒）
  throttle?: number

  // 可选：是否允许并发请求（默认 false）
  allowConcurrent?: boolean
}

/**
 * useMutation 返回值
 */
export interface MutationReturn<TPayload, TResult> {
  execute: (payload?: TPayload) => Promise<TResult | null> // 执行变更（参数可选）
  loading: Ref<boolean> // 加载状态
  error: Ref<Error | null> // 错误信息
  data: Ref<TResult | null> // 返回数据
  reset: () => void // 重置状态
}

/**
 * 统一的数据变更 Composable
 * 用于所有 POST/PUT/DELETE 请求
 *
 * 替代原有的 useCreate、useUpdate、useDelete
 *
 * @example
 * // 创建资源（POST）
 * const { execute: createPost, loading } = useMutation(
 *   postService.createPost,
 *   {
 *     successMessage: '发布成功',
 *     onSuccess: (result) => router.push(`/post/${result.id}`)
 *   }
 * )
 * await createPost({ title: '标题', content: '内容' })
 *
 * @example
 * // 更新资源（PUT）
 * const userId = ref(123)
 * const { execute: updateUser } = useMutation(
 *   (data) => userService.updateUser(userId.value, data),
 *   { successMessage: '更新成功' }
 * )
 * await updateUser({ name: '新名字' })
 *
 * @example
 * // 删除资源（DELETE，带确认）
 * const { execute: deletePost } = useMutation(
 *   postService.deletePost,
 *   {
 *     successMessage: '删除成功',
 *     confirm: true,
 *     confirmMessage: '确定要删除这篇帖子吗？'
 *   }
 * )
 * await deletePost(postId)
 */
export function useMutation<TPayload, TResult>(
  apiFn: (payload: TPayload) => Promise<ApiResponse<TResult>>,
  options: MutationOptions<TPayload, TResult> = {}
): MutationReturn<TPayload, TResult> {
  const {
    successMessage,
    errorMessage,
    onSuccess,
    onError,
    showToast = true,
    confirm = false,
    confirmMessage = i18n.global.t('common.confirmAction'),
    debounce: debounceDelay,
    throttle: throttleDelay,
    allowConcurrent = false,
  } = options

  // 状态
  const loading = ref(false)
  const error = ref<Error | null>(null)
  const data = ref<TResult | null>(null) as Ref<TResult | null>

  // 核心执行函数
  const executeCore = async (payload?: TPayload): Promise<TResult | null> => {
    // 去重保护
    if (!allowConcurrent && loading.value) {
      console.warn('[useMutation] Request already in progress, ignoring duplicate call')
      return null
    }

    // 确认对话框
    if (confirm) {
      const confirmed = window.confirm(confirmMessage)
      if (!confirmed) {
        return null
      }
    }

    loading.value = true
    error.value = null

    try {
      const result = await handleApiCall(() => apiFn(payload as TPayload), {
        successMessage,
        errorMessage,
        onSuccess: (result) => onSuccess?.(result, payload as TPayload),
        onError,
        showToast,
      })

      data.value = result
      return result
    } catch (err) {
      error.value = err instanceof Error ? err : new Error('Unknown error')
      return null
    } finally {
      loading.value = false
    }
  }

  // 应用防抖或节流
  let execute = executeCore

  if (debounceDelay) {
    execute = debounce(executeCore, debounceDelay) as (payload?: TPayload) => Promise<TResult | null>
  } else if (throttleDelay) {
    execute = throttle(executeCore, throttleDelay) as (payload?: TPayload) => Promise<TResult | null>
  }

  // 重置状态
  const reset = () => {
    loading.value = false
    error.value = null
    data.value = null
  }

  return {
    execute,
    loading,
    error,
    data,
    reset,
  }
}
