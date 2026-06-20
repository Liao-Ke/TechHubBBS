import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

const { mockAiApi } = vi.hoisted(() => ({
  mockAiApi: {
    getSummary: vi.fn<(...args: unknown[]) => unknown>(),
    generateSummary: vi.fn<(...args: unknown[]) => unknown>(),
    askQuestion: vi.fn<(...args: unknown[]) => unknown>(),
    getQaHistory: vi.fn<(...args: unknown[]) => unknown>(),
  },
}))

vi.mock('@/api/modules/ai', () => ({
  aiApi: mockAiApi,
}))

import AiQaPanel from '../AiQaPanel.vue'
import type { AiQaResponse } from '@/api/types'

function makeQaItem(overrides: Partial<AiQaResponse> = {}): AiQaResponse {
  return {
    id: 'qa-1',
    question: 'What is this about?',
    answer: 'This is about Vue 3.',
    createTime: '2026-05-28T10:00:00Z',
    ...overrides,
  }
}

function makePageResult(items: AiQaResponse[], page = 1, pages = 1) {
  return {
    code: 200,
    message: 'success',
    data: {
      records: items,
      total: items.length,
      size: 10,
      current: page,
      pages,
    },
  }
}

function mountPanel(props: { postId?: string; hasSummary?: boolean } = {}) {
  return mount(AiQaPanel, {
    props: {
      postId: 'post-1',
      hasSummary: true,
      ...props,
    },
  })
}

