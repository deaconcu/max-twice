/**
 * 统一的表单验证规则工具
 * 基于后端配置动态生成验证规则
 */
import { useValidationConfigStore } from '@/stores/validationConfig'
import { computed, type ComputedRef } from 'vue'

/**
 * 创建基于配置的验证规则
 * @param fieldKey 字段键（如：'card-front', 'comment-content'）
 * @returns 验证规则数组
 */
export function useValidationRules(fieldKey: string): ComputedRef<Array<(v: string) => boolean | string>> {
  const validationStore = useValidationConfigStore()

  return computed(() => validationStore.createRules(fieldKey))
}

/**
 * 获取字段的最大长度（用于 counter）
 * @param fieldKey 字段键
 * @returns 最大长度
 */
export function useMaxLength(fieldKey: string): ComputedRef<number> {
  const validationStore = useValidationConfigStore()

  return computed(() => validationStore.getRule(fieldKey)?.maxLength || 500)
}

/**
 * 获取字段的配置信息
 * @param fieldKey 字段键
 * @returns 验证规则配置
 */
export function useValidationRule(fieldKey: string) {
  const validationStore = useValidationConfigStore()

  const rule = computed(() => validationStore.getRule(fieldKey))
  const rules = computed(() => validationStore.createRules(fieldKey))
  const maxLength = computed(() => rule.value?.maxLength || 500)
  const minLength = computed(() => rule.value?.minLength || 0)
  const label = computed(() => rule.value?.label || '字段')

  return {
    rule,
    rules,
    maxLength,
    minLength,
    label,
  }
}

// ========== 特殊验证规则（不依赖后端配置）==========

/**
 * 邮箱格式验证
 */
export const emailFormatRule = (v: string) => {
  if (!v) return true
  const pattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return pattern.test(v) || '请输入有效的邮箱地址'
}

/**
 * 手机号格式验证
 */
export const phoneFormatRule = (v: string) => {
  if (!v) return true
  const pattern = /^1[3-9]\d{9}$/
  return pattern.test(v) || '请输入有效的手机号码'
}

/**
 * 验证码格式验证（6位数字）
 */
export const verificationCodeRule = (v: string) => {
  if (!v) return true
  const pattern = /^\d{6}$/
  return pattern.test(v) || '请输入6位验证码'
}

/**
 * 确认密码验证
 */
export const confirmPasswordRule = (password: string) => (v: string) => {
  return !v || v === password || '两次输入的密码不一致'
}

/**
 * 用户名格式验证（字母、数字、下划线）
 */
export const usernameFormatRule = (v: string) => {
  if (!v) return true
  const pattern = /^[a-zA-Z0-9_]+$/
  return pattern.test(v) || '用户名只能包含字母、数字和下划线'
}

/**
 * 创建带格式验证的邮箱规则
 */
export function useEmailRules(): ComputedRef<Array<(v: string) => boolean | string>> {
  const baseRules = useValidationRules('email')

  return computed(() => [
    ...baseRules.value,
    emailFormatRule
  ])
}

/**
 * 创建带格式验证的用户名规则
 */
export function useUsernameRules(): ComputedRef<Array<(v: string) => boolean | string>> {
  const baseRules = useValidationRules('username')

  return computed(() => [
    ...baseRules.value,
    usernameFormatRule
  ])
}
