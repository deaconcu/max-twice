import type { User } from '@/types/user'

const USER_STATE_BANNED = 2

export function getUserDisplayName(user: User | null | undefined): string {
  if (!user) {
    return '未知用户'
  }

  if (user.state === USER_STATE_BANNED) {
    return '此账号已被暂停'
  }

  return user.name || '未知用户'
}

export function isUserBanned(user: User | null | undefined): boolean {
  return user?.state === USER_STATE_BANNED
}

/**
 * 获取对象类型的中文名称
 * @param objectType 对象类型 'post' | 'node' | 'roadmap' | 'comment' | 'memoryCardDeck'
 * @returns 对象类型的中文名称
 */
export function getObjectTypeName(objectType: string): string {
  const typeMap: Record<string, string> = {
    'post': '帖子',
    'node': '节点',
    'roadmap': '路线图',
    'comment': '评论',
    'memoryCardDeck': '记忆卡片组',
    'course': '课程',
    'profession': '职业'
  }
  return typeMap[objectType] || '内容'
}
