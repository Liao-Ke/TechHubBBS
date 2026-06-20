import { describe, it, expect, vi, beforeEach } from 'vitest'
import { api } from '@/api'
import { aiApi } from '@/api/modules/ai'

vi.mock('@/api', () => ({
  api: vi.fn<(...args: unknown[]) => unknown>(),
}))

describe('aiApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getSummary calls GET /posts/{postId}/ai/summary', () => {
    aiApi.getSummary('post-1')
    expect(api).toHaveBeenCalledWith('/posts/post-1/ai/summary')
  })

  it('generateSummary calls POST /posts/{postId}/ai/summary', () => {
    aiApi.generateSummary('post-1')
    expect(api).toHaveBeenCalledWith('/posts/post-1/ai/summary', {
      method: 'POST',
    })
  })

  it('askQuestion calls POST /posts/{postId}/ai/qa with body', () => {
    aiApi.askQuestion('post-1', { question: 'What is this about?' })
    expect(api).toHaveBeenCalledWith('/posts/post-1/ai/qa', {
      method: 'POST',
      body: { question: 'What is this about?' },
    })
  })

  it('getQaHistory calls GET /posts/{postId}/ai/qa with query', () => {
    aiApi.getQaHistory('post-1', { page: 1, size: 10 })
    expect(api).toHaveBeenCalledWith('/posts/post-1/ai/qa/history', {
      query: { page: 1, size: 10 },
    })
  })

  it('getQaHistory works without params', () => {
    aiApi.getQaHistory('post-1')
    expect(api).toHaveBeenCalledWith('/posts/post-1/ai/qa/history', {
      query: undefined,
    })
  })
})
