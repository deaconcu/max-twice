import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 支持的语言站
 */
export type SiteLanguage = 'zh' | 'en'

/**
 * 语言站配置
 */
export const SITE_LANGUAGES: { value: SiteLanguage; label: string }[] = [
  { value: 'zh', label: '中文站' },
  { value: 'en', label: 'English' },
]

/**
 * 站点 Store
 * 管理当前选择的语言站
 */
export const useSiteStore = defineStore(
  'site',
  () => {
    // 当前语言站，默认中文
    const currentLanguage = ref<SiteLanguage>('zh')

    // 当前语言站的显示名称
    const currentLanguageLabel = computed(() => {
      const lang = SITE_LANGUAGES.find((l) => l.value === currentLanguage.value)
      return lang?.label || '中文站'
    })

    /**
     * 切换语言站
     */
    const setLanguage = (lang: SiteLanguage) => {
      currentLanguage.value = lang
    }

    return {
      currentLanguage,
      currentLanguageLabel,
      setLanguage,
    }
  },
  {
    persist: {
      key: 'site',
      pick: ['currentLanguage'],
    },
  }
)
