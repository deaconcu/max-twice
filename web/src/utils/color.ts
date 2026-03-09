/**
 * 颜色工具函数
 */

const COLORS = ['purple', 'orange', 'teal', 'pink', 'info', 'success', 'warning', 'primary']

/**
 * 根据字符串生成稳定的颜色
 * 同一字符串总是返回相同颜色
 */
export function getColorByString(str: string): string {
  if (!str) return COLORS[0]
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash)
  }
  return COLORS[Math.abs(hash) % COLORS.length]
}
