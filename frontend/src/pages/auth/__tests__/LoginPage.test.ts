import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'

// Mock user store with controllable state
const mockStore = {
  login: vi.fn(),
  isLoggedIn: false,
}

vi.mock('@/stores/user', () => ({
  useUserStore: () => mockStore,
}))

import LoginPage from '@/pages/auth/LoginPage.vue'

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/login', name: 'login', component: LoginPage },
      { path: '/', name: 'home', component: { template: '<div>home</div>' } },
      { path: '/register', name: 'register', component: { template: '<div>register</div>' } },
    ],
  })
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockStore.isLoggedIn = false
  })

  async function mountPage(initialRoute = '/login') {
    const router = createTestRouter()
    const pinia = createPinia()
    setActivePinia(pinia)

    await router.push(initialRoute)
    await router.isReady()

    const wrapper = mount(LoginPage, {
      global: { plugins: [router, pinia] },
    })

    return { wrapper, router }
  }

  describe('rendering', () => {
    it('renders LoginForm component', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.findComponent({ name: 'LoginForm' }).exists()).toBe(true)
    })

    it('is not a stub (has real content)', async () => {
      const { wrapper } = await mountPage()
      const text = wrapper.text()
      expect(text.length).toBeGreaterThan(5)
    })
  })

  describe('on login-success', () => {
    it('redirects to / by default', async () => {
      const { wrapper, router } = await mountPage()
      const loginForm = wrapper.findComponent({ name: 'LoginForm' })

      await loginForm.vm.$emit('login-success')
      await nextTick()
      await vi.waitFor(() => {
        expect(router.currentRoute.value.path).toBe('/')
      }, { timeout: 2000 })
    })

    it('redirects to query.redirect when present', async () => {
      const { wrapper, router } = await mountPage('/login?redirect=/posts/new')
      const loginForm = wrapper.findComponent({ name: 'LoginForm' })

      await loginForm.vm.$emit('login-success')
      await nextTick()
      await vi.waitFor(() => {
        expect(router.currentRoute.value.path).toBe('/posts/new')
      }, { timeout: 2000 })
    })
  })

  describe('redirect when already logged in', () => {
    it('redirects to / when isLoggedIn is true on mount', async () => {
      mockStore.isLoggedIn = true

      const router = createTestRouter()
      const pinia = createPinia()
      setActivePinia(pinia)
      await router.push('/login')
      await router.isReady()

      mount(LoginPage, {
        global: { plugins: [router, pinia] },
      })

      await nextTick()
      await vi.waitFor(() => {
        expect(router.currentRoute.value.path).toBe('/')
      }, { timeout: 2000 })
    })
  })
})
