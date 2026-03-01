/**
 * 职业相关的类型定义
 * 参考：backend/learn-application/src/main/java/com/prosper/learn/application/dto/response/ProfessionDTO.java
 */

/**
 * 职业信息接口
 */
export interface Profession {
  id: number
  name: string // 职业名称
  description?: string // 职业描述
  price?: string // 价格
  skills?: string // 技能要求
  mainCategory?: number // 主分类
  subCategory?: number // 子分类
  icon?: string // 图标
  learnerCount?: number // 学习人数
  state?: number // 状态
  reason?: string // 拒绝/屏蔽原因
  creator?: {
    id: number
    name: string
    avatar?: string
  }
  createdAt?: string // 创建时间
  bookmarked?: boolean // 是否已收藏
}

/**
 * 职业分类
 */
export interface ProfessionCategory {
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
 * 带显示属性的职业信息（用于前端展示）
 */
export interface CareerWithDisplay extends Profession {
  icon?: string
  iconColor?: string
}
