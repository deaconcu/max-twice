import { defineStore } from 'pinia'
import { ref } from 'vue'
import { systemApi } from '@/api'

/**
 * 课程分类数据结构
 */
export interface CourseCategory {
  id: number
  name: string
  description?: string
}

/**
 * 职业分类数据结构
 */
export interface ProfessionCategory {
  id: number
  title: string
  description?: string
}

/**
 * 课程分类映射数据结构
 */
export interface CourseCategoryMapping {
  mainCategoryId: number
  subCategories: Array<{
    id: number
    name: string
  }>
}

/**
 * 职业分类映射数据结构
 */
export interface ProfessionCategoryMapping {
  mainCategoryId: number
  subcategories: Array<{
    // 注意：小写 c
    id: number
    name: string
  }>
}

/**
 * 课程分类响应数据
 */
export interface CourseCategoriesData {
  mainCategories: CourseCategory[]
  categoryMapping: CourseCategoryMapping[]
}

/**
 * 职业分类响应数据
 */
export interface ProfessionCategoriesData {
  mainCategories: ProfessionCategory[]
  categoryMapping: ProfessionCategoryMapping[]
}

/**
 * 分类数据 Store
 * 从后端获取课程和职业分类数据，统一管理并缓存
 * 参考 validationConfig.ts 的实现模式
 */
export const useCategoryStore = defineStore(
  'category',
  () => {
    // 课程分类数据
    const courseCategories = ref<CourseCategoriesData | null>(null)
    // 职业分类数据
    const professionCategories = ref<ProfessionCategoriesData | null>(null)
    // 最后检查时间
    const lastChecked = ref<number>(0)
    // 加载状态
    const loading = ref(false)
    // 检查间隔：3分钟（与 validationConfig 保持一致）
    const CHECK_INTERVAL = 3 * 60 * 1000

    /**
     * 检查并加载配置（带节流）
     * 页面 onMounted 时调用，避免频繁请求
     */
    async function checkAndLoad() {
      const now = Date.now()

      // 如果最近检查过，跳过
      if (lastChecked.value && now - lastChecked.value < CHECK_INTERVAL) {
        console.log(
          `[CategoryStore] 距离上次检查仅 ${Math.round((now - lastChecked.value) / 1000)}s，跳过`
        )
        return {
          courseCategories: courseCategories.value,
          professionCategories: professionCategories.value,
        }
      }

      // 执行检查
      console.log('[CategoryStore] 检查分类数据更新...')
      await loadCategories()
      lastChecked.value = now

      return {
        courseCategories: courseCategories.value,
        professionCategories: professionCategories.value,
      }
    }

    /**
     * 加载分类数据（内部方法）
     * 同时加载课程和职业分类
     */
    async function loadCategories() {
      loading.value = true
      try {
        // 并行加载课程和职业分类
        const [courseResponse, professionResponse] = await Promise.all([
          systemApi.getCourseCategories(),
          systemApi.getProfessionCategories(),
        ])

        // 更新课程分类
        if (courseResponse.data) {
          const oldCourseConfig = JSON.stringify(courseCategories.value)
          const newCourseConfig = JSON.stringify(courseResponse.data)

          if (oldCourseConfig !== newCourseConfig) {
            console.log('[CategoryStore] ✅ 课程分类已更新')
            courseCategories.value = courseResponse.data
          } else {
            console.log('[CategoryStore] 课程分类无变化')
          }
        }

        // 更新职业分类
        if (professionResponse.data) {
          const oldProfessionConfig = JSON.stringify(professionCategories.value)
          const newProfessionConfig = JSON.stringify(professionResponse.data)

          if (oldProfessionConfig !== newProfessionConfig) {
            console.log('[CategoryStore] ✅ 职业分类已更新')
            professionCategories.value = professionResponse.data
          } else {
            console.log('[CategoryStore] 职业分类无变化')
          }
        }
      } catch (error) {
        console.error('[CategoryStore] 加载分类数据失败', error)
        // 失败时继续使用 localStorage 中的旧数据
      } finally {
        loading.value = false
      }
    }

    /**
     * 手动刷新分类数据（供管理页面使用）
     */
    async function refresh() {
      lastChecked.value = 0 // 重置检查时间
      await loadCategories()
    }

    /**
     * 获取课程主分类列表
     */
    function getCourseMainCategories(): CourseCategory[] {
      return courseCategories.value?.mainCategories || []
    }

    /**
     * 获取课程子分类列表
     */
    function getCourseSubCategories(mainCategoryId?: number): Array<{ id: number; name: string }> {
      if (!courseCategories.value?.categoryMapping) return []
      if (!mainCategoryId) return []

      const mapping = courseCategories.value.categoryMapping.find(
        (m) => m.mainCategoryId === mainCategoryId
      )
      return mapping?.subCategories || []
    }

    /**
     * 获取职业主分类列表
     */
    function getProfessionMainCategories(): ProfessionCategory[] {
      return professionCategories.value?.mainCategories || []
    }

    /**
     * 获取职业子分类列表
     */
    function getProfessionSubCategories(
      mainCategoryId?: number
    ): Array<{ id: number; name: string }> {
      if (!professionCategories.value?.categoryMapping) return []
      if (!mainCategoryId) return []

      const mapping = professionCategories.value.categoryMapping.find(
        (m) => m.mainCategoryId === mainCategoryId
      )
      return mapping?.subcategories || [] // 注意：小写 c
    }

    /**
     * 根据 ID 获取课程主分类名称
     */
    function getCourseMainCategoryName(id?: number): string {
      if (!id) return ''
      const category = getCourseMainCategories().find((c) => c.id === id)
      return category?.name || ''
    }

    /**
     * 根据 ID 获取职业主分类名称
     */
    function getProfessionMainCategoryName(id?: number): string {
      if (!id) return ''
      const category = getProfessionMainCategories().find((c) => c.id === id)
      return category?.title || ''
    }

    /**
     * 根据 ID 获取子分类名称
     */
    function getSubCategoryName(mainCategoryId: number, subCategoryId: number): string {
      const subCategories = getCourseSubCategories(mainCategoryId)
      const subCategory = subCategories.find((s) => s.id === subCategoryId)
      return subCategory?.name || ''
    }

    /**
     * 清除缓存（供调试使用）
     */
    function clearCache() {
      courseCategories.value = null
      professionCategories.value = null
      lastChecked.value = 0
      console.log('[CategoryStore] 缓存已清除')
    }

    return {
      courseCategories,
      professionCategories,
      lastChecked,
      loading,
      checkAndLoad,
      refresh,
      getCourseMainCategories,
      getCourseSubCategories,
      getProfessionMainCategories,
      getProfessionSubCategories,
      getCourseMainCategoryName,
      getProfessionMainCategoryName,
      getSubCategoryName,
      clearCache,
    }
  },
  {
    persist: {
      key: 'categoryStore',
      paths: ['courseCategories', 'professionCategories', 'lastChecked'], // 持久化分类数据和最后检查时间
    },
  }
)
