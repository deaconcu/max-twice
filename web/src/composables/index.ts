/**
 * Composables 统一导出
 */

export { useFetch } from './useFetch'
export { useMutation } from './useMutation'
export { setApiConfig, globalConfig } from './config'
export { handleApiCall, debounce, throttle } from './utils'

export type { FetchOptions, FetchReturn } from './useFetch'
export type { MutationOptions, MutationReturn } from './useMutation'
export type { ApiResponse, CursorParams, LoadMoreCallback } from './types'
export type { StatusCodeHandler, ApiComposableConfig } from './config'
