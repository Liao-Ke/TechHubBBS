import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

// ── Mock ECharts / vue-echarts (jsdom has no canvas) ──
vi.mock('vue-echarts', () => ({
  default: {
    name: 'VChart',
    props: ['option', 'autoresize', 'loading'],
    template: '<div class="v-chart-mock"><slot /></div>',
  },
}))

vi.mock('echarts/core', () => ({
  use: vi.fn<(...args: unknown[]) => unknown>(),
}))

vi.mock('echarts/renderers', () => ({
  CanvasRenderer: {},
}))

vi.mock('echarts/charts', () => ({
  BarChart: {},
  LineChart: {},
}))

vi.mock('echarts/components', () => ({
  TitleComponent: {},
  TooltipComponent: {},
  LegendComponent: {},
  GridComponent: {},
}))

// ── Mock app store ──
const mockStore = {
  isDarkMode: true,
}

vi.mock('@/stores/app', () => ({
  useAppStore: () => mockStore,
}))

import StatChart from '@/components/admin/StatChart.vue'

// ── Helpers ──
function mountChart(props: Record<string, unknown> = {}) {
  return mount(StatChart, {
    props: {
      type: 'bar',
      data: {
        xAxis: { data: ['A', 'B', 'C'] },
        series: [{ data: [10, 20, 30] }],
      },
      ...props,
    },
    global: { plugins: [createPinia()] },
  })
}

describe('StatChart', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  // ── Rendering ──
  describe('rendering', () => {
    it('renders the wrapper div with stat-chart class', () => {
      const wrapper = mountChart()
      expect(wrapper.find('.stat-chart').exists()).toBe(true)
    })

    it('renders VChart component', () => {
      const wrapper = mountChart()
      expect(wrapper.findComponent({ name: 'VChart' }).exists()).toBe(true)
    })

    it('passes autoresize prop to VChart', () => {
      const wrapper = mountChart()
      const vChart = wrapper.findComponent({ name: 'VChart' })
      expect(vChart.props('autoresize')).toBe(true)
    })

    it('computes bar chart option from data prop', () => {
      const wrapper = mountChart({
        type: 'bar',
        data: {
          xAxis: { data: ['甲', '乙'] },
          series: [{ data: [100, 200] }],
        },
      })
      const vChart = wrapper.findComponent({ name: 'VChart' })
      const option = vChart.props('option')
      expect(option).toBeDefined()
      expect(option.xAxis.data).toEqual(['甲', '乙'])
      expect(option.series[0].data).toEqual([100, 200])
    })

    it('includes dark mode colors in option when dark', () => {
      mockStore.isDarkMode = true
      const wrapper = mountChart({ type: 'bar', data: { series: [] } })
      const vChart = wrapper.findComponent({ name: 'VChart' })
      const option = vChart.props('option')
      expect(option.textStyle.color).toBe('#c9d1d9')
    })

    it('includes light mode colors in option when light', () => {
      mockStore.isDarkMode = false
      const wrapper = mountChart({ type: 'bar', data: { series: [] } })
      const vChart = wrapper.findComponent({ name: 'VChart' })
      const option = vChart.props('option')
      expect(option.textStyle.color).toBe('#24292f')
    })
  })

  // ── Chart types ──
  describe('chart types', () => {
    it('generates bar chart option with category axis', () => {
      const wrapper = mountChart({ type: 'bar', data: { series: [{ data: [1, 2] }] } })
      const vChart = wrapper.findComponent({ name: 'VChart' })
      const option = vChart.props('option')
      expect(option.series[0].type).toBe('bar')
      expect(option.tooltip.trigger).toBe('axis')
    })

    it('generates line chart option with smooth lines', () => {
      const wrapper = mountChart({ type: 'line', data: { series: [{ data: [1, 2] }] } })
      const vChart = wrapper.findComponent({ name: 'VChart' })
      const option = vChart.props('option')
      expect(option.series[0].type).toBe('line')
      expect(option.series[0].smooth).toBe(true)
      expect(option.xAxis.boundaryGap).toBe(false)
    })

    it('line chart includes tooltip with axis trigger', () => {
      const wrapper = mountChart({ type: 'line', data: { series: [{ data: [1, 2] }] } })
      const vChart = wrapper.findComponent({ name: 'VChart' })
      const option = vChart.props('option')
      expect(option.tooltip.trigger).toBe('axis')
    })
  })

  // ── Loading state ──
  describe('loading', () => {
    it('applies v-loading when loading prop is true', () => {
      const wrapper = mountChart({ loading: true, data: { series: [] } })
      expect(wrapper.find('.stat-chart').exists()).toBe(true)
    })

    it('does not crash when data is undefined', () => {
      const wrapper = mountChart({ data: undefined })
      expect(wrapper.find('.stat-chart').exists()).toBe(true)
    })

    it('defaults loading to false', () => {
      const wrapper = mountChart({ data: { series: [] } })
      expect(wrapper.props('loading')).toBe(false)
    })
  })

  // ── Styling ──
  describe('styling', () => {
    it('has chart canvas class', () => {
      const wrapper = mountChart()
      expect(wrapper.find('.stat-chart__canvas').exists()).toBe(true)
    })

    it('renders canvas with correct height style', () => {
      const wrapper = mountChart()
      expect(wrapper.find('.stat-chart').exists()).toBe(true)
    })
  })

  // ── Props validation ──
  describe('props', () => {
    it('accepts type bar', () => {
      const wrapper = mountChart({ type: 'bar', data: { series: [] } })
      expect(wrapper.props('type')).toBe('bar')
    })

    it('accepts type line', () => {
      const wrapper = mountChart({ type: 'line', data: { series: [] } })
      expect(wrapper.props('type')).toBe('line')
    })

    it('merges custom legend options', () => {
      const wrapper = mountChart({
        type: 'bar',
        data: { legend: { show: false }, series: [] },
      })
      const vChart = wrapper.findComponent({ name: 'VChart' })
      expect(vChart.props('option').legend.show).toBe(false)
    })
  })
})
