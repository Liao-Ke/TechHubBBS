import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia } from 'pinia'
import App from '../App.vue'

/**
 * Helper: create a test router with a catch-all route so RouterView renders content.
 */
function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/:pathMatch(.*)*', name: 'fallback', component: { template: '<div>page-content</div>' } },
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

    const transition = wrapper.findComponent({ name: 'Transition' })
    expect(transition.exists()).toBe(true)
    expect(transition.attributes('name')).toBe('page-fade')
    expect(transition.attributes('mode')).toBe('out-in')
  })
})
