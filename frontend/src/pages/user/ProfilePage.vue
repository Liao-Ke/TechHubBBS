<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/modules/user'
import { postApi } from '@/api/modules/post'
import type { UserProfileVO, PostVO, FollowStatusVO } from '@/api/types'
import UserAvatar from '@/components/common/UserAvatar.vue'
import PostList from '@/components/post/PostList.vue'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import {
  UserFilled,
  Plus,
  Check,
  Document,
  Star,
  Connection,
  User,
} from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()

const PAGE_SIZE = 10

// ── User data ──
const user = ref<UserProfileVO | null>(null)
const loadingUser = ref(true)
const userError = ref<Error | null>(null)

const userId = computed(() => route.params.id as string)
const isOwnProfile = computed(() => {
  return userStore.isLoggedIn && userStore.userInfo?.id === userId.value
})

// ── Follow state ──
const isFollowing = ref(false)
const followLoading = ref(false)

// ── Tabs ──
type TabName = 'posts' | 'favorites' | 'following' | 'followers'
const activeTab = ref<TabName>('posts')

// ── Posts tab ──
const posts = ref<PostVO[]>([])
const postsLoading = ref(false)
const postsPage = ref(1)
const postsTotal = ref(0)
const postsTotalPages = ref(0)
const postsError = ref<Error | null>(null)

// ── Favorites tab ──
const favPosts = ref<PostVO[]>([])
const favLoading = ref(false)
const favPage = ref(1)
const favTotal = ref(0)
const favTotalPages = ref(0)
const favError = ref<Error | null>(null)

// ── Following tab ──
const followingUsers = ref<UserProfileVO[]>([])
const followingLoading = ref(false)
const followingPage = ref(1)
const followingTotal = ref(0)
const followingTotalPages = ref(0)
const followingError = ref<Error | null>(null)

// ── Followers tab ──
const followerUsers = ref<UserProfileVO[]>([])
const followersLoading = ref(false)
const followersPage = ref(1)
const followersTotal = ref(0)
const followersTotalPages = ref(0)
const followersError = ref<Error | null>(null)

// ── Stats (from API response or fallback to computed counts) ──
const postCount = computed(() => user.value?.postCount ?? 0)
const followerCount = computed(() => user.value?.followerCount ?? 0)
const followingCount = computed(() => user.value?.followingCount ?? 0)

// ── Load user profile ──
async function loadUser() {
  loadingUser.value = true
  userError.value = null
  try {
    const res = await userApi.getById(userId.value)
    user.value = res.data
    // Load initial tab
    await nextTick()
    loadActiveTab()
  } catch (e: any) {
    userError.value = e
  } finally {
    loadingUser.value = false
  }
}

// ── Check follow status ──
async function checkFollowStatus() {
  if (!userStore.isLoggedIn || isOwnProfile.value) return
  try {
    const res = await userApi.checkFollow(userId.value)
    isFollowing.value = res.data?.following ?? false
  } catch {
    // Silently fail — button will show default state
  }
}

// ── Toggle follow ──
async function toggleFollow() {
  if (followLoading.value) return
  followLoading.value = true
  try {
    if (isFollowing.value) {
      const res = await userApi.unfollow(userId.value)
      isFollowing.value = res.data?.following ?? false
    } else {
      const res = await userApi.follow(userId.value)
      isFollowing.value = res.data?.following ?? true
    }
  } catch {
    // Keep current state on error
  } finally {
    followLoading.value = false
  }
}

// ── Tab content loaders ──
async function loadPosts() {
  if (postsLoading.value) return
  postsError.value = null
  postsLoading.value = true
  try {
    const res = await userApi.getUserPosts(userId.value, {
      page: postsPage.value,
      size: PAGE_SIZE,
    })
    const data = res.data
    if (data) {
      posts.value = data.records
      postsTotal.value = data.total
      postsTotalPages.value = data.pages
    }
  } catch (e: any) {
    postsError.value = e
  } finally {
    postsLoading.value = false
  }
}

async function loadFavorites() {
  favError.value = null
  favLoading.value = true
  try {
    const res = await postApi.getList({
      userId: userId.value,
      page: favPage.value,
      size: PAGE_SIZE,
    })
    const data = res.data
    if (data) {
      favPosts.value = data.records
      favTotal.value = data.total
      favTotalPages.value = data.pages
    }
  } catch (e: any) {
    favError.value = e
  } finally {
    favLoading.value = false
  }
}

async function loadFollowing() {
  followingError.value = null
  followingLoading.value = true
  try {
    // Placeholder: API endpoint pending. Show empty state.
    followingUsers.value = []
    followingTotal.value = 0
    followingTotalPages.value = 0
  } catch (e: any) {
    followingError.value = e
  } finally {
    followingLoading.value = false
  }
}

async function loadFollowers() {
  followersError.value = null
  followersLoading.value = true
  try {
    // Placeholder: API endpoint pending. Show empty state.
    followerUsers.value = []
    followersTotal.value = 0
    followersTotalPages.value = 0
  } catch (e: any) {
    followersError.value = e
  } finally {
    followersLoading.value = false
  }
}

