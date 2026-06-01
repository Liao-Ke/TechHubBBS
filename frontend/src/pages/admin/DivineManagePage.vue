<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Medal, Refresh, View, Link } from '@element-plus/icons-vue'
import { adminApi } from '@/api/modules/admin'
import { formatRelativeTime } from '@/utils/format'
import type { AdminCommentItem, PageResult } from '@/api/types'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'

// ── State ──
const loading = ref(true)
const error = ref(false)
const comments = ref<AdminCommentItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const searchInput = ref('')

// ── Fetch ──
async function fetchComments(page = 1) {
  loading.value = true
  error.value = false
  currentPage.value = page
  try {
    const res = await adminApi.getComments({
      page,
      size: pageSize.value,
      keyword: keyword.value || undefined,
    })
    const data: PageResult<AdminCommentItem> = res.data
    comments.value = data.records ?? []
    total.value = data.total ?? 0
  } catch {
    error.value = true
    comments.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// ── Search ──
function handleSearch() {
  keyword.value = searchInput.value.trim()
  fetchComments(1)
}

function handleReset() {
  searchInput.value = ''
  keyword.value = ''
  fetchComments(1)
}

// ── Pagination ──
function handlePageChange(page: number) {
  fetchComments(page)
}

// ── Divine Actions ──
async function handleSetDivine(comment: AdminCommentItem) {
  try {
    await ElMessageBox.confirm(
      `确定要将这条评论设为神评吗？`,
      '设为神评',
      {
        type: 'info',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }
  try {
    await adminApi.setDivine(comment.id, true)
    ElMessage.success('已设为神评')
    comment.isDivine = true
    comment.divineTime = new Date().toISOString()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleRevokeDivine(comment: AdminCommentItem) {
  try {
    await ElMessageBox.confirm(
      `确定要取消这条评论的神评状态吗？`,
      '取消神评',
      {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }
  try {
    await adminApi.setDivine(comment.id, false)
    ElMessage.success('已取消神评')
    comment.isDivine = false
    comment.divineTime = null
  } catch {
    ElMessage.error('操作失败')
  }
}

// ── Helpers ──
function truncateContent(content: string, maxLen = 80): string {
  if (!content) return ''
  return content.length > maxLen ? content.slice(0, maxLen) + '…' : content
}

function formatDivineTime(time: string | null): string {
  if (!time) return '-'
  try {
    return formatRelativeTime(time)
  } catch {
    return time.slice(0, 10)
  }
}

// ── Init ──
onMounted(() => {
  fetchComments(1)
})
</script>

<template>
  <div class="divine-manage">
    <!-- Header -->
    <div class="divine-manage__header">
      <h2 class="divine-manage__title">
        <el-icon :size="20"><Medal /></el-icon>
        神评管理
      </h2>
      <span class="divine-manage__subtitle">
        强制设置或撤销评论的神评状态
      </span>
    </div>

    <!-- Search Bar -->
    <div class="divine-manage__search">
      <el-input
        v-model="searchInput"
        placeholder="搜索评论内容或帖子标题…"
        :prefix-icon="Search"
        clearable
        class="divine-manage__search-input"
        @keyup.enter="handleSearch"
        @clear="handleReset"
      />
      <el-button type="primary" :icon="Search" @click="handleSearch">
        搜索
      </el-button>
      <el-button :icon="Refresh" @click="handleReset">
        重置
      </el-button>
    </div>

    <!-- Loading -->
    <LoadingSkeleton v-if="loading" variant="table" />

    <!-- Error -->
    <el-alert
      v-else-if="error"
      title="加载失败，请重试"
      type="error"
      show-icon
      :closable="false"
      class="divine-manage__error"
    >
      <template #default>
        <el-button size="small" type="danger" plain @click="fetchComments(currentPage)">
          重试
        </el-button>
      </template>
    </el-alert>

    <!-- Empty -->
    <EmptyState
      v-else-if="!loading && comments.length === 0"
      :icon="Medal"
      title="暂无评论数据"
      description="神评管理功能允许管理员强制设置或撤销评论的神评状态"
    />

    <!-- Table -->
    <template v-else>
      <div class="divine-manage__table-wrap">
        <el-table
          :data="comments"
          stripe
          border
          size="default"
          class="divine-manage__table"
        >
          <el-table-column label="评论内容" min-width="280">
            <template #default="{ row }">
              <div
                :class="[
                  'divine-manage__content',
                  { 'divine-manage__content--divine': row.isDivine },
                ]"
              >
                {{ truncateContent(row.content) }}
                <el-tag
                  v-if="row.isDivine"
                  type="warning"
                  effect="dark"
                  size="small"
                  class="divine-manage__divine-tag"
                >
                  <el-icon :size="13"><Medal /></el-icon>
                  神评
                </el-tag>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="作者" width="120">
            <template #default="{ row }">
              <span class="divine-manage__author">{{ row.username }}</span>
            </template>
          </el-table-column>

          <el-table-column label="所属帖子" min-width="160">
            <template #default="{ row }">
              <router-link
                v-if="row.postId"
                :to="`/posts/${row.postId}`"
                class="divine-manage__post-link"
              >
                <el-icon :size="14"><Link /></el-icon>
                {{ row.postTitle || '查看帖子' }}
              </router-link>
              <span v-else class="divine-manage__post-link--na">-</span>
            </template>
          </el-table-column>

          <el-table-column label="点赞" width="80" align="center">
            <template #default="{ row }">
              <span class="divine-manage__stat">{{ row.likeCount }}</span>
            </template>
          </el-table-column>

          <el-table-column label="推荐" width="80" align="center">
            <template #default="{ row }">
              <span class="divine-manage__stat">{{ row.recommendCount }}</span>
            </template>
          </el-table-column>

          <el-table-column label="神评时间" width="150">
            <template #default="{ row }">
              <span
                :class="[
                  'divine-manage__divine-time',
                  { 'divine-manage__divine-time--active': row.divineTime },
                ]"
              >
                {{ formatDivineTime(row.divineTime) }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <div class="divine-manage__actions">
                <el-button
                  v-if="!row.isDivine"
                  type="warning"
                  size="small"
                  :icon="Medal"
                  @click="handleSetDivine(row)"
                >
                  设为神评
                </el-button>
                <el-button
                  v-else
                  type="info"
                  size="small"
                  plain
                  @click="handleRevokeDivine(row)"
                >
                  撤销神评
                </el-button>
                <el-button
                  size="small"
                  :icon="View"
                  circle
                  title="查看帖子"
                  @click="$router.push(`/posts/${row.postId}`)"
                />
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Pagination -->
      <div v-if="total > pageSize" class="divine-manage__pagination">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="pageSize"
          :current-page="currentPage"
          @current-change="handlePageChange"
        />
      </div>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.divine-manage {
  max-width: 1200px;

  // ── Header ──
  &__header {
    display: flex;
    align-items: baseline;
    gap: var(--th-spacing-sm);
    margin-bottom: var(--th-spacing-lg);
  }

  &__title {
    margin: 0;
    display: inline-flex;
    align-items: center;
    gap: var(--th-spacing-xs);
    font-size: 20px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  &__subtitle {
    font-size: 14px;
    color: var(--el-text-color-secondary);
  }

  // ── Search ──
  &__search {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-sm);
    margin-bottom: var(--th-spacing-lg);
  }

  &__search-input {
    max-width: 360px;
  }

  // ── Error ──
  &__error {
    margin-bottom: var(--th-spacing-lg);
  }

  // ── Table ──
  &__table-wrap {
    margin-bottom: var(--th-spacing-md);
  }

  &__table {
    width: 100%;
  }

  // ── Content cell ──
  &__content {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-sm);
    font-size: 14px;
    line-height: 1.5;
    color: var(--el-text-color-regular);

    &--divine {
      font-weight: 500;
    }
  }

  &__divine-tag {
    flex-shrink: 0;
  }

  // ── Author ──
  &__author {
    font-size: 14px;
    font-weight: 500;
    color: var(--el-text-color-primary);
  }

  // ── Post link ──
  &__post-link {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 14px;
    color: var(--el-color-primary);
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }

    &--na {
      color: var(--el-text-color-placeholder);
    }
  }

  // ── Stats ──
  &__stat {
    font-size: 14px;
    font-weight: 500;
    color: var(--el-text-color-regular);
  }

  // ── Divine time ──
  &__divine-time {
    font-size: 13px;
    color: var(--el-text-color-secondary);

    &--active {
      color: var(--th-color-divine);
      font-weight: 500;
    }
  }

  // ── Actions ──
  &__actions {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-xs);
  }

  // ── Pagination ──
  &__pagination {
    display: flex;
    justify-content: center;
    margin-top: var(--th-spacing-lg);
  }
}
</style>
