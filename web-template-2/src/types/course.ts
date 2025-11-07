export interface CourseCategory {
  id: number
  title: string
  icon: string
  order: number
}

export interface Subcategory {
  id: number
  name: string
  order: number
}

export interface CategoryMapping {
  mainCategoryId: number
  subcategories: Subcategory[]
}

export interface CourseWithDisplay {
  id: number
  name: string
  description: string
  mainCategory: number
  subCategory: number
  learnerCount: number
  icon: string
  iconColor: string
}

export interface CourseApplication {
  name: string
  description: string
  mainCategory: number | null
  subCategory: number | null
}
