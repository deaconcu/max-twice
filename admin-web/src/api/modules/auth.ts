import apiClient from '../client'
import type { ApiResponse } from '@/types/api'
import type { User, LoginResponseData } from '@/types/user'

/**
 * 认证相关 API
 * 参考：web-ts/src/services/api/v1/apiServiceV1.ts (authServiceV1)
 */
export const authApi = {
  /**
   * 用户登录
   */
  login(email: string, password: string): Promise<ApiResponse<LoginResponseData>> {
    return apiClient.post('/v1/auth/login', {
      email,
      password,
    })
  },

  /**
   * 用户注册
   */
  register(email: string, password: string): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/auth/register', {
      email,
      password,
    })
  },

  /**
   * 邮箱验证
   */
  validateEmail(email: string, code: string): Promise<ApiResponse<User>> {
    return apiClient.post('/v1/auth/validate-email', {
      email,
      code,
    })
  },

  /**
   * 重新发送验证码
   */
  resendVerificationCode(email: string): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/auth/resend-verification-code', { email })
  },
}
