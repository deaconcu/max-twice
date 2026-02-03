import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'
import { useUserStore } from './user'
import { logger } from '@/utils/logger'
import type { LoginResponseData, User } from '@/types/user'

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

    // 计算属性
    const isAuthenticated = computed(() => !!userStore.currentUser)

    /**
     * 登录
     * @throws {Error} 当登录失败时抛出错误，包含错误码和错误信息
     */
    const login = async (email: string, password: string): Promise<boolean> => {
      try {
        isLoggingIn.value = true
        const response = await authApi.login(email, password)

        if (response.code === 200 && response.data) {
          const loginData: LoginResponseData = response.data

          // 保存 token (假设后端返回 token，如果没有需要调整)
          // TODO: 根据实际后端响应调整 token 获取方式
          const userToken = localStorage.getItem('token')
          if (userToken) {
            token.value = userToken
          }

          // 保存用户信息
          userStore.setUser({
            id: loginData.id,
            name: loginData.name,
            subscriptions: loginData.subscriptions,
            role: loginData.role,
          } as User)

          return true
        }

        // 业务错误：后端返回非 200 的 code
        const error: any = new Error(response.message || '登录失败')
        error.code = response.code
        throw error
      } finally {
        isLoggingIn.value = false
      }
    }

    /**
     * 注册
     * @throws {Error} 当注册失败时抛出错误，错误信息为后端返回的 message
     */
    const register = async (email: string, password: string): Promise<boolean> => {
      try {
        isRegistering.value = true
        const response = await authApi.register(email, password)

        // 注册成功
        if (response.code === 200) {
          return true
        }

        // 业务错误：后端返回非 200 的 code
        throw new Error(response.message || '注册失败')
      } finally {
        isRegistering.value = false
      }
    }

    /**
     * 邮箱验证
     */
    const validateEmail = async (email: string, code: string): Promise<User | null> => {
      try {
        const response = await authApi.validateEmail(email, code)
        if (response.code === 200 && response.data) {
          userStore.setUser(response.data)
          return response.data
        }
        return null
      } catch (error) {
        logger.error('邮箱验证失败', error)
        return null
      }
    }

    /**
     * 退出登录
     */
    const logout = () => {
      token.value = null
      userStore.logout()
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }

    /**
     * 从 localStorage 恢复 token
     */
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

      // 方法
      login,
      register,
      validateEmail,
      logout,
      restoreToken,
    }
  },
  {
    persist: {
      key: 'auth',
      paths: ['token'], // 只持久化 token
    },
  }
)
