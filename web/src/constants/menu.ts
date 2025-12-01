import type { MenuItem, ToolButton } from '@/types/menu'

/**
 * 主导航菜单项
 */
export const MAIN_MENU_ITEMS: MenuItem[] = [
  {
    icon: 'mdi-home-variant-outline',
    textKey: 'nav.home',
    path: '/',
  },
  {
    icon: 'mdi-briefcase-search-outline',
    textKey: 'nav.career',
    path: '/career',
  },
  {
    icon: 'mdi-book-open-page-variant-outline',
    textKey: 'nav.courses',
    path: '/courses',
  },
  {
    icon: 'mdi-brain',
    textKey: 'nav.memoryReview',
    path: '/review',
  },
  {
    icon: 'mdi-account-circle-outline',
    textKey: 'nav.profile',
    path: '/users/me',
  },
]

/**
 * 底部工具按钮
 */
export const BOTTOM_TOOLS: ToolButton[] = [
  {
    icon: 'mdi-cog',
    titleKey: 'common.settings',
    path: '/settings',
  },
  {
    icon: 'mdi-shield-lock',
    titleKey: 'common.privacy',
    path: '/privacy',
  },
  {
    icon: 'mdi-star',
    titleKey: 'common.favorites',
    path: '/favorites',
  },
  {
    icon: 'mdi-help-circle',
    titleKey: 'common.help',
    path: '/help',
  },
]
