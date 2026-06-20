import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import NoticeBanner from '../NoticeBanner.vue'
import type { CategoryNoticeVO } from '@/api/types'

// ── Mock noticeApi ──
const mockGetList = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('@/api/modules/notice', () => ({
  noticeApi: {
    getList: (...args: unknown[]) => mockGetList(...args),
  },
}))

function makeNotice(overrides: Partial<CategoryNoticeVO> = {}): CategoryNoticeVO {
  return {
    id: 'n1',
    categoryId: '1',
    title: '测试公告',
    content: '公告内容',
    type: 0,
    authorId: 'u1',
    authorName: 'admin',
    isPinned: 0,
    status: 1,
    createTime: '2026-05-28T10:00:00',
    updateTime: '2026-05-28T10:00:00',
    ...overrides,
  }
}

function mountBanner(categoryId = '1') {
  return mount(NoticeBanner, {
    props: { categoryId },
    global: {
      stubs: {
        'router-link': {
          template: '<a :href="to"><slot /></a>',
          props: ['to'],
        },
        'el-carousel': {
          template: '<div class="el-carousel"><slot /></div>',
        },
        'el-carousel-item': {
          template: '<div class="el-carousel-item"><slot /></div>',
        },
      },
    },
  })
}

describe('NoticeBanner.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // ── Empty state ──
  describe('empty state', () => {
    it('renders nothing when no notices returned', async () => {
      mockGetList.mockResolvedValue({ data: [] })
      const wrapper = mountBanner()
      await flushPromises()
      // The root element should be a comment node or empty div
      expect(wrapper.find('.notice-banner').exists()).toBe(false)
    })

    it('calls noticeApi.getList with correct categoryId', async () => {
      mockGetList.mockResolvedValue({ data: [] })
      mountBanner('cat-42')
      await flushPromises()
      expect(mockGetList).toHaveBeenCalledWith({ categoryId: 'cat-42' })
    })
  })

  // ── Single notice ──
  describe('single notice (static display)', () => {
    it('renders static display for a single notice', async () => {
      mockGetList.mockResolvedValue({
        data: [
          makeNotice({ id: 'n1', title: '唯一公告', isPinned: 1, type: 0 }),
        ],
      })
      const wrapper = mountBanner()
      await flushPromises()

      expect(wrapper.find('.notice-banner').exists()).toBe(true)
      expect(wrapper.find('.notice-banner__single').exists()).toBe(true)
      expect(wrapper.find('.notice-banner__carousel').exists()).toBe(false)
      expect(wrapper.text()).toContain('唯一公告')
    })

    it('shows 须知 tag for type=0', async () => {
      mockGetList.mockResolvedValue({
        data: [makeNotice({ type: 0, isPinned: 1 })],
      })
      const wrapper = mountBanner()
      await flushPromises()

      expect(wrapper.text()).toContain('须知')
      expect(wrapper.find('.el-tag--info').exists() || wrapper.find('.notice-banner__tag').exists()).toBe(true)
    })

    it('shows 活动 tag for type=1', async () => {
      mockGetList.mockResolvedValue({
        data: [makeNotice({ type: 1, isPinned: 1 })],
      })
      const wrapper = mountBanner()
      await flushPromises()

      expect(wrapper.text()).toContain('活动')
    })

    it('renders title as link', async () => {
      mockGetList.mockResolvedValue({
        data: [makeNotice({ id: 'n42', title: '公告标题', isPinned: 1 })],
      })
      const wrapper = mountBanner()
      await flushPromises()

      const link = wrapper.find('.notice-banner__title')
      expect(link.text()).toBe('公告标题')
      expect(link.attributes('href')).toBe('/notices/n42')
    })
  })

  // ── Multiple pinned → carousel ──
  describe('multiple pinned notices (carousel)', () => {
    it('renders carousel when >1 pinned notice', async () => {
      mockGetList.mockResolvedValue({
        data: [
          makeNotice({ id: 'n1', title: '公告1', isPinned: 1 }),
          makeNotice({ id: 'n2', title: '公告2', isPinned: 1 }),
        ],
      })
      const wrapper = mountBanner()
      await flushPromises()

      expect(wrapper.find('.notice-banner__carousel').exists()).toBe(true)
      expect(wrapper.find('.notice-banner__single').exists()).toBe(false)
    })

    it('renders each pinned notice as a carousel slide', async () => {
      mockGetList.mockResolvedValue({
        data: [
          makeNotice({ id: 'n1', title: '公告1', isPinned: 1 }),
          makeNotice({ id: 'n2', title: '公告2', isPinned: 1 }),
        ],
      })
      const wrapper = mountBanner()
      await flushPromises()

      const slides = wrapper.findAll('.el-carousel-item')
      expect(slides.length).toBe(2)
      expect(wrapper.text()).toContain('公告1')
      expect(wrapper.text()).toContain('公告2')
    })
  })

  // ── Non-pinned list ──
  describe('non-pinned notices list', () => {
    it('renders non-pinned notices in a list', async () => {
      mockGetList.mockResolvedValue({
        data: [
          makeNotice({ id: 'n1', title: '置顶公告', isPinned: 1 }),
          makeNotice({ id: 'n2', title: '普通公告', isPinned: 0 }),
          makeNotice({ id: 'n3', title: '普通公告2', isPinned: 0 }),
        ],
      })
      const wrapper = mountBanner()
      await flushPromises()

      expect(wrapper.find('.notice-banner__list').exists()).toBe(true)
      const items = wrapper.findAll('.notice-banner__item')
      expect(items.length).toBe(2)
    })

    it('shows only first 3 non-pinned by default', async () => {
      const nonPinned = Array.from({ length: 5 }, (_, i) =>
        makeNotice({ id: `n${i + 1}`, title: `公告${i + 1}`, isPinned: 0 })
      )
      mockGetList.mockResolvedValue({ data: nonPinned })
      const wrapper = mountBanner()
      await flushPromises()

      const items = wrapper.findAll('.notice-banner__item')
      expect(items.length).toBe(3)
    })

    it('shows toggle button when non-pinned > 3', async () => {
      const nonPinned = Array.from({ length: 5 }, (_, i) =>
        makeNotice({ id: `n${i + 1}`, title: `公告${i + 1}`, isPinned: 0 })
      )
      mockGetList.mockResolvedValue({ data: nonPinned })
      const wrapper = mountBanner()
      await flushPromises()

      const toggle = wrapper.find('.notice-banner__toggle')
      expect(toggle.exists()).toBe(true)
      expect(toggle.text()).toContain('查看全部 5 条公告')
    })

    it('expands to show all non-pinned on toggle click', async () => {
      const nonPinned = Array.from({ length: 5 }, (_, i) =>
        makeNotice({ id: `n${i + 1}`, title: `公告${i + 1}`, isPinned: 0 })
      )
      mockGetList.mockResolvedValue({ data: nonPinned })
      const wrapper = mountBanner()
      await flushPromises()

      await wrapper.find('.notice-banner__toggle').trigger('click')
      const items = wrapper.findAll('.notice-banner__item')
      expect(items.length).toBe(5)
      expect(wrapper.find('.notice-banner__toggle').text()).toContain('收起')
    })

    it('collapses back on second toggle click', async () => {
      const nonPinned = Array.from({ length: 5 }, (_, i) =>
        makeNotice({ id: `n${i + 1}`, title: `公告${i + 1}`, isPinned: 0 })
      )
      mockGetList.mockResolvedValue({ data: nonPinned })
      const wrapper = mountBanner()
      await flushPromises()

      const toggle = wrapper.find('.notice-banner__toggle')
      await toggle.trigger('click') // expand
      await toggle.trigger('click') // collapse
      const items = wrapper.findAll('.notice-banner__item')
      expect(items.length).toBe(3)
    })

    it('does not show toggle when non-pinned <= 3', async () => {
      const nonPinned = [
        makeNotice({ id: 'n1', title: 'A', isPinned: 0 }),
        makeNotice({ id: 'n2', title: 'B', isPinned: 0 }),
        makeNotice({ id: 'n3', title: 'C', isPinned: 0 }),
      ]
      mockGetList.mockResolvedValue({ data: nonPinned })
      const wrapper = mountBanner()
      await flushPromises()

      expect(wrapper.find('.notice-banner__toggle').exists()).toBe(false)
    })
  })

  // ── Single pinned + non-pinned (static, no carousel) ──
  describe('single pinned with non-pinned', () => {
    it('uses static display for 1 pinned + non-pinned', async () => {
      mockGetList.mockResolvedValue({
        data: [
          makeNotice({ id: 'n1', title: '唯一置顶', isPinned: 1 }),
          makeNotice({ id: 'n2', title: '普通', isPinned: 0 }),
        ],
      })
      const wrapper = mountBanner()
      await flushPromises()

      expect(wrapper.find('.notice-banner__single').exists()).toBe(true)
      expect(wrapper.find('.notice-banner__carousel').exists()).toBe(false)
      expect(wrapper.find('.notice-banner__list').exists()).toBe(true)
    })
  })

  // ── Reactivity: categoryId change ──
  describe('categoryId reactivity', () => {
    it('re-fetches when categoryId changes', async () => {
      mockGetList.mockResolvedValue({ data: [] })
      const wrapper = mountBanner('cat-1')
      await flushPromises()

      expect(mockGetList).toHaveBeenCalledWith({ categoryId: 'cat-1' })

      await wrapper.setProps({ categoryId: 'cat-2' })
      await flushPromises()

      expect(mockGetList).toHaveBeenCalledWith({ categoryId: 'cat-2' })
    })
  })
})
