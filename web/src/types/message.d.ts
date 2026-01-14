import type { User } from './user'
import type { MessageType } from '@/enums'

/**
 * 消息相关的类型定义
 * 参考：web-ts/src/types/message.ts
 */

/**
 * 消息信息接口
 */
export interface Message {
  id: number
  sender?: User // 发送者
  receiver?: User // 接收者
  content?: string // 消息内容（JSON 格式）
  type?: MessageType // 消息类型
  isRead?: number // 是否已读（已废弃，使用 isNew 代替）
  isNew?: boolean // 是否为新消息（前端计算）
  createdAt?: string // 创建时间
}
