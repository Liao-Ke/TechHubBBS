<script setup lang="ts">
import type { PostVO } from '@/api/types/post'
import { visibilityLabel, visibilityIcon } from '@/composables/useVisibility'
import { formatRelativeTime } from '@/utils/format'
import { View, Star, ChatDotRound, Medal } from '@element-plus/icons-vue'
import UserAvatar from '@/components/common/UserAvatar.vue'

defineProps<{
  post: PostVO | null
}>()

/** Strip basic markdown syntax for plain-text display */
function stripMarkdown(md: string): string {
  return md
    .replace(/[#*`~>_[\]()!|-]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}
</script>

<template>
  <!-- Loading skeleton -->
  <article v-if="post === null" class="post-card post-card--skeleton" aria-busy="true">
    <div class="post-card__skeleton-header">
      <div class="post-card__skeleton-avatar skeleton-shimmer" />
      <div class="post-card__skeleton-line skeleton-shimmer" style="width: 30%" />
    </div>
    <div class="post-card__skeleton-line skeleton-shimmer" style="width: 70%" />
    <div class="post-card__skeleton-line skeleton-shimmer" style="width: 50%" />
    <div class="post-card__skeleton-footer">
      <div class="post-card__skeleton-line skeleton-shimmer" style="width: 20%" />
      <div class="post-card__skeleton-line skeleton-shimmer" style="width: 15%" />
    </div>
  </article>

  <!-- Content card -->
  <article
    v-else
    class="post-card"
    :class="{
      'post-card--pinned': post.type === 2,
      'post-card--featured': post.type === 1,
    }"
  >
    <!-- First row: avatar + username + time + badges + visibility -->
    <header class="post-card__header">
      <div class="post-card__author">
        <UserAvatar
          :src="post.author.avatarUrl"
          :size="32"
          class="post-card__avatar"
        />
        <span class="post-card__username">{{ post.author.username }}</span>
        <span class="post-card__time">{{ formatRelativeTime(post.createTime) }}</span>
      </div>
      <div class="post-card__badges">
        <span v-if="post.type === 2" class="post-card__badge post-card__badge--pinned">
          <el-tag type="danger" size="small" effect="dark">置顶</el-tag>
        </span>
        <span v-if="post.type === 1" class="post-card__badge post-card__badge--featured">
          <el-tag color="var(--th-color-elite)" size="small" effect="dark">精华</el-tag>
        </span>
        <span class="post-card__visibility" :title="visibilityLabel(post.visibility)">
          <el-icon :size="16"><component :is="visibilityIcon(post.visibility)" /></el-icon>
        </span>
      </div>
    </header>

    <!-- Title -->
    <h3 class="post-card__title">
      <router-link :to="`/posts/${post.id}`" class="post-card__title-link">
        {{ post.title }}
      </router-link>
    </h3>

    <!-- Summary (optional) -->
    <p v-if="post.summary" class="post-card__summary">{{ stripMarkdown(post.summary) }}</p>

    <!-- Footer: category + stats -->
    <footer class="post-card__footer">
      <router-link
        :to="`/categories/${post.categoryId}`"
        class="post-card__category"
      >
        {{ post.categoryName }}
      </router-link>

      <div class="post-card__stats">
        <span class="post-card__stat">
          <el-icon :size="14"><View /></el-icon>
          {{ post.viewCount }}
        </span>
        <span class="post-card__stat">
          <el-icon :size="14"><Star /></el-icon>
          {{ post.likeCount }}
        </span>
        <span class="post-card__stat">
          <el-icon :size="14"><ChatDotRound /></el-icon>
          {{ post.commentCount }}
        </span>
        <span v-if="post.divineCommentCount > 0" class="post-card__stat post-card__stat--divine">
          <el-icon :size="14"><Medal /></el-icon>
          {{ post.divineCommentCount }}
        </span>
      </div>
    </footer>
  </article>
</template>

<style lang="scss" scoped>
.post-card {
  position: relative;
  padding: var(--th-spacing-md);
  border-bottom: 1px solid var(--el-border-color-light);
  transition: background-color var(--th-transition-fast);
  overflow: hidden;

  &:hover {
    background-color: var(--el-fill-color-light);
  }

  // ── Pinned: red left border ──
  &--pinned::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    background: var(--th-color-pinned);
    border-radius: 0 var(--th-radius-sm) var(--th-radius-sm) 0;
  }

  // ── Featured: purple title ──
  &--featured &__title-link {
    color: var(--th-color-elite);
  }

  // ── Header row ──
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--th-spacing-sm);
    margin-bottom: var(--th-spacing-sm);
  }

  &__author {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-sm);
    min-width: 0;
    flex: 1;
  }

  &__avatar {
    flex-shrink: 0;
  }

  &__username {
    font-size: 14px;
    font-weight: 500;
    color: var(--el-text-color-primary);
    white-space: nowrap;
  }

  &__time {
    font-size: 13px;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
  }

  &__badges {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-xs);
    flex-shrink: 0;
  }

  &__visibility {
    color: var(--el-text-color-secondary);
    margin-left: 2px;
    display: flex;
    align-items: center;
  }

  // ── Title ──
  &__title {
    margin: 0 0 var(--th-spacing-xs);
    font-size: 18px;
    font-weight: 600;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__title-link {
    color: var(--el-text-color-primary);
    text-decoration: none;
    transition: color var(--th-transition-fast);

    &:hover {
      color: var(--el-color-primary);
    }
  }

  // ── Summary ──
  &__summary {
    margin: 0 0 var(--th-spacing-sm);
    font-size: 14px;
    font-weight: 400;
    line-height: 1.5;
    color: var(--el-text-color-secondary);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  // ── Footer ──
  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--th-spacing-sm);
  }

  &__category {
    font-size: 13px;
    color: var(--el-color-primary);
    text-decoration: none;
    padding: 2px var(--th-spacing-sm);
    background: var(--th-color-ai-panel-bg);
    border-radius: var(--th-radius-sm);
    transition: background-color var(--th-transition-fast);
    white-space: nowrap;

    &:hover {
      background: var(--th-color-ai-panel-border);
    }
  }

  &__stats {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-md);
    flex-shrink: 0;
  }

  &__stat {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    font-size: 13px;
    color: var(--el-text-color-secondary);

    &--divine {
      color: var(--th-color-divine);
    }
  }

  // ── Skeleton ──
  &--skeleton {
    cursor: default;
    pointer-events: none;
  }

  &__skeleton-header {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-sm);
    margin-bottom: var(--th-spacing-md);
  }

  &__skeleton-avatar {
    width: 32px;
    height: 32px;
    border-radius: var(--th-radius-full);
    flex-shrink: 0;
  }

  &__skeleton-line {
    height: 14px;
    border-radius: var(--th-radius-sm);
    background: var(--el-fill-color);
  }

  &__skeleton-footer {
    display: flex;
    gap: var(--th-spacing-md);
    margin-top: var(--th-spacing-sm);
  }
}

// ── Skeleton shimmer animation ──
.skeleton-shimmer {
  background: var(--el-fill-color);
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
