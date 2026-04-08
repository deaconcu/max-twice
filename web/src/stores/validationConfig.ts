import { defineStore } from 'pinia'
import { ref } from 'vue'
import { apiClient } from '@/api/client'
import i18n from '@/i18n'

export interface ValidationRule {
  minLength: number
  maxLength: number
  label: string
}

export type ValidationConfig = Record<string, ValidationRule>

/**
 * 验证规则配置 Store
 * 从后端获取验证规则，统一管理前端表单验证
 */
export const useValidationConfigStore = defineStore(
  'validationConfig',
  () => {
    const config = ref<ValidationConfig | null>(null)
    const lastChecked = ref<number>(0)
    const loading = ref(false)
    const initialized = ref(false)

    const CHECK_INTERVAL = 3 * 60 * 1000 // 3 分钟

    /**
     * 初始化配置（应用启动时调用一次）
     */
    async function init() {
      if (initialized.value) return

      // Pinia 持久化插件会自动从 localStorage 恢复 config
      // 如果有缓存，页面可以立即使用

      // 后台检查更新
      await checkAndLoad()
      initialized.value = true
    }

    /**
     * 检查并加载配置（带节流）
     * 在路由切换时调用，避免频繁请求
     */
    async function checkAndLoad() {
      const now = Date.now()

      // 如果最近检查过，跳过
      if (lastChecked.value && now - lastChecked.value < CHECK_INTERVAL) {
        console.log(
          `[ValidationConfig] 距离上次检查仅 ${Math.round((now - lastChecked.value) / 1000)}s，跳过`
        )
        return config.value
      }

      // 执行检查
      console.log('[ValidationConfig] 检查配置更新...')
      await loadConfig()
      lastChecked.value = now

      return config.value
    }

    /**
     * 加载配置（内部方法）
     * axios 会自动处理 ETag:
     * - 配置未变：返回 304，使用缓存
     * - 配置变了：返回 200，更新数据
     */
    async function loadConfig() {
      loading.value = true
      try {
        const response = await apiClient.get<ValidationConfig>('/v1/config/validation')

        if (response.data) {
          const oldConfig = JSON.stringify(config.value)
          const newConfig = JSON.stringify(response.data)

          if (oldConfig !== newConfig) {
            console.log('[ValidationConfig] ✅ 配置已更新')
            config.value = response.data
          } else {
            console.log('[ValidationConfig] 配置无变化')
          }
        }
      } catch (error) {
        console.error('[ValidationConfig] 加载配置失败', error)
        // 失败时继续使用 localStorage 中的旧配置
      } finally {
        loading.value = false
      }
    }

    /**
     * 手动刷新配置（供开发者使用）
     */
    async function refresh() {
      lastChecked.value = 0 // 重置检查时间
      await loadConfig()
    }

    /**
     * 获取指定字段的规则
     */
    function getRule(fieldKey: string): ValidationRule | null {
      return config.value?.[fieldKey] || null
    }

    /**
     * 创建 Vuetify 验证规则
     */
    function createRules(fieldKey: string): ((v: string) => boolean | string)[] {
      const rule = getRule(fieldKey)
      if (!rule) {
        console.warn(`[ValidationConfig] 未找到字段的验证规则: ${fieldKey}`)
        return []
      }

      const rules: ((v: string) => boolean | string)[] = []

      // 必填验证
      if (rule.minLength > 0) {
        rules.push((v) => !!v || i18n.global.t('validation.cannotBeEmpty', { label: rule.label }))
      }

      // 最小长度验证
      if (rule.minLength > 0) {
        rules.push(
          (v) =>
            (v && v.length >= rule.minLength) ||
            i18n.global.t('validation.minLength', { label: rule.label, min: rule.minLength })
        )
      }

      // 最大长度验证
      rules.push(
        (v) =>
          !v ||
          v.length <= rule.maxLength ||
          i18n.global.t('validation.maxLength', { label: rule.label, max: rule.maxLength })
      )

      return rules
    }

    /**
     * 清除缓存（供调试使用）
     */
    function clearCache() {
      config.value = null
      lastChecked.value = 0
      initialized.value = false
      console.log('[ValidationConfig] 缓存已清除')
    }

    return {
      config,
      lastChecked,
      loading,
      initialized,
      init,
      checkAndLoad,
      refresh,
      getRule,
      createRules,
      clearCache,
    }
  },
  {
    persist: {
      key: 'validationConfig',
      paths: ['config', 'lastChecked'], // 持久化配置和最后检查时间
    },
  }
)
