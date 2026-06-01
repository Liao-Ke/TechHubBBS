<script setup lang="ts">
import { ref, watch, onMounted, nextTick } from 'vue'
import { useUserStore } from '@/stores/user'
import { postApi } from '@/api/modules/post'
import { recommendationApi } from '@/api/modules/recommendation'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import type { RecommendationVO, PostVO } from '@/api/types'
import PostCard from '@/components/post/PostCard.vue'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { Loading, TrendCharts, UserFilled } from '@element-plus/icons-vue'

const userStore = useUserStore()

// ── Tab state ──
const activeTab = ref<'recommend' | 'hot'>('recommend')
const PAGE_SIZE = 10

// ── Recommendation tab ──
const recPosts = ref<RecommendationVO[]>([])
const recPage = ref(1)

async function loadRecFn(): Promise<void> {
  const res = await recommendationApi.getRecommendations({
    page: recPage.value,
    size: PAGE_SIZE,
  })
  const data = res.data
  if (!data || data.length === 0) {
    recHasMore.value = false
    return
  }
  recPosts.value.push(...data)
  recPage.value++
}

const {
  sentinelRef: recSentinel,
  loading: recLoading,
  hasMore: recHasMore,
  error: recError,
  loadMore: recLoadMore,
  reset: recReset,
} = useInfiniteScroll(loadRecFn)

// ── Hot tab ──
const hotPosts = ref<PostVO[]>([])
const hotPage = ref(1)

async function loadHotFn(): Promise<void> {
  const res = await postApi.getList({
    sort: 'hot',
    page: hotPage.value,
    size: PAGE_SIZE,
  })
  const pageResult = res.data
  if (!pageResult || pageResult.records.length === 0) {
    hotHasMore.value = false
    return
  }
  hotPosts.value.push(...pageResult.records)
  hotPage.value++
  if (hotPage.value > pageResult.pages) {
    hotHasMore.value = false
  }
}

const {
  sentinelRef: hotSentinel,
  loading: hotLoading,
  hasMore: hotHasMore,
  error: hotError,
  loadMore: hotLoadMore,
  reset: hotReset,
} = useInfiniteScroll(loadHotFn)

// ── Tab change ──
watch(activeTab, async (tab) => {
  if (tab === 'recommend') {
    recReset()
    recPosts.value = []
    recPage.value = 1
    await nextTick()
    recLoadMore()
  } else {
    hotReset()
    hotPosts.value = []
    hotPage.value = 1
    await nextTick()
    hotLoadMore()
  }
})

// ── Lifecycle ──
onMounted(async () => {
  await nextTick()
  recLoadMore()
})

// ── Helpers ──
function reasonLabel(reason: string): string {
  const map: Record<string, string> = {
    content_based: '内容推荐',
    collaborative: '猜你喜欢',
    hot: '热门推荐',
  }
  return map[reason] || reason
}

function reasonTheme(reason: string): '' | 'primary' | 'success' | 'warning' | 'danger' {
  const map: Record<string, '' | 'primary' | 'success' | 'warning' | 'danger'> = {
    content_based: 'primary',
    collaborative: 'success',
    hot: 'warning',
  }
  return map[reason] || ''
}
</script>

