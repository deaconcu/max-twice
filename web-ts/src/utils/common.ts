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
