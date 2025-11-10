import { useI18n as useVueI18n } from 'vue-i18n'
import { LOCALE_STORAGE_KEY, type Locale } from '@/constants/locale'

/**
 * 国际化 composable
 * 封装 vue-i18n 的 useI18n，提供类型安全的翻译函数
 */
export function useI18n() {
  const { t, locale, availableLocales } = useVueI18n()

  /**
   * 切换语言
   */
  const switchLocale = (newLocale: Locale) => {
    if (availableLocales.includes(newLocale)) {
      locale.value = newLocale
      // 持久化到 localStorage
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem(LOCALE_STORAGE_KEY, newLocale)
      }
      // 更新 HTML lang 属性
      if (typeof document !== 'undefined') {
        document.documentElement.lang = newLocale
      }
    }
  }

  /**
   * 获取当前语言
   */
  const currentLocale = (): Locale => locale.value as Locale

  return {
    t,
    locale,
    availableLocales,
    switchLocale,
    currentLocale,
  }
}
