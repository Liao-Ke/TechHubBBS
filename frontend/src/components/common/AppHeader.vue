<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { categoryApi } from '@/api/modules/category'
import type { Category } from '@/api/types'
import NotificationBell from '@/components/notification/NotificationBell.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import {
  Search,
  ArrowDown,
  UserFilled,
  Setting,
  Document,
  EditPen,
  SwitchButton,
  Grid,
  Moon,
  Menu,
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const categories = ref<Category[]>([])
const searchKeyword = ref('')
const mobileDrawerVisible = ref(false)

onMounted(async () => {
  try {
    const res = await categoryApi.getList()
    if (res.data) {
      categories.value = res.data
    }
  } catch {
    // Silently fail — categories dropdown just won't show
  }
})

function handleSearch() {
  const keyword = searchKeyword.value.trim()
  if (!keyword) return
  mobileDrawerVisible.value = false
  router.push({ path: '/', query: { keyword } })
}

function handleLogout() {
  mobileDrawerVisible.value = false
  userStore.logout()
  router.push('/')
}

function navigateTo(path: string) {
  mobileDrawerVisible.value = false
  router.push(path)
}
</script>

<template>
  <header class="app-header">
    <div class="app-header__inner">
      <!-- Hamburger (mobile only) -->
      <el-button
        class="app-header__hamburger"
        :icon="Menu"
        text
        @click="mobileDrawerVisible = true"
      />

      <!-- Logo -->
      <router-link to="/" class="app-header__logo">
        TechHub
      </router-link>

      <!-- Desktop nav items (hidden on mobile) -->
      <div class="app-header__desktop-nav">
        <!-- Category dropdown -->
        <el-dropdown
          v-if="categories.length > 0"
          trigger="hover"
          class="app-header__category-dropdown"
        >
          <span class="app-header__category-trigger">
            版块 <el-icon class="app-header__category-arrow"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="cat in categories"
                :key="cat.id"
                @click="navigateTo(`/categories/${cat.id}`)"
              >
                {{ cat.name }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <!-- Search -->
        <el-input
          v-model="searchKeyword"
          placeholder="搜索..."
          :prefix-icon="Search"
          class="app-header__search"
          clearable
          @keyup.enter="handleSearch"
        />

        <!-- Create post button -->
        <el-button
          v-if="userStore.isLoggedIn"
          type="primary"
          :icon="EditPen"
          class="app-header__create-btn"
          @click="navigateTo('/posts/new')"
        >
          发帖
        </el-button>
      </div>

      <!-- Spacer -->
      <div class="app-header__spacer" />

      <!-- Desktop right area (hidden on mobile) -->
      <div class="app-header__desktop-right">
        <!-- Notification bell with unread badge -->
        <NotificationBell />

        <!-- User area -->
        <div v-if="!userStore.isLoggedIn" class="app-header__auth">
          <router-link to="/login" class="app-header__login-link">
            登录
          </router-link>
        </div>

        <el-dropdown v-else trigger="hover" class="app-header__user-dropdown">
          <span class="app-header__user-trigger">
            <UserAvatar
              :src="userStore.userInfo?.avatarUrl"
              :size="32"
              class="app-header__avatar"
            />
            <span class="app-header__username">
              {{ userStore.userInfo?.username }}
            </span>
            <el-icon class="app-header__user-arrow"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="navigateTo(`/users/${userStore.userInfo?.id}`)">
                <el-icon><UserFilled /></el-icon> 个人主页
              </el-dropdown-item>
              <el-dropdown-item @click="navigateTo('/drafts')">
                <el-icon><Document /></el-icon> 草稿箱
              </el-dropdown-item>
            <el-dropdown-item @click="navigateTo('/settings')">
              <el-icon><Setting /></el-icon> 设置
            </el-dropdown-item>
            <el-dropdown-item divided class="app-header__theme-toggle">
              <el-icon><Moon /></el-icon>
              <span>切换主题</span>
              <el-switch
                :model-value="appStore.isDarkMode"
                size="small"
                @click.stop
                @change="appStore.toggleDarkMode()"
              />
            </el-dropdown-item>
            <el-dropdown-item
              v-if="userStore.isAdmin"
              divided
              @click="navigateTo('/admin')"
            >
                <el-icon><Grid /></el-icon> 管理后台
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">
                <el-icon><SwitchButton /></el-icon> 退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- Mobile drawer -->
    <el-drawer
      v-model="mobileDrawerVisible"
      direction="ltr"
      size="280px"
      :z-index="2000"
      :with-header="false"
      class="app-header__mobile-drawer"
    >
      <div class="app-header__drawer-content">
        <!-- Drawer header -->
        <div class="app-header__drawer-title">TechHub</div>

        <!-- Mobile search -->
        <el-input
          v-model="searchKeyword"
          placeholder="搜索..."
          :prefix-icon="Search"
          class="app-header__drawer-search"
          clearable
          @keyup.enter="handleSearch"
        />

        <!-- Mobile categories -->
        <div v-if="categories.length > 0" class="app-header__drawer-section">
          <div class="app-header__drawer-label">版块</div>
          <div
            v-for="cat in categories"
            :key="cat.id"
            class="app-header__drawer-item"
            @click="navigateTo(`/categories/${cat.id}`)"
          >
            {{ cat.name }}
          </div>
        </div>

        <!-- Mobile nav links -->
        <div class="app-header__drawer-section">
          <div class="app-header__drawer-label">导航</div>
          <router-link to="/" class="app-header__drawer-item" @click="mobileDrawerVisible = false">
            首页
          </router-link>
          <div
            v-if="userStore.isLoggedIn"
            class="app-header__drawer-item"
            @click="navigateTo('/posts/new')"
          >
            发帖
          </div>
          <router-link
            v-if="!userStore.isLoggedIn"
            to="/login"
            class="app-header__drawer-item"
            @click="mobileDrawerVisible = false"
          >
            登录
          </router-link>
          <template v-else>
            <div
              class="app-header__drawer-item"
              @click="navigateTo(`/users/${userStore.userInfo?.id}`)"
            >
              个人主页
            </div>
            <div class="app-header__drawer-item" @click="navigateTo('/drafts')">
              草稿箱
            </div>
            <div class="app-header__drawer-item" @click="navigateTo('/notifications')">
              通知
            </div>
            <div class="app-header__drawer-item" @click="navigateTo('/settings')">
              设置
            </div>
            <div
              v-if="userStore.isAdmin"
              class="app-header__drawer-item"
              @click="navigateTo('/admin')"
            >
              管理后台
            </div>
            <div class="app-header__drawer-item app-header__drawer-item--logout" @click="handleLogout">
              退出登录
            </div>
          </template>
        </div>
      </div>
    </el-drawer>
  </header>
</template>

<style lang="scss" scoped>
// ======== Breakpoints ========
$bp-md: 768px;

.app-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  height: var(--th-header-height);
  background-color: var(--el-bg-color, #fff);
  border-bottom: 1px solid var(--el-border-color-light, #ebeef5);
  backdrop-filter: blur(8px);

  &__inner {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-4);
    height: 100%;
    max-width: var(--th-page-max-width);
    margin: 0 auto;
    padding: 0 var(--th-spacing-4);
  }

  // ======== Hamburger (mobile only) ========
  &__hamburger {
    display: none;
    flex-shrink: 0;
    font-size: 20px;

    @media (max-width: #{$bp-md - 1}) {
      display: flex;
    }
  }

  // ======== Desktop nav wrapper ========
  &__desktop-nav {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-4);

    @media (max-width: #{$bp-md - 1}) {
      display: none;
    }
  }

  // ======== Desktop right area ========
  &__desktop-right {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-3);

    @media (max-width: #{$bp-md - 1}) {
      display: none;
    }
  }

  // Logo
  &__logo {
    font-size: 20px;
    font-weight: 700;
    color: var(--th-brand-primary);
    text-decoration: none;
    white-space: nowrap;
    flex-shrink: 0;

    &:hover {
      color: var(--th-brand-primary-hover);
    }
  }

  // Category dropdown trigger
  &__category-trigger {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-1);
    cursor: pointer;
    font-size: 14px;
    color: var(--el-text-color-regular);
    white-space: nowrap;
    user-select: none;

    &:hover {
      color: var(--th-brand-primary);
    }
  }

  &__category-arrow {
    font-size: 12px;
    transition: transform var(--th-transition-fast);
  }

  // Search
  &__search {
    width: 240px;
    flex-shrink: 0;
  }

  // Create post button
  &__create-btn {
    flex-shrink: 0;
    font-weight: 600;

    @media (max-width: #{$bp-md - 1}) {
      display: none;
    }
  }

  // Spacer
  &__spacer {
    flex: 1;
  }

  // Login link
  &__auth {
    flex-shrink: 0;
  }

  &__login-link {
    font-size: 14px;
    color: var(--th-brand-primary);
    text-decoration: none;

    &:hover {
      color: var(--th-brand-primary-hover);
    }
  }

  // User dropdown trigger
  &__user-trigger {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-2);
    cursor: pointer;
    user-select: none;
    flex-shrink: 0;
  }

  &__avatar {
    flex-shrink: 0;
  }

  &__username {
    font-size: 14px;
    color: var(--el-text-color-regular);
    max-width: 100px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__user-arrow {
    font-size: 12px;
    color: var(--el-text-color-secondary, #909399);
    transition: transform var(--th-transition-fast);
  }

  // ======== Mobile drawer ========
  &__mobile-drawer {
    :deep(.el-drawer) {
      background-color: var(--el-bg-color);
    }
  }

  &__drawer-content {
    display: flex;
    flex-direction: column;
    gap: var(--th-spacing-4);
    padding-top: var(--th-spacing-2);
  }

  &__drawer-title {
    font-size: 18px;
    font-weight: 700;
    color: var(--th-brand-primary);
    padding-bottom: var(--th-spacing-3);
    border-bottom: 1px solid var(--el-border-color-light);
  }

  &__drawer-search {
    width: 100%;
  }

  &__drawer-section {
    display: flex;
    flex-direction: column;
  }

  &__drawer-label {
    font-size: 12px;
    font-weight: 600;
    color: var(--el-text-color-secondary);
    text-transform: uppercase;
    letter-spacing: 0.5px;
    padding: var(--th-spacing-2) 0;
  }

  &__drawer-item {
    display: block;
    padding: var(--th-spacing-3) var(--th-spacing-2);
    font-size: 15px;
    color: var(--el-text-color-primary);
    text-decoration: none;
    border-radius: var(--th-radius-sm);
    cursor: pointer;

    &:hover {
      background-color: var(--el-fill-color);
      color: var(--th-brand-primary);
    }

    &--logout {
      color: var(--el-color-danger);
    }
  }

  // Theme toggle in dropdown
  &__theme-toggle {
    :deep(.el-dropdown-menu__item) {
      display: flex !important;
      align-items: center;
      gap: var(--th-spacing-2);
    }
    .el-switch {
      margin-left: auto;
    }
  }
}

// Dark mode
:global(.dark) .app-header {
  background-color: var(--el-bg-color, #141414);
  border-bottom-color: var(--el-border-color-lighter, #333);
}
</style>
