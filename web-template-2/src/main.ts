import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import '@mdi/font/css/materialdesignicons.css'
import 'vuetify/styles'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'

import App from './App.vue'
import router from './router'

// 简约风格主题配置
const vuetify = createVuetify({
  components,
  directives,
  theme: {
    defaultTheme: 'light',
    themes: {
      light: {
        colors: {
          primary: '#1867C0',
          secondary: '#5CBBF6',
          background: '#FFFFFF',
          surface: '#FFFFFF',
          'surface-variant': '#F8F9FA',
          'on-surface': '#1A1A1B',
          'on-background': '#1A1A1B',
          border: '#EDEFF1'
        }
      }
    }
  },
  defaults: {
    VBtn: {
      elevation: 0,
      flat: true,
      style: 'text-transform: none;'
    },
    VCard: {
      elevation: 0,
      flat: true,
      variant: 'outlined'
    }
  }
})

const pinia = createPinia()
const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(vuetify)

app.mount('#app')
