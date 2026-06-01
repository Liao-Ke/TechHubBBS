import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'

// Mock user store
vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    register: vi.fn(),
    isLoggedIn: false,
  }),
}))

import RegisterPage from '@/pages/auth/RegisterPage.vue'

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/login', name: 'login', component: { template: '<div>login</div>' } },
      { path: '/register', name: 'register', component: RegisterPage },
    ],
  })
}

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  async function mountPage() {
    const router = createTestRouter()
    const pinia = createPinia()
    setActivePinia(pinia)

    await router.push('/register')
    await router.isReady()

    const wrapper = mount(RegisterPage, {
      global: { plugins: [router, pinia] },
    })

    return { wrapper, router }
  }

  describe('rendering', () => {
    it('renders RegisterForm component', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.findComponent({ name: 'RegisterForm' }).exists()).toBe(true)
    })

    it('is not a stub (has real content)', async () => {
      const { wrapper } = await mountPage()
      const text = wrapper.text()
      expect(text.length).toBeGreaterThan(5)
    })
  })

  describe('on register-success', () => {
    it('navigates to /login after successful registration', async () => {
      const { wrapper, router } = await mountPage()
      const registerForm = wrapper.findComponent({ name: 'RegisterForm' })

      await registerForm.vm.$emit('register-success')
      await nextTick()

      await vi.waitFor(() => {
        expect(router.currentRoute.value.path).toBe('/login')
      }, { timeout: 2000 })
    })
  })
})
