import apiClient from '../client'
import type { ApiResponse } from '@/types/api'
import type { ReadResponse } from '@/types/page'

/**
 * 页面聚合相关 API
 * 参考：web-ts/src/services/api/v1/apiServiceV1.ts (pageServiceV1)
 */
export const pageApi = {
  /**
   * 根据课程路径读取内容
   */
  readByCoursePath(courseId: number, path: string): Promise<ApiResponse<ReadResponse>> {
    return apiClient.get('/v1/pages/read', {
      params: { courseId, path },
    })
  },

  /**
   * 根据节点ID读取内容
   */
  readByNode(nodeId: number): Promise<ApiResponse<ReadResponse>> {
    return apiClient.get('/v1/pages/read', {
      params: { nodeId },
    })
  },

  /**
   * 根据帖子ID读取内容
   */
  readByPost(postId: number): Promise<ApiResponse<ReadResponse>> {
    return apiClient.get('/v1/pages/read', {
      params: { postId },
    })
  },

  /**
   * 根据评论ID读取内容
   */
  readByComment(commentId: number): Promise<ApiResponse<ReadResponse>> {
    return apiClient.get('/v1/pages/read', {
      params: { commentId },
    })
  },
}
