import apiClient from '../client'
import type { User } from '@/types/user'
import type { Post } from '@/types/post'
import type { Roadmap } from '@/types/roadmap'
import type { CursorPage } from '@/types/api'

/**
 * 用户管理相关 API
 */
export const userApi = {
  /**
   * 获取当前用户信息
   */
  getCurrentUser(): Promise<User> {
    return apiClient.get('/users/current')
  },

  /**
   * 更新当前用户信息
   */
  updateCurrentUser(
    name: string,
    biography: string,
    avatar?: string,
    timezone?: string
  ): Promise<User> {
    const data: Record<string, string> = { name, biography }
    if (avatar) {
      data.avatar = avatar
    }
    if (timezone) {
      data.timezone = timezone
    }
    return apiClient.put('/users/current', data)
  },

  /**
   * 更新用户头像
   */
  updateAvatar(file: File): Promise<string> {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient.post('/users/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },

  /**
   * 根据用户名获取用户信息
   */
  getUser(username: string): Promise<User> {
    return apiClient.get(`/users/${username}`)
  },

  /**
   * 搜索用户
   */
  searchUser(name: string): Promise<User[]> {
    return apiClient.get('/users/search', {
      params: { name },
    })
  },

  /**
   * 获取用户的帖子列表
   */
  getUserPosts(userId: number, cursor?: string, type = 2): Promise<CursorPage<Post>> {
    return apiClient.get(`/users/${String(userId)}/posts`, {
      params: { cursor, type },
    })
  },

  /**
   * 获取当前用户的所有帖子
   */
  getCurrentUserAllPosts(
    cursor?: string,
    type?: number,
    state?: number
  ): Promise<CursorPage<Post>> {
    const params: { cursor?: string; type?: number; state?: number } = {}
    if (cursor !== undefined) params.cursor = cursor
    if (type !== undefined) params.type = type
    if (state !== undefined) params.state = state
    return apiClient.get('/users/me/posts', { params })
  },

  /**
   * 获取当前用户的路线图列表
   */
  getCurrentUserRoadmaps(cursor?: string, state?: number): Promise<Roadmap[]> {
    const params: { cursor?: string; state?: number } = {}
    if (cursor !== undefined) params.cursor = cursor
    if (state !== undefined) params.state = state
    return apiClient.get('/users/me/roadmaps', { params })
  },

  /**
   * 获取用户的路线图列表
   */
  getUserRoadmaps(userId: number, cursor?: string): Promise<Roadmap[]> {
    return apiClient.get(`/users/${String(userId)}/roadmaps`, {
      params: { cursor },
    })
  },

  /**
   * 删除路线图
   */
  deleteRoadmap(roadmapId: number): Promise<void> {
    return apiClient.delete(`/roadmaps/${String(roadmapId)}`)
  },
}

/**
 * 关注相关 API
 */
export const followApi = {
  /**
   * 关注用户
   */
  follow(followeeId: number): Promise<void> {
    return apiClient.post('/follows', {
      followeeId,
    })
  },

  /**
   * 取消关注
   */
  unfollow(followeeId: number): Promise<void> {
    return apiClient.delete(`/follows/${String(followeeId)}`)
  },

  /**
   * 获取用户关注的人列表
   */
  getFollowees(userId: number, cursor?: string): Promise<User[]> {
    return apiClient.get(`/users/${String(userId)}/followees`, {
      params: { cursor },
    })
  },
}
