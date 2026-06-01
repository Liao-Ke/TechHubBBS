<script setup lang="ts">
// AuthLayout — centered card layout for login/register pages
</script>

<template>
  <div class="auth-layout">
    <div class="auth-layout__card">
      <div class="auth-layout__logo">
        <span class="auth-layout__logo-text">TechHub</span>
        <span class="auth-layout__logo-tagline">技术社区</span>
      </div>
      <div class="auth-layout__content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
$bp-xs-max: 639px;

.auth-layout {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: var(--th-spacing-4);
  background:
    radial-gradient(ellipse 80% 50% at 50% -20%, rgba(88, 166, 255, 0.06), transparent),
    var(--el-bg-color-page);

  @media (min-width: #{$bp-xs-max + 1}) {
    padding: var(--th-spacing-6);
  }

  &__card {
    width: 100%;
    max-width: 420px;
    padding: var(--th-spacing-6);
    background: var(--el-bg-color-overlay);
    border: 1px solid var(--el-border-color-light);
    border-radius: var(--th-radius-xl);
    box-shadow: var(--th-shadow-md);
    animation: auth-card-enter 0.4s ease-out;

    @media (min-width: #{$bp-xs-max + 1}) {
      padding: var(--th-spacing-8);
    }
  }

  &__logo {
    text-align: center;
    margin-bottom: var(--th-spacing-8);
  }

  &__logo-text {
    display: block;
    font-size: 28px;
    font-weight: 700;
    color: var(--th-brand-primary);
    letter-spacing: -0.5px;
    line-height: 1.2;
  }

  &__logo-tagline {
    display: block;
    font-size: 13px;
    font-weight: 400;
    color: var(--el-text-color-secondary);
    margin-top: var(--th-spacing-1);
    letter-spacing: 0.5px;
  }

  &__content {
    // router-view content
  }
}

@keyframes auth-card-enter {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// Dark mode fine-tuning
:global(.dark) .auth-layout {
  &__card {
    box-shadow:
      0 0 0 1px rgba(88, 166, 255, 0.08),
      var(--th-shadow-md);
  }
}
</style>
