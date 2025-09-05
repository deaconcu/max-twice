/**
 * 消息相关的类型定义
 * 基于后端 MessageDTO.java
 */

import type { User } from './user'
import type { MessageType } from './enums'

// 消息信息 (对应后端 MessageDTO)
export interface Message {
  id: number              // Long -> number
  sender?: User           // UserDTOV4 -> User (可选)
  receiver?: User         // UserDTOV4 -> User (可选)
  type?: MessageType      // Integer -> MessageType (可选)
  isRead?: number         // Integer -> number (可选)
  createdAt?: string      // String (可选)
}