<template>
  <div class="home-page">
    <!-- Header -->
    <div class="home-page__hero">
      <h1 class="home-page__greeting">
        探索 TechHub
      </h1>
      <p class="home-page__subtitle">
        发现技术社区的精彩内容
      </p>
    </div>

    <!-- Tabs -->
    <el-tabs
      v-model="activeTab"
      class="home-page__tabs"
    >
      <!-- ======== 推荐 Tab ======== -->
      <el-tab-pane name="recommend">
        <template #label>
          <span class="home-page__tab-label">
            <el-icon :size="14"><TrendCharts /></el-icon>
            推荐
          </span>
        </template>

        <!-- Not logged in -->
        <EmptyState
          v-if="!userStore.isLoggedIn"
          :icon="UserFilled"
          title="登录解锁个性化推荐"
          description="登录后我们将根据您的阅读偏好，为您智能推荐优质技术内容"
          action-text="立即登录"
          action-route="/login?redirect=/"
        />

        <!-- Logged in: loading (first load) -->
        <LoadingSkeleton
          v-else-if="recLoading && recPosts.length === 0"
          variant="post-list"
        />

        <!-- Logged in: error (first load) -->
        <div
          v-else-if="recError && recPosts.length === 0"
          class="home-page__error-block"
        >
          <el-alert
            type="error"
            title="加载推荐失败"
            :description="recError.message"
            show-icon
            :closable="false"
          />
          <el-button
            type="primary"
            size="small"
            class="home-page__retry-btn"
            @click="recLoadMore()"
          >
            重试
          </el-button>
        </div>

        <!-- Logged in: empty -->
        <EmptyState
          v-else-if="!recLoading && recPosts.length === 0 && !recError"
          title="还没有推荐内容"
          description="去看看热门帖子，发现更多精彩"
        />

        <!-- Logged in: recommendation list -->
        <div v-else class="home-page__feed">
          <TransitionGroup name="card-stagger">
            <article
              v-for="rec in recPosts"
              :key="rec.id"
              class="rec-card"
            >
              <!-- Header: avatar + username + reason badge -->
              <header class="rec-card__header">
                <div class="rec-card__author">
                  <UserAvatar
                    :src="rec.author.avatarUrl"
                    :size="28"
                    class="rec-card__avatar"
                  />
                  <span class="rec-card__username">{{ rec.author.username }}</span>
                  <el-tag
                    :type="reasonTheme(rec.reason)"
                    size="small"
                    effect="plain"
                    class="rec-card__reason"
                  >
                    {{ reasonLabel(rec.reason) }}
                  </el-tag>
                </div>
              </header>

              <!-- Title -->
              <h3 class="rec-card__title">
                <router-link :to="`/posts/${rec.postId}`" class="rec-card__title-link">
                  {{ rec.title }}
                </router-link>
              </h3>

              <!-- Summary -->
              <p v-if="rec.summary" class="rec-card__summary">{{ rec.summary }}</p>
            </article>
          </TransitionGroup>

          <!-- Sentinel (infinite scroll trigger) -->
          <div ref="recSentinel" class="home-page__sentinel" />

          <!-- Loading more -->
          <div v-if="recLoading && recPosts.length > 0" class="home-page__loading-more">
            <el-icon class="home-page__loading-spin" :size="18"><Loading /></el-icon>
            <span>加载更多推荐...</span>
          </div>

          <!-- Error (after data) -->
          <div v-if="recError && recPosts.length > 0" class="home-page__error-block">
            <el-alert
              type="error"
              title="加载失败"
              :description="recError.message"
              show-icon
              :closable="false"
            />
            <el-button
              type="primary"
              size="small"
              class="home-page__retry-btn"
              @click="recLoadMore()"
            >
              重试
            </el-button>
          </div>

          <!-- End of list -->
          <p
            v-if="!recHasMore && recPosts.length > 0 && !recLoading"
            class="home-page__end"
          >
            已加载全部推荐内容
          </p>
        </div>
      </el-tab-pane>

      <!-- ======== 热门 Tab ======== -->
      <el-tab-pane name="hot">
        <template #label>
          <span class="home-page__tab-label">
            <el-icon :size="14"><TrendCharts /></el-icon>
            热门
          </span>
        </template>

        <!-- Loading (first load) -->
        <LoadingSkeleton
          v-if="hotLoading && hotPosts.length === 0"
          variant="post-list"
        />

        <!-- Error (first load) -->
        <div
          v-else-if="hotError && hotPosts.length === 0"
          class="home-page__error-block"
        >
          <el-alert
            type="error"
            title="加载热门帖子失败"
            :description="hotError.message"
            show-icon
            :closable="false"
          />
          <el-button
            type="primary"
            size="small"
            class="home-page__retry-btn"
            @click="hotLoadMore()"
          >
            重试
          </el-button>
        </div>

        <!-- Empty -->
        <EmptyState
          v-else-if="!hotLoading && hotPosts.length === 0 && !hotError"
          title="暂无热门帖子"
          description="社区还没有足够的内容，快来发布第一篇帖子吧"
        />

        <!-- Hot post list -->
        <div v-else class="home-page__feed">
          <TransitionGroup name="card-stagger">
            <PostCard
              v-for="post in hotPosts"
              :key="post.id"
              :post="post"
            />
          </TransitionGroup>

          <!-- Sentinel -->
          <div ref="hotSentinel" class="home-page__sentinel" />

          <!-- Loading more -->
          <div v-if="hotLoading && hotPosts.length > 0" class="home-page__loading-more">
            <el-icon class="home-page__loading-spin" :size="18"><Loading /></el-icon>
            <span>加载更多帖子...</span>
          </div>

          <!-- Error (after data) -->
          <div v-if="hotError && hotPosts.length > 0" class="home-page__error-block">
            <el-alert
              type="error"
              title="加载失败"
              :description="hotError.message"
              show-icon
              :closable="false"
            />
            <el-button
              type="primary"
              size="small"
              class="home-page__retry-btn"
              @click="hotLoadMore()"
            >
              重试
            </el-button>
          </div>

          <!-- End of list -->
          <p
            v-if="!hotHasMore && hotPosts.length > 0 && !hotLoading"
            class="home-page__end"
          >
            已加载全部热门帖子
          </p>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style lang="scss" scoped>
