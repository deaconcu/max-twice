/**
 * 角色相关的类型定义
 * 参考：backend/application/src/main/java/com/twicemax/application/dto/response/RoleDTO.java
 */

/**
 * 角色信息接口
 */
export interface Role {
  id: number
  name: string // 角色名称
  description?: string // 角色描述
  price?: string // 价格
  skills?: string // 技能要求
  mainCategory?: number // 主分类
  subCategory?: number // 子分类
  icon?: string // 图标
  learnerCount?: number // 学习人数
  createdAt?: string // 创建时间
  bookmarked?: boolean // 是否已收藏
}

/**
 * 角色分类
 */
export interface RoleCategory {
  id: number
  title: string
  icon?: string
}

/**
 * 子分类
 */
export interface Subcategory {
  id: number
  name: string
}

/**
 * 分类映射关系
 */
export interface CategoryMapping {
  mainCategoryId: number
  subcategories: Subcategory[]
}

/**
 * 带显示属性的角色信息（用于前端展示）
 */
export interface RoleWithDisplay extends Role {
  icon?: string
  iconColor?: string
}
