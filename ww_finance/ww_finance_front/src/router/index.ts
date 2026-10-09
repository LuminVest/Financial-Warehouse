import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/home' },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' },
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' },
  },
  {
    path: '/invest',
    name: 'InvestList',
    component: () => import('@/views/InvestList.vue'),
    meta: { title: '投资列表' },
  },
  {
    path: '/safety',
    name: 'Safety',
    component: () => import('@/views/StaticPages.vue'),
    meta: { title: '安全保障' },
  },
  {
    path: '/about',
    name: 'About',
    component: () => import('@/views/StaticPages.vue'),
    meta: { title: '关于我们' },
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/views/Chat.vue'),
    meta: { title: '智能客服' },
  },
  {
    path: '/lend/:id',
    name: 'LendDetail',
    component: () => import('@/views/LendDetail.vue'),
    meta: { title: '标的详情' },
  },
  {
    path: '/center',
    name: 'Center',
    component: () => import('@/views/Center.vue'),
    meta: { title: '个人中心', requiresAuth: true },
    children: [
      { path: '', redirect: '/center/profile' },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/center/Profile.vue'),
        meta: { title: '个人中心' },
      },
      {
        path: 'bind',
        name: 'Bind',
        component: () => import('@/views/center/Bind.vue'),
        meta: { title: '账户绑定' },
      },
      {
        path: 'flow',
        name: 'Flow',
        component: () => import('@/views/center/Flow.vue'),
        meta: { title: '资金记录' },
      },
      {
        path: 'borrow-record',
        name: 'BorrowRecord',
        component: () => import('@/views/center/BorrowRecord.vue'),
        meta: { title: '借款记录' },
      },
      {
        path: 'my-lend-return',
        name: 'MyLendReturn',
        component: () => import('@/views/center/MyLendReturn.vue'),
        meta: { title: '还款计划' },
      },
      {
        path: 'charge',
        name: 'Charge',
        component: () => import('@/views/center/Charge.vue'),
        meta: { title: '充值' },
      },
      {
        path: 'withdraw',
        name: 'Withdraw',
        component: () => import('@/views/center/Withdraw.vue'),
        meta: { title: '提现' },
      },
      {
        path: 'borrower-auth',
        name: 'BorrowerAuth',
        component: () => import('@/views/center/BorrowerAuth.vue'),
        meta: { title: '借款人认证' },
      },
      {
        path: 'borrow-apply',
        name: 'BorrowApply',
        component: () => import('@/views/center/BorrowApply.vue'),
        meta: { title: '立即借款' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home',
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to) => {
  document.title = `${String(to.meta.title ?? '')} - 旺旺金融`
  const token = document.cookie.match(/(?:^|;\s*)user_token=([^;]*)/)?.[1]
  // 已登录用户访问登录/注册页时，直接进首页
  if (token && (to.path === '/login' || to.path === '/register')) {
    return { path: '/home' }
  }
  // 公开页面：游客可看，不需要登录
  const publicPages = ['/home', '/invest', '/safety', '/about', '/login', '/register']
  const isPublic = publicPages.some(p => to.path.startsWith(p)) || to.path.startsWith('/lend/')
  // 需要登录的页面：/center/*、/chat
  if (!token && !isPublic) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
