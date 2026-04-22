import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'
import { useUserStore } from './user'
import { logger } from '@/utils/logger'
import type {
  LoginResponseData,
  PasswordResetSession,
  PendingSession,
  User,
} from '@/types/user'
import i18n from '@/i18n'

const PENDING_SESSION_STORAGE_KEY = 'pendingSession'
const RESET_SESSION_STORAGE_KEY = 'passwordResetSession'

/**
 * 读取 sessionStorage 中的 pending session（只在浏览器环境）
 */
const loadPendingSession = (): PendingSession | null => {
  if (typeof window === 'undefined') return null
  const raw = sessionStorage.getItem(PENDING_SESSION_STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as PendingSession
  } catch {
    return null
  }
}

/**
 * 读取 sessionStorage 中的 password reset session
 */
const loadResetSession = (): PasswordResetSession | null => {
  if (typeof window === 'undefined') return null
  const raw = sessionStorage.getItem(RESET_SESSION_STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as PasswordResetSession
  } catch {
    return null
  }
}

/**
 * 认证 Store
 * 管理登录、注册、token 等认证相关状态
 */
export const useAuthStore = defineStore(
  'auth',
  () => {
    const userStore = useUserStore()

    // 状态
    const token = ref<string | null>(null)
    const isLoggingIn = ref(false)
    const isRegistering = ref(false)
    const pendingSession = ref<PendingSession | null>(loadPendingSession())
    const resetSession = ref<PasswordResetSession | null>(loadResetSession())

    // 计算属性
    const isAuthenticated = computed(() => !!userStore.currentUser)

    // ========== 内部工具 ==========

    const setPendingSession = (session: PendingSession | null) => {
      pendingSession.value = session
      if (typeof window === 'undefined') return
      if (session) {
        sessionStorage.setItem(PENDING_SESSION_STORAGE_KEY, JSON.stringify(session))
      } else {
        sessionStorage.removeItem(PENDING_SESSION_STORAGE_KEY)
      }
    }

    const setResetSession = (session: PasswordResetSession | null) => {
      resetSession.value = session
      if (typeof window === 'undefined') return
      if (session) {
        sessionStorage.setItem(RESET_SESSION_STORAGE_KEY, JSON.stringify(session))
      } else {
        sessionStorage.removeItem(RESET_SESSION_STORAGE_KEY)
      }
    }

    // ========== 登录 ==========

    /**
     * 登录
     * 返回值：
     * - 'success'：已验证邮箱，登录成功
     * - 'pending'：密码对但邮箱未验证，需要跳转验证码页，pending 已存入 store
     * @throws {Error} 带 code 字段的业务错误
     */
    const login = async (
      email: string,
      password: string,
      turnstileToken?: string
    ): Promise<'success' | 'pending'> => {
      try {
        isLoggingIn.value = true
        const response = await authApi.login(email, password, turnstileToken)

        if (response.code === 200 && response.data) {
          if (response.data.user) {
            const loginData: LoginResponseData = response.data.user
            userStore.setUser({
              id: loginData.id,
              name: loginData.name,
              avatar: loginData.avatar,
            } as User)
            setPendingSession(null)
            return 'success'
          }
          if (response.data.pending) {
            setPendingSession(response.data.pending)
            return 'pending'
          }
        }

        const error: Error & { code?: number } = new Error(
          response.message ?? i18n.global.t('user.login.loginFailed')
        )
        error.code = response.code
        throw error
      } finally {
        isLoggingIn.value = false
      }
    }

    /**
     * 注册
     * 成功返回 true 并把 pending 存入 store；失败抛出错误
     */
    const register = async (
      email: string,
      password: string,
      turnstileToken: string
    ): Promise<boolean> => {
      try {
        isRegistering.value = true
        const response = await authApi.register(email, password, turnstileToken)

        if (response.code === 200 && response.data) {
          setPendingSession(response.data)
          return true
        }

        throw new Error(response.message ?? i18n.global.t('user.register.registerFailed'))
      } finally {
        isRegistering.value = false
      }
    }

    /**
     * 邮箱验证（凭 store 中的 pendingSessionToken）
     */
    const validateEmail = async (code: string): Promise<User | null> => {
      try {
        const pending = pendingSession.value
        if (!pending) return null
        const response = await authApi.validateEmail(pending.pendingSessionToken, code)
        if (response.code === 200 && response.data) {
          userStore.setUser(response.data)
          setPendingSession(null)
          return response.data
        }
        return null
      } catch (error) {
        logger.error('邮箱验证失败', error)
        throw error
      }
    }

    /**
     * 重发验证码，返回新的 pending（包含新的倒计时快照）
     */
    const resendVerificationCode = async (): Promise<PendingSession | null> => {
      const pending = pendingSession.value
      if (!pending) return null
      const response = await authApi.resendVerificationCode(pending.pendingSessionToken)
      if (response.code === 200 && response.data) {
        setPendingSession(response.data)
        return response.data
      }
      throw new Error(response.message ?? i18n.global.t('user.verifyEmail.sendFailed'))
    }

    /**
     * 清除 pending session（用户主动放弃验证 / token 失效）
     */
    const clearPendingSession = () => {
      setPendingSession(null)
    }

    // ========== 忘记密码 ==========

    /**
     * 请求发送重置验证码；成功后把 resetSession 存入 store
     */
    const requestPasswordReset = async (
      email: string,
      turnstileToken: string
    ): Promise<PasswordResetSession> => {
      const response = await authApi.requestPasswordReset(email, turnstileToken)
      if (response.code === 200 && response.data) {
        setResetSession(response.data)
        return response.data
      }
      throw new Error(
        response.message ?? i18n.global.t('user.forgotPassword.requestFailed')
      )
    }

    /**
     * 重发重置验证码（使用 store 中的 resetSessionToken）
     */
    const resendPasswordResetCode = async (): Promise<PasswordResetSession | null> => {
      const session = resetSession.value
      if (!session) return null
      const response = await authApi.resendPasswordResetCode(session.resetSessionToken)
      if (response.code === 200 && response.data) {
        setResetSession(response.data)
        return response.data
      }
      throw new Error(
        response.message ?? i18n.global.t('user.verifyEmail.sendFailed')
      )
    }

    /**
     * 校验重置验证码
     */
    const verifyPasswordResetCode = async (code: string): Promise<boolean> => {
      const session = resetSession.value
      if (!session) return false
      try {
        const response = await authApi.verifyPasswordResetCode(
          session.resetSessionToken,
          code
        )
        return response.code === 200
      } catch (error) {
        logger.error('密码重置验证码校验失败', error)
        throw error
      }
    }

    /**
     * 确认新密码；成功后清理 resetSession
     */
    const confirmPasswordReset = async (newPassword: string): Promise<boolean> => {
      const session = resetSession.value
      if (!session) return false
      const response = await authApi.confirmPasswordReset(
        session.resetSessionToken,
        newPassword
      )
      if (response.code === 200) {
        setResetSession(null)
        return true
      }
      throw new Error(
        response.message ?? i18n.global.t('user.forgotPassword.resetFailed')
      )
    }

    /**
     * 清除 reset session
     */
    const clearResetSession = () => {
      setResetSession(null)
    }

    /**
     * 退出登录
     */
    const logout = () => {
      token.value = null
      userStore.logout()
      setPendingSession(null)
      setResetSession(null)
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }

    const restoreToken = () => {
      const savedToken = localStorage.getItem('token')
      if (savedToken) {
        token.value = savedToken
      }
    }

    return {
      // 状态
      token,
      isLoggingIn,
      isRegistering,
      isAuthenticated,
      pendingSession,
      resetSession,

      // 方法
      login,
      register,
      validateEmail,
      resendVerificationCode,
      clearPendingSession,
      requestPasswordReset,
      resendPasswordResetCode,
      verifyPasswordResetCode,
      confirmPasswordReset,
      clearResetSession,
      logout,
      restoreToken,
    }
  },
  {
    persist: {
      key: 'auth',
      paths: ['token'],
    },
  }
)
