import { describe, it, expect, vi, beforeAll } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia } from 'pinia'

beforeAll(() => {
  const store: Record<string, string> = {}
  Object.defineProperty(globalThis, 'localStorage', {
    value: {
      getItem: (key: string) => store[key] ?? null,
      setItem: (key: string, value: string) => { store[key] = value },
      removeItem: (key: string) => { delete store[key] },
      clear: () => { Object.keys(store).forEach((k) => delete store[k]) },
    },
    writable: true,
  })
})

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    isLoggedIn: false,
    userInfo: null,
    token: null,
    role: 'USER',
    fetchUserInfo: vi.fn(),
    logout: vi.fn(),
  }),
}))

vi.mock('@/stores/notification', () => ({
  useNotificationStore: () => ({
    fetchUnreadCount: vi.fn(),
  }),
}))

vi.mock('@/layouts/DefaultLayout.vue', () => ({
  default: {
    name: 'DefaultLayout',
    template: '<div class="default-layout"><slot /></div>',
  },
}))

import App from '../App.vue'

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/:pathMatch(.*)*', name: 'fallback', meta: { layout: 'default' }, component: { template: '<div class="page-content">page-content</div>' } },
    ],
  })
}

describe('App.vue', () => {
  it('mounts without errors', async () => {
    const router = createTestRouter()
    const pinia = createPinia()

    const wrapper = mount(App, {
      global: { plugins: [router, pinia] },
    })

    await router.isReady()
    expect(wrapper.exists()).toBe(true)
  })

  it('renders RouterView component', async () => {
    const router = createTestRouter()
    const pinia = createPinia()

    const wrapper = mount(App, {
      global: { plugins: [router, pinia] },
    })

    await router.isReady()
    expect(wrapper.findComponent({ name: 'RouterView' }).exists()).toBe(true)
  })

  it('renders the active route content through RouterView', async () => {
    const router = createTestRouter()
    const pinia = createPinia()

    const wrapper = mount(App, {
      global: { plugins: [router, pinia] },
    })

    await router.isReady()
    expect(wrapper.text()).toContain('page-content')
  })

  it('configures page-fade transition with out-in mode', async () => {
    const router = createTestRouter()
    const pinia = createPinia()

    const wrapper = mount(App, {
      global: { plugins: [router, pinia] },
    })

    await router.isReady()

    const transitions = wrapper.findAllComponents({ name: 'Transition' })
    const pageFade = transitions.find((t) => t.attributes('name') === 'page-fade')
    expect(pageFade).toBeDefined()
    expect(pageFade!.attributes('mode')).toBe('out-in')
  })
})
