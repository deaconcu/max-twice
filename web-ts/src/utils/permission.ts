/**
 * 权限工具函数
 * 用于前端权限判断
 */

import type { User } from '@/types/user'
import { UserRole } from '@/types/user'

/**
 * 判断是否为管理员或更高级别
 */
export const isAdmin = (user: User | null | undefined): boolean => {
  if (!user || user.role === undefined) return false
  return user.role >= UserRole.ADMIN
}

/**
 * 判断是否为超级管理员
 */
export const isSuperAdmin = (user: User | null | undefined): boolean => {
  if (!user || user.role === undefined) return false
  return user.role === UserRole.SUPER_ADMIN
}

/**
 * 判断是否为审核员或更高级别
 */
export const isModerator = (user: User | null | undefined): boolean => {
  if (!user || user.role === undefined) return false
  return user.role >= UserRole.MODERATOR
}

/**
 * 判断是否可以编辑资源
 * @param user 当前用户
 * @param creatorId 资源创建者ID
 */
export const canEdit = (user: User | null | undefined, creatorId: number): boolean => {
  if (!user) return false
  // 管理员可以编辑所有内容，或者是资源的创建者
  return isAdmin(user) || user.id === creatorId
}

/**
 * 判断是否可以删除资源
 * @param user 当前用户
 * @param creatorId 资源创建者ID
 */
export const canDelete = (user: User | null | undefined, creatorId: number): boolean => {
  // 删除权限与编辑权限相同
  return canEdit(user, creatorId)
}

/**
 * 判断是否可以修改用户角色
 * @param operator 操作者
 * @param targetRole 目标角色
 */
export const canModifyRole = (operator: User | null | undefined, targetRole: UserRole): boolean => {
  if (!operator) return false

  // 只有超级管理员可以设置超级管理员
  if (targetRole === UserRole.SUPER_ADMIN) {
    return isSuperAdmin(operator)
  }

  // 管理员及以上可以设置其他角色
  return isAdmin(operator)
}

/**
 * 判断是否可以访问管理后台
 */
export const canAccessAdmin = (user: User | null | undefined): boolean => {
  return isModerator(user)
}

/**
 * 判断是否可以审核内容
 */
export const canModerate = (user: User | null | undefined): boolean => {
  return isModerator(user)
}

/**
 * 获取角色名称
 */
export const getRoleName = (role: UserRole | undefined): string => {
  if (role === undefined) return '普通用户'

  switch (role) {
    case UserRole.USER:
      return '普通用户'
    case UserRole.MODERATOR:
      return '审核员'
    case UserRole.ADMIN:
      return '管理员'
    case UserRole.SUPER_ADMIN:
      return '超级管理员'
    default:
      return '未知角色'
  }
}
