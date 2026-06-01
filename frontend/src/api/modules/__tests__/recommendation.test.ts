import { describe, it, expect, vi, beforeEach } from 'vitest'
import { api } from '@/api'
import { recommendationApi } from '@/api/modules/recommendation'

vi.mock('@/api', () => ({
  api: vi.fn(),
}))

describe('recommendationApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getRecommendations calls GET /recommendations with query', () => {
    recommendationApi.getRecommendations({ page: 1, size: 10 })
    expect(api).toHaveBeenCalledWith('/recommendations', {
      query: { page: 1, size: 10 },
    })
  })

  it('getRecommendations works without params', () => {
    recommendationApi.getRecommendations()
    expect(api).toHaveBeenCalledWith('/recommendations', {
      query: undefined,
    })
  })

  it('getRelatedPosts calls GET /posts/{postId}/related with limit', () => {
    recommendationApi.getRelatedPosts('post-1', 5)
    expect(api).toHaveBeenCalledWith('/posts/post-1/related', {
      query: { limit: 5 },
    })
  })

  it('getRelatedPosts works without limit', () => {
    recommendationApi.getRelatedPosts('post-1')
    expect(api).toHaveBeenCalledWith('/posts/post-1/related', {
      query: { limit: undefined },
    })
  })
})
