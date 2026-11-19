import Vue from 'vue'
import VueRouter from 'vue-router'
import store from '@/store'
import MainLayout from '@/layout/MainLayout.vue'

Vue.use(VueRouter)

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { public: true, title: '登录' } },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue'), meta: { public: true, title: '注册' } },
  {
    path: '/',
    component: MainLayout,
    redirect: '/home',
    children: [
      { path: 'home', name: 'Home', component: () => import('@/views/student/Home.vue'), meta: { title: '首页' } },
      { path: 'activities', name: 'ActivityList', component: () => import('@/views/student/ActivityList.vue'), meta: { title: '活动中心' } },
      { path: 'activity/:id', name: 'ActivityDetail', component: () => import('@/views/student/ActivityDetail.vue'), meta: { title: '活动详情' } },
      { path: 'my-registrations', name: 'MyRegistrations', component: () => import('@/views/student/MyRegistrations.vue'), meta: { title: '我的报名' } },
      { path: 'my-reviews', name: 'MyReviews', component: () => import('@/views/student/MyReviews.vue'), meta: { title: '我的评价' } },
      { path: 'notices', name: 'Notices', component: () => import('@/views/student/Notices.vue'), meta: { title: '消息中心' } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/student/Profile.vue'), meta: { title: '个人中心' } },

      { path: 'organizer/activities', name: 'MyActivities', component: () => import('@/views/organizer/MyActivities.vue'), meta: { title: '我的活动', roles: ['ORGANIZER', 'ADMIN'] } },
      { path: 'organizer/activity/edit/:id?', name: 'ActivityEdit', component: () => import('@/views/organizer/ActivityEdit.vue'), meta: { title: '发布活动', roles: ['ORGANIZER', 'ADMIN'] } },
      { path: 'organizer/registrations/:id', name: 'RegistrationList', component: () => import('@/views/organizer/RegistrationList.vue'), meta: { title: '报名名单', roles: ['ORGANIZER', 'ADMIN'] } },
      { path: 'organizer/checkin/:id', name: 'CheckinManage', component: () => import('@/views/organizer/CheckinManage.vue'), meta: { title: '签到管理', roles: ['ORGANIZER', 'ADMIN'] } },
      { path: 'organizer/stats', name: 'ActivityStats', component: () => import('@/views/organizer/ActivityStats.vue'), meta: { title: '活动数据', roles: ['ORGANIZER', 'ADMIN'] } },

      { path: 'admin/audit', name: 'ActivityAudit', component: () => import('@/views/admin/ActivityAudit.vue'), meta: { title: '活动审核', roles: ['ADMIN'] } },
      { path: 'admin/users', name: 'UserManage', component: () => import('@/views/admin/UserManage.vue'), meta: { title: '用户管理', roles: ['ADMIN'] } },
      { path: 'admin/categories', name: 'CategoryManage', component: () => import('@/views/admin/CategoryManage.vue'), meta: { title: '分类管理', roles: ['ADMIN'] } },
      { path: 'admin/announcements', name: 'AnnouncementManage', component: () => import('@/views/admin/AnnouncementManage.vue'), meta: { title: '公告管理', roles: ['ADMIN'] } },
      { path: 'admin/stats', name: 'PlatformStats', component: () => import('@/views/admin/PlatformStats.vue'), meta: { title: '平台统计', roles: ['ADMIN'] } },
      { path: 'admin/configs', name: 'SysConfig', component: () => import('@/views/admin/SysConfig.vue'), meta: { title: '系统参数', roles: ['ADMIN'] } }
    ]
  },
  { path: '*', name: 'NotFound', component: () => import('@/views/NotFound.vue'), meta: { public: true, title: '页面不存在' } }
]

const router = new VueRouter({
  mode: 'hash',
  routes,
  scrollBehavior: () => ({ y: 0 })
})

router.beforeEach((to, from, next) => {
  document.title = (to.meta && to.meta.title ? to.meta.title + ' - ' : '') + '校园活动管理系统'
  if (to.meta && to.meta.public) {
    next()
    return
  }
  if (!store.getters.isLogin) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  const roles = to.meta && to.meta.roles
  if (roles && roles.indexOf(store.getters.role) < 0) {
    next({ path: '/home' })
    return
  }
  next()
})

export default router
