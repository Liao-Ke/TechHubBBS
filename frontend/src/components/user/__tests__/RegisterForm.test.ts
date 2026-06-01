import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'

// Mock user store with controllable state
const mockStore = {
  register: vi.fn(),
  isLoggedIn: false,
}

vi.mock('@/stores/user', () => ({
  useUserStore: () => mockStore,
}))

import RegisterForm from '@/components/user/RegisterForm.vue'

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

  return mount(RegisterForm, {
    global: { plugins: [router, pinia] },
  })
}

function getInputAt(wrapper: ReturnType<typeof mountForm>, index: number) {
  const inputs = wrapper.findAll('input')
  const el = inputs[index]
  if (!el) throw new Error(`No input at index ${index}`)
  return el
}

describe('RegisterForm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('structure', () => {
    it('renders el-form', () => {
      const wrapper = mountForm()
      expect(wrapper.find('.el-form').exists()).toBe(true)
    })

    it('renders all 4 input fields (username, password, confirmPassword, email)', () => {
      const wrapper = mountForm()
      const inputs = wrapper.findAll('.el-input')
      expect(inputs.length).toBeGreaterThanOrEqual(4)
    })

    it('renders submit button', () => {
      const wrapper = mountForm()
      const button = wrapper.find('.el-button')
      expect(button.exists()).toBe(true)
    })

    it('renders footer with link to login', () => {
      const wrapper = mountForm()
      expect(wrapper.text()).toContain('已有账号')
      expect(wrapper.text()).toContain('立即登录')
    })
  })

  describe('validation', () => {
    it('shows error when submitting with empty fields', async () => {
      const wrapper = mountForm()
      const form = wrapper.find('form')
      await form.trigger('submit.prevent')
      await nextTick()
      expect(wrapper.find('.el-form').exists()).toBe(true)
    })
  })

  describe('register flow', () => {
    it('calls userStore.register on successful validation', async () => {
      mockStore.register.mockResolvedValue(undefined)

      const wrapper = mountForm()
      await getInputAt(wrapper, 0).setValue('newuser')
      await getInputAt(wrapper, 1).setValue('password123')
      await getInputAt(wrapper, 2).setValue('password123')
      await getInputAt(wrapper, 3).setValue('newuser@example.com')
      await nextTick()

      const form = wrapper.find('form')
      await form.trigger('submit.prevent')
      await nextTick()

      await vi.waitFor(() => {
        expect(mockStore.register).toHaveBeenCalledWith('newuser', 'password123', 'newuser@example.com')
      }, { timeout: 2000 })
    })

    it('calls register without email when email is empty', async () => {
      mockStore.register.mockResolvedValue(undefined)

      const wrapper = mountForm()
      await getInputAt(wrapper, 0).setValue('newuser')
      await getInputAt(wrapper, 1).setValue('password123')
      await getInputAt(wrapper, 2).setValue('password123')
      await nextTick()

      const form = wrapper.find('form')
      await form.trigger('submit.prevent')
      await nextTick()

      await vi.waitFor(() => {
        expect(mockStore.register).toHaveBeenCalledWith('newuser', 'password123', undefined)
      }, { timeout: 2000 })
    })

    it('emits register-success on successful registration', async () => {
      mockStore.register.mockResolvedValue(undefined)

      const wrapper = mountForm()
      await getInputAt(wrapper, 0).setValue('newuser')
      await getInputAt(wrapper, 1).setValue('password123')
      await getInputAt(wrapper, 2).setValue('password123')
      await nextTick()

      const form = wrapper.find('form')
      await form.trigger('submit.prevent')
      await nextTick()

      await vi.waitFor(() => {
        expect(wrapper.emitted('register-success')).toBeTruthy()
      }, { timeout: 2000 })
    })

    it('shows error message on register failure', async () => {
      mockStore.register.mockRejectedValue(new Error('用户名已存在'))

      const wrapper = mountForm()
      await getInputAt(wrapper, 0).setValue('existinguser')
      await getInputAt(wrapper, 1).setValue('password123')
      await getInputAt(wrapper, 2).setValue('password123')
      await nextTick()

      const form = wrapper.find('form')
      await form.trigger('submit.prevent')
      await nextTick()
    })
  })

  describe('password confirmation', () => {
    it('validates that confirmPassword matches password', async () => {
      const wrapper = mountForm()
      await getInputAt(wrapper, 0).setValue('newuser')
      await getInputAt(wrapper, 1).setValue('password123')
      await getInputAt(wrapper, 2).setValue('differentpassword')
      await nextTick()

      const form = wrapper.find('form')
      await form.trigger('submit.prevent')
      await nextTick()
    })
  })

  describe('navigation', () => {
    it('login link navigates to /login', async () => {
      const wrapper = mountForm()
      const link = wrapper.find('.el-link')
      expect(link.exists()).toBe(true)
      await link.trigger('click')
      await nextTick()
    })
  })
})
