/**
 * 支持的语言列表
 */
export const SUPPORTED_LOCALES = ['zh', 'en'] as const

/**
 * 默认语言
 */
export const DEFAULT_LOCALE = 'zh' as const

/**
 * Fallback 语言
 */
export const FALLBACK_LOCALE = 'zh' as const

/**
 * 语言类型
 */
export type Locale = (typeof SUPPORTED_LOCALES)[number]

/**
 * localStorage 中存储语言的 key
 */
export const LOCALE_STORAGE_KEY = 'locale'
