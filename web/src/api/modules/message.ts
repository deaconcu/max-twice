import apiClient from '../client'
import type { Message } from '@/types/message'
import type { MessageType } from '@/enums'

/**
 * 消息列表响应
 */
export interface MessageListResponse {
  messages: Message[]
  lastViewedMessageId?: number
}

/**
 * 消息管理相关 API
 */
export const messageApi = {
  /**
   * 按分类获取消息
   * @param category 消息分类 (1=互动消息, 2=系统消息, 3=全部, 4=私信)
   * @param cursor 分页游标
   * @param type 可选的消息类型过滤
   */
  getMessagesByCategory(
    category: number,
    cursor?: string,
    type?: MessageType
  ): Promise<MessageListResponse> {
    const params: Record<string, unknown> = { category }
    if (cursor != null) params.cursor = cursor
    if (type != null) params.type = type
    return apiClient.get('/messages/category', { params })
  },

  /**
   * 获取未读消息数量
   */
  getUnreadCount(): Promise<number> {
    return apiClient.get('/messages/unread-count')
  },

  /**
   * 邀请用户
   */
  inviteUser(userId: number, nodeId: number): Promise<void> {
    return apiClient.post('/messages/invite', {
      userId,
      nodeId,
    })
  },
}
