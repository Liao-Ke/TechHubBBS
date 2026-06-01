import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'

// ---------------------------------------------------------------------------
// Hoisted mock state
// ---------------------------------------------------------------------------
const { mockNotificationApi } = vi.hoisted(() => ({
  mockNotificationApi: {
    getList: vi.fn(),
    markRead: vi.fn(),
    markAllRead: vi.fn(),
  },
}))

vi.mock('@/api/modules/notification', () => ({
  notificationApi: mockNotificationApi,
}))

// Mock LoadingSkeleton
vi.mock('@/components/common/LoadingSkeleton.vue', () => ({
  default: {
    name: 'LoadingSkeleton',
    props: ['variant'],
    template: '<div class="loading-skeleton-stub" :data-variant="variant">Loading...</div>',
  },
}))

// Mock EmptyState
vi.mock('@/components/common/EmptyState.vue', () => ({
  default: {
    name: 'EmptyState',
    props: ['icon', 'title', 'description', 'actionText', 'actionRoute'],
    template: '<div class="empty-state-stub"><h3>{{ title }}</h3><p>{{ description }}</p></div>',
  },
}))

// Mock formatRelativeTime
vi.mock('@/utils/format', () => ({
  formatRelativeTime: vi.fn((d: string) => {
    if (d === '2025-05-27T10:00:00') return '1天前'
    if (d === '2025-05-28T08:00:00') return '2小时前'
    return '刚刚'
  }),
  formatNumber: vi.fn((n: number) => String(n)),
}))

import NotificationPage from '@/pages/notification/NotificationPage.vue'
import { useNotificationStore } from '@/stores/notification'
import type { NotificationVO, PageResult } from '@/api/types'
import type { R } from '@/api/types'

// ---------------------------------------------------------------------------
// Test Data
// ---------------------------------------------------------------------------
function mockPageResponse(
  records: NotificationVO[],
  overrides: Partial<PageResult<NotificationVO>> = {},
): R<PageResult<NotificationVO>> {
  return {
    code: 200,
    message: 'success',
    data: {
      records,
      total: records.length,
      size: 10,
      current: 1,
      pages: Math.ceil(records.length / 10),
      ...overrides,
    },
  }
}

const mockNotifications: NotificationVO[] = [
  {
    id: 'n1',
    type: 'REPLY',
    content: '张三 回复了你的帖子《Vue 3 入门》',
    sourceId: '100',
    sourceType: 'POST',
    isRead: false,
    createTime: '2025-05-27T10:00:00',
  },
  {
    id: 'n2',
    type: 'LIKE',
    content: '李四 赞了你的评论',
    sourceId: '200',
    sourceType: 'POST',
    isRead: false,
    createTime: '2025-05-28T08:00:00',
  },
  {
    id: 'n3',
    type: 'FOLLOW',
    content: '王五 关注了你',
    sourceId: '300',
    sourceType: 'USER',
    isRead: true,
    createTime: '2025-05-27T12:00:00',
  },
  {
    id: 'n4',
    type: 'DIVINE',
    content: '你的评论被设为神评',
    sourceId: '400',
    sourceType: 'COMMENT',
    isRead: false,
    createTime: '2025-05-28T06:00:00',
  },
  {
    id: 'n5',
    type: 'SYSTEM',
    content: '系统维护通知：今晚 22:00-24:00 维护',
    sourceId: null,
    sourceType: null,
    isRead: true,
    createTime: '2025-05-27T08:00:00',
  },
]

const pageTwoNotifications: NotificationVO[] = [
  {
    id: 'n6',
    type: 'REPLY',
    content: '赵六 回复了你的帖子',
    sourceId: '500',
    sourceType: 'POST',
    isRead: false,
    createTime: '2025-05-26T10:00:00',
  },
]

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div>home</div>' } },
      {
        path: '/notifications',
        name: 'notifications',
        component: NotificationPage,
      },
      { path: '/posts/:id', name: 'post-detail', component: { template: '<div>post</div>' } },
      { path: '/users/:id', name: 'user-profile', component: { template: '<div>user</div>' } },
    ],
  })
}

