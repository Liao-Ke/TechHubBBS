# TechHub 前端开发文档

> 基于「TechHub 技术社区论坛 —— 综合课程设计方案」的前端工程实现文档
> 技术栈：Vue 3 + TypeScript + Vite + Element Plus + Pinia + Vue Router 4 + ofetch + ECharts

---

## 目录

1. [技术栈与版本](#1-技术栈与版本)
2. [项目初始化](#2-项目初始化)
3. [目录结构](#3-目录结构)
4. [架构设计](#4-架构设计)
5. [路由设计](#5-路由设计)
6. [状态管理](#6-状态管理)
7. [API 层设计](#7-api-层设计)
8. [核心功能实现](#8-核心功能实现)
   - [8.1 Markdown 编辑器与安全渲染](#81-markdown-编辑器与安全渲染)
   - [8.2 帖子发布与可见权限](#82-帖子发布与可见权限)
   - [8.3 草稿自动保存与恢复](#83-草稿自动保存与恢复)
   - [8.4 AI 总结与问答面板](#84-ai-总结与问答面板)
   - [8.5 神评机制前端](#85-神评机制前端)
   - [8.6 通知系统](#86-通知系统)
   - [8.7 版块公告展示](#87-版块公告展示)
   - [8.8 管理后台](#88-管理后台)
   - [8.9 文件上传](#89-文件上传头像--帖子内嵌图片)
   - [8.10 权限指令](#810-权限指令)
9. [安全措施](#9-安全措施)
10. [构建与部署](#10-构建与部署)
11. [组件清单](#11-组件清单)

---

## 1. 技术栈与版本

| 类别       | 技术                               | 用途说明                       |
| ---------- | ---------------------------------- | ------------------------------ |
| 框架       | Vue 3.5+                           | 组合式 API，`<script setup>`   |
| 语言       | TypeScript 5.x                     | 类型安全                       |
| 构建工具   | Vite 6.x                           | 开发服务器，HMR，生产构建      |
| UI 组件库  | Element Plus 2.x                   | 统一 UI，表单/表格/弹窗/分页等 |
| 状态管理   | Pinia 2.x                          | 用户信息、未读通知数           |
| 路由       | Vue Router 4.x                     | 动态路由，导航守卫             |
| HTTP 客户端 | ofetch 1.x                         | 拦截器统一处理 Token 与错误    |
| Markdown   | markdown-it 14.x + DOMPurify 3.x   | 安全渲染                       |
| 代码高亮   | highlight.js 11.x                  | 代码块语法高亮                 |
| 图表       | vue-echarts 7.x (Apache ECharts 5) | 管理后台数据统计               |
| 图标       | @element-plus/icons-vue            | 统一图标风格                   |
| 文件上传   | ofetch (multipart/form-data)       | 头像上传、帖子内嵌图片上传     |

> **Node.js 最低版本**：`^20.19.0 || >=22.12.0`（Vite 6 要求）

---

## 2. 项目初始化

### 2.1 创建项目

```bash
pnpm create vue@latest techhub-frontend
```

在交互式选项中勾选：
- TypeScript
- Vue Router
- Pinia

### 2.2 安装依赖

```bash
cd techhub-frontend

# UI 组件库
pnpm add element-plus @element-plus/icons-vue

# Markdown 渲染
pnpm add markdown-it highlight.js dompurify
pnpm add -D @types/markdown-it @types/dompurify

# HTTP 客户端
pnpm add ofetch

# 图表
pnpm add echarts vue-echarts

# 按需导入插件
pnpm add -D unplugin-auto-import unplugin-vue-components unplugin-element-plus
```

### 2.3 Vite 配置

```typescript
// vite.config.ts
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import ElementPlus from 'unplugin-element-plus/vite'

export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 组件按需自动导入
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
    // Element Plus 样式按需导入
    ElementPlus({}),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

### 2.4 TypeScript 配置

```jsonc
// tsconfig.json (关键字段)
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    },
    "types": ["element-plus/global"]
  }
}
```

---

## 3. 目录结构

```
techhub-frontend/
├── public/
│   └── favicon.ico
├── src/
│   ├── api/                          # API 请求层
│   │   ├── index.ts                  # ofetch 实例 + 拦截器
│   │   ├── modules/                  # 按模块拆分
│   │   │   ├── auth.ts              # 认证相关 API
│   │   │   ├── user.ts              # 用户相关 API
│   │   │   ├── post.ts              # 帖子相关 API
│   │   │   ├── comment.ts           # 回复 / 神评 API
│   │   │   ├── category.ts          # 版块 API
│   │   │   ├── notification.ts      # 通知 API
│   │   │   ├── recommendation.ts    # 推荐 API
│   │   │   ├── ai.ts                # AI 总结 / 问答 API
│   │   │   ├── draft.ts             # 草稿 API
│   │   │   ├── notice.ts            # 版块公告 API
│   │   │   ├── file.ts              # 文件上传 API
│   │   │   └── admin.ts             # 管理后台 API
│   │   └── types/                   # 接口响应类型
│   │       ├── response.ts          # 统一响应体泛型
│   │       ├── user.ts
│   │       ├── post.ts
│   │       ├── comment.ts
│   │       ├── notification.ts
│   │       ├── file.ts              # 文件上传响应类型
│   │       └── ...
│   ├── assets/                       # 静态资源
│   │   ├── styles/
│   │   │   ├── variables.scss       # SCSS 变量
│   │   │   ├── markdown.scss        # Markdown 渲染样式
│   │   │   └── global.scss          # 全局样式
│   │   └── images/
│   ├── components/                   # 公共组件
│   │   ├── common/                   # 通用组件
│   │   │   ├── AppHeader.vue        # 全局头部导航
│   │   │   ├── AppFooter.vue        # 全局底部
│   │   │   ├── AppSidebar.vue       # 侧边栏
│   │   │   ├── UserAvatar.vue       # 用户头像
│   │   │   ├── ImageUpload.vue      # 图片上传（头像/帖子图片）
│   │   │   ├── EmptyState.vue       # 空状态
│   │   │   └── LoadingSkeleton.vue  # 骨架屏
│   │   ├── markdown/                 # Markdown 相关
│   │   │   ├── MdEditor.vue         # Markdown 编辑器
│   │   │   ├── MdViewer.vue         # Markdown 安全渲染器
│   │   │   └── Toolbar.vue          # 编辑器工具栏
│   │   ├── post/                     # 帖子相关
│   │   │   ├── PostCard.vue         # 帖子卡片（列表项）
│   │   │   ├── PostList.vue         # 帖子列表（分页）
│   │   │   ├── VisibilitySelector.vue # 可见权限选择器
│   │   │   └── DivineCommentBadge.vue # 神评徽章
│   │   ├── user/                     # 用户相关
│   │   │   ├── LoginForm.vue
│   │   │   └── RegisterForm.vue
│   │   ├── notice/                   # 公告相关
│   │   │   ├── NoticeBanner.vue     # 版块公告横幅
│   │   │   └── NoticeCarousel.vue   # 公告轮播
│   │   ├── ai/                       # AI 相关
│   │   │   ├── AiSummaryPanel.vue   # AI 总结面板
│   │   │   └── AiQaPanel.vue        # AI 问答面板
│   │   ├── notification/             # 通知相关
│   │   │   └── NotificationBell.vue # 通知铃铛 + 下拉
│   │   └── admin/                    # 管理后台组件
│   │       ├── StatChart.vue        # ECharts 统计图表
│   │       ├── UserTable.vue        # 用户管理表格
│   │       └── PostTable.vue        # 帖子管理表格
│   ├── composables/                  # 组合式函数
│   │   ├── useAuth.ts               # 认证逻辑
│   │   ├── useDraft.ts              # 草稿自动保存
│   │   ├── usePagination.ts         # 分页逻辑
│   │   ├── useInfiniteScroll.ts     # 无限滚动
│   │   ├── useVisibility.ts         # 可见权限判断
│   │   ├── useFileUpload.ts         # 文件上传（进度跟踪）
│   │   └── usePermission.ts         # 权限检查
│   ├── directives/                   # 自定义指令
│   │   └── permission.ts            # v-permission 权限指令
│   ├── layouts/                      # 布局组件
│   │   ├── DefaultLayout.vue        # 默认布局（头部 + 内容 + 底部）
│   │   ├── AdminLayout.vue          # 管理后台布局
│   │   └── AuthLayout.vue           # 登录/注册布局
│   ├── pages/                        # 页面组件（路由级别）
│   │   ├── auth/
│   │   │   ├── LoginPage.vue
│   │   │   └── RegisterPage.vue
│   │   ├── home/
│   │   │   └── HomePage.vue         # 首页（推荐流）
│   │   ├── category/
│   │   │   └── CategoryPage.vue     # 版块帖子列表
│   │   ├── post/
│   │   │   ├── PostCreatePage.vue   # 发布帖子
│   │   │   ├── PostEditPage.vue     # 编辑帖子
│   │   │   └── PostDetailPage.vue   # 帖子详情
│   │   ├── user/
│   │   │   ├── ProfilePage.vue      # 个人主页
│   │   │   └── SettingsPage.vue     # 个人设置
│   │   ├── notification/
│   │   │   └── NotificationPage.vue # 通知列表
│   │   ├── draft/
│   │   │   └── DraftPage.vue        # 草稿箱
│   │   ├── admin/
│   │   │   ├── DashboardPage.vue    # 管理仪表盘
│   │   │   ├── UserManagePage.vue
│   │   │   ├── PostManagePage.vue
│   │   │   ├── CategoryManagePage.vue
│   │   │   ├── NoticeManagePage.vue
│   │   │   └── DivineManagePage.vue
│   │   └── error/
│   │       ├── NotFoundPage.vue     # 404
│   │       └── ForbiddenPage.vue    # 403
│   ├── router/
│   │   └── index.ts                 # 路由配置 + 守卫
│   ├── stores/                       # Pinia 状态管理
│   │   ├── user.ts                  # 用户信息 / Token
│   │   ├── notification.ts          # 未读通知数
│   │   ├── draft.ts                 # 草稿状态
│   │   └── app.ts                   # 全局设置（侧边栏折叠等）
│   ├── utils/                        # 工具函数
│   │   ├── token.ts                 # Token 存取（localStorage）
│   │   ├── format.ts               # 日期/数字格式化
│   │   └── sanitize.ts             # HTML 清洗
│   ├── App.vue
│   └── main.ts
├── .env                              # 环境变量（开发）
├── .env.production                   # 环境变量（生产）
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── eslint.config.ts
```

---

## 4. 架构设计

### 4.1 分层架构

```
┌─────────────────────────────────────────────────┐
│                    Pages (路由页面)                │
│  HomePage  PostDetailPage  AdminDashboard  ...   │
├─────────────────────────────────────────────────┤
│                  Components (组件)                │
│  PostCard  MdEditor  AiPanel  StatChart  ...     │
├─────────────────────────────────────────────────┤
│                 Composables (逻辑)                │
│  useAuth  useDraft  usePagination  useVisibility │
├──────────────────────┬──────────────────────────┤
│   Stores (Pinia)     │    API Layer (ofetch)     │
│   user / notif /     │    modules/auth.ts        │
│   draft / app        │    modules/post.ts ...    │
├──────────────────────┴──────────────────────────┤
│         Utils / Directives / Types               │
└─────────────────────────────────────────────────┘
```

### 4.2 组件树（核心页面）

```
App.vue
├── AppHeader.vue (全局导航 + 通知铃铛 + 用户菜单)
├── <RouterView>
│   ├── DefaultLayout.vue
│   │   ├── HomePage.vue
│   │   │   ├── NoticeCarousel.vue          ← 公告轮播
│   │   │   ├── PostList.vue                ← 推荐流
│   │   │   │   └── PostCard.vue × N        ← 帖子卡片
│   │   │   └── LoadingSkeleton.vue
│   │   ├── CategoryPage.vue
│   │   │   ├── NoticeBanner.vue            ← 版块公告横幅
│   │   │   ├── PostList.vue
│   │   │   └── Pagination
│   │   ├── PostDetailPage.vue
│   │   │   ├── MdViewer.vue                ← 帖子正文渲染
│   │   │   ├── VisibilitySelector.vue       ← 编辑时可见
│   │   │   ├── DivineCommentBadge.vue × N  ← 神评区
│   │   │   ├── AiSummaryPanel.vue          ← AI 总结
│   │   │   │   └── AiQaPanel.vue           ← AI 问答
│   │   │   ├── Comment Section             ← 回复列表
│   │   │   └── Related Posts               ← 相关帖子
│   │   ├── PostCreatePage.vue
│   │   │   ├── MdEditor.vue                ← Markdown 编辑器
│   │   │   │   └── Toolbar.vue
│   │   │   └── VisibilitySelector.vue       ← 可见权限
│   │   ├── UserProfilePage.vue
│   │   ├── NotificationPage.vue
│   │   └── DraftPage.vue
│   ├── AdminLayout.vue
│   │   ├── AppSidebar.vue (管理菜单)
│   │   ├── DashboardPage.vue
│   │   │   └── StatChart.vue × N           ← ECharts
│   │   ├── UserManagePage.vue → UserTable.vue
│   │   ├── PostManagePage.vue → PostTable.vue
│   │   ├── CategoryManagePage.vue
│   │   ├── NoticeManagePage.vue
│   │   └── DivineManagePage.vue
│   └── AuthLayout.vue
│       ├── LoginPage.vue → LoginForm.vue
│       └── RegisterPage.vue → RegisterForm.vue
└── AppFooter.vue
```

---

## 5. 路由设计

### 5.1 路由表

```typescript
// src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

// 路由元信息类型扩展
declare module 'vue-router' {
  interface RouteMeta {
    title?: string           // 页面标题
    requiresAuth?: boolean   // 需要登录
    roles?: string[]         // 允许的角色 ['USER', 'MODERATOR', 'ADMIN']
    layout?: 'default' | 'admin' | 'auth'
  }
}

const routes: RouteRecordRaw[] = [
  // ==================== 公开路由 ====================
  {
    path: '/',
    name: 'Home',
    component: () => import('@/pages/home/HomePage.vue'),
    meta: { title: 'TechHub - 首页', layout: 'default' },
  },
  {
    path: '/categories/:categoryId',
    name: 'Category',
    component: () => import('@/pages/category/CategoryPage.vue'),
    meta: { title: '版块', layout: 'default' },
  },
  {
    path: '/posts/:id',
    name: 'PostDetail',
    component: () => import('@/pages/post/PostDetailPage.vue'),
    meta: { title: '帖子详情', layout: 'default' },
  },
  {
    path: '/users/:id',
    name: 'UserProfile',
    component: () => import('@/pages/user/ProfilePage.vue'),
    meta: { title: '用户主页', layout: 'default' },
  },

  // ==================== 需登录路由 ====================
  {
    path: '/posts/new',
    name: 'PostCreate',
    component: () => import('@/pages/post/PostCreatePage.vue'),
    meta: { title: '发布帖子', requiresAuth: true, layout: 'default' },
  },
  {
    path: '/posts/:id/edit',
    name: 'PostEdit',
    component: () => import('@/pages/post/PostEditPage.vue'),
    meta: { title: '编辑帖子', requiresAuth: true, layout: 'default' },
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/pages/notification/NotificationPage.vue'),
    meta: { title: '通知', requiresAuth: true, layout: 'default' },
  },
  {
    path: '/drafts',
    name: 'Drafts',
    component: () => import('@/pages/draft/DraftPage.vue'),
    meta: { title: '草稿箱', requiresAuth: true, layout: 'default' },
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/pages/user/SettingsPage.vue'),
    meta: { title: '设置', requiresAuth: true, layout: 'default' },
  },

  // ==================== 认证路由（未登录可访问） ====================
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/auth/LoginPage.vue'),
    meta: { title: '登录', layout: 'auth' },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/pages/auth/RegisterPage.vue'),
    meta: { title: '注册', layout: 'auth' },
  },

  // ==================== 管理后台路由（需管理员/版主） ====================
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN', 'MODERATOR'], layout: 'admin' },
    children: [
      {
        path: '',
        name: 'AdminDashboard',
        component: () => import('@/pages/admin/DashboardPage.vue'),
        meta: { title: '管理仪表盘', roles: ['ADMIN'] },
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/pages/admin/UserManagePage.vue'),
        meta: { title: '用户管理', roles: ['ADMIN'] },
      },
      {
        path: 'posts',
        name: 'AdminPosts',
        component: () => import('@/pages/admin/PostManagePage.vue'),
        meta: { title: '帖子管理', roles: ['ADMIN', 'MODERATOR'] },
      },
      {
        path: 'categories',
        name: 'AdminCategories',
        component: () => import('@/pages/admin/CategoryManagePage.vue'),
        meta: { title: '版块管理', roles: ['ADMIN'] },
      },
      {
        path: 'notices',
        name: 'AdminNotices',
        component: () => import('@/pages/admin/NoticeManagePage.vue'),
        meta: { title: '公告管理', roles: ['ADMIN', 'MODERATOR'] },
      },
      {
        path: 'divine',
        name: 'AdminDivine',
        component: () => import('@/pages/admin/DivineManagePage.vue'),
        meta: { title: '神评管理', roles: ['ADMIN'] },
      },
    ],
  },

  // ==================== 错误页面 ====================
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/pages/error/ForbiddenPage.vue'),
    meta: { title: '无权限', layout: 'default' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
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
```

### 5.2 导航守卫

```typescript
// src/router/guards.ts
import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'

export function setupRouterGuards(router: Router) {
  // 全局前置守卫：认证与权限
  router.beforeEach((to, from) => {
    const userStore = useUserStore()

    // 1. 设置页面标题
    document.title = (to.meta.title as string) || 'TechHub'

    // 2. 需要登录但未登录 → 跳转登录页
    if (to.meta.requiresAuth && !userStore.isLoggedIn) {
      return {
        path: '/login',
        query: { redirect: to.fullPath },
      }
    }

    // 3. 已登录访问登录/注册页 → 跳转首页
    if (userStore.isLoggedIn && (to.name === 'Login' || to.name === 'Register')) {
      return { path: '/' }
    }

    // 4. 角色权限校验
    const requiredRoles = to.meta.roles as string[] | undefined
    if (requiredRoles && requiredRoles.length > 0) {
      const hasRole = requiredRoles.includes(userStore.role)
      if (!hasRole) {
        return { path: '/403', replace: true }
      }
    }
  })
}
```

```typescript
// main.ts 中注册
import { setupRouterGuards } from './router/guards'
setupRouterGuards(router)
```

---

## 6. 状态管理

### 6.1 用户 Store（useUserStore）

```typescript
// src/stores/user.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/token'
import { authApi, userApi } from '@/api'

interface UserInfo {
  id: string
  username: string
  email: string
  avatarUrl: string
  bio: string
  role: 'USER' | 'MODERATOR' | 'ADMIN'
}

export const useUserStore = defineStore('user', () => {
  // --- State ---
  const token = ref<string | null>(getToken())
  const userInfo = ref<UserInfo | null>(null)

  // --- Getters ---
  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role || 'USER')
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isModerator = computed(() => role.value === 'MODERATOR' || role.value === 'ADMIN')

  // --- Actions ---
  async function login(username: string, password: string) {
    const res = await authApi.login({ username, password })
    token.value = res.token
    setToken(res.token)
    await fetchUserInfo()
  }

  async function fetchUserInfo() {
    userInfo.value = await userApi.getMe()
  }

  async function register(data: { username: string; password: string; email: string }) {
    await authApi.register(data)
  }

  function logout() {
    token.value = null
    userInfo.value = null
    removeToken()
  }

  return { token, userInfo, isLoggedIn, role, isAdmin, isModerator, login, register, logout, fetchUserInfo }
})
```

### 6.2 通知 Store（useNotificationStore）

```typescript
// src/stores/notification.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { notificationApi } from '@/api'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const hasNew = computed(() => unreadCount.value > 0)

  async function fetchUnreadCount() {
    unreadCount.value = await notificationApi.getUnreadCount()
  }

  function increment() {
    unreadCount.value++
  }

  function decrement(count = 1) {
    unreadCount.value = Math.max(0, unreadCount.value - count)
  }

  function reset() {
    unreadCount.value = 0
  }

  return { unreadCount, hasNew, fetchUnreadCount, increment, decrement, reset }
})
```

### 6.3 应用 Store（useAppStore）

```typescript
// src/stores/app.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const isMobileMenuOpen = ref(false)

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return { sidebarCollapsed, isMobileMenuOpen, toggleSidebar }
})
```

---

## 7. API 层设计

### 7.1 ofetch 实例与拦截器

```typescript
// src/api/index.ts
import { ofetch } from 'ofetch'
import type { ApiResponse } from './types/response'
import { useUserStore } from '@/stores/user'
import { router } from '@/router'
import { ElMessage } from 'element-plus'

export const api = ofetch.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },

  // 请求拦截器：自动附加 Token
  async onRequest({ options }) {
    const userStore = useUserStore()
    if (userStore.token) {
      options.headers.set('Authorization', `Bearer ${userStore.token}`)
    }
  },

  // 请求错误处理
  async onRequestError({ request, error }) {
    console.error(`[Request Error] ${request}:`, error.message)
  },

  // 响应处理
  async onResponse({ response }) {
    // 统一响应格式 { code, message, data }
    const body = response._data
    if (body && body.code !== undefined && body.code !== 200) {
      // 业务错误
      throw new ApiError(body.code, body.message || '请求失败')
    }
  },

  // 响应错误处理：401 跳登录，403 提示
  async onResponseError({ response }) {
    const body = response._data
    switch (response.status) {
      case 401: {
        const userStore = useUserStore()
        userStore.logout()
        router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
        ElMessage.error('登录已过期，请重新登录')
        break
      }
      case 403:
        ElMessage.error('没有权限执行此操作')
        break
      case 404:
        // 由业务侧处理，不全局提示
        break
      default:
        ElMessage.error(body?.message || '网络错误，请稍后再试')
    }
  },
})

// 自定义业务异常
export class ApiError extends Error {
  constructor(
    public code: number,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

// 响应数据类型
export type { ApiResponse }
```

### 7.2 API 模块示例

```typescript
// src/api/modules/post.ts
import { api } from '../index'
import type { Post, PostListParams, PostListResult } from '../types/post'

export const postApi = {
  /** 帖子列表（含可见性过滤、分页、排序） */
  getList(params: PostListParams) {
    return api<PostListResult>('/posts', { query: params })
  },

  /** 帖子详情 */
  getDetail(id: string) {
    return api<Post>('/posts/' + id)
  },

  /** 发布帖子 */
  create(data: { title: string; content: string; categoryId: string; visibility: number; draftPostId?: string }) {
    return api<Post>('/posts', { method: 'POST', body: data })
  },

  /** 编辑帖子 */
  update(id: string, data: Partial<{ title: string; content: string; visibility: number }>) {
    return api<Post>('/posts/' + id, { method: 'PATCH', body: data })
  },

  /** 删除帖子 */
  remove(id: string) {
    return api<void>('/posts/' + id, { method: 'DELETE' })
  },

  /** 点赞帖子 */
  like(id: string) {
    return api<void>('/posts/' + id + '/likes', { method: 'POST' })
  },

  /** 取消点赞 */
  unlike(id: string) {
    return api<void>('/posts/' + id + '/likes', { method: 'DELETE' })
  },

  /** 收藏帖子 */
  favorite(id: string) {
    return api<void>('/posts/' + id + '/favorites', { method: 'POST' })
  },

  /** 取消收藏 */
  unfavorite(id: string) {
    return api<void>('/posts/' + id + '/favorites', { method: 'DELETE' })
  },
}
```

### 7.3 文件上传 API

```typescript
// src/api/modules/file.ts
import { api } from '../index'
import type { FileUploadResult } from '../types/file'

export const fileApi = {
  /**
   * 上传文件（头像、帖子内嵌图片等）
   * 使用 FormData 发送 multipart/form-data 请求
   */
  upload(file: File, onProgress?: (percent: number) => void) {
    const formData = new FormData()
    formData.append('file', file)

    return api<FileUploadResult>('/files/upload', {
      method: 'POST',
      body: formData,
      // 不设置 Content-Type，让浏览器自动设置 multipart/form-data 的 boundary
      headers: {},
      // 上传进度回调
      ...(onProgress ? {
        onUploadProgress: (event: { loaded: number; total: number }) => {
          const percent = Math.round((event.loaded / event.total) * 100)
          onProgress(percent)
        },
      } : {}),
    } as any)
  },
}
```

```typescript
// src/api/types/file.ts
export interface FileUploadResult {
  url: string       // 文件访问 URL，如 http://localhost:8080/file/avatar/xxx.png
  filename: string  // 原始文件名
  size: number      // 文件大小（字节）
}
```

### 7.4 TypeScript 类型定义（帖子）

```typescript
// src/api/types/response.ts
/** 后端统一响应体 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

// src/api/types/post.ts
export interface Post {
  id: string
  title: string
  content: string
  categoryId: string
  categoryName: string
  authorId: string
  authorName: string
  authorAvatar: string
  type: 0 | 1 | 2          // 0普通 1精华 2置顶
  status: 0 | 1 | 2        // 0锁定 1正常 2已删除
  visibility: 0 | 1 | 2 | 3 // 0公开 1登录可见 2粉丝可见 3私密
  viewCount: number
  likeCount: number
  commentCount: number
  divineCommentCount: number
  createTime: string
  updateTime: string
  liked: boolean            // 当前用户是否已点赞
  favorited: boolean        // 当前用户是否已收藏
}

export interface PostListParams {
  page?: number
  size?: number
  categoryId?: string
  sort?: 'latest' | 'hot'
  keyword?: string
}

export interface PostListResult {
  records: Post[]
  total: number
  page: number
  size: number
}
```

---

## 8. 核心功能实现

### 8.1 Markdown 编辑器与安全渲染

#### 8.1.1 Markdown 渲染器（MdViewer）

```typescript
// src/utils/markdown.ts
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import DOMPurify from 'dompurify'
import 'highlight.js/styles/github-dark.css'

// 创建 markdown-it 实例
const md = new MarkdownIt({
  html: false,            // ⚠️ 禁用原始 HTML，防 XSS
  linkify: true,           // 自动识别 URL
  typographer: true,       // 智能引号、破折号
  breaks: true,            // 换行转 <br>
  langPrefix: 'language-',
  highlight(str: string, lang: string) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${
          hljs.highlight(str, { language: lang, ignoreIllegals: true }).value
        }</code></pre>`
      } catch {
        // fall through to auto-detect
      }
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  },
})

/**
 * 渲染 Markdown 为安全的 HTML
 * 流程：Markdown → HTML → DOMPurify 清洗 → 安全 HTML
 */
export function renderMarkdown(content: string): string {
  const rawHtml = md.render(content)
  return DOMPurify.sanitize(rawHtml, {
    ALLOWED_TAGS: [
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'p', 'br', 'hr',
      'ul', 'ol', 'li',
      'blockquote', 'pre', 'code',
      'table', 'thead', 'tbody', 'tr', 'th', 'td',
      'strong', 'em', 'del', 'ins', 'sub', 'sup',
      'a', 'img',
      'span', 'div',
    ],
    ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'class', 'id', 'target'],
  })
}
```

```vue
<!-- src/components/markdown/MdViewer.vue -->
<template>
  <div class="markdown-body" v-html="safeHtml" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps<{ content: string }>()
const safeHtml = computed(() => renderMarkdown(props.content))
</script>

<style lang="scss">
@use '@/assets/styles/markdown.scss';
</style>
```

#### 8.1.2 Markdown 编辑器（MdEditor）

```vue
<!-- src/components/markdown/MdEditor.vue -->
<template>
  <div class="md-editor">
    <div class="md-editor__toolbar">
      <Toolbar @insert="handleInsert" />
    </div>
    <div class="md-editor__body">
      <el-input
        v-model="localContent"
        type="textarea"
        :rows="15"
        placeholder="支持 Markdown 语法..."
        class="md-editor__textarea"
        @input="handleInput"
      />
      <div class="md-editor__preview">
        <MdViewer :content="localContent" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import MdViewer from './MdViewer.vue'
import Toolbar from './Toolbar.vue'

const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const localContent = ref(props.modelValue)

watch(() => props.modelValue, (val) => { localContent.value = val })

function handleInput(value: string) {
  emit('update:modelValue', value)
}

function handleInsert(markdown: string) {
  localContent.value += markdown
  emit('update:modelValue', localContent.value)
}
</script>

<style lang="scss" scoped>
.md-editor {
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  overflow: hidden;

  &__body {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0;
  }

  &__textarea {
    :deep(.el-textarea__inner) {
      border: none;
      border-radius: 0;
      resize: none;
      font-family: 'Cascadia Code', 'Fira Code', monospace;
      font-size: 14px;
      line-height: 1.6;
    }
  }

  &__preview {
    padding: 12px 16px;
    border-left: 1px solid var(--el-border-color);
    overflow-y: auto;
    max-height: 360px;
  }
}
</style>
```

---

### 8.2 帖子发布与可见权限

#### 8.2.1 可见权限选择器

```vue
<!-- src/components/post/VisibilitySelector.vue -->
<template>
  <div class="visibility-selector">
    <label class="visibility-selector__label">可见范围</label>
    <el-select :model-value="modelValue" @update:model-value="handleChange" style="width: 180px">
      <el-option
        v-for="opt in options"
        :key="opt.value"
        :label="opt.label"
        :value="opt.value"
      >
        <div class="visibility-option">
          <el-icon><component :is="opt.icon" /></el-icon>
          <span>{{ opt.label }}</span>
        </div>
      </el-option>
    </el-select>
    <span class="visibility-selector__hint">{{ selectedHint }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Lock, View, User, Star } from '@element-plus/icons-vue'

const props = defineProps<{ modelValue: number }>()
const emit = defineEmits<{ 'update:modelValue': [value: number] }>()

const options = [
  { value: 0, label: '公开', icon: View, hint: '所有人可见' },
  { value: 1, label: '登录可见', icon: Lock, hint: '仅登录用户可见' },
  { value: 2, label: '粉丝可见', icon: User, hint: '仅粉丝可见' },
  { value: 3, label: '私密', icon: Star, hint: '仅自己可见' },
]

const selectedHint = computed(
  () => options.find(o => o.value === props.modelValue)?.hint || ''
)

function handleChange(val: number) {
  emit('update:modelValue', val)
}
</script>
```

#### 8.2.2 帖子列表可见权限处理

帖子列表由后端根据可见权限动态过滤，前端无需额外处理。但在帖子卡片上展示可见性图标提示：

```vue
<!-- PostCard.vue 片段 -->
<template>
  <el-card class="post-card">
    <template #header>
      <div class="post-card__header">
        <span class="post-card__title">{{ post.title }}</span>
        <el-tag v-if="post.type === 1" type="warning" size="small">精华</el-tag>
        <el-tag v-if="post.type === 2" type="danger" size="small">置顶</el-tag>
        <!-- 可见性图标 -->
        <el-tooltip :content="visibilityLabel" placement="top">
          <el-icon class="post-card__visibility-icon">
            <Lock v-if="post.visibility === 3" />
            <User v-else-if="post.visibility === 2" />
            <View v-else-if="post.visibility === 1" />
          </el-icon>
        </el-tooltip>
      </div>
    </template>
    <!-- ... -->
  </el-card>
</template>
```

---

### 8.3 草稿自动保存与恢复

#### 8.3.1 useDraft Composable

```typescript
// src/composables/useDraft.ts
import { ref, watch, onUnmounted } from 'vue'
import { draftApi } from '@/api'
import { ElMessage } from 'element-plus'

export function useDraft(postId?: string) {
  const AUTO_SAVE_INTERVAL = 30_000 // 30秒
  const title = ref('')
  const content = ref('')
  const categoryId = ref<string | null>(null)
  const visibility = ref<number>(0)
  const lastSavedAt = ref<string | null>(null)
  const isDirty = ref(false)

  let timer: ReturnType<typeof setInterval> | null = null

  /** 检查是否有已存在的草稿 */
  async function checkDraft() {
    try {
      const draft = await draftApi.check({ postId })
      if (draft) {
        return draft // 通知调用方：存在草稿，询问是否恢复
      }
    } catch {
      // 无草稿
    }
    return null
  }

  /** 恢复草稿 */
  async function restoreDraft(draftId: string) {
    const draft = await draftApi.getDetail(draftId)
    title.value = draft.title || ''
    content.value = draft.content || ''
    categoryId.value = draft.categoryId
    visibility.value = draft.visibility ?? 0
    isDirty.value = true
  }

  /** 保存草稿（Upsert） */
  async function saveDraft() {
    if (!isDirty.value) return
    try {
      await draftApi.save({
        postId,
        title: title.value,
        content: content.value,
        categoryId: categoryId.value ?? undefined,
        visibility: visibility.value,
      })
      lastSavedAt.value = new Date().toISOString()
      isDirty.value = false
    } catch (e) {
      console.error('草稿保存失败:', e)
    }
  }

  /** 放弃草稿 */
  async function discardDraft(draftId: string) {
    await draftApi.remove(draftId)
    isDirty.value = false
  }

  /** 开启自动保存 */
  function startAutoSave() {
    timer = setInterval(saveDraft, AUTO_SAVE_INTERVAL)
  }

  /** 停止自动保存 */
  function stopAutoSave() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  // 监听标题/内容变更，标记脏状态
  watch([title, content, categoryId, visibility], () => {
    isDirty.value = true
  }, { deep: true })

  // 组件卸载时停止定时器并保存一次
  onUnmounted(() => {
    stopAutoSave()
    if (isDirty.value) saveDraft()
  })

  return {
    title, content, categoryId, visibility,
    isDirty, lastSavedAt,
    checkDraft, restoreDraft, saveDraft, discardDraft,
    startAutoSave, stopAutoSave,
  }
}
```

#### 8.3.2 在 PostCreatePage 中使用

```vue
<!-- src/pages/post/PostCreatePage.vue -->
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useDraft } from '@/composables/useDraft'
import { postApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const { title, content, categoryId, visibility, checkDraft, restoreDraft, saveDraft, startAutoSave, stopAutoSave, isDirty } = useDraft()
const submitting = ref(false)

onMounted(async () => {
  // 检查是否存在草稿
  const draft = await checkDraft()
  if (draft) {
    await ElMessageBox.confirm(
      '检测到未发布的草稿，是否恢复？',
      '草稿恢复',
      { confirmButtonText: '恢复', cancelButtonText: '放弃', type: 'info' }
    )
    await restoreDraft(draft.id)
    ElMessage.success('草稿已恢复')
  }
  // 启动自动保存
  startAutoSave()
})

async function handleSubmit() {
  submitting.value = true
  try {
    const post = await postApi.create({
      title: title.value,
      content: content.value,
      categoryId: categoryId.value!,
      visibility: visibility.value,
    })
    ElMessage.success('发布成功')
    // 发布成功后，后端自动删除对应草稿
    router.push({ name: 'PostDetail', params: { id: post.id } })
  } catch {
    ElMessage.error('发布失败，草稿已保存')
  } finally {
    submitting.value = false
  }
}

// 离开页面前保存草稿（由 onUnmounted 处理）
</script>
```

---

### 8.4 AI 总结与问答面板

```vue
<!-- src/components/ai/AiSummaryPanel.vue -->
<template>
  <div class="ai-panel" v-if="isLoggedIn">
    <!-- 内容长度检查 -->
    <el-alert
      v-if="postContent.length < minLength"
      title="帖子内容过短，暂不支持 AI 总结"
      type="info" :closable="false"
    />

    <!-- 已有总结 -->
    <div v-else-if="summary" class="ai-panel__summary">
      <div class="ai-panel__header">
        <h4>AI 智能总结</h4>
        <el-button size="small" text @click="regenerate" :loading="generating">重新生成</el-button>
      </div>
      <div class="ai-panel__content markdown-body" v-html="renderedSummary" />
      <el-divider />
      <!-- 问答子面板 -->
      <AiQaPanel :post-id="postId" :has-summary="true" />
    </div>

    <!-- 未生成 -->
    <div v-else class="ai-panel__empty">
      <p>生成 AI 总结，快速了解帖子内容</p>
      <el-button type="primary" @click="generate" :loading="generating">
        {{ generating ? '生成中...' : '生成 AI 总结' }}
      </el-button>
      <p v-if="errorMsg" class="ai-panel__error">{{ errorMsg }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { aiApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { renderMarkdown } from '@/utils/markdown'
import { ElMessage } from 'element-plus'
import AiQaPanel from './AiQaPanel.vue'

const props = defineProps<{ postId: string; postContent: string }>()

const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)

const minLength = 50
const summary = ref<string | null>(null)
const generating = ref(false)
const errorMsg = ref('')

const renderedSummary = computed(() =>
  summary.value ? renderMarkdown(summary.value) : ''
)

onMounted(async () => {
  if (!isLoggedIn.value || props.postContent.length < minLength) return
  try {
    const res = await aiApi.getSummary(props.postId)
    if (res && res.status !== 2) {
      summary.value = res.content
    }
  } catch {
    // 无总结或获取失败
  }
})

async function generate() {
  generating.value = true
  errorMsg.value = ''
  try {
    const res = await aiApi.generateSummary(props.postId)
    summary.value = res.content
    ElMessage.success('AI 总结生成成功')
  } catch (e: any) {
    errorMsg.value = e.message || 'AI 服务暂时不可用，请稍后重试'
    ElMessage.error('生成失败')
  } finally {
    generating.value = false
  }
}

async function regenerate() {
  generating.value = true
  try {
    const res = await aiApi.generateSummary(props.postId)
    summary.value = res.content
    ElMessage.success('已重新生成')
  } catch (e: any) {
    errorMsg.value = e.message || '重新生成失败'
  } finally {
    generating.value = false
  }
}
</script>
```

---

### 8.5 神评机制前端

```vue
<!-- src/components/post/DivineCommentBadge.vue -->
<template>
  <div class="divine-comment" v-if="comment.isDivine">
    <el-tag type="warning" effect="dark" size="small">
      <el-icon><Medal /></el-icon> 神评
    </el-tag>
  </div>
</template>
```

**前端负责：**
- 神评专区展示（通过 `GET /api/v1/posts/{id}/divine-comments`）
- 推荐按钮（需注册 ≥7 天）：`POST /api/v1/comments/{id}/recommend`
- 神评徽章高亮（帖子详情页回复列表中标记 `isDivine` 字段）

> 双维度达标判定（点赞 ≥10 且推荐 ≥5）与动态撤销由后端定时任务处理，前端仅负责展示与推荐请求。

---

### 8.6 通知系统

#### 8.6.1 通知铃铛组件

```vue
<!-- src/components/notification/NotificationBell.vue -->
<template>
  <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
    <el-icon :size="20" class="notification-bell" @click="openNotifications">
      <Bell />
    </el-icon>
  </el-badge>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notification'
import { Bell } from '@element-plus/icons-vue'

const router = useRouter()
const notifStore = useNotificationStore()
const unreadCount = computed(() => notifStore.unreadCount)

function openNotifications() {
  router.push('/notifications')
}
</script>
```

#### 8.6.2 轮询未读数

```typescript
// 在 App.vue 或 DefaultLayout.vue 中启动轮询
import { onMounted, onUnmounted } from 'vue'
import { useNotificationStore } from '@/stores/notification'
import { useUserStore } from '@/stores/user'

const notifStore = useNotificationStore()
const userStore = useUserStore()
let pollTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  if (userStore.isLoggedIn) {
    notifStore.fetchUnreadCount()
    // 每 30 秒刷新一次未读通知数
    pollTimer = setInterval(() => notifStore.fetchUnreadCount(), 30_000)
  }
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
```

---

### 8.7 版块公告展示

```vue
<!-- src/components/notice/NoticeBanner.vue -->
<template>
  <div class="notice-banner" v-if="notices.length > 0">
    <!-- 置顶公告轮播 -->
    <el-carousel
      v-if="pinnedNotices.length > 0"
      :interval="5000" height="40px" indicator-position="none"
      arrow="never"
    >
      <el-carousel-item v-for="notice in pinnedNotices" :key="notice.id">
        <div class="notice-banner__item">
          <el-tag :type="notice.type === 0 ? 'info' : 'success'" size="small">
            {{ notice.type === 0 ? '须知' : '活动' }}
          </el-tag>
          <router-link :to="`/notices/${notice.id}`">
            {{ notice.title }}
          </router-link>
        </div>
      </el-carousel-item>
    </el-carousel>

    <!-- 公告列表（展开） -->
    <div class="notice-banner__list" v-if="showAll">
      <div
        v-for="notice in notices"
        :key="notice.id"
        class="notice-banner__row"
      >
        <el-tag :type="notice.type === 0 ? 'info' : 'success'" size="small">
          {{ notice.type === 0 ? '须知' : '活动' }}
        </el-tag>
        <router-link :to="`/notices/${notice.id}`">{{ notice.title }}</router-link>
        <span class="notice-banner__time">{{ formatDate(notice.createTime) }}</span>
      </div>
    </div>

    <el-button v-if="notices.length > 1" text size="small" @click="showAll = !showAll">
      {{ showAll ? '收起' : `查看全部 ${notices.length} 条公告` }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { noticeApi } from '@/api'
import type { Notice } from '@/api/types/notice'
import { formatDate } from '@/utils/format'

const props = defineProps<{ categoryId: string }>()

const notices = ref<Notice[]>([])
const showAll = ref(false)

const pinnedNotices = computed(() => notices.value.filter(n => n.isPinned))

onMounted(async () => {
  notices.value = await noticeApi.getList(props.categoryId)
})
</script>
```

---

### 8.8 管理后台

#### 8.8.1 管理布局

```vue
<!-- src/layouts/AdminLayout.vue -->
<template>
  <el-container class="admin-layout">
    <el-aside :width="sidebarCollapsed ? '64px' : '220px'">
      <el-menu
        :default-active="currentRoute"
        :collapse="sidebarCollapsed"
        router
        class="admin-menu"
      >
        <el-menu-item index="/admin">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/admin/users" v-if="isAdmin">
          <el-icon><UserFilled /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/posts" v-if="isModerator">
          <el-icon><Document /></el-icon>
          <span>帖子管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/categories" v-if="isAdmin">
          <el-icon><Grid /></el-icon>
          <span>版块管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/notices" v-if="isModerator">
          <el-icon><Bell /></el-icon>
          <span>公告管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/divine" v-if="isAdmin">
          <el-icon><Medal /></el-icon>
          <span>神评管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
```

#### 8.8.2 ECharts 数据统计图表

```vue
<!-- src/components/admin/StatChart.vue -->
<template>
  <div class="stat-chart">
    <VChart
      class="stat-chart__canvas"
      :option="chartOption"
      :autoresize="{ throttle: 100 }"
      :loading="loading"
    />
  </div>
</template>

<script setup lang="ts">
import { shallowRef, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, LegendComponent,
  GridComponent, DatasetComponent,
} from 'echarts/components'
import VChart from 'vue-echarts'
import { adminApi } from '@/api'

// 按需注册 ECharts 模块
use([
  CanvasRenderer, LineChart, BarChart, PieChart,
  TitleComponent, TooltipComponent, LegendComponent,
  GridComponent, DatasetComponent,
])

const props = defineProps<{
  type: 'userGrowth' | 'postVolume' | 'activity' | 'recommendEffect'
}>()

const loading = shallowRef(true)
const chartOption = shallowRef<any>({})

onMounted(async () => {
  try {
    const data = await adminApi.getStatistics(props.type)
    chartOption.value = buildOption(props.type, data)
  } finally {
    loading.value = false
  }
})

function buildOption(type: string, data: any) {
  switch (type) {
    case 'userGrowth':
      return {
        title: { text: '用户增长趋势', left: 'center' },
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: data.labels },
        yAxis: { type: 'value' },
        series: [{ type: 'line', data: data.values, smooth: true, areaStyle: {} }],
      }
    case 'postVolume':
      return {
        title: { text: '每日发帖量', left: 'center' },
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: data.labels },
        yAxis: { type: 'value' },
        series: [{ type: 'bar', data: data.values, itemStyle: { borderRadius: [4, 4, 0, 0] } }],
      }
    // ... 其他图表类型
    default:
      return {}
  }
}
</script>

<style scoped>
.stat-chart {
  width: 100%;
  height: 100%;
}
.stat-chart__canvas {
  height: 400px;
}
</style>
```

---

### 8.9 文件上传（头像 / 帖子内嵌图片）

#### 8.9.1 useFileUpload Composable

```typescript
// src/composables/useFileUpload.ts
import { ref } from 'vue'
import { fileApi } from '@/api'
import { ElMessage } from 'element-plus'

const MAX_SIZE_MB = 5                  // 最大 5MB
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']

export function useFileUpload() {
  const uploading = ref(false)
  const progress = ref(0)
  const uploadedUrl = ref<string | null>(null)

  /** 校验文件 */
  function validate(file: File): string | null {
    if (!ALLOWED_TYPES.includes(file.type)) {
      return '仅支持 JPG、PNG、GIF、WebP 格式的图片'
    }
    if (file.size > MAX_SIZE_MB * 1024 * 1024) {
      return `文件大小不能超过 ${MAX_SIZE_MB}MB`
    }
    return null
  }

  /** 上传文件 */
  async function upload(file: File): Promise<string | null> {
    const error = validate(file)
    if (error) {
      ElMessage.warning(error)
      return null
    }

    uploading.value = true
    progress.value = 0

    try {
      const result = await fileApi.upload(file, (pct) => {
        progress.value = pct
      })
      uploadedUrl.value = result.url
      ElMessage.success('上传成功')
      return result.url
    } catch {
      ElMessage.error('上传失败，请重试')
      return null
    } finally {
      uploading.value = false
    }
  }

  /** 重置状态 */
  function reset() {
    uploading.value = false
    progress.value = 0
    uploadedUrl.value = null
  }

  return { uploading, progress, uploadedUrl, upload, validate, reset }
}
```

#### 8.9.2 图片上传组件（ImageUpload）

```vue
<!-- src/components/common/ImageUpload.vue -->
<template>
  <div class="image-upload">
    <!-- 已上传预览 -->
    <div v-if="modelValue" class="image-upload__preview">
      <el-image
        :src="modelValue"
        :style="{ width: size + 'px', height: size + 'px' }"
        fit="cover"
        class="image-upload__img"
      />
      <div class="image-upload__mask">
        <el-button circle size="small" @click="handleRemove">
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 上传区域 -->
    <div
      v-else
      class="image-upload__trigger"
      :style="{ width: size + 'px', height: size + 'px' }"
      @click="triggerUpload"
    >
      <el-icon v-if="!uploading" :size="24"><Plus /></el-icon>
      <el-progress
        v-else
        type="circle"
        :percentage="progress"
        :width="size * 0.7"
        :stroke-width="4"
      />
    </div>

    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      style="display: none"
      @change="handleFileChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Delete, Plus } from '@element-plus/icons-vue'
import { useFileUpload } from '@/composables/useFileUpload'

const props = withDefaults(defineProps<{
  modelValue: string | null  // 当前图片 URL
  size?: number              // 上传区域尺寸
}>(), { size: 120 })

const emit = defineEmits<{ 'update:modelValue': [value: string | null] }>()

const { uploading, progress, upload } = useFileUpload()
const fileInput = ref<HTMLInputElement>()

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  const url = await upload(file)
  if (url) {
    emit('update:modelValue', url)
  }
  // 重置 input 以允许重复选择同一文件
  target.value = ''
}

function handleRemove() {
  emit('update:modelValue', null)
}
</script>

<style lang="scss" scoped>
.image-upload {
  &__preview {
    position: relative;
    border-radius: 4px;
    overflow: hidden;

    &:hover .image-upload__mask {
      opacity: 1;
    }
  }

  &__img {
    display: block;
  }

  &__mask {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.45);
    opacity: 0;
    transition: opacity 0.2s;
  }

  &__trigger {
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px dashed var(--el-border-color);
    border-radius: 4px;
    cursor: pointer;
    color: var(--el-text-color-secondary);
    transition: border-color 0.2s;

    &:hover {
      border-color: var(--el-color-primary);
      color: var(--el-color-primary);
    }
  }
}
</style>
```

#### 8.9.3 使用场景

| 场景 | 集成方式 | 说明 |
|------|---------|------|
| **用户头像** | SettingsPage 中使用 `<ImageUpload v-model="avatarUrl" :size="100" />` | 上传后通过 `PATCH /users/me` 保存 `avatarUrl` |
| **帖子图片** | MdEditor 工具栏中"插入图片"按钮触发上传，插入 `![alt](url)` | 上传后自动生成 Markdown 图片语法 |
| **公告配图** | NoticeManagePage 中可选上传封面图 | 公告内容为 Markdown，同理处理 |


---


---

### 8.10 权限指令

```typescript
// src/directives/permission.ts
import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * v-permission 指令
 * 用法：
 *   v-permission="'ADMIN'"           → 仅管理员可见
 *   v-permission="['ADMIN','MOD']"   → 管理员或版主可见
 *   v-permission:else                → 权限不满足时显示（需配合 v-if）
 */
export const permission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const userStore = useUserStore()
    const requiredRoles = Array.isArray(binding.value) ? binding.value : [binding.value]

    const hasPermission = requiredRoles.some(role => userStore.role === role)

    if (binding.arg === 'else') {
      // v-permission:else → 无权限时显示
      if (hasPermission) {
        el.style.display = 'none'
      }
    } else {
      // 默认 v-permission → 有权限时显示
      if (!hasPermission) {
        el.style.display = 'none'
      }
    }
  },
}
```

```typescript
// main.ts 中注册
import { permission } from '@/directives/permission'
app.directive('permission', permission)
```

```vue
<!-- 使用示例 -->
<el-button v-permission="'ADMIN'">管理员专属按钮</el-button>
<el-button v-permission="['ADMIN', 'MODERATOR']">管理操作</el-button>

<!-- 或使用 v-if 模式（更灵活） -->
<el-button v-if="userStore.isAdmin">删除用户</el-button>
```

## 9. 安全措施

| 安全项 | 措施 | 实现位置 |
|--------|------|----------|
| **XSS 防护** | markdown-it 设置 `html: false`，禁用原始 HTML；DOMPurify 清洗白名单标签 | `src/utils/markdown.ts` |
| **Token 安全** | JWT 存 localStorage，ofetch 拦截器自动附加 Authorization 头 | `src/api/index.ts` |
| **未认证拦截** | Vue Router 导航守卫 `beforeEach`，检查 `meta.requiresAuth` | `src/router/guards.ts` |
| **角色权限** | 导航守卫 + `v-permission` 指令 + 条件渲染 `v-if` | 路由 / 指令 / 组件 |
| **401 处理** | 响应拦截器捕获 401 → 清除 Token → 跳转登录页 | `src/api/index.ts` |
| **ID 精度** | 后端雪花 ID 序列化为字符串，前端统一使用 `string` 类型 | TypeScript 类型定义 |
| **AI 数据隔离** | AI 总结/问答均为私有，后端过滤，前端无特殊处理 | - |
| **防 Prompt 注入** | AI 输入预检（前端可做最小长度校验），后端 Prompt 固定边界 | 后端主导，前端校验 |
| **文件上传安全** | 前端校验文件类型（白名单 JPG/PNG/GIF/WebP）和大小（≤5MB）；后端二次校验 | `src/composables/useFileUpload.ts` |
| **HTTPS** | 生产环境强制 HTTPS | Nginx / 部署配置 |
| **环境变量** | API 地址等敏感配置通过 `.env` 管理，不硬编码 | `.env.production` |

---

## 10. 构建与部署

### 10.1 环境变量

```bash
# .env (开发环境)
VITE_API_BASE_URL=/api/v1
VITE_APP_TITLE=TechHub 开发环境

# .env.production (生产环境)
VITE_API_BASE_URL=https://api.techhub.example.com/api/v1
VITE_APP_TITLE=TechHub 技术社区
```

### 10.2 构建

```bash
# 类型检查
pnpm vue-tsc --noEmit

# 生产构建
pnpm build

# 预览构建结果
pnpm preview
```

### 10.3 Nginx 部署配置

```nginx
server {
    listen 80;
    server_name techhub.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name techhub.example.com;

    root /var/www/techhub-frontend/dist;
    index index.html;

    # SPA 路由：所有非静态资源请求回退到 index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理（或直接指向后端）
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 静态资源强缓存
    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## 11. 组件清单

### 11.1 页面组件（Pages）

| # | 文件 | 路由 | 说明 |
|---|------|------|------|
| 1 | `pages/home/HomePage.vue` | `/` | 首页推荐流 + 热门榜单 |
| 2 | `pages/category/CategoryPage.vue` | `/categories/:categoryId` | 版块帖子列表 + 公告 |
| 3 | `pages/post/PostCreatePage.vue` | `/posts/new` | 发布帖子（编辑器 + 可见权限 + 草稿） |
| 4 | `pages/post/PostEditPage.vue` | `/posts/:id/edit` | 编辑帖子 |
| 5 | `pages/post/PostDetailPage.vue` | `/posts/:id` | 帖子详情（正文 + 神评 + AI + 回复） |
| 6 | `pages/auth/LoginPage.vue` | `/login` | 登录 |
| 7 | `pages/auth/RegisterPage.vue` | `/register` | 注册 |
| 8 | `pages/user/ProfilePage.vue` | `/users/:id` | 用户主页（帖子/收藏/关注/粉丝） |
| 9 | `pages/user/SettingsPage.vue` | `/settings` | 个人信息修改、修改密码 |
| 10 | `pages/notification/NotificationPage.vue` | `/notifications` | 通知列表（已读/未读） |
| 11 | `pages/draft/DraftPage.vue` | `/drafts` | 草稿箱 |
| 12 | `pages/admin/DashboardPage.vue` | `/admin` | 管理仪表盘（ECharts） |
| 13 | `pages/admin/UserManagePage.vue` | `/admin/users` | 用户管理（封禁、角色分配） |
| 14 | `pages/admin/PostManagePage.vue` | `/admin/posts` | 帖子管理（精华/置顶/锁定/删除） |
| 15 | `pages/admin/CategoryManagePage.vue` | `/admin/categories` | 版块管理（CRUD） |
| 16 | `pages/admin/NoticeManagePage.vue` | `/admin/notices` | 公告管理 |
| 17 | `pages/admin/DivineManagePage.vue` | `/admin/divine` | 神评管理 |
| 18 | `pages/error/NotFoundPage.vue` | `/:pathMatch(.*)*` | 404 页面 |
| 19 | `pages/error/ForbiddenPage.vue` | `/403` | 403 无权限页面 |

### 11.2 公共组件（Components）

| # | 组件 | 说明 |
|---|------|------|
| 1 | `common/AppHeader.vue` | 全局导航栏（Logo + 版块 + 搜索 + 通知铃铛 + 用户菜单） |
| 2 | `common/AppFooter.vue` | 全局底部 |
| 3 | `common/AppSidebar.vue` | 管理后台侧边栏 |
| 4 | `common/UserAvatar.vue` | 用户头像（带默认图） |
| 5 | `common/ImageUpload.vue` | 图片上传（头像/帖子图片，拖拽+点击） |
| 6 | `common/EmptyState.vue` | 空状态占位 |
| 7 | `common/LoadingSkeleton.vue` | 骨架屏加载 |
| 7 | `markdown/MdEditor.vue` | Markdown 编辑器（左右分栏实时预览） |
| 8 | `markdown/MdViewer.vue` | Markdown 安全渲染器 |
| 9 | `markdown/Toolbar.vue` | 编辑器工具栏（加粗/斜体/代码/链接/图片上传等） |
| 10 | `post/PostCard.vue` | 帖子卡片（列表项） |
| 11 | `post/PostList.vue` | 帖子列表（集成分页） |
| 12 | `post/VisibilitySelector.vue` | 可见权限下拉选择器 |
| 13 | `post/DivineCommentBadge.vue` | 神评徽章 |
| 14 | `user/LoginForm.vue` | 登录表单 |
| 15 | `user/RegisterForm.vue` | 注册表单 |
| 16 | `notice/NoticeBanner.vue` | 版块公告横幅（轮播 + 列表） |
| 17 | `notice/NoticeCarousel.vue` | 公告轮播组件 |
| 18 | `ai/AiSummaryPanel.vue` | AI 总结面板（生成/刷新/展示） |
| 19 | `ai/AiQaPanel.vue` | AI 问答面板（提问/历史记录） |
| 20 | `notification/NotificationBell.vue` | 通知铃铛 + 未读数徽标 |
| 21 | `admin/StatChart.vue` | ECharts 统计图表 |
| 22 | `admin/UserTable.vue` | 用户管理表格 |
| 23 | `admin/PostTable.vue` | 帖子管理表格 |

---

## 附录 A：关键 API 对接矩阵

| 前端组件/页面 | 调用 API | Method | 说明 |
|---|---|---|---|
| LoginForm | `/auth/login` | POST | 登录，获取 JWT |
| RegisterForm | `/auth/register` | POST | 注册 |
| AppHeader (用户菜单) | `/users/me` | GET | 获取当前用户信息 |
| SettingsPage | `/users/me` | PATCH | 更新个人信息（含头像 URL） |
| SettingsPage | `/files/upload` | POST | 上传头像图片，返回 URL |
| PostCreatePage - 图片 | `/files/upload` | POST | 帖子内嵌图片上传，返回 Markdown 图片 URL |
| HomePage | `/recommendations` | GET | 个性化推荐流 |
| HomePage | `/posts` | GET | 热门榜单（兜底） |
| CategoryPage | `/categories/{id}/notices` | GET | 公告列表 |
| CategoryPage | `/posts` | GET | 帖子列表（版块筛选） |
| PostCreatePage | `/posts` | POST | 发布帖子 |
| PostCreatePage - 草稿 | `/drafts` | POST | 自动/手动保存草稿 |
| PostCreatePage - 草稿恢复 | `/drafts/check` | GET | 检测草稿存在 |
| PostDetailPage | `/posts/{id}` | GET | 帖子详情 |
| PostDetailPage | `/posts/{id}/comments` | GET | 回复列表 |
| PostDetailPage | `/posts/{id}/divine-comments` | GET | 神评列表 |
| PostDetailPage - 神评 | `/comments/{id}/recommend` | POST/DELETE | 推荐/取消神评 |
| PostDetailPage - AI | `/posts/{id}/ai/summary` | GET/POST | 获取/生成总结 |
| PostDetailPage - AI | `/posts/{id}/ai/qa` | POST | AI 提问 |
| PostDetailPage - AI | `/posts/{id}/ai/qa/history` | GET | 问答历史 |
| PostDetailPage - 相关 | `/posts/{id}/related` | GET | 相关帖子推荐 |
| NotificationPage | `/notifications` | GET | 通知列表 |
| NotificationBell | `/notifications/unread-count` | GET | 未读通知数（轮询） |
| DraftPage | `/drafts` | GET/DELETE | 草稿列表/删除 |
| AdminDashboard | `/admin/statistics` | GET | 站点统计 |
| AdminUserPage | `/admin/users` | GET | 用户列表 |
| AdminUserPage | `/admin/users/{id}/ban` | PATCH | 封禁/解封 |
| AdminUserPage | `/admin/users/{id}/role` | PATCH | 修改角色 |

---

> 本文档涵盖 TechHub 前端工程的全部设计与实现细节。开发时请严格按照目录结构组织代码，复用公共组件和 composables，确保类型安全与 XSS 防护。
