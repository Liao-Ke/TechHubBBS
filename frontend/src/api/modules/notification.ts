/**
 * Notification API module
 */
import { api } from '@/api'
import type { R, NotificationVO, PageResult } from '@/api/types'

export const notificationApi = {
  /** Get notification list (paginated) */
  getList: (params?: { page?: number; size?: number }) =>
    api<R<PageResult<NotificationVO>>>('/notifications', { query: params }),

  /** Mark a single notification as read */
  markRead: (id: string) =>
    api<R<null>>(`/notifications/${id}/read`, { method: 'POST' }),

  /** Mark all notifications as read */
  markAllRead: () =>
    api<R<null>>('/notifications/read-all', { method: 'POST' }),

  /** Get unread notification count */
  getUnreadCount: () =>
    api<R<{ count: number }>>('/notifications/unread-count'),
}
