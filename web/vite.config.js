import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// 获取语言参数
const locale = process.env.VITE_LOCALE || 'zh'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  build: {
    outDir: `dist/${locale}`,
    assetsDir: 'assets',
    rollupOptions: {
      output: {
        // 为不同语言版本生成不同的文件名
        entryFileNames: `assets/[name]-${locale}.[hash].js`,
        chunkFileNames: `assets/[name]-${locale}.[hash].js`,
        assetFileNames: `assets/[name]-${locale}.[hash].[ext]`
      }
    }
  },
  define: {
    // 在构建时定义语言环境变量
    'import.meta.env.VITE_LOCALE': JSON.stringify(locale)
  }
})
