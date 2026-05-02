// src/router/index.js

//网页刷新的尝试代码
import { createRouter, createWebHistory } from 'vue-router';
import Home from '../views/Home.vue'
import { useUserInfoStore } from '@/stores/userInfo'
import AdminReview from '@/components/admin/AdminReview.vue' // 导入管理员审核组件
import UserManagement from '@/components/admin/UserManagement.vue' // 导入用户管理组件
import StatisticsPanel from '@/components/admin/StatisticsPanel.vue' // 导入统计面板组件
import SystemSettings from '@/components/admin/SystemSettings.vue' // 导入系统设置组件
import EmojiManagement from '@/components/admin/EmojiManagement.vue' // 导入表情包管理组件

// Vue.use(VueRouter)
const routes = [
  {
    path: '/',
    name: 'Home',
    // component: () => import('@/views/Home.vue')  // 首页对应的组件
    component: Home,
    meta: { requiresAuth: true }  // 明确标记不需要认证
  },
  
  {path: '/login',
  name: 'Login',
  component: () => import('@/views/LoginModal.vue'),
   meta: { guestOnly: true }  // 明确标记为仅访客可访问
  },
  {
    path: '/gallery',
    name: 'Gallery',
    component: () => import('@/views/ArtGallery.vue'),
    meta: {guestOnly: true }  // 明确标记为仅访客可访问
  },

  //画廊页面的尝试代码
  {
    path: '/AIGallery',
    name: 'AIGallery',
    component: () => import('@/views/AIGallery.vue'),
    meta: { requiresAuth: true }  // 需要认证
  },
  {
    path: '/userSubmission',
    name: 'UserSubmission',
    component: () => import('@/views/UserSubmission.vue'),
    meta: { requiresAuth: true }  // 需要认证
  },
     //画廊页面的尝试代码
     {
      path: '/admin',
      name: 'Admin',
      component: () => import('@/components/admin/Navbar.vue'), // 导入管理员导航栏组件
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        { path: 'review', component: AdminReview, name: 'admin-review' },
        { path: 'users', component: UserManagement, name: 'admin-users' },
        { path: 'statistics', component: StatisticsPanel, name: 'admin-statistics' },
        { path: 'settings', component: SystemSettings, name: 'admin-settings' },
        { path: 'emoji', component: EmojiManagement, name: 'admin-emoji' },
        { path: 'dashboard', component: () => import('@/components/admin/Dashboard.vue'), name: 'admin-dashboard' },
        { path: 'logs/operation', component: () => import('@/components/admin/OperationLogs.vue'), name: 'admin-logs-operation' },
        { path: 'logs/login', component: () => import('@/components/admin/LoginRecords.vue'), name: 'admin-logs-login' },
        { path: '', name: 'admin-default', redirect: { name: 'admin-review' } }
      ]
    }

];

const router = createRouter({
  history: createWebHistory(),
  routes
});


// 添加路由守卫保护

router.beforeEach((to, from, next) => {
  const userStore = useUserInfoStore()
  
  // 如果目标路由需要认证
  if (to.meta.requiresAuth) {
    // 用户未登录，重定向到登录页
    if (!userStore.isAuthenticated) {
      return next({
        path: '/login',
        query: { redirect: to.fullPath }  // 保存目标路径以便登录后跳转
      })
    }
    
    // 用户已登录但需要管理员权限
    if (to.meta.requiresAdmin && !userStore.isAdmin) {
      return next('/')  // 非管理员跳转到首页
    }
  }
  
  // 已登录用户访问登录页，跳转到首页
  if (to.path === '/login' && userStore.isAuthenticated) {
    return next('/')
  }
  
  next()  // 正常放行
})
// router.beforeEach((to, from, next) => {
//   const isAuthenticated = checkAuth() // 你的认证检查逻辑
//   const isAdmin = checkAdmin() // 你的管理员检查逻辑
  
//   if (to.matched.some(record => record.meta.requiresAuth)) {
//     if (!isAuthenticated) {
//       next('/login')
//     } else if (to.matched.some(record => record.meta.requiresAdmin)) {
//       if (!isAdmin) {
//         next('/unauthorized')
//       } else {
//         next()
//       }
//     } else {
//       next()
//     }
//   } else {
//     next()
//   }
// })
//网页刷新的尝试代码
// const router = new VueRouter({
//   mode: 'history', // 使用history模式去除URL中的#
//   routes
// });

export default router;