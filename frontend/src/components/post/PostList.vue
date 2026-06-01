<script setup lang="ts">
import type { PostVO } from '@/api/types/post'
import PostCard from './PostCard.vue'

defineProps<{
  posts: PostVO[]
  loading: boolean
  currentPage: number
  totalPages: number
  total: number
}>()

defineEmits<{
  'page-change': [page: number]
}>()
</script>

<template>
  <div class="post-list">
    <!-- Loading: show skeleton cards -->
    <template v-if="loading">
      <PostCard v-for="n in 3" :key="'skeleton-' + n" :post="null" />
    </template>

    <!-- Empty state -->
    <div v-else-if="posts.length === 0" class="post-list__empty">
      <div class="post-list__empty-icon">
        <el-icon :size="48"><svg viewBox="0 0 1024 1024" width="1em" height="1em" fill="currentColor"><path d="M810.666667 170.666667H213.333333c-46.933333 0-85.333333 38.4-85.333333 85.333333v512c0 46.933333 38.4 85.333333 85.333333 85.333333h597.333334c46.933333 0 85.333333-38.4 85.333333-85.333333V256c0-46.933333-38.4-85.333333-85.333333-85.333333z m0 85.333333v85.333334H213.333333V256h597.333334z m0 170.666667v256H213.333333v-256h597.333334z m-597.333334 512v-85.333334h597.333334v85.333334H213.333333z" /></svg></el-icon>
      </div>
      <p class="post-list__empty-text">暂无帖子</p>
      <p class="post-list__empty-hint">这里还没有任何内容发布</p>
    </div>

    <!-- Posts -->
    <template v-else>
      <PostCard
        v-for="post in posts"
        :key="post.id"
        :post="post"
      />
    </template>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="post-list__pagination">
      <el-pagination
        :current-page="currentPage"
        :page-count="totalPages"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="$emit('page-change', $event)"
      />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.post-list {
  // ── Empty ──
  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: var(--th-spacing-3xl) var(--th-spacing-md);
    text-align: center;
  }

  &__empty-icon {
    color: var(--el-text-color-placeholder);
    margin-bottom: var(--th-spacing-md);
  }

  &__empty-text {
    margin: 0;
    font-size: 16px;
    font-weight: 500;
    color: var(--el-text-color-secondary);
    margin-bottom: var(--th-spacing-xs);
  }

  &__empty-hint {
    margin: 0;
    font-size: 14px;
    color: var(--el-text-color-placeholder);
  }

  // ── Pagination ──
  &__pagination {
    display: flex;
    justify-content: center;
    padding: var(--th-spacing-lg) var(--th-spacing-md);
  }
}
</style>
