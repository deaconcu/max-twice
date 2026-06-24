import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'
import { useUserStore } from './user'
import { logger } from '@/utils/logger'
import type { PasswordResetSession, PendingSession, User } from '@/types/user'

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

    // ========== 邮箱验证码登录 ==========

    /**
     * 发送登录验证码；成功后把 pending session 存入 store
     */
    const loginSendCode = async (
      email: string,
      turnstileToken: string
    ): Promise<PendingSession> => {
      try {
        isLoggingIn.value = true
        const session = await authApi.loginSendCode(email, turnstileToken)
        setPendingSession(session)
        return session
      } finally {
        isLoggingIn.value = false
      }
    }

    /**
     * 校验登录验证码（不存在时后端自动建号）
     */
    const verifyLoginCode = async (code: string): Promise<User | null> => {
      const pending = pendingSession.value
      if (!pending) return null
      const user = await authApi.loginVerifyCode(pending.pendingSessionToken, code)
      userStore.setUser(user)
      setPendingSession(null)
      return user
    }

    /**
     * 重发登录验证码
     */
    const resendLoginCode = async (): Promise<PendingSession | null> => {
      const pending = pendingSession.value
      if (!pending) return null
      const session = await authApi.resendLoginCode(pending.pendingSessionToken)
      setPendingSession(session)
      return session
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

        if (response.user) {
          userStore.setUser(response.user)
          setPendingSession(null)
          return 'success'
        }
        if (response.pending) {
          setPendingSession(response.pending)
          return 'pending'
        }

        throw new Error('login failed')
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
        const session = await authApi.register(email, password, turnstileToken)
        setPendingSession(session)
        return true
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
        const user = await authApi.validateEmail(pending.pendingSessionToken, code)
        userStore.setUser(user)
        setPendingSession(null)
        return user
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
      const session = await authApi.resendVerificationCode(pending.pendingSessionToken)
      setPendingSession(session)
      return session
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
      const session = await authApi.requestPasswordReset(email, turnstileToken)
      setResetSession(session)
      return session
    }

    /**
     * 重发重置验证码（使用 store 中的 resetSessionToken）
     */
    const resendPasswordResetCode = async (): Promise<PasswordResetSession | null> => {
      const session = resetSession.value
      if (!session) return null
      const newSession = await authApi.resendPasswordResetCode(session.resetSessionToken)
      setResetSession(newSession)
      return newSession
    }

    /**
     * 校验重置验证码
     */
    const verifyPasswordResetCode = async (code: string): Promise<boolean> => {
      const session = resetSession.value
      if (!session) return false
      try {
        await authApi.verifyPasswordResetCode(session.resetSessionToken, code)
        return true
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
      await authApi.confirmPasswordReset(session.resetSessionToken, newPassword)
      setResetSession(null)
      return true
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
      loginSendCode,
      verifyLoginCode,
      resendLoginCode,
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
