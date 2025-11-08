import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './config/pinia'
import vuetify from './config/vuetify'
import './style.css'

const app = createApp(App)

app.use(router)
app.use(pinia)
app.use(vuetify)

app.mount('#app')
