<script setup lang="ts">
import { FolderOpened } from '@element-plus/icons-vue'
import type { Component } from 'vue'

withDefaults(defineProps<{
  icon?: Component
  title: string
  description?: string
  actionText?: string
  actionRoute?: string
}>(), {
  icon: () => FolderOpened,
})
</script>

<template>
  <div class="empty-state">
    <el-icon :size="64" class="empty-state__icon">
      <component :is="icon" />
    </el-icon>
    <h3 class="empty-state__title">{{ title }}</h3>
    <p v-if="description" class="empty-state__desc">{{ description }}</p>
    <router-link v-if="actionText && actionRoute" :to="actionRoute">
      <el-button type="primary" class="empty-state__action">
        {{ actionText }}
      </el-button>
    </router-link>
  </div>
</template>

<style lang="scss" scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--th-spacing-12) var(--th-spacing-4);
  text-align: center;

  &__icon {
    color: var(--el-text-color-placeholder);
  }

  &__title {
    margin-top: var(--th-spacing-4);
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    line-height: 1.5;
  }

  &__desc {
    margin-top: var(--th-spacing-2);
    font-size: 14px;
    color: var(--el-text-color-secondary);
    line-height: 1.5;
    max-width: 320px;
  }

  &__action {
    margin-top: var(--th-spacing-4);
  }
}
</style>
