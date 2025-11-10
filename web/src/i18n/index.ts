import { createI18n } from 'vue-i18n'
import {
  SUPPORTED_LOCALES,
  DEFAULT_LOCALE,
  FALLBACK_LOCALE,
  LOCALE_STORAGE_KEY,
  type Locale,
} from '@/constants/locale'
import zh from './locales/zh.json'
import en from './locales/en.json'

/**
 * 检查是否为有效的语言代码
 */
function isValidLocale(locale: string): locale is Locale {
  return SUPPORTED_LOCALES.includes(locale as Locale)
}

/**
 * 获取默认语言
 * 优先级：环境变量 > 域名 > localStorage > 浏览器语言 > 默认中文
 */
function getDefaultLocale(): Locale {
  // 1. 检查环境变量
  const envLocale = import.meta.env.VITE_LOCALE
  if (envLocale && isValidLocale(envLocale)) {
    return envLocale
  }

  // 2. 检查域名（仅在浏览器环境）
  if (typeof window !== 'undefined') {
    const { hostname } = window.location
    for (const locale of SUPPORTED_LOCALES) {
      if (hostname.includes(`${locale}.`)) {
        return locale
      }
    }
  }

  // 3. 检查 localStorage（仅在浏览器环境）
  if (typeof localStorage !== 'undefined') {
    const savedLocale = localStorage.getItem(LOCALE_STORAGE_KEY)
    if (savedLocale && isValidLocale(savedLocale)) {
      return savedLocale
    }
  }

  // 4. 检查浏览器语言（仅在浏览器环境）
  if (typeof navigator !== 'undefined') {
    const browserLang = navigator.language.toLowerCase()
    for (const locale of SUPPORTED_LOCALES) {
      if (browserLang.startsWith(locale)) {
        return locale
      }
    }
  }

  // 5. 返回默认语言
  return DEFAULT_LOCALE
}

/**
 * 创建 i18n 实例
 */
const i18n = createI18n({
  legacy: false, // 使用 Composition API 模式
  locale: getDefaultLocale(),
  fallbackLocale: FALLBACK_LOCALE,
  messages: {
    zh,
    en,
  },
  globalInjection: false, // 不使用全局注入，通过 useI18n composable 使用
})

export default i18n
