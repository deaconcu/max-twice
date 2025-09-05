/**
 * Vue Flow 相关的类型定义
 * 用于路线图和学习视图中的流程图组件
 */

import type { Position } from '@vue-flow/core'

// 基础位置类型
export interface NodePosition {
  x: number
  y: number
}

// 基础节点数据类型
export interface BaseNodeData {
  label: string
  link?: string | null
  [key: string]: any
}

// 扩展节点数据类型（用于学习视图）
export interface ExtendedNodeData extends BaseNodeData {
  completed?: boolean
  progress?: number
  current?: boolean
  courseId?: string | number
  type?: string
}

// 基础节点类型
export interface FlowNode {
  id: string
  type?: string
  data: BaseNodeData
  position: NodePosition
  sourcePosition?: Position
  targetPosition?: Position
}

// 扩展节点类型（用于学习视图）
export interface ExtendedFlowNode {
  id: string
  type: string
  data: ExtendedNodeData
  position: NodePosition
  targetPosition?: Position  // 使用 Vue Flow 的 Position 枚举
  sourcePosition?: Position  // 使用 Vue Flow 的 Position 枚举
  class?: string
  style?: Record<string, string>
}

// 边连接类型
export interface FlowEdge {
  id: string
  source: string  // Vue Flow 标准类型，节点ID必须是字符串
  target: string  // Vue Flow 标准类型，节点ID必须是字符串
  type?: string
  animated?: boolean
  label?: string
  [key: string]: any
}