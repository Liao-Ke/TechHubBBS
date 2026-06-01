<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { adminApi } from '@/api/modules/admin'
import { useUserStore } from '@/stores/user'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { UserProfileVO } from '@/api/types'

// ── Extended type — admin endpoint may return email ──
interface AdminUserVO extends UserProfileVO {
  email?: string
}

const userStore = useUserStore()

// ── Search ──
const keyword = ref('')
const DEBOUNCE_MS = 500
let debounceTimer: ReturnType<typeof setTimeout> | null = null

// ── Table ──
const users = ref<AdminUserVO[]>([])
const loading = ref(true)
const error = ref(false)
const errorMsg = ref('')
const page = ref(1)
const size = 20
const total = ref(0)
const totalPages = ref(0)

// ── Role config ──
const roleOptions = [
  { label: '普通用户', value: 'USER' },
  { label: '版主', value: 'MODERATOR' },
  { label: '管理员', value: 'ADMIN' },
] as const

const ROLE_TAG_MAP: Record<string, 'danger' | 'warning' | 'info'> = {
  ADMIN: 'danger',
  MODERATOR: 'warning',
  USER: 'info',
}

const ROLE_LABEL_MAP: Record<string, string> = {
  ADMIN: '管理员',
  MODERATOR: '版主',
  USER: '普通用户',
}

// ── Fetch ──
async function fetchUsers() {
  loading.value = true
  error.value = false
  try {
    const res = await adminApi.getUsers({
      page: page.value,
      size,
      keyword: keyword.value || undefined,
    })
    if (res.data) {
      users.value = res.data.records as AdminUserVO[]
      total.value = res.data.total
      totalPages.value = res.data.pages
    }
  } catch (e: unknown) {
    error.value = true
    errorMsg.value = (e as { message?: string })?.message || '加载用户列表失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// ── Search debounce ──
function onSearchInput() {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    page.value = 1
    fetchUsers()
  }, DEBOUNCE_MS)
}

// ── Pagination ──
function onPageChange(p: number) {
  page.value = p
  fetchUsers()
}

// ── Helpers ──
function isSelf(row: AdminUserVO): boolean {
  return userStore.userInfo?.id === row.id
}

function roleTagType(role: string): 'danger' | 'warning' | 'info' | '' {
  return ROLE_TAG_MAP[role] || 'info'
}

function roleLabel(role: string): string {
  return ROLE_LABEL_MAP[role] || role
}

function statusLabel(status: number): string {
  return status === 0 ? '封禁' : '正常'
}

