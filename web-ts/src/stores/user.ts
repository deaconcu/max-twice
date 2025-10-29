import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Ref } from 'vue'
import type { User } from '@/types/user'

export const useUserStore = defineStore(
  'user',
  () => {
    // 保存完整的用户对象
    const currentUser: Ref<User | null> = ref(null)

    // 设置完整的用户信息
    const setUser = (user: User | null): void => {
      currentUser.value = user
      console.log(`user set:`, user)
    }

    // 清除用户信息（退出登录）
    const logout = (): void => {
      currentUser.value = null
    }

    return {
      currentUser,
      setUser,
      logout
    }
  },
  {
    persist: true // 启用持久化插件
  }
)