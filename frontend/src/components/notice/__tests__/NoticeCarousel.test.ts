import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import NoticeCarousel from '../NoticeCarousel.vue'
import type { CategoryNoticeVO } from '@/api/types'

// ── Mock noticeApi ──
const mockGetList = vi.fn()
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
    isPinned: 1,
    status: 1,
    createTime: '2026-05-28T10:00:00',
    updateTime: '2026-05-28T10:00:00',
    ...overrides,
  }
}

function mountCarousel() {
  return mount(NoticeCarousel, {
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

describe('NoticeCarousel.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // ── Empty state ──
  describe('empty state', () => {
    it('renders nothing when no notices returned', async () => {
      mockGetList.mockResolvedValue({ data: { records: [] } })
      const wrapper = mountCarousel()
      await flushPromises()
      expect(wrapper.find('.notice-carousel').exists()).toBe(false)
    })

    it('renders nothing when only non-pinned notices exist', async () => {
      mockGetList.mockResolvedValue({
        data: {
          records: [
            makeNotice({ id: 'n1', isPinned: 0 }),
            makeNotice({ id: 'n2', isPinned: 0 }),
          ],
        },
      })
      const wrapper = mountCarousel()
      await flushPromises()
      expect(wrapper.find('.notice-carousel').exists()).toBe(false)
    })

    it('calls noticeApi.getList without categoryId', async () => {
      mockGetList.mockResolvedValue({ data: { records: [] } })
      mountCarousel()
      await flushPromises()
      expect(mockGetList).toHaveBeenCalledWith()
    })
  })

  // ── Single pinned → static ──
  describe('single pinned notice (static)', () => {
    it('renders static display for single pinned notice', async () => {
      mockGetList.mockResolvedValue({
        data: { records: [makeNotice({ id: 'n1', title: '全局公告', isPinned: 1 })] },
      })
      const wrapper = mountCarousel()
      await flushPromises()

      expect(wrapper.find('.notice-carousel--single').exists()).toBe(true)
      expect(wrapper.find('.el-carousel').exists()).toBe(false)
      expect(wrapper.text()).toContain('全局公告')
    })

    it('shows type tag', async () => {
      mockGetList.mockResolvedValue({
        data: { records: [makeNotice({ type: 0, isPinned: 1 })] },
      })
      const wrapper = mountCarousel()
      await flushPromises()

      expect(wrapper.text()).toContain('须知')
    })

    it('renders title as link', async () => {
      mockGetList.mockResolvedValue({
        data: { records: [makeNotice({ id: 'n99', title: '公告链接', isPinned: 1 })] },
      })
      const wrapper = mountCarousel()
      await flushPromises()

      const link = wrapper.find('.notice-carousel__link')
      expect(link.text()).toBe('公告链接')
      expect(link.attributes('href')).toBe('/notices/n99')
    })
  })

  // ── Multiple pinned → carousel ──
  describe('multiple pinned notices (carousel)', () => {
    it('renders carousel for multiple pinned notices', async () => {
      mockGetList.mockResolvedValue({
        data: {
          records: [
            makeNotice({ id: 'n1', title: '公告A', isPinned: 1 }),
            makeNotice({ id: 'n2', title: '公告B', isPinned: 1 }),
          ],
        },
      })
      const wrapper = mountCarousel()
      await flushPromises()

      expect(wrapper.find('.el-carousel').exists()).toBe(true)
      expect(wrapper.find('.notice-carousel--single').exists()).toBe(false)
    })

    it('renders each pinned notice as slide', async () => {
      mockGetList.mockResolvedValue({
        data: {
          records: [
            makeNotice({ id: 'n1', title: '公告A', isPinned: 1 }),
            makeNotice({ id: 'n2', title: '公告B', isPinned: 1 }),
          ],
        },
      })
      const wrapper = mountCarousel()
      await flushPromises()

      const slides = wrapper.findAll('.el-carousel-item')
      expect(slides.length).toBe(2)
    })

    it('filters out non-pinned notices', async () => {
      mockGetList.mockResolvedValue({
        data: {
          records: [
            makeNotice({ id: 'n1', title: '置顶A', isPinned: 1 }),
            makeNotice({ id: 'n2', title: '普通公告', isPinned: 0 }),
            makeNotice({ id: 'n3', title: '置顶B', isPinned: 1 }),
          ],
        },
      })
      const wrapper = mountCarousel()
      await flushPromises()

      const slides = wrapper.findAll('.el-carousel-item')
      expect(slides.length).toBe(2)
      expect(wrapper.text()).toContain('置顶A')
      expect(wrapper.text()).toContain('置顶B')
      expect(wrapper.text()).not.toContain('普通公告')
    })
  })

  // ── Type tag display ──
  describe('type tags', () => {
    it('shows 须知 for type=0', async () => {
      mockGetList.mockResolvedValue({
        data: { records: [makeNotice({ type: 0, isPinned: 1 })] },
      })
      const wrapper = mountCarousel()
      await flushPromises()
      expect(wrapper.text()).toContain('须知')
    })

    it('shows 活动 for type=1', async () => {
      mockGetList.mockResolvedValue({
        data: { records: [makeNotice({ type: 1, isPinned: 1 })] },
      })
      const wrapper = mountCarousel()
      await flushPromises()
      expect(wrapper.text()).toContain('活动')
    })
  })
})
