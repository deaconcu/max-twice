import apiClient from '../client'
import type { ApiResponse } from '@/types/api'
import type { SetPasswordSession } from '@/types/user'

/**
 * 已登录用户账户设置相关 API。
 * 后端对应 AccountController，路径前缀 /v1/account。
 */
export const accountApi = {
  /**
   * 为空密码账号发送设置密码 OTP 到当前用户邮箱。
   */
  sendSetPasswordCode(): Promise<ApiResponse<SetPasswordSession>> {
    return apiClient.post('/v1/account/password/send-code')
  },

  /**
   * 校验 OTP 并为空密码账号设置新密码。
   */
  confirmSetPassword(code: string, newPassword: string): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/account/password/confirm', { code, newPassword })
  },

  /**
   * 更新当前用户偏好语言。成功后前端负责刷新页面（数据分库）。
   */
  updateLocale(locale: 'zh' | 'en'): Promise<ApiResponse<void>> {
    return apiClient.put('/v1/account/locale', { locale })
  },
}
