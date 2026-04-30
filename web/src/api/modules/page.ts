import apiClient from '../client'

// 从统一类型定义导入
export type { ReadResponse } from '@/types/page'
import type { ReadResponse } from '@/types/page'

/**
 * 页面聚合相关 API
 */
export const pageApi = {
  /**
   * 根据课程路径读取内容（完整版）
   * 用于 ContentReadPage，返回所有数据
   */
  readByCoursePath(courseId: number, path: string, cursor?: string): Promise<ReadResponse> {
    return apiClient.get('/pages/read', {
      params: { courseId, path, cursor },
    })
  },

  /**
   * 根据节点ID读取内容（完整版）
   * 支持 path 参数用于访问子节点
   */
  readByNode(nodeId: number, path?: string, cursor?: string): Promise<ReadResponse> {
    return apiClient.get('/pages/read', {
      params: { nodeId, path, cursor },
    })
  },

  /**
   * 根据帖子ID读取内容（完整版）
   * @deprecated 使用 readPostDetail 代替，性能更好
   */
  readByPost(postId: number): Promise<ReadResponse> {
    return apiClient.get('/pages/read', {
      params: { postId },
    })
  },

  /**
   * 根据评论ID读取内容（完整版）
   * @deprecated 使用 readPostDetail 代替，性能更好
   */
  readByComment(commentId: number): Promise<ReadResponse> {
    return apiClient.get('/pages/read', {
      params: { commentId },
    })
  },

  /**
   * 读取节点帖子列表（优化版）
   * 仅返回 NodePostsPage 需要的数据，不包含 TOC 和固定帖子
   */
  readNodePosts(nodeId: number): Promise<ReadResponse> {
    return apiClient.get('/pages/node', {
      params: { nodeId },
    })
  },

  /**
   * 读取帖子详情（优化版）
   * 仅返回 PostDetailPage 需要的数据，不包含 TOC 和帖子列表
   * 支持通过 postId 或 commentId 定位
   */
  readPostDetail(params: { postId?: number; commentId?: number }): Promise<ReadResponse> {
    return apiClient.get('/pages/post', {
      params,
    })
  },
}
