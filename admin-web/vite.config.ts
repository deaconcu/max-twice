import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vuetify from 'vite-plugin-vuetify'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // 加载环境变量
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [
      vue(),
      // Vuetify 插件配置
      vuetify({
        autoImport: true,
      }),
    ],

    // 路径别名
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },

    // 开发服务器配置
    server: {
      port: Number(env.VITE_APP_PORT) || 5175, // Admin端口改为5175
      host: true,
      open: false,
      // API 代理配置
      proxy: {
        '/api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:9202',
          changeOrigin: true,
        },
      },
    },

    // 构建配置
    build: {
      minify: 'terser',
      terserOptions: {
        compress: {
          drop_console: true,
          drop_debugger: true,
        },
      },
    },
  }
})
