import { createI18n } from 'vue-i18n'
import zh from './locales/zh.json'
import en from './locales/en.json'

/**
 * 获取默认语言
 * 优先级：环境变量 > 域名 > localStorage > 浏览器语言 > 默认中文
 */
function getDefaultLocale(): 'zh' | 'en' {
  // 1. 检查环境变量
  const envLocale = import.meta.env.VITE_LOCALE
  if (envLocale === 'zh') {
    return 'zh'
  }
  if (envLocale === 'en') {
    return 'en'
  }

  // 2. 检查域名
  const { hostname } = window.location
  if (hostname.includes('zh.')) {
    return 'zh'
  } else if (hostname.includes('en.')) {
    return 'en'
  }

  // 3. 检查 localStorage
  const savedLocale = localStorage.getItem('locale')
  if (savedLocale === 'zh') {
    return 'zh'
  }
  if (savedLocale === 'en') {
    return 'en'
  }

  // 4. 检查浏览器语言
  const browserLang = navigator.language.toLowerCase()
  if (browserLang.startsWith('zh')) {
    return 'zh'
  } else if (browserLang.startsWith('en')) {
    return 'en'
  }

  // 5. 默认中文
  return 'zh'
}

/**
 * 创建 i18n 实例
 */
const i18n = createI18n({
  legacy: false, // 使用 Composition API 模式
  locale: getDefaultLocale(),
  fallbackLocale: 'zh',
  messages: {
    zh,
    en,
  },
  globalInjection: true, // 全局注入 $t
})

export default i18n
