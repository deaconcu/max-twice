import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import VerifyEmailView from '@/views/VerifyEmailView.vue'
import ReadView from '@/views/ReadView.vue'
import CareerCenter from '@/views/CareerCenter.vue'
import CourseCenter from '@/views/CourseCenter.vue'
import MyCourses from '@/views/MyCourses.vue'
import MyCareers from '@/views/MyCareers.vue'
import HomePage from '@/views/HomePage.vue'
import MemoryReviewView from '@/views/MemoryReviewView.vue'
import RoadmapListView from '@/views/RoadmapListView.vue'
import RoadmapDetailView from '@/views/RoadmapDetailView.vue'
import RoadmapCreateView from '@/views/RoadmapCreateView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior(to, from, savedPosition) {
    // 如果有保存的位置（浏览器前进/后退），使用保存的位置
    if (savedPosition) {
      return savedPosition
    } else {
      // 否则滚动到顶部
      return { top: 0 }
    }
  },
  routes: [
    {
      path: '/',
      redirect: '/home'
    },
    {
      path: '/home',
      name: 'home',
      component: HomePage
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
    },
    {
      path: '/career',
      name: 'career',
      component: CareerCenter
    },
    {
      path: '/learning',
      name: 'learning',
      component: CourseCenter
    },
    {
      path: '/my-courses',
      name: 'my-courses',
      component: MyCourses
    },
    {
      path: '/my-careers',
      name: 'my-careers',
      component: MyCareers
    },
    {
      path: '/my-memory-review',
      name: 'my-memory-review',
      component: MemoryReviewView
    },
    {
      path: '/roadmap/:professionId',
      name: 'roadmap-list',
      component: RoadmapListView
    },
    {
      path: '/roadmap/:professionId/detail/:roadmapId',
      name: 'roadmap-detail',
      component: RoadmapDetailView
    },
    {
      path: '/roadmap/:professionId/create',
      name: 'roadmap-create',
      component: RoadmapCreateView
    }
  ]
})

export default router
