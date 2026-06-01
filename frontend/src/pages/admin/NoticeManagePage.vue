<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { adminApi } from '@/api/modules/admin'
import { noticeApi } from '@/api/modules/notice'
import { categoryApi } from '@/api/modules/category'
import type { CategoryNoticeVO, Category } from '@/api/types'
import { formatDate } from '@/utils/format'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import MdEditor from '@/components/markdown/MdEditor.vue'

const notices = ref<CategoryNoticeVO[]>([])
const categories = ref<Category[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filterCategoryId = ref<string | null>(null)
const filterType = ref<string | null>(null)

const dialogVisible = ref(false)
const editingNotice = ref<CategoryNoticeVO | null>(null)
const submitting = ref(false)

const form = reactive({
  categoryId: '',
  title: '',
  content: '',
  type: 0,
  isPinned: false,
})

const categoryMap = computed<Record<string, string>>(() => {
  const map: Record<string, string> = {}
  for (const cat of categories.value) {
    map[String(cat.id)] = cat.name
  }
  return map
})

const typeOptions: Record<string, { label: string; tag: 'info' | 'success' }> = {
  '0': { label: '须知', tag: 'info' },
  '1': { label: '活动', tag: 'success' },
}

function categoryName(categoryId: string): string {
  return categoryMap.value[categoryId] || `#${categoryId}`
}

function typeLabel(type: number): string {
  return typeOptions[String(type)]?.label ?? '未知'
}

function typeTagType(type: number): '' | 'success' | 'info' {
  return typeOptions[String(type)]?.tag ?? 'info'
}

async function fetchCategories() {
  try {
    const res = await categoryApi.getList()
    categories.value = res.data ?? []
  } catch {
    // categories are non-critical, silently ignore
  }
}

async function fetchNotices() {
  loading.value = true
  error.value = null
  try {
    const res = await adminApi.getNotices({
      page: page.value,
      size: size.value,
      categoryId: filterCategoryId.value ?? undefined,
      type: filterType.value ?? undefined,
    })
    const data = res.data
    notices.value = data.records ?? []
    total.value = data.total ?? 0
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function onPageChange(p: number) {
  page.value = p
  fetchNotices()
}

function openCreateDialog() {
  editingNotice.value = null
  form.categoryId = ''
  form.title = ''
  form.content = ''
  form.type = 0
  form.isPinned = false
  dialogVisible.value = true
}

function openEditDialog(notice: CategoryNoticeVO) {
  editingNotice.value = notice
  form.categoryId = notice.categoryId
  form.title = notice.title
  form.content = notice.content
  form.type = notice.type
  form.isPinned = notice.isPinned === 1
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.title.trim()) {
    ElMessage.warning('请输入公告标题')
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('请输入公告内容')
    return
  }
  if (!editingNotice.value && !form.categoryId) {
    ElMessage.warning('请选择所属版块')
    return
  }

  submitting.value = true
  try {
    if (editingNotice.value) {
      await noticeApi.update(editingNotice.value.id, {
        title: form.title.trim(),
        content: form.content,
        type: form.type,
        isPinned: form.isPinned ? 1 : 0,
      })
      ElMessage.success('公告已更新')
    } else {
      await noticeApi.create(form.categoryId, {
        title: form.title.trim(),
        content: form.content,
        type: form.type,
        isPinned: form.isPinned ? 1 : 0,
      })
      ElMessage.success('公告已创建')
    }
    dialogVisible.value = false
    await fetchNotices()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(notice: CategoryNoticeVO) {
  try {
    await ElMessageBox.confirm(`确定要删除公告「${notice.title}」吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await noticeApi.delete(notice.id)
    ElMessage.success('公告已删除')
    await fetchNotices()
  } catch (e: unknown) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e instanceof Error ? e.message : '删除失败')
    }
  }
}

async function handlePinToggle(notice: CategoryNoticeVO, pinned: string | number | boolean) {
  const val = pinned ? 1 : 0
  try {
    await noticeApi.update(notice.id, { isPinned: val })
    notice.isPinned = val
    ElMessage.success(val === 1 ? '已置顶' : '已取消置顶')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
    // Revert the switch visual state
    fetchNotices()
  }
}

function handleFilterChange() {
  page.value = 1
  fetchNotices()
}

onMounted(() => {
  fetchCategories()
  fetchNotices()
})
</script>

<template>
  <div class="notice-manage">
    <!-- Header -->
    <div class="notice-manage__header">
      <h2 class="notice-manage__title">公告管理</h2>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">
        新建公告
      </el-button>
    </div>

    <!-- Filters -->
    <div class="notice-manage__filters">
      <el-select
        v-model="filterCategoryId"
        placeholder="所有版块"
        clearable
        class="notice-manage__filter-item"
        @change="handleFilterChange"
      >
        <el-option
          v-for="cat in categories"
          :key="cat.id"
          :label="cat.name"
          :value="String(cat.id)"
        />
      </el-select>
      <el-select
        v-model="filterType"
        placeholder="公告类型"
        clearable
        class="notice-manage__filter-item"
        @change="handleFilterChange"
      >
        <el-option label="须知" value="0" />
        <el-option label="活动" value="1" />
      </el-select>
    </div>

    <!-- Loading -->
    <LoadingSkeleton v-if="loading" variant="table" />

    <!-- Error -->
    <el-alert
      v-else-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
      class="notice-manage__alert"
    >
      <template #default>
        <el-button text type="primary" size="small" @click="fetchNotices">
          重试
        </el-button>
      </template>
    </el-alert>

    <!-- Empty -->
    <EmptyState
      v-else-if="notices.length === 0"
      title="暂无公告"
      description="还没有任何公告，点击上方按钮创建"
      :action-text="undefined"
      :action-route="undefined"
    />

    <!-- Table -->
    <template v-else>
      <el-table :data="notices" class="notice-manage__table" stripe>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="版块" width="120">
          <template #default="{ row }">
            {{ categoryName(row.categoryId) }}
          </template>
        </el-table-column>
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small">
              {{ typeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.isPinned === 1"
              size="small"
              @change="(val: string | number | boolean) => handlePinToggle(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="authorName" label="作者" width="100" />
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div v-if="total > size" class="notice-manage__pagination">
        <el-pagination
          :current-page="page"
          :page-size="size"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="onPageChange"
        />
      </div>
    </template>

    <!-- Create / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingNotice ? '编辑公告' : '新建公告'"
      width="720px"
      :close-on-click-modal="false"
      destroy-on-close
      class="notice-manage__dialog"
    >
      <el-form label-width="64px">
        <el-form-item v-if="!editingNotice" label="版块" required>
          <el-select
            v-model="form.categoryId"
            placeholder="选择所属版块"
            style="width: 100%"
          >
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.name"
              :value="String(cat.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input
            v-model="form.title"
            placeholder="公告标题"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="内容" required>
          <MdEditor v-model="form.content" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 160px">
            <el-option label="须知" :value="0" />
            <el-option label="活动" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="置顶">
          <el-checkbox v-model="form.isPinned" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.notice-manage {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--th-spacing-6);
  }

  &__title {
    font-size: 20px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    margin: 0;
  }

  &__filters {
    display: flex;
    gap: var(--th-spacing-3);
    margin-bottom: var(--th-spacing-4);
  }

  &__filter-item {
    width: 180px;
  }

  &__alert {
    margin: var(--th-spacing-4) 0;
  }

  &__table {
    margin-top: var(--th-spacing-2);
  }

  &__pagination {
    display: flex;
    justify-content: center;
    padding: var(--th-spacing-6) 0;
  }
}
</style>
