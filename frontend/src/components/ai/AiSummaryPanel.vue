<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { aiApi } from '@/api/modules/ai'
import type { AiSummaryResponse } from '@/api/types'
import MdViewer from '@/components/markdown/MdViewer.vue'
import AiQaPanel from './AiQaPanel.vue'

const props = defineProps<{
  postId: string
  postContent: string
}>()

const State = {
  ContentTooShort: 'content_too_short',
  NotLoggedIn: 'not_logged_in',
  Idle: 'idle',
  Generating: 'generating',
  Generated: 'generated',
  Error: 'error',
} as const

type S = (typeof State)[keyof typeof State]

// 轮询配置
const POLL_INTERVAL_MS = 2000
const POLL_MAX_ATTEMPTS = 30 // 最多轮询 60 秒

const userStore = useUserStore()
const state = ref<S>(State.Idle)
const summaryContent = ref('')
const errorMessage = ref('')
const loadingExisting = ref(false)
const hasExistingSummary = ref(false)

let pollTimer: ReturnType<typeof setInterval> | null = null

const canGenerate = computed(() => {
  return props.postContent.length >= 50 && userStore.isLoggedIn
})

const hasSummary = computed(() => state.value === State.Generated)

function resetToGate() {
  if (props.postContent.length < 50) {
    state.value = State.ContentTooShort
  } else if (!userStore.isLoggedIn) {
    state.value = State.NotLoggedIn
  } else {
    state.value = State.Idle
  }
}

async function checkExistingSummary() {
  if (!canGenerate.value) {
    resetToGate()
    return
  }

  loadingExisting.value = true
  try {
    const res = await aiApi.getSummary(props.postId)
    if (res.data) {
      handleSummaryResponse(res.data)
    } else {
      state.value = State.Idle
    }
  } catch {
    // No existing summary or error — show generate button
    state.value = State.Idle
  } finally {
    loadingExisting.value = false
  }
}

