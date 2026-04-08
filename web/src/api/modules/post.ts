import apiClient from '../client'
import type { ApiResponse, KeysetPageResponse } from '@/types/api'
import type { Post } from '@/types/post'
import type { Node } from '@/types/node'

/**
 * 帖子管理相关 API
 * 参考：web-ts/src/services/api/v1/apiServiceV1.ts (postServiceV1)
 */
export const postApi = {
  /**
   * 按 IDs 批量获取帖子
   */
  getPostsByIds(ids: number[]): Promise<ApiResponse<Post[]>> {
    return apiClient.get('/v1/posts', {
      params: {
        ids: ids.join(','),
      },
    })
  },

  /**
   * 获取节点帖子列表（分页）
   */
  getNodePosts(
    nodeId: number,
    lastScore?: number,
    lastId?: number
  ): Promise<ApiResponse<KeysetPageResponse<Post>>> {
    return apiClient.get('/v1/posts', {
      params: {
        nodeId,
        lastScore,
        lastId,
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
   * 内容操作（选择目录）
   * action: 1=设置为目录, 2=取消设置为目录
   */
  operateContent(data: {
    path: string
    nodeId: number
    postingId: number
    action: number
  }): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/contents', data)
  },

  /**
   * 搜索相似节点
   */
  searchSimilarNodes(query: string, topK = 10, threshold = 0.0): Promise<ApiResponse<Node[]>> {
    return apiClient.get('/v1/nodes/search', {
      params: {
        query,
        topK,
        threshold,
      },
    })
  },

  /**
   * 检查课程内是否存在同名已发布节点
   */
  checkDuplicateNode(courseId: number, name: string): Promise<ApiResponse<boolean>> {
    return apiClient.get('/v1/nodes/check-duplicate', {
      params: {
        courseId,
        name,
      },
    })
  },

  /**
   * 初始化节点 Embedding（管理员）
   */
  initNodeEmbeddings(
    batchSize = 20
  ): Promise<ApiResponse<{ successCount: number; failCount: number; totalProcessed: number }>> {
    return apiClient.post('/v1/admin/contents/nodes/init-embeddings', null, {
      params: {
        batchSize,
      },
    })
  },
}
