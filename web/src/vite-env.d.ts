/// <reference types="vite/client" />

// 声明 .vue 文件模块
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent
  export default component
}

// 声明 CSS 模块
declare module '*.css' {
  const content: string
  export default content
}
