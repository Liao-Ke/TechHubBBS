import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { setupRouterGuards } from '../guards'

// Mock useUserStore
const mockStore = {
  token: null as string | null,
  role: 'USER' as string,
  isLoggedIn: false,
  userInfo: null as { id: string; username: string; role: string } | null,
  fetchUserInfo: vi.fn<(...args: unknown[]) => unknown>(),
}

vi.mock('@/stores/user', () => ({
  useUserStore: () => mockStore,
}))

// Build a minimal test router with representative routes
const testRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'home',
    component: { template: '<div>Home</div>' },
    meta: { layout: 'default' },
  },
  {
    path: '/login',
    name: 'login',
    component: { template: '<div>Login</div>' },
    meta: { title: '登录', layout: 'auth' },
  },
  {
    path: '/register',
    name: 'register',
    component: { template: '<div>Register</div>' },
    meta: { title: '注册', layout: 'auth' },
  },
  {
    path: '/posts/new',
    name: 'post-create',
    component: { template: '<div>New Post</div>' },
    meta: { requiresAuth: true, layout: 'default' },
  },
  {
    path: '/admin',
    name: 'admin',
    component: { template: '<div>Admin</div>' },
    meta: { requiresAuth: true, roles: ['ADMIN', 'MODERATOR'] },
  },
  {
    path: '/403',
    name: 'forbidden',
    component: { template: '<div>Forbidden</div>' },
    meta: { layout: 'default' },
  },
]

function createTestRouter() {
  const router = createRouter({
    history: createWebHistory(),
    routes: testRoutes,
  })
  setupRouterGuards(router)
  return router
}

describe('Navigation guards', () => {
  beforeEach(() => {
    // Reset to unauthenticated user
    mockStore.token = null
    mockStore.role = 'USER'
    mockStore.isLoggedIn = false
    mockStore.userInfo = null
    mockStore.fetchUserInfo = vi.fn<(...args: unknown[]) => unknown>().mockResolvedValue(undefined)
  })

  describe('unauthenticated users', () => {
    it('allows access to public routes', async () => {
      const router = createTestRouter()
      await router.push('/')
      await router.isReady()
      expect(router.currentRoute.value.name).toBe('home')
    })

    it('redirects from requiresAuth route to /login?redirect=', async () => {
      const router = createTestRouter()
      await router.push('/posts/new')
      await router.isReady()
      expect(router.currentRoute.value.name).toBe('login')
      expect(router.currentRoute.value.query.redirect).toBe('/posts/new')
    })

    it('allows access to /login', async () => {
      const router = createTestRouter()
      await router.push('/login')
      await router.isReady()
      expect(router.currentRoute.value.name).toBe('login')
    })

    it('allows access to /register', async () => {
      const router = createTestRouter()
      await router.push('/register')
      await router.isReady()
      expect(router.currentRoute.value.name).toBe('register')
    })
  })

  describe('authenticated users', () => {
    beforeEach(() => {
      mockStore.token = 'valid-token'
      mockStore.isLoggedIn = true
      mockStore.userInfo = { id: '1', username: 'test', role: 'USER' }
    })

    it('redirects from /login to /', async () => {
      const router = createTestRouter()
      await router.push('/login')
      await router.isReady()
      expect(router.currentRoute.value.name).toBe('home')
    })

    it('redirects from /register to /', async () => {
      const router = createTestRouter()
      await router.push('/register')
      await router.isReady()
      expect(router.currentRoute.value.name).toBe('home')
    })

    it('allows access to requiresAuth routes', async () => {
      const router = createTestRouter()
      await router.push('/posts/new')
      await router.isReady()
      expect(router.currentRoute.value.name).toBe('post-create')
    })
  })

  describe('role-based access', () => {
    it('USER role is redirected from /admin to /403', async () => {
      mockStore.token = 'valid-token'
      mockStore.isLoggedIn = true
      mockStore.role = 'USER'
      mockStore.userInfo = { id: '1', username: 'test', role: 'USER' }

      const router = createTestRouter()
      await router.push('/admin')
      await router.isReady()
      expect(router.currentRoute.value.name).toBe('forbidden')
    })

    it('MODERATOR role can access /admin', async () => {
      mockStore.token = 'valid-token'
      mockStore.isLoggedIn = true
      mockStore.role = 'MODERATOR'
      mockStore.userInfo = { id: '2', username: 'mod', role: 'MODERATOR' }

      const router = createTestRouter()
      await router.push('/admin')
      await router.isReady()
      expect(router.currentRoute.value.name).toBe('admin')
    })

    it('ADMIN role can access /admin', async () => {
      mockStore.token = 'valid-token'
      mockStore.isLoggedIn = true
      mockStore.role = 'ADMIN'
      mockStore.userInfo = { id: '3', username: 'admin', role: 'ADMIN' }

      const router = createTestRouter()
      await router.push('/admin')
      await router.isReady()
      expect(router.currentRoute.value.name).toBe('admin')
    })

    it('unauthenticated user accessing /admin is redirected to /login first', async () => {
      mockStore.token = null
      mockStore.isLoggedIn = false

      const router = createTestRouter()
      await router.push('/admin')
      await router.isReady()
      // requiresAuth check fires before roles check
      expect(router.currentRoute.value.name).toBe('login')
      expect(router.currentRoute.value.query.redirect).toBe('/admin')
    })
  })

  describe('document title', () => {
    it('sets document title from route meta', async () => {
      const router = createTestRouter()
      await router.push('/login')
      await router.isReady()
      expect(document.title).toBe('登录 - TechHub')
    })

    it('falls back to TechHub when no title meta', async () => {
      // Reset title first since previous test may have set it
      document.title = 'TechHub'
      const router = createTestRouter()
      await router.push('/')
      await router.isReady()
      expect(document.title).toBe('TechHub')
    })
  })
})
