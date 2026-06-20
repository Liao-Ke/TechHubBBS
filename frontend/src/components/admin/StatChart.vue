<script setup lang="ts">
import { computed, ref } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components'
import VChart from 'vue-echarts'
import { useAppStore } from '@/stores/app'

// Tree-shake: register only what we need
use([
  CanvasRenderer,
  BarChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
])

const appStore = useAppStore()

const props = withDefaults(defineProps<{
  type: 'bar' | 'line'
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  data: any
  loading?: boolean
}>(), {
  loading: false,
})

const chartRef = ref<InstanceType<typeof VChart> | null>(null)

// Dark mode aware theme colors
const isDark = computed(() => appStore.isDarkMode)

const textColor = computed(() => (isDark.value ? '#c9d1d9' : '#24292f'))
const axisLineColor = computed(() => (isDark.value ? '#30363d' : '#d0d7de'))
const splitLineColor = computed(() => (isDark.value ? '#21262d' : '#eaeef2'))

const option = computed(() => {
  const base = props.data ?? {}

  if (props.type === 'bar') {
    return {
      backgroundColor: 'transparent',
      textStyle: { color: textColor.value },
      tooltip: {
        trigger: 'axis' as const,
        axisPointer: { type: 'shadow' as const },
        backgroundColor: isDark.value ? '#161b22' : '#ffffff',
        borderColor: axisLineColor.value,
        textStyle: { color: textColor.value },
      },
      legend: {
        ...base.legend,
        textStyle: { color: textColor.value },
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true,
        ...base.grid,
      },
      xAxis: {
        type: 'category' as const,
        axisLine: { lineStyle: { color: axisLineColor.value } },
        axisLabel: { color: textColor.value },
        axisTick: { show: false },
        ...base.xAxis,
      },
      yAxis: {
        type: 'value' as const,
        splitLine: { lineStyle: { color: splitLineColor.value } },
        axisLabel: { color: textColor.value },
        ...base.yAxis,
      },
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      series: (base.series ?? []).map((s: any) => ({
        type: 'bar' as const,
        emphasis: { focus: 'series' as const },
        itemStyle: { borderRadius: [4, 4, 0, 0] },
        ...s,
      })),
    }
  }

  // Line chart
  return {
    backgroundColor: 'transparent',
    textStyle: { color: textColor.value },
    tooltip: {
      trigger: 'axis' as const,
      backgroundColor: isDark.value ? '#161b22' : '#ffffff',
      borderColor: axisLineColor.value,
      textStyle: { color: textColor.value },
    },
    legend: {
      ...base.legend,
      textStyle: { color: textColor.value },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
      ...base.grid,
    },
    xAxis: {
      type: 'category' as const,
      boundaryGap: false,
      axisLine: { lineStyle: { color: axisLineColor.value } },
      axisLabel: { color: textColor.value },
      axisTick: { show: false },
      ...base.xAxis,
    },
    yAxis: {
      type: 'value' as const,
      splitLine: { lineStyle: { color: splitLineColor.value } },
      axisLabel: { color: textColor.value },
      ...base.yAxis,
    },
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    series: (base.series ?? []).map((s: any) => ({
      type: 'line' as const,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 2 },
      areaStyle: { opacity: 0.08 },
      ...s,
    })),
  }
})

// Expose the underlying ECharts instance if needed
defineExpose({ chartRef })
</script>

<template>
  <div class="stat-chart" v-loading="loading">
    <v-chart
      ref="chartRef"
      class="stat-chart__canvas"
      :option="option"
      :autoresize="true"
    />
  </div>
</template>

<style lang="scss" scoped>
.stat-chart {
  position: relative;
  width: 100%;
  min-height: 280px;
  padding: var(--th-spacing-3);
  background-color: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  border-radius: var(--th-radius-lg);

  &__canvas {
    width: 100%;
    height: 280px;
  }
}

// Dark mode refinement
:global(.dark) .stat-chart {
  background-color: var(--el-bg-color-overlay, #161b22);
}
</style>
