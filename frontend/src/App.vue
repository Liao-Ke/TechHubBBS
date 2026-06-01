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
      // Routes that provide their own layout wrapper as a route component
      // (e.g., /admin → AdminLayout.vue). Do NOT return null —
      // Vue 3 <component :is="null"> does not render slot children.
      return undefined
  }
})
</script>

<template>
  <!-- Default / Auth layouts: wrap page components with layout + transition -->
  <template v-if="layoutComponent">
    <component :is="layoutComponent" :key="route.meta.layout as string">
      <router-view v-slot="{ Component, route: childRoute }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" :key="childRoute.path" />
        </transition>
      </router-view>
    </component>
  </template>

  <!-- Admin routes: AdminLayout is the route component itself with its own <router-view>.
       Render directly without wrapping — let Vue Router handle nested depth tracking. -->
  <router-view v-else />
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
