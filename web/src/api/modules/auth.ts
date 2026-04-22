import apiClient from '../client'
import type { ApiResponse } from '@/types/api'
import type { User, AuthLoginResponse, PendingSession, PasswordResetSession } from '@/types/user'

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

  /**
   * 忘记密码：请求发送重置验证码
   */
  requestPasswordReset(
    email: string,
    turnstileToken: string
  ): Promise<ApiResponse<PasswordResetSession>> {
    return apiClient.post('/v1/auth/password-reset/request', { email, turnstileToken })
  },

  /**
   * 忘记密码：重发验证码
   */
  resendPasswordResetCode(
    resetSessionToken: string
  ): Promise<ApiResponse<PasswordResetSession>> {
    // 后端复用 ResendVerificationCodeRequest 结构，字段名为 pendingSessionToken
    return apiClient.post('/v1/auth/password-reset/resend', {
      pendingSessionToken: resetSessionToken,
    })
  },

  /**
   * 忘记密码：校验验证码
   */
  verifyPasswordResetCode(
    resetSessionToken: string,
    code: string
  ): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/auth/password-reset/verify-code', {
      resetSessionToken,
      code,
    })
  },

  /**
   * 忘记密码：确认新密码
   */
  confirmPasswordReset(
    resetSessionToken: string,
    newPassword: string
  ): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/auth/password-reset/confirm', {
      resetSessionToken,
      newPassword,
    })
  },
}
