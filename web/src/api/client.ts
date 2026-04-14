import axios, {
  AxiosError,
  type InternalAxiosRequestConfig,
  type AxiosResponse,
  type AxiosRequestConfig,
} from 'axios'
import type { ApiResponse } from '@/types/api'
import { logger } from '@/utils/logger'
import i18n from '@/i18n'

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
 * ETag 缓存管理
 */
class ETagCache {
  private cache = new Map<string, { etag: string; data: any }>()

  set(url: string, etag: string, data: any) {
    this.cache.set(url, { etag, data })
  }

  get(url: string): { etag: string; data: any } | undefined {
    return this.cache.get(url)
  }

  getETag(url: string): string | undefined {
    return this.cache.get(url)?.etag
  }

  getData(url: string): any | undefined {
    return this.cache.get(url)?.data
  }

  clear() {
    this.cache.clear()
  }
}

const etagCache = new ETagCache()

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
 * 开发环境：API 延时配置（用于测试 loading 状态）
 * 设置为 0 则禁用延时
 */
const API_DELAY = import.meta.env.DEV ? 500 : 0 // 开发环境延时 500ms

/**
 * 请求拦截器 - 添加认证 token、开发延时和 ETag
 */
axiosInstance.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    // 记录请求日志
    console.log('[API Request]', {
      method: config.method?.toUpperCase(),
      url: config.url,
      params: config.params,
      data: config.data,
    })

    // 开发环境：添加延时以便测试 loading 状态
    if (API_DELAY > 0) {
      await new Promise((resolve) => setTimeout(resolve, API_DELAY))
    }

    // 从 localStorage 获取 token
    const token = localStorage.getItem('token')

    // 如果存在 token，添加到请求头
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 添加语言头，让后端返回对应语言的错误信息
    const locale = i18n.global.locale.value || 'zh'
    config.headers['Accept-Language'] = locale

    // 添加站点语言头，用于后端数据源路由
    config.headers['X-Site-Lang'] = locale

    // 添加 ETag 缓存头（If-None-Match）
    if (config.url) {
      const cachedETag = etagCache.getETag(config.url)
      if (cachedETag) {
        config.headers['If-None-Match'] = cachedETag
      }
    }

    return config
  },
  (error: AxiosError) => {
    logger.error('请求拦截器错误', error)
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器 - 统一处理响应、错误和 ETag
 */
axiosInstance.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // 处理 ETag 缓存
    const etag = response.headers.etag
    if (etag && response.config.url) {
      // 保存 ETag 和响应数据
      etagCache.set(response.config.url, etag, response.data)
      console.log(`[ETag] 缓存已保存: ${response.config.url} -> ${etag}`)
    }

    // 不在这里解包，让包装方法处理
    return response
  },
  (error: AxiosError) => {
    // 处理 304 Not Modified
    if (error.response?.status === 304) {
      const cachedData = etagCache.getData(error.config?.url || '')
      if (cachedData) {
        console.log(`[ETag] 使用缓存数据: ${error.config?.url}`)
        // 返回缓存的数据
        return {
          ...error.response,
          data: cachedData,
          status: 200,
        } as AxiosResponse<ApiResponse>
      }
    }

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
          logger.error('未授权，请重新登录')
          break

        case 403:
          logger.error('没有权限访问该资源')
          break

        case 404:
          logger.error('请求的资源不存在')
          break

        case 500:
          logger.error('服务器内部错误')
          break

        default:
          logger.error(`请求错误 [${String(status)}]`, error.message)
      }

      // 返回自定义 ApiError
      const apiData = data as ApiResponse
      return Promise.reject(new ApiError(status, apiData.message ?? error.message, apiData))
    } else if (error.request) {
      // 请求已发出但没有收到响应
      const networkErrorMsg = i18n.global.t('error.networkError')
      logger.error(networkErrorMsg)
      return Promise.reject(new ApiError(0, networkErrorMsg))
    } else {
      // 请求配置错误
      logger.error('请求配置错误', error.message)
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

// 命名导出（供新代码使用）
export { apiClient }

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

// 默认导出（向后兼容）
export default apiClient
