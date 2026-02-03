import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores'
import { useUserStore } from '@/stores'

/**
 * 认证组合式函数
 * 提供登录、注册、登出等认证功能
 */
export function useAuth() {
  const router = useRouter()
  const authStore = useAuthStore()
  const userStore = useUserStore()

  // 状态
  const isAuthenticated = computed(() => authStore.isAuthenticated)
  const isLoggingIn = computed(() => authStore.isLoggingIn)
  const isRegistering = computed(() => authStore.isRegistering)
  const currentUser = computed(() => userStore.currentUser)

  /**
   * 登录
   * @param email 邮箱
   * @param password 密码
   * @returns Promise<boolean> 登录是否成功
   */
  const login = async (email: string, password: string): Promise<boolean> => {
    const success = await authStore.login(email, password)
    if (success) {
      // 登录成功后跳转到首页或重定向地址
      const redirect = router.currentRoute.value.query.redirect as string
      await router.push(redirect || '/')
    }
    return success
  }

  /**
   * 注册
   * @param email 邮箱
   * @param password 密码
   * @returns Promise<boolean> 注册是否成功
   */
  const register = async (email: string, password: string): Promise<boolean> => {
    return await authStore.register(email, password)
  }

  /**
   * 验证邮箱
   * @param email 邮箱
   * @param code 验证码
   * @returns Promise<boolean> 验证是否成功
   */
  const validateEmail = async (email: string, code: string): Promise<boolean> => {
    const user = await authStore.validateEmail(email, code)
    if (user) {
      // 验证成功后跳转到首页
      await router.push('/')
      return true
    }
    return false
  }

  /**
   * 登出
   */
  const logout = async () => {
    authStore.logout()
    userStore.logout()
    await router.push('/login')
  }

  /**
   * 检查是否已登录，如果未登录则跳转到登录页
   */
  const requireAuth = () => {
    if (!isAuthenticated.value) {
      void router.push({
        path: '/login',
        query: { redirect: router.currentRoute.value.fullPath },
      })
      return false
    }
    return true
  }

  return {
    // 状态
    isAuthenticated,
    isLoggingIn,
    isRegistering,
    currentUser,
    // 方法
    login,
    register,
    validateEmail,
    logout,
    requireAuth,
  }
}
