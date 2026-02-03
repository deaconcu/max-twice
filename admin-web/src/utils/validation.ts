/**
 * 表单验证规则
 * 用于 Vuetify 表单验证
 */

/**
 * 必填项验证
 */
export const required =
  (message = '此项为必填项') =>
  (value: string) => {
    return !!value || message
  }

/**
 * 邮箱验证
 */
export const email =
  (message = '请输入有效的邮箱地址') =>
  (value: string) => {
    const pattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return !value || pattern.test(value) || message
  }

/**
 * 邮箱验证规则（必填）
 */
export const emailRules = [required('邮箱不能为空'), email('请输入有效的邮箱地址')]

/**
 * 密码长度验证
 */
export const minLength = (min: number, message?: string) => (value: string) => {
  const msg = message ?? `长度至少为 ${String(min)} 个字符`
  return !value || value.length >= min || msg
}

/**
 * 密码最大长度验证
 */
export const maxLength = (max: number, message?: string) => (value: string) => {
  const msg = message ?? `长度不能超过 ${String(max)} 个字符`
  return !value || value.length <= max || msg
}

/**
 * 密码验证规则（必填，6-20位）
 */
export const passwordRules = [
  required('密码不能为空'),
  minLength(6, '密码至少6个字符'),
  maxLength(20, '密码不能超过20个字符'),
]

/**
 * 用户名验证
 */
export const username =
  (message = '用户名只能包含字母、数字和下划线') =>
  (value: string) => {
    const pattern = /^[a-zA-Z0-9_]+$/
    return !value || pattern.test(value) || message
  }

/**
 * 用户名验证规则（必填，3-20位）
 */
export const usernameRules = [
  required('用户名不能为空'),
  minLength(3, '用户名至少3个字符'),
  maxLength(20, '用户名不能超过20个字符'),
  username(),
]

/**
 * 确认密码验证
 */
export const confirmPassword =
  (password: string, message = '两次输入的密码不一致') =>
  (value: string) => {
    return !value || value === password || message
  }

/**
 * 手机号验证
 */
export const phone =
  (message = '请输入有效的手机号码') =>
  (value: string) => {
    const pattern = /^1[3-9]\d{9}$/
    return !value || pattern.test(value) || message
  }

/**
 * 验证码验证（6位数字）
 */
export const verificationCode =
  (message = '请输入6位验证码') =>
  (value: string) => {
    const pattern = /^\d{6}$/
    return !value || pattern.test(value) || message
  }
