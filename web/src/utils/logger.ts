/**
 * 日志工具
 * 开发环境输出到控制台，生产环境可以发送到监控服务
 */

type LogLevel = 'info' | 'warn' | 'error' | 'debug'

class Logger {
  private isDevelopment = import.meta.env.MODE === 'development'

  /**
   * 信息日志
   */
  info(message: string, ...args: unknown[]): void {
    if (this.isDevelopment) {
      // eslint-disable-next-line no-console
      console.info(`[INFO] ${message}`, ...args)
    }
  }

  /**
   * 警告日志
   */
  warn(message: string, ...args: unknown[]): void {
    if (this.isDevelopment) {
      console.warn(`[WARN] ${message}`, ...args)
    }
  }

  /**
   * 错误日志
   */
  error(message: string, ...args: unknown[]): void {
    if (this.isDevelopment) {
      console.error(`[ERROR] ${message}`, ...args)
    } else {
      // 生产环境：可以发送到监控服务（如 Sentry）
      this.sendToMonitoring('error', message, args)
    }
  }

  /**
   * 调试日志
   */
  debug(message: string, ...args: unknown[]): void {
    if (this.isDevelopment) {
      // eslint-disable-next-line no-console
      console.debug(`[DEBUG] ${message}`, ...args)
    }
  }

  /**
   * 发送到监控服务
   * TODO: 接入实际的监控服务（如 Sentry、LogRocket 等）
   */
  private sendToMonitoring(level: LogLevel, message: string, args: unknown[]): void {
    // 预留：发送到监控服务
    // 例如：Sentry.captureException(new Error(message))
    void level
    void message
    void args
  }
}

export const logger = new Logger()
