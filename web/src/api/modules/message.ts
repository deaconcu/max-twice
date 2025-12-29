import apiClient from '../client'
import type { ApiResponse } from '@/types/api'
import type { Message } from '@/types/message'
import type { MessageType } from '@/enums'

/**
 * 消息管理相关 API
 * 参考：web-ts/src/services/api/v1/apiServiceV1.ts (messageServiceV1)
 */
export const messageApi = {
  /**
   * 获取系统消息
   */
  getSystemMessages(type?: MessageType, lastId?: number): Promise<ApiResponse<Message[]>> {
    return apiClient.get('/v1/messages/system', {
      params: { type, lastId },
    })
  },

  /**
   * 按分类获取消息
   * @param category 消息分类 (1=互动消息, 2=系统消息, 3=私信)
   * @param lastId 最后一条消息的 ID，用于分页
   * @param type 可选的消息类型过滤
   */
  getMessagesByCategory(
    category: number,
    lastId?: number,
    type?: MessageType
  ): Promise<ApiResponse<Message[]>> {
    const params: Record<string, number> = { category }
    if (lastId != null) {
      params.lastId = lastId
    }
    if (type != null) {
      params.type = type
    }
    return apiClient.get('/v1/messages/category', { params })
  },

  /**
   * 获取所有消息
   */
  getMessages(): Promise<ApiResponse<Message[]>> {
    return apiClient.get('/v1/messages')
  },

  /**
   * 发送系统消息
   */
  sendSystemMessage(type: MessageType, to: number, content: string): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/messages/system', {
      type,
      to,
      content,
    })
  },

  /**
   * 邀请用户
   */
  inviteUser(userId: number, nodeId: number): Promise<ApiResponse<void>> {
    return apiClient.post('/v1/messages/invite', {
      userId,
      nodeId,
    })
  },
}
