import { useI18n } from 'vue-i18n'
import type { ApiResponse } from '@/types/api'

/**
 * API错误处理工具，统一处理错误消息的国际化
 */
export const useApiErrorHandler = () => {
  const { t } = useI18n()

  /**
   * 处理API错误响应，返回国际化的错误消息
   */
  const handleApiError = (error?: Error | null, response: ApiResponse<any> | null = null): string => {
    // 如果有响应对象，优先处理响应中的错误
    if (response) {
      switch (response.code) {
        case 401:
          return t('message.loginRequired', '请先登录')
        case 403:
          return t('message.permissionDenied', '权限不足')
        case 404:
          return t('message.resourceNotFound', '资源不存在')
        case 500:
          return t('message.serverError', '服务器错误')
        default:
          // 如果响应中有message字段，尝试国际化
          if (response.message) {
            return tryTranslateMessage(response.message)
          }
          return t('message.unknownError', '未知错误')
      }
    }

    // 处理网络错误
    if (error) {
      if ((error as any).code === 'NETWORK_ERROR' || error.message === 'Network Error') {
        return t('message.networkError')
      }
      if ((error as any).code === 'TIMEOUT') {
        return t('message.timeout', '请求超时')
      }
      // 尝试翻译错误消息
      return tryTranslateMessage(error.message) || t('message.systemError')
    }

    return t('message.systemError')
  }

  /**
   * 尝试翻译消息，如果找不到对应的翻译则返回原消息
   */
  const tryTranslateMessage = (message: string): string => {
    if (!message) return ''

    // 尝试将常见的后端错误消息映射到国际化key
    const errorMessageMap: Record<string, string> = {
      用户不存在: 'user.not.found',
      密码错误: 'user.password.wrong',
      邮箱未验证: 'user.email.not.validated',
      课程不存在: 'course.not.found',
      权限不足: 'message.permissionDenied',
      参数错误: 'message.validationError',
      系统繁忙: 'message.systemError',
      'User not found': 'user.not.found',
      'Invalid password': 'user.password.wrong',
      'Email not verified': 'user.email.not.validated',
      'Course not found': 'course.not.found',
      'Permission denied': 'message.permissionDenied',
      'Validation error': 'message.validationError',
      'System busy': 'message.systemError',
    }

    const translationKey = errorMessageMap[message]
    if (translationKey) {
      try {
        return t(translationKey)
      } catch {
        // 如果翻译失败，返回原消息
        return message
      }
    }

    return message
  }

  /**
   * 处理表单验证错误
   */
  const handleValidationErrors = (validationErrors: Record<string, string> | null | undefined): string => {
    if (!validationErrors || typeof validationErrors !== 'object') {
      return t('message.validationError')
    }

    const errors = Object.values(validationErrors)
    if (errors.length === 0) {
      return t('message.validationError')
    }

    // 返回第一个错误的国际化消息
    return tryTranslateMessage(errors[0]) || t('message.validationError')
  }

  return {
    handleApiError,
    tryTranslateMessage,
    handleValidationErrors,
  }
}