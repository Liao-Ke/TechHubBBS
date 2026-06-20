import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'

// ---------------------------------------------------------------------------
// Hoisted mock state
// ---------------------------------------------------------------------------
const { mockNotificationApi } = vi.hoisted(() => ({
  mockNotificationApi: {
    getUnreadCount: vi.fn<(...args: unknown[]) => unknown>(),
  },
}))

vi.mock('@/api/modules/notification', () => ({
  notificationApi: mockNotificationApi,
}))

vi.mock('@/utils/token', () => ({
  getToken: vi.fn(() => null),
  setToken: vi.fn<(...args: unknown[]) => unknown>(),
  removeToken: vi.fn<(...args: unknown[]) => unknown>(),
}))

import NotificationBell from '@/components/notification/NotificationBell.vue'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div>home</div>' } },
      { path: '/notifications', name: 'notifications', component: { template: '<div>notifications</div>' } },
    ],
  })
}

async function mountBell() {
  const router = createTestRouter()
  const pinia = createPinia()
  setActivePinia(pinia)

  await router.push('/')
  await router.isReady()

  const wrapper = mount(NotificationBell, {
    global: {
      plugins: [router, pinia],
    },
  })

  return { wrapper, router, pinia }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------
describe('NotificationBell', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('visibility', () => {
    it('does not render when user is not logged in', async () => {
      const { wrapper } = await mountBell()
      expect(wrapper.find('.notification-bell').exists()).toBe(false)
    })

    it('renders when user is logged in', async () => {
      const { wrapper } = await mountBell()
      const userStore = useUserStore()
      userStore.token = 'test-token'
      await wrapper.vm.$nextTick()
      expect(wrapper.find('.notification-bell').exists()).toBe(true)
    })
  })

  describe('badge', () => {
    it('shows unread count on badge', async () => {
      const { wrapper } = await mountBell()
      const userStore = useUserStore()
      const notifStore = useNotificationStore()
      userStore.token = 'test-token'
      notifStore.unreadCount = 5
      await wrapper.vm.$nextTick()

      // The el-badge renders the value as a sup element
      const badge = wrapper.find('.el-badge')
      expect(badge.exists()).toBe(true)
      expect(badge.find('.el-badge__content').text()).toBe('5')
    })

    it('hides badge when unreadCount is 0', async () => {
      const { wrapper } = await mountBell()
      const userStore = useUserStore()
      const notifStore = useNotificationStore()
      userStore.token = 'test-token'
      notifStore.unreadCount = 0
      await wrapper.vm.$nextTick()

      const badge = wrapper.find('.el-badge')
      expect(badge.exists()).toBe(true)
      // When hidden, el-badge does not render el-badge__content
      expect(badge.find('.el-badge__content').exists()).toBe(false)
    })
  })

  describe('navigation', () => {
    it('navigates to /notifications on icon click', async () => {
      const { wrapper, router } = await mountBell()
      const userStore = useUserStore()
      userStore.token = 'test-token'
      await wrapper.vm.$nextTick()

      const icon = wrapper.find('.notification-bell__icon')
      expect(icon.exists()).toBe(true)
      await icon.trigger('click')
      await flushPromises()
      await router.isReady()

      expect(router.currentRoute.value.path).toBe('/notifications')
    })
  })

  describe('aria', () => {
    it('has aria-label for accessibility', async () => {
      const { wrapper } = await mountBell()
      const userStore = useUserStore()
      userStore.token = 'test-token'
      await wrapper.vm.$nextTick()

      expect(wrapper.find('[aria-label="通知"]').exists()).toBe(true)
    })
  })
})
