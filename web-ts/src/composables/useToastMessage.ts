import { useI18n } from 'vue-i18n'
import { useApiErrorHandler } from './useApiErrorHandler'
import type { ApiResponse } from '@/types/api'

/**
 * Toast/Snackbar消息国际化工具
 */
export const useToastMessage = () => {
  const { t } = useI18n()
  const { tryTranslateMessage, handleApiError } = useApiErrorHandler()

  /**
   * 显示成功消息
   */
  const successMessage = (messageKey: string, params: any[] = []): string => {
    try {
      return t(messageKey, params)
    } catch {
      return tryTranslateMessage(messageKey) || messageKey
    }
  }

  /**
   * 显示错误消息
   */
  const errorMessage = (error: string | Error | any, response: ApiResponse | null = null): string => {
    if (typeof error === 'string') {
      try {
        return t(error)
      } catch {
        return tryTranslateMessage(error) || error
      }
    }

    return handleApiError(error, response)
  }

  /**
   * 显示警告消息
   */
  const warningMessage = (messageKey: string, params: any[] = []): string => {
    try {
      return t(messageKey, params)
    } catch (e) {
      console.error('Warning message translation error:', e)
      return tryTranslateMessage(messageKey) || messageKey
    }
  }

  /**
   * 显示信息消息
   */
  const infoMessage = (messageKey: string, params: any[] = []): string => {
    try {
      return t(messageKey, params)
    } catch (e) {
      console.error('Info message translation error:', e)
      return tryTranslateMessage(messageKey) || messageKey
    }
  }

  /**
   * 通用消息处理，自动识别消息类型
   */
  const getMessage = (messageKey: string, type = 'info', params: any[] = []): string => {
    switch (type) {
      case 'success':
        return successMessage(messageKey, params)
      case 'error':
        return errorMessage(messageKey)
      case 'warning':
        return warningMessage(messageKey, params)
      case 'info':
      default:
        return infoMessage(messageKey, params)
    }
  }

  return {
    successMessage,
    errorMessage,
    warningMessage,
    infoMessage,
    getMessage,
  }
}