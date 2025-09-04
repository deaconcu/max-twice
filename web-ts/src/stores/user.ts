import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Ref } from 'vue'

export const useUserStore = defineStore(
  'user',
  () => {
    // 用户 ID
    const userId: Ref<number | null> = ref(null)
    const subscription: Ref<any[] | null> = ref(null)

    // 设置用户 ID
    const setUserId = (id: number | null): void => {
      userId.value = id
      console.log(`user id: ${userId.value}`)
    }

    const setSubscription = (list: any[] | null): void => {
      subscription.value = list
      console.log(`subscription: ${subscription.value}`)
    }

    // 清除用户 ID（退出登录）
    const logout = (): void => {
      userId.value = null
      subscription.value = null
    }

    return { userId, subscription, setUserId, setSubscription, logout }
  },
  {
    persist: true, // 启用持久化插件
  }
)