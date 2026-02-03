import apiClient from '../client'
import type { ApiResponse } from '@/types/api'

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
  searchCourses(
    query: string,
    limit: number = 20,
    offset: number = 0
  ): Promise<ApiResponse<SearchResultItem[]>> {
    return apiClient.get('/v1/search/courses', { params: { q: query, limit, offset } })
  },

  /**
   * 搜索节点
   */
  searchNodes(
    query: string,
    limit: number = 20,
    offset: number = 0
  ): Promise<ApiResponse<SearchResultItem[]>> {
    return apiClient.get('/v1/search/nodes', { params: { q: query, limit, offset } })
  },

  /**
   * 搜索用户
   */
  searchUsers(
    query: string,
    limit: number = 20,
    offset: number = 0
  ): Promise<ApiResponse<SearchResultItem[]>> {
    return apiClient.get('/v1/search/users', { params: { q: query, limit, offset } })
  },

  /**
   * 搜索职业
   */
  searchProfessions(
    query: string,
    limit: number = 20,
    offset: number = 0
  ): Promise<ApiResponse<SearchResultItem[]>> {
    return apiClient.get('/v1/search/professions', { params: { q: query, limit, offset } })
  },

  /**
   * 全局搜索（所有类型）
   */
  searchAll(query: string, limit: number = 10): Promise<
    ApiResponse<{
      courses: SearchResultItem[]
      nodes: SearchResultItem[]
      users: SearchResultItem[]
      professions: SearchResultItem[]
    }>
  > {
    return apiClient.get('/v1/search/all', { params: { q: query, limit } })
  },
}
