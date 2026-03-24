import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './config/pinia'
import vuetify from './config/vuetify'
import i18n from './i18n'
import { useValidationConfigStore } from './stores/validationConfig'
import { setGlobalRouter } from './composables/config'
import { errorCollector } from './utils/errorCollector'

// 全局样式
import './styles/global.css'

// VueFlow 样式
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/minimap/dist/style.css'
import '@vue-flow/controls/dist/style.css'

const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(vuetify)
app.use(i18n)

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
