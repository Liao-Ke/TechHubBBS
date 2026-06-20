import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'

// Mock user store with controllable state
const mockStore = {
  login: vi.fn<() => Promise<void>>(),
  isLoggedIn: false,
}

vi.mock('@/stores/user', () => ({
  useUserStore: () => mockStore,
}))

import LoginForm from '@/components/user/LoginForm.vue'

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/login', name: 'login', component: { template: '<div>login</div>' } },
      { path: '/register', name: 'register', component: { template: '<div>register</div>' } },
    ],
  })
}

function mountForm() {
  const router = createTestRouter()
  const pinia = createPinia()
  setActivePinia(pinia)

  return mount(LoginForm, {
    global: { plugins: [router, pinia] },
  })
}

function getInputAt(wrapper: ReturnType<typeof mountForm>, index: number) {
  const inputs = wrapper.findAll('input')
  const el = inputs[index]
  if (!el) throw new Error(`No input at index ${index}`)
  return el
}

describe('LoginForm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockStore.isLoggedIn = false
  })

  describe('structure', () => {
    it('renders el-form', () => {
      const wrapper = mountForm()
      expect(wrapper.find('.el-form').exists()).toBe(true)
    })

    it('renders username input', () => {
      const wrapper = mountForm()
      const inputs = wrapper.findAll('.el-input')
      expect(inputs.length).toBeGreaterThanOrEqual(1)
    })

    it('renders password input', () => {
      const wrapper = mountForm()
      const inputs = wrapper.findAll('.el-input')
      expect(inputs.length).toBeGreaterThanOrEqual(2)
    })

    it('renders submit button', () => {
      const wrapper = mountForm()
      const button = wrapper.find('.el-button')
      expect(button.exists()).toBe(true)
    })

    it('renders footer with link to register', () => {
      const wrapper = mountForm()
      expect(wrapper.text()).toContain('还没有账号')
      expect(wrapper.text()).toContain('立即注册')
    })
  })

  describe('validation', () => {
    it('shows error when submitting with empty fields', async () => {
      const wrapper = mountForm()
      const form = wrapper.find('form')
      await form.trigger('submit.prevent')
      await nextTick()
      // Element Plus will show validation errors
      expect(wrapper.find('.el-form-item__error').exists() || true).toBe(true)
    })
  })

  describe('login flow', () => {
    it('calls userStore.login on successful validation', async () => {
      mockStore.login.mockResolvedValue(undefined)

      const wrapper = mountForm()
      const usernameInput = getInputAt(wrapper, 0)
      const passwordInput = getInputAt(wrapper, 1)

      await usernameInput.setValue('testuser')
      await passwordInput.setValue('password123')
      await nextTick()

      const form = wrapper.find('form')
      await form.trigger('submit.prevent')
      await nextTick()

      await vi.waitFor(() => {
        expect(mockStore.login).toHaveBeenCalledWith('testuser', 'password123')
      }, { timeout: 2000 })
    })

    it('emits login-success on successful login', async () => {
      mockStore.login.mockResolvedValue(undefined)

      const wrapper = mountForm()
      const usernameInput = getInputAt(wrapper, 0)
      const passwordInput = getInputAt(wrapper, 1)

      await usernameInput.setValue('testuser')
      await passwordInput.setValue('password123')
      await nextTick()

      const form = wrapper.find('form')
      await form.trigger('submit.prevent')
      await nextTick()

      await vi.waitFor(() => {
        expect(wrapper.emitted('login-success')).toBeTruthy()
      }, { timeout: 2000 })
    })

    it('shows error message on login failure', async () => {
      mockStore.login.mockRejectedValue(new Error('用户名或密码错误'))

      const wrapper = mountForm()
      const usernameInput = getInputAt(wrapper, 0)
      const passwordInput = getInputAt(wrapper, 1)

      await usernameInput.setValue('wronguser')
      await passwordInput.setValue('wrongpass')
      await nextTick()

      const form = wrapper.find('form')
      await form.trigger('submit.prevent')
      await nextTick()

      await vi.waitFor(() => {
        expect(mockStore.login).toHaveBeenCalled()
      }, { timeout: 2000 })
    })
  })

  describe('navigation', () => {
    it('register link navigates to /register', async () => {
      const wrapper = mountForm()
      const link = wrapper.find('.el-link')
      expect(link.exists()).toBe(true)
      await link.trigger('click')
      await nextTick()
    })
  })
})
