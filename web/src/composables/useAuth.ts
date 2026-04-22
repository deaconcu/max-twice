import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores'
import { useUserStore } from '@/stores'

/**
 * 认证组合式函数
 */
export function useAuth() {
  const router = useRouter()
  const authStore = useAuthStore()
  const userStore = useUserStore()

  const isAuthenticated = computed(() => authStore.isAuthenticated)
  const isLoggingIn = computed(() => authStore.isLoggingIn)
  const isRegistering = computed(() => authStore.isRegistering)
  const currentUser = computed(() => userStore.currentUser)
  const pendingSession = computed(() => authStore.pendingSession)

  /**
   * 登录
   * @returns 'success' | 'pending'；'success' 会自动跳转到首页/redirect，'pending' 由调用方决定
   */
  const login = async (
    email: string,
    password: string,
    turnstileToken?: string
  ): Promise<'success' | 'pending'> => {
    const result = await authStore.login(email, password, turnstileToken)
    if (result === 'success') {
      const redirect = router.currentRoute.value.query.redirect as string
      await router.push(redirect || '/')
    }
    return result
  }

  /**
   * 注册
   */
  const register = async (
    email: string,
    password: string,
    turnstileToken: string
  ): Promise<boolean> => {
    return await authStore.register(email, password, turnstileToken)
  }

  /**
   * 邮箱验证 - 凭 store 中的 pending token
   */
  const validateEmail = async (code: string): Promise<boolean> => {
    const user = await authStore.validateEmail(code)
    if (user) {
      await router.push('/')
      return true
    }
    return false
  }

  const logout = async () => {
    authStore.logout()
    userStore.logout()
    await router.push('/login')
  }

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
    isAuthenticated,
    isLoggingIn,
    isRegistering,
    currentUser,
    pendingSession,
    login,
    register,
    validateEmail,
    logout,
    requireAuth,
  }
}
