import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Ref } from 'vue'

export const useUserStore = defineStore(
  'user',
  () => {
    // 用户 ID
    const userId: Ref<number | null> = ref(null)
    const name: Ref<string | null> = ref(null)
    const subscription: Ref<any[] | null> = ref(null)

    // 设置用户 ID
    const setUserId = (id: number | null): void => {
      userId.value = id
      console.log(`user id: ${userId.value}`)
    }

    const setName = (val: string | null): void => {
      name.value = val 
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

    return { userId, name, subscription, setUserId, setSubscription, logout }
  },
  {
    persist: true, // 启用持久化插件
  }
)