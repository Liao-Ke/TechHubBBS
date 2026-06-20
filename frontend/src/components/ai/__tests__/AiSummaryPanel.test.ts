import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'

// ── Mock API modules ──
const mockGetSummary = vi.fn<(...args: unknown[]) => unknown>()
const mockGenerateSummary = vi.fn<(...args: unknown[]) => unknown>()

vi.mock('@/api/modules/ai', () => ({
  aiApi: {
    getSummary: (...args: unknown[]) => mockGetSummary(...args),
    generateSummary: (...args: unknown[]) => mockGenerateSummary(...args),
    askQuestion: vi.fn<(...args: unknown[]) => unknown>(),
    getQaHistory: vi.fn<(...args: unknown[]) => unknown>(),
  },
}))

// ── Mock user store ──
const mockStore = {
  isLoggedIn: false,
}

vi.mock('@/stores/user', () => ({
  useUserStore: () => mockStore,
}))

import AiSummaryPanel from '../AiSummaryPanel.vue'
import type { AiSummaryResponse } from '@/api/types'

function makeSummaryResponse(overrides: Partial<AiSummaryResponse> = {}): AiSummaryResponse {
  return {
    id: 'summary-1',
    postId: 'post-1',
    content: null,
    status: 0,
    createTime: '2026-05-28T10:00:00Z',
    updateTime: '2026-05-28T10:00:00Z',
    ...overrides,
  }
}

function mountPanel(props: { postId?: string; postContent?: string } = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)

  return mount(AiSummaryPanel, {
    props: {
      postId: 'post-1',
      postContent: 'A'.repeat(100),
      ...props,
    },
    global: {
      plugins: [pinia],
      stubs: {
        'router-link': {
          template: '<a :href="to"><slot /></a>',
          props: ['to'],
        },
      },
    },
  })
}

