/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string
  readonly VITE_API_URL: string
  readonly VITE_APP_PORT?: string
  readonly VITE_ENABLE_MOCK?: string
  readonly VITE_DEBUG_MODE?: string
  readonly VITE_LOCALE?: 'zh' | 'en'
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

// 声明 JSON 模块类型
declare module '*.json' {
  const value: Record<string, unknown>
  export default value
}
