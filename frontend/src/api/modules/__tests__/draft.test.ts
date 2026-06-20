import { describe, it, expect, vi, beforeEach } from 'vitest'
import { api } from '@/api'
import { draftApi } from '@/api/modules/draft'

vi.mock('@/api', () => ({
  api: vi.fn<(...args: unknown[]) => unknown>(),
}))

describe('draftApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('save calls POST /drafts with body', () => {
    draftApi.save({ title: 'Test', content: 'Hello' })
    expect(api).toHaveBeenCalledWith('/drafts', {
      method: 'POST',
      body: { title: 'Test', content: 'Hello' },
    })
  })

  it('getList calls GET /drafts', () => {
    draftApi.getList()
    expect(api).toHaveBeenCalledWith('/drafts')
  })

  it('getDetail calls GET /drafts/{id}', () => {
    draftApi.getDetail('draft-1')
    expect(api).toHaveBeenCalledWith('/drafts/draft-1')
  })

  it('check calls GET /drafts/check with postId', () => {
    draftApi.check('post-1')
    expect(api).toHaveBeenCalledWith('/drafts/check', {
      query: { postId: 'post-1' },
    })
  })

  it('check calls GET /drafts/check without query when postId empty', () => {
    draftApi.check()
    expect(api).toHaveBeenCalledWith('/drafts/check', {
      query: {},
    })
  })

  it('remove calls DELETE /drafts/{id}', () => {
    draftApi.remove('draft-1')
    expect(api).toHaveBeenCalledWith('/drafts/draft-1', {
      method: 'DELETE',
    })
  })
})
