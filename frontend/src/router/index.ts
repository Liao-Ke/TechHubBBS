import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

// Extend route meta
declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    requiresAuth?: boolean
    roles?: string[]
    layout?: 'default' | 'admin' | 'auth'
  }
}

const routes: RouteRecordRaw[] = [
  // === Public / Main Routes (DefaultLayout) ===
  {
    path: '/',
    name: 'home',
    component: () => import('@/pages/home/HomePage.vue'),
    meta: { title: 'TechHub', layout: 'default' },
  },
  {
    path: '/categories/:categoryId',
    name: 'category',
    component: () => import('@/pages/category/CategoryPage.vue'),
    meta: { title: '版块', layout: 'default' },
  },
  {
    path: '/posts/:id',
    name: 'post-detail',
    component: () => import('@/pages/post/PostDetailPage.vue'),
    meta: { title: '帖子详情', layout: 'default' },
  },
  {
    path: '/notices/:id',
    name: 'notice-detail',
    component: () => import('@/pages/notice/NoticeDetailPage.vue'),
    meta: { title: '公告详情', layout: 'default' },
  },
  {
    path: '/posts/new',
    name: 'post-create',
    component: () => import('@/pages/post/PostCreatePage.vue'),
    meta: { title: '发布帖子', requiresAuth: true, layout: 'default' },
  },
  {
    path: '/posts/:id/edit',
    name: 'post-edit',
    component: () => import('@/pages/post/PostEditPage.vue'),
    meta: { title: '编辑帖子', requiresAuth: true, layout: 'default' },
  },
  {
    path: '/users/:id',
    name: 'user-profile',
    component: () => import('@/pages/user/ProfilePage.vue'),
    meta: { title: '用户主页', layout: 'default' },
  },
  {
    path: '/settings',
    name: 'settings',
    component: () => import('@/pages/user/SettingsPage.vue'),
    meta: { title: '设置', requiresAuth: true, layout: 'default' },
  },
  {
    path: '/notifications',
    name: 'notifications',
    component: () => import('@/pages/notification/NotificationPage.vue'),
    meta: { title: '通知', requiresAuth: true, layout: 'default' },
  },
  {
    path: '/drafts',
    name: 'drafts',
    component: () => import('@/pages/draft/DraftPage.vue'),
    meta: { title: '草稿箱', requiresAuth: true, layout: 'default' },
  },

  // === Auth Routes (AuthLayout) ===
  {
    path: '/login',
    name: 'login',
    component: () => import('@/pages/auth/LoginPage.vue'),
    meta: { title: '登录', layout: 'auth' },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/pages/auth/RegisterPage.vue'),
    meta: { title: '注册', layout: 'auth' },
  },

  // === Admin Routes (AdminLayout) ===
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN', 'MODERATOR'] },
    children: [
      {
        path: '',
        name: 'admin-dashboard',
        component: () => import('@/pages/admin/DashboardPage.vue'),
        meta: { title: '管理仪表盘' },
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/pages/admin/UserManagePage.vue'),
        meta: { title: '用户管理', roles: ['ADMIN'] },
      },
      {
        path: 'posts',
        name: 'admin-posts',
        component: () => import('@/pages/admin/PostManagePage.vue'),
        meta: { title: '帖子管理' },
      },
      {
        path: 'categories',
        name: 'admin-categories',
        component: () => import('@/pages/admin/CategoryManagePage.vue'),
        meta: { title: '版块管理', roles: ['ADMIN'] },
      },
      {
        path: 'notices',
        name: 'admin-notices',
        component: () => import('@/pages/admin/NoticeManagePage.vue'),
        meta: { title: '公告管理' },
      },
      {
        path: 'divine',
        name: 'admin-divine',
        component: () => import('@/pages/admin/DivineManagePage.vue'),
        meta: { title: '神评管理', roles: ['ADMIN'] },
      },
    ],
  },

  // === Error Pages ===
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/pages/error/ForbiddenPage.vue'),
    meta: { title: '无权限', layout: 'default' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/pages/error/NotFoundPage.vue'),
    meta: { title: '页面不存在', layout: 'default' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

export default router
