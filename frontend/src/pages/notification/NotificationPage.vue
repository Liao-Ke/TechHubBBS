<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { notificationApi } from '@/api/modules/notification'
import { useNotificationStore } from '@/stores/notification'
import { formatRelativeTime } from '@/utils/format'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { NotificationVO, NotificationType } from '@/api/types'
import {
  ChatDotRound,
  StarFilled,
  UserFilled,
  Medal,
  InfoFilled,
  Bell,
} from '@element-plus/icons-vue'
import type { Component } from 'vue'

const router = useRouter()
const store = useNotificationStore()

const notifications = ref<NotificationVO[]>([])
const loading = ref(true)
const error = ref(false)
const errorMessage = ref('')
const currentPage = ref(1)
const totalPages = ref(0)
const total = ref(0)
const pageSize = 10

const typeConfig: Record<NotificationType, { icon: Component; color: string; label: string }> = {
  REPLY:   { icon: ChatDotRound, color: 'var(--el-color-primary)',  label: '回复'   },
  LIKE:    { icon: StarFilled,   color: 'var(--el-color-danger)',   label: '点赞'   },
  FOLLOW:  { icon: UserFilled,   color: 'var(--el-color-success)',  label: '关注'   },
  DIVINE:  { icon: Medal,        color: 'var(--el-color-warning)',  label: '神评'   },
  SYSTEM:  { icon: InfoFilled,   color: 'var(--el-color-info)',     label: '系统'   },
}

async function fetchNotifications() {
  loading.value = true
  error.value = false
  try {
    const res = await notificationApi.getList({ page: currentPage.value, size: pageSize })
    if (res.data) {
      notifications.value = res.data.records
      total.value = res.data.total
      totalPages.value = res.data.pages
    }
  } catch (e: unknown) {
    error.value = true
    errorMessage.value = e instanceof Error ? e.message : '加载通知失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function handleNotificationClick(item: NotificationVO) {
  // Mark as read if unread
  if (!item.isRead) {
    try {
      await notificationApi.markRead(item.id)
      item.isRead = true
      store.decrement(1)
    } catch {
      // Silently fail — still allow navigation
    }
  }

  // Navigate to source if available
  if (item.sourceId && item.sourceType) {
    if (item.sourceType === 'POST') {
      router.push(`/posts/${item.sourceId}`)
    } else if (item.sourceType === 'COMMENT') {
      router.push(`/posts/${item.sourceId}`)
    } else if (item.sourceType === 'USER') {
      router.push(`/users/${item.sourceId}`)
    }
  }
}

async function handleMarkAllRead() {
  try {
    await notificationApi.markAllRead()
    // Update local state
    for (const n of notifications.value) {
      n.isRead = true
    }
    store.reset()
  } catch {
    // Silently fail
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchNotifications()
}

function handleRetry() {
  fetchNotifications()
}

onMounted(() => {
  fetchNotifications()
})
</script>

<template>
  <div class="notification-page">
    <!-- Header -->
    <div class="notification-page__header">
      <h1 class="notification-page__title">通知中心</h1>
      <el-button
        v-if="notifications.length > 0 && !loading"
        type="primary"
        plain
        size="small"
        @click="handleMarkAllRead"
      >
        全部标为已读
      </el-button>
    </div>

    <!-- Loading state -->
    <LoadingSkeleton v-if="loading" variant="table" />

    <!-- Error state -->
    <el-alert
      v-else-if="error"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
      class="notification-page__alert"
    >
      <template #default>
        <el-button size="small" type="primary" @click="handleRetry">
          重试
        </el-button>
      </template>
    </el-alert>

    <!-- Empty state -->
    <EmptyState
      v-else-if="notifications.length === 0"
      :icon="Bell"
      title="暂无通知"
      description="当有人回复、点赞或关注你时，会在这里显示"
    />

    <!-- Notification list -->
    <div v-else class="notification-page__list">
      <div
        v-for="item in notifications"
        :key="item.id"
        class="notification-item"
        :class="{ 'notification-item--unread': !item.isRead }"
        @click="handleNotificationClick(item)"
      >
        <!-- Type icon -->
        <div class="notification-item__icon-wrap">
          <el-icon
            :size="18"
            class="notification-item__type-icon"
            :style="{ color: typeConfig[item.type].color }"
          >
            <component :is="typeConfig[item.type].icon" />
          </el-icon>
        </div>

        <!-- Content area -->
        <div class="notification-item__body">
          <div class="notification-item__content">
            <el-tag
              :type="
                item.type === 'REPLY' ? 'primary' :
                item.type === 'LIKE' ? 'danger' :
                item.type === 'FOLLOW' ? 'success' :
                item.type === 'DIVINE' ? 'warning' : 'info'
              "
              size="small"
              effect="plain"
              class="notification-item__tag"
            >
              {{ typeConfig[item.type].label }}
            </el-tag>
            <span class="notification-item__text">{{ item.content }}</span>
          </div>
          <span class="notification-item__time">
            {{ formatRelativeTime(item.createTime) }}
          </span>
        </div>

        <!-- Unread dot -->
        <div v-if="!item.isRead" class="notification-item__dot" />
      </div>

      <!-- Pagination -->
      <el-pagination
        v-if="totalPages > 1"
        :current-page="currentPage"
        :page-count="totalPages"
        :total="total"
        layout="prev, pager, next"
        background
        class="notification-page__pagination"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.notification-page {
  max-width: 720px;
  margin: 0 auto;
  padding: var(--th-spacing-6) 0;

  // ── Header ──
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--th-spacing-4);
  }

  &__title {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    color: var(--el-text-color-primary);
  }

  // ── Alert ──
  &__alert {
    margin-top: var(--th-spacing-4);
  }

  // ── Pagination ──
  &__pagination {
    display: flex;
    justify-content: center;
    margin-top: var(--th-spacing-6);
  }

  // ── List ──
  &__list {
    display: flex;
    flex-direction: column;
  }
}

// ── Notification Item ──
.notification-item {
  display: flex;
  align-items: flex-start;
  gap: var(--th-spacing-3);
  padding: var(--th-spacing-3) var(--th-spacing-4);
  border-radius: var(--th-radius-md);
  cursor: pointer;
  transition: background-color var(--th-transition-fast);
  position: relative;

  &:hover {
    background-color: var(--el-fill-color-light);
  }

  &--unread {
    background-color: var(--th-brand-primary-alpha, rgba(64, 158, 255, 0.04));
  }

  &__icon-wrap {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background-color: var(--el-fill-color-light);
    flex-shrink: 0;
    margin-top: 2px;
  }

  &__type-icon {
    flex-shrink: 0;
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__content {
    display: flex;
    align-items: flex-start;
    gap: var(--th-spacing-2);
    flex-wrap: wrap;
  }

  &__tag {
    flex-shrink: 0;
  }

  &__text {
    font-size: 14px;
    color: var(--el-text-color-primary);
    line-height: 1.6;
    word-break: break-word;
  }

  &__time {
    display: block;
    margin-top: var(--th-spacing-1);
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }

  &__dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background-color: var(--th-brand-primary);
    flex-shrink: 0;
    margin-top: 10px;
  }
}

// Dark mode
:global(.dark) .notification-item {
  &--unread {
    background-color: rgba(64, 158, 255, 0.08);
  }
}
</style>
