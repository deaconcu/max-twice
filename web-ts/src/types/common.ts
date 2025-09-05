/**
 * 通用界面类型定义
 * 用于在多个组件间共享的通用接口
 */

// 标签页项目
export interface TabItem {
  value: string
  text: string
  icon?: string
  badge?: string | number
  badgeColor?: string
  group?: string
}

// 排序选项
export interface SortOption {
  value: string
  title: string
}

// 通用组件属性 - 用于动态组件传参
export interface ComponentProps {
  [key: string]: any
}

// 分类相关类型
export interface MainCategory {
  id: number
  name: string
  list?: SubCategory[]  // 可选的子分类列表
}

export interface SubCategory {
  id: number
  name: string
}

export interface CategoryMapping {
  mainCategoryId: number
  subCategories: SubCategory[]
}

// 状态配置
export interface StateConfig {
  text: string
  color: string
  icon: string
}

// 状态选项（用于下拉选择等）
export interface StateOption {
  value: number
  text: string
  color: string
  icon: string
}