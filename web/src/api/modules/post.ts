import apiClient from '../client'
import type { ApiResponse } from '@/types/api'
import type { Post } from '@/types/post'

/**
 * 帖子管理相关 API
 * 参考：web-ts/src/services/api/v1/apiServiceV1.ts (postServiceV1)
 */
export const postApi = {
  /**
   * 获取帖子列表
   */
  getPosts(
    ids?: number[],
    nodeId?: number,
    lastScore?: number,
    lastPostingId?: number
  ): Promise<ApiResponse<Post[]>> {
    return apiClient.get('/v1/posts', {
      params: {
        ids: ids?.join(','),
        nodeId,
        lastScore,
        lastId: lastPostingId,
      },
    })
  },

  /**
   * 创建帖子
   */
  createPost(postData: Partial<Post>): Promise<ApiResponse<Post>> {
    return apiClient.post('/v1/posts', postData)
  },

  /**
   * 更新帖子
   */
  updatePost(id: number, postData: Partial<Post>): Promise<ApiResponse<Post>> {
    return apiClient.put(`/v1/posts/${String(id)}`, postData)
  },

  /**
   * 删除帖子
   */
  deletePost(id: number): Promise<ApiResponse<void>> {
    return apiClient.delete(`/v1/posts/${String(id)}`)
  },

  /**
   * 获取帖子详情
   */
  getPost(id: number): Promise<ApiResponse<Post>> {
    return apiClient.get(`/v1/posts/${String(id)}`)
  },

  /**
   * 获取节点下的帖子列表
   */
  getNodePosts(nodeId: number): Promise<ApiResponse<Post[]>> {
    return apiClient.get(`/v1/nodes/${String(nodeId)}/posts`)
  },

  /**
   * 内容操作（选择目录、固定等）
   * action: 1=设置为目录, 2=取消设置为目录, 3=置顶, 4=取消置顶
   */
  operateContent(data: {
    path: string
    courseId: number
    postingId: number
    action: number
  }): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/contents', data)
  },
}
