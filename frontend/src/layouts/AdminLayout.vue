<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { Menu } from '@element-plus/icons-vue'
import AppSidebar from '@/components/common/AppSidebar.vue'

const appStore = useAppStore()
const mobileDrawerVisible = ref(false)
const isMobile = ref(false)

const BP_MD = 768

function checkMobile() {
  isMobile.value = window.innerWidth < BP_MD
}

let resizeTimer: ReturnType<typeof setTimeout> | null = null

function onResize() {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = setTimeout(checkMobile, 100)
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  if (resizeTimer) clearTimeout(resizeTimer)
})

function openMobileDrawer() {
  mobileDrawerVisible.value = true
}

function closeMobileDrawer() {
  mobileDrawerVisible.value = false
}
</script>

<template>
  <el-container class="admin-layout">
    <!-- Desktop sidebar (hidden on mobile) -->
    <el-aside
      v-if="!isMobile"
      :width="appStore.sidebarCollapsed ? '64px' : '220px'"
      class="admin-layout__aside"
    >
      <AppSidebar />
    </el-aside>

    <!-- Mobile top bar -->
    <div v-if="isMobile" class="admin-layout__mobile-bar">
      <el-button
        :icon="Menu"
        text
        class="admin-layout__hamburger"
        @click="openMobileDrawer"
      />
      <span class="admin-layout__mobile-title">管理后台</span>
    </div>

    <el-container>
      <el-main class="admin-layout__main">
        <router-view />
      </el-main>
    </el-container>

    <!-- Mobile drawer with sidebar -->
    <el-drawer
      v-model="mobileDrawerVisible"
      direction="ltr"
      size="220px"
      :z-index="2000"
      :with-header="false"
      class="admin-layout__mobile-drawer"
      @close="closeMobileDrawer"
    >
      <AppSidebar />
    </el-drawer>
  </el-container>
</template>

<style lang="scss" scoped>
$bp-md: 768px;

.admin-layout {
  min-height: 100vh;
  background-color: var(--el-bg-color-page, #f5f7fa);

  &__aside {
    transition: width var(--th-transition-normal);
    overflow: hidden;
    background-color: var(--el-bg-color, #fff);
    border-right: 1px solid var(--el-border-color-light, #ebeef5);
  }

  // ======== Mobile top bar ========
  &__mobile-bar {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-3);
    height: var(--th-header-height);
    padding: 0 var(--th-spacing-4);
    background-color: var(--el-bg-color, #fff);
    border-bottom: 1px solid var(--el-border-color-light, #ebeef5);
    flex-shrink: 0;
  }

  &__hamburger {
    flex-shrink: 0;
    font-size: 20px;
  }

  &__mobile-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  // ======== Mobile drawer ========
  &__mobile-drawer {
    :deep(.el-drawer) {
      background-color: var(--el-bg-color);
    }
  }

  &__main {
    padding: var(--th-spacing-4);
    min-height: 100vh;
    background-color: var(--el-bg-color-page, #f5f7fa);

    @media (min-width: $bp-md) {
      padding: var(--th-spacing-6);
    }
  }
}

// Dark mode overrides
:global(.dark) .admin-layout {
  &__aside,
  &__mobile-bar {
    background-color: var(--el-bg-color, #141414);
    border-right-color: var(--el-border-color-lighter, #333);
    border-bottom-color: var(--el-border-color-lighter, #333);
  }
}
</style>
