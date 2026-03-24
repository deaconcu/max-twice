/**
 * 全局错误收集器
 * 捕获前端错误并上报到后端
 */

import type { App } from 'vue'
import { errorApi } from '@/api'

class ErrorCollector {
  private isInitialized = false

  /**
   * 初始化错误收集
   */
  init(app: App): void {
    if (this.isInitialized) return
    this.isInitialized = true

    // Vue 组件错误
    this.setupVueErrorHandler(app)

    // JS 运行时错误
    this.setupWindowErrorHandler()

    // Promise 未捕获异常
    this.setupUnhandledRejectionHandler()
  }

  /**
   * Vue 组件错误处理
   */
  private setupVueErrorHandler(app: App): void {
    app.config.errorHandler = (err, instance, info) => {
      const error = err as Error
      this.report({
        errorType: error.name || 'VueError',
        message: error.message,
        stackTrace: error.stack,
        extraData: JSON.stringify({ componentInfo: info }),
      })
    }
  }

  /**
   * JS 运行时错误处理
   */
  private setupWindowErrorHandler(): void {
    window.onerror = (message, source, lineno, colno, error) => {
      this.report({
        errorType: error?.name || 'RuntimeError',
        message: String(message),
        stackTrace: error?.stack,
        extraData: JSON.stringify({ source, lineno, colno }),
      })
      // 返回 true 阻止默认处理
      return true
    }
  }

  /**
   * Promise 未捕获异常处理
   */
  private setupUnhandledRejectionHandler(): void {
    window.onunhandledrejection = (event) => {
      const error = event.reason
      this.report({
        errorType: error?.name || 'UnhandledRejection',
        message: error?.message || String(error),
        stackTrace: error?.stack,
      })
    }
  }

  /**
   * 上报错误
   */
  private report(data: {
    errorType: string
    message: string
    stackTrace?: string
    extraData?: string
  }): void {
    // 开发环境也打印到控制台
    if (import.meta.env.MODE === 'development') {
      console.error('[ErrorCollector]', data.errorType, data.message)
    }

    // 上报到后端
    errorApi
      .report({
        ...data,
        url: window.location.href,
        userAgent: navigator.userAgent,
      })
      .catch(() => {
        // 上报失败，静默处理，避免死循环
      })
  }

  /**
   * 手动上报错误（供外部调用）
   */
  captureError(error: Error, extraData?: Record<string, unknown>): void {
    this.report({
      errorType: error.name,
      message: error.message,
      stackTrace: error.stack,
      extraData: extraData ? JSON.stringify(extraData) : undefined,
    })
  }

  /**
   * 测试前端异常（开发调试用）
   */
  testException(): void {
    throw new Error('这是一个前端测试异常')
  }
}

export const errorCollector = new ErrorCollector()
