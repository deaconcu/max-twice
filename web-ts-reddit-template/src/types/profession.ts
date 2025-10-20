// 职业接口
export interface Profession {
  id: number
  name: string
  description: string
  mainCategory: number
  subCategory: number | null
  skills: string
  createdAt?: string
  updatedAt?: string
}

// 带显示属性的职业
export interface CareerWithDisplay extends Profession {
  icon?: string
  iconColor?: string
}

// 主分类
export interface ProfessionCategory {
  id: number
  title: string
  icon: string
  order: number
}

// 子分类
export interface Subcategory {
  id: number
  name: string
  order: number
}

// 分类映射
export interface CategoryMapping {
  mainCategoryId: number
  subcategories: Subcategory[]
}

// 职业申请表单
export interface CareerApplication {
  name: string
  description: string
  mainCategory: number | null
  subCategory: number | null
  skills: string
}

// ========== 课程相关类型定义 ==========

// 基础课程接口
export interface Course {
  id: number
  name: string
  description?: string
  mainCategory: number
  subCategory: number
  learnerCount?: number
  subscriptionCount?: number
  createdAt?: string
  updatedAt?: string
}

// 带显示属性的课程
export interface CourseWithDisplay extends Course {
  icon?: string
  iconColor?: string
}

// 课程分类（复用 ProfessionCategory 和 Subcategory）
export interface CourseCategory extends ProfessionCategory {}

// 课程申请表单
export interface CourseApplication {
  name: string
  description: string
  mainCategory: number | null
  subCategory: number | null
}
