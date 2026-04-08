import apiClient from '../client'
import type { ApiResponse, KeysetPageResponse } from '@/types/api'
import type { Role } from '@/types/role'

/**
 * 角色管理相关 API
 */
export const roleApi = {
  /**
   * 获取角色列表（分页）
   * @param lastId 分页游标
   * @param mainCategory 主分类ID（可选）
   * @param subCategory 子分类ID（可选）
   * 不传分类参数时返回所有已发布角色
   */
  getRolesByCategory(
    lastId?: number,
    mainCategory?: number,
    subCategory?: number
  ): Promise<ApiResponse<KeysetPageResponse<Role>>> {
    return apiClient.get('/v1/roles', {
      params: { lastId, mainCategory, subCategory },
    })
  },

  /**
   * 获取角色详情
   */
  getRole(id: number): Promise<ApiResponse<Role>> {
    return apiClient.get(`/v1/roles/${String(id)}`)
  },

  /**
   * 创建角色
   */
  createRole(roleData: Partial<Role>): Promise<ApiResponse<Role>> {
    return apiClient.post('/v1/roles', roleData)
  },

  /**
   * 更新角色（管理接口）
   */
  updateRole(id: number, roleData: Partial<Role>): Promise<ApiResponse<Role>> {
    return apiClient.put(`/v1/admin/contents/role/${String(id)}`, roleData)
  },

  /**
   * 审核角色（管理接口）
   */
  approveRole(id: number, action: string, reason?: string): Promise<ApiResponse<void>> {
    return apiClient.post(`/v1/admin/contents/role/${String(id)}/operate`, {
      action,
      reason,
    })
  },

  /**
   * 获取热门角色
   */
  getHotRoles(limit = 10): Promise<ApiResponse<Role[]>> {
    return apiClient.get('/v1/roles/hot', {
      params: { limit },
    })
  },

  /**
   * 搜索角色
   */
  searchRoles(keyword: string): Promise<ApiResponse<Role[]>> {
    return apiClient.get('/v1/roles/search', {
      params: { keyword },
    })
  },
}
