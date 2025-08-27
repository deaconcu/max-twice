import { useI18n } from 'vue-i18n'
import { useApiErrorHandler } from './useApiErrorHandler'

/**
 * Toast/Snackbar消息国际化工具
 */
export function useToastMessage() {
  const { t } = useI18n()
  const { tryTranslateMessage, handleApiError } = useApiErrorHandler()

  /**
   * 显示成功消息
   * @param {string} messageKey - 消息key或直接消息
   * @param {Array} params - 消息参数
   * @returns {string} 国际化后的消息
   */
  const successMessage = (messageKey, params = []) => {
    try {
      return t(messageKey, params)
    } catch (e) {
      return tryTranslateMessage(messageKey) || messageKey
    }
  }

  /**
   * 显示错误消息
   * @param {string|Error|Object} error - 错误信息
   * @param {Object} response - API响应对象
   * @returns {string} 国际化后的错误消息
   */
  const errorMessage = (error, response = null) => {
    if (typeof error === 'string') {
      try {
        return t(error)
      } catch (e) {
        return tryTranslateMessage(error) || error
      }
    }
    
    return handleApiError(error, response)
  }

  /**
   * 显示警告消息
   * @param {string} messageKey - 消息key或直接消息
   * @param {Array} params - 消息参数
   * @returns {string} 国际化后的消息
   */
  const warningMessage = (messageKey, params = []) => {
    try {
      return t(messageKey, params)
    } catch (e) {
      return tryTranslateMessage(messageKey) || messageKey
    }
  }

  /**
   * 显示信息消息
   * @param {string} messageKey - 消息key或直接消息
   * @param {Array} params - 消息参数
   * @returns {string} 国际化后的消息
   */
  const infoMessage = (messageKey, params = []) => {
    try {
      return t(messageKey, params)
    } catch (e) {
      return tryTranslateMessage(messageKey) || messageKey
    }
  }

  /**
   * 通用消息处理，自动识别消息类型
   * @param {string} messageKey - 消息key
   * @param {string} type - 消息类型: success, error, warning, info
   * @param {Array} params - 消息参数
   * @returns {string} 国际化后的消息
   */
  const getMessage = (messageKey, type = 'info', params = []) => {
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
    getMessage
  }
}