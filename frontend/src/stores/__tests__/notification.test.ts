import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Mock notification API module
vi.mock('@/api/modules/notification', () => ({
  notificationApi: {
    getUnreadCount: vi.fn(),
    getList: vi.fn(),
    markRead: vi.fn(),
    markAllRead: vi.fn(),
  },
}))

import { useNotificationStore } from '@/stores/notification'
import { notificationApi } from '@/api/modules/notification'
import type { R } from '@/api/types'

function mockUnreadResponse(count: number): R<{ count: number }> {
  return {
    code: 200,
    message: 'success',
    data: { count },
  }
}

describe('useNotificationStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('initial state', () => {
    it('has unreadCount of 0', () => {
      const store = useNotificationStore()
      expect(store.unreadCount).toBe(0)
    })

    it('has hasNew as false', () => {
      const store = useNotificationStore()
      expect(store.hasNew).toBe(false)
    })
  })

  describe('fetchUnreadCount', () => {
    it('updates unreadCount on success', async () => {
      vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(
        mockUnreadResponse(5),
      )

      const store = useNotificationStore()
      await store.fetchUnreadCount()

      expect(store.unreadCount).toBe(5)
      expect(store.hasNew).toBe(true)
    })

    it('sets hasNew to true when count > 0', async () => {
      vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(
        mockUnreadResponse(3),
      )

      const store = useNotificationStore()
      await store.fetchUnreadCount()

      expect(store.hasNew).toBe(true)
    })

    it('sets hasNew to false when count is 0', async () => {
      vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(
        mockUnreadResponse(0),
      )

      const store = useNotificationStore()
      await store.fetchUnreadCount()

      expect(store.unreadCount).toBe(0)
      expect(store.hasNew).toBe(false)
    })

    it('silently fails on API error', async () => {
      vi.mocked(notificationApi.getUnreadCount).mockRejectedValue(
        new Error('Network error'),
      )

      const store = useNotificationStore()
      store.unreadCount = 10

      await store.fetchUnreadCount()

      // Count should remain unchanged
      expect(store.unreadCount).toBe(10)
    })
  })

  describe('increment', () => {
    it('increments unreadCount by 1', () => {
      const store = useNotificationStore()
      store.unreadCount = 2

      store.increment()

      expect(store.unreadCount).toBe(3)
    })

    it('updates hasNew after incrementing', () => {
      const store = useNotificationStore()
      expect(store.hasNew).toBe(false)

      store.increment()

      expect(store.hasNew).toBe(true)
    })
  })

  describe('decrement', () => {
    it('decrements unreadCount by given amount', () => {
      const store = useNotificationStore()
      store.unreadCount = 10

      store.decrement(3)

      expect(store.unreadCount).toBe(7)
    })

    it('decrements by 1 when no argument given', () => {
      const store = useNotificationStore()
      store.unreadCount = 5

      store.decrement()

      expect(store.unreadCount).toBe(4)
    })

    it('does not go below 0', () => {
      const store = useNotificationStore()
      store.unreadCount = 2

      store.decrement(5)

      expect(store.unreadCount).toBe(0)
    })
  })

  describe('reset', () => {
    it('sets unreadCount to 0', () => {
      const store = useNotificationStore()
      store.unreadCount = 42

      store.reset()

      expect(store.unreadCount).toBe(0)
      expect(store.hasNew).toBe(false)
    })
  })
})
