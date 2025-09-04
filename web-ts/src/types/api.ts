/**
 * API 响应的基础格式
 */
export interface ApiResponse<T = any> {
  code: number           // 状态码：200 成功，401 未授权等
  data: T               // 响应数据
  message?: string      // 错误信息（可选）
}