describe('AiSummaryPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockStore.isLoggedIn = false
  })

  // ── Content too short ──
  describe('content too short', () => {
    it('shows warning when postContent < 50 chars', async () => {
      const wrapper = mountPanel({ postContent: 'short' })
      await nextTick()
      expect(wrapper.text()).toContain('帖子内容过短')
      expect(wrapper.text()).toContain('暂不支持 AI 总结')
    })

    it('does not call getSummary when content is too short', () => {
      mountPanel({ postContent: 'short' })
      expect(mockGetSummary).not.toHaveBeenCalled()
    })
  })

  // ── Not logged in ──
  describe('not logged in', () => {
    it('shows login prompt when user is not logged in', async () => {
      mockStore.isLoggedIn = false
      const wrapper = mountPanel()
      await nextTick()
      expect(wrapper.text()).toContain('登录后使用 AI 智能总结')
    })

    it('renders login link', async () => {
      mockStore.isLoggedIn = false
      const wrapper = mountPanel()
      await nextTick()
      const link = wrapper.find('a')
      expect(link.exists()).toBe(true)
      expect(link.attributes('href')).toBe('/login')
    })

    it('does not call getSummary when not logged in', () => {
      mockStore.isLoggedIn = false
      mountPanel()
      expect(mockGetSummary).not.toHaveBeenCalled()
    })
  })

  // ── Existing summary check ──
  describe('existing summary check', () => {
    it('calls getSummary on mount when logged in and content ok', () => {
      mockStore.isLoggedIn = true
      mockGetSummary.mockResolvedValue({
        code: 200,
        message: 'success',
        data: makeSummaryResponse({ status: 0 }),
      })

      mountPanel()
      expect(mockGetSummary).toHaveBeenCalledWith('post-1')
    })

    it('shows idle state when no existing summary', async () => {
      mockStore.isLoggedIn = true
      mockGetSummary.mockResolvedValue({
        code: 200,
        message: 'success',
        data: makeSummaryResponse({ status: 0 }),
      })

      const wrapper = mountPanel()
      await nextTick()
      await vi.waitFor(() => {
        expect(wrapper.text()).toContain('生成 AI 总结')
      })
    })

    it('shows generated state when summary exists', async () => {
      mockStore.isLoggedIn = true
      mockGetSummary.mockResolvedValue({
        code: 200,
        message: 'success',
        data: makeSummaryResponse({
          status: 2,
          content: '## AI Summary\n\nThis is a summary.',
        }),
      })

      const wrapper = mountPanel()
      await vi.waitFor(() => {
        expect(wrapper.text()).toContain('AI 摘要')
        expect(wrapper.text()).toContain('AI Summary')
      })
    })

    it('shows generating state when summary is being generated', async () => {
      mockStore.isLoggedIn = true
      mockGetSummary.mockResolvedValue({
        code: 200,
        message: 'success',
        data: makeSummaryResponse({ status: 1 }),
      })

      const wrapper = mountPanel()
      // Wait for getSummary to resolve and state to update
      await vi.waitFor(() => {
        expect(wrapper.text()).toContain('AI 正在总结')
      })
    })

    it('shows idle state when getSummary fails', async () => {
      mockStore.isLoggedIn = true
      mockGetSummary.mockRejectedValue(new Error('Not found'))

      const wrapper = mountPanel()
      await vi.waitFor(() => {
        expect(wrapper.text()).toContain('生成 AI 总结')
      })
    })
  })

  // ── Generate flow ──
  describe('generate summary', () => {
    it('shows generating state after clicking generate button', async () => {
      mockStore.isLoggedIn = true
      mockGetSummary.mockResolvedValue({
        code: 200,
        message: 'success',
        data: makeSummaryResponse({ status: 0 }),
      })
      // Delay the generateSummary so we can test the intermediate state
      mockGenerateSummary.mockReturnValue(
        new Promise((resolve) => {
          setTimeout(() => {
            resolve({
              code: 200,
              message: 'success',
              data: makeSummaryResponse({ status: 2, content: 'Generated summary' }),
            })
          }, 100)
        }),
      )

      const wrapper = mountPanel()
      await new Promise(r => setTimeout(r, 0))

      const btn = wrapper.find('.el-button')
      expect(btn.exists()).toBe(true)
      await btn.trigger('click')

      // Should show generating state
      expect(wrapper.text()).toContain('AI 正在总结')
    })

    it('shows generated summary on success', async () => {
      mockStore.isLoggedIn = true
      mockGetSummary
        .mockResolvedValueOnce({
          code: 200,
          message: 'success',
          data: makeSummaryResponse({ status: 0 }),
        })
        .mockResolvedValue({
          code: 200,
          message: 'success',
          data: makeSummaryResponse({ status: 1, content: '# Generated Summary' }),
        })
      mockGenerateSummary.mockResolvedValue({
        code: 200,
        message: 'success',
        data: makeSummaryResponse({ status: 0 }),
      })

      const wrapper = mountPanel()
      await new Promise(r => setTimeout(r, 0))

      const btn = wrapper.find('.el-button')
      expect(btn.exists()).toBe(true)
      await btn.trigger('click')

      await vi.waitFor(() => {
        expect(wrapper.text()).toContain('AI 摘要')
        expect(wrapper.text()).toContain('Generated Summary')
      }, { timeout: 5000 })
    })

    it('shows regenerate button after successful generation', async () => {
      mockStore.isLoggedIn = true
      mockGetSummary
        .mockResolvedValueOnce({
          code: 200,
          message: 'success',
          data: makeSummaryResponse({ status: 0 }),
        })
        .mockResolvedValue({
          code: 200,
          message: 'success',
          data: makeSummaryResponse({ status: 1, content: 'Summary content' }),
        })
      mockGenerateSummary.mockResolvedValue({
        code: 200,
        message: 'success',
        data: makeSummaryResponse({ status: 0 }),
      })

      const wrapper = mountPanel()
      await new Promise(r => setTimeout(r, 0))

      const btn = wrapper.find('.el-button')
      expect(btn.exists()).toBe(true)
      await btn.trigger('click')

      await vi.waitFor(() => {
        expect(wrapper.text()).toContain('重新生成')
      }, { timeout: 5000 })
    })

    it('calls generateSummary API with correct postId', async () => {
      mockStore.isLoggedIn = true
      mockGetSummary.mockResolvedValue({
        code: 200,
        message: 'success',
        data: makeSummaryResponse({ status: 0 }),
      })
      mockGenerateSummary.mockResolvedValue({
        code: 200,
        message: 'success',
        data: makeSummaryResponse({ status: 2, content: 'ok' }),
      })

      const wrapper = mountPanel()
      await new Promise(r => setTimeout(r, 0))

      const btn = wrapper.find('.el-button')
      await btn.trigger('click')

      expect(mockGenerateSummary).toHaveBeenCalledWith('post-1')
    })
  })

  // ── Error state ──
  describe('error state', () => {
    it('shows error message when generation fails', async () => {
      mockStore.isLoggedIn = true
      mockGetSummary.mockResolvedValue({
        code: 200,
        message: 'success',
        data: makeSummaryResponse({ status: 0 }),
      })
      mockGenerateSummary.mockRejectedValue(new Error('API error'))

      const wrapper = mountPanel()
      await new Promise(r => setTimeout(r, 0))

      const btn = wrapper.find('.el-button')
      await btn.trigger('click')

      await vi.waitFor(() => {
        expect(wrapper.text()).toContain('生成失败')
      })
    })

    it('shows retry button on error', async () => {
      mockStore.isLoggedIn = true
      mockGetSummary.mockResolvedValue({
        code: 200,
        message: 'success',
        data: makeSummaryResponse({ status: 0 }),
      })
      mockGenerateSummary.mockRejectedValue(new Error('API error'))

      const wrapper = mountPanel()
      await new Promise(r => setTimeout(r, 0))

      const btn = wrapper.find('.el-button')
      await btn.trigger('click')

      await vi.waitFor(() => {
        expect(wrapper.text()).toContain('重试')
      })
    })

    it('retries generation when retry button clicked', async () => {
      mockStore.isLoggedIn = true
      mockGetSummary
        .mockResolvedValueOnce({
          code: 200,
          message: 'success',
          data: makeSummaryResponse({ status: 0 }),
        })
        .mockResolvedValue({
          code: 200,
          message: 'success',
          data: makeSummaryResponse({ status: 1, content: 'Success after retry' }),
        })
      mockGenerateSummary
        .mockRejectedValueOnce(new Error('First fail'))
        .mockResolvedValueOnce({
          code: 200,
          message: 'success',
          data: makeSummaryResponse({ status: 0 }),
        })

      const wrapper = mountPanel()
      await new Promise(r => setTimeout(r, 0))

      const btn = wrapper.find('.el-button')
      expect(btn.exists()).toBe(true)
      await btn.trigger('click')

      await vi.waitFor(() => {
        expect(wrapper.text()).toContain('重试')
      })

      const retryBtn = wrapper.find('.el-button')
      await retryBtn.trigger('click')

      await vi.waitFor(() => {
        expect(wrapper.text()).toContain('Success after retry')
      }, { timeout: 5000 })

      expect(mockGenerateSummary).toHaveBeenCalledTimes(2)
    })
  })

  // ── AiQaPanel integration ──
  describe('AiQaPanel integration', () => {
    it('does not show AiQaPanel when no summary', async () => {
      mockStore.isLoggedIn = true
      mockGetSummary.mockResolvedValue({
        code: 200,
        message: 'success',
        data: makeSummaryResponse({ status: 0 }),
      })

      const wrapper = mountPanel()
      await vi.waitFor(() => {
        expect(wrapper.text()).toContain('生成 AI 总结')
      })

      expect(wrapper.findComponent({ name: 'AiQaPanel' }).exists()).toBe(false)
    })

    it('shows AiQaPanel when summary is generated', async () => {
      mockStore.isLoggedIn = true
      mockGetSummary.mockResolvedValue({
        code: 200,
        message: 'success',
        data: makeSummaryResponse({ status: 2, content: 'Summary text' }),
      })

      const wrapper = mountPanel()
      await vi.waitFor(() => {
        expect(wrapper.text()).toContain('AI 摘要')
      })

      expect(wrapper.findComponent({ name: 'AiQaPanel' }).exists()).toBe(true)
    })
  })

  // ── Edge cases ──
  describe('edge cases', () => {
    it('handles exactly 50 character content as valid', async () => {
      mockStore.isLoggedIn = true
      mockGetSummary.mockResolvedValue({
        code: 200,
        message: 'success',
        data: makeSummaryResponse({ status: 0 }),
      })

      const wrapper = mountPanel({ postContent: 'A'.repeat(50) })
      await vi.waitFor(() => {
        expect(wrapper.text()).toContain('生成 AI 总结')
      })
      expect(wrapper.text()).not.toContain('帖子内容过短')
    })

    it('handles summary with errorMessage from API', async () => {
      mockStore.isLoggedIn = true
      mockGetSummary.mockResolvedValue({
        code: 200,
        message: 'success',
        data: makeSummaryResponse({
          status: 2,
          content: null,
          errorMessage: 'AI service timeout',
        }),
      })

      const wrapper = mountPanel()
      await vi.waitFor(() => {
        expect(wrapper.text()).toContain('生成失败')
        expect(wrapper.text()).toContain('AI service timeout')
      })
    })
  })
})
