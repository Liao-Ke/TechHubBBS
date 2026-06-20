import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import type { AdminCommentItem } from '@/api/types'

// ── Mock adminApi ──
const mockGetComments = vi.fn<(...args: unknown[]) => unknown>()
const mockSetDivine = vi.fn<(...args: unknown[]) => unknown>()

vi.mock('@/api/modules/admin', () => ({
  adminApi: {
    getComments: (...args: unknown[]) => mockGetComments(...args),
    setDivine: (...args: unknown[]) => mockSetDivine(...args),
  },
}))

// ── Mock Element Plus ──
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: { success: vi.fn<(...args: unknown[]) => unknown>(), error: vi.fn<(...args: unknown[]) => unknown>(), warning: vi.fn<(...args: unknown[]) => unknown>() },
    ElMessageBox: { confirm: vi.fn<(...args: unknown[]) => unknown>() },
  }
})

// ── Mock LoadingSkeleton ──
vi.mock('@/components/common/LoadingSkeleton.vue', () => ({
  default: {
    name: 'LoadingSkeleton',
    props: ['variant'],
    template: '<div class="loading-skeleton-stub" />',
  },
}))

// ── Mock EmptyState ──
vi.mock('@/components/common/EmptyState.vue', () => ({
  default: {
    name: 'EmptyState',
    props: ['icon', 'title', 'description', 'actionText', 'actionRoute'],
    template: '<div class="empty-state-stub"><h3>{{ title }}</h3></div>',
  },
}))

// ── Mock formatRelativeTime ──
vi.mock('@/utils/format', () => ({
  formatRelativeTime: vi.fn(() => '3天前'),
  formatNumber: vi.fn((n: number) => String(n)),
}))

import DivineManagePage from '@/pages/admin/DivineManagePage.vue'
import { ElMessageBox } from 'element-plus'

// ── Test Data ──
function makeComment(overrides: Partial<AdminCommentItem> = {}): AdminCommentItem {
  return {
    id: overrides.id ?? 'c1',
    postId: overrides.postId ?? 'p1',
    content: overrides.content ?? '这是一条测试评论内容',
    username: overrides.username ?? 'testuser',
    avatarUrl: overrides.avatarUrl ?? '',
    userId: overrides.userId ?? 'u1',
    postTitle: overrides.postTitle ?? '测试帖子标题',
    likeCount: overrides.likeCount ?? 5,
    recommendCount: overrides.recommendCount ?? 3,
    isDivine: overrides.isDivine ?? false,
    divineTime: overrides.divineTime ?? null,
    createTime: overrides.createTime ?? '2026-05-20T10:00:00',
  }
}

const mockComments: AdminCommentItem[] = [
  makeComment({ id: 'c1', content: '评论一', username: '用户A' }),
  makeComment({
    id: 'c2',
    content: '评论二',
    username: '用户B',
    isDivine: true,
    divineTime: '2026-05-19T08:00:00',
    postTitle: '神评帖子',
  }),
]

// ── Helpers ──
function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/admin/divine', name: 'admin-divine', component: DivineManagePage },
      { path: '/posts/:id', name: 'post-detail', component: { template: '<div>post</div>' } },
    ],
  })
}

async function mountPage() {
  const router = createTestRouter()
  const pinia = createPinia()
  setActivePinia(pinia)

  await router.push('/admin/divine')
  await router.isReady()

  const wrapper = mount(DivineManagePage, {
    global: {
      plugins: [router, pinia],
      stubs: {
        teleport: true,
        ElPagination: { template: '<div class="el-pagination-stub" />' },
      },
    },
  })

  await flushPromises()
  await nextTick()
  return { wrapper, router }
}

