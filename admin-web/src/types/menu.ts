/**
 * 菜单项接口
 */
export interface MenuItem {
  icon: string
  textKey: string // i18n 翻译 key
  path: string
  permission?: string // 权限标识（可选）
}

/**
 * 工具按钮接口
 */
export interface ToolButton {
  icon: string
  titleKey: string // i18n 翻译 key
  path?: string // 跳转路径（可选）
  action?: () => void // 点击动作（可选）
}

/**
 * 通知接口
 */
export interface Notification {
  id: number
  title: string
  content: string
  time: string
  read: boolean
  icon: string
  iconColor: string
}
