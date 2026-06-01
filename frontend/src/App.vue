<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import AuthLayout from '@/layouts/AuthLayout.vue'
// Dark mode is initialized in main.ts from localStorage/system preference.
// When stores/app.ts is implemented (Task 11), dark mode toggling will be
// managed through the app store here.

const route = useRoute()

const layoutComponent = computed(() => {
  switch (route.meta.layout) {
    case 'default':
      return DefaultLayout
    case 'auth':
      return AuthLayout
    default:
      // Admin routes use AdminLayout directly as route component (no meta.layout)
      return null
  }
})
</script>

<template>
  <component :is="layoutComponent" :key="(route.meta.layout as string) ?? 'none'">
    <router-view v-slot="{ Component, route: childRoute }">
      <transition name="page-fade" mode="out-in">
        <component :is="Component" :key="childRoute.path" />
      </transition>
    </router-view>
  </component>
</template>

<style lang="scss">
#app {
  min-height: 100vh;
}

// Page fade transition for route changes
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.25s ease;
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
}
</style>
