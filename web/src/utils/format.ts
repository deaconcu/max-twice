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

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  if (months < 12) return `${months}个月前`
  return `${years}年前`
}
