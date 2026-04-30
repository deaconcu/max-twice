import axios, {
  AxiosError,
  type InternalAxiosRequestConfig,
  type AxiosResponse,
  type AxiosRequestConfig,
} from 'axios'
import type { ApiErrorResponse } from '@/types/api'
import { logger } from '@/utils/logger'
import i18n from '@/i18n'

/**
 * 自定义 API 错误类
 */
export class ApiError extends Error {
  /** HTTP 状态码 */
  httpStatus: number
  /** 后端字符串错误码，如 "INVALID_PARAMETER" */
  code: string
  details?: Record<string, unknown>

  constructor(
    httpStatus: number,
    code: string,
    message: string,
    details?: Record<string, unknown>
  ) {
    super(message)
    this.name = 'ApiError'
    this.httpStatus = httpStatus
    this.code = code
    this.details = details
    Object.setPrototypeOf(this, ApiError.prototype)
  }
}

/**
 * 创建 Axios 实例
 */
const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  timeout: 60000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

/**
 * 开发环境：API 延时配置（用于测试 loading 状态）
 */
const API_DELAY = import.meta.env.DEV ? 500 : 0

/**
 * 请求拦截器
 */
axiosInstance.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    console.log('[API Request]', {
      method: config.method?.toUpperCase(),
      url: config.url,
      params: config.params,
      data: config.data,
    })

    if (API_DELAY > 0) {
      await new Promise((resolve) => setTimeout(resolve, API_DELAY))
    }

    // token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 语言头
    const locale = i18n.global.locale.value || 'zh'
    config.headers['Accept-Language'] = locale
    config.headers['X-Site-Lang'] = locale

    return config
  },
  (error: AxiosError) => {
    logger.error('请求拦截器错误', error)
    return Promise.reject(error)
  }
)

/**
 * 从错误响应中提取 ApiError
 */
function parseApiError(status: number, data: unknown, fallbackMessage: string): ApiError {
  const body = data as ApiErrorResponse | undefined
  const errorBody = body?.error
  const code = errorBody?.code ?? 'UNKNOWN'
  const message = errorBody?.message ?? fallbackMessage
  const details = errorBody?.details
  return new ApiError(status, code, message, details)
}

/**
 * 响应拦截器 - 直接返回 response.data，错误转换为 ApiError
 */
axiosInstance.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error: AxiosError) => {
    if (error.response) {
      const { status, data } = error.response

      if (status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        logger.error('未授权，请重新登录')
      } else if (status === 403) {
        logger.error('没有权限访问该资源')
      } else if (status === 500) {
        logger.error('服务器内部错误')
      }

      return Promise.reject(parseApiError(status, data, error.message))
    } else if (error.request) {
      const msg = i18n.global.t('error.networkError')
      logger.error(msg)
      return Promise.reject(new ApiError(0, 'NETWORK_ERROR', msg))
    } else {
      logger.error('请求配置错误', error.message)
      return Promise.reject(new ApiError(-1, 'REQUEST_ERROR', error.message))
    }
  }
)

/**
 * API 客户端 - 直接返回 response.data（无 ApiResponse 包装）
 */
const apiClient = {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return axiosInstance.get<T>(url, config).then((r) => r.data)
  },

  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return axiosInstance.post<T>(url, data, config).then((r) => r.data)
  },

  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return axiosInstance.put<T>(url, data, config).then((r) => r.data)
  },

  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return axiosInstance.delete<T>(url, config).then((r) => r.data)
  },

  patch<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return axiosInstance.patch<T>(url, data, config).then((r) => r.data)
  },
}

export { apiClient }
export default apiClient
