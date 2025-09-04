/**
 * 内容相关的类型定义
 * 基于后端 ContentsDTO.java
 */

// 内容信息
export interface Contents {
  id: number
  userId: number             // 用户ID
  contents: string           // 内容
  createdAt: string          // 创建时间
  updatedAt: string          // 更新时间
}