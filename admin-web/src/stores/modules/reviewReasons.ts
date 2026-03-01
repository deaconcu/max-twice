import { defineStore } from 'pinia'
import { ref } from 'vue'
import { adminApi } from '@/api'

/**
 * 审核理由配置 Store
 * 从后端系统配置获取拒绝/屏蔽理由，统一管理并缓存
 */
export const useReviewReasonsStore = defineStore(
  'reviewReasons',
  () => {
    const rejectReasons = ref<string[]>([])
    const banReasons = ref<string[]>([])
    const lastChecked = ref<number>(0)
    const loading = ref(false)
    const CHECK_INTERVAL = 3 * 60 * 1000

    async function checkAndLoad() {
      const now = Date.now()

      if (lastChecked.value && now - lastChecked.value < CHECK_INTERVAL) {
        return { rejectReasons: rejectReasons.value, banReasons: banReasons.value }
      }

      await loadReasons()
      lastChecked.value = now

      return { rejectReasons: rejectReasons.value, banReasons: banReasons.value }
    }

    async function loadReasons() {
      loading.value = true
      try {
        const response = await adminApi.getSystemConfig()
        if (response.data && Array.isArray(response.data)) {
          for (const item of response.data) {
            if (item.key === 'rejectReasons') {
              try {
                const parsed = typeof item.value === 'string' ? JSON.parse(item.value) : item.value
                if (Array.isArray(parsed)) {
                  rejectReasons.value = parsed
                }
              } catch {
                console.error('[ReviewReasonsStore] 解析 rejectReasons 失败')
              }
            } else if (item.key === 'banReasons') {
              try {
                const parsed = typeof item.value === 'string' ? JSON.parse(item.value) : item.value
                if (Array.isArray(parsed)) {
                  banReasons.value = parsed
                }
              } catch {
                console.error('[ReviewReasonsStore] 解析 banReasons 失败')
              }
            }
          }
        }
      } catch (error) {
        console.error('[ReviewReasonsStore] 加载审核理由配置失败', error)
      } finally {
        loading.value = false
      }
    }

    async function refresh() {
      lastChecked.value = 0
      await loadReasons()
    }

    return {
      rejectReasons,
      banReasons,
      loading,
      checkAndLoad,
      refresh,
    }
  },
  {
    persist: {
      key: 'reviewReasonsStore',
      paths: ['rejectReasons', 'banReasons', 'lastChecked'],
    },
  }
)
