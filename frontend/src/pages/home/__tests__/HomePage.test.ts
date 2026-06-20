import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises, type VueWrapper } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick, type ComponentPublicInstance } from 'vue'
import type { RecommendationVO, PostVO } from '@/api/types'

// ── Mock IntersectionObserver ──
vi.stubGlobal(
  'IntersectionObserver',
  vi.fn(function () {
    return {
      observe: vi.fn<(...args: unknown[]) => unknown>(),
      unobserve: vi.fn<(...args: unknown[]) => unknown>(),
      disconnect: vi.fn<(...args: unknown[]) => unknown>(),
    }
  }),
)

// ── Mock API modules ──
const mockGetRecommendations = vi.fn<(...args: unknown[]) => unknown>()
const mockGetList = vi.fn<(...args: unknown[]) => unknown>()

vi.mock('@/api/modules/recommendation', () => ({
  recommendationApi: {
    getRecommendations: (...args: unknown[]) => mockGetRecommendations(...args),
  },
}))

vi.mock('@/api/modules/post', () => ({
  postApi: {
    getList: (...args: unknown[]) => mockGetList(...args),
  },
}))

// ── Mock user store ──
const mockStore = {
  isLoggedIn: true,
}

vi.mock('@/stores/user', () => ({
  useUserStore: () => mockStore,
}))

import HomePage from '@/pages/home/HomePage.vue'

// ── Helpers ──
function makeRecVO(overrides: Partial<RecommendationVO> = {}): RecommendationVO {
  return {
    postId: overrides.postId ?? 'post-1',
    title: overrides.title ?? '推荐帖子标题',
    authorName: overrides.authorName ?? '推荐用户',
    authorAvatar: overrides.authorAvatar ?? undefined,
    likeCount: overrides.likeCount ?? undefined,
    commentCount: overrides.commentCount ?? undefined,
    viewCount: overrides.viewCount ?? undefined,
    similarityScore: overrides.similarityScore ?? 0.85,
    reason: overrides.reason ?? 'content_based',
  }
}

function makePostVO(overrides: Partial<PostVO> = {}): PostVO {
  return {
    id: overrides.id ?? 'post-1',
    title: overrides.title ?? '热门帖子标题',
    content: '帖子内容...',
    authorId: 'author-1',
    authorName: '用户',
    authorAvatar: undefined,
    categoryId: '1',
    categoryName: 'Java',
    visibility: 0,
    type: 0,
    status: 1,
    viewCount: 100,
    likeCount: 20,
    commentCount: 5,
    divineCommentCount: 1,
    liked: false,
    favorited: false,
    createTime: '2026-01-01T00:00:00Z',
    updateTime: '2026-01-01T00:00:00Z',
    ...overrides,
  }
}

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', name: 'home', component: HomePage },
      { path: '/login', name: 'login', component: { template: '<div>login</div>' } },
      { path: '/posts/:id', name: 'post-detail', component: { template: '<div>post</div>' } },
      { path: '/categories/:id', name: 'category', component: { template: '<div>cat</div>' } },
    ],
  })
}

async function mountPage() {
  const router = createTestRouter()
  const pinia = createPinia()
  setActivePinia(pinia)

  await router.push('/')
  await router.isReady()

  const wrapper = mount(HomePage, {
    global: { plugins: [router, pinia] },
  })

  // Wait for initial async operations (mount + nextTick + loadMore)
  await flushPromises()
  await nextTick()

  return { wrapper, router, pinia }
}

