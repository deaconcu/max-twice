import axios, {
  AxiosError,
  type InternalAxiosRequestConfig,
  type AxiosResponse,
  type AxiosRequestConfig,
} from 'axios'
import type { ApiResponse } from '@/types/api'

/**
 * 自定义 API 错误类
 */
export class ApiError extends Error {
  code: number
  details?: unknown

  constructor(code: number, message: string, details?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.details = details
    // 保持正确的原型链
    Object.setPrototypeOf(this, ApiError.prototype)
  }
}

/**
 * 创建 Axios 实例
 */
const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8090/api',
  timeout: 60000, // 60秒超时
  withCredentials: true, // 允许携带凭证（cookies）
  headers: {
    'Content-Type': 'application/json',
  },
})

/**
 * 请求拦截器 - 添加认证 token
 */
axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 从 localStorage 获取 token
    const token = localStorage.getItem('token')

    // 如果存在 token，添加到请求头
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error: AxiosError) => {
    console.error('请求拦截器错误:', error)
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器 - 统一处理响应和错误
 */
axiosInstance.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // 不在这里解包，让包装方法处理
    return response
  },
  (error: AxiosError) => {
    // 错误处理
    if (error.response) {
      const { status, data } = error.response

      // 根据状态码处理不同错误
      switch (status) {
        case 401:
          // 未授权 - 清除 token 并跳转到登录页
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          // TODO: 使用路由跳转到登录页
          console.error('未授权，请重新登录')
          break

        case 403:
          console.error('没有权限访问该资源')
          break

        case 404:
          console.error('请求的资源不存在')
          break

        case 500:
          console.error('服务器内部错误')
          break

        default:
          console.error(`请求错误 [${String(status)}]:`, error.message)
      }

      // 返回自定义 ApiError
      const apiData = data as Record<string, unknown> | undefined
      return Promise.reject(
        new ApiError(status, (apiData?.message as string | undefined) ?? error.message, apiData)
      )
    } else if (error.request) {
      // 请求已发出但没有收到响应
      console.error('网络错误：无法连接到服务器')
      return Promise.reject(new ApiError(0, '网络错误：无法连接到服务器'))
    } else {
      // 请求配置错误
      console.error('请求配置错误:', error.message)
      return Promise.reject(new ApiError(-1, error.message))
    }
  }
)

/**
 * 包装 API 客户端，提供自动解包功能
 */
const apiClient = {
  /**
   * GET 请求
   */
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return axiosInstance.get<ApiResponse<T>>(url, config).then((response) => response.data)
  },

  /**
   * POST 请求
   */
  post<T = unknown>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig
  ): Promise<ApiResponse<T>> {
    return axiosInstance.post<ApiResponse<T>>(url, data, config).then((response) => response.data)
  },

  /**
   * PUT 请求
   */
  put<T = unknown>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig
  ): Promise<ApiResponse<T>> {
    return axiosInstance.put<ApiResponse<T>>(url, data, config).then((response) => response.data)
  },

  /**
   * DELETE 请求
   */
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return axiosInstance.delete<ApiResponse<T>>(url, config).then((response) => response.data)
  },

  /**
   * PATCH 请求
   */
  patch<T = unknown>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig
  ): Promise<ApiResponse<T>> {
    return axiosInstance.patch<ApiResponse<T>>(url, data, config).then((response) => response.data)
  },
}

/**
 * Token 刷新函数（预留）
 * TODO: 根据后端实现调整
 */
export function refreshToken(): Promise<string | null> {
  // 这里应该调用后端的 token 刷新接口
  // const response = await apiClient.post<{ token: string }>('/auth/refresh')
  // return response.data.token
  return Promise.resolve(null)
}

export default apiClient
