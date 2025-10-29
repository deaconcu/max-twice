/**
 * 操作日志类型定义
 */

import type { UserRole } from './enums'

/**
 * 操作级别枚举
 */
export enum OperationLevel {
  LOW = 1,    // 低：审核通过、恢复内容
  MEDIUM = 2, // 中：审核拒绝、临时屏蔽
  HIGH = 3    // 高：删除、封禁、修改角色
}

/**
 * 操作日志DTO
 */
export interface OperationLogDTO {
  id: number
  operatorId: number
  operatorName: string
  operatorRole: UserRole
  module: string
  operationType: string
  operationLevel: OperationLevel
  targetType: string
  targetId: number
  targetName?: string
  reason?: string
  extraData?: Record<string, unknown>
  ipAddress?: string
  createdAt: string
}

/**
 * 操作日志查询请求
 */
export interface OperationLogQueryRequest {
  operatorId?: number
  module?: string
  operationType?: string
  targetType?: string
  operationLevel?: OperationLevel
  startTime?: string
  endTime?: string
  lastId?: number
  limit?: number
}

/**
 * 操作日志分页响应
 */
export interface OperationLogPageResponse {
  items: OperationLogDTO[]
  hasMore: boolean
  nextLastId?: number
}
