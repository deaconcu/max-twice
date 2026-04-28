import apiClient from '../client'

/**
 * 搜索结果项
 */
export interface SearchResultItem {
  id: number
  name: string
  description?: string
}

/**
 * 搜索 API
 */
export const searchApi = {
  /**
   * 搜索课程
   */
  searchCourses(query: string, limit = 20, offset = 0): Promise<SearchResultItem[]> {
    return apiClient.get('/search/courses', { params: { q: query, limit, offset } })
  },

  /**
   * 搜索节点
   */
  searchNodes(query: string, limit = 20, offset = 0): Promise<SearchResultItem[]> {
    return apiClient.get('/search/nodes', { params: { q: query, limit, offset } })
  },

  /**
   * 搜索用户
   */
  searchUsers(query: string, limit = 20, offset = 0): Promise<SearchResultItem[]> {
    return apiClient.get('/search/users', { params: { q: query, limit, offset } })
  },

  /**
   * 搜索角色
   */
  searchRoles(query: string, limit = 20, offset = 0): Promise<SearchResultItem[]> {
    return apiClient.get('/search/roles', { params: { q: query, limit, offset } })
  },

  /**
   * 全局搜索（所有类型）
   */
  searchAll(
    query: string,
    limit = 10
  ): Promise<{
    courses: SearchResultItem[]
    nodes: SearchResultItem[]
    users: SearchResultItem[]
    roles: SearchResultItem[]
  }> {
    return apiClient.get('/search/all', { params: { q: query, limit } })
  },
}
