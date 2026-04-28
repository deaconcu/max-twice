import { createApp } from 'vue'
import { VueQueryPlugin, QueryClient, QueryCache, MutationCache } from '@tanstack/vue-query'
import App from './App.vue'
import router from './router'
import pinia from './config/pinia'
import vuetify from './config/vuetify'
import i18n from './i18n'
import { useValidationConfigStore } from './stores/validationConfig'
import { setGlobalRouter } from './composables/config'
import { getGlobalSnackbar, getGlobalRouter } from './composables/config'
import { getErrorMessage } from './composables/utils'
import { ApiError } from './api/client'
import { errorCollector } from './utils/errorCollector'

// 全局样式
import './styles/global.css'

// VueFlow 样式
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/minimap/dist/style.css'
import '@vue-flow/controls/dist/style.css'

const app = createApp(App)

const handleGlobalError = (error: unknown) => {
  const showSnackbar = getGlobalSnackbar()
  const router = getGlobalRouter()
  if (error instanceof ApiError && error.httpStatus === 401) {
    showSnackbar?.(i18n.global.t('error.pleaseLogin'), 'error')
    router?.push('/login')
    return
  }
  showSnackbar?.(getErrorMessage(error), 'error')
}

// TanStack Query 全局默认：
// - retry=false：后端是 HTTP 语义错误码（业务错误不是网络瞬时故障），重试无意义
// - refetchOnWindowFocus=false：国内场景下切 tab 频繁，自动重拉会打爆后端；
//   确实需要"协作实时感"的查询（如通知），在具体 useQuery 里单独开
// - staleTime 不设（=0）：默认每次 mount 都重拉，行为和原有 useFetch 对齐；
//   迁移时不会突然看到老数据，个别高频查询后续再单独调大 staleTime
const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: handleGlobalError,
  }),
  mutationCache: new MutationCache({
    onError: handleGlobalError,
  }),
  defaultOptions: {
    queries: {
      retry: false,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: false,
    },
  },
})

app.use(pinia)
app.use(router)
app.use(vuetify)
app.use(i18n)
app.use(VueQueryPlugin, { queryClient })

// 设置全局 router 实例供 API 错误处理使用
setGlobalRouter(router)

// 初始化全局错误收集
errorCollector.init(app)

// 初始化验证配置（后台异步加载，不阻塞页面）
const validationStore = useValidationConfigStore()
validationStore.init().catch((error) => {
  console.error('[App] 验证配置初始化失败', error)
})

app.mount('#app')
