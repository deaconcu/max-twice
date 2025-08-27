import { createI18n } from 'vue-i18n'
import zh from './locales/zh.json'
import en from './locales/en.json'

// 获取默认语言，根据域名或环境变量
function getDefaultLocale() {
  // 检查环境变量
  if (import.meta.env.VITE_LOCALE) {
    return import.meta.env.VITE_LOCALE
  }
  
  // 检查域名
  const hostname = window.location.hostname
  if (hostname.includes('zh.')) {
    return 'zh'
  } else if (hostname.includes('en.')) {
    return 'en'
  }
  
  // 默认中文
  return 'zh'
}

const messages = {
  zh,
  en
}

const i18n = createI18n({
  legacy: false, // 使用 Composition API 模式
  locale: getDefaultLocale(),
  fallbackLocale: 'zh',
  messages
})

export default i18n