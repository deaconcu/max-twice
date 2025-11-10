import type { User } from '@/types/user'
import { UserRole } from '@/enums'

/**
 * 权限检查工具函数
 */

/**
 * 检查是否是超级管理员
 */
export function isSuperAdmin(user: User | null): boolean {
  return user?.role === UserRole.SUPER_ADMIN
}

/**
 * 检查是否是管理员（包括超级管理员）
 */
export function isAdmin(user: User | null): boolean {
  return (user?.role ?? UserRole.USER) >= UserRole.ADMIN
}

/**
 * 检查是否是审核员（包括管理员和超级管理员）
 */
export function isModerator(user: User | null): boolean {
  return (user?.role ?? UserRole.USER) >= UserRole.MODERATOR
}
