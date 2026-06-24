import apiClient from '../client'

/**
 * 图片上传响应
 */
export interface ImageUploadResponse {
  fileUrl: string
}

/**
 * 配额使用情况
 */
export interface QuotaUsage {
  minuteUsed: number
  minuteLimit: number
  hourUsed: number
  hourLimit: number
  dailyUsed: number
  dailyLimit: number
}

/**
 * 图片上传历史
 */
export interface ImageUploadHistory {
  id: number
  fileUrl: string
  fileName: string
  fileSize: number
  refType: string
  refId: number | null
  status: number
  createdAt: string
  usedAt: string | null
}

/**
 * 图片上传相关 API
 */
export const imageApi = {
  /**
   * 上传图片
   * @param file 图片文件
   * @param refType 引用类型: post/comment/avatar/course/roadmap
   */
  upload(file: File, refType: string): Promise<ImageUploadResponse> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('refType', refType)

    return apiClient.post('/images/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },

  /**
   * 标记图片为使用中
   * @param fileUrls 图片URL列表
   * @param refId 引用资源ID
   */
  markAsUsed(fileUrls: string[], refId: number): Promise<void> {
    return apiClient.post('/images/mark-used', {
      fileUrls,
      refId,
    })
  },

  /**
   * 删除图片
   * @param fileUrl 图片URL
   */
  delete(fileUrl: string): Promise<void> {
    return apiClient.delete('/images', {
      params: { fileUrl },
    })
  },

  /**
   * 获取配额使用情况
   */
  getQuota(): Promise<QuotaUsage> {
    return apiClient.get('/images/quota')
  },

  /**
   * 获取上传历史
   * @param limit 返回数量
   */
  getHistory(limit = 20): Promise<ImageUploadHistory[]> {
    return apiClient.get('/images/history', {
      params: { limit },
    })
  },
}
