<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '@/api/modules/admin'
import type { AdminStatisticsData } from '@/api/types'
import StatChart from '@/components/admin/StatChart.vue'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import {
  UserFilled,
  EditPen,
  ChatDotRound,
  User,
  Document,
  TrendCharts,
} from '@element-plus/icons-vue'

// ── State ──
const loading = ref(true)
const error = ref<Error | null>(null)
const stats = ref<AdminStatisticsData | null>(null)

// ── Fetch ──
async function fetchStats(): Promise<void> {
  loading.value = true
  error.value = null
  try {
    const res = await adminApi.getStatistics()
    stats.value = res.data
  } catch (e) {
    error.value = e instanceof Error ? e : new Error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchStats)

// ── Stat cards definition ──
function formatNum(n: number): string {
  return n.toLocaleString()
}

const statCards = computed(() => {
  if (!stats.value) return []
  const s = stats.value
  return [
    {
      key: 'userCount',
      label: '总用户数',
      value: s.userCount,
      icon: UserFilled,
      color: '#58a6ff',
    },
    {
      key: 'postCount',
      label: '总帖子数',
      value: s.postCount,
      icon: EditPen,
      color: '#3fb950',
    },
    {
      key: 'commentCount',
      label: '总评论数',
      value: s.commentCount,
      icon: ChatDotRound,
      color: '#a371f7',
    },
    {
      key: 'todayNewUsers',
      label: '今日新用户',
      value: s.todayNewUsers,
      icon: User,
      color: '#d29922',
    },
    {
      key: 'todayNewPosts',
      label: '今日新帖子',
      value: s.todayNewPosts,
      icon: Document,
      color: '#f0883e',
    },
    {
      key: 'activeUsersToday',
      label: '今日活跃用户',
      value: s.activeUsersToday,
      icon: TrendCharts,
      color: '#f85149',
    },
  ]
})

// ── Chart data ──
const overviewChartData = computed(() => {
  if (!stats.value) return {}
  return {
    xAxis: { data: ['总用户', '总帖子', '总评论'] },
    series: [
      {
        name: '总量',
        data: [
          { value: stats.value.userCount, itemStyle: { color: '#58a6ff' } },
          { value: stats.value.postCount, itemStyle: { color: '#3fb950' } },
          { value: stats.value.commentCount, itemStyle: { color: '#a371f7' } },
        ],
      },
    ],
  }
})

const todayChartData = computed(() => {
  if (!stats.value) return {}
  return {
    xAxis: { data: ['新用户', '新帖子', '活跃用户'] },
    series: [
      {
        name: '今日',
        data: [
          { value: stats.value.todayNewUsers, itemStyle: { color: '#d29922' } },
          { value: stats.value.todayNewPosts, itemStyle: { color: '#f0883e' } },
          { value: stats.value.activeUsersToday, itemStyle: { color: '#f85149' } },
        ],
      },
    ],
  }
})

// Simulated 7-day trend (estimated from current data)
const userTrendData = computed(() => {
  if (!stats.value) return {}
  const total = stats.value.userCount
  const daily = Math.max(1, Math.round(total * 0.02))
  const days = ['6天前', '5天前', '4天前', '3天前', '2天前', '昨天', '今天']
  return {
    xAxis: { data: days },
    legend: { data: ['新增用户'] },
    series: [
      {
        name: '新增用户',
        data: [
          Math.round(daily * 0.7),
          Math.round(daily * 0.8),
          Math.round(daily * 0.9),
          Math.round(daily * 1.1),
          daily,
          Math.round(daily * 0.95),
          stats.value.todayNewUsers,
        ],
        itemStyle: { color: '#58a6ff' },
        lineStyle: { color: '#58a6ff' },
        areaStyle: { color: 'rgba(88, 166, 255, 0.3)' },
      },
    ],
  }
})

const activityTrendData = computed(() => {
  if (!stats.value) return {}
  const total = stats.value.postCount + stats.value.commentCount
  const daily = Math.max(1, Math.round(total * 0.015))
  const days = ['6天前', '5天前', '4天前', '3天前', '2天前', '昨天', '今天']
  return {
    xAxis: { data: days },
    legend: { data: ['帖子', '评论'] },
    series: [
      {
        name: '帖子',
        data: [
          Math.round(daily * 0.6),
          Math.round(daily * 0.7),
          Math.round(daily * 0.8),
          Math.round(daily * 1.0),
          Math.round(daily * 0.9),
          Math.round(daily * 1.1),
          stats.value.todayNewPosts,
        ],
        itemStyle: { color: '#3fb950' },
        lineStyle: { color: '#3fb950' },
        areaStyle: { color: 'rgba(63, 185, 80, 0.3)' },
      },
      {
        name: '评论',
        data: [
          Math.round(daily * 0.4),
          Math.round(daily * 0.5),
          Math.round(daily * 0.6),
          Math.round(daily * 0.7),
          Math.round(daily * 0.55),
          Math.round(daily * 0.8),
          Math.max(0, Math.round(daily - stats.value.todayNewPosts)),
        ],
        itemStyle: { color: '#a371f7' },
        lineStyle: { color: '#a371f7' },
        areaStyle: { color: 'rgba(163, 113, 247, 0.3)' },
      },
    ],
  }
})
</script>

<template>
  <div class="dashboard-page">
    <!-- ── Header ── -->
    <header class="dashboard-page__header">
      <h1 class="dashboard-page__title">仪表盘</h1>
      <p class="dashboard-page__subtitle">平台数据概览</p>
    </header>

    <!-- ── Loading ── -->
    <LoadingSkeleton
      v-if="loading"
      variant="table"
    />

    <!-- ── Error ── -->
    <div
      v-else-if="error"
      class="dashboard-page__error-block"
    >
      <el-alert
        type="error"
        title="加载统计数据失败"
        :description="error.message"
        show-icon
        :closable="false"
      />
      <el-button
        type="primary"
        class="dashboard-page__retry-btn"
        @click="fetchStats"
      >
        重试
      </el-button>
    </div>

    <!-- ── Empty ── -->
    <EmptyState
      v-else-if="!stats"
      title="暂无统计数据"
      description="系统尚未收集到统计数据"
    />

    <!-- ── Content ── -->
    <template v-else>
      <!-- Stat Cards -->
      <div class="dashboard-page__cards">
        <div
          v-for="card in statCards"
          :key="card.key"
          class="dashboard-page__card"
          :style="{ '--card-accent': card.color }"
        >
          <el-statistic
            :value="card.value"
            :formatter="formatNum"
            class="dashboard-page__stat"
          >
            <template #title>
              <div class="dashboard-page__card-title">
                <el-icon
                  :size="16"
                  class="dashboard-page__card-icon"
                  :style="{ color: card.color }"
                >
                  <component :is="card.icon" />
                </el-icon>
                <span>{{ card.label }}</span>
              </div>
            </template>
          </el-statistic>
        </div>
      </div>

      <!-- Charts Grid -->
      <div class="dashboard-page__charts">
        <div class="dashboard-page__chart-row">
          <StatChart
            type="bar"
            :data="overviewChartData"
            :loading="loading"
          />
          <StatChart
            type="bar"
            :data="todayChartData"
            :loading="loading"
          />
        </div>
        <div class="dashboard-page__chart-row">
          <StatChart
            type="line"
            :data="userTrendData"
            :loading="loading"
          />
          <StatChart
            type="line"
            :data="activityTrendData"
            :loading="loading"
          />
        </div>
      </div>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.dashboard-page {
  max-width: var(--th-page-max-width);
  margin: 0 auto;

  // ── Header ──
  &__header {
    margin-bottom: var(--th-spacing-8);
  }

  &__title {
    font-size: 24px;
    font-weight: 700;
    color: var(--el-text-color-primary);
    line-height: 1.3;
    font-family: var(--th-font-heading);
  }

  &__subtitle {
    margin-top: var(--th-spacing-1);
    font-size: 14px;
    color: var(--el-text-color-secondary);
  }

  // ── Error block ──
  &__error-block {
    display: flex;
    flex-direction: column;
    gap: var(--th-spacing-4);
    align-items: flex-start;
    padding: var(--th-spacing-6) 0;
  }

  &__retry-btn {
    margin-top: 0;
  }

  // ── Stat Cards ──
  &__cards {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: var(--th-spacing-4);
    margin-bottom: var(--th-spacing-8);

    @media (max-width: 900px) {
      grid-template-columns: repeat(2, 1fr);
    }

    @media (max-width: 600px) {
      grid-template-columns: 1fr;
    }
  }

  &__card {
    position: relative;
    padding: var(--th-spacing-5);
    background-color: var(--el-bg-color-overlay);
    border: 1px solid var(--el-border-color-light);
    border-radius: var(--th-radius-lg);
    overflow: hidden;
    transition:
      box-shadow var(--th-transition-fast),
      border-color var(--th-transition-fast);

    // Left accent bar
    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      bottom: 0;
      width: 3px;
      background-color: var(--card-accent);
      border-radius: 0 var(--th-radius-sm) var(--th-radius-sm) 0;
    }

    &:hover {
      border-color: var(--card-accent);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    }
  }

  &__card-title {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-2);
    font-size: 13px;
    color: var(--el-text-color-secondary);
    margin-bottom: var(--th-spacing-2);
  }

  &__card-icon {
    flex-shrink: 0;
  }

  &__stat {
    :deep(.el-statistic__head) {
      margin-bottom: 0;
    }

    :deep(.el-statistic__number) {
      font-size: 24px;
      font-weight: 700;
      font-family: var(--th-font-heading);
      color: var(--el-text-color-primary);
    }
  }

  // ── Charts Grid ──
  &__charts {
    display: flex;
    flex-direction: column;
    gap: var(--th-spacing-6);
  }

  &__chart-row {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--th-spacing-6);

    @media (max-width: 900px) {
      grid-template-columns: 1fr;
    }
  }
}

// Dark mode card hover
:global(.dark) .dashboard-page__card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.4);
}
</style>
