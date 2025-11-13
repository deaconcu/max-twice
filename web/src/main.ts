import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './config/pinia'
import vuetify from './config/vuetify'
import i18n from './i18n'

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

app.mount('#app')
