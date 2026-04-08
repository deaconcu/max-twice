/**
 * 统一的表单验证规则工具
 * 基于后端配置动态生成验证规则
 */
import { useValidationConfigStore } from '@/stores/validationConfig'
import { computed, type ComputedRef } from 'vue'
import { useI18n } from '@/composables/useI18n'

/**
 * 创建基于配置的验证规则
 * @param fieldKey 字段键（如：'card-front', 'comment-content'）
 * @returns 验证规则数组
 */
export function useValidationRules(
  fieldKey: string
): ComputedRef<((v: string) => boolean | string)[]> {
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
  const { t } = useI18n()
  const label = computed(() => rule.value?.label || t('validation.fieldLabel'))

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
export function useEmailFormatRule() {
  const { t } = useI18n()
  return (v: string) => {
    if (!v) return true
    const pattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return pattern.test(v) || t('validation.format.email')
  }
}

/**
 * 手机号格式验证
 */
export function usePhoneFormatRule() {
  const { t } = useI18n()
  return (v: string) => {
    if (!v) return true
    const pattern = /^1[3-9]\d{9}$/
    return pattern.test(v) || t('validation.format.phone')
  }
}

/**
 * 验证码格式验证（6位数字）
 */
export function useVerificationCodeRule() {
  const { t } = useI18n()
  return (v: string) => {
    if (!v) return true
    const pattern = /^\d{6}$/
    return pattern.test(v) || t('validation.format.verificationCode')
  }
}

/**
 * 确认密码验证
 */
export function useConfirmPasswordRule() {
  const { t } = useI18n()
  return (password: string) => (v: string) => {
    return !v || v === password || t('validation.format.passwordMismatch')
  }
}

/**
 * 用户名格式验证（字母、数字、下划线）
 */
export function useUsernameFormatRule() {
  const { t } = useI18n()
  return (v: string) => {
    if (!v) return true
    const pattern = /^[a-zA-Z0-9_]+$/
    return pattern.test(v) || t('validation.format.username')
  }
}

/**
 * 创建带格式验证的邮箱规则
 */
export function useEmailRules(): ComputedRef<((v: string) => boolean | string)[]> {
  const baseRules = useValidationRules('email')
  const emailFormatRule = useEmailFormatRule()

  return computed(() => [...baseRules.value, emailFormatRule])
}

/**
 * 创建带格式验证的用户名规则
 */
export function useUsernameRules(): ComputedRef<((v: string) => boolean | string)[]> {
  const baseRules = useValidationRules('username')
  const usernameFormatRule = useUsernameFormatRule()

  return computed(() => [...baseRules.value, usernameFormatRule])
}
