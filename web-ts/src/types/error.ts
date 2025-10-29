/**
 * 错误信息类型定义
 */
export interface ErrorInfo {
  code: number
  message: string
  icon?: string
}

/**
 * 错误页面图标映射
 */
export const ERROR_ICONS: Record<number, string> = {
  // HTTP 标准错误码
  401: 'mdi-account-alert-outline', // 未登录
  403: 'mdi-lock-outline', // 权限不足
  404: 'mdi-file-search-outline', // 页面不存在
  500: 'mdi-server-network-off', // 服务器错误

  // 业务错误码
  1201: 'mdi-alert-circle-outline', // 课程不存在
  1208: 'mdi-lock-outline', // 课程被屏蔽

  // 网络错误
  '-1': 'mdi-wifi-off' // 网络错误
}

/**
 * 根据错误码获取图标
 */
export function getErrorIcon(code: number): string {
  return ERROR_ICONS[code] || 'mdi-alert-circle-outline'
}
