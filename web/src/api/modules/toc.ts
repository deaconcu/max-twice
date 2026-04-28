import apiClient from '../client'

/**
 * TOC (Table of Contents) API
 */
export const tocApi = {
  /**
   * 更新用户节点目录组
   */
  updateUserNodeToc(nodeId: number, indexArray: string): Promise<void> {
    return apiClient.put(`/users/current/nodes/${nodeId}/toc`, {
      indexArray,
    })
  },

  /**
   * 获取用户节点目录
   */
  getUserNodeToc(nodeId: number): Promise<string> {
    return apiClient.get(`/users/current/nodes/${nodeId}/toc`)
  },
}
