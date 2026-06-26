import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: {
      title: '首页 - AI智能助手平台',
      description: 'AI智能助手平台提供知途AI出行规划和AI超级智能体服务'
    }
  },
  {
    path: '/travel-planner',
    name: 'TravelPlanner',
    component: () => import('../views/TravelPlanner.vue'),
    meta: {
      title: '知途AI出行规划 - AI智能助手平台',
      description: '知途AI出行规划，帮你智能规划旅行方案'
    }
  },
  {
    path: '/super-agent',
    name: 'SuperAgent',
    component: () => import('../views/SuperAgent.vue'),
    meta: {
      title: 'AI超级智能体 - AI智能助手平台',
      description: 'AI超级智能体是全能助手，能解答各类专业问题'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.title) document.title = to.meta.title
  next()
})

export default router
