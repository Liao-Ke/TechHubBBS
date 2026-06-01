<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/')
}
</script>

<template>
  <div class="error-page">
    <div class="error-page__code">404</div>
    <h1 class="error-page__title">页面不存在</h1>
    <p class="error-page__desc">你访问的页面不存在或已被删除</p>
    <el-button type="primary" size="large" @click="goHome">返回首页</el-button>
  </div>
</template>

<style lang="scss" scoped>
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  text-align: center;

  &__code {
    font-size: 72px;
    font-weight: bold;
    color: var(--el-text-color-placeholder);
    line-height: 1;
    margin-bottom: 16px;
  }

  &__title {
    font-size: 24px;
    font-weight: 600;
    margin-bottom: 8px;
    color: var(--el-text-color-primary);
  }

  &__desc {
    font-size: 14px;
    color: var(--el-text-color-secondary);
    margin-bottom: 24px;
  }
}
</style>
