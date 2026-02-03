import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types/user'
import { logger } from '@/utils/logger'

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
    const userRole = computed(() => currentUser.value?.role ?? 0)
    const isAdmin = computed(() => {
      const role = currentUser.value?.role ?? 0
      return role >= 2 // ADMIN or SUPER_ADMIN
    })
    const isModerator = computed(() => {
      const role = currentUser.value?.role ?? 0
      return role >= 1 // MODERATOR, ADMIN or SUPER_ADMIN
    })

    /**
     * 设置用户信息
     */
    const setUser = (user: User | null) => {
      currentUser.value = user
      if (user) {
        logger.info('用户信息已更新', user.name)
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
      userRole,
      isAdmin,
      isModerator,
      isLoggedIn,

      // 方法
      setUser,
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
