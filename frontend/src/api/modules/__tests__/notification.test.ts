import { describe, it, expect, vi, beforeEach } from 'vitest'
import { api } from '@/api'
import { notificationApi } from '@/api/modules/notification'

vi.mock('@/api', () => ({
  api: vi.fn<(...args: unknown[]) => unknown>(),
}))

describe('notificationApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getList calls GET /notifications with query params', () => {
    notificationApi.getList({ page: 1, size: 20 })
    expect(api).toHaveBeenCalledWith('/notifications', {
      query: { page: 1, size: 20 },
    })
  })

  it('getList works without params', () => {
    notificationApi.getList()
    expect(api).toHaveBeenCalledWith('/notifications', {
      query: undefined,
    })
  })

  it('markRead calls POST /notifications/{id}/read', () => {
    notificationApi.markRead('notif-1')
    expect(api).toHaveBeenCalledWith('/notifications/notif-1/read', {
      method: 'PATCH',
    })
  })

  it('markAllRead calls POST /notifications/read-all', () => {
    notificationApi.markAllRead()
    expect(api).toHaveBeenCalledWith('/notifications/read-all', {
      method: 'PATCH',
    })
  })

  it('getUnreadCount calls GET /notifications/unread-count', () => {
    notificationApi.getUnreadCount()
    expect(api).toHaveBeenCalledWith('/notifications/unread-count')
  })
})
