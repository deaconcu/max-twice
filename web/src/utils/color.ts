/**
 * 颜色和图标工具函数
 */

const COLORS = ['purple', 'orange', 'teal', 'pink', 'info', 'success', 'warning', 'primary']

const COURSE_ICONS = [
  'mdi-language-python',
  'mdi-language-javascript',
  'mdi-language-java',
  'mdi-vuejs',
  'mdi-react',
  'mdi-nodejs',
  'mdi-database',
  'mdi-code-braces',
  'mdi-cloud',
  'mdi-server',
  'mdi-laptop',
  'mdi-book-open-variant',
]

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

/**
 * 根据ID生成稳定的颜色
 * 同一ID总是返回相同颜色
 */
export function getColorById(id: number): string {
  return COLORS[id % COLORS.length]
}

/**
 * 根据ID生成稳定的课程图标
 * 同一ID总是返回相同图标
 */
export function getCourseIconById(id: number): string {
  return COURSE_ICONS[id % COURSE_ICONS.length]
}
