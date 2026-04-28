import apiClient from '../client'
import type { Roadmap } from '@/types/roadmap'

/**
 * 路线图管理相关 API
 */
export const roadmapApi = {
  /**
   * 获取角色的路线图列表
   */
  getRoleRoadmaps(roleId: number, cursor?: string, sortBy?: string): Promise<Roadmap[]> {
    const params: Record<string, string> = {}
    if (cursor != null) params.cursor = cursor
    if (sortBy) params.sortBy = sortBy
    return apiClient.get(`/roles/${String(roleId)}/roadmaps`, { params })
  },

  /**
   * 更新路线图
   */
  updateRoadmap(
    id: number,
    content: string,
    description: string,
    state: number
  ): Promise<Roadmap> {
    return apiClient.put(`/roadmaps/${String(id)}`, { content, description, state })
  },

  /**
   * 创建路线图
   */
  createRoadmap(
    roleId: number,
    content: string,
    description: string,
    state: number
  ): Promise<Roadmap> {
    return apiClient.post('/roadmaps', {
      roleId,
      content,
      description,
      state,
    })
  },

  /**
   * 获取路线图详情
   */
  getRoadmap(id: number): Promise<Roadmap> {
    return apiClient.get(`/roadmaps/${String(id)}`)
  },

  /**
   * 置顶路线图
   */
  pinRoadmap(roleId: number, roadmapId: number): Promise<boolean> {
    return apiClient.post('/roadmaps/pin', {
      roleId,
      roadmapId,
    })
  },

  /**
   * 更新路线图描述
   */
  updateRoadmapDescription(id: number, description: string): Promise<Roadmap> {
    return apiClient.put(`/roadmaps/${String(id)}/description`, { description })
  },
}
