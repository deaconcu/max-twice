import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '@/views/Dashboard.vue'
import Login from '@/views/Login.vue'
import axios from 'axios'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: Login,
      meta: { requiresAuth: false },
    },
    {
      path: '/',
      name: 'dashboard',
      component: Dashboard,
      meta: { requiresAuth: true },
    },
  ],
})

// 路由守卫：检查登录状态
router.beforeEach(async (to, from, next) => {
  if (to.meta.requiresAuth) {
    try {
      // 检查是否已登录（调用当前用户接口）
      const response = await axios.get('/api/v1/users/current')
      if (response.data.code === 200) {
        next()
      } else {
        next('/login')
      }
    } catch (error) {
      next('/login')
    }
  } else {
    next()
  }
})

export default router
