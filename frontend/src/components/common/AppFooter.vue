<script setup lang="ts">
// Simple footer — no logic needed
</script>

<template>
  <footer class="app-footer">
    <div class="app-footer__inner">
      <span class="app-footer__copyright">TechHub &copy; 2026</span>
      <nav class="app-footer__links">
        <router-link to="/about" class="app-footer__link">关于</router-link>
        <router-link to="/feedback" class="app-footer__link">反馈</router-link>
        <router-link to="/terms" class="app-footer__link">用户协议</router-link>
      </nav>
    </div>
  </footer>
</template>

<style lang="scss" scoped>
$bp-sm: 640px;

.app-footer {
  border-top: 1px solid var(--el-border-color-light, #ebeef5);
  padding: var(--th-spacing-4);
  background-color: var(--el-bg-color, #fff);

  @media (min-width: $bp-sm) {
    padding: var(--th-spacing-6);
  }

  &__inner {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: var(--th-spacing-2);
    max-width: var(--th-page-max-width);
    margin: 0 auto;
    font-size: 14px;
    color: var(--el-text-color-secondary, #909399);

    @media (min-width: $bp-sm) {
      flex-direction: row;
      gap: var(--th-spacing-4);
    }
  }

  &__copyright {
    font-size: 14px;
  }

  &__links {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-3);
  }

  &__link {
    font-size: 14px;
    color: var(--el-text-color-secondary, #909399);
    text-decoration: none;
    transition: color var(--th-transition-fast);

    &:hover {
      color: var(--th-brand-primary);
    }
  }
}

// Dark mode
:global(.dark) .app-footer {
  background-color: var(--el-bg-color, #141414);
  border-top-color: var(--el-border-color-lighter, #333);
}
</style>
