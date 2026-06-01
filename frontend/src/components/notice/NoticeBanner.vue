<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { noticeApi } from '@/api/modules/notice'
import type { CategoryNoticeVO } from '@/api/types'
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue'

const props = defineProps<{
  categoryId: string
}>()

const notices = ref<CategoryNoticeVO[]>([])
const loading = ref(false)
const showAllNonPinned = ref(false)

const pinnedNotices = computed(() =>
  notices.value.filter((n) => n.isPinned === 1)
)

const nonPinnedNotices = computed(() =>
  notices.value.filter((n) => n.isPinned !== 1)
)

/** Collapsed: show first 3; expanded: show all */
const visibleNonPinned = computed(() =>
  showAllNonPinned.value
    ? nonPinnedNotices.value
    : nonPinnedNotices.value.slice(0, 3)
)

const hasNonPinned = computed(() => nonPinnedNotices.value.length > 0)

const noticeTypeLabel = (type: number): string =>
  type === 1 ? '活动' : '须知'

const noticeTypeTagType = (type: number): 'success' | 'info' =>
  type === 1 ? 'success' : 'info'

async function fetchNotices() {
  loading.value = true
  try {
    const res = await noticeApi.getList({ categoryId: props.categoryId })
    notices.value = res.data.records || []
  } finally {
    loading.value = false
  }
}

onMounted(fetchNotices)
watch(() => props.categoryId, fetchNotices)
</script>

<template>
  <!-- No notices → render nothing -->
  <div v-if="notices.length === 0 && !loading" />

  <!-- Loading skeleton -->
  <div v-else-if="loading" class="notice-banner notice-banner--loading">
    <div class="notice-banner__skeleton skeleton-shimmer" />
  </div>

  <!-- Has notices -->
  <div v-else class="notice-banner">
    <!-- ── Pinned: single (static) ── -->
    <div v-if="pinnedNotices.length === 1" class="notice-banner__single">
      <el-tag
        :type="noticeTypeTagType(pinnedNotices[0]!.type)"
        size="small"
        effect="plain"
        class="notice-banner__tag"
      >
        {{ noticeTypeLabel(pinnedNotices[0]!.type) }}
      </el-tag>
      <router-link
        :to="`/notices/${pinnedNotices[0]!.id}`"
        class="notice-banner__title"
      >
        {{ pinnedNotices[0]!.title }}
      </router-link>
    </div>

    <!-- ── Pinned: multiple (carousel) ── -->
    <el-carousel
      v-else-if="pinnedNotices.length > 1"
      height="40px"
      :interval="5000"
      arrow="never"
      :indicator="false"
      class="notice-banner__carousel"
    >
      <el-carousel-item
        v-for="notice in pinnedNotices"
        :key="notice.id"
      >
        <div class="notice-banner__slide">
          <el-tag
            :type="noticeTypeTagType(notice.type)"
            size="small"
            effect="plain"
            class="notice-banner__tag"
          >
            {{ noticeTypeLabel(notice.type) }}
          </el-tag>
          <router-link
            :to="`/notices/${notice.id}`"
            class="notice-banner__title"
          >
            {{ notice.title }}
          </router-link>
        </div>
      </el-carousel-item>
    </el-carousel>

    <!-- ── Non-pinned: collapsible list ── -->
    <ul v-if="hasNonPinned" class="notice-banner__list">
      <li
        v-for="notice in visibleNonPinned"
        :key="notice.id"
        class="notice-banner__item"
      >
        <el-tag
          :type="noticeTypeTagType(notice.type)"
          size="small"
          effect="plain"
          class="notice-banner__tag"
        >
          {{ noticeTypeLabel(notice.type) }}
        </el-tag>
        <router-link
          :to="`/notices/${notice.id}`"
          class="notice-banner__title"
        >
          {{ notice.title }}
        </router-link>
        <span class="notice-banner__time">{{ notice.createTime }}</span>
      </li>
    </ul>

    <!-- Toggle button when non-pinned > 3 -->
    <button
      v-if="nonPinnedNotices.length > 3"
      class="notice-banner__toggle"
      @click="showAllNonPinned = !showAllNonPinned"
    >
      <el-icon :size="14" class="notice-banner__toggle-icon">
        <ArrowUp v-if="showAllNonPinned" />
        <ArrowDown v-else />
      </el-icon>
      <span>
        {{ showAllNonPinned ? '收起' : `查看全部 ${nonPinnedNotices.length} 条公告` }}
      </span>
    </button>
  </div>
</template>

<style lang="scss" scoped>
.notice-banner {
  --nb-height: 40px;

  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);

  // ── Loading skeleton ──
  &--loading {
    padding: 0 var(--th-spacing-md);
  }

  &__skeleton {
    height: var(--nb-height);
    border-radius: var(--th-radius-sm);
    background: var(--el-fill-color);
  }

  // ── Single pinned notice ──
  &__single {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-sm);
    height: var(--nb-height);
    padding: 0 var(--th-spacing-md);
  }

  // ── Carousel ──
  &__carousel {
    :deep(.el-carousel__container) {
      height: var(--nb-height);
    }
  }

  &__slide {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-sm);
    height: var(--nb-height);
    padding: 0 var(--th-spacing-md);
  }

  // ── Tag ──
  &__tag {
    flex-shrink: 0;
    font-size: 12px;
  }

  // ── Title link ──
  &__title {
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

  // ── Non-pinned list ──
  &__list {
    list-style: none;
    margin: 0;
    padding: 0;
  }

  &__item {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-sm);
    padding: var(--th-spacing-xs) var(--th-spacing-md);
    border-top: 1px solid var(--el-border-color-extra-light);
    font-size: 13px;
  }

  &__time {
    flex-shrink: 0;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }

  // ── Toggle ──
  &__toggle {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    width: 100%;
    padding: var(--th-spacing-xs) 0;
    border: none;
    border-top: 1px solid var(--el-border-color-extra-light);
    background: var(--el-fill-color-lighter);
    color: var(--el-text-color-secondary);
    font-size: 13px;
    cursor: pointer;
    transition: background-color var(--th-transition-fast);

    &:hover {
      background: var(--el-fill-color-light);
      color: var(--el-color-primary);
    }
  }

  &__toggle-icon {
    transition: transform var(--th-transition-fast);
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
