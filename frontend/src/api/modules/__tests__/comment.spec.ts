import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { R, CommentVO, CommentCreateRequest, PageResult } from '@/api/types'

const mockApi = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('@/api', () => ({
  api: mockApi,
}))

const { commentApi } = await import('../comment')

describe('commentApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getList sends GET /posts/{postId}/comments with query params', async () => {
    mockApi.mockResolvedValue({} as R<PageResult<CommentVO>>)
    await commentApi.getList('p1', { page: 1, size: 20 })
    expect(mockApi).toHaveBeenCalledWith('/posts/p1/comments', { query: { page: 1, size: 20 } })
  })

  it('getList omits query when no params', async () => {
    mockApi.mockResolvedValue({} as R<PageResult<CommentVO>>)
    await commentApi.getList('p1')
    expect(mockApi).toHaveBeenCalledWith('/posts/p1/comments', { query: undefined })
  })

  it('create sends POST /posts/{postId}/comments with body', async () => {
    const data: CommentCreateRequest = { content: 'Nice post!' }
    mockApi.mockResolvedValue({} as R<CommentVO>)
    await commentApi.create('p1', data)
    expect(mockApi).toHaveBeenCalledWith('/posts/p1/comments', { method: 'POST', body: data })
  })

  it('remove sends DELETE /comments/{id}', async () => {
    mockApi.mockResolvedValue({} as R<null>)
    await commentApi.remove('c1')
    expect(mockApi).toHaveBeenCalledWith('/comments/c1', { method: 'DELETE' })
  })

  it('like sends POST /comments/{id}/likes', async () => {
    mockApi.mockResolvedValue({} as R<null>)
    await commentApi.like('c1')
    expect(mockApi).toHaveBeenCalledWith('/comments/c1/likes', { method: 'POST' })
  })

  it('unlike sends DELETE /comments/{id}/likes', async () => {
    mockApi.mockResolvedValue({} as R<null>)
    await commentApi.unlike('c1')
    expect(mockApi).toHaveBeenCalledWith('/comments/c1/likes', { method: 'DELETE' })
  })

  it('recommend sends POST /comments/{id}/recommend', async () => {
    mockApi.mockResolvedValue({} as R<null>)
    await commentApi.recommend('c1')
    expect(mockApi).toHaveBeenCalledWith('/comments/c1/recommend', { method: 'POST' })
  })

  it('unrecommend sends DELETE /comments/{id}/recommend', async () => {
    mockApi.mockResolvedValue({} as R<null>)
    await commentApi.unrecommend('c1')
    expect(mockApi).toHaveBeenCalledWith('/comments/c1/recommend', { method: 'DELETE' })
  })

  it('getDivineComments sends GET /posts/{postId}/comments/divine', async () => {
    mockApi.mockResolvedValue({} as R<CommentVO[]>)
    await commentApi.getDivineComments('p1')
    expect(mockApi).toHaveBeenCalledWith('/posts/p1/comments/divine')
  })
})
