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
  getProfessionRoadmaps(professionId: number, lastId?: number, sortBy?: string): Promise<ApiResponse<Roadmap[]>> {
    const params: Record<string, string | number> = {}
    if (lastId != null) {
      params.lastId = lastId
    }
    if (sortBy) {
      params.sortBy = sortBy
    }
    return apiClient.get(`/v1/professions/${String(professionId)}/roadmaps`, { params })
  },

  /**
   * 更新路线图
   */
  updateRoadmap(
    id: number,
    content: string,
    description: string,
    state: number
  ): Promise<ApiResponse<Roadmap>> {
    return apiClient.put(`/v1/roadmaps/${String(id)}`, { content, description, state })
  },

  /**
   * 创建路线图
   */
  createRoadmap(
    professionId: number,
    content: string,
    description: string,
    state: number
  ): Promise<ApiResponse<Roadmap>> {
    return apiClient.post('/v1/roadmaps', {
      professionId,
      content,
      description,
      state,
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
   * 更新路线图描述
   */
  updateRoadmapDescription(id: number, description: string): Promise<ApiResponse<Roadmap>> {
    return apiClient.put(`/v1/roadmaps/${String(id)}/description`, { description })
  },
}