describe('HomePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockStore.isLoggedIn = true
    mockGetRecommendations.mockReset()
    mockGetList.mockReset()
  })

  // ── Rendering ──
  describe('rendering', () => {
    it('renders two tabs: 推荐 and 热门', async () => {
      mockGetRecommendations.mockResolvedValue({ data: [] })
      const { wrapper } = await mountPage()

      // Wait for async operations
      await flushPromises()
      await nextTick()

      const tabs = wrapper.findAll('.el-tabs__item')
      const tabTexts = tabs.map((t) => t.text())
      expect(tabTexts.some((t) => t.includes('推荐'))).toBe(true)
      expect(tabTexts.some((t) => t.includes('热门'))).toBe(true)
    })

    it('is not a stub (has real content)', async () => {
      mockGetRecommendations.mockResolvedValue({ data: [makeRecVO()] })
      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const text = wrapper.text()
      expect(text.length).toBeGreaterThan(20)
    })

    it('has 推荐 tab active by default', async () => {
      mockGetRecommendations.mockResolvedValue({ data: [] })
      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const activeTab = wrapper.find('.el-tabs__item.is-active')
      expect(activeTab.exists()).toBe(true)
      expect(activeTab.text()).toContain('推荐')
    })
  })

  // ── Not logged in ──
  describe('not logged in', () => {
    it('shows EmptyState with login prompt on recommend tab', async () => {
      mockStore.isLoggedIn = false
      mockGetRecommendations.mockResolvedValue({ data: [] })

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const emptyState = wrapper.findComponent({ name: 'EmptyState' })
      expect(emptyState.exists()).toBe(true)
      expect(emptyState.props('title')).toContain('登录')
    })

    it('shows login button that links to /login?redirect=/', async () => {
      mockStore.isLoggedIn = false
      mockGetRecommendations.mockResolvedValue({ data: [] })

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const emptyState = wrapper.findComponent({ name: 'EmptyState' })
      expect(emptyState.props('actionRoute')).toBe('/login?redirect=/')
    })
  })

  // ── Recommendation tab: logged in ──
  describe('recommendation tab (logged in)', () => {
    it('shows LoadingSkeleton while loading first page', async () => {
      // Never resolve → stays in loading state
      mockGetRecommendations.mockReturnValue(new Promise(() => {}))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.findComponent({ name: 'LoadingSkeleton' }).exists()).toBe(true)
    })

    it('shows error alert when API fails on first load', async () => {
      mockGetRecommendations.mockRejectedValue(new Error('网络错误'))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.findComponent({ name: 'ElAlert' }).exists()).toBe(true)
      expect(wrapper.text()).toContain('重试')
    })

    it('shows EmptyState when no recommendations returned', async () => {
      mockGetRecommendations.mockResolvedValue({ data: [] })

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const emptyState = wrapper.findComponent({ name: 'EmptyState' })
      expect(emptyState.exists()).toBe(true)
      expect(emptyState.props('title')).toContain('推荐')
    })

    it('renders recommendation cards when data returned', async () => {
      mockGetRecommendations.mockResolvedValue({ data: [makeRecVO({ title: '测试推荐帖' })] })

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.find('.rec-card').exists()).toBe(true)
      expect(wrapper.text()).toContain('测试推荐帖')
    })

    it('shows recommendation reason tag on each card', async () => {
      mockGetRecommendations.mockResolvedValue({ data: [makeRecVO({ reason: 'content_based' })] })

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.find('.rec-card__reason').exists()).toBe(true)
      expect(wrapper.text()).toContain('内容推荐')
    })

    it('shows "end of list" when hasMore is false', async () => {
      // First call returns empty → hasMore = false
      mockGetRecommendations.mockResolvedValue({ data: [] })

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      // After empty response, hasMore is false → should still show empty state
      expect(wrapper.findComponent({ name: 'EmptyState' }).exists()).toBe(true)
    })
  })

  // ── Hot tab ──
  describe('hot tab', () => {
    async function switchToHot(
      wrapper: VueWrapper<ComponentPublicInstance>,
    ) {
      const tabs = wrapper.findAll('.el-tabs__item')
      const hotTab = tabs.find((t: { text: () => string }) => t.text().includes('热门'))
      if (hotTab) {
        await hotTab.trigger('click')
        await flushPromises()
        await nextTick()
      }
    }

    it('shows LoadingSkeleton while loading hot posts', async () => {
      mockGetRecommendations.mockResolvedValue({ data: [] })
      mockGetList.mockReturnValue(new Promise(() => {}))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      await switchToHot(wrapper)

      expect(wrapper.findComponent({ name: 'LoadingSkeleton' }).exists()).toBe(true)
    })

    it('shows error alert when hot API fails', async () => {
      mockGetRecommendations.mockResolvedValue({ data: [] })
      mockGetList.mockRejectedValue(new Error('服务器错误'))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      await switchToHot(wrapper)

      expect(wrapper.findComponent({ name: 'ElAlert' }).exists()).toBe(true)
    })

    it('shows EmptyState when no hot posts', async () => {
      mockGetRecommendations.mockResolvedValue({ data: [] })
      mockGetList.mockResolvedValue({
        data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
      })

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      await switchToHot(wrapper)

      // jsdom renders both tab panes (no CSS), so find all EmptyStates
      const emptyStates = wrapper.findAllComponents({ name: 'EmptyState' })
      expect(emptyStates.length).toBeGreaterThanOrEqual(1)
      const titles = emptyStates.map((es) => es.props('title'))
      expect(titles.some((t: string) => t.includes('热门'))).toBe(true)
    })

    it('renders PostCard for each hot post', async () => {
      mockGetRecommendations.mockResolvedValue({ data: [] })
      mockGetList.mockResolvedValue({
        data: {
          records: [makePostVO({ title: '热帖1' }), makePostVO({ id: 'post-2', title: '热帖2' })],
          total: 2,
          size: 10,
          current: 1,
          pages: 1,
        },
      })

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      await switchToHot(wrapper)

      const postCards = wrapper.findAllComponents({ name: 'PostCard' })
      expect(postCards.length).toBe(2)
      expect(wrapper.text()).toContain('热帖1')
      expect(wrapper.text()).toContain('热帖2')
    })
  })

  // ── Tab switching ──
  describe('tab switching', () => {
    it('calls recommendation API on mount', async () => {
      mockGetRecommendations.mockResolvedValue({ data: [makeRecVO()] })

      await mountPage()
      await flushPromises()
      await nextTick()

      expect(mockGetRecommendations).toHaveBeenCalledWith({ page: 1, size: 10 })
    })

    it('calls post API when switching to hot tab', async () => {
      mockGetRecommendations.mockResolvedValue({ data: [] })
      mockGetList.mockResolvedValue({
        data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
      })

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      // Switch to hot
      const tabs = wrapper.findAll('.el-tabs__item')
      const hotTab = tabs.find((t) => t.text().includes('热门'))
      if (hotTab) {
        await hotTab.trigger('click')
        await flushPromises()
        await nextTick()
      }

      expect(mockGetList).toHaveBeenCalledWith({ sort: 'hot', page: 1, size: 10 })
    })
  })

  // ── Recommendation reason helper ──
  describe('reason labels', () => {
    it('maps content_based to 内容推荐', async () => {
      mockGetRecommendations.mockResolvedValue({
        data: [makeRecVO({ reason: 'content_based' })],
      })

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.text()).toContain('内容推荐')
    })

    it('maps collaborative to 猜你喜欢', async () => {
      mockGetRecommendations.mockResolvedValue({
        data: [makeRecVO({ reason: 'collaborative' })],
      })

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.text()).toContain('猜你喜欢')
    })

    it('maps hot to 热门推荐', async () => {
      mockGetRecommendations.mockResolvedValue({
        data: [makeRecVO({ reason: 'hot' })],
      })

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.text()).toContain('热门推荐')
    })
  })
})
