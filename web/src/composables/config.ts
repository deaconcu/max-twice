import type { Router } from 'vue-router'

/**
 * 全局 router 实例引用（供错误处理跳转使用）
 */
let globalRouter: Router | null = null

export function setGlobalRouter(router: Router) {
  globalRouter = router
}

export function getGlobalRouter(): Router | null {
  return globalRouter
}

/**
 * Snackbar 函数引用
 */
let globalShowSnackbar: ((message: string, type: string) => void) | null = null

export function setGlobalSnackbar(fn: (message: string, type: string) => void) {
  globalShowSnackbar = fn
}

export function getGlobalSnackbar() {
  return globalShowSnackbar
}