describe('DivineManagePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetComments.mockResolvedValue({
      data: {
        records: mockComments.map(c => ({ ...c })),
        total: 2,
        size: 20,
        current: 1,
        pages: 1,
      },
    })
    mockSetDivine.mockResolvedValue({ data: null })
  })

  // ── Rendering ──
  describe('rendering', () => {
    it('renders title "神评管理"', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.text()).toContain('神评管理')
    })

    it('is not a stub (has real content)', async () => {
      const { wrapper } = await mountPage()
      const text = wrapper.text()
      expect(text.length).toBeGreaterThan(20)
    })

    it('renders search input', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.find('.divine-manage__search-input').exists()).toBe(true)
    })

    it('renders search and reset buttons', async () => {
      const { wrapper } = await mountPage()
      const searchBar = wrapper.find('.divine-manage__search')
      expect(searchBar.text()).toContain('搜索')
      expect(searchBar.text()).toContain('重置')
    })
  })

  // ── Loading State ──
  describe('loading state', () => {
    it('shows LoadingSkeleton while fetching', async () => {
      mockGetComments.mockReturnValue(new Promise(() => {}))
      const { wrapper } = await mountPage()
      expect(wrapper.findComponent({ name: 'LoadingSkeleton' }).exists()).toBe(true)
    })
  })

  // ── Error State ──
  describe('error state', () => {
    it('shows error alert when API fails', async () => {
      mockGetComments.mockRejectedValue(new Error('Network error'))
      const { wrapper } = await mountPage()
      expect(wrapper.findComponent({ name: 'ElAlert' }).exists()).toBe(true)
      expect(wrapper.text()).toContain('重试')
    })
  })

  // ── Empty State ──
  describe('empty state', () => {
    it('shows EmptyState when no comments', async () => {
      mockGetComments.mockResolvedValue({
        data: { records: [], total: 0, size: 20, current: 1, pages: 0 },
      })
      const { wrapper } = await mountPage()
      expect(wrapper.findComponent({ name: 'EmptyState' }).exists()).toBe(true)
      expect(wrapper.text()).toContain('暂无评论数据')
    })
  })

  // ── Table ──
  describe('table rendering', () => {
    it('renders el-table with comment data', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.find('.el-table').exists()).toBe(true)
    })

    it('displays comment content in table', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.text()).toContain('评论一')
      expect(wrapper.text()).toContain('评论二')
    })

    it('displays author username', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.text()).toContain('用户A')
      expect(wrapper.text()).toContain('用户B')
    })

    it('displays post title as link', async () => {
      const { wrapper } = await mountPage()
      const links = wrapper.findAll('.divine-manage__post-link')
      expect(links.length).toBeGreaterThanOrEqual(1)
      expect(links.some((l) => l.text().includes('测试帖子标题'))).toBe(true)
    })

    it('displays likeCount and recommendCount', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.text()).toContain('5')
      expect(wrapper.text()).toContain('3')
    })

    it('shows divine tag for divine comments', async () => {
      const { wrapper } = await mountPage()
      const divineTags = wrapper.findAll('.divine-manage__divine-tag')
      expect(divineTags.length).toBe(1)
      expect(divineTags[0]!.text()).toContain('神评')
    })

    it('shows 设为神评 button for non-divine comments', async () => {
      const { wrapper } = await mountPage()
      const setBtns = wrapper.findAll('.divine-manage__actions .el-button--warning')
      expect(setBtns.length).toBe(1)
      expect(setBtns[0]!.text()).toContain('设为神评')
    })

    it('shows 撤销神评 button for divine comments', async () => {
      const { wrapper } = await mountPage()
      const text = wrapper.text()
      expect(text).toContain('撤销神评')
    })
  })

  // ── Divine Actions ──
  describe('divine actions', () => {
    it('calls setDivine with true when 设为神评 clicked and confirmed', async () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
      vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm' as any)
      const { wrapper } = await mountPage()

      const setBtn = wrapper.find('.divine-manage__actions .el-button--warning')
      await setBtn.trigger('click')
      await flushPromises()
      await nextTick()

      expect(ElMessageBox.confirm).toHaveBeenCalled()
      expect(mockSetDivine).toHaveBeenCalledWith('c1', true)
    })

    it('does not call setDivine when confirm is cancelled', async () => {
      vi.mocked(ElMessageBox.confirm).mockRejectedValue(new Error('cancel'))
      mockSetDivine.mockClear()
      const { wrapper } = await mountPage()

      const setBtn = wrapper.find('.divine-manage__actions .el-button--warning')
      await setBtn.trigger('click')
      await flushPromises()
      await nextTick()

      expect(mockSetDivine).not.toHaveBeenCalled()
    })
  })

  // ── Search ──
  describe('search', () => {
    it('fetches comments with keyword when search triggered', async () => {
      const { wrapper } = await mountPage()
      // First call is on mount
      expect(mockGetComments).toHaveBeenCalledTimes(1)

      const input = wrapper.find('.divine-manage__search-input input')
      await input.setValue('测试关键词')
      await wrapper.find('.divine-manage__search .el-button--primary').trigger('click')
      await flushPromises()
      await nextTick()

      expect(mockGetComments).toHaveBeenCalledTimes(2)
      expect(mockGetComments).toHaveBeenLastCalledWith({
        page: 1,
        size: 20,
        keyword: '测试关键词',
      })
    })

    it('resets search and refetches on reset button', async () => {
      const { wrapper } = await mountPage()

      const input = wrapper.find('.divine-manage__search-input input')
      await input.setValue('test')
      await wrapper.find('.divine-manage__search .el-button:last-child').trigger('click')
      await flushPromises()
      await nextTick()

      expect(mockGetComments).toHaveBeenLastCalledWith({
        page: 1,
        size: 20,
      })
    })
  })

  // ── Pagination ──
  describe('pagination', () => {
    it('shows pagination when total > pageSize', async () => {
      mockGetComments.mockResolvedValue({
        data: {
          records: mockComments,
          total: 50,
          size: 20,
          current: 1,
          pages: 3,
        },
      })
      const { wrapper } = await mountPage()
      expect(wrapper.find('.divine-manage__pagination').exists()).toBe(true)
    })

    it('hides pagination when total <= pageSize', async () => {
      const { wrapper } = await mountPage()
      // total = 2, pageSize = 20 -> should not show pagination
      expect(wrapper.find('.divine-manage__pagination').exists()).toBe(false)
    })
  })

  // ── Integration ──
  describe('integration', () => {
    it('fetches comments on mount', async () => {
      await mountPage()
      expect(mockGetComments).toHaveBeenCalledWith({
        page: 1,
        size: 20,
      })
    })
  })
})
