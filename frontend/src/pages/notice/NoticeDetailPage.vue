<script setup lang="ts">
/**
 * NoticeDetailPage.vue — Read-only notice/announcement detail page.
 *
 * States: loading (skeleton), error (404 / 5xx with EmptyState), content.
 */
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { noticeApi } from '@/api/modules/notice'
import { ApiError } from '@/api'
import { formatRelativeTime } from '@/utils/format'
import type { CategoryNoticeVO } from '@/api/types'

import MdViewer from '@/components/markdown/MdViewer.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { WarningFilled, ArrowLeft } from '@element-plus/icons-vue'

// ---------------------------------------------------------------------------
// Router
// ---------------------------------------------------------------------------
const router = useRouter()
const route = useRoute()

const noticeId = route.params.id as string

// ---------------------------------------------------------------------------
// Reactive state
// ---------------------------------------------------------------------------
const loading = ref(true)
const errorCode = ref<number | null>(null)
const notice = ref<CategoryNoticeVO | null>(null)

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
const noticeTypeLabel = (type: number): string =>
  type === 1 ? '活动' : '须知'

const noticeTypeTagType = (type: number): 'success' | 'info' =>
  type === 1 ? 'success' : 'info'

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

// ---------------------------------------------------------------------------
// Fetch
// ---------------------------------------------------------------------------
onMounted(async () => {
  loading.value = true
  errorCode.value = null
  try {
    const res = await noticeApi.getDetail(noticeId)
    notice.value = res.data
    document.title = `${res.data.title} - TechHub`
  } catch (err: unknown) {
    if (err instanceof ApiError) {
      errorCode.value = err.code
    } else {
      errorCode.value = 500
    }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="notice-detail">
    <!-- ================================================================ -->
    <!-- LOADING STATE (skeleton)                                          -->
    <!-- ================================================================ -->
    <div v-if="loading" class="notice-detail__loading">
      <div class="notice-detail__loading-header">
        <div class="skeleton-line skeleton-line--title" />
        <div class="skeleton-line skeleton-line--meta" />
        <div class="skeleton-line skeleton-line--meta" style="width: 40%" />
      </div>
      <div class="notice-detail__loading-content">
        <div class="skeleton-line skeleton-line--para" />
        <div class="skeleton-line skeleton-line--para" style="width: 90%" />
        <div class="skeleton-line skeleton-line--para" style="width: 80%" />
        <div class="skeleton-line skeleton-line--para" style="width: 60%" />
      </div>
    </div>

    <!-- ================================================================ -->
    <!-- ERROR / NOT FOUND                                                 -->
    <!-- ================================================================ -->
    <div v-else-if="errorCode" class="notice-detail__error">
      <EmptyState
        :icon="WarningFilled"
        :title="errorCode === 404 ? '公告不存在' : '无法加载公告'"
        :description="errorCode === 404
          ? '该公告可能已被删除或不存在。'
          : '加载公告时出现错误，请稍后重试。'"
        action-text="返回首页"
        action-route="/"
      />
    </div>

    <!-- ================================================================ -->
    <!-- MAIN CONTENT                                                      -->
    <!-- ================================================================ -->
    <template v-else-if="notice">
      <!-- Back button -->
      <button class="notice-detail__back" @click="goBack">
        <el-icon :size="18">
          <ArrowLeft />
        </el-icon>
        <span>返回</span>
      </button>

      <!-- Header -->
      <header class="notice-detail__header">
        <h1 class="notice-detail__title">{{ notice.title }}</h1>

        <div class="notice-detail__meta">
          <div class="notice-detail__meta-left">
            <span class="notice-detail__author">{{ notice.authorName }}</span>
            <span class="notice-detail__separator">·</span>
            <span class="notice-detail__time">{{ formatRelativeTime(notice.createTime) }}</span>
          </div>
          <div class="notice-detail__meta-right">
            <el-tag
              :type="noticeTypeTagType(notice.type)"
              size="small"
              effect="plain"
            >
              {{ noticeTypeLabel(notice.type) }}
            </el-tag>
          </div>
        </div>
      </header>

      <!-- Content -->
      <section class="notice-detail__content">
        <MdViewer :content="notice.content" />
      </section>
    </template>
  </div>
</template>

<style lang="scss" scoped>
// =========================================================================
// CSS Variables
// =========================================================================
$max-content-width: 800px;

// =========================================================================
// Container
// =========================================================================
.notice-detail {
  max-width: $max-content-width;
  margin: 0 auto;
  padding: var(--th-spacing-lg) var(--th-spacing-md);
  min-height: 60vh;

  // ── Loading / Skeleton ──
  &__loading {
    padding: var(--th-spacing-xl) 0;
  }

  &__loading-header {
    margin-bottom: var(--th-spacing-xl);
  }

  &__loading-content {
    display: flex;
    flex-direction: column;
    gap: var(--th-spacing-sm);
  }

  // ── Error ──
  &__error {
    padding-top: var(--th-spacing-16);
  }
}

// =========================================================================
// Skeleton lines (shared)
// =========================================================================
.skeleton-line {
  height: 16px;
  border-radius: var(--th-radius-sm);
  background: var(--el-fill-color);
  margin-bottom: var(--th-spacing-sm);
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

  &--title {
    height: 28px;
    width: 65%;
    margin-bottom: var(--th-spacing-md);
  }

  &--meta {
    width: 30%;
    height: 14px;
  }

  &--para {
    width: 100%;
  }
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

// =========================================================================
// Back button
// =========================================================================
.notice-detail {
  &__back {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px var(--th-spacing-sm);
    margin-bottom: var(--th-spacing-md);
    border: none;
    background: none;
    color: var(--el-text-color-secondary);
    font-size: 14px;
    cursor: pointer;
    border-radius: var(--th-radius-sm);
    transition: background-color var(--th-transition-fast),
                color var(--th-transition-fast);

    &:hover {
      background: var(--el-fill-color);
      color: var(--el-text-color-primary);
    }
  }
}

// =========================================================================
// Header
// =========================================================================
.notice-detail {
  &__header {
    margin-bottom: var(--th-spacing-lg);
    padding-bottom: var(--th-spacing-md);
    border-bottom: 1px solid var(--el-border-color-light);
  }

  &__title {
    margin: 0 0 var(--th-spacing-md);
    font-size: 28px;
    font-weight: 700;
    line-height: 1.35;
    color: var(--el-text-color-primary);
  }

  &__meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: var(--th-spacing-sm);
  }

  &__meta-left {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-xs);
  }

  &__meta-right {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-sm);
  }

  &__author {
    font-size: 15px;
    font-weight: 500;
    color: var(--el-text-color-primary);
  }

  &__separator {
    font-size: 14px;
    color: var(--el-text-color-placeholder);
  }

  &__time {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }
}

// =========================================================================
// Content
// =========================================================================
.notice-detail {
  &__content {
    line-height: 1.8;
    font-size: 16px;
  }
}
</style>
