<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { aiApi } from '@/api/modules/ai'
import type { AiQaResponse } from '@/api/types'
import MdViewer from '@/components/markdown/MdViewer.vue'

const props = defineProps<{
  postId: string
  hasSummary: boolean
}>()

const question = ref('')
const submitting = ref(false)
const loadingHistory = ref(false)
const history = ref<AiQaResponse[]>([])
const currentPage = ref(1)
const totalPages = ref(0)
const submitError = ref('')
const historyError = ref('')

const pageSize = 10

async function submitQuestion() {
  const q = question.value.trim()
  if (!q) return

  submitting.value = true
  submitError.value = ''

  try {
    const res = await aiApi.askQuestion(props.postId, { question: q })
    // Prepend the new Q&A to history
    history.value.unshift(res.data)
    question.value = ''
  } catch (e: unknown) {
    const err = e as { message?: string }
    submitError.value = err.message || '提问失败，请稍后再试'
  } finally {
    submitting.value = false
  }
}

async function loadHistory(page: number = 1) {
  loadingHistory.value = true
  historyError.value = ''

  try {
    const res = await aiApi.getQaHistory(props.postId, { page, size: pageSize })
    history.value = res.data.records ?? []
    currentPage.value = res.data.current
    totalPages.value = res.data.pages
  } catch (e: unknown) {
    const err = e as { message?: string }
    historyError.value = err.message || '加载问答记录失败'
  } finally {
    loadingHistory.value = false
  }
}

function handlePageChange(page: number) {
  loadHistory(page)
}

onMounted(() => {
  if (props.hasSummary) {
    loadHistory()
  }
})
</script>

<template>
  <div class="ai-qa-panel">
    <!-- No summary placeholder -->
    <div v-if="!hasSummary" class="ai-qa-panel__placeholder">
      <el-result icon="info" title="AI 问答" sub-title="请先生成 AI 摘要" />
    </div>

    <template v-else>
      <div class="ai-qa-panel__header">
        <h3 class="ai-qa-panel__title">AI 问答</h3>
        <span class="ai-qa-panel__desc">基于帖子内容回答你的问题</span>
      </div>

      <!-- Question input -->
      <div class="ai-qa-panel__input-row">
        <el-input
          v-model="question"
          placeholder="输入你的问题..."
          :disabled="submitting"
          @keyup.enter="submitQuestion"
        >
          <template #append>
            <el-button
              type="primary"
              :loading="submitting"
              :disabled="!question.trim()"
              @click="submitQuestion"
            >
              发送
            </el-button>
          </template>
        </el-input>
      </div>

      <!-- Submit error -->
      <el-alert
        v-if="submitError"
        type="error"
        :title="submitError"
        show-icon
        closable
        class="ai-qa-panel__alert"
        @close="submitError = ''"
      />

      <!-- History loading -->
      <div v-if="loadingHistory" class="ai-qa-panel__loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载问答记录...</span>
      </div>

      <!-- History error -->
      <el-result
        v-else-if="historyError && history.length === 0"
        icon="error"
        title="加载失败"
        :sub-title="historyError"
      >
        <template #extra>
          <el-button type="primary" @click="loadHistory()">重试</el-button>
        </template>
      </el-result>

      <!-- History list -->
      <template v-else>
        <div v-if="history.length === 0" class="ai-qa-panel__empty">
          <el-empty description="暂无问答记录" />
        </div>

        <div v-else class="ai-qa-panel__history">
          <div
            v-for="item in history"
            :key="item.id"
            class="ai-qa-panel__qa-item"
          >
            <div class="ai-qa-panel__question">
              <span class="ai-qa-panel__qa-label">Q</span>
              <span class="ai-qa-panel__qa-text">{{ item.question }}</span>
            </div>
            <div class="ai-qa-panel__answer">
              <span class="ai-qa-panel__qa-label ai-qa-panel__qa-label--answer">A</span>
              <div class="ai-qa-panel__qa-text">
                <MdViewer :content="item.answer" />
              </div>
            </div>
            <div class="ai-qa-panel__qa-time">{{ item.createTime }}</div>
          </div>

          <!-- Pagination -->
          <div v-if="totalPages > 1" class="ai-qa-panel__pagination">
            <el-pagination
              :current-page="currentPage"
              :total="totalPages * pageSize"
              :page-size="pageSize"
              layout="prev, pager, next"
              small
              @current-change="handlePageChange"
            />
          </div>
        </div>
      </template>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.ai-qa-panel {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 20px;
  background: var(--el-bg-color);
  margin-top: 16px;

  &__placeholder {
    // el-result handles styling
  }

  &__header {
    margin-bottom: 16px;
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    margin: 0 0 4px 0;
  }

  &__desc {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  &__input-row {
    margin-bottom: 12px;
  }

  &__alert {
    margin-bottom: 12px;
  }

  &__loading {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 24px 0;
    color: var(--el-text-color-secondary);
  }

  &__empty {
    // el-empty handles styling
  }

  &__history {
    // container
  }

  &__qa-item {
    border-bottom: 1px solid var(--el-border-color-lighter);
    padding: 16px 0;

    &:last-child {
      border-bottom: none;
    }
  }

  &__question,
  &__answer {
    display: flex;
    gap: 10px;
    margin-bottom: 8px;
  }

  &__qa-label {
    flex-shrink: 0;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 13px;
    font-weight: 700;
    color: #fff;
    background: var(--el-color-primary);
  }

  &__qa-label--answer {
    background: var(--el-color-success);
  }

  &__qa-text {
    flex: 1;
    font-size: 14px;
    color: var(--el-text-color-primary);
    line-height: 1.6;

    :deep(.markdown-body) {
      font-size: 14px;
      background: none;
    }
  }

  &__qa-time {
    font-size: 12px;
    color: var(--el-text-color-placeholder);
    padding-left: 34px;
  }

  &__pagination {
    display: flex;
    justify-content: center;
    margin-top: 16px;
    padding-top: 12px;
  }
}
</style>
