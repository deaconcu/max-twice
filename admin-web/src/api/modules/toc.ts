import apiClient from '../client'
import type { ApiResponse } from '@/types/api'

/**
 * TOC (Table of Contents) API
 */
export const tocApi = {
  /**
   * 更新用户节点目录组
   */
  updateUserNodeToc(nodeId: number, indexArray: string): Promise<ApiResponse<void>> {
    return apiClient.put(`/v1/users/current/nodes/${nodeId}/toc`, {
      indexArray,
    })
  },

  /**
   * 获取用户节点目录
   */
  getUserNodeToc(nodeId: number): Promise<ApiResponse<string>> {
    return apiClient.get(`/v1/users/current/nodes/${nodeId}/toc`)
  },
}
