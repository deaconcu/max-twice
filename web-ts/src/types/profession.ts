/**
 * 职业相关的类型定义
 * 基于后端 ProfessionDTO.java
 */

import { ProfessionState } from './enums'

// 职业信息
export interface Profession {
  id: number
  name: string               // 职业名称 (可选)
  description?: string        // 职业描述 (可选)
  price?: string             // 价格 (可选)
  skills?: string            // 技能要求 (可选)
  mainCategory?: number      // 主分类 (可选)
  subCategory?: number       // 子分类 (可选)
  state?: ProfessionState    // 状态：SUBMITTED(0), APPROVED(1), REJECTED(2) (可选)
  rejectedReason?: string    // 拒绝原因 (可选)
  icon?: string              // 图标 (可选)
  creator?: number           // 创建者ID (可选)
  learnerCount?: number      // 学习人数 (可选)
  createdAt?: string         // 创建时间 (可选)
  updatedAt?: string         // 更新时间 (可选)
}

// 职业分类
export interface ProfessionCategory {
  id: number
  title: string
  icon?: string
}

// 子分类
export interface Subcategory {
  id: number
  name: string
}

// 分类映射关系
export interface CategoryMapping {
  mainCategoryId: number
  subcategories: Subcategory[]
}