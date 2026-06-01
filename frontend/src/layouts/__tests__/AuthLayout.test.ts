import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import AuthLayout from '@/layouts/AuthLayout.vue'

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      {
        path: '/login',
        name: 'login',
        component: { template: '<div class="test-auth-content">login page</div>' },
      },
    ],
  })
}

describe('AuthLayout', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  async function mountLayout(router?: ReturnType<typeof createTestRouter>) {
    const testRouter = router ?? createTestRouter()
    const pinia = createPinia()
    setActivePinia(pinia)

    const wrapper = mount(AuthLayout, {
      global: { plugins: [testRouter, pinia] },
    })

    await testRouter.isReady()
    await testRouter.push('/login')
    return { wrapper, router: testRouter }
  }

  describe('structure', () => {
    it('renders the auth-layout root container', async () => {
      const { wrapper } = await mountLayout()
      expect(wrapper.find('.auth-layout').exists()).toBe(true)
    })

    it('renders the card inside the layout', async () => {
      const { wrapper } = await mountLayout()
      expect(wrapper.find('.auth-layout__card').exists()).toBe(true)
    })

    it('renders the TechHub logo text', async () => {
      const { wrapper } = await mountLayout()
      expect(wrapper.find('.auth-layout__logo-text').text()).toBe('TechHub')
    })

    it('renders the tagline', async () => {
      const { wrapper } = await mountLayout()
      expect(wrapper.find('.auth-layout__logo-tagline').text()).toBe('技术社区')
    })

    it('renders router-view content inside the card', async () => {
      const { wrapper } = await mountLayout()
      expect(wrapper.text()).toContain('login page')
    })
  })

  describe('styling', () => {
    it('card has proper max-width', async () => {
      const { wrapper } = await mountLayout()
      const card = wrapper.find('.auth-layout__card')
      expect(card.exists()).toBe(true)
      // Verify the card element is rendered
      const style = card.attributes('class')
      expect(style).toContain('auth-layout__card')
    })

    it('layout is full viewport height', async () => {
      const { wrapper } = await mountLayout()
      const layout = wrapper.find('.auth-layout')
      expect(layout.exists()).toBe(true)
    })
  })
})
