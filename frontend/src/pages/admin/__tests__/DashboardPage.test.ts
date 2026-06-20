import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import type { AdminStatisticsData } from '@/api/types'

// ── Mock ECharts / vue-echarts (jsdom has no canvas) ──
vi.mock('vue-echarts', () => ({
  default: {
    name: 'VChart',
    props: ['option', 'autoresize', 'loading'],
    template: '<div class="v-chart-mock" />',
  },
}))

vi.mock('echarts/core', () => ({ use: vi.fn<(...args: unknown[]) => unknown>() }))
vi.mock('echarts/renderers', () => ({ CanvasRenderer: {} }))
vi.mock('echarts/charts', () => ({ BarChart: {}, LineChart: {} }))
vi.mock('echarts/components', () => ({
  TitleComponent: {},
  TooltipComponent: {},
  LegendComponent: {},
  GridComponent: {},
}))

// ── Mock admin API ──
const mockGetStatistics = vi.fn<(...args: unknown[]) => unknown>()

vi.mock('@/api/modules/admin', () => ({
  adminApi: {
    getStatistics: (...args: unknown[]) => mockGetStatistics(...args),
    getUsers: vi.fn<(...args: unknown[]) => unknown>(),
    banUser: vi.fn<(...args: unknown[]) => unknown>(),
    setRole: vi.fn<(...args: unknown[]) => unknown>(),
    getPosts: vi.fn<(...args: unknown[]) => unknown>(),
    setPostType: vi.fn<(...args: unknown[]) => unknown>(),
    lockPost: vi.fn<(...args: unknown[]) => unknown>(),
    deletePost: vi.fn<(...args: unknown[]) => unknown>(),
    getNotices: vi.fn<(...args: unknown[]) => unknown>(),
    setDivine: vi.fn<(...args: unknown[]) => unknown>(),
  },
}))

// ── Mock app store ──
const mockStore = {
  isDarkMode: true,
  sidebarCollapsed: false,
  toggleSidebar: vi.fn<(...args: unknown[]) => unknown>(),
  toggleDarkMode: vi.fn<(...args: unknown[]) => unknown>(),
}

vi.mock('@/stores/app', () => ({
  useAppStore: () => mockStore,
}))

import DashboardPage from '@/pages/admin/DashboardPage.vue'

// ── Helpers ──
function makeStats(overrides: Partial<AdminStatisticsData> = {}): AdminStatisticsData {
  return {
    userCount: 1234,
    postCount: 567,
    commentCount: 890,
    todayNewUsers: 12,
    todayNewPosts: 34,
    activeUsersToday: 56,
    ...overrides,
  }
}

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/admin', name: 'admin-dashboard', component: DashboardPage },
    ],
  })
}