function statusTagType(status: number): 'danger' | 'success' {
  return status === 0 ? 'danger' : 'success'
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function truncateId(id: string): string {
  if (!id) return '-'
  return id.length > 12 ? id.slice(0, 12) + '…' : id
}

// ── Actions ──
async function handleBan(row: AdminUserVO) {
  const isBanned = row.status === 0
  const action = isBanned ? '解封' : '封禁'
  const icon = isBanned ? undefined : 'warning' as const
  try {
    await ElMessageBox.confirm(
      `确定要${action}用户「${row.username}」吗？`,
      `${action}确认`,
      { confirmButtonText: action, cancelButtonText: '取消', type: icon || 'warning' },
    )
    await adminApi.banUser(row.id, !isBanned)
    row.status = isBanned ? 1 : 0
    ElMessage.success(`${action}成功`)
  } catch {
    // cancelled or error
  }
}

async function handleRoleChange(row: AdminUserVO, newRole: string) {
  if (newRole === row.role) return
  try {
    await ElMessageBox.confirm(
      `确定要将「${row.username}」的角色修改为「${roleLabel(newRole)}」吗？`,
      '修改角色确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
    )
    await adminApi.setRole(row.id, newRole)
    row.role = newRole
    ElMessage.success('角色修改成功')
  } catch {
    // cancelled or error
  }
}

// ── Lifecycle ──
onMounted(() => {
  fetchUsers()
})

onUnmounted(() => {
  if (debounceTimer) clearTimeout(debounceTimer)
})
</script>

<template>
  <div class="user-manage-page">
    <h1 class="user-manage-page__title">用户管理</h1>

    <!-- ══════════════════════════════════════════ -->
    <!-- Search Bar                                 -->
    <!-- ══════════════════════════════════════════ -->
    <div class="user-manage-page__toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索用户名或邮箱…"
        :prefix-icon="Search"
        clearable
        class="user-manage-page__search"
        @input="onSearchInput"
        @clear="onSearchInput"
      />
    </div>

    <!-- ══════════════════════════════════════════ -->
    <!-- Error                                      -->
    <!-- ══════════════════════════════════════════ -->
    <el-alert
      v-if="error"
      type="error"
      title="加载失败"
      :description="errorMsg"
      show-icon
      :closable="false"
      class="user-manage-page__alert"
    >
      <template #default>
        <el-button size="small" text type="primary" @click="fetchUsers">
          重试
        </el-button>
      </template>
    </el-alert>

    <!-- ══════════════════════════════════════════ -->
    <!-- Loading Skeleton                           -->
    <!-- ══════════════════════════════════════════ -->
    <LoadingSkeleton v-if="loading" variant="table" />

    <!-- ══════════════════════════════════════════ -->
    <!-- Empty State                                -->
    <!-- ══════════════════════════════════════════ -->
    <EmptyState
      v-else-if="!loading && users.length === 0"
      title="暂无用户"
      description="没有找到匹配的用户记录"
    />

    <!-- ══════════════════════════════════════════ -->
    <!-- User Table                                 -->
    <!-- ══════════════════════════════════════════ -->
    <template v-else>
      <el-table
        :data="users"
        stripe
        class="user-manage-page__table"
        table-layout="auto"
      >
        <!-- ID -->
        <el-table-column label="ID" width="140">
          <template #default="{ row }">
            <span class="user-manage-page__cell-mono" :title="row.id">
              {{ truncateId(row.id) }}
            </span>
          </template>
        </el-table-column>

        <!-- Username -->
        <el-table-column label="用户名" min-width="120">
          <template #default="{ row }">
            <span class="user-manage-page__cell-username">{{ row.username }}</span>
          </template>
        </el-table-column>

        <!-- Email -->
        <el-table-column label="邮箱" min-width="160">
          <template #default="{ row }">
            <span class="user-manage-page__cell-email">
              {{ row.email || '—' }}
            </span>
          </template>
        </el-table-column>

        <!-- Role -->
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag
              :type="roleTagType(row.role)"
              size="small"
              disable-transitions
            >
              {{ roleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- Status -->
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag
              :type="statusTagType(row.status)"
              size="small"
              disable-transitions
            >
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- Create Time -->
        <el-table-column label="注册时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>

        <!-- Actions -->
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="!isSelf(row)">
              <el-button
                :type="row.status === 0 ? 'success' : 'danger'"
                size="small"
                plain
                @click="handleBan(row)"
              >
                {{ row.status === 0 ? '解封' : '封禁' }}
              </el-button>
              <el-select
                :model-value="row.role"
                size="small"
                class="user-manage-page__role-select"
                @change="(val: string) => handleRoleChange(row, val)"
              >
                <el-option
                  v-for="opt in roleOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </template>
            <span v-else class="user-manage-page__self-hint">当前账号</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="user-manage-page__pagination">
        <el-pagination
          :current-page="page"
          :page-count="totalPages"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="onPageChange"
        />
      </div>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.user-manage-page {
  max-width: var(--th-page-max-width);
  margin: 0 auto;
  padding: var(--th-spacing-4) var(--th-spacing-4) var(--th-spacing-12);

  &__title {
    margin: 0 0 var(--th-spacing-6);
    font-size: 22px;
    font-weight: 700;
    font-family: var(--th-font-heading);
    color: var(--el-text-color-primary);
  }

  &__toolbar {
    display: flex;
    align-items: center;
    margin-bottom: var(--th-spacing-4);
  }

  &__search {
    max-width: 360px;
  }

  &__alert {
    margin-bottom: var(--th-spacing-4);
  }

  &__table {
    border-radius: var(--th-radius-md);
    border: 1px solid var(--el-border-color-light);
    overflow: hidden;
  }

  &__cell-mono {
    font-family: var(--th-font-mono);
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  &__cell-username {
    font-weight: 600;
  }

  &__cell-email {
    color: var(--el-text-color-secondary);
    font-size: 13px;
    word-break: break-all;
  }

  &__role-select {
    width: 100px;
    margin-left: var(--th-spacing-2);
  }

  &__self-hint {
    font-size: 13px;
    color: var(--el-text-color-placeholder);
  }

  &__pagination {
    display: flex;
    justify-content: center;
    padding: var(--th-spacing-6) 0 0;
  }
}
</style>
