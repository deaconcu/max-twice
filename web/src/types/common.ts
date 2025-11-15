/**
 * 通用类型定义
 */

/**
 * 主分类
 */
export interface MainCategory {
  id: number
  name: string
}

/**
 * 子分类
 */
export interface SubCategory {
  id: number
  name: string
}

/**
 * 分类映射关系
 */
export interface CategoryMapping {
  mainCategoryId: number
  subCategories: SubCategory[]
}

/**
 * 状态配置
 */
export interface StateConfig {
  text: string
  color: string
  icon: string
}