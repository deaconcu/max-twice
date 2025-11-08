import apiClient from '../client'
import type { ApiResponse } from '@/types/api'
import type { Roadmap } from '@/types/roadmap'

/**
 * 路线图管理相关 API
 * 参考：web-ts/src/services/api/v1/apiServiceV1.ts (roadmapServiceV1)
 */
export const roadmapApi = {
  /**
   * 获取职业的路线图列表
   */
  getProfessionRoadmaps(professionId: number, lastId?: number): Promise<ApiResponse<Roadmap[]>> {
    const params: Record<string, number> = {}
    if (lastId != null) {
      params.lastId = lastId
    }
    return apiClient.get(`/v1/professions/${String(professionId)}/roadmaps`, { params })
  },

  /**
   * 更新路线图
   */
  updateRoadmap(id: number, content: string): Promise<ApiResponse<Roadmap>> {
    return apiClient.put(`/v1/roadmaps/${String(id)}`, { content })
  },

  /**
   * 创建路线图
   */
  createRoadmap(
    professionId: number,
    content: string,
    description: string
  ): Promise<ApiResponse<Roadmap>> {
    return apiClient.post('/v1/roadmaps', {
      professionId,
      content,
      description,
    })
  },

  /**
   * 获取路线图详情
   */
  getRoadmap(id: number): Promise<ApiResponse<Roadmap>> {
    return apiClient.get(`/v1/roadmaps/${String(id)}`)
  },

  /**
   * 置顶路线图
   */
  pinRoadmap(professionId: number, roadmapId: number): Promise<ApiResponse<boolean>> {
    return apiClient.post('/v1/roadmaps/pin', {
      professionId,
      roadmapId,
    })
  },

  /**
   * 审核路线图
   */
  approveRoadmap(id: number, action: string, reason?: string): Promise<ApiResponse<Roadmap>> {
    return apiClient.post(`/v1/roadmaps/${String(id)}/approve`, {
      action,
      reason,
    })
  },

  /**
   * 更新路线图描述
   */
  updateRoadmapDescription(id: number, description: string): Promise<ApiResponse<Roadmap>> {
    return apiClient.put(`/v1/roadmaps/${String(id)}/description`, { description })
  },
}
