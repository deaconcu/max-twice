import apiClient from '../client'
import type { CourseCategoriesData, RoleCategoriesData } from '@/stores/modules/category'

/**
 * 系统配置相关 API（公开接口）
 */
export const systemApi = {
  /**
   * 获取课程分类数据
   */
  getCourseCategories(): Promise<CourseCategoriesData> {
    return apiClient.get('/system/course-categories')
  },

  /**
   * 获取角色分类数据
   */
  getRoleCategories(): Promise<RoleCategoriesData> {
    return apiClient.get('/system/role-categories')
  },

  /**
   * 获取只读模式状态（公开接口，无需登录）
   */
  getReadonlyMode(): Promise<{ enabled: boolean }> {
    return apiClient.get('/public/readonly-mode')
  },
}
