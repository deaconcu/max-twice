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
 * 角色分类数据结构
 */
export interface RoleCategory {
  id: number
  title: string
  description?: string
}

/**
 * 课程分类映射数据结构
 */
export interface CourseCategoryMapping {
  mainCategoryId: number
  subcategories: {
    id: number
    name: string
  }[]
}

/**
 * 角色分类映射数据结构
 */
export interface RoleCategoryMapping {
  mainCategoryId: number
  subcategories: {
    id: number
    name: string
  }[]
}

/**
 * 课程分类响应数据
 */
export interface CourseCategoriesData {
  mainCategories: CourseCategory[]
  categoryMapping: CourseCategoryMapping[]
}

/**
 * 角色分类响应数据
 */
export interface RoleCategoriesData {
  mainCategories: RoleCategory[]
  categoryMapping: RoleCategoryMapping[]
}

/**
 * 分类数据 Store
 * 从后端获取课程和角色分类数据，统一管理并缓存
 * 参考 validationConfig.ts 的实现模式
 */
export const useCategoryStore = defineStore(
  'category',
  () => {
    // 课程分类数据
    const courseCategories = ref<CourseCategoriesData | null>(null)
    // 角色分类数据
    const roleCategories = ref<RoleCategoriesData | null>(null)
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
          roleCategories: roleCategories.value,
        }
      }

      // 执行检查
      console.log('[CategoryStore] 检查分类数据更新...')
      await loadCategories()
      lastChecked.value = now

      return {
        courseCategories: courseCategories.value,
        roleCategories: roleCategories.value,
      }
    }

    /**
     * 加载分类数据（内部方法）
     * 同时加载课程和角色分类
     */
    async function loadCategories() {
      loading.value = true
      try {
        // 并行加载课程和角色分类
        const [courseResponse, roleResponse] = await Promise.all([
          systemApi.getCourseCategories(),
          systemApi.getRoleCategories(),
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

        // 更新角色分类
        if (roleResponse.data) {
          const oldRoleConfig = JSON.stringify(roleCategories.value)
          const newRoleConfig = JSON.stringify(roleResponse.data)

          if (oldRoleConfig !== newRoleConfig) {
            console.log('[CategoryStore] ✅ 角色分类已更新')
            roleCategories.value = roleResponse.data
          } else {
            console.log('[CategoryStore] 角色分类无变化')
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
    function getCourseSubCategories(mainCategoryId?: number): { id: number; name: string }[] {
      if (!courseCategories.value?.categoryMapping) return []
      if (!mainCategoryId) return []

      const mapping = courseCategories.value.categoryMapping.find(
        (m) => m.mainCategoryId === mainCategoryId
      )
      return mapping?.subcategories || []
    }

    /**
     * 获取角色主分类列表
     */
    function getRoleMainCategories(): RoleCategory[] {
      return roleCategories.value?.mainCategories || []
    }

    /**
     * 获取角色子分类列表
     */
    function getRoleSubCategories(mainCategoryId?: number): { id: number; name: string }[] {
      if (!roleCategories.value?.categoryMapping) return []
      if (!mainCategoryId) return []

      const mapping = roleCategories.value.categoryMapping.find(
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
     * 根据 ID 获取角色主分类名称
     */
    function getRoleMainCategoryName(id?: number): string {
      if (!id) return ''
      const category = getRoleMainCategories().find((c) => c.id === id)
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
     * 获取课程完整分类文本
     */
    function getCourseFullCategoryText(mainCategoryId?: number, subCategoryId?: number): string {
      if (!mainCategoryId) return ''

      const mainCategoryName = getCourseMainCategoryName(mainCategoryId)
      const subCategoryName = subCategoryId ? getSubCategoryName(mainCategoryId, subCategoryId) : ''

      return subCategoryName ? `${mainCategoryName} / ${subCategoryName}` : mainCategoryName
    }

    /**
     * 清除缓存（供调试使用）
     */
    function clearCache() {
      courseCategories.value = null
      roleCategories.value = null
      lastChecked.value = 0
      console.log('[CategoryStore] 缓存已清除')
    }

    return {
      courseCategories,
      roleCategories,
      lastChecked,
      loading,
      checkAndLoad,
      refresh,
      getCourseMainCategories,
      getCourseSubCategories,
      getRoleMainCategories,
      getRoleSubCategories,
      getCourseMainCategoryName,
      getRoleMainCategoryName,
      getSubCategoryName,
      getCourseFullCategoryText,
      clearCache,
    }
  },
  {
    persist: {
      key: 'categoryStore',
      paths: ['courseCategories', 'roleCategories', 'lastChecked'], // 持久化分类数据和最后检查时间
    },
  }
)
