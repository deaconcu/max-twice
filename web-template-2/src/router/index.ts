import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/views/HomePage.vue'
import CareerCenter from '@/views/CareerCenter.vue'
import CourseCenter from '@/views/CourseCenter.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import VerifyEmailView from '@/views/VerifyEmailView.vue'
import MyCareers from '@/views/MyCareers.vue'
import MyCourses from '@/views/MyCourses.vue'
import ReadView from '@/views/ReadView.vue'
import MemoryReviewView from '@/views/MemoryReviewView.vue'
import ProfileView from '@/views/ProfileView.vue'
import RoadmapListView from '@/views/RoadmapListView.vue'
import RoadmapDetailView from '@/views/RoadmapDetailView.vue'
import RoadmapCreateView from '@/views/RoadmapCreateView.vue'
import CourseDetailView from '@/views/CourseDetailView.vue'
import ContentReadView from '@/views/ContentReadView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
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
      path: '/my-careers',
      name: 'my-careers',
      component: MyCareers
    },
    {
      path: '/my-courses',
      name: 'my-courses',
      component: MyCourses
    },
    {
      path: '/read/:nodeId?',
      name: 'read',
      component: ReadView
    },
    {
      path: '/memory-review',
      name: 'memory-review',
      component: MemoryReviewView
    },
    {
      path: '/profile',
      name: 'profile',
      component: ProfileView
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
    },
    {
      path: '/course/:courseId',
      name: 'course-detail',
      component: CourseDetailView
    },
    {
      path: '/course/:courseId/read/:subCourseId',
      name: 'content-read',
      component: ContentReadView
    }
  ]
})

export default router
