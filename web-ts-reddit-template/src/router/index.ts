import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import VerifyEmailView from '@/views/VerifyEmailView.vue'
import ReadView from '@/views/ReadView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView
    },
    {
      path: '/verify-email',
      name: 'verify-email',
      component: VerifyEmailView
    },
    {
      path: '/read',
      name: 'read',
      component: ReadView
    }
  ]
})

export default router
