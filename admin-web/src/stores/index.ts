/**
 * Pinia Stores 统一导出
 * 使用方式：import { useAuthStore, useUserStore } from '@/stores'
 */

export { useAuthStore } from './modules/auth'
export { useUserStore } from './modules/user'
export { useCategoryStore } from './modules/category'
export { useSystemConfigStore } from './modules/systemConfig'
export { useValidationConfigStore } from './validationConfig'
