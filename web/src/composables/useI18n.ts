import { useI18n as useVueI18n } from 'vue-i18n'

/**
 * 国际化 composable
 * 封装 vue-i18n 的 useI18n，提供类型安全的翻译函数
 */
export function useI18n() {
  const { t, locale, availableLocales } = useVueI18n()

  /**
   * 切换语言
   */
  const switchLocale = (newLocale: string) => {
    if (availableLocales.includes(newLocale)) {
      locale.value = newLocale
      localStorage.setItem('locale', newLocale)
      // 更新 HTML lang 属性
      document.documentElement.lang = newLocale
    }
  }

  /**
   * 获取当前语言
   */
  const currentLocale = () => locale.value

  return {
    t,
    locale,
    availableLocales,
    switchLocale,
    currentLocale,
  }
}
