<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { Bell } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const notificationStore = useNotificationStore()

function handleClick() {
  router.push('/notifications')
}
</script>

<template>
  <div
    v-if="userStore.isLoggedIn"
    class="notification-bell"
    aria-label="通知"
  >
    <el-badge
      :value="notificationStore.unreadCount"
      :hidden="notificationStore.unreadCount === 0"
      :max="99"
      class="notification-bell__badge"
    >
      <el-icon
        :size="20"
        class="notification-bell__icon"
        @click="handleClick"
      >
        <Bell />
      </el-icon>
    </el-badge>
  </div>
</template>

<style lang="scss" scoped>
.notification-bell {
  display: flex;
  align-items: center;
  flex-shrink: 0;

  &__badge {
    display: flex;
    align-items: center;
  }

  &__icon {
    cursor: pointer;
    color: var(--el-text-color-secondary, #909399);
    transition: color var(--th-transition-fast);

    &:hover {
      color: var(--th-brand-primary);
    }
  }
}
</style>
