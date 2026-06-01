<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Delete, EditPen } from '@element-plus/icons-vue'
import { draftApi } from '@/api/modules/draft'
import { visibilityLabel } from '@/composables/useVisibility'
import { formatRelativeTime } from '@/utils/format'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { PostDraft } from '@/api/types'

const router = useRouter()

// ── Reactive state ──
const loading = ref(true)
const error = ref(false)
const drafts = ref<PostDraft[]>([])

// ── Data fetching ──
async function fetchDrafts() {
  loading.value = true
  error.value = false
  try {
    const res = await draftApi.getList()
    drafts.value = res.data || []
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

// ── Actions ──
async function handleContinue(draft: PostDraft) {
  if (draft.postId) {
    router.push(`/posts/${draft.postId}/edit`)
  } else {
    router.push('/posts/new')
  }
}

async function handleDelete(draft: PostDraft) {
  try {
    await ElMessageBox.confirm('确定要删除该草稿吗？删除后无法恢复。', '删除草稿', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await draftApi.remove(draft.id)
    ElMessage.success('草稿已删除')
    await fetchDrafts()
  } catch {
    // User cancelled the confirm dialog or API error — both are handled silently
  }
}

// ── Lifecycle ──
onMounted(() => {
  fetchDrafts()
})
</script>

<template>
  <div class="draft-page">
    <!-- ══════════════════════════════════════════ -->
    <!-- Page Header                                 -->
    <!-- ══════════════════════════════════════════ -->
    <header class="draft-page__header">
      <h1 class="draft-page__title">草稿箱</h1>
      <span
        v-if="!loading && !error && drafts.length > 0"
        class="draft-page__count"
      >
        共 {{ drafts.length }} 篇草稿
      </span>
    </header>

    <!-- ══════════════════════════════════════════ -->
    <!-- Error State                                 -->
    <!-- ══════════════════════════════════════════ -->
    <el-alert
      v-if="error"
      type="error"
      title="加载草稿失败"
      show-icon
      :closable="false"
      class="draft-page__alert"
    >
      <template #default>
        <el-button
          size="small"
          text
          type="primary"
          @click="fetchDrafts"
        >
          重试
        </el-button>
      </template>
    </el-alert>

    <!-- ══════════════════════════════════════════ -->
    <!-- Loading Skeleton                            -->
    <!-- ══════════════════════════════════════════ -->
    <LoadingSkeleton
      v-else-if="loading"
      variant="post-list"
    />

    <!-- ══════════════════════════════════════════ -->
    <!-- Empty State                                 -->
    <!-- ══════════════════════════════════════════ -->
    <EmptyState
      v-else-if="drafts.length === 0"
      title="暂无草稿"
      description="还没有保存的草稿，去写一篇帖子吧"
      action-text="去写帖子"
      action-route="/posts/new"
    />

    <!-- ══════════════════════════════════════════ -->
    <!-- Draft List                                  -->
    <!-- ══════════════════════════════════════════ -->
    <div
      v-else
      class="draft-list"
    >
      <article
        v-for="draft in drafts"
        :key="draft.id"
        class="draft-card"
      >
        <div class="draft-card__body">
          <!-- Title -->
          <h2 class="draft-card__title">
            {{ draft.title || '无标题' }}
          </h2>

          <!-- Meta -->
          <div class="draft-card__meta">
            <span
              v-if="draft.categoryName"
              class="draft-card__meta-item"
            >
              {{ draft.categoryName }}
            </span>
            <span
              v-if="draft.visibility !== undefined"
              class="draft-card__meta-item"
            >
              {{ visibilityLabel(draft.visibility) }}
            </span>
            <span class="draft-card__meta-item draft-card__meta-item--time">
              {{ formatRelativeTime(draft.lastSavedAt) }}
            </span>
          </div>
        </div>

        <!-- Actions -->
        <div class="draft-card__actions">
          <el-button
            type="primary"
            size="small"
            :icon="EditPen"
            plain
            @click="handleContinue(draft)"
          >
            继续编辑
          </el-button>
          <el-button
            type="danger"
            size="small"
            :icon="Delete"
            text
            @click="handleDelete(draft)"
          >
            删除
          </el-button>
        </div>
      </article>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.draft-page {
  max-width: var(--th-content-max-width);
  margin: 0 auto;
  padding: var(--th-spacing-4) var(--th-spacing-4) var(--th-spacing-12);

  &__alert {
    margin-bottom: var(--th-spacing-4);
  }
}

/* ======== Header ======== */
.draft-page__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--th-spacing-4);
  padding-bottom: var(--th-spacing-6);
  margin-bottom: var(--th-spacing-4);
  border-bottom: 1px solid var(--el-border-color-light);
}

.draft-page__title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  font-family: var(--th-font-heading);
  color: var(--el-text-color-primary);
  line-height: 1.3;
}

.draft-page__count {
  font-size: 13px;
  color: var(--el-text-color-placeholder);
  flex-shrink: 0;
}

/* ======== Draft List ======== */
.draft-list {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-light);
  border-radius: var(--th-radius-md);
  overflow: hidden;
}

/* ======== Draft Card ======== */
.draft-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--th-spacing-4);
  padding: var(--th-spacing-4);

  &:not(:last-child) {
    border-bottom: 1px solid var(--el-border-color-light);
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__title {
    margin: 0 0 var(--th-spacing-1);
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-3);
    flex-wrap: wrap;
  }

  &__meta-item {
    font-size: 13px;
    color: var(--el-text-color-secondary);

    &--time {
      color: var(--el-text-color-placeholder);
    }
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-2);
    flex-shrink: 0;
  }
}
</style>
