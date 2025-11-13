import { inject } from 'vue'
import { useRouter } from 'vue-router'
import type { ApiResponse } from './types'

/**
 * 状态码处理器类型
 * 返回 true 表示已处理，不再执行后续逻辑
 */
export type StatusCodeHandler = (response: ApiResponse) => boolean

/**
 * Snackbar 函数类型
 */
type ShowSnackbarFn = (message: string, type: string) => void

/**
 * API Composable 全局配置接口
 */
export interface ApiComposableConfig {
  // 状态码处理器映射
  statusHandlers: Record<number, StatusCodeHandler>

  // 默认错误消息
  defaultErrorMessage: string

  // 是否自动显示错误提示
  showErrorToast: boolean
}

/**
 * 默认配置
 */
export const defaultConfig: ApiComposableConfig = {
  statusHandlers: {
    // 401 未登录
    401: (_response) => {
      const router = useRouter()
      const showSnackbar = inject<ShowSnackbarFn>('showSnackbar')

      showSnackbar?.('请先登录', 'error')
      router.push('/login')

      return true // 返回 true 表示已处理
    },

    // 403 无权限
    403: (_response) => {
      const showSnackbar = inject<ShowSnackbarFn>('showSnackbar')
      showSnackbar?.('无权限访问', 'error')
      return true
    },

    // 500 服务器错误
    500: (_response) => {
      const showSnackbar = inject<ShowSnackbarFn>('showSnackbar')
      showSnackbar?.('服务器错误，请稍后重试', 'error')
      return true
    },
  },

  defaultErrorMessage: '操作失败，请重试',
  showErrorToast: true,
}

/**
 * 全局配置实例（可以被用户覆盖）
 */
export let globalConfig = { ...defaultConfig }

/**
 * 设置全局配置
 */
export function setApiConfig(config: Partial<ApiComposableConfig>) {
  globalConfig = { ...globalConfig, ...config }
}
