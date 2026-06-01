<script setup lang="ts">
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import {
  DataAnalysis,
  UserFilled,
  Document,
  Grid,
  Bell,
  Medal,
  Expand,
  Fold,
} from '@element-plus/icons-vue'

const route = useRoute()
const appStore = useAppStore()
const userStore = useUserStore()

const collapsed = appStore.sidebarCollapsed
const isAdmin = userStore.isAdmin
const isModerator = userStore.isModerator
</script>

<template>
  <div class="app-sidebar">
    <div class="app-sidebar__header">
      <span v-if="!collapsed" class="app-sidebar__title">管理后台</span>
      <el-button
        :icon="collapsed ? Expand : Fold"
        text
        class="app-sidebar__toggle"
        @click="appStore.toggleSidebar()"
      />
    </div>
    <el-menu
      :router="true"
      :default-active="route.path"
      :collapse="collapsed"
      class="app-sidebar__menu"
    >
      <el-menu-item index="/admin">
        <el-icon><DataAnalysis /></el-icon>
        <span>仪表盘</span>
      </el-menu-item>
      <el-menu-item v-if="isAdmin" index="/admin/users">
        <el-icon><UserFilled /></el-icon>
        <span>用户管理</span>
      </el-menu-item>
      <el-menu-item v-if="isModerator" index="/admin/posts">
        <el-icon><Document /></el-icon>
        <span>帖子管理</span>
      </el-menu-item>
      <el-menu-item v-if="isAdmin" index="/admin/categories">
        <el-icon><Grid /></el-icon>
        <span>版块管理</span>
      </el-menu-item>
      <el-menu-item v-if="isModerator" index="/admin/notices">
        <el-icon><Bell /></el-icon>
        <span>公告管理</span>
      </el-menu-item>
      <el-menu-item v-if="isAdmin" index="/admin/divine">
        <el-icon><Medal /></el-icon>
        <span>神评管理</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<style lang="scss" scoped>
.app-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: var(--th-header-height);
    padding: 0 var(--th-spacing-4);
    border-bottom: 1px solid var(--el-border-color-light, #ebeef5);
    flex-shrink: 0;
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__toggle {
    flex-shrink: 0;
  }

  &__menu {
    flex: 1;
    border-right: none;
    overflow-y: auto;
    overflow-x: hidden;

    .el-menu-item {
      height: 48px;
      line-height: 48px;
      font-size: 14px;

      .el-icon {
        font-size: 18px;
      }
    }
  }
}
</style>
