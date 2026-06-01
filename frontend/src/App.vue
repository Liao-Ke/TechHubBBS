<script setup lang="ts">
// Dark mode is initialized in main.ts from localStorage/system preference.
// When stores/app.ts is implemented (Task 11), dark mode toggling will be
// managed through the app store here.
</script>

<template>
  <router-view v-slot="{ Component, route }">
    <transition name="page-fade" mode="out-in">
      <component :is="Component" :key="route.path" />
    </transition>
  </router-view>
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
