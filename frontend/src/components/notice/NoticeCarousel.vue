<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { noticeApi } from '@/api/modules/notice'
import type { CategoryNoticeVO } from '@/api/types'

const notices = ref<CategoryNoticeVO[]>([])
const loading = ref(false)

const noticeTypeLabel = (type: number): string =>
  type === 1 ? '活动' : '须知'

const noticeTypeTagType = (type: number): 'success' | 'info' =>
  type === 1 ? 'success' : 'info'

async function fetchNotices() {
  loading.value = true
  try {
    const res = await noticeApi.getList()
    // Only show pinned notices in the global carousel
    notices.value = (res.data.records || []).filter((n) => n.isPinned === 1)
  } catch {
    // Silently suppress errors — notices are non-critical UI
    notices.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchNotices)
</script>

<template>
  <!-- No pinned notices → render nothing -->
  <div v-if="notices.length === 0 && !loading" />

  <!-- Loading skeleton -->
  <div v-else-if="loading" class="notice-carousel notice-carousel--loading">
    <div class="notice-carousel__skeleton skeleton-shimmer" />
  </div>

  <!-- Single pinned notice → static display -->
  <div v-else-if="notices.length === 1" class="notice-carousel notice-carousel--single">
    <div class="notice-carousel__slide">
      <el-tag
        :type="noticeTypeTagType(notices[0]!.type)"
        size="small"
        effect="plain"
      >
        {{ noticeTypeLabel(notices[0]!.type) }}
      </el-tag>
      <router-link
        :to="`/notices/${notices[0]!.id}`"
        class="notice-carousel__link"
      >
        {{ notices[0]!.title }}
      </router-link>
    </div>
  </div>

  <!-- Multiple pinned notices → carousel -->
  <el-carousel
    v-else
    height="40px"
    :interval="5000"
    arrow="never"
    :indicator="false"
    class="notice-carousel"
  >
    <el-carousel-item
      v-for="notice in notices"
      :key="notice.id"
    >
      <div class="notice-carousel__slide">
        <el-tag
          :type="noticeTypeTagType(notice.type)"
          size="small"
          effect="plain"
        >
          {{ noticeTypeLabel(notice.type) }}
        </el-tag>
        <router-link
          :to="`/notices/${notice.id}`"
          class="notice-carousel__link"
        >
          {{ notice.title }}
        </router-link>
      </div>
    </el-carousel-item>
  </el-carousel>
</template>

<style lang="scss" scoped>
.notice-carousel {
  --nc-height: 40px;

  width: 100%;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);

  :deep(.el-carousel__container) {
    height: var(--nc-height);
  }

  // ── Loading ──
  &--loading {
    padding: 0 var(--th-spacing-md);
  }

  &__skeleton {
    height: var(--nc-height);
    border-radius: var(--th-radius-sm);
    background: var(--el-fill-color);
  }

  // ── Single pinned ──
  &--single {
    display: flex;
    align-items: center;
  }

  // ── Slide ──
  &__slide {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-sm);
    height: var(--nc-height);
    padding: 0 var(--th-spacing-md);
  }

  // ── Link ──
  &__link {
    font-size: 14px;
    color: var(--el-text-color-primary);
    text-decoration: none;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    flex: 1;

    &:hover {
      color: var(--el-color-primary);
    }
  }
}

// ── Skeleton shimmer ──
.skeleton-shimmer {
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(
      90deg,
      transparent 0%,
      var(--el-fill-color-light) 50%,
      transparent 100%
    );
    animation: shimmer 1.5s ease-in-out infinite;
  }
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}
</style>
