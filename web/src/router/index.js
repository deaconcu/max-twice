import { createRouter, createWebHistory } from 'vue-router'

import Login from '../views/Login.vue'
import Main from '../views/Main.vue'
import CourseList from '../views/CourseList.vue'
import Read from '../views/Read.vue'
import Admin from '../views/Admin.vue'
import User from '../views/User.vue'
import Self from '../views/Self.vue'
import Message from '../views/Message.vue'
import RoadmapFlow from '../views/RoadmapFlow.vue'
import CareerCenter from '../views/CareerCenter.vue'
import Learning from '../views/Learning.vue'
import HotRanking from '../views/HotRanking.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    /*
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    */
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },
    {
      path: '/login',
      name: 'login',
      component: Login,
    },
    {
      path: '/',
      name: 'main',
      component: Main,
      children: [
        { path: 'admin', name:"admin", component: Admin},
        { path: 'course/list', name: "courseList", component: CourseList },
        { path: 'ranking', name: "hotRanking", component: HotRanking },
        { path: 'read', name: "read", component: Read },
        { path: 'user', name:"user", component: User },
        { path: 'self', name:"self", component: Self },
        { path: 'message', name:"message", component: Message},
        { path: 'roadmap/:professionId?', name: "roadmap", component: RoadmapFlow},
        { path: 'career', name: "careerCenter", component: CareerCenter},
        { path: 'learning', name: "learning", component: Learning}
      ]
    },

  ],
})

export default router