.home-page {
  max-width: var(--th-content-max-width);
  margin: 0 auto;
  padding-top: var(--th-spacing-6);

  // ── Hero ──
  &__hero {
    margin-bottom: var(--th-spacing-2);
  }

  &__greeting {
    font-size: 28px;
    font-weight: 700;
    color: var(--el-text-color-primary);
    line-height: 1.3;
    margin: 0;
  }

  &__subtitle {
    margin-top: var(--th-spacing-1);
    font-size: 14px;
    color: var(--el-text-color-secondary);
  }

  // ── Tabs ──
  &__tabs {
    :deep(.el-tabs__header) {
      margin-bottom: 0;
      border-bottom: 1px solid var(--el-border-color-light);
    }

    :deep(.el-tabs__nav-wrap::after) {
      display: none;
    }

    :deep(.el-tabs__active-bar) {
      background-color: var(--th-brand-primary);
      height: 2px;
    }

    :deep(.el-tabs__item) {
      font-size: 14px;
      font-weight: 500;
      color: var(--el-text-color-secondary);
      padding: 0 var(--th-spacing-5);
      height: 44px;
      line-height: 44px;
      transition: color var(--th-transition-fast);

      &:hover {
        color: var(--el-text-color-primary);
      }

      &.is-active {
        color: var(--th-brand-primary);
      }
    }

    :deep(.el-tabs__content) {
      padding-top: var(--th-spacing-4);
    }
  }

  &__tab-label {
    display: inline-flex;
    align-items: center;
    gap: 5px;
  }

  // ── Feed ──
  &__feed {
    position: relative;
  }

  // ── Sentinel ──
  &__sentinel {
    height: 1px;
    width: 100%;
  }

  // ── Loading more ──
  &__loading-more {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--th-spacing-2);
    padding: var(--th-spacing-4) 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  &__loading-spin {
    animation: spin 0.8s linear infinite;
    color: var(--th-brand-primary);
  }

  // ── Error ──
  &__error-block {
    padding: var(--th-spacing-4) 0;
    text-align: center;
  }

  &__retry-btn {
    margin-top: var(--th-spacing-3);
  }

  // ── End ──
  &__end {
    text-align: center;
    padding: var(--th-spacing-6) 0;
    font-size: 13px;
    color: var(--el-text-color-placeholder);
    margin: 0;
  }
}

// ── Recommendation Card ──
.rec-card {
  padding: var(--th-spacing-4) 0;
  border-bottom: 1px solid var(--el-border-color-light);
  transition: background-color var(--th-transition-fast);

  &:hover {
    background-color: var(--el-fill-color-light);
  }

  &__header {
    margin-bottom: var(--th-spacing-2);
  }

  &__author {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-2);
    min-width: 0;
  }

  &__avatar {
    flex-shrink: 0;
  }

  &__username {
    font-size: 13px;
    font-weight: 500;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
  }

  &__reason {
    margin-left: auto;
    font-size: 11px;
    flex-shrink: 0;
  }

  &__title {
    margin: 0 0 var(--th-spacing-1);
    font-size: 16px;
    font-weight: 600;
    line-height: 1.45;
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
      color: var(--th-brand-primary);
    }
  }

  &__summary {
    margin: 0;
    font-size: 13px;
    line-height: 1.5;
    color: var(--el-text-color-secondary);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

// ── Card stagger animation ──
.card-stagger-enter-active {
  transition: opacity 0.35s ease, transform 0.35s ease;
}

.card-stagger-leave-active {
  transition: opacity 0.2s ease;
}

.card-stagger-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.card-stagger-leave-to {
  opacity: 0;
}

// ── Spin keyframe ──
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
