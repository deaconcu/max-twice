import i18n from '@/i18n'

/**
 * 格式化相对时间
 * @param timeStr 时间字符串
 * @returns 格式化后的相对时间，如"刚刚"、"5分钟前"、"3天前"等
 */
export const formatRelativeTime = (timeStr?: string): string => {
  if (!timeStr) return ''

  const time = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - time.getTime()

  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  const months = Math.floor(days / 30)
  const years = Math.floor(days / 365)

  const t = i18n.global.t
  if (minutes < 1) return t('time.justNow')
  if (minutes < 60) return t('time.minutesAgo', { n: minutes })
  if (hours < 24) return t('time.hoursAgo', { n: hours })
  if (days < 30) return t('time.daysAgo', { n: days })
  if (months < 12) return t('time.monthsAgo', { n: months })
  return t('time.yearsAgo', { n: years })
}
