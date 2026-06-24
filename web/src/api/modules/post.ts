import apiClient from '../client'
import type { Post } from '@/types/post'
import type { Node } from '@/types/node'
import type { CursorPage } from '@/types/api'

/**
 * 帖子管理相关 API
 */
export const postApi = {
  /**
   * 按 IDs 批量获取帖子
   */
  getPostsByIds(ids: number[]): Promise<Post[]> {
    return apiClient.get('/posts/batch', {
      params: { ids },
    })
  },

  /**
   * 获取节点帖子列表（分页）
   */
  getNodePosts(nodeId: number, cursor?: string): Promise<CursorPage<Post>> {
    return apiClient.get(`/nodes/${String(nodeId)}/posts`, {
      params: { cursor },
    })
  },

  /**
   * 创建帖子
   */
  createPost(postData: Partial<Post>): Promise<Post> {
    return apiClient.post('/posts', postData)
  },

  /**
   * 更新帖子
   */
  updatePost(id: number, postData: Partial<Post>): Promise<Post> {
    return apiClient.put(`/posts/${String(id)}`, postData)
  },

  /**
   * 删除帖子
   */
  deletePost(id: number): Promise<void> {
    return apiClient.delete(`/posts/${String(id)}`)
  },

  /**
   * 获取帖子详情
   */
  getPost(id: number): Promise<Post> {
    return apiClient.get(`/posts/${String(id)}`)
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
  }): Promise<void> {
    return apiClient.post('/contents', data)
  },

  /**
   * 搜索相似节点
   */
  searchSimilarNodes(query: string, topK = 10, threshold = 0.0): Promise<Node[]> {
    return apiClient.get('/nodes/search', {
      params: { query, topK, threshold },
    })
  },

  /**
   * 检查课程内是否存在同名已发布节点
   */
  checkDuplicateNode(courseId: number, name: string): Promise<boolean> {
    return apiClient.get('/nodes/check-duplicate', {
      params: { courseId, name },
    })
  },

  /**
   * 初始化节点 Embedding（管理员）
   */
  initNodeEmbeddings(
    batchSize = 20
  ): Promise<{ successCount: number; failCount: number; totalProcessed: number }> {
    return apiClient.post('/admin/contents/nodes/init-embeddings', null, {
      params: { batchSize },
    })
  },
}
