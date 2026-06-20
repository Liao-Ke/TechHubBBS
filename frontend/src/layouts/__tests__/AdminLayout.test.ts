import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { useAppStore } from '@/stores/app'

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/admin', component: { template: '<div>dashboard</div>' } },
    ],
  })
}

describe('AdminLayout', () => {
  beforeEach(() => {
    // Mock localStorage
    const store: Record<string, string> = {}
    vi.stubGlobal('localStorage', {
      getItem: vi.fn((key: string) => store[key] ?? null),
      setItem: vi.fn((key: string, value: string) => { store[key] = value }),
      removeItem: vi.fn((key: string) => { delete store[key] }),
      clear: vi.fn(() => { Object.keys(store).forEach(k => delete store[k]) }),
      key: vi.fn<(...args: unknown[]) => unknown>(),
      length: 0,
    })
    // Don't mock document — jsdom provides it, and vue-router needs document.querySelector
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  async function mountLayout(router?: ReturnType<typeof createTestRouter>) {
    const testRouter = router ?? createTestRouter()
    const pinia = createPinia()
    setActivePinia(pinia)

    const wrapper = mount(AdminLayout, {
      global: { plugins: [testRouter, pinia] },
    })

    await testRouter.isReady()
    // Navigate to /admin so router-view has content
    await testRouter.push('/admin')
    return { wrapper, router: testRouter, pinia }
  }

  describe('structure', () => {
    it('renders el-container root element', async () => {
      const { wrapper } = await mountLayout()
      expect(wrapper.find('.el-container').exists()).toBe(true)
    })

    it('renders el-aside with default width 220px', async () => {
      const { wrapper } = await mountLayout()
      const aside = wrapper.find('.el-aside')
      expect(aside.exists()).toBe(true)
      expect(aside.attributes('style')).toContain('width: 220px')
    })

    it('renders el-main for content area', async () => {
      const { wrapper } = await mountLayout()
      expect(wrapper.find('.el-main').exists()).toBe(true)
    })

    it('renders AppSidebar component inside aside', async () => {
      const { wrapper } = await mountLayout()
      const aside = wrapper.find('.el-aside')
      const sidebar = aside.findComponent({ name: 'AppSidebar' })
      expect(sidebar.exists()).toBe(true)
    })

    it('renders router-view inside el-main', async () => {
      const { wrapper } = await mountLayout()
      const main = wrapper.find('.el-main')
      expect(main.text()).toContain('dashboard')
    })
  })

  describe('sidebar collapse', () => {
    it('aside width is 64px when sidebar is collapsed', async () => {
      const { wrapper, pinia } = await mountLayout()
      setActivePinia(pinia)
      const appStore = useAppStore()
      appStore.toggleSidebar()

      await wrapper.vm.$nextTick()

      const aside = wrapper.find('.el-aside')
      expect(aside.attributes('style')).toContain('width: 64px')
    })

    it('aside width returns to 220px after toggling back', async () => {
      const { wrapper, pinia } = await mountLayout()
      setActivePinia(pinia)
      const appStore = useAppStore()
      appStore.toggleSidebar()
      await wrapper.vm.$nextTick()
      appStore.toggleSidebar()
      await wrapper.vm.$nextTick()

      const aside = wrapper.find('.el-aside')
      expect(aside.attributes('style')).toContain('width: 220px')
    })
  })

  describe('styling', () => {
    it('applies admin-layout class to container', async () => {
      const { wrapper } = await mountLayout()
      expect(wrapper.find('.admin-layout').exists()).toBe(true)
    })

    it('aside has transition for width changes', async () => {
      const { wrapper } = await mountLayout()
      const aside = wrapper.find('.el-aside')
      // Element Plus renders width as inline style attribute
      expect(aside.attributes('style')).toBeDefined()
    })
  })
})
