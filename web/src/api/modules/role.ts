import apiClient from '../client'
import type { Role } from '@/types/role'
import type { CursorPage } from '@/types/api'

/**
 * 角色重新提交 / 更新请求体（与 admin update 共用）
 */
export interface RoleUpdateRequest {
  name: string
  description?: string
  icon?: string
  skills?: string
  mainCategory: number
  subCategory: number
}

/**
 * 角色管理相关 API
 */
export const roleApi = {
  /**
   * 获取角色列表（分页）
   * @param cursor 分页游标
   * @param mainCategory 主分类ID（可选）
   * @param subCategory 子分类ID（可选）
   * 不传分类参数时返回所有已发布角色
   */
  getRolesByCategory(
    cursor?: string,
    mainCategory?: number,
    subCategory?: number
  ): Promise<CursorPage<Role>> {
    return apiClient.get('/roles', {
      params: { cursor, mainCategory, subCategory },
    })
  },

  /**
   * 获取角色详情
   */
  getRole(id: number): Promise<Role> {
    return apiClient.get(`/roles/${String(id)}`)
  },

  /**
   * 创建角色
   */
  createRole(roleData: Partial<Role>): Promise<Role> {
    return apiClient.post('/roles', roleData)
  },

  /**
   * 更新角色（管理接口）
   */
  updateRole(id: number, roleData: Partial<Role>): Promise<Role> {
    return apiClient.put(`/admin/contents/role/${String(id)}`, roleData)
  },

  /**
   * 审核角色（管理接口）
   */
  approveRole(id: number, action: string, reason?: string): Promise<void> {
    return apiClient.post(`/admin/contents/role/${String(id)}/operate`, {
      action,
      reason,
    })
  },

  /**
   * 获取热门角色
   */
  getHotRoles(limit = 10): Promise<Role[]> {
    return apiClient.get('/roles/hot', {
      params: { limit },
    })
  },

  /**
   * 搜索角色
   */
  searchRoles(keyword: string): Promise<Role[]> {
    return apiClient.get('/roles/search', {
      params: { keyword },
    })
  },

  /**
   * 当前用户的角色申请列表
   * @param state 角色主体状态：NEVER_PUBLISHED | PUBLISHED（BANNED 由后端拦截）
   */
  getCurrentUserRoles(cursor?: string, state?: string): Promise<CursorPage<Role>> {
    const params: { cursor?: string; state?: string } = {}
    if (cursor !== undefined) params.cursor = cursor
    if (state !== undefined) params.state = state
    return apiClient.get('/users/me/roles', { params })
  },

  /**
   * 重新提交（被驳回 / 撤回后再申请）
   */
  resubmitRole(id: number, data: RoleUpdateRequest): Promise<void> {
    return apiClient.post(`/roles/${String(id)}/resubmit`, data)
  },

  /**
   * 作者撤回 pending revision
   */
  withdrawRole(id: number): Promise<void> {
    return apiClient.post(`/roles/${String(id)}/withdraw`)
  },
}