describe('AiQaPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // ── No summary ──
  describe('no summary', () => {
    it('shows placeholder when hasSummary is false', () => {
      const wrapper = mountPanel({ hasSummary: false })
      expect(wrapper.text()).toContain('请先生成 AI 摘要')
    })

    it('does not show question input when hasSummary is false', () => {
      const wrapper = mountPanel({ hasSummary: false })
      expect(wrapper.find('input').exists()).toBe(false)
    })

    it('does not call getQaHistory when hasSummary is false', () => {
      mountPanel({ hasSummary: false })
      expect(mockAiApi.getQaHistory).not.toHaveBeenCalled()
    })
  })

  // ── Question input ──
  describe('question input', () => {
    it('renders question input when hasSummary is true', () => {
      mockAiApi.getQaHistory.mockResolvedValue(makePageResult([]))
      const wrapper = mountPanel()
      expect(wrapper.find('input').exists()).toBe(true)
    })

    it('renders send button', () => {
      mockAiApi.getQaHistory.mockResolvedValue(makePageResult([]))
      const wrapper = mountPanel()
      const buttons = wrapper.findAll('.el-button')
      const sendBtn = buttons.find(b => b.text() === '发送')
      expect(sendBtn).toBeTruthy()
    })

    it('calls askQuestion on submit', async () => {
      mockAiApi.getQaHistory.mockResolvedValue(makePageResult([]))
      mockAiApi.askQuestion.mockResolvedValue({
        code: 200,
        message: 'success',
        data: makeQaItem(),
      })

      const wrapper = mountPanel()
      const input = wrapper.find('input')
      await input.setValue('Test question?')
      await nextTick()

      const buttons = wrapper.findAll('.el-button')
      const sendBtn = buttons.find(b => b.text() === '发送')
      await sendBtn!.trigger('click')

      await vi.waitFor(() => {
        expect(mockAiApi.askQuestion).toHaveBeenCalledWith('post-1', {
          question: 'Test question?',
        })
      })
    })

    it('clears input after successful submission', async () => {
      mockAiApi.getQaHistory.mockResolvedValue(makePageResult([]))
      mockAiApi.askQuestion.mockResolvedValue({
        code: 200,
        message: 'success',
        data: makeQaItem(),
      })

      const wrapper = mountPanel()
      const input = wrapper.find('input')
      await input.setValue('Question?')

      const buttons = wrapper.findAll('.el-button')
      const sendBtn = buttons.find(b => b.text() === '发送')
      await sendBtn!.trigger('click')

      await vi.waitFor(() => {
        expect((input.element as HTMLInputElement).value).toBe('')
      })
    })

    it('shows error on failed submission', async () => {
      mockAiApi.getQaHistory.mockResolvedValue(makePageResult([]))
      mockAiApi.askQuestion.mockRejectedValue(new Error('Service unavailable'))

      const wrapper = mountPanel()
      const input = wrapper.find('input')
      await input.setValue('Question?')

      const buttons = wrapper.findAll('.el-button')
      const sendBtn = buttons.find(b => b.text() === '发送')
      await sendBtn!.trigger('click')

      await vi.waitFor(() => {
        expect(wrapper.text()).toContain('Service unavailable')
      })
    })

    it('does not submit empty question', async () => {
      mockAiApi.getQaHistory.mockResolvedValue(makePageResult([]))

      const wrapper = mountPanel()
      const input = wrapper.find('input')
      await input.setValue('   ')

      const buttons = wrapper.findAll('.el-button')
      const sendBtn = buttons.find(b => b.text() === '发送')
      await sendBtn!.trigger('click')

      expect(mockAiApi.askQuestion).not.toHaveBeenCalled()
    })
  })

  // ── History display ──
  describe('history display', () => {
    it('calls getQaHistory on mount when hasSummary is true', () => {
      mockAiApi.getQaHistory.mockResolvedValue(makePageResult([]))
      mountPanel()
      expect(mockAiApi.getQaHistory).toHaveBeenCalledWith('post-1', {
        page: 1,
        size: 10,
      })
    })

    it('shows empty state when no history', async () => {
      mockAiApi.getQaHistory.mockResolvedValue(makePageResult([]))

      const wrapper = mountPanel()
      await vi.waitFor(() => {
        expect(wrapper.text()).toContain('暂无问答记录')
      })
    })

    it('renders Q&A items from history', async () => {
      mockAiApi.getQaHistory.mockResolvedValue(
        makePageResult([
          makeQaItem({ id: '1', question: 'Q1', answer: 'A1' }),
          makeQaItem({ id: '2', question: 'Q2', answer: 'A2' }),
        ]),
      )

      const wrapper = mountPanel()
      await vi.waitFor(() => {
        expect(wrapper.text()).toContain('Q1')
        expect(wrapper.text()).toContain('A1')
        expect(wrapper.text()).toContain('Q2')
        expect(wrapper.text()).toContain('A2')
      })
    })

    it('renders Q labels for questions and answers', async () => {
      mockAiApi.getQaHistory.mockResolvedValue(
        makePageResult([makeQaItem()]),
      )

      const wrapper = mountPanel()
      await vi.waitFor(() => {
        const qLabels = wrapper.findAll('.ai-qa-panel__qa-label')
        expect(qLabels.length).toBeGreaterThanOrEqual(2)
        expect(qLabels[0]!.text()).toBe('Q')
        expect(qLabels[1]!.text()).toBe('A')
      })
    })

    it('shows error state when history loading fails', async () => {
      mockAiApi.getQaHistory.mockRejectedValue(new Error('Network error'))

      const wrapper = mountPanel()
      await vi.waitFor(() => {
        expect(wrapper.text()).toContain('加载失败')
        expect(wrapper.text()).toContain('Network error')
      })
    })

    it('shows retry button on history error', async () => {
      mockAiApi.getQaHistory.mockRejectedValue(new Error('Network error'))

      const wrapper = mountPanel()
      await vi.waitFor(() => {
        expect(wrapper.text()).toContain('重试')
      })
    })
  })

  // ── Pagination ──
  describe('pagination', () => {
    it('shows pagination when totalPages > 1', async () => {
      mockAiApi.getQaHistory.mockResolvedValue(
        makePageResult([makeQaItem()], 1, 3),
      )

      const wrapper = mountPanel()
      await vi.waitFor(() => {
        expect(wrapper.find('.ai-qa-panel__pagination').exists()).toBe(true)
      })
    })

    it('hides pagination when totalPages <= 1', async () => {
      mockAiApi.getQaHistory.mockResolvedValue(
        makePageResult([]),
      )

      const wrapper = mountPanel()
      await vi.waitFor(() => {
        expect(wrapper.find('.ai-qa-panel__pagination').exists()).toBe(false)
      })
    })

    it('loads next page on pagination change', async () => {
      mockAiApi.getQaHistory.mockResolvedValue(
        makePageResult([makeQaItem()], 1, 3),
      )

      const wrapper = mountPanel()
      await vi.waitFor(() => {
        expect(wrapper.find('.ai-qa-panel__pagination').exists()).toBe(true)
      })

      // Reset mock to track next call
      vi.clearAllMocks()
      mockAiApi.getQaHistory.mockResolvedValue(
        makePageResult([makeQaItem({ id: 'qa-2' })], 2, 3),
      )

      const pagination = wrapper.findComponent({ name: 'ElPagination' })
      await pagination.vm.$emit('current-change', 2)

      await vi.waitFor(() => {
        expect(mockAiApi.getQaHistory).toHaveBeenCalledWith('post-1', {
          page: 2,
          size: 10,
        })
      })
    })
  })

  // ── Header display ──
  describe('header', () => {
    it('renders title when hasSummary is true', async () => {
      mockAiApi.getQaHistory.mockResolvedValue(makePageResult([]))
      const wrapper = mountPanel()
      expect(wrapper.text()).toContain('AI 问答')
    })

    it('renders description text', async () => {
      mockAiApi.getQaHistory.mockResolvedValue(makePageResult([]))
      const wrapper = mountPanel()
      expect(wrapper.text()).toContain('基于帖子内容回答你的问题')
    })
  })

  // ── Markdown rendering in answers ──
  describe('answer rendering', () => {
    it('renders markdown answers via MdViewer', async () => {
      mockAiApi.getQaHistory.mockResolvedValue(
        makePageResult([
          makeQaItem({
            id: '1',
            question: 'What?',
            answer: '**bold answer**',
          }),
        ]),
      )

      const wrapper = mountPanel()
      await vi.waitFor(() => {
        const answerDiv = wrapper.find('.ai-qa-panel__answer .ai-qa-panel__qa-text')
        expect(answerDiv.exists()).toBe(true)
      })
    })
  })
})
