import apiClient from '../client'
import type { ApiResponse } from '@/types/api'
import type { User } from '@/types/user'
import type { Post } from '@/types/post'
import type { Roadmap } from '@/types/roadmap'

/**
 * 用户管理相关 API
 * 参考：web-ts/src/services/api/v1/apiServiceV1.ts (userServiceV1)
 */
export const userApi = {
  /**
   * 获取当前用户信息
   */
  getCurrentUser(): Promise<ApiResponse<User>> {
    return apiClient.get('/v1/users/current')
  },

  /**
   * 更新当前用户信息
   */
  updateCurrentUser(
    name: string,
    biography: string,
    avatar?: string
  ): Promise<ApiResponse<User>> {
    const data: any = { name, biography }
    if (avatar) {
      data.avatar = avatar
    }
    return apiClient.put('/v1/users/current', data)
  },

  /**
   * 更新用户头像
   */
  updateAvatar(file: File): Promise<ApiResponse<string>> {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient.post('/v1/users/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },

  /**
   * 根据用户名获取用户信息
   */
  getUser(username: string): Promise<ApiResponse<User>> {
    return apiClient.get(`/v1/users/${username}`)
  },

  /**
   * 搜索用户
   */
  searchUser(name: string): Promise<ApiResponse<User[]>> {
    return apiClient.get('/v1/users/search', {
      params: { name },
    })
  },

  /**
   * 获取用户的帖子列表
   */
  getUserPosts(userId: number, lastId?: number, type = 2): Promise<ApiResponse<Post[]>> {
    return apiClient.get(`/v1/users/${String(userId)}/posts`, {
      params: { lastId, type },
    })
  },

  /**
   * 获取当前用户的所有帖子
   */
  getCurrentUserAllPosts(
    lastId?: number,
    type?: number
  ): Promise<
    ApiResponse<{
      items: Post[]
      hasMore: boolean
      nextCursor?: {
        lastScore?: number
        lastId?: number
      }
    }>
  > {
    const params: { lastId?: number; type?: number } = {}
    if (lastId !== undefined && lastId !== null) {
      params.lastId = lastId
    }
    if (type !== undefined && type !== null) {
      params.type = type
    }
    return apiClient.get('/v1/users/me/posts', { params })
  },

  /**
   * 获取当前用户的路线图列表
   */
  getCurrentUserRoadmaps(lastId?: number): Promise<ApiResponse<Roadmap[]>> {
    return apiClient.get('/v1/users/me/roadmaps', {
      params: { lastId },
    })
  },

  /**
   * 获取用户的路线图列表
   */
  getUserRoadmaps(userId: number, lastId?: number): Promise<ApiResponse<Roadmap[]>> {
    return apiClient.get(`/v1/users/${String(userId)}/roadmaps`, {
      params: { lastId },
    })
  },

  /**
   * 删除路线图
   */
  deleteRoadmap(roadmapId: number): Promise<ApiResponse<void>> {
    return apiClient.delete(`/v1/roadmaps/${String(roadmapId)}`)
  },
}

/**
 * 关注相关 API
 * 参考：web-ts/src/services/api/v1/apiServiceV1.ts (followServiceV1)
 */
export const followApi = {
  /**
   * 关注用户
   */
  follow(followeeId: number): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/follows', {
      followeeId,
    })
  },

  /**
   * 取消关注
   */
  unfollow(followeeId: number): Promise<ApiResponse<void>> {
    return apiClient.delete(`/v1/follows/${String(followeeId)}`)
  },

  /**
   * 获取用户关注的人列表
   */
  getFollowees(userId: number, lastCreateTime?: string): Promise<ApiResponse<User[]>> {
    const params: { lastCreateTime?: string } = {}
    if (lastCreateTime) {
      params.lastCreateTime = lastCreateTime
    }
    return apiClient.get(`/v1/users/${String(userId)}/followees`, { params })
  },
}
