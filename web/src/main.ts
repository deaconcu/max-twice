import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './config/pinia'
import vuetify from './config/vuetify'
import i18n from './i18n'

const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(vuetify)
app.use(i18n)

app.mount('#app')
