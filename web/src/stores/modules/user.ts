import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types/user'
import { logger } from '@/utils/logger'
import { LOCALE_STORAGE_KEY, type Locale, SUPPORTED_LOCALES } from '@/constants/locale'
import i18n from '@/i18n'

/**
 * 用户 Store
 * 管理当前用户信息和状态
 */
export const useUserStore = defineStore(
  'user',
  () => {
    // 状态
    const currentUser = ref<User | null>(null)

    // 计算属性
    const userId = computed(() => currentUser.value?.id ?? null)
    const userName = computed(() => currentUser.value?.name ?? '')

    /**
     * 设置用户信息。
     * <p>
     * 所有登录/注册/验证流程成功后都会走到这里，是 locale 跟随账号的唯一同步点 ——
     * 后端 profile.locale 会覆盖本地 i18n / localStorage。登录流程随后会 router.push，
     * 目标页组件 mount 时用新 locale header 拉数据，无需 reload。
     */
    const setUser = (user: User | null): void => {
      currentUser.value = user
      if (!user) return
      syncLocaleFromProfile(user.locale)
      logger.info('用户信息已更新', user.name)
    }

    /**
     * 用后端 profile.locale 覆盖本地 i18n / localStorage。被 setUser 和手动同步场景复用。
     */
    const syncLocaleFromProfile = (profileLocale?: string): void => {
      if (!profileLocale || !SUPPORTED_LOCALES.includes(profileLocale as Locale)) {
        return
      }
      if (i18n.global.locale.value === profileLocale) {
        return
      }
      i18n.global.locale.value = profileLocale as Locale
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem(LOCALE_STORAGE_KEY, profileLocale)
      }
      if (typeof document !== 'undefined') {
        document.documentElement.lang = profileLocale
      }
    }

    /**
     * 更新用户部分信息
     */
    const updateUser = (updates: Partial<User>) => {
      if (currentUser.value) {
        currentUser.value = {
          ...currentUser.value,
          ...updates,
        }
        logger.info('用户信息已部分更新')
      }
    }

    /**
     * 清除用户信息（退出登录）
     */
    const logout = () => {
      currentUser.value = null
      logger.info('用户已退出')
    }

    /**
     * 检查是否已登录
     */
    const isLoggedIn = computed(() => currentUser.value !== null)

    return {
      // 状态
      currentUser,

      // 计算属性
      userId,
      userName,
      isLoggedIn,

      // 方法
      setUser,
      syncLocaleFromProfile,
      updateUser,
      logout,
    }
  },
  {
    persist: {
      key: 'user',
      paths: ['currentUser'], // 持久化用户信息
    },
  }
)
