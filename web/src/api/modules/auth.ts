import apiClient from '../client'
import type { ApiResponse } from '@/types/api'
import type { User, AuthLoginResponse, PendingSession } from '@/types/user'

/**
 * 认证相关 API
 */
export const authApi = {
  /**
   * 用户登录
   * 响应 data：
   * - 已验证邮箱：{ user: {...} }
   * - 未验证邮箱：{ pending: {...} }
   */
  login(
    email: string,
    password: string,
    turnstileToken?: string
  ): Promise<ApiResponse<AuthLoginResponse>> {
    return apiClient.post('/v1/auth/login', {
      email,
      password,
      turnstileToken,
    })
  },

  /**
   * 用户注册（成功后返回 pending session）
   */
  register(
    email: string,
    password: string,
    turnstileToken: string
  ): Promise<ApiResponse<PendingSession>> {
    return apiClient.post('/v1/auth/register', {
      email,
      password,
      turnstileToken,
    })
  },

  /**
   * 邮箱验证 - 凭 pending session token + 验证码
   */
  validateEmail(pendingSessionToken: string, code: string): Promise<ApiResponse<User>> {
    return apiClient.post('/v1/auth/validate-email', {
      pendingSessionToken,
      code,
    })
  },

  /**
   * 重新发送验证码 - 凭 pending session token
   */
  resendVerificationCode(pendingSessionToken: string): Promise<ApiResponse<PendingSession>> {
    return apiClient.post('/v1/auth/resend-verification-code', { pendingSessionToken })
  },
}