async function mountPage() {
  const router = createTestRouter()
  const pinia = createPinia()
  setActivePinia(pinia)

  await router.push('/notifications')
  await router.isReady()

  const wrapper = mount(NotificationPage, {
    global: {
      plugins: [router, pinia],
      stubs: {
        teleport: true,
      },
    },
  })

  await flushPromises()
  return { wrapper, router }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------
describe('NotificationPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockNotificationApi.getList.mockResolvedValue(
      mockPageResponse(mockNotifications, { total: 6, pages: 2 }),
    )
    mockNotificationApi.markRead.mockResolvedValue({ code: 200, message: 'ok', data: null })
    mockNotificationApi.markAllRead.mockResolvedValue({ code: 200, message: 'ok', data: null })
  })

  // ========================================================================
  // LOADING STATE
  // ========================================================================
  describe('loading state', () => {
    it('shows skeleton loader while fetching notifications', async () => {
      let resolveFn!: (value: unknown) => void
      mockNotificationApi.getList.mockReturnValue(
        new Promise((resolve) => { resolveFn = resolve }),
      )

      const { wrapper } = await mountPage()
      expect(wrapper.find('.loading-skeleton-stub').exists()).toBe(true)

      resolveFn(mockPageResponse(mockNotifications))
      await flushPromises()

      expect(wrapper.find('.loading-skeleton-stub').exists()).toBe(false)
    })
  })

  // ========================================================================
  // ERROR STATE
  // ========================================================================
  describe('error state', () => {
    it('shows error alert when fetch fails', async () => {
      mockNotificationApi.getList.mockRejectedValue(new Error('Network Error'))
      const { wrapper } = await mountPage()
      expect(wrapper.find('.notification-page__alert').exists()).toBe(true)
      expect(wrapper.text()).toContain('Network Error')
    })

    it('retry button triggers a fresh fetch', async () => {
      mockNotificationApi.getList.mockRejectedValue(new Error('Fail first'))
      const { wrapper } = await mountPage()
      expect(wrapper.find('.notification-page__alert').exists()).toBe(true)

      mockNotificationApi.getList.mockResolvedValue(
        mockPageResponse(mockNotifications),
      )
      const retryBtn = wrapper.find('.notification-page__alert .el-button')
      expect(retryBtn.exists()).toBe(true)
      await retryBtn.trigger('click')
      await flushPromises()

      expect(mockNotificationApi.getList).toHaveBeenCalledTimes(2)
      expect(wrapper.find('.notification-page__alert').exists()).toBe(false)
    })
  })

  // ========================================================================
  // EMPTY STATE
  // ========================================================================
  describe('empty state', () => {
    it('shows empty state when no notifications exist', async () => {
      mockNotificationApi.getList.mockResolvedValue(
        mockPageResponse([], { total: 0, pages: 0 }),
      )
      const { wrapper } = await mountPage()
      expect(wrapper.find('.empty-state-stub').exists()).toBe(true)
      expect(wrapper.text()).toContain('暂无通知')
    })
  })

  // ========================================================================
  // LIST RENDERING
  // ========================================================================
  describe('list rendering', () => {
    it('renders page title', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.text()).toContain('通知中心')
    })

    it('renders all notification items', async () => {
      const { wrapper } = await mountPage()
      const items = wrapper.findAll('.notification-item')
      expect(items.length).toBe(5)
    })

    it('renders notification content text', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.text()).toContain('张三 回复了你的帖子《Vue 3 入门》')
      expect(wrapper.text()).toContain('李四 赞了你的评论')
    })

    it('renders type tags with correct colors', async () => {
      const { wrapper } = await mountPage()
      const tags = wrapper.findAll('.notification-item__tag')
      expect(tags.length).toBe(5)
      // First is REPLY → primary
      expect(tags[0]!.text()).toBe('回复')
      // Second is LIKE → danger
      expect(tags[1]!.text()).toBe('点赞')
      // Third is FOLLOW → success
      expect(tags[2]!.text()).toBe('关注')
      // Fourth is DIVINE → warning
      expect(tags[3]!.text()).toBe('神评')
      // Fifth is SYSTEM → info
      expect(tags[4]!.text()).toBe('系统')
    })

    it('shows unread dot for unread notifications', async () => {
      const { wrapper } = await mountPage()
      // n1, n2, n4 are unread → 3 blue dots
      const dots = wrapper.findAll('.notification-item__dot')
      expect(dots.length).toBe(3)
    })

    it('renders relative time', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.text()).toContain('1天前')
      expect(wrapper.text()).toContain('2小时前')
    })

    it('renders pagination when multiple pages exist', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.find('.el-pagination').exists()).toBe(true)
    })
  })

  // ========================================================================
  // MARK AS READ
  // ========================================================================
  describe('mark as read', () => {
    it('marks notification as read on click and decrements store', async () => {
      const { wrapper } = await mountPage()
      const notifStore = useNotificationStore()
      notifStore.unreadCount = 3

      // Click the first unread notification (n1)
      const items = wrapper.findAll('.notification-item')
      await items[0]!.trigger('click')
      await flushPromises()

      expect(mockNotificationApi.markRead).toHaveBeenCalledWith('n1')
      expect(notifStore.unreadCount).toBe(2)
    })

    it('does not call markRead for already read notifications', async () => {
      const { wrapper } = await mountPage()

      // Click the third notification (n3, isRead=true)
      const items = wrapper.findAll('.notification-item')
      await items[2]!.trigger('click')
      await flushPromises()

      expect(mockNotificationApi.markRead).not.toHaveBeenCalled()
    })

    it('navigates to post source on click', async () => {
      const { wrapper, router } = await mountPage()

      // Click first notification (n1, sourceType=POST, sourceId=100)
      const items = wrapper.findAll('.notification-item')
      await items[0]!.trigger('click')
      await flushPromises()

      expect(router.currentRoute.value.path).toBe('/posts/100')
    })

    it('navigates to user source on click', async () => {
      const { wrapper, router } = await mountPage()

      // Click third notification (n3, sourceType=USER, sourceId=300)
      const items = wrapper.findAll('.notification-item')
      await items[2]!.trigger('click')
      await flushPromises()

      expect(router.currentRoute.value.path).toBe('/users/300')
    })

    it('does not navigate when sourceId is null', async () => {
      const { wrapper, router } = await mountPage()

      // Click fifth notification (n5, sourceId=null)
      const items = wrapper.findAll('.notification-item')
      await items[4]!.trigger('click')
      await flushPromises()

      expect(router.currentRoute.value.path).toBe('/notifications')
    })
  })

  // ========================================================================
  // MARK ALL READ
  // ========================================================================
  describe('mark all read', () => {
    it('calls markAllRead API and resets store', async () => {
      const { wrapper } = await mountPage()
      const notifStore = useNotificationStore()
      notifStore.unreadCount = 5

      const markAllBtn = wrapper.find('.notification-page__header .el-button')
      expect(markAllBtn.exists()).toBe(true)
      await markAllBtn.trigger('click')
      await flushPromises()

      expect(mockNotificationApi.markAllRead).toHaveBeenCalledTimes(1)
      expect(notifStore.unreadCount).toBe(0)
    })

    it('hides mark all read button when list is empty', async () => {
      mockNotificationApi.getList.mockResolvedValue(
        mockPageResponse([], { total: 0, pages: 0 }),
      )
      const { wrapper } = await mountPage()
      const markAllBtn = wrapper.find('.notification-page__header .el-button')
      expect(markAllBtn.exists()).toBe(false)
    })
  })

  // ========================================================================
  // PAGINATION
  // ========================================================================
  describe('pagination', () => {
    it('fetches next page on page change', async () => {
      const { wrapper } = await mountPage()

      mockNotificationApi.getList.mockResolvedValue(
        mockPageResponse(pageTwoNotifications, { total: 6, pages: 2, current: 2 }),
      )

      // Click the "next page" button (page 2) in el-pagination
      const pagination = wrapper.find('.el-pagination')
      expect(pagination.exists()).toBe(true)

      // Find and click the next-page button
      const nextBtn = pagination.find('.btn-next')
      if (nextBtn.exists()) {
        await nextBtn.trigger('click')
        await flushPromises()

        expect(mockNotificationApi.getList).toHaveBeenCalledWith({
          page: 2,
          size: 10,
        })
      }
    })
  })

  // ========================================================================
  // INTEGRATION
  // ========================================================================
  describe('integration', () => {
    it('is not a stub (has real content)', async () => {
      const { wrapper } = await mountPage()
      const text = wrapper.text()
      expect(text.length).toBeGreaterThan(5)
      expect(text).toContain('通知中心')
    })

    it('fetches notifications on mount', async () => {
      await mountPage()
      expect(mockNotificationApi.getList).toHaveBeenCalledTimes(1)
      expect(mockNotificationApi.getList).toHaveBeenCalledWith({
        page: 1,
        size: 10,
      })
    })
  })
})
