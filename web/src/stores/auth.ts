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
    const isAuthenticated = computed(() => !!token.value && !!userStore.currentUser)

    /**
     * 登录
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
        return false
      } catch (error) {
        logger.error('登录失败', error)
        return false
      } finally {
        isLoggingIn.value = false
      }
    }

    /**
     * 注册
     */
    const register = async (email: string, password: string): Promise<boolean> => {
      try {
        isRegistering.value = true
        const response = await authApi.register(email, password)
        return response.code === 200
      } catch (error) {
        logger.error('注册失败', error)
        return false
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