async function mountPage() {
  const router = createTestRouter()
  const pinia = createPinia()
  setActivePinia(pinia)

  await router.push('/admin')
  await router.isReady()

  const wrapper = mount(DashboardPage, {
    global: { plugins: [router, pinia] },
  })

  // Wait for initial async operations
  await flushPromises()
  await nextTick()

  return { wrapper, router, pinia }
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetStatistics.mockReset()
  })

  // ── Initial render ──
  describe('initial render', () => {
    it('is not a stub (has real content)', async () => {
      mockGetStatistics.mockResolvedValue({ data: makeStats() })
      const { wrapper } = await mountPage()

      expect(wrapper.text().length).toBeGreaterThan(20)
      expect(wrapper.find('.dashboard-page').exists()).toBe(true)
    })

    it('renders header with title', async () => {
      mockGetStatistics.mockResolvedValue({ data: makeStats() })
      const { wrapper } = await mountPage()

      expect(wrapper.find('.dashboard-page__title').text()).toBe('仪表盘')
      expect(wrapper.find('.dashboard-page__subtitle').text()).toBe('平台数据概览')
    })
  })

  // ── Loading state ──
  describe('loading state', () => {
    it('shows LoadingSkeleton while fetching', async () => {
      mockGetStatistics.mockReturnValue(new Promise(() => {})) // never resolves

      const { wrapper } = await mountPage()

      expect(wrapper.findComponent({ name: 'LoadingSkeleton' }).exists()).toBe(true)
    })

    it('does not show stat cards while loading', async () => {
      mockGetStatistics.mockReturnValue(new Promise(() => {}))

      const { wrapper } = await mountPage()

      expect(wrapper.find('.dashboard-page__card').exists()).toBe(false)
    })

    it('calls getStatistics on mount', async () => {
      mockGetStatistics.mockResolvedValue({ data: makeStats() })

      await mountPage()

      expect(mockGetStatistics).toHaveBeenCalledOnce()
    })
  })

  // ── Error state ──
  describe('error state', () => {
    it('shows error alert when API fails', async () => {
      mockGetStatistics.mockRejectedValue(new Error('网络错误'))

      const { wrapper } = await mountPage()

      expect(wrapper.findComponent({ name: 'ElAlert' }).exists()).toBe(true)
      expect(wrapper.text()).toContain('加载统计数据失败')
    })

    it('shows error description message', async () => {
      mockGetStatistics.mockRejectedValue(new Error('服务器内部错误'))

      const { wrapper } = await mountPage()

      expect(wrapper.text()).toContain('服务器内部错误')
    })

    it('shows retry button on error', async () => {
      mockGetStatistics.mockRejectedValue(new Error('超时'))

      const { wrapper } = await mountPage()

      expect(wrapper.text()).toContain('重试')
    })

    it('retry button re-fetches statistics', async () => {
      mockGetStatistics.mockRejectedValue(new Error('超时'))

      const { wrapper } = await mountPage()
      expect(mockGetStatistics).toHaveBeenCalledTimes(1)

      // Click retry
      const retryBtn = wrapper.find('.dashboard-page__retry-btn')
      expect(retryBtn.exists()).toBe(true)

      // Reset mock for retry call
      mockGetStatistics.mockResolvedValue({ data: makeStats() })
      await retryBtn.trigger('click')
      await flushPromises()
      await nextTick()

      expect(mockGetStatistics).toHaveBeenCalledTimes(2)
    })

    it('does not show stat cards when error occurs', async () => {
      mockGetStatistics.mockRejectedValue(new Error('错误'))

      const { wrapper } = await mountPage()

      expect(wrapper.find('.dashboard-page__card').exists()).toBe(false)
    })
  })

  // ── Data loaded state ──
  describe('data loaded', () => {
    it('renders 6 stat cards', async () => {
      mockGetStatistics.mockResolvedValue({ data: makeStats() })

      const { wrapper } = await mountPage()

      const cards = wrapper.findAll('.dashboard-page__card')
      expect(cards).toHaveLength(6)
    })

    it('displays userCount correctly', async () => {
      mockGetStatistics.mockResolvedValue({ data: makeStats({ userCount: 9999 }) })

      const { wrapper } = await mountPage()

      expect(wrapper.text()).toContain('9,999')
      expect(wrapper.text()).toContain('总用户数')
    })

    it('displays postCount correctly', async () => {
      mockGetStatistics.mockResolvedValue({ data: makeStats({ postCount: 500 }) })

      const { wrapper } = await mountPage()

      expect(wrapper.text()).toContain('500')
      expect(wrapper.text()).toContain('总帖子数')
    })

    it('displays commentCount correctly', async () => {
      mockGetStatistics.mockResolvedValue({ data: makeStats({ commentCount: 2500 }) })

      const { wrapper } = await mountPage()

      expect(wrapper.text()).toContain('2,500')
      expect(wrapper.text()).toContain('总评论数')
    })

    it('displays todayNewUsers correctly', async () => {
      mockGetStatistics.mockResolvedValue({ data: makeStats({ todayNewUsers: 42 }) })

      const { wrapper } = await mountPage()

      expect(wrapper.text()).toContain('42')
      expect(wrapper.text()).toContain('今日新用户')
    })

    it('displays todayNewPosts correctly', async () => {
      mockGetStatistics.mockResolvedValue({ data: makeStats({ todayNewPosts: 18 }) })

      const { wrapper } = await mountPage()

      expect(wrapper.text()).toContain('18')
      expect(wrapper.text()).toContain('今日新帖子')
    })

    it('displays activeUsersToday correctly', async () => {
      mockGetStatistics.mockResolvedValue({ data: makeStats({ activeUsersToday: 77 }) })

      const { wrapper } = await mountPage()

      expect(wrapper.text()).toContain('77')
      expect(wrapper.text()).toContain('今日活跃用户')
    })

    it('renders 4 StatChart components', async () => {
      mockGetStatistics.mockResolvedValue({ data: makeStats() })

      const { wrapper } = await mountPage()

      const charts = wrapper.findAllComponents({ name: 'StatChart' })
      expect(charts).toHaveLength(4)
    })

    it('first two StatCharts are bar type', async () => {
      mockGetStatistics.mockResolvedValue({ data: makeStats() })

      const { wrapper } = await mountPage()

      const charts = wrapper.findAllComponents({ name: 'StatChart' })
      expect(charts[0]!.props('type')).toBe('bar')
      expect(charts[1]!.props('type')).toBe('bar')
    })

    it('last two StatCharts are line type', async () => {
      mockGetStatistics.mockResolvedValue({ data: makeStats() })

      const { wrapper } = await mountPage()

      const charts = wrapper.findAllComponents({ name: 'StatChart' })
      expect(charts[2]!.props('type')).toBe('line')
      expect(charts[3]!.props('type')).toBe('line')
    })

    it('each stat card has an icon', async () => {
      mockGetStatistics.mockResolvedValue({ data: makeStats() })

      const { wrapper } = await mountPage()

      const icons = wrapper.findAll('.dashboard-page__card-icon')
      expect(icons).toHaveLength(6)
    })

    it('each stat card has an accent color via CSS variable', async () => {
      mockGetStatistics.mockResolvedValue({ data: makeStats() })

      const { wrapper } = await mountPage()

      const cards = wrapper.findAll('.dashboard-page__card')
      cards.forEach((card) => {
        expect(card.attributes('style')).toContain('--card-accent')
      })
    })
  })

  // ── Empty/null data ──
  describe('empty data', () => {
    it('shows EmptyState when statistics data is null', async () => {
      mockGetStatistics.mockResolvedValue({ data: null })

      const { wrapper } = await mountPage()

      expect(wrapper.findComponent({ name: 'EmptyState' }).exists()).toBe(true)
      expect(wrapper.text()).toContain('暂无统计数据')
    })
  })

  // ── Chart data derivation ──
  describe('chart data', () => {
    it('passes overview chart data to first StatChart', async () => {
      mockGetStatistics.mockResolvedValue({ data: makeStats({ userCount: 100, postCount: 200, commentCount: 300 }) })

      const { wrapper } = await mountPage()

      const charts = wrapper.findAllComponents({ name: 'StatChart' })
      const overviewData = charts[0]!.props('data')
      expect(overviewData.xAxis.data).toEqual(['总用户', '总帖子', '总评论'])
      expect(overviewData.series[0].data[0].value).toBe(100)
      expect(overviewData.series[0].data[1].value).toBe(200)
      expect(overviewData.series[0].data[2].value).toBe(300)
    })

    it('passes today chart data to second StatChart', async () => {
      mockGetStatistics.mockResolvedValue({
        data: makeStats({ todayNewUsers: 5, todayNewPosts: 10, activeUsersToday: 15 }),
      })

      const { wrapper } = await mountPage()

      const charts = wrapper.findAllComponents({ name: 'StatChart' })
      const todayData = charts[1]!.props('data')
      expect(todayData.xAxis.data).toEqual(['新用户', '新帖子', '活跃用户'])
      expect(todayData.series[0].data[0].value).toBe(5)
      expect(todayData.series[0].data[1].value).toBe(10)
      expect(todayData.series[0].data[2].value).toBe(15)
    })

    it('passes user trend data to third StatChart (line)', async () => {
      mockGetStatistics.mockResolvedValue({
        data: makeStats({ userCount: 1000, todayNewUsers: 20 }),
      })

      const { wrapper } = await mountPage()

      const charts = wrapper.findAllComponents({ name: 'StatChart' })
      const trendData = charts[2]!.props('data')
      expect(trendData.xAxis.data).toHaveLength(7)
      expect(trendData.series[0].name).toBe('新增用户')
      // Last data point should equal today's new users
      const lastValue = trendData.series[0].data[6]
      expect(lastValue).toBe(20)
    })

    it('passes activity trend data to fourth StatChart (line)', async () => {
      mockGetStatistics.mockResolvedValue({
        data: makeStats({ postCount: 500, commentCount: 500, todayNewPosts: 8 }),
      })

      const { wrapper } = await mountPage()

      const charts = wrapper.findAllComponents({ name: 'StatChart' })
      const activityData = charts[3]!.props('data')
      expect(activityData.xAxis.data).toHaveLength(7)
      expect(activityData.series[0].name).toBe('帖子')
      expect(activityData.series[1].name).toBe('评论')
    })
  })

  // ── Styling ──
  describe('styling', () => {
    it('uses grid layout for stat cards', async () => {
      mockGetStatistics.mockResolvedValue({ data: makeStats() })

      const { wrapper } = await mountPage()

      expect(wrapper.find('.dashboard-page__cards').exists()).toBe(true)
    })

    it('uses grid layout for chart rows', async () => {
      mockGetStatistics.mockResolvedValue({ data: makeStats() })

      const { wrapper } = await mountPage()

      const chartRows = wrapper.findAll('.dashboard-page__chart-row')
      expect(chartRows).toHaveLength(2)
    })
  })

  // ── Edge cases ──
  describe('edge cases', () => {
    it('handles zero values in statistics', async () => {
      mockGetStatistics.mockResolvedValue({
        data: makeStats({
          userCount: 0,
          postCount: 0,
          commentCount: 0,
          todayNewUsers: 0,
          todayNewPosts: 0,
          activeUsersToday: 0,
        }),
      })

      const { wrapper } = await mountPage()

      // Should render without error
      expect(wrapper.find('.dashboard-page').exists()).toBe(true)
      expect(wrapper.findAll('.dashboard-page__card')).toHaveLength(6)
    })

    it('handles unknown error type gracefully', async () => {
      mockGetStatistics.mockRejectedValue('未知错误字符串')

      const { wrapper } = await mountPage()

      expect(wrapper.findComponent({ name: 'ElAlert' }).exists()).toBe(true)
      expect(wrapper.text()).toContain('加载统计数据失败')
    })

    it('does not show loading after data is fetched', async () => {
      mockGetStatistics.mockResolvedValue({ data: makeStats() })

      const { wrapper } = await mountPage()

      expect(wrapper.findComponent({ name: 'LoadingSkeleton' }).exists()).toBe(false)
    })

    it('displays stat values with locale formatting for large numbers', async () => {
      mockGetStatistics.mockResolvedValue({
        data: makeStats({ userCount: 1000000 }),
      })

      const { wrapper } = await mountPage()

      expect(wrapper.text()).toContain('1,000,000')
    })
  })
})
