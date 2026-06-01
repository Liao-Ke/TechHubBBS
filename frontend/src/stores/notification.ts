import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { notificationApi } from '@/api/modules/notification'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)

  const hasNew = computed(() => unreadCount.value > 0)

  async function fetchUnreadCount() {
    try {
      const res = await notificationApi.getUnreadCount()
      if (res.data) {
        unreadCount.value = res.data.count
      }
    } catch {
      // Silently fail — notification count is non-critical
    }
  }

  function increment() {
    unreadCount.value++
  }

  function decrement(n: number = 1) {
    unreadCount.value = Math.max(0, unreadCount.value - n)
  }

  function reset() {
    unreadCount.value = 0
  }

  return { unreadCount, hasNew, fetchUnreadCount, increment, decrement, reset }
})
