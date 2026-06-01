/**
 * Notification type enumeration
 */
export type NotificationType = 'REPLY' | 'LIKE' | 'FOLLOW' | 'DIVINE' | 'SYSTEM'

/**
 * Notification view object
 */
export interface NotificationVO {
  id: string
  type: NotificationType
  content: string
  sourceId: string | null
  sourceType: string | null
  isRead: boolean
  createTime: string
}