function loadActiveTab() {
  switch (activeTab.value) {
    case 'posts':
      loadPosts()
      break
    case 'favorites':
      loadFavorites()
      break
    case 'following':
      loadFollowing()
      break
    case 'followers':
      loadFollowers()
      break
  }
}

// ── Pagination ──
async function onPageChange(tab: TabName, page: number) {
  switch (tab) {
    case 'posts':
      postsPage.value = page
      await loadPosts()
      break
    case 'favorites':
      favPage.value = page
      await loadFavorites()
      break
    case 'following':
      followingPage.value = page
      await loadFollowing()
      break
    case 'followers':
      followersPage.value = page
      await loadFollowers()
      break
  }
}

// ── Tab change ──
watch(activeTab, () => {
  loadActiveTab()
})

// ── Watch route param changes (navigating between profiles) ──
watch(userId, () => {
  user.value = null
  posts.value = []
  favPosts.value = []
  followingUsers.value = []
  followerUsers.value = []
  postsPage.value = 1
  favPage.value = 1
  followingPage.value = 1
  followersPage.value = 1
  activeTab.value = 'posts'
  loadUser()
  checkFollowStatus()
})

onMounted(async () => {
  await loadUser()
  await checkFollowStatus()
})
</script>

<template>
  <div class="profile-page">
    <!-- ── Loading: full page skeleton ── -->
    <LoadingSkeleton v-if="loadingUser" variant="detail" />

    <!-- ── Error state ── -->
    <div v-else-if="userError" class="profile-page__error">
      <el-alert
        type="error"
        title="加载用户信息失败"
        :description="userError.message || '请稍后重试'"
        show-icon
        :closable="false"
      />
      <el-button type="primary" size="small" class="profile-page__retry-btn" @click="loadUser()">
        重试
      </el-button>
    </div>

    <!-- ── Not found ── -->
    <EmptyState
      v-else-if="!user"
      :icon="UserFilled"
      title="用户不存在"
      description="该用户可能已被删除或不存在"
    />

    <!-- ── Profile content ── -->
    <template v-else>
      <!-- ======== Header ======== -->
      <div class="profile-page__header">
        <div class="profile-page__header-inner">
          <UserAvatar
            :src="user.avatarUrl"
            :size="96"
            class="profile-page__avatar"
          />
          <h1 class="profile-page__username">{{ user.username }}</h1>
          <p v-if="user.bio" class="profile-page__bio">{{ user.bio }}</p>

          <!-- Stats -->
          <div class="profile-page__stats">
            <div class="profile-page__stat">
              <span class="profile-page__stat-value">{{ postCount }}</span>
              <span class="profile-page__stat-label">帖子</span>
            </div>
            <div class="profile-page__stat">
              <span class="profile-page__stat-value">{{ followingCount }}</span>
              <span class="profile-page__stat-label">关注</span>
            </div>
            <div class="profile-page__stat">
              <span class="profile-page__stat-value">{{ followerCount }}</span>
              <span class="profile-page__stat-label">粉丝</span>
            </div>
          </div>

          <!-- Actions -->
          <div class="profile-page__actions">
            <router-link v-if="isOwnProfile" to="/settings">
              <el-button type="primary" plain size="default">
                编辑资料
              </el-button>
            </router-link>
            <el-button
              v-else-if="userStore.isLoggedIn"
              :type="isFollowing ? 'default' : 'primary'"
              :loading="followLoading"
              size="default"
              @click="toggleFollow"
            >
              <el-icon v-if="isFollowing" :size="14" class="profile-page__follow-icon">
                <Check />
              </el-icon>
              <el-icon v-else :size="14" class="profile-page__follow-icon">
                <Plus />
              </el-icon>
              {{ isFollowing ? '已关注' : '关注' }}
            </el-button>
          </div>
        </div>
      </div>

      <!-- ======== Tabs ======== -->
      <el-tabs v-model="activeTab" class="profile-page__tabs">
        <!-- 帖子 -->
        <el-tab-pane name="posts">
          <template #label>
            <span class="profile-page__tab-label">
              <el-icon :size="14"><Document /></el-icon>
              帖子
            </span>
          </template>

          <LoadingSkeleton v-if="postsLoading && posts.length === 0" variant="post-list" />
          <div v-else-if="postsError" class="profile-page__error">
            <el-alert type="error" title="加载帖子失败" :closable="false" show-icon />
            <el-button type="primary" size="small" class="profile-page__retry-btn" @click="loadPosts()">重试</el-button>
          </div>
          <EmptyState v-else-if="!postsLoading && posts.length === 0" title="暂无帖子" description="该用户还没有发布过帖子" />
          <PostList
            v-else
            :posts="posts"
            :loading="postsLoading"
            :current-page="postsPage"
            :total-pages="postsTotalPages"
            :total="postsTotal"
            @page-change="(p: number) => onPageChange('posts', p)"
          />
        </el-tab-pane>

        <!-- 收藏 -->
        <el-tab-pane name="favorites">
          <template #label>
            <span class="profile-page__tab-label">
              <el-icon :size="14"><Star /></el-icon>
              收藏
            </span>
          </template>

          <LoadingSkeleton v-if="favLoading && favPosts.length === 0" variant="post-list" />
          <div v-else-if="favError" class="profile-page__error">
            <el-alert type="error" title="加载收藏失败" :closable="false" show-icon />
            <el-button type="primary" size="small" class="profile-page__retry-btn" @click="loadFavorites()">重试</el-button>
          </div>
          <EmptyState v-else-if="!favLoading && favPosts.length === 0" title="暂无收藏" description="该用户还没有收藏过帖子" />
          <PostList
            v-else
            :posts="favPosts"
            :loading="favLoading"
            :current-page="favPage"
            :total-pages="favTotalPages"
            :total="favTotal"
            @page-change="(p: number) => onPageChange('favorites', p)"
          />
        </el-tab-pane>

        <!-- 关注 -->
        <el-tab-pane name="following">
          <template #label>
            <span class="profile-page__tab-label">
              <el-icon :size="14"><Connection /></el-icon>
              关注
            </span>
          </template>

          <LoadingSkeleton v-if="followingLoading" variant="detail" />
          <div v-else-if="followingError" class="profile-page__error">
            <el-alert type="error" title="加载关注列表失败" :closable="false" show-icon />
            <el-button type="primary" size="small" class="profile-page__retry-btn" @click="loadFollowing()">重试</el-button>
          </div>
          <EmptyState v-else-if="!followingLoading && followingUsers.length === 0" title="暂无关注" description="该用户还没有关注其他人" />
          <div v-else class="profile-page__user-list">
            <div
              v-for="fu in followingUsers"
              :key="fu.id"
              class="profile-page__user-row"
            >
              <UserAvatar :src="fu.avatarUrl" :size="40" />
              <span class="profile-page__user-row-name">{{ fu.username }}</span>
            </div>
          </div>
        </el-tab-pane>

        <!-- 粉丝 -->
        <el-tab-pane name="followers">
          <template #label>
            <span class="profile-page__tab-label">
              <el-icon :size="14"><User /></el-icon>
              粉丝
            </span>
          </template>

          <LoadingSkeleton v-if="followersLoading" variant="detail" />
          <div v-else-if="followersError" class="profile-page__error">
            <el-alert type="error" title="加载粉丝列表失败" :closable="false" show-icon />
            <el-button type="primary" size="small" class="profile-page__retry-btn" @click="loadFollowers()">重试</el-button>
          </div>
          <EmptyState v-else-if="!followersLoading && followerUsers.length === 0" title="暂无粉丝" description="该用户还没有粉丝" />
          <div v-else class="profile-page__user-list">
            <div
              v-for="fr in followerUsers"
              :key="fr.id"
              class="profile-page__user-row"
            >
              <UserAvatar :src="fr.avatarUrl" :size="40" />
              <span class="profile-page__user-row-name">{{ fr.username }}</span>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.profile-page {
  max-width: var(--th-content-max-width);
  margin: 0 auto;
  padding-top: var(--th-spacing-6);

  // ── Header ──
  &__header {
    padding-bottom: var(--th-spacing-6);
    border-bottom: 1px solid var(--el-border-color-light);
    margin-bottom: var(--th-spacing-2);
  }

  &__header-inner {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  &__avatar {
    margin-bottom: var(--th-spacing-4);
  }

  &__username {
    margin: 0;
    font-size: 20px;
    font-weight: 700;
    color: var(--el-text-color-primary);
    line-height: 1.3;
  }

  &__bio {
    margin: var(--th-spacing-2) 0 0;
    font-size: 14px;
    color: var(--el-text-color-secondary);
    line-height: 1.6;
    max-width: 480px;
    word-break: break-word;
  }

  // ── Stats ──
  &__stats {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--th-spacing-8);
    margin-top: var(--th-spacing-5);
  }

  &__stat {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;

    &-value {
      font-size: 18px;
      font-weight: 700;
      color: var(--el-text-color-primary);
      line-height: 1.3;
    }

    &-label {
      font-size: 13px;
      color: var(--el-text-color-secondary);
    }
  }

  // ── Actions ──
  &__actions {
    margin-top: var(--th-spacing-5);
    display: flex;
    justify-content: center;
  }

  &__follow-icon {
    margin-right: 2px;
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

  // ── Error / retry ──
  &__error {
    padding: var(--th-spacing-4) 0;
    text-align: center;
  }

  &__retry-btn {
    margin-top: var(--th-spacing-3);
  }

  // ── User list (following / followers) ──
  &__user-list {
    display: flex;
    flex-direction: column;
    gap: var(--th-spacing-1);
  }

  &__user-row {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-3);
    padding: var(--th-spacing-3) var(--th-spacing-4);
    border-radius: var(--th-radius-md);
    transition: background-color var(--th-transition-fast);

    &:hover {
      background-color: var(--el-fill-color-light);
    }

    &-name {
      font-size: 14px;
      font-weight: 500;
      color: var(--el-text-color-primary);
    }
  }
}
</style>