function handleSummaryResponse(data: AiSummaryResponse) {
  // status 0 = 生成中或没有摘要 → 显示"生成"按钮
  if (data.status === 0) {
    state.value = State.Idle
    return
  }

  if (data.status === 1) {
    if (data.content) {
      summaryContent.value = data.content
      state.value = State.Generated
      hasExistingSummary.value = true
    } else {
      state.value = State.Generating
    }
    return
  }

  if (data.status === 2) {
    if (data.content) {
      // 即使 status=2，如果有旧内容仍展示
      summaryContent.value = data.content
      state.value = State.Generated
      hasExistingSummary.value = true
    } else if (data.errorMessage) {
      errorMessage.value = data.errorMessage
      state.value = State.Error
    } else {
      state.value = State.Idle
    }
    return
  }

  // Default: no valid summary
  state.value = State.Idle
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function generateSummary() {
  state.value = State.Generating
  errorMessage.value = ''
  stopPolling()

  try {
    // 1. POST 触发异步生成（后端返回 R<Void>，不直接返回摘要内容）
    await aiApi.generateSummary(props.postId)

    // 2. 开始轮询 GET /summary 查询生成结果
    let pollCount = 0

    pollTimer = setInterval(async () => {
      pollCount++

      try {
        const res = await aiApi.getSummary(props.postId)
        const data = res.data

        if (!data) {
          // 还未创建摘要记录，继续等待
          if (pollCount >= POLL_MAX_ATTEMPTS) {
            stopPolling()
            errorMessage.value = '生成超时，请重试'
            state.value = State.Error
          }
          return
        }

        if (data.status === 1) {
          // 生成成功
          stopPolling()
          if (data.content) {
            summaryContent.value = data.content
            state.value = State.Generated
            hasExistingSummary.value = true
          } else {
            // 内容为空但状态成功，视为异常
            errorMessage.value = '摘要内容为空'
            state.value = State.Error
          }
        } else if (data.status === 2) {
          // 生成失败
          stopPolling()
          errorMessage.value = data.errorMessage || '生成失败'
          state.value = State.Error
        } else if (data.status === 0) {
          // 仍在生成中
          if (pollCount >= POLL_MAX_ATTEMPTS) {
            stopPolling()
            errorMessage.value = '生成超时，请重试'
            state.value = State.Error
          }
        }
      } catch {
        // GET 查询出错不停止轮询，继续重试
        if (pollCount >= POLL_MAX_ATTEMPTS) {
          stopPolling()
          errorMessage.value = '查询摘要超时，请重试'
          state.value = State.Error
        }
      }
    }, POLL_INTERVAL_MS)
  } catch (e: unknown) {
    const err = e as { message?: string }
    errorMessage.value = err.message || '触发摘要生成失败'
    state.value = State.Error
  }
}

onMounted(() => {
  checkExistingSummary()
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<template>
  <div class="ai-summary-panel">
    <!-- Content too short -->
    <el-result
      v-if="state === State.ContentTooShort"
      icon="warning"
      title="帖子内容过短"
      sub-title="帖子内容过短，暂不支持 AI 总结"
    />

    <!-- Not logged in -->
    <el-result
      v-else-if="state === State.NotLoggedIn"
      icon="info"
      title="需要登录"
      sub-title="登录后使用 AI 智能总结"
    >
      <template #extra>
        <router-link to="/login">
          <el-button type="primary">去登录</el-button>
        </router-link>
      </template>
    </el-result>

    <!-- Main panel -->
    <template v-else>
      <!-- Loading existing summary -->
      <div v-if="loadingExisting" class="ai-summary-panel__loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>正在检查已有摘要...</span>
      </div>

      <!-- Idle: show generate button -->
      <div v-else-if="state === State.Idle" class="ai-summary-panel__idle">
        <p class="ai-summary-panel__hint">AI 可以为你总结这篇帖子</p>
        <el-button type="primary" @click="generateSummary">
          生成 AI 总结
        </el-button>
      </div>

      <!-- Generating -->
      <div v-else-if="state === State.Generating" class="ai-summary-panel__generating">
        <el-icon class="is-loading ai-summary-panel__spinner"><Loading /></el-icon>
        <span>AI 正在总结...</span>
      </div>

      <!-- Generated -->
      <div v-else-if="state === State.Generated" class="ai-summary-panel__generated">
        <div class="ai-summary-panel__summary-header">
          <h3 class="ai-summary-panel__summary-title">AI 摘要</h3>
          <el-button
            type="primary"
            plain
            @click="generateSummary"
          >
            重新生成
          </el-button>
        </div>
        <div class="ai-summary-panel__summary-content">
          <MdViewer :content="summaryContent" />
        </div>
      </div>

      <!-- Error -->
      <div v-else-if="state === State.Error" class="ai-summary-panel__error">
        <el-result icon="error" title="生成失败" :sub-title="errorMessage">
          <template #extra>
            <el-button type="primary" @click="generateSummary">重试</el-button>
          </template>
        </el-result>
      </div>
    </template>

    <!-- Q&A Panel — shown when summary is generated -->
    <AiQaPanel
      v-if="hasSummary"
      :post-id="props.postId"
      :has-summary="true"
    />
  </div>
</template>

<style lang="scss" scoped>
.ai-summary-panel {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 20px;
  background: var(--el-bg-color);
  margin-top: 16px;

  &__loading,
  &__generating {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 24px 0;
    color: var(--el-text-color-secondary);
    font-size: 14px;
  }

  &__spinner {
    font-size: 20px;
  }

  &__idle {
    text-align: center;
    padding: 16px 0;
  }

  &__hint {
    color: var(--el-text-color-secondary);
    margin-bottom: 12px;
    font-size: 14px;
  }

  &__generated {
    // container
  }

  &__summary-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
  }

  &__summary-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    margin: 0;
  }

  &__summary-content {
    padding: 12px 16px;
    background: var(--el-fill-color-light);
    border-radius: 6px;
    max-height: 360px;
    overflow-y: auto;

    :deep(.markdown-body) {
      font-size: 14px;
    }
  }

  &__error {
    // el-result handles styling
  }
}
</style>
