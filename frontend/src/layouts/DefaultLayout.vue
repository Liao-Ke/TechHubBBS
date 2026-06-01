<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import AppHeader from '@/components/common/AppHeader.vue'
import AppFooter from '@/components/common/AppFooter.vue'
import { useNotificationStore } from '@/stores/notification'
import { useUserStore } from '@/stores/user'

const notifStore = useNotificationStore()
const userStore = useUserStore()

let pollTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  if (userStore.isLoggedIn) {
    notifStore.fetchUnreadCount()
    pollTimer = setInterval(() => notifStore.fetchUnreadCount(), 30000)
  }
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
})
</script>

<template>
  <div class="default-layout">
    <AppHeader />
    <main class="default-layout__main">
      <router-view />
    </main>
    <AppFooter />
  </div>
</template>

<style lang="scss" scoped>
$bp-md: 768px;
$bp-lg: 1024px;

.default-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;

  &__main {
    flex: 1;
    padding-top: var(--th-header-height);
    max-width: var(--th-page-max-width);
    width: 100%;
    margin: 0 auto;
    padding-left: var(--th-spacing-3);
    padding-right: var(--th-spacing-3);
    padding-bottom: var(--th-spacing-6);

    @media (min-width: $bp-md) {
      padding-left: var(--th-spacing-4);
      padding-right: var(--th-spacing-4);
      padding-bottom: var(--th-spacing-8);
    }

    @media (min-width: $bp-lg) {
      padding-left: var(--th-spacing-6);
      padding-right: var(--th-spacing-6);
    }
  }
}
</style>
