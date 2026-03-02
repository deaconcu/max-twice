import { defineStore } from 'pinia'
import { ref } from 'vue'
import { adminApi } from '@/api'

/**
 * 系统配置 Store
 * 管理前端 URL、审核理由等系统级配置
 */
export const useSystemConfigStore = defineStore(
  'systemConfig',
  () => {
    const frontendUrl = ref<string>('')
    const rejectReasons = ref<string[]>([])
    const banReasons = ref<string[]>([])
    const loading = ref(false)

    /**
     * 初始化配置（登录后调用，每次登录都会重新加载）
     */
    async function init() {
      await loadConfig()
    }

    /**
     * 加载配置
     */
    async function loadConfig(): Promise<boolean> {
      loading.value = true
      try {
        const response = await adminApi.getSystemConfig()
        if (response.code === 200 && response.data) {
          for (const item of response.data) {
            if (item.key === 'frontendUrl') {
              frontendUrl.value = item.value || ''
            } else if (item.key === 'rejectReasons') {
              try {
                const value = item.value
                if (Array.isArray(value)) {
                  rejectReasons.value = value
                } else if (typeof value === 'string') {
                  const parsed = JSON.parse(value)
                  if (Array.isArray(parsed)) {
                    rejectReasons.value = parsed
                  }
                }
              } catch (e) {
                console.error('[SystemConfig] 解析 rejectReasons 失败', e)
              }
            } else if (item.key === 'banReasons') {
              try {
                const value = item.value
                if (Array.isArray(value)) {
                  banReasons.value = value
                } else if (typeof value === 'string') {
                  const parsed = JSON.parse(value)
                  if (Array.isArray(parsed)) {
                    banReasons.value = parsed
                  }
                }
              } catch (e) {
                console.error('[SystemConfig] 解析 banReasons 失败', e)
              }
            }
          }
          return true
        }
        return false
      } catch (error) {
        console.error('[SystemConfig] 加载配置失败', error)
        return false
      } finally {
        loading.value = false
      }
    }

    /**
     * 更新前端 URL（供 SystemConfiguration 组件调用）
     */
    function setFrontendUrl(url: string) {
      frontendUrl.value = url
    }

    /**
     * 更新拒绝理由
     */
    function setRejectReasons(reasons: string[]) {
      rejectReasons.value = reasons
    }

    /**
     * 更新屏蔽理由
     */
    function setBanReasons(reasons: string[]) {
      banReasons.value = reasons
    }

    /**
     * 获取职业前台链接
     */
    function getProfessionUrl(professionId: number): string {
      return `${frontendUrl.value}/career/${professionId}`
    }

    /**
     * 获取课程前台链接
     */
    function getCourseUrl(courseId: number): string {
      return `${frontendUrl.value}/courses/${courseId}`
    }

    /**
     * 获取节点前台链接
     */
    function getNodeUrl(nodeId: number): string {
      return `${frontendUrl.value}/node/${nodeId}`
    }

    /**
     * 获取用户前台链接
     */
    function getUserUrl(userId: number): string {
      return `${frontendUrl.value}/user/${userId}`
    }

    /**
     * 获取路线图前台链接
     */
    function getRoadmapUrl(roadmapId: number): string {
      return `${frontendUrl.value}/roadmap/${roadmapId}`
    }

    return {
      frontendUrl,
      rejectReasons,
      banReasons,
      loading,
      init,
      loadConfig,
      setFrontendUrl,
      setRejectReasons,
      setBanReasons,
      getProfessionUrl,
      getCourseUrl,
      getNodeUrl,
      getUserUrl,
      getRoadmapUrl,
    }
  },
  {
    persist: {
      key: 'systemConfig',
      pick: ['frontendUrl', 'rejectReasons', 'banReasons'],
    },
  }
)
