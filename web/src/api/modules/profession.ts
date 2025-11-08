import apiClient from '../client'
import type { ApiResponse } from '@/types/api'
import type { Profession } from '@/types/profession'
import type { ApprovalResponse } from '@/types/course'

/**
 * 职业管理相关 API
 * 参考：web-ts/src/services/api/v1/apiServiceV1.ts (professionServiceV1)
 */
export const professionApi = {
  /**
   * 分页获取职业列表
   */
  getProfessionsByPage(page = 1): Promise<ApiResponse<Profession[]>> {
    return apiClient.get('/v1/professions', {
      params: { page },
    })
  },

  /**
   * 按分类获取职业（前端使用）
   */
  getProfessionsByCategory(
    lastId?: number,
    mainCategory?: number,
    subCategory?: number
  ): Promise<ApiResponse<Profession[]>> {
    return apiClient.get('/v1/professions', {
      params: { lastId, mainCategory, subCategory },
    })
  },

  /**
   * 获取已审核通过的职业列表
   */
  getApprovedProfessions(lastId?: number | null): Promise<ApiResponse<Profession[]>> {
    const params = lastId !== undefined && lastId !== null ? { lastId } : {}
    return apiClient.get('/v1/professions/approved', { params })
  },

  /**
   * 获取职业详情
   */
  getProfession(id: number): Promise<ApiResponse<Profession>> {
    return apiClient.get(`/v1/professions/${String(id)}`)
  },

  /**
   * 创建职业
   */
  createProfession(professionData: Partial<Profession>): Promise<ApiResponse<Profession>> {
    return apiClient.post('/v1/professions', professionData)
  },

  /**
   * 更新职业
   */
  updateProfession(
    id: number,
    professionData: Partial<Profession>
  ): Promise<ApiResponse<Profession>> {
    return apiClient.put(`/v1/admin/professions/${String(id)}`, professionData)
  },

  /**
   * 审核职业
   */
  approveProfession(
    id: number,
    action: string,
    reason?: string
  ): Promise<ApiResponse<ApprovalResponse>> {
    return apiClient.post(`/v1/admin/professions/${String(id)}/approve`, {
      action,
      reason,
    })
  },

  /**
   * 删除职业
   */
  deleteProfession(id: number): Promise<ApiResponse<void>> {
    return apiClient.delete(`/v1/professions/${String(id)}`)
  },

  /**
   * 获取热门职业
   */
  getHotProfessions(limit = 10): Promise<ApiResponse<Profession[]>> {
    return apiClient.get('/v1/professions/hot', {
      params: { limit },
    })
  },
